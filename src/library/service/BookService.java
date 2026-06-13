package library.service;

import library.model.Book;
import library.model.BookStatus;
import library.model.BorrowRecord;
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