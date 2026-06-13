datebase
DatabaseManager
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
                    "created_at TEXT, " +
                    "FOREIGN KEY(user_id) REFERENCES users(user_id), " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id))");

            // 5. 建立罰款表（💡 修正：更新欄位以完美配合您的 BorrowService.returnBook 罰金儲存規格）
            stmt.execute("CREATE TABLE IF NOT EXISTS fines (" +
                    "fine_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "record_id INTEGER, " +
                    "amount INTEGER, " +
                    "status TEXT, " +                  // UNPAID / PAID
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

DataLoader
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

model
Book
package library.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
private int bookId;
private String title;
private String authors;       // 配合新 DB：改為複數型欄位
private String subjects;      // 主題
private String publisher;     // 出版者
private String publishYear;   // 出版日期/年
private String edition;       // 版本
private String formatDesc;    // 格式
private String source;        // 資料來源
private String note;          // 附註
private BookStatus status;
private List<String> isbns;   // 配合新 DB：一對多拆表，改用 List 儲存複數 ISBN

    // ✨ 完整欄位建構子：供新版 BookRepository 使用
    public Book(int bookId, String title, String authors, String subjects, String publisher,
                String publishYear, String edition, String formatDesc, String source, String note) {
        this.bookId = bookId;
        this.title = title;
        this.authors = authors;
        this.subjects = subjects;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.edition = edition;
        this.formatDesc = formatDesc;
        this.source = source;
        this.note = note;
        this.status = BookStatus.AVAILABLE; // 預設可借
        this.isbns = new ArrayList<>();     // 初始化 List 避免 NullPointerException
    }

    // 💡 保留舊版 4 欄位建構子（自由選擇保留，若其他測試程式有用到可防呆）
    public Book(int bookId, String title, String author, String isbn) {
        this.bookId = bookId;
        this.title = title;
        this.authors = author;
        this.status = BookStatus.AVAILABLE;
        this.isbns = new ArrayList<>();
        if (isbn != null) {
            this.isbns.add(isbn);
        }
    }

    public boolean isAvailable() {
        return this.status == BookStatus.AVAILABLE;
    }

    // 填滿原本空著的 Setter
    public void setStatus(BookStatus status) {
        this.status = status;
    }

    public void setIsbns(List<String> isbns) {
        this.isbns = isbns;
    }

    // 實用方法：方便一筆一筆加入自 book_isbns 表撈出來的資料
    public void addIsbn(String isbn) {
        if (this.isbns == null) {
            this.isbns = new ArrayList<>();
        }
        this.isbns.add(isbn);
    }

    // Getters
    public int getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthors() { return authors; } // 原 getAuthor() 改為複數
    public List<String> getIsbns() { return isbns; }
    public BookStatus getStatus() { return status; }
    public String getSubjects() { return subjects; }
    public String getPublisher() { return publisher; }
    public String getPublishYear() { return publishYear; }
    public String getEdition() { return edition; }
    public String getFormatDesc() { return formatDesc; }
    public String getSource() { return source; }
    public String getNote() { return note; }
}

