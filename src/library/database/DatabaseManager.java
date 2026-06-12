package library.database; // 必須包含 library.

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:library.db";

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // 💡 專業提示：SQLite 預設不會強制檢查外鍵限制，Demo 時執行這行可以開啟外鍵功能，確保資料完整性
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 1. 建立使用者表 (對應簡報 Page 18 與 User.java)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_no TEXT UNIQUE NOT NULL, " + // 學號欄位
                    "name TEXT NOT NULL, " +
                    "password TEXT NOT NULL, " +
                    "role_level TEXT, " +             // NORMAL / VIP / ADMIN
                    "status TEXT, " +                 // ACTIVE / SUSPENDED
                    "created_at TEXT)");

            // 2. 建立書籍表 (對應簡報 Page 20 與 Book.java)
            // 💡 注意：依據規範已將單一 isbn 欄位移除
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "book_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "authors TEXT, " +                // 複數作者字串
                    "subjects TEXT, " +               // 主題
                    "publisher TEXT, " +              // 出版者
                    "publish_year TEXT, " +           // 出版年
                    "edition TEXT, " +                // 版本
                    "format_desc TEXT, " +            // 格式描述
                    "source TEXT, " +                 // 資料來源
                    "note TEXT, " +                   // 附註
                    "status TEXT)");                  // AVAILABLE / BORROWED

            // 3. 建立書籍 ISBN 表 (✨ 核心修改：對應簡報 Page 21 的一對多拆表)
            stmt.execute("CREATE TABLE IF NOT EXISTS book_isbns (" +
                    "isbn_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id INTEGER, " +
                    "isbn TEXT NOT NULL, " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id) ON DELETE CASCADE)");

            // 4. 建立借閱紀錄表 (對應簡報 Page 22 與 BorrowRecord.java)
            // 💡 注意：依據規範，移除 is_overdue 欄位，改納入 borrow_days 與 created_at
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "book_id INTEGER, " +
                    "borrow_date TEXT, " +
                    "due_date TEXT, " +
                    "return_date TEXT, " +
                    "borrow_days INTEGER, " +          // 租借天數
                    "created_at TEXT, " +             // 紀錄建立時間
                    "FOREIGN KEY(user_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id))");

            // 5. 建立罰款表 (對應 Fine.java，保留作為實作功能亮點)
            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "fine_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "record_id INTEGER, " +
                    "amount INTEGER, " +
                    "is_paid INTEGER, " +
                    "FOREIGN KEY(record_id) REFERENCES borrow_records(record_id) ON DELETE CASCADE)");

            System.out.println("📊 SQLite 資料表完全依據專題簡報規格初始化/檢查完成。");
        } catch (SQLException e) {
            System.err.println("❌ 資料庫初始化失敗！");
            e.printStackTrace();
        }
    }
}