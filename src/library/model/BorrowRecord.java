package library.model;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class BorrowRecord {
    private int recordId;
    private User user;
    private Book book;
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

    public boolean checkOverdue() {
        if (returnDate != null) {
            this.isOverdue = returnDate.isAfter(dueDate);
        } else {
            this.isOverdue = LocalDateTime.now().isAfter(dueDate);
        }
        return this.isOverdue;
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
}