BorrowRecord
package library.model;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class BorrowRecord {
private int recordId;
private User user;
private Book book;
private int borrowDays;
private LocalDateTime borrowDate;
private LocalDateTime dueDate;
private LocalDateTime returnDate;                          //null->沒還
private boolean isOverdue;

    public BorrowRecord(int recordId, User user, Book book, int borrowDays) {
        this.recordId = recordId;
        this.user = user;
        this.book = book;
        this.borrowDate = LocalDateTime.now();
        this.dueDate = borrowDate.plusDays(borrowDays);
        this.returnDate = null;
        this.isOverdue = false;
    }

    public void markReturned() {
        this.returnDate = LocalDateTime.now();
        checkOverdue();                                     //還書時check逾期
    }
    public void hydrateHistoricalDates(LocalDateTime borrowDate, LocalDateTime dueDate, LocalDateTime returnDate) {
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;

        // 歷史時間重設後，立即根據還書狀態與時間，重新計算該筆紀錄目前是否逾期
        this.checkOverdue();
    }

    /**
     * 💡 檢查並刷新逾期狀態的邏輯（確保動態跟隨還原的時間）
     */
    public void checkOverdue() {
        if (this.returnDate != null) {
            // 如果已經還書，就比較「還書時間」有沒有超過「到期日」
            this.isOverdue = this.returnDate.isAfter(this.dueDate);
        } else {
            // 如果還沒還書，就比較「此時此刻」有沒有超過「到期日」
            this.isOverdue = LocalDateTime.now().isAfter(this.dueDate);
        }
    }

    public boolean isUpcomingDue(int alertDays) {
        if (returnDate != null) return false;
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), this.dueDate);
        return daysLeft >= 0 && daysLeft <= alertDays;
    }
    // Getters
    public int getRecordId() { return recordId; }
    public User getUser() { return user; }
    public Book getBook() { return book; }
    public LocalDateTime getBorrowDate() { return borrowDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public boolean isOverdue() { return isOverdue; }


    public int getBorrowDays() {return borrowDays;}
}
Fine
package library.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Fine {
private int fineId;
private BorrowRecord record; // 關聯的借閱紀錄
private int amount;          // 若已結清，記錄當時繳納的金額；若未結清，則作為快照緩存
private boolean isPaid;

    private static final int DAILY_FINE = 10; // 每天 10 元

    /**
     * 從資料庫撈出已存在罰款紀錄時使用的建構子
     */
    public Fine(int fineId, BorrowRecord record, int amount, boolean isPaid) {
        this.fineId = fineId;
        this.record = record;
        this.amount = amount;
        this.isPaid = isPaid;
    }

    /**
     * 新生成罰款紀錄時使用的建構子（預設未繳清）
     */
    public Fine(int fineId, BorrowRecord record) {
        this.fineId = fineId;
        this.record = record;
        this.isPaid = false;
        this.amount = calculateCurrentAmount();
    }

    /**
     * ✨ 動態計算最新應繳金額
     */
    public int calculateCurrentAmount() {
        if (record == null || !record.isOverdue()) return 0;

        // 如果書已經還了，就計算到「歸還當天」；如果還沒還，就動態計算到「此時此刻」
        LocalDateTime endPoint = (record.getReturnDate() != null)
                ? record.getReturnDate()
                : LocalDateTime.now();

        long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), endPoint);

        // 確保不會出現負數天數
        return overdueDays > 0 ? (int) overdueDays * DAILY_FINE : 0;
    }

    /**
     * ✨ 修改此 Getter：確保未繳清前，每次 GUI 刷新看到的都是最新累積的罰金
     */
    public int getAmount() {
        if (!isPaid) {
            // 還沒付錢前，金額隨時間動態與時俱進
            this.amount = calculateCurrentAmount();
        }
        return amount;
    }

    /**
     * 執行繳款
     */
    public void pay() {
        // 繳款當下，鎖定最終的罰金金額
        this.amount = calculateCurrentAmount();
        this.isPaid = true;
    }

    // 其他 Getters
    public int getFineId() { return fineId; }
    public BorrowRecord getRecord() { return record; }
    public boolean isPaid() { return isPaid; }
}
User
package library.model;

public class User {
private int userId;
private String studentNo;    // 學號
private String name;
private String password;     // 密碼
private UserRole roleLevel;    // 權限等級：NORMAL / VIP / ADMIN
private UserStatus status;       // 狀態：ACTIVE / SUSPENDED
private String createdAt;    // 帳號建立時間

    // ✨ 完整欄位建構子：供新版 UserRepository 使用
    public User(int userId, String studentNo, String name, String password, UserRole roleLevel, UserStatus status, String createdAt) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.name = name;
        this.password = password;
        this.roleLevel = roleLevel;
        this.status = status;
        this.createdAt = createdAt;
    }

    // 🎯 核心邏輯優化：依據簡報規範之身份，動態回傳借書上限
    public int getBorrowLimit() {
        if ("VIP".equalsIgnoreCase(String.valueOf(roleLevel))) {
            return 5; // 舉例：VIP 可借 5 本
        } else if ("ADMIN".equalsIgnoreCase(String.valueOf(roleLevel))) {
            return 999; // 管理員不限
        }
        return 3; // NORMAL 預設 3 本
    }

    // 🎯 核心邏輯優化：不只要看數量有沒有超標，還要看這個使用者有沒有被停權（SUSPENDED）！
    public boolean canBorrow(int currentBorrowCount) {
        // 只有處於 ACTIVE 狀態，且目前借閱量小於該身份上限的人才可以借書
        return "ACTIVE".equalsIgnoreCase(String.valueOf(this.status)) && currentBorrowCount < getBorrowLimit();
    }

    // Getters & Setters
    public int getUserId() { return userId; }
    public String getStudentNo() { return studentNo; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public UserRole getRoleLevel() { return roleLevel; }
    public void setRoleLevel(UserRole roleLevel) { this.roleLevel = roleLevel; }
    public String getCreatedAt() { return createdAt; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }}
BookStatus
package library.model;

public enum BookStatus {
AVAILABLE,    // 可借閱
BORROWED,     // 已借出
RESERVED,     // 預約中
UNAVAILABLE   // 不可借（下架/維修）
}
UserRole
package library.model;

