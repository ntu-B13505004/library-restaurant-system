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
    /**
     * ✨ 動態計算最新應繳金額 (已修正時間與狀態判斷)
     */
    public int calculateCurrentAmount() {
        if (record == null || record.getDueDate() == null) return 0;

        // 1. 決定結算終點：如果已還書，算到歸還日；如果未還，算到今天
        LocalDateTime endPoint = (record.getReturnDate() != null)
                ? record.getReturnDate()
                : LocalDateTime.now();

        // 2. 判斷是否過期 (捨棄依賴 isOverdue 旗標，直接比對時間)
        if (endPoint.isBefore(record.getDueDate()) || endPoint.isEqual(record.getDueDate())) {
            return 0; // 沒過期，罰金 0
        }

        // 3. 為了避免未滿 24 小時算 0 天，我們將時間切換為 LocalDate (純日期) 來算天數差
        long overdueDays = ChronoUnit.DAYS.between(
                record.getDueDate().toLocalDate(),
                endPoint.toLocalDate()
        );

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
