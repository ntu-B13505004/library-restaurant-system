package library.database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class DataLoader {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void loadInitialData() {
        if (isDataExists()) {
            System.out.println("ℹ️ 資料庫已有數據，跳過 JSON 導入。");
            return;
        }

        System.out.println("⏳ 開始從 JSON 檔案導入 20 筆使用者、200 筆書籍與 30 筆借閱歷史...");
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // 開啟事務加快批次寫入速度

            importUsers(conn);
            importBooks(conn);
            importBorrowRecords(conn);

            conn.commit();
            System.out.println("🎉 歷史數據順利導入完畢！");
        } catch (Exception e) {
            System.err.println("❌ 導入失敗，正在回滾變更...");
            e.printStackTrace();
        }
    }

    private static boolean isDataExists() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    private static void importUsers(Connection conn) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("Users.json")) {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> users = gson.fromJson(reader, listType);

            String sql = "INSERT INTO users (name, email, role, max_books) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Map<String, Object> u : users) {
                    pstmt.setString(1, (String) u.get("name"));
                    pstmt.setString(2, (String) u.get("email"));
                    pstmt.setString(3, "READER"); // 預設角色
                    pstmt.setInt(4, 3);          // 預設上限
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }

    private static void importBooks(Connection conn) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("Books.json")) {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> books = gson.fromJson(reader, listType);

            String sql = "INSERT INTO books (title, author, isbn, status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Map<String, Object> b : books) {
                    pstmt.setString(1, (String) b.get("title"));
                    pstmt.setString(2, (String) b.get("author"));
                    pstmt.setString(3, (String) b.get("isbn"));
                    pstmt.setString(4, "AVAILABLE"); // 預設可借閱
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        }
    }

    private static void importBorrowRecords(Connection conn) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("Borrow_records.json")) {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> records = gson.fromJson(reader, listType);

            String sql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, is_overdue) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (Map<String, Object> r : records) {
                    int userId = ((Double) r.get("user_id")).intValue();
                    int bookId = ((Double) r.get("book_id")).intValue();

                    // ✨ 動態解析相對時間
                    String borrowDate = convertRelativeTime((String) r.get("borrow_date"));
                    String dueDate = convertRelativeTime((String) r.get("due_date"));
                    String returnDate = convertRelativeTime((String) r.get("return_date"));

                    // 計算是否過期
                    int isOverdue = 0;
                    if (returnDate != null && dueDate != null) {
                        LocalDateTime ret = LocalDateTime.parse(returnDate, formatter);
                        LocalDateTime due = LocalDateTime.parse(dueDate, formatter);
                        isOverdue = ret.isAfter(due) ? 1 : 0;
                    }

                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, bookId);
                    pstmt.setString(3, borrowDate);
                    pstmt.setString(4, dueDate);
                    pstmt.setString(5, returnDate);
                    pstmt.setInt(6, isOverdue);
                    pstmt.addBatch();

                    // 💡 如果該紀錄顯示「尚未歸還」(return_date 為空)，同步將該書籍狀態改為已借出
                    if (returnDate == null) {
                        try (PreparedStatement uStmt = conn.prepareStatement("UPDATE books SET status = 'BORROWED' WHERE book_id = ?")) {
                            uStmt.setInt(1, bookId);
                            uStmt.executeUpdate();
                        }
                    }
                }
                pstmt.executeBatch();
            }
        }
    }

    private static String convertRelativeTime(String relativeStr) {
        if (relativeStr == null || relativeStr.trim().isEmpty() || "null".equalsIgnoreCase(relativeStr)) {
            return null;
        }
        // 例如 "-45 days" -> 抽取出 -45
        String numberPart = relativeStr.replace("days", "").trim();
        int days = Integer.parseInt(numberPart);

        // 以執行導入當前的時間為基準進行加減
        return LocalDateTime.now().plusDays(days).format(formatter);
    }
}