public enum UserRole {
NORMAL,   // 讀者
VIP,
ADMIN     // 管理員
}
UserStatus
package library.model;

public enum UserStatus {
ACTIVE,
SUSPENDED
}

repository
BookRepository
package library.repository;

import library.database.DatabaseManager;
import library.model.Book;
import library.model.BookStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookRepository {

    /**
     * ✨ 效能優化版 findAll()：利用 LEFT JOIN 解決 N+1 問題
     * 一次性將書籍主表與 ISBN 子表聯集撈出，並在 Java 端用 LinkedHashMap 自動分組歸類。
     */
    public List<Book> findAll() {
        // 使用 LinkedHashMap 保持從資料庫撈出來的原始順序
        Map<Integer, Book> bookMap = new LinkedHashMap<>();

        String sql = "SELECT b.*, i.isbn FROM books b " +
                "LEFT JOIN book_isbns i ON b.book_id = i.book_id";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int bookId = rs.getInt("book_id");

                // 檢查這本書是不是在同一個聯集結果中已經被建立過了
                Book book = bookMap.get(bookId);
                if (book == null) {
                    book = new Book(
                            bookId,
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("subjects"),
                            rs.getString("publisher"),
                            rs.getString("publish_year"),
                            rs.getString("edition"),
                            rs.getString("format_desc"),
                            rs.getString("source"),
                            rs.getString("note")
                    );

                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        book.setStatus(BookStatus.valueOf(statusStr));
                    }
                    bookMap.put(bookId, book);
                }

                // 一對多拆表精隨：直接將這一橫列撈到的 isbn 塞進該書的 List 中
                String isbn = rs.getString("isbn");
                if (isbn != null) {
                    book.addIsbn(isbn); // 呼叫我們在 Book.java 寫好的方便方法
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 查詢所有書籍失敗（JOIN 版）");
            e.printStackTrace();
        }
        return new ArrayList<>(bookMap.values());
    }

    /**
     * 根據書籍 ID 尋找單一書籍（包含多個 ISBN）
     */
    public Book findById(int bookId) {
        String sql = "SELECT * FROM books WHERE book_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Book book = new Book(
                            rs.getInt("book_id"),
                            rs.getString("title"),
                            rs.getString("authors"),
                            rs.getString("subjects"),
                            rs.getString("publisher"),
                            rs.getString("publish_year"),
                            rs.getString("edition"),
                            rs.getString("format_desc"),
                            rs.getString("source"),
                            rs.getString("note")
                    );

                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        book.setStatus(BookStatus.valueOf(statusStr));
                    }

                    // 單筆查詢直接帶入對應 ISBN
                    book.setIsbns(findIsbnsByBookId(conn, bookId));
                    return book;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 根據 ID 查詢書籍失敗: " + bookId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 亮點功能：跨資料表模糊關鍵字搜尋 (為 GUI 搜尋框量身打造)
     * 支援同時檢索：書名、作者、主題、以及該書名下的任一條 ISBN！
     */
    public List<Book> search(String keyword) {
        Map<Integer, Book> bookMap = new LinkedHashMap<>();

        // 使用 DISTINCT 與 LEFT JOIN 確保關鍵字撞到多個 ISBN 時不會產生重複的 Book 物件
        String sql = "SELECT DISTINCT b.*, i.isbn FROM books b " +
                "LEFT JOIN book_isbns i ON b.book_id = i.book_id " +
                "WHERE b.title LIKE ? OR b.authors LIKE ? OR b.subjects LIKE ? OR i.isbn LIKE ?";

        String matchKey = "%" + keyword + "%";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, matchKey);
            pstmt.setString(2, matchKey);
            pstmt.setString(3, matchKey);
            pstmt.setString(4, matchKey);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int bookId = rs.getInt("book_id");
                    Book book = bookMap.get(bookId);
                    if (book == null) {
                        book = new Book(
                                bookId,
                                rs.getString("title"),
                                rs.getString("authors"),
                                rs.getString("subjects"),
                                rs.getString("publisher"),
                                rs.getString("publish_year"),
                                rs.getString("edition"),
                                rs.getString("format_desc"),
                                rs.getString("source"),
                                rs.getString("note")
                        );
                        String statusStr = rs.getString("status");
                        if (statusStr != null) {
                            book.setStatus(BookStatus.valueOf(statusStr));
                        }
                        bookMap.put(bookId, book);
                    }
                    String isbn = rs.getString("isbn");
                    if (isbn != null) {
                        book.addIsbn(isbn);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 關鍵字搜尋書籍失敗，關鍵字: " + keyword);
            e.printStackTrace();
        }
        return new ArrayList<>(bookMap.values());
    }

    /**
     * 更新書籍狀態（例如：借出、還書時使用）
     */
    public void updateStatus(int bookId, BookStatus status) {
        String sql = "UPDATE books SET status = ? WHERE book_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name());
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("❌ 更新書籍狀態失敗");
            e.printStackTrace();
        }
    }

    /**
     * 輔助私有方法：專門用來撈取某個 book_id 的所有 ISBN 列表
     */
    private List<String> findIsbnsByBookId(Connection conn, int bookId) {
        List<String> isbns = new ArrayList<>();
        String sql = "SELECT isbn FROM book_isbns WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    isbns.add(rs.getString("isbn"));
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ 查詢書籍 ISBN 失敗，Book ID: " + bookId);
        }
        return isbns;
    }
    public boolean updateStatusInTransaction(Connection conn, int bookId, String status) throws SQLException {
        String sql = "UPDATE books SET status = ? WHERE book_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
BorrowRecordRepository
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
}
FineRepository
package library.repository;

