package library.repository;

import library.database.DatabaseManager;
import library.model.Book;
import library.model.BorrowRecord;
import library.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BorrowRecordRepository {

    private final UserRepository userRepository = new UserRepository();
    private final BookRepository bookRepository = new BookRepository();

    private static final DateTimeFormatter SQLITE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✨ 1. 新增借閱紀錄 (由 BorrowService.borrowBook 在 Transaction 中呼叫)
     * 💡 移除不必要的內部連線建立，改由外部 Service 傳入共用的 conn
     */
    public boolean saveInTransaction(Connection conn, int userId, int bookId, LocalDateTime borrowDate, LocalDateTime dueDate, int borrowDays) throws SQLException {
        String sql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, borrow_days, is_overdue) VALUES (?, ?, ?, ?, ?, 0)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setString(3, borrowDate.format(SQLITE_DATE_FORMATTER));
            pstmt.setString(4, dueDate.format(SQLITE_DATE_FORMATTER));
            pstmt.setInt(5, borrowDays);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * ✨ 2. 更新歸還時間與逾期狀態 (由 BorrowService.returnBook 在 Transaction 中呼叫)
     * 💡 移除不必要的內部連線建立，改由外部 Service 傳入共用的 conn
     */
    public boolean updateReturnStatusInTransaction(Connection conn, int recordId, LocalDateTime returnDate, boolean isOverdue) throws SQLException {
        String sql = "UPDATE borrow_records SET return_date = ?, is_overdue = ? WHERE record_id = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, returnDate != null ? returnDate.format(SQLITE_DATE_FORMATTER) : null);
            pstmt.setInt(2, isOverdue ? 1 : 0);
            pstmt.setInt(3, recordId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * ✨ 3. 找出某本書目前「尚未歸還」的那筆借閱紀錄
     */
    public BorrowRecord findActiveRecordByBookId(int bookId) {
        String sql = "SELECT * FROM borrow_records WHERE book_id = ? AND return_date IS NULL";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRecord(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 尋找進行中借閱紀錄失敗，Book ID: " + bookId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 4. 統計特定學生目前「正在借閱中（未還）」的書籍數量
     */
    public int countActiveBorrowsByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND return_date IS NULL";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * ✨ 5. 撈取特定學生的所有借閱歷史（個人專區表格數據源）
     */
    public List<BorrowRecord> findAllByUserId(int userId) {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM borrow_records WHERE user_id = ? ORDER BY borrow_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 獲取用戶歷史紀錄失敗，User ID: " + userId);
            e.printStackTrace();
        }
        return records;
    }

    public List<BorrowRecord> findAllByBookId(int bookId) {

        List<BorrowRecord> records = new ArrayList<>();

        String sql =
                "SELECT * FROM borrow_records " +
                        "WHERE book_id = ? " +
                        "ORDER BY borrow_date DESC";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, bookId);

            try (ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {

                    records.add(
                            mapResultSetToRecord(rs)
                    );

                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

        return records;
    }

    /**
     * ✨ 6. 查詢全體借閱紀錄（支援學號模糊/精確過濾）
     * 供 AdminDashboardView 借還紀錄查詢使用
     */
    public List<BorrowRecord> findAll(String studentNoFilter) {
        List<BorrowRecord> records = new ArrayList<>();

        // 利用 JOIN 關聯 users 表，以便透過 student_no 進行篩選
        StringBuilder sql = new StringBuilder(
                "SELECT r.* FROM borrow_records r " +
                        "JOIN users u ON r.user_id = u.user_id "
        );

        if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
            sql.append("WHERE u.student_no LIKE ? ");
        }
        sql.append("ORDER BY r.borrow_date DESC");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
                pstmt.setString(1, "%" + studentNoFilter.trim() + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetToRecord(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 查詢全體借還紀錄失敗");
            e.printStackTrace();
        }
        return records;
    }
    /**
     * 🛠️ 輔助方法：將資料庫欄位，拼裝轉換回強型別的物件組合
     */
    private BorrowRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        int recordId = rs.getInt("record_id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        int borrowDays = rs.getInt("borrow_days");

        User user = userRepository.findById(userId);
        Book book = bookRepository.findById(bookId);

        BorrowRecord record = new BorrowRecord(recordId, user, book, borrowDays);

        String borrowStr = rs.getString("borrow_date");
        String dueStr = rs.getString("due_date");
        String returnStr = rs.getString("return_date");

        LocalDateTime borrowDate = parseSqliteDateTime(borrowStr);
        LocalDateTime dueDate = parseSqliteDateTime(dueStr);
        LocalDateTime returnDate = parseSqliteDateTime(returnStr);

        record.hydrateHistoricalDates(borrowDate, dueDate, returnDate);

        return record;
    }

    public List<BorrowRecord> findActiveByUserId(int userId) {

        List<BorrowRecord> records =
                new ArrayList<>();

        String sql =
                "SELECT * FROM borrow_records " +
                        "WHERE user_id = ? " +
                        "AND return_date IS NULL";

        try (
                Connection conn =
                        DatabaseManager.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, userId);

            try (ResultSet rs =
                         pstmt.executeQuery()) {

                while (rs.next()) {

                    records.add(
                            mapResultSetToRecord(rs)
                    );

                }
            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

        return records;
    }

    /**
     * 🛠️ 輔助方法：安全地將資料庫時間字串轉為 Java LocalDateTime
     */
    private LocalDateTime parseSqliteDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            String cleanStr = dateStr.replace("T", " ");
            return LocalDateTime.parse(cleanStr, SQLITE_DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception ex) {
                System.err.println("❌ 嚴重錯誤：無法解析時間欄位 [" + dateStr + "]，返回 null。");
                return null;
            }
        }
    }

    public BorrowRecord findById(int recordId) {

        String sql =
                "SELECT * FROM borrow_records " +
                        "WHERE record_id = ?";

        try (
                Connection conn =
                        DatabaseManager.getConnection();

                PreparedStatement pstmt =
                        conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, recordId);

            try (ResultSet rs =
                         pstmt.executeQuery()) {

                if (rs.next()) {

                    return mapResultSetToRecord(rs);

                }

            }

        } catch (SQLException e) {

            e.printStackTrace();

        }

        return null;
    }

}