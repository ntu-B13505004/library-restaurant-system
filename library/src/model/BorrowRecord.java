package library.src.model;
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
        this.borrowDays = borrowDays;
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

