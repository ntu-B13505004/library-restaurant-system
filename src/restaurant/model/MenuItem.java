package model;

public class MenuItem {
    private int itemId;
    private String itemName;
    private int price;
    private String category;
    private boolean isSoldOut;
    private boolean isDeleted = false;  // "刪除"菜單用

    // "刪除"菜單用，因為是原本沒有的所以放在這邊
    // 但只是標記這個菜"被刪掉了" 不會真的刪除
    public void setDeleted(boolean deleted) { this.isDeleted = deleted; }  // setter
    public boolean isDeleted() { return isDeleted; }  // getter

    // 建構子
    public MenuItem(int itemId, String itemName, int price, String category) {
        this.itemId    = itemId;
        this.itemName  = itemName;
        this.price     = price;
        this.category  = category;
        this.isSoldOut = false; // 新增餐點預設為供應中
    }

    // Getters
    public int     getItemId()    { return itemId; }
    public String  getItemName()  { return itemName; }
    public int     getPrice()     { return price; }
    public String  getCategory()  { return category; }
    public boolean isSoldOut()    { return isSoldOut; }

    // Setters（只可以修改價格跟售完）
    public void setPrice(int price)          { this.price = price; }
    public void setSoldOut(boolean soldOut)  { this.isSoldOut = soldOut; }
    //不確定要不要加可改名字

    

    @Override
    public String toString() {
        String status = isSoldOut ? "【售完】" : "";
        return String.format("[%d] %s %s - $%d (%s)", itemId, status, itemName, price, category);
    }
}