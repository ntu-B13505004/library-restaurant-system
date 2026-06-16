package library.src.repository;

import library.src.database.DatabaseManager;
import library.src.model.Book;
import library.src.model.BorrowRecord;
import library.src.model.Fine;
import library.src.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FineRepository {

    private final UserRepository userRepository = new UserRepository();
    private final BookRepository bookRepository = new BookRepository();
    private static final DateTimeFormatter SQLITE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * ✨ 1. 新增或更新罰單金額與狀態
     */
    public boolean saveOrUpdate(Fine fine) {
        String checkSql = "SELECT COUNT(*) FROM fines WHERE record_id = ?";
        String insertSql = "INSERT INTO fines (record_id, amount, is_paid) VALUES (?, ?, ?)";
        String updateSql = "UPDATE fines SET amount = ?, is_paid = ? WHERE record_id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            boolean exists = false;
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, fine.getRecord().getRecordId());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) exists = true;
                }
            }

            if (!exists) {
                try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                    pstmt.setInt(1, fine.getRecord().getRecordId());
                    pstmt.setInt(2, fine.getAmount());
                    pstmt.setInt(3, fine.isPaid() ? 1 : 0);
                    return pstmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setInt(1, fine.getAmount());
                    pstmt.setInt(2, fine.isPaid() ? 1 : 0);
                    pstmt.setInt(3, fine.getRecord().getRecordId());
                    return pstmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 2. 統計某位學生目前「未繳清」的罰單總件數
     */
    public int countUnpaidFinesByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM fines f " +
                "JOIN borrow_records r ON f.record_id = r.record_id " +
                "WHERE r.user_id = ? AND f.is_paid = 0";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * ✨ 3. 透過 Fine ID 尋找單一罰單 (繳費時使用)
     */
    public Fine findById(int fineId) {
        String sql = getBaseSelectSql() + " WHERE f.fine_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fineId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToFine(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 4. 透過借閱紀錄 ID 尋找對應的罰單
     */
    public Fine findByRecordId(int recordId) {
        String sql = getBaseSelectSql() + " WHERE f.record_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, recordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToFine(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 5. 獲取特定學生的所有未繳罰單
     */
    public List<Fine> findUnpaidByUserId(int userId) {
        List<Fine> fines = new ArrayList<>();
        String sql = getBaseSelectSql() + " WHERE r.user_id = ? AND f.is_paid = 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) fines.add(mapResultSetToFine(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fines;
    }

    /**
     * ✨ 6. 獲取全校未繳罰單 (供管理員報表)
     */
    public List<Fine> findAllUnpaid() {
        List<Fine> fines = new ArrayList<>();
        String sql = getBaseSelectSql() + " WHERE f.is_paid = 0 ORDER BY f.created_at DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) fines.add(mapResultSetToFine(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fines;
    }

    // 🛠️ 共用 SQL 基底：聯集查詢 fines 與 borrow_records
    private String getBaseSelectSql() {
        return "SELECT f.fine_id, f.amount, f.is_paid, r.record_id, r.user_id, r.book_id, r.borrow_days, " +
                "r.borrow_date, r.due_date, r.return_date, r.is_overdue " +
                "FROM fines f JOIN borrow_records r ON f.record_id = r.record_id";
    }

    // 🛠️ 輔助方法：將 Result Set 轉換為完整的 Fine 與 BorrowRecord 物件
    private Fine mapResultSetToFine(ResultSet rs) throws SQLException {
        int recordId = rs.getInt("record_id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");
        int borrowDays = rs.getInt("borrow_days");

        User user = userRepository.findById(userId);
        Book book = bookRepository.findById(bookId);
        BorrowRecord record = new BorrowRecord(recordId, user, book, borrowDays);

        record.hydrateHistoricalDates(
                parseSqliteDateTime(rs.getString("borrow_date")),
                parseSqliteDateTime(rs.getString("due_date")),
                parseSqliteDateTime(rs.getString("return_date"))
        );

        return new Fine(
                rs.getInt("fine_id"),
                record,
                rs.getInt("amount"),
                rs.getInt("is_paid") == 1
        );
    }

    private LocalDateTime parseSqliteDateTime(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateStr.replace("T", " "), SQLITE_DATE_FORMATTER);
        } catch (Exception e) {
            return LocalDateTime.parse(dateStr);
        }
    }
}