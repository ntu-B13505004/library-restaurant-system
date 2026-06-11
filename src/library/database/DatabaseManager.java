package library.database; // 必須包含 library.

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:library.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. 建立使用者表 (對應 User.java)
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT, " +
                    "role TEXT, " +
                    "max_books INTEGER)");

            // 2. 建立書籍表 (對應 Book.java)
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "book_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT, " +
                    "isbn TEXT, " +
                    "status TEXT)");

            // 3. 建立借閱紀錄表 (對應 BorrowRecord.java)
            stmt.execute("CREATE TABLE IF NOT EXISTS borrow_records (" +
                    "record_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, " +
                    "book_id INTEGER, " +
                    "borrow_date TEXT, " +
                    "due_date TEXT, " +
                    "return_date TEXT, " +
                    "is_overdue INTEGER, " +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id))");

            // 4. 建立罰款表 (對應 Fine.java)
            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "fine_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "record_id INTEGER, " +
                    "amount INTEGER, " +
                    "is_paid INTEGER, " +
                    "FOREIGN KEY(record_id) REFERENCES borrow_records(record_id))");

            System.out.println("📊 SQLite 資料表初始化/檢查完成。");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}