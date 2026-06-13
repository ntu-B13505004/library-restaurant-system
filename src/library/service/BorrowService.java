package library.service;

import library.database.DatabaseManager;
import library.model.*;
import library.repository.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
     * ✨ 核心功能 1：辦理借書交易（實作 Transaction 事務機制，確保資料一致性）
     */
    public String borrowBook(int userId, int bookId, int numDays) {
        // 1️⃣ 驗證書籍狀態
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            return "❌ 系統錯誤：找不到該書籍資料。";
        }
        if (book.getStatus() != BookStatus.AVAILABLE){
            return "⚠️ 借閱失敗：該書籍目前已被他人借出或不可借。";
        }

        // 2️⃣ 驗證使用者狀態與借閱限制規則
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

        // 3️⃣ 驗證全數通過，執行強原子性交易更新
        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            bookRepository.updateStatusInTransaction(conn, bookId, "BORROWED");

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
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * ✨ 核心功能 2：辦理還書交易（自動整合 Fine 罰款模型）
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

            bookRepository.updateStatusInTransaction(conn, bookId, "AVAILABLE");

            LocalDateTime returnDate = LocalDateTime.now();
            boolean isOverdue = returnDate.isAfter(activeRecord.getDueDate());

            borrowRepository.updateReturnStatusInTransaction(conn, activeRecord.getRecordId(), returnDate, isOverdue);

            if (isOverdue) {
                activeRecord.hydrateHistoricalDates(activeRecord.getBorrowDate(), activeRecord.getDueDate(), returnDate);

                long overdueDays = ChronoUnit.DAYS.between(activeRecord.getDueDate(), returnDate);
                if (overdueDays == 0) overdueDays = 1;

                int fineAmount =
                        (int)(overdueDays * Fine.DAILY_FINE);
                // 💡 修正：直接在事務中連帶將罰金寫入資料庫，不再留白註解
                String insertFineSql = "INSERT INTO fines (record_id, amount, is_paid, created_at) VALUES (?, ?, 0, ?)";
                try (java.sql.PreparedStatement pstmtFine = conn.prepareStatement(insertFineSql)) {
                    pstmtFine.setInt(1, activeRecord.getRecordId());
                    pstmtFine.setInt(2, fineAmount);
                    pstmtFine.setString(3, returnDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    pstmtFine.executeUpdate();
                }

                conn.commit();
                return "SUCCESS_WITH_FINE:" + fineAmount;
            }

            conn.commit();
            return "SUCCESS";
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return "❌ 還書失敗，資料庫連線異常。";
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * ✨ 核心功能 3：查詢特定學生的目前借閱與歷史總紀錄（學生端畫面表格數據源）
     */
    public List<BorrowRecord> getUserBorrowHistory(int userId) {
        return borrowRepository.findAllByUserId(userId);
    }

    public List<BorrowRecord> getBookBorrowHistory(int bookId) {

        return borrowRepository.findAllByBookId(bookId);

    }

    public List<BorrowRecord>
    getIncomingDueReminders(int userId) {

        List<BorrowRecord> activeRecords =
                borrowRepository
                        .findActiveByUserId(userId);

        List<BorrowRecord> reminders =
                new ArrayList<>();

        for (BorrowRecord record : activeRecords) {

            if (record.isUpcomingDue(3)) {

                reminders.add(record);

            }

        }

        return reminders;
    }
}