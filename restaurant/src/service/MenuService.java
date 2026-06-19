package service;

import model.MenuItem;
import java.util.ArrayList;
import java.util.List;

public class MenuService {
    private List<MenuItem> menu = new ArrayList<>();
    private int nextItemId = 1; // 自動遞增 ID

    // 新增餐點
    public MenuItem addMenuItem(String itemName, int price, String category) {
        MenuItem item = new MenuItem(nextItemId++, itemName, price, category);
        menu.add(item);
        System.out.println("✅ 已新增餐點：" + item);
        return item;
    }

    // 刪除餐點（軟刪除）
    public boolean removeMenuItem(int itemId) {
        MenuItem item = findById(itemId);
        if (item == null) {
            System.out.println("❌ 找不到餐點 ID：" + itemId);
            return false;
        }
        item.setDeleted(true);
        System.out.println("✅ 已刪除餐點：" + item.getItemName());
        return true;
    }

    // 修改價格
    public boolean updatePrice(int itemId, int newPrice) {
        MenuItem item = findById(itemId);
        if (item == null) {
            System.out.println("❌ 找不到餐點 ID：" + itemId);
            return false;
        }
        int oldPrice = item.getPrice();
        item.setPrice(newPrice);
        System.out.println("✅ 已更新 " + item.getItemName() + " 價格：$" + oldPrice + " → $" + newPrice);
        return true;
    }

    // 設定售完 / 恢復供應
    public boolean setSoldOut(int itemId, boolean soldOut) {
        MenuItem item = findById(itemId);
        if (item == null) {
            System.out.println("❌ 找不到餐點 ID：" + itemId);
            return false;
        }
        item.setSoldOut(soldOut);
        String status = soldOut ? "售完" : "供應中";
        System.out.println("✅ 已設定 " + item.getItemName() + " 為【" + status + "】");
        return true;
    }

    // 取得可供應的餐點（未售完、未刪除）
    public List<MenuItem> getAvailableItems() {
        List<MenuItem> available = new ArrayList<>();
        for (MenuItem item : menu) {
            if (!item.isDeleted() && !item.isSoldOut()) {
                available.add(item);
            }
        }
        return available;
    }

    // 取得完整菜單（含售完，不含已刪除）—— 給後台查看用
    public List<MenuItem> getAllItems() {
        List<MenuItem> all = new ArrayList<>();
        for (MenuItem item : menu) {
            if (!item.isDeleted()) {
                all.add(item);
            }
        }
        return all;
    }

    // 內部用：用 ID 找餐點（含已刪除，方便管理）
    public MenuItem findById(int itemId) {
        for (MenuItem item : menu) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    // CSV 載入用：直接加入已還原的 MenuItem 物件（不走 addMenuItem 流程）
    public void loadMenuItem(MenuItem item) {
        menu.add(item);
    }

    // CSV 載入用：還原正確的下一個 id
    public void setNextItemId(int id) {
        this.nextItemId = id;
    }

    // 取得所有餐點（含已刪除，給 CSV 存檔用）
    public List<MenuItem> getAllItemsIncludeDeleted() {
        return menu;
    }

    // 印出菜單
    public void printMenu() {
        List<MenuItem> all = getAllItems();
        if (all.isEmpty()) {
            System.out.println("（菜單目前沒有餐點）");
            return;
        }
        System.out.println("══════ 菜單 ══════");
        for (MenuItem item : all) {
            System.out.println(item);
        }
        System.out.println("══════════════════");
    }
}