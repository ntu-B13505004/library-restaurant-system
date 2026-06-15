package library.repository;

import library.database.DatabaseManager;
import library.model.Book;
import library.model.BookStatus;
import library.model.BorrowRecord;

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
                "WHERE b.title LIKE ? OR b.authors LIKE ? OR b.subjects LIKE ? OR b.publisher LIKE ?OR i.isbn LIKE ?";

        String matchKey = "%" + keyword + "%";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, matchKey);
            pstmt.setString(2, matchKey);
            pstmt.setString(3, matchKey);
            pstmt.setString(4, matchKey);
            pstmt.setString(5, matchKey);

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

    /**
     * ✨ 管理者功能：安全上架新書（包含寫入一對多的 ISBN 表）
     */
    public boolean insert(Book book) {
        String insertBookSql = "INSERT INTO books (title, authors, subjects, publisher, publish_year, edition, format_desc, source, note, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String insertIsbnSql = "INSERT INTO book_isbns (book_id, isbn) VALUES (?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false); // 開啟交易

            // 1. 寫入書籍主表並取得自動生成的 book_id
            try (PreparedStatement pstmt = conn.prepareStatement(insertBookSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, book.getTitle());
                pstmt.setString(2, book.getAuthors());
                pstmt.setString(3, book.getSubjects());
                pstmt.setString(4, book.getPublisher());
                pstmt.setString(5, book.getPublishYear());
                pstmt.setString(6, book.getEdition());
                pstmt.setString(7, book.getFormatDesc());
                pstmt.setString(8, book.getSource());
                pstmt.setString(9, book.getNote());
                pstmt.setString(10, book.getStatus().name()); // AVAILABLE
                pstmt.executeUpdate();

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        book.setBookId(generatedKeys.getInt(1)); // 回填產生的 ID
                    } else {
                        throw new SQLException("建立書籍失敗，無法取得 Generated Key。");
                    }
                }
            }

            // 2. 寫入 ISBN 子表
            if (book.getIsbns() != null && !book.getIsbns().isEmpty()) {
                try (PreparedStatement pstmtIsbn = conn.prepareStatement(insertIsbnSql)) {
                    for (String isbn : book.getIsbns()) {
                        if (isbn != null && !isbn.trim().isEmpty()) {
                            pstmtIsbn.setInt(1, book.getBookId());
                            pstmtIsbn.setString(2, isbn.trim());
                            pstmtIsbn.addBatch();
                        }
                    }
                    pstmtIsbn.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    /**
     * ✨ 管理者功能：安全下架書籍（級聯刪除書籍與關聯的 ISBN）
     */
    public boolean delete(int bookId) {
        String deleteIsbnsSql = "DELETE FROM book_isbns WHERE book_id = ?";
        String deleteBookSql = "DELETE FROM books WHERE book_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseManager.getConnection();
            conn.setAutoCommit(false);

            // 1. 先刪除外鍵子表紀錄
            try (PreparedStatement pstmt = conn.prepareStatement(deleteIsbnsSql)) {
                pstmt.setInt(1, bookId);
                pstmt.executeUpdate();
            }

            // 2. 再刪除書籍主表紀錄
            try (PreparedStatement pstmt = conn.prepareStatement(deleteBookSql)) {
                pstmt.setInt(1, bookId);
                int affected = pstmt.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return false;
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }
}