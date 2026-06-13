package library.repository;

import library.database.DatabaseManager;
import library.model.Book;
import library.model.BorrowRecord;
import library.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BorrowRecordRepository {

    private final UserRepository userRepository = new UserRepository();
    private final BookRepository bookRepository = new BookRepository();

    /**
     * ✨ 1. 新增借閱紀錄 (由 BorrowService.borrowBook 呼叫)
     */
    public boolean save(int userId, int bookId, LocalDateTime borrowDate, LocalDateTime dueDate) {
        String sql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, is_overdue) VALUES (?, ?, ?, ?, 0)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.setString(3, borrowDate.toString());
            pstmt.setString(4, dueDate.toString());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 儲存借閱紀錄失敗");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 2. 更新歸還時間與逾期狀態 (由 BorrowService.returnBook 呼叫)
     */
    public boolean updateReturnStatus(int recordId, LocalDateTime returnDate, boolean isOverdue) {
        String sql = "UPDATE borrow_records SET return_date = ?, is_overdue = ? WHERE record_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, returnDate != null ? returnDate.toString() : null);
            pstmt.setInt(2, isOverdue ? 1 : 0);
            pstmt.setInt(3, recordId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 更新歸還紀錄失敗，Record ID: " + recordId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 3. 找出某本書目前「尚未歸還」的那筆借閱紀錄
     * 這是還書邏輯的核心，用來找出到底是誰、在什麼時候借了這本即將要還的書。
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
     * 用於串接你的 User.canBorrow(currentBorrowCount) 防禦邏輯！
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

    /**
     * 🛠️ 輔助方法：將資料庫欄位，拼裝轉換回強型別的物件組合
     */
    private BorrowRecord mapResultSetToRecord(ResultSet rs) throws SQLException {
        int recordId = rs.getInt("record_id");
        int userId = rs.getInt("user_id");
        int bookId = rs.getInt("book_id");

        // 透過各自的 Repository 抓取完整的物件，實作多表關聯對接
        User user = userRepository.findById(userId);
        Book book = bookRepository.findById(bookId);

        BorrowRecord record = new BorrowRecord(recordId, user, book,int borrowDays);

        // 解析時間
        String borrowStr = rs.getString("borrow_date");
        String dueStr = rs.getString("due_date");
        String returnStr = rs.getString("return_date");

        // 覆蓋掉建構子預設的時間，還原歷史真實數據
        if (borrowStr != null) {
            // 注意：若資料庫存取格式包含空檔，可視情況做 replace(" ", "T") 處理
            record.getClass().getDeclaredFields(); // 概念性提示
            // 實務上可透過為 BorrowRecord 額外開一個完整欄位 Setter 來注入時間：
            // record.setDates(LocalDateTime.parse(borrowStr), LocalDateTime.parse(dueStr), returnStr != null ? LocalDateTime.parse(returnStr) : null);
        }

        record.checkOverdue(); // 順便依據目前的 return_date 或今日時間刷新逾期布林值
        return record;
    }
}