import library.database.DatabaseManager;
import library.model.BorrowRecord;
import library.model.Fine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FineRepository {

    // 需要用這個來重組 BorrowRecord 物件
    // private final BorrowRecordRepository recordRepository = new BorrowRecordRepository();

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
     * 用於 FineService 自動稽核該學生能不能「滿血復權（ACTIVE）」！
     */
    public int countUnpaidFinesByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM fines f " +
                "JOIN borrow_records r ON f.record_id = r.record_id " +
                "WHERE r.user_id = ? AND f.is_paid = 0";

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
}
UserRepository
package library.repository;

import library.database.DatabaseManager;
import library.model.*;

import java.sql.*;

public class UserRepository {

    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            UserRole.valueOf(rs.getString("role_level").toUpperCase().trim()),
                            UserStatus.valueOf(rs.getString("status").toUpperCase().trim()),
                            rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 根據 ID 查詢使用者失敗: " + userId);
            e.printStackTrace();
        }
        return null;
    }

    public User findByStudentNo(String studentNo) {
        String sql = "SELECT * FROM users WHERE student_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserRole role = UserRole.valueOf(rs.getString("role_level").toUpperCase().trim());
                    UserStatus status = UserStatus.valueOf(rs.getString("status").toUpperCase().trim());

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            role,
                            status,
                            rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 根據學號查詢使用者失敗: " + studentNo);
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateUserStatus(int userId, UserStatus newStatus) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 更新使用者狀態失敗，User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 新增功能：變更使用者角色等級（用於管理者設定或升級 VIP/ADMIN）
     */
    public boolean updateUserRole(int userId, UserRole newRole) {
        String sql = "UPDATE users SET role_level = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.name()); // 傳入 "ADMIN", "VIP", 或 "NORMAL"
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 更新使用者權限等級失敗，User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }
}

service
BookService
package library.service;

import library.model.Book;
import library.model.BookStatus;
import library.repository.BookRepository;

