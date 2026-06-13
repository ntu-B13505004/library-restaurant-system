package library.database;

import java.sql.*;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:library.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 1. 建立使用者表
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_no TEXT UNIQUE NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "role_level TEXT, " +             // NORMAL / VIP / ADMIN
                    "status TEXT, " +                 // ACTIVE / SUSPENDED
                    "created_at TEXT)");

            // 2. 建立書籍表
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "book_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "authors TEXT, " +
                    "subjects TEXT, " +
                    "publisher TEXT, " +
                    "publish_year TEXT, " +
                    "edition TEXT, " +
                    "format_desc TEXT, " +
                    "source TEXT, " +
                    "note TEXT, " +
                    "status TEXT)");                  // AVAILABLE / BORROWED

            // 3. 建立書籍 ISBN 表
            stmt.execute("CREATE TABLE IF NOT EXISTS book_isbns (" +
                    "isbn_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id INTEGER, " +
                    "isbn TEXT NOT NULL, " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id) ON DELETE CASCADE)");

            // 4. 建立借閱紀錄表
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "book_id INTEGER, " +
                    "borrow_date TEXT, " +
                    "due_date TEXT, " +
                    "return_date TEXT, " +
                    "borrow_days INTEGER, " +
                    "is_overdue INTEGER DEFAULT 0, " +
                    "created_at TEXT, " +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id))");
            // 5. 建立罰款表（💡 修正：更新欄位以完美配合您的 BorrowService.returnBook 罰金儲存規格）
            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "fine_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "record_id INTEGER, " +
                    "amount INTEGER, " +
                    "is_paid INTEGER DEFAULT 0, " +
                    "created_at TEXT, " +
                    "FOREIGN KEY(record_id) REFERENCES borrow_records(record_id) ON DELETE CASCADE)");
            System.out.println("📊 SQLite 資料表完全依據專題簡報規格初始化/檢查完成。");

            // 🚩 ✨ 新增：自動檢查並建置系統預設管理員（Data Seeding）
            String checkAdminSql = "SELECT COUNT(*) FROM users WHERE role_level = 'ADMIN'";
            try (ResultSet rs = stmt.executeQuery(checkAdminSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertAdminSql = "INSERT INTO users (student_no, name, password, role_level, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                        pstmt.setString(1, "ADMIN001"); // 管理員學號（登入帳號）
                        pstmt.setString(2, "系統管理員");
                        pstmt.setString(3, "admin123"); // 預設密碼
                        pstmt.setString(4, library.model.UserRole.ADMIN.name());   // "ADMIN"
                        pstmt.setString(5, library.model.UserStatus.ACTIVE.name()); // "ACTIVE"
                        pstmt.setString(6, java.time.LocalDateTime.now().toString());

                        pstmt.executeUpdate();
                        System.out.println("🚩 系統成功自動內建管理員帳號：[ ADMIN001 ]，密碼：[ admin123 ]");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ 資料庫初始化失敗！");
            e.printStackTrace();
        }
    }
}