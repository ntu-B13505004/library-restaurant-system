package library.service;

import library.model.Book;
import library.model.BookStatus;
import library.repository.BookRepository;

import java.util.List;

public class BookService {

    private final BookRepository bookRepository;

    public BookService() {
        this.bookRepository = new BookRepository();
    }

    /**
     * 1. 查詢所有書籍清單（用於表格呈現）
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
     * 3. ✨ 管理者功能：新書上架
     * 驗證必要欄位（書名），通過後透過 Repository 寫入資料庫
     */
    public boolean addBook(Book book) {
        if (book == null || book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            System.err.println("⚠️ 上架失敗：書籍標題不可為空。");
            return false;
        }
        return bookRepository.insert(book);
    }

    /**
     * 4. ✨ 管理者功能：書籍下架
     * 安全檢查：如果書籍目前是「借出狀態 (BORROWED)」，則拒絕刪除，維護資料庫完整性。
     */
    public boolean removeBook(int bookId) {
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            System.err.println("⚠️ 下架失敗：找不到該書籍 ID。");
            return false;
        }

        // 核心業務規則：被借出的書不能直接從系統中抹除
        if (book.getStatus() == BookStatus.BORROWED) {
            System.err.println("❌ 下架失敗：該書籍目前正被學生借閱中，無法執行下架。");
            return false;
        }

        return bookRepository.delete(bookId);
    }
}