// 以下為概念性 import，視你實際的 package 命名調整
// import library.model.User;
// import library.model.BorrowRecord;
// import library.repository.BorrowRecordRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BookService {

    private final BookRepository bookRepository;
    // private final BorrowRecordRepository borrowRecordRepository; // 實作借還紀錄時對接

    public BookService() {
        this.bookRepository = new BookRepository();
        // this.borrowRecordRepository = new BorrowRecordRepository();
    }

    /**
     * 1. 查詢所有書籍
     */
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    /**
     * 2. 跨欄位關鍵字搜尋（過濾空白字串，提升系統強健度）
     */
    public List<Book> searchBooks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.search(keyword.trim());
    }

    /**
     * 3. ✨ 核心業務邏輯：辦理借書
     * 驗證書籍狀態、使用者權限、可借天數，確保資料完整性。
     * * @param bookId   要借閱的書籍 ID
     * @param student  目前登入的學生用戶物件（可用來檢查 VIP 或停權狀態）
     * @param numDays  選擇的租借期限 (1, 3, 7, 14 天)
     * @return 借書結果訊息（若成功回傳 "SUCCESS"，失敗則回傳原因供 GUI 彈窗顯示）
     */
    public String borrowBook(int bookId, Object student, int numDays) {
        // [業務驗證 A] 檢查書籍是否存在
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            return "❌ 系統錯誤：找不到該書籍資料。";
        }

        // [業務驗證 B] 檢查書籍目前狀態是否可借 (簡報第 12 頁規格)
        if (book.getStatus() != BookStatus.AVAILABLE) {
            return "⚠️ 借閱失敗：該書籍已被他人借出。";
        }

        // [業務驗證 C] 檢查使用者是否被管理員「停權 (SUSPENDED)」(簡報第 14 頁規格)
        /*
        if (student.getStatus().equals("SUSPENDED")) {
            return "❌ 權限受限：您的帳號已被停權，請洽管理員復權。";
        }
        */

        // [業務驗證 D] 驗證普通與 VIP 的權限差異 (簡報第 13 頁規格)
        /*
        if (student.getRoleLevel().equals("NORMAL") && numDays > 7) {
            return "⚠️ 權限不足：普通會員單次借閱上限為 7 天，14 天天數僅限 VIP 借閱。";
        }
        */

        // 驗證全數通過，開始執行資料庫更新（Transaction 概念）
        try {
            // 1. 更新書籍主表狀態為已借出 (BORROWED)
            bookRepository.updateStatus(bookId, BookStatus.BORROWED);

            // 2. 建立一筆新的借閱紀錄 (對接你的 Borrow_records 表)
            // LocalDateTime borrowDate = LocalDateTime.now();
            // LocalDateTime dueDate = borrowDate.plusDays(numDays);
            // borrowRecordRepository.save(new BorrowRecord(..., student.getId(), bookId, borrowDate, dueDate));

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 資料庫更新失敗，請稍後再試。";
        }
    }

    /**
     * 4. ✨ 核心業務邏輯：辦理還書
     */
    public String returnBook(int bookId, int recordId) {
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            return "❌ 找不到書籍。";
        }

        try {
            // 1. 將書籍狀態調回可借閱 (AVAILABLE)
            bookRepository.updateStatus(bookId, BookStatus.AVAILABLE);

            // 2. 結束借閱紀錄，填入 return_date (對接 borrowRecordRepository)
            // BorrowRecord record = borrowRecordRepository.findById(recordId);
            // record.setReturnDate(LocalDateTime.now());
            // borrowRecordRepository.update(record);

            // 3. 💡 結合你的 Fine 模型：檢查是否逾期並跳出金額
            // if (record.isOverdue()) {
            //     Fine fine = new Fine(0, record);
            //     return "SUCCESS_WITH_FINE:" + fine.getAmount();
            // }

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 還書處理失敗。";
        }
    }

    /**
     * 5. ✨ 進階業務亮點：篩選「即將到期（剩餘 3 天內）」的借閱通知
     * 為 GUI 的「到期提醒彈窗/公告欄」提供專屬數據源 (簡報第 13 頁規格)
     */
    public List<Object> getIncomingDueReminders(int userId) {
        List<Object> reminders = new ArrayList<>();
        // 1. 從 Repository 撈出該用戶「所有未歸還」的借閱紀錄
        // List<BorrowRecord> activeRecords = borrowRecordRepository.findActiveByUserId(userId);

        // 2. 跑迴圈比對時間差
        /*
        for (BorrowRecord record : activeRecords) {
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), record.getDueDate());
            if (daysLeft >= 0 && daysLeft <= 3) { // 距離到期日剩餘 3 天內
                reminders.add(record);
            }
        }
        */
        return reminders;
    }

    /**
     * 6. 管理者功能：新增書籍（上架）(簡報第 14 頁規格)
     */
    public boolean addBook(Book book) {
        // 可在此處做輸入欄位欄位檢驗（如書名不可為空、ISBN 格式驗證等）
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            return false;
        }
        // 呼叫 bookRepository.insert(book); (待擴充)
        return true;
    }

    /**
     * 7. 管理者功能：下架書籍 (簡報第 14 頁規格)
     */
    public boolean removeBook(int bookId) {
        Book book = bookRepository.findById(bookId);
        // 如果書本還在被借出狀態，不允許下架，保護資料一致性
        if (book == null || book.getStatus() == BookStatus.BORROWED) {
            return false;
        }
        // 呼叫 bookRepository.delete(bookId); (待擴充)
        return true;
    }
}
BorrowService
package library.service;

import library.database.DatabaseManager;
import library.model.*;
import library.repository.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        if (!"AVAILABLE".equalsIgnoreCase(String.valueOf(book.getStatus()))) {
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

                int fineAmount = (int) (overdueDays * 50);

                // 💡 修正：直接在事務中連帶將罰金寫入資料庫，不再留白註解
                String insertFineSql = "INSERT INTO fines (record_id, amount, status, created_at) VALUES (?, ?, 'UNPAID', ?)";
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
}
FineService
package library.service;

import library.model.BorrowRecord;
import library.model.Fine;
import java.util.ArrayList;
import java.util.List;

// 以下為概念性 import，視你實際的 Repository 命名與架構調整
// import library.repository.FineRepository;
// import library.repository.UserRepository;

public class FineService {

    // private final FineRepository fineRepository;
    // private final UserRepository userRepository;

