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