package library.service;

import library.model.Book;
import library.model.BookStatus;
import library.model.BorrowRecord;
import library.model.Fine;
import library.repository.BookRepository;
// import library.repository.BorrowRecordRepository; // 需實作：用來操作 borrow_records 資料表
// import library.repository.UserRepository;         // 需實作：用來檢查學生 status 與 role_level

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BorrowService {

    private final BookRepository bookRepository;
    // private final BorrowRecordRepository borrowRepository;
    // private final UserRepository userRepository;

    public BorrowService() {
        this.bookRepository = new BookRepository();
        // this.borrowRepository = new BorrowRecordRepository();
        // this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心功能 1：辦理借書交易（包含全方位規則驗證）
     * * @param userId   借閱學生的用戶 ID (DB 的 user_id)
     * @param bookId   要借閱的書籍 ID
     * @param numDays  學生選擇的租借天數 (1, 3, 7, 14 天)
     * @return 借書結果狀態字串（"SUCCESS" 表示成功；其餘字串為失敗原因，可直接用於 GUI 彈窗提示）
     */
    public String borrowBook(int userId, int bookId, int numDays) {

        // 1️⃣ 驗證書籍狀態：書籍是否存在？是否為可借閱 (AVAILABLE)？
        Book book = bookRepository.findById(bookId);
        if (book == null) {
            return "❌ 系統錯誤：找不到該書籍資料。";
        }
        if (book.getStatus() != BookStatus.AVAILABLE) {
            return "⚠️ 借閱失敗：該書籍目前已被他人借出。";
        }

        // 2️⃣ 驗證使用者狀態（對接 UserRepository）
        /*
        User user = userRepository.findById(userId);
        if (user == null) {
            return "❌ 系統錯誤：找不到該使用者帳號。";
        }

        // 【簡報規格：停權懲罰】如果狀態為 SUSPENDED，拒絕借書
        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            return "❌ 權限受限：您的帳號目前處於「停權」狀態，請先繳清罰款或洽管理員。";
        }

        // 【簡報規格：等級差異】普通會員限制
        if ("NORMAL".equalsIgnoreCase(user.getRoleLevel()) && numDays > 7) {
            return "⚠️ 權限不足：普通會員單次借閱上限為 7 天，14 天天數僅限 VIP 借閱。";
        }

        // 【進階功能：借閱上限】例如限制每人最多同時借 5 本
        int currentBorrowedCount = borrowRepository.countActiveBorrowsByUserId(userId);
        if (currentBorrowedCount >= 5) {
            return "⚠️ 借閱失敗：您已達到最高借閱上限（5本），請先還書後再借。";
        }
        */

        // 3️⃣ 驗證全數通過，執行交易更新
        try {
            // A. 將書籍狀態在 DB 中改為 BORROWED
            bookRepository.updateStatus(bookId, BookStatus.BORROWED);

            // B. 計算時間並新增一筆借閱歷史紀錄
            LocalDateTime borrowDate = LocalDateTime.now();
            LocalDateTime dueDate = borrowDate.plusDays(numDays);

            // 呼叫 borrowRepository 寫入一筆新的紀錄到資料庫
            // borrowRepository.save(userId, bookId, borrowDate, dueDate, numDays);

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 資料庫操作失敗，借書交易未完成。";
        }
    }

    /**
     * ✨ 核心功能 2：辦理還書交易（自動整合你的 Fine 罰款模型）
     * * @param bookId   要歸還的書籍 ID
     * @return 還書結果字串。若逾期會包含罰金提示（如 "SUCCESS_WITH_FINE:30"）
     */
    public String returnBook(int bookId) {
        try {
            // 1️⃣ 找出這本書「目前尚未歸還」的那筆借閱紀錄
            // BorrowRecord activeRecord = borrowRepository.findActiveRecordByBookId(bookId);
            // if (activeRecord == null) {
            //     return "❌ 系統錯誤：此書籍在紀錄中並未被借出。";
            // }

            // 2️⃣ 更新書籍狀態為 AVAILABLE
            bookRepository.updateStatus(bookId, BookStatus.AVAILABLE);

            // 3️⃣ 更新借閱紀錄的歸還時間 (return_date 改為目前時間)
            LocalDateTime returnDate = LocalDateTime.now();
            // borrowRepository.updateReturnDate(activeRecord.getRecordId(), returnDate);

            // 4️⃣ 💡 動態整合你的 Fine 模型檢查逾期
            // activeRecord.setReturnDate(returnDate); // 讓 record 知道歸還時間以便 Fine 計算
            // if (activeRecord.isOverdue()) {
            //     Fine fine = new Fine(0, activeRecord);
            //     int fineAmount = fine.getAmount();
            //
            //     // 可在此將罰款紀錄同步寫入資料庫的 fines 表
            //     // fineRepository.save(fine);
            //
            //     return "SUCCESS_WITH_FINE:" + fineAmount;
            // }

            return "SUCCESS";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 還書失敗，資料庫連線異常。";
        }
    }

    /**
     * ✨ 核心功能 3：查詢特定學生的目前借閱與歷史總紀錄（學生端畫面使用）
     */
    public List<BorrowRecord> getUserBorrowHistory(int userId) {
        // return borrowRepository.findAllByUserId(userId);
        return new ArrayList<>(); // 暫代 stub
    }

    /**
     * ✨ 核心功能 4：管理者全域審查（管理員後台：依學號或全部查詢流水帳）
     */
    public List<BorrowRecord> getAllBorrowRecords(String studentNoFilter) {
        /*
        if (studentNoFilter == null || studentNoFilter.trim().isEmpty()) {
            return borrowRepository.findAll();
        } else {
            return borrowRepository.findAllByStudentNo(studentNoFilter.trim());
        }
        */
        return new ArrayList<>(); // 暫代 stub
    }

    /**
     * ✨ 進階加分功能 5：獲取特定學生的未繳罰款總額與清單
     */
    public int getTotalUnpaidFines(int userId) {
        int total = 0;
        // 1. 從資料庫撈出該用戶所有「未還且已逾期」或「已還但尚未繳罰款」的紀錄
        // List<Fine> unpaidFines = fineRepository.findUnpaidByUserId(userId);
        // for (Fine f : unpaidFines) {
        //     total += f.getAmount(); // 呼叫你的 fine.getAmount() 自動動態累加最新金額
        // }
        return total;
    }

    /**
     * ✨ 進階加分功能 6：繳清罰款
     */
    public boolean payFine(int fineId) {
        try {
            // 1. 撈出該筆罰款，執行支付
            // Fine fine = fineRepository.findById(fineId);
            // if (fine != null) {
            //     fine.pay(); // 鎖定最終金額並標記 isPaid = true
            //     fineRepository.update(fine); // 更新資料庫
            //     return true;
            // }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}