    public FineService() {
        // this.fineRepository = new FineRepository();
        // this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心交易 1：為逾期紀錄建立/更新罰單
     * 當學生「還書時」發現逾期，或是管理者「手動刷新整理」時呼叫。
     * * @param record 該筆逾期的借閱紀錄
     * @return 建立或更新後的 Fine 物件
     */
    public Fine createOrUpdateFine(BorrowRecord record) {
        if (record == null || !record.isOverdue()) {
            return null;
        }

        try {
            // 1. 先去資料庫查這筆借閱紀錄是否已經有對應的罰單紀錄
            // Fine existingFine = fineRepository.findByRecordId(record.getRecordId());
            Fine existingFine = null; // 暫代 stub

            if (existingFine == null) {
                // 如果是新逾期，new 一個 Fine 物件（此時會呼叫你寫的 calculateCurrentAmount() 算錢）
                Fine newFine = new Fine(0, record);

                // 寫入資料庫 fines 表
                // fineRepository.save(newFine);

                // 💡 聯動停權邏輯：一旦產生新罰單，立刻將該學生狀態改為 SUSPENDED (簡報第14頁規格)
                // userRepository.updateStatus(record.getUserId(), "SUSPENDED");

                return newFine;
            } else {
                // 如果罰單早就存在且還沒付錢，每次呼叫 getAmount() 都會動態根據最新時間重新計價
                if (!existingFine.isPaid()) {
                    int currentAmount = existingFine.getAmount(); // 觸發你寫的動態更新邏輯
                    // fineRepository.updateAmount(existingFine.getFineId(), currentAmount);
                }
                return existingFine;
            }
        } catch (Exception e) {
            System.err.println("❌ 處理罰單紀錄失敗");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ✨ 核心交易 2：辦理繳納罰款 (為 GUI 「繳費按鈕」量身打造)
     * * @param fineId 罰單的 ID
     * @return 繳費結果訊息（"SUCCESS" 或 錯誤原因）
     */
    public String payFine(int fineId) {
        try {
            // 1. 從資料庫撈出這筆罰單
            // Fine fine = fineRepository.findById(fineId);
            Fine fine = null; // 暫代 stub

            if (fine == null) {
                return "❌ 系統錯誤：找不到該筆罰款紀錄。";
            }
            if (fine.isPaid()) {
                return "⚠️ 提示：此筆罰款先前已繳清，請勿重複繳納。";
            }

            // 2. 執行繳款（鎖定最終金額，將 isPaid 設為 true）
            fine.pay();

            // 3. 更新資料庫中該筆罰單的狀態與最終金額
            // fineRepository.update(fine);

            // 4. 💡 自動復權稽核（關鍵加分業務）：檢查該學生是否「所有罰單都繳清了」？
            int userId = fine.getRecord().getRecordId();
            // int remainingUnpaid = fineRepository.countUnpaidByUserId(userId);
            int remainingUnpaid = 0; // 模擬全部繳清的情境

            if (remainingUnpaid == 0) {
                // 【簡報規格：復權】如果債務全部清空，自動幫學生改回 ACTIVE 狀態！
                // userRepository.updateStatus(userId, "ACTIVE");
                return "SUCCESS_AND_ACTIVATED"; // 讓 GUI 知道學生滿血復活了
            }

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 資料庫更新失敗，繳費交易未完成。";
        }
    }

    /**
     * ✨ 查詢功能 3：獲取特定學生目前「所有未繳清」的罰單清單（學生個人專區、未繳警示表格使用）
     */
    public List<Fine> getUnpaidFinesByUser(int userId) {
        List<Fine> unpaidList = new ArrayList<>();
        // 1. 從 DB 撈出該用戶 is_paid = 0 的所有罰單
        // unpaidList = fineRepository.findUnpaidByUserId(userId);

        // 2. 💡 極度重要：因為未繳清的罰單金額會隨時間每天+10元，
        //    所以在拋給 GUI 顯示前，跑迴圈呼叫一次 getAmount() 確保畫面上顯示的是「今日最新應繳總額」
        for (Fine fine : unpaidList) {
            fine.getAmount();
        }
        return unpaidList;
    }

    /**
     * ✨ 查詢功能 4：計算特定學生目前的累積未繳總金額
     * 用於 GUI 主畫面右上角顯示：「您目前累積未繳罰款：$ X 元」
     */
    public int getTotalUnpaidAmount(int userId) {
        List<Fine> unpaidFines = getUnpaidFinesByUser(userId);
        int total = 0;
        for (Fine fine : unpaidFines) {
            total += fine.getAmount();
        }
        return total;
    }

    /**
     * ✨ 管理者功能 5：全校未繳罰款黑名單流水帳（管理員後台報表）
     */
    public List<Fine> getAllUnpaidFines() {
        // List<Fine> allUnpaid = fineRepository.findAllUnpaid();
        // for(Fine f : allUnpaid) { f.getAmount(); }
        // return allUnpaid;
        return new ArrayList<>();
    }
}
ReportService
package library.service;

import library.database.DatabaseManager;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportService {

    public ReportService() {
        // 初始化需要的 Repository 或設定
    }

    /**
     * ✨ 進階加分功能 1：統計各書籍主題（Subjects）的借閱熱度排名
     * 專為 JavaFX PieChart（圓餅圖）或 BarChart（柱狀圖）設計的數據源。
     * * @return Map<主題名稱, 借閱次數>，使用 LinkedHashMap 確保排序由高到低
     */
    public Map<String, Integer> getBookSubjectPopularity() {
        Map<String, Integer> reportData = new LinkedHashMap<>();

        // 透過 JOIN 結合書籍表與借閱紀錄表，並依據主題進行分組統計
        String sql = "SELECT b.subjects, COUNT(r.record_id) AS borrow_count " +
                "FROM borrow_records r " +
                "JOIN books b ON r.book_id = b.book_id " +
                "WHERE b.subjects IS NOT NULL AND b.subjects != '' " +
                "GROUP BY b.subjects " +
                "ORDER BY borrow_count DESC";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String subject = rs.getString("subjects");
                int count = rs.getInt("borrow_count");
                reportData.put(subject, count);
            }
        } catch (SQLException e) {
            System.err.println("❌ 產生書籍主題借閱統計報表失敗");
            e.printStackTrace();
        }
        return reportData;
    }

    /**
     * ✨ 進階加分功能 2：統計全校借閱排行榜 Top 5 書籍
     * 讓管理員知道哪些書最熱門、最需要購置新副本。
     * * @return Map<書名, 借閱次數>
     */
    public Map<String, Integer> getTop5BorrowedBooks() {
        Map<String, Integer> reportData = new LinkedHashMap<>();

        String sql = "SELECT b.title, COUNT(r.record_id) AS borrow_count " +
                "FROM borrow_records r " +
                "JOIN books b ON r.book_id = b.book_id " +
                "GROUP BY b.book_id " +
                "ORDER BY borrow_count DESC " +
                "LIMIT 5";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                reportData.put(rs.getString("title"), rs.getInt("borrow_count"));
            }
        } catch (SQLException e) {
            System.err.println("❌ 產生 Top 5 熱門書籍報表失敗");
            e.printStackTrace();
        }
        return reportData;
    }

    /**
     * ✨ 財務營運報表 3：統計全系統罰款營運狀況
     * 整合你的 Fine 模型，統計目前的財務數據。
     * * @return Map<指標名稱, 金額/數量>
     */
    public Map<String, Object> getLibraryFineSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        String sqlTotalFines = "SELECT COUNT(*) AS total_count, SUM(amount) AS total_amount FROM fines";
        String sqlUnpaidFines = "SELECT COUNT(*) AS unpaid_count, SUM(amount) AS unpaid_amount FROM fines WHERE is_paid = 0";

        try (Connection conn = DatabaseManager.getConnection()) {

            // 1. 統計歷史總罰單金額與數量
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlTotalFines)) {
                if (rs.next()) {
                    summary.put("歷史罰單總總數", rs.getInt("total_count"));
                    summary.put("歷史罰款總金額", rs.getInt("total_amount"));
                }
            }

            // 2. 統計未繳清的罰單金額（風控指標）
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sqlUnpaidFines)) {
                if (rs.next()) {
                    summary.put("未繳罰單總件數", rs.getInt("unpaid_count"));
                    summary.put("未繳罰款總金額", rs.getInt("unpaid_amount"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 產生罰款摘要報表失敗");
            e.printStackTrace();
        }
        return summary;
    }

    /**
     * ✨ 營運稽核 4：統計目前圖書館的「關鍵績效指標 (KPI)」
     * 秀在管理員首頁的資訊看板（Dashboard）。
     */
    public Map<String, Integer> getLibraryGeneralKPIs() {
        Map<String, Integer> kpis = new LinkedHashMap<>();

        String sqlBooks = "SELECT COUNT(*) FROM books";
        String sqlBorrowed = "SELECT COUNT(*) FROM books WHERE status = 'BORROWED'";
        String sqlSuspendedUsers = "SELECT COUNT(*) FROM users WHERE status = 'SUSPENDED'";

        try (Connection conn = DatabaseManager.getConnection()) {

            // 總藏書量
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlBooks)) {
                if (rs.next()) kpis.put("總藏書量", rs.getInt(1));
            }
            // 目前在外借出數量
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlBorrowed)) {
                if (rs.next()) kpis.put("目前借出冊數", rs.getInt(1));
            }
            // 目前被停權的學生總數
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSuspendedUsers)) {
                if (rs.next()) kpis.put("停權學生人數", rs.getInt(1));
            }

        } catch (SQLException e) {
            System.err.println("❌ 讀取圖書館 KPI 失敗");
            e.printStackTrace();
        }
        return kpis;
    }
}
UserService
package library.service;

