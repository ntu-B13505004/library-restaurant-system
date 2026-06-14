package library.database;

import library.model.BookStatus;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class DataLoader {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void loadInitialData() {
        if (isDataExists()) {
            System.out.println("資料庫已有數據，跳過 JSON 導入。");
            return;
        }

        System.out.println("開始依據專題簡報規格導入使用者、書籍（含ISBN拆表）與借閱歷史...");

        // 調整 try 結構，讓 conn 變數在 catch 區塊中也能被存取，以便安全執行 rollback()
        try (Connection conn = DatabaseManager.getConnection()) {
            try {
                conn.setAutoCommit(false); // 開啟事務加快寫入速度

                importUsers(conn);
                importBooks(conn);          // 安全取回真實自增 ID 並寫入 ISBN
                importBorrowRecords(conn);

                conn.commit(); // 統一提交事務
                System.out.println("🎉 貼近真實情境之歷史數據已順利導入完畢！");
            } catch (Exception e) {
                System.err.println("❌ 導入內部發生錯誤，正在執行資料庫回滾 (Rollback)...");
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("⚠️ 回滾失敗！");
                    ex.printStackTrace();
                }
                e.printStackTrace(); // 印出原始的 JSON 解析或 SQL 錯誤
            }
        } catch (SQLException e) {
            System.err.println("❌ 無法取得資料庫連線！");
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

            String sql = "INSERT INTO users (student_no, name, password, role_level, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int count = 0;
                for (Map<String, Object> u : users) {
                    count++;
                    String studentNo = (String) u.get("student_no");
                    String name = (String) u.get("name");
                    String password = (String) u.get("password");
                    String roleLevel = (String) u.get("role_level");
                    String status = (String) u.get("status");
                    String createdAt = (String) u.get("created_at");

                    if (studentNo == null || name == null) {
                        System.err.println("⚠️ 警告：Users.json 第 " + count + " 筆資料缺少必要欄位，已跳過。");
                        continue;
                    }

                    pstmt.setString(1, studentNo);
                    pstmt.setString(2, name);
                    pstmt.setString(3, password);
                    // 💡 優化：寫入前強制轉大寫，確保符合 Enum 規格
                    pstmt.setString(4, roleLevel != null ? roleLevel.toUpperCase().trim() : "NORMAL");
                    pstmt.setString(5, status != null ? status.toUpperCase().trim() : "ACTIVE");
                    pstmt.setString(6, createdAt != null ? createdAt : LocalDateTime.now().format(formatter));

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

            String bookSql = "INSERT INTO books (title, authors, subjects, publisher, publish_year, edition, format_desc, source, note, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            String isbnSql = "INSERT INTO book_isbns (book_id, isbn) VALUES (?, ?)";

            // 💡 傳入 Statement.RETURN_GENERATED_KEYS 以安全取得資料庫配發的真正主鍵
            try (PreparedStatement bookPstmt = conn.prepareStatement(bookSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement isbnPstmt = conn.prepareStatement(isbnSql)) {

                for (Map<String, Object> b : books) {
                    String title = (String) b.get("題名");
                    if (title == null || title.trim().isEmpty()) {
                        continue;
                    }

                    String authors = joinListOrString(b.get("作者"));
                    String subjects = joinListOrString(b.get("主題"));
                    String publisher = (String) b.get("出版者");
                    String publishYear = (String) b.get("出版日期");
                    String edition = (String) b.get("版本");
                    String formatDesc = (String) b.get("格式");
                    String source = (String) b.get("資料來源");
                    String note = (String) b.get("附註");

                    bookPstmt.setString(1, title);
                    bookPstmt.setString(2, authors != null ? authors : "未知作者");
                    bookPstmt.setString(3, subjects);
                    bookPstmt.setString(4, publisher);
                    bookPstmt.setString(5, publishYear);
                    bookPstmt.setString(6, edition);
                    bookPstmt.setString(7, formatDesc);
                    bookPstmt.setString(8, source);
                    bookPstmt.setString(9, note);
                    bookPstmt.setString(10, "AVAILABLE");

                    // 💡 直接執行單筆寫入。因為關閉了 AutoCommit，仍在同個事務內，速度飛快且絕對安全！
                    bookPstmt.executeUpdate();

                    // 💡 核心修正：向資料庫索取剛剛那本書真正的自增主鍵 ID
                    int realBookId = -1;
                    try (ResultSet generatedKeys = bookPstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            realBookId = generatedKeys.getInt(1);
                        }
                    }

                    // 確保拿到有效 ID 後，再將多個 ISBN 裝進批次佇列中
                    if (realBookId != -1) {
                        Object isbnObj = b.get("識別號");
                        if (isbnObj instanceof List) {
                            for (Object isbn : (List<?>) isbnObj) {
                                isbnPstmt.setInt(1, realBookId);
                                isbnPstmt.setString(2, isbn.toString());
                                isbnPstmt.addBatch();
                            }
                        } else if (isbnObj != null && !isbnObj.toString().trim().isEmpty()) {
                            isbnPstmt.setInt(1, realBookId);
                            isbnPstmt.setString(2, isbnObj.toString());
                            isbnPstmt.addBatch();
                        }
                    }
                }

                // 書籍已在迴圈內陸續寫入，此處一次性把所有收集到的 ISBN 批次打入資料庫
                isbnPstmt.executeBatch();
            }
        }
    }

    private static void importBorrowRecords(Connection conn) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("Borrow_records.json")) {
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            List<Map<String, Object>> records = gson.fromJson(reader, listType);

            String insertSql = "INSERT INTO borrow_records (user_id, book_id, borrow_date, due_date, return_date, borrow_days, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String updateBookSql = "UPDATE books SET status = 'BORROWED' WHERE book_id = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql);
                 PreparedStatement uStmt = conn.prepareStatement(updateBookSql)) {

                boolean hasBookUpdates = false;
                String nowStr = LocalDateTime.now().format(formatter);

                for (Map<String, Object> r : records) {
                    // 💡 安全防禦：改用 ((Number) ...).intValue() 避免潛在的類型轉換異常
                    int userId = ((Number) r.get("user_id")).intValue();
                    int bookId = ((Number) r.get("book_id")).intValue();

                    String borrowDate = convertRelativeTime((String) r.get("borrow_date"));
                    String dueDate = convertRelativeTime((String) r.get("due_date"));
                    String returnDate = convertRelativeTime((String) r.get("return_date"));

                    int borrowDays = 0;
                    if (borrowDate != null && dueDate != null) {
                        LocalDateTime start = LocalDateTime.parse(borrowDate, formatter);
                        LocalDateTime end = LocalDateTime.parse(dueDate, formatter);
                        borrowDays = (int) ChronoUnit.DAYS.between(start, end);
                    }

                    pstmt.setInt(1, userId);
                    pstmt.setInt(2, bookId);
                    pstmt.setString(3, borrowDate);
                    pstmt.setString(4, dueDate);

                    if (returnDate != null) {
                        pstmt.setString(5, returnDate);
                    } else {
                        pstmt.setNull(5, Types.VARCHAR);
                    }

                    pstmt.setInt(6, borrowDays);
                    pstmt.setString(7, nowStr);
                    pstmt.addBatch();

                    if (returnDate == null) {
                        uStmt.setInt(1, bookId);
                        uStmt.addBatch();
                        hasBookUpdates = true;
                    }
                }

                pstmt.executeBatch();
                if (hasBookUpdates) {
                    uStmt.executeBatch();
                }
            }
        }
    }

    private static String joinListOrString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            if (list.isEmpty()) return null;
            return String.join(", ", list.stream().map(Object::toString).toArray(String[]::new));
        }
        String str = obj.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private static String convertRelativeTime(String relativeStr) {
        if (relativeStr == null || relativeStr.trim().isEmpty() || "null".equalsIgnoreCase(relativeStr)) {
            return null;
        }
        try {
            String numberPart = relativeStr.replace("days", "").trim();
            int days = Integer.parseInt(numberPart);
            return LocalDateTime.now().plusDays(days).format(formatter);
        } catch (Exception e) {
            return null;
        }
    }
}