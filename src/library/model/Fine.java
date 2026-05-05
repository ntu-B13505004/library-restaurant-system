public class Fine {
    private int fineId;
    private BorrowRecord record;
    private int amount;
    private boolean isPaid;

    private static final int DAILY_FINE = 10;       //暫定每天10塊

    public Fine(int fineId, BorrowRecord record) {
        this.fineId = fineId;
        this.record = record;
        this.isPaid = false;
        this.amount = calculateAmount();
    }

    public int calculateAmount() {
        if (!record.isOverdue()) return 0;

        long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(
            record.getDueDate(),
            record.getReturnDate() != null ? record.getReturnDate() : java.time.LocalDateTime.now()
        );
        return (int) overdueDays * DAILY_FINE;
    }

    public void pay() {
        this.isPaid = true;
    }

    // Getters
    public int getFineId() { return fineId; }
    public BorrowRecord getRecord() { return record; }
    public int getAmount() { return amount; }
    public boolean isPaid() { return isPaid; }
}