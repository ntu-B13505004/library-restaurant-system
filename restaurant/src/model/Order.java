package model;

import enums.OrderStatus;
import enums.TableNumber;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Order {
    private int             orderId;
    private LocalDateTime   orderTime;
    private OrderStatus     status;
    private TableNumber     table;       // TAKEOUT 或 TABLE_1~TABLE_10
    private Member          member;      // 可為 null（非會員）
    private List<OrderDetail> details;
    private int             totalAmount; // 折扣前原價
    private int             finalAmount; // 折扣後實付（由 OrderService 計算後寫入）

    // 建構子
    public Order(int orderId, TableNumber table, Member member, List<OrderDetail> details) {
        this.orderId     = orderId;
        this.orderTime   = LocalDateTime.now();
        this.status      = OrderStatus.PENDING;  // 新訂單預設「未接單」
        this.table       = table;
        this.member      = member;
        this.details     = details;
        this.totalAmount = calculateTotal();
        this.finalAmount = totalAmount;          // 預設等於原價，折扣由 OrderService 覆寫
    }

    // 計算原始總金額（不含折扣）
    public int calculateTotal() {
        int sum = 0;
        for (OrderDetail d : details) {
            sum += d.getSubtotal();
        }
        return sum;
    }

    // Getters
    public int              getOrderId()     { return orderId; }
    public LocalDateTime    getOrderTime()   { return orderTime; }
    public OrderStatus      getStatus()      { return status; }
    public TableNumber      getTable()       { return table; }
    public Member           getMember()      { return member; }
    public List<OrderDetail> getDetails()   { return details; }
    public int              getTotalAmount() { return totalAmount; }
    public int              getFinalAmount() { return finalAmount; }

    // Setters
    public void setStatus(OrderStatus status)      { this.status = status; }
    public void setFinalAmount(int finalAmount)    { this.finalAmount = finalAmount; }

    @Override
    public String toString() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String memberInfo = (member != null) ? member.getName() + "（" + member.getTier().getDisplayName() + "）" : "非會員";
        String location   = table.isDineIn() ? table.getDisplayName() : "外帶";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("═══ 訂單 #%d ═══\n", orderId));
        sb.append(String.format("時間：%s\n", orderTime.format(fmt)));
        sb.append(String.format("狀態：%s\n", status.getDisplayName()));
        sb.append(String.format("位置：%s｜會員：%s\n", location, memberInfo));
        sb.append("明細：\n");
        for (OrderDetail d : details) {
            sb.append(d.toString()).append("\n");
        }
        sb.append(String.format("原價：$%d\n", totalAmount));
        if (finalAmount != totalAmount) {
            sb.append(String.format("實付：$%d\n", finalAmount));
        }
        return sb.toString();
    }
}