import library.database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 以下為概念性 import，視你實際的 Model 與 Repository 調整
// import library.model.User;
// import library.repository.UserRepository;

public class UserService {

    // private final UserRepository userRepository;

    public UserService() {
        // this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心功能 1：使用者登入驗證 (為 Login 畫面量身打造)
     * * @param studentNo 學號（登入帳號）
     * @param password  密碼
     * @return 登入成功回傳包含使用者資訊的 Map（或 User 物件），失敗則回傳 null
     */
    public Map<String, Object> login(String studentNo, String password) {
        if (studentNo == null || password == null) return null;

        String sql = "SELECT * FROM users WHERE student_no = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentNo.trim());
            pstmt.setString(2, password); // 實務上密碼可做雜湊加密，Demo 專案用明文比對即可

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> userSession = new HashMap<>();
                    userSession.put("userId", rs.getInt("user_id"));
                    userSession.put("studentNo", rs.getString("student_no"));
                    userSession.put("name", rs.getString("name"));
                    userSession.put("roleLevel", rs.getString("role_level")); // NORMAL or VIP
                    userSession.put("status", rs.getString("status"));         // ACTIVE or SUSPENDED
                    return userSession;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 登入驗證查詢失敗");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 核心功能 2：學生帳號註冊 (Register 畫面使用)
     * 【簡報規格：欄位需包含學號、姓名、密碼，並可自由選擇 NORMAL 或 VIP】
     */
    public String registerUser(String studentNo, String name, String password, String roleLevel) {
        // 1️⃣ 防禦性驗證：檢查欄位是否留白
        if (studentNo == null || studentNo.trim().isEmpty() ||
                name == null || name.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            return "⚠️ 註冊失敗：所有欄位皆為必填！";
        }

        // 驗證角色等級格式是否正確
        if (!"NORMAL".equalsIgnoreCase(roleLevel) && !"VIP".equalsIgnoreCase(roleLevel)) {
            return "❌ 系統錯誤：不合法的會員等級。";
        }

        // 2️⃣ 檢查學號是否重複 (學號具有唯一性)
        String checkSql = "SELECT COUNT(*) FROM users WHERE student_no = ?";
        String insertSql = "INSERT INTO users (student_no, name, password, role_level, status) VALUES (?, ?, ?, ?, 'ACTIVE')";

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setString(1, studentNo.trim());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "⚠️ 註冊失敗：此學號（" + studentNo + "）已被註冊！";
                    }
                }
            }

