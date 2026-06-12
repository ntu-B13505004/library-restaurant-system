package model;

//NOTE. 這裡的subtotal是小計，還沒算優惠
public class OrderDetail {
    private int      detailId;
    private MenuItem item;
    private int      quantity;
    private String   note;
    private int      subtotal;  // price × quantity，建立時自動計算

    // 建構子
    public OrderDetail(int detailId, MenuItem item, int quantity, String note) {
        this.detailId = detailId;
        this.item     = item;
        this.quantity = quantity;
        this.note     = note;
        this.subtotal = item.getPrice() * quantity; // 自動計算小計
    }

    // Getters
    public int      getDetailId() { return detailId; }
    public MenuItem getItem()     { return item; }
    public int      getQuantity() { return quantity; }
    public String   getNote()     { return note; }
    public int      getSubtotal() { return subtotal; }

    @Override
    public String toString() {
        String noteStr = (note != null && !note.isEmpty()) ? "（備註：" + note + "）" : "";
        return String.format("  - %s x%d %s → $%d",
                item.getItemName(), quantity, noteStr, subtotal);
    }
}