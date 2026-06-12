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
}