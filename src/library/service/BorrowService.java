package library.service;

import library.database.DatabaseManager;
import library.model.*;
import library.repository.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BorrowService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRepository;
    private final UserRepository userRepository;

    public BorrowService() {
        this.bookRepository = new BookRepository();
        this.borrowRepository = new BorrowRecordRepository();
        this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心功能 1：辦理借書交易（修復 Race Condition，確保資料一致性）
     */
    public String borrowBook(int userId, int bookId, int numDays) {
        // 1️⃣ 驗證使用者狀態與借閱限制規則 (提早驗證，減少 DB 負擔)
        User user = userRepository.findById(userId);
        if (user == null) {
            return "❌ 系統錯誤：找不到該使用者帳號。";
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            return "❌ 權限受限：您的帳號目前處於「停權」狀態，請先繳清罰款或洽管理員。";
        }

        if (user.getRoleLevel() == UserRole.NORMAL && numDays > 7) {
            return "⚠️ 權限不足：普通會員單次借閱上限為 7 天，14 天天數僅限 VIP 會員借閱。";
        }

        int currentBorrowedCount = borrowRepository.countActiveBorrowsByUserId(userId);
        if (!user.canBorrow(currentBorrowedCount)) {
            return "⚠️ 借閱失敗：已達到您該身分的最高借閱上限，請先還書後再借。";
        }

        // 2️⃣ 執行強原子性交易更新
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 💡 修正 Race Condition：直接在 UPDATE 時加上 status = 'AVAILABLE' 的條件
            // 如果 executeUpdate() 回傳 0，代表這本書不存在或剛剛在零點幾秒前被別人借走了
            String safeUpdateSql = "UPDATE books SET status = 'BORROWED' WHERE book_id = ? AND status = 'AVAILABLE'";
            try (PreparedStatement pstmt = conn.prepareStatement(safeUpdateSql)) {
                pstmt.setInt(1, bookId);
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return "⚠️ 借閱失敗：該書籍已被他人搶先借出或目前不可借。";
                }
            }

            LocalDateTime borrowDate = LocalDateTime.now();
            LocalDateTime dueDate = borrowDate.plusDays(numDays);

            boolean saveRecordSuccess = borrowRepository.saveInTransaction(conn, userId, bookId, borrowDate, dueDate, numDays);
            if (!saveRecordSuccess) {
                throw new SQLException("寫入借閱紀錄時失敗");
            }

            conn.commit();
            return "SUCCESS";
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("⚠️ 借書事務出錯，已安全回滾 (Rollback)。");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return "❌ 資料庫操作失敗，借書交易未完成。";
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * ✨ 核心功能 2：辦理還書交易（修復時間計算誤差與罰款封裝破壞問題）
     */
    public String returnBook(int bookId) {
        Connection conn = null;
        try {
            BorrowRecord activeRecord = borrowRepository.findActiveRecordByBookId(bookId);
            if (activeRecord == null) {
                return "❌ 系統錯誤：此書籍在紀錄中並未被借出。";
            }

            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. 更新書籍狀態為可借
            bookRepository.updateStatusInTransaction(conn, bookId, "AVAILABLE");

            LocalDateTime returnDate = LocalDateTime.now();
            boolean isOverdue = returnDate.isAfter(activeRecord.getDueDate());

            // 2. 更新借閱紀錄歸還時間與狀態
            borrowRepository.updateReturnStatusInTransaction(conn, activeRecord.getRecordId(), returnDate, isOverdue);

            // 3. 處理逾期罰款與停權邏輯
            if (isOverdue) {
                // 將歷史時間塞回 Record，以便 Fine Model 動態計算時使用
                activeRecord.hydrateHistoricalDates(activeRecord.getBorrowDate(), activeRecord.getDueDate(), returnDate);

                // 💡 修正時間誤差：轉為 LocalDate 計算，避免未滿 24 小時被算成 0 天
                long overdueDays = ChronoUnit.DAYS.between(activeRecord.getDueDate().toLocalDate(), returnDate.toLocalDate());
                if (overdueDays <= 0) overdueDays = 1; // 防呆機制：發生跨日但時數極短的邊界情況，至少算 1 天

                // 💡 修正罰款寫死問題：實例化 Fine 物件，讓 Model 自行依照內部常數 (DAILY_FINE) 算錢
                Fine newFine = new Fine(0, activeRecord);

                // 寫入罰款紀錄 (確保在同一個 Transaction 內)
                String insertFineSql = "INSERT INTO fines (record_id, amount, is_paid, created_at) VALUES (?, ?, 0, ?)";
                try (PreparedStatement pstmtFine = conn.prepareStatement(insertFineSql)) {
                    pstmtFine.setInt(1, activeRecord.getRecordId());
                    pstmtFine.setInt(2, newFine.getAmount()); // 取得標準計算出的金額
                    pstmtFine.setString(3, returnDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    pstmtFine.executeUpdate();
                }

                // 💡 聯動停權：產生新罰單，立刻將該學生狀態改為 SUSPENDED
                String suspendSql = "UPDATE users SET status = 'SUSPENDED' WHERE user_id = ?";
                try (PreparedStatement pstmtUser = conn.prepareStatement(suspendSql)) {
                    // 根據簡報規格，被停權的使用者將無法繼續借書
                    pstmtUser.setInt(1, activeRecord.getUser().getUserId());
                    pstmtUser.executeUpdate();
                }

                conn.commit();
                return "SUCCESS_WITH_FINE:" + newFine.getAmount();
            }

            conn.commit();
            return "SUCCESS";
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return "❌ 還書失敗，資料庫連線/操作異常。";
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * ✨ 核心功能 3：查詢特定學生的目前借閱與歷史總紀錄
     */
    public List<BorrowRecord> getUserBorrowHistory(int userId) {
        return borrowRepository.findAllByUserId(userId);
    }
}