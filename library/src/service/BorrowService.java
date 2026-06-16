package library.src.service;

import library.src.database.DatabaseManager;
import library.src.model.*;
import library.src.repository.*;

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
            return "❌系統錯誤：找不到該使用者帳號。";
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            return "❌ 權限受限：您的帳號目前處於「停權」狀態，請先繳清罰款或洽管理員。";
        }

        if (user.getRoleLevel() == UserRole.NORMAL && numDays > 7) {
            return "⚠️ 權限不足：普通會員單次借閱上限為 7 天，14 天天數僅限 VIP 會員借閱。";
        }

        int currentBorrowedCount = borrowRepository.countActiveBorrowsByUserId(userId);
        if (!user.canBorrow(currentBorrowedCount)) {
            return "⚠️借閱失敗：已達到您該身分的最高借閱上限，請先還書後再借。";
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
                    return "⚠️借閱失敗：該書籍已被他人搶先借出或目前不可借。";
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
                    System.err.println("⚠️借書事務出錯，已安全回滾 (Rollback)。");
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
     * ✨ 核心功能 2：辦理還書交易（修復先繳罰金再還書導致重複停權的 Bug）
     */
    public String returnBook(int bookId) {
        Connection conn = null;
        try {
            BorrowRecord activeRecord = borrowRepository.findActiveRecordByBookId(bookId);
            if (activeRecord == null) {
                return "❌系統錯誤：此書籍在紀錄中並未被借出。";
            }

            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. 更新書籍狀態為可借
            bookRepository.updateStatusInTransaction(conn, bookId, "AVAILABLE");

            LocalDateTime returnDate = LocalDateTime.now();
            boolean isOverdue = returnDate.isAfter(activeRecord.getDueDate());

            // 2. 更新借閱紀錄歸還時間與狀態
            borrowRepository.updateReturnStatusInTransaction(conn, activeRecord.getRecordId(), returnDate, isOverdue);

            // 3. 處理逾期罰款與聯動停權邏輯
            if (isOverdue) {
                activeRecord.hydrateHistoricalDates(activeRecord.getBorrowDate(), activeRecord.getDueDate(), returnDate);

                long overdueDays = ChronoUnit.DAYS.between(activeRecord.getDueDate().toLocalDate(), returnDate.toLocalDate());
                if (overdueDays <= 0) overdueDays = 1;

                Fine newFine = new Fine(0, activeRecord);

                // 💡 優化點：同時檢查這筆罰單的「歷史金額」與「是否已繳清」
                String checkFineSql = "SELECT amount, is_paid FROM fines WHERE record_id = ?";
                int existingAmount = 0;
                boolean fineExists = false;
                boolean alreadyPaid = false;

                try (PreparedStatement pstmtCheck = conn.prepareStatement(checkFineSql)) {
                    pstmtCheck.setInt(1, activeRecord.getRecordId());
                    try (java.sql.ResultSet rs = pstmtCheck.executeQuery()) {
                        if (rs.next()) {
                            fineExists = true;
                            existingAmount = rs.getInt("amount");
                            alreadyPaid = rs.getInt("is_paid") == 1;
                        }
                    }
                }

                boolean needsSuspension = true; // 預設需要停權

                if (fineExists) {
                    if (newFine.getAmount() > existingAmount) {
                        // 【狀況 A】新罰金大於舊金額（代表繳完罰金後又拖延了天數才還書）
                        // 更新罰金金額，且因為金額增加，必須將繳款狀態重設為「未繳 (0)」
                        String updateFineSql = "UPDATE fines SET amount = ?, is_paid = 0 WHERE record_id = ?";
                        try (PreparedStatement pstmtUpdate = conn.prepareStatement(updateFineSql)) {
                            pstmtUpdate.setInt(1, newFine.getAmount());
                            pstmtUpdate.setInt(2, activeRecord.getRecordId());
                            pstmtUpdate.executeUpdate();
                        }
                        needsSuspension = true;
                    } else {
                        // 【狀況 B】金額沒有變多（代表繳完當天立刻還書，或罰金已達系統上限）
                        if (alreadyPaid) {
                            needsSuspension = false; // 既然之前繳清了且無新罰金，就不需要停權！
                        }
                    }
                } else {
                    // 【狀況 C】原本沒有預建罰單，第一次還書時現場產生
                    String insertFineSql = "INSERT INTO fines (record_id, amount, is_paid, created_at) VALUES (?, ?, 0, ?)";
                    try (PreparedStatement pstmtInsert = conn.prepareStatement(insertFineSql)) {
                        pstmtInsert.setInt(1, activeRecord.getRecordId());
                        pstmtInsert.setInt(2, newFine.getAmount());
                        pstmtInsert.setString(3, returnDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        pstmtInsert.executeUpdate();
                    }
                    needsSuspension = true;
                }

                // 4. 智能動態聯動讀者狀態
                if (needsSuspension) {
                    // 確實有未繳罰金，執行停權
                    String suspendSql = "UPDATE users SET status = 'SUSPENDED' WHERE user_id = ?";
                    try (PreparedStatement pstmtUser = conn.prepareStatement(suspendSql)) {
                        pstmtUser.setInt(1, activeRecord.getUser().getUserId());
                        pstmtUser.executeUpdate();
                    }
                } else {
                    // 💡 防呆安全機制：雖然這本書的罰金繳清了，但要確認他「名下有沒有其他逾期未繳的罰單」
                    String checkOtherFinesSql = "SELECT COUNT(*) FROM fines f " +
                            "JOIN borrow_records br ON f.record_id = br.record_id " +
                            "WHERE br.user_id = ? AND f.is_paid = 0";
                    boolean hasOtherUnpaid = false;
                    try (PreparedStatement pstmtCheckOther = conn.prepareStatement(checkOtherFinesSql)) {
                        pstmtCheckOther.setInt(1, activeRecord.getUser().getUserId());
                        try (java.sql.ResultSet rs = pstmtCheckOther.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                hasOtherUnpaid = true;
                            }
                        }
                    }

                    // 如果名下全數繳清，沒有任何欠款，則確保帳號維持在 ACTIVE 啟用狀態
                    if (!hasOtherUnpaid) {
                        String activeSql = "UPDATE users SET status = 'ACTIVE' WHERE user_id = ?";
                        try (PreparedStatement pstmtUser = conn.prepareStatement(activeSql)) {
                            pstmtUser.setInt(1, activeRecord.getUser().getUserId());
                            pstmtUser.executeUpdate();
                        }
                    }
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
            return "❌還書失敗，資料庫連線/操作異常。";
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

    /**
     * ✨ 管理員功能：查詢全體借閱紀錄（支援學號過濾）
     * 供 AdminDashboardView 呼叫
     */
    public List<BorrowRecord> getAllBorrowRecords(String studentNoFilter) {
        return borrowRepository.findAll(studentNoFilter);
    }

    /**
     * ✨ 新增功能：查詢特定書籍的近期借還歷史紀錄
     */
    public List<BorrowRecord> getBookBorrowHistory(int bookId) {
        return borrowRepository.findAllByBookId(bookId);
    }
}