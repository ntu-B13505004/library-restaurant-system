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
                    "created_at TEXT DEFAULT CURRENT_TIMESTAMP)");

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

// 🚩 ✨ 修正：自動檢查並建置系統預設管理員（Data Seeding）
// 💡 只有在資料庫「已經有其他學生資料，但缺乏管理員」時才在這裡建立。
// 如果是全新資料庫（連學生都沒有），就留給 DataLoader 統一匯入，避免 ID 鎖死在 10000。
            String checkTotalUsers = "SELECT COUNT(*) FROM users";
            String checkAdminSql = "SELECT COUNT(*) FROM users WHERE role_level = 'ADMIN'";
            try (Statement checkStmt = conn.createStatement();
                 ResultSet rsTotal = checkStmt.executeQuery(checkTotalUsers);
                 ResultSet rsAdmin = checkStmt.executeQuery(checkAdminSql)) {

                int totalUsers = rsTotal.next() ? rsTotal.getInt(1) : 0;
                int adminCount = rsAdmin.next() ? rsAdmin.getInt(1) : 0;

                if (totalUsers > 0 && adminCount == 0) {
                    String insertAdminSql = "INSERT INTO users (user_id, student_no, name, password, role_level, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                        pstmt.setInt(1, 9999);
                        pstmt.setString(2, "ADMIN001");
                        pstmt.setString(3, "系統管理員");
                        pstmt.setString(4, "admin123");
                        pstmt.setString(5, library.model.UserRole.ADMIN.name());
                        pstmt.setString(6, library.model.UserStatus.ACTIVE.name());
                        pstmt.setString(7, java.time.LocalDateTime.now().toString());
                        pstmt.executeUpdate();
                        System.out.println("🚩 系統成功補建管理員帳號：[ ADMIN001 ] (ID: 9999)");
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ 資料庫初始化失敗！");
            e.printStackTrace();
        }
    }
}