            // 3️⃣ 執行寫入
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                insertPstmt.setString(1, studentNo.trim());
                insertPstmt.setString(2, name.trim());
                insertPstmt.setString(3, password);
                insertPstmt.setString(4, roleLevel.toUpperCase());
                insertPstmt.executeUpdate();
                return "SUCCESS";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "❌ 資料庫寫入錯誤，註冊未完成。";
        }
    }

    /**
     * ✨ 管理者功能 3：手動將學生「停權」或「復權」 (管理員後台一鍵切換)
     * 【簡報規格：管理員可手動停權/復權，被停權者無法借書】
     * * @param userId 使用者 ID
     * @param status 新的狀態 ("ACTIVE" 或 "SUSPENDED")
     */
    public boolean updateUserStatus(int userId, String status) {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"SUSPENDED".equalsIgnoreCase(status)) {
            return false;
        }

        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.toUpperCase());
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ 更新使用者狀態失敗，User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 管理者功能 4：撈取全校學生名單 (管理員後台表格數據源)
     * 支援透過學號進行關鍵字模糊過濾。
     */
    public List<Map<String, Object>> getAllStudents(String studentNoFilter) {
        List<Map<String, Object>> students = new ArrayList<>();

        String sql = "SELECT user_id, student_no, name, role_level, status FROM users";
        if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
            sql += " WHERE student_no LIKE ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
                pstmt.setString(1, "%" + studentNoFilter.trim() + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> student = new HashMap<>();
                    student.put("userId", rs.getInt("user_id"));
                    student.put("studentNo", rs.getString("student_no"));
                    student.put("name", rs.getString("name"));
                    student.put("roleLevel", rs.getString("role_level"));
                    student.put("status", rs.getString("status"));
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 獲取學生列表失敗");
            e.printStackTrace();
        }
        return students;
    }
}