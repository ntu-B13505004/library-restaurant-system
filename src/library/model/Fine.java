package library.model;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Fine {
    private int fineId;
    private BorrowRecord record; // 關聯的借閱紀錄
    private int paidAmount;          // 若已結清，記錄當時繳納的金額；若未結清，則作為快照緩存
    private boolean isPaid;

    public static final int DAILY_FINE = 10;

    /**
     * 從資料庫撈出已存在罰款紀錄時使用的建構子
     */
    public Fine(int fineId, BorrowRecord record, int paidAmount, boolean isPaid) {
        this.fineId = fineId;
        this.record = record;
        this.paidAmount = paidAmount;
        this.isPaid = isPaid;
    }

    /**
     * 新生成罰款紀錄時使用的建構子（預設未繳清）
     */
    public Fine(int fineId, BorrowRecord record) {
        this.fineId = fineId;
        this.record = record;
        this.isPaid = false;
        this.paidAmount = 0;
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
            return calculateCurrentAmount();
        }

        return paidAmount;
    }

    /**
     * 執行繳款
     */
    public void pay() {
        this.paidAmount = calculateCurrentAmount();
        this.isPaid = true;
    }
    // 其他 Getters
    public int getFineId() { return fineId; }
    public BorrowRecord getRecord() { return record; }
    public boolean isPaid() { return isPaid; }
}