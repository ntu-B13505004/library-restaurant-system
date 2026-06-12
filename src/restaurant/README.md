# 餐廳點餐管理系統 README
---

## 📁 專案結構

```
restaurant/
└── src/
    ├── enums/
    │   ├── OrderStatus.java     # 訂單狀態
    │   ├── TableNumber.java     # 桌號／外帶
    │   └── MemberTier.java      # 會員等級與折扣
    ├── model/
    │   ├── MenuItem.java        # 餐點資料
    │   ├── Member.java          # 會員資料
    │   ├── OrderDetail.java     # 訂單明細（單項餐點）
    │   └── Order.java           # 訂單
    ├── service/
    │   ├── MenuService.java     # 菜單管理
    │   ├── MemberService.java   # 會員管理
    │   ├── OrderService.java    # 訂單管理
    │   └── ReportService.java   # 報表
    └── Main.java                # ⬅ 介面在這裡寫
```

---

## 🗂️ Enum 說明

### `OrderStatus`（訂單狀態）
| 常數 | 顯示名稱 |
|------|---------|
| `PENDING` | 未接單 |
| `PREPARING` | 製作中 |
| `COMPLETED` | 已完成 |

```java
order.getStatus().getDisplayName(); // 取得中文名稱
```

### `TableNumber`（桌號）
| 常數 | 說明 |
|------|------|
| `TABLE_1` ~ `TABLE_10` | 內用 1～10 號桌 |
| `TAKEOUT` | 外帶 |

```java
table.getDisplayName(); // "3號桌" 或 "外帶"
table.isDineIn();       // true = 內用, false = 外帶
```

### `MemberTier`（會員等級）
| 等級 | 升級門檻（累積消費） | 一般折扣 | 生日當月 |
|------|-------------------|---------|---------|
| `BRONZE` 銅卡 | $0 起 | 95折 | 無額外優惠 |
| `SILVER` 銀卡 | $500 起 | 9折 | 再疊加 95折 → **85.5折** |
| `GOLD` 金卡 | $1000 起 | 85折 | 再疊加 95折 → **80.75折** |

---

## 📦 Model 說明

### `MenuItem`（餐點）
| 欄位 | 型別 | 說明 |
|------|------|------|
| `itemId` | int | 自動產生，不可修改 |
| `itemName` | String | 餐點名稱，不可修改 |
| `price` | int | 價格，可透過 `MenuService.updatePrice()` 修改 |
| `category` | String | 分類（例：主餐、飲料） |
| `isSoldOut` | boolean | 售完狀態，預設 false |
| `isDeleted` | boolean | 軟刪除，預設 false |

### `Member`（會員）
| 欄位 | 型別 | 說明 |
|------|------|------|
| `memberId` | int | 自動產生 |
| `name` | String | 姓名 |
| `phone` | String | 電話（不可重複） |
| `birthMonth` | int | 生日月份 1～12 |
| `totalSpent` | int | 累積消費金額 |
| `tier` | MemberTier | 等級，根據 totalSpent 自動升級 |

```java
member.isBirthdayMonth(currentMonth); // 判斷是否為生日月
```

### `OrderDetail`（訂單明細）
| 欄位 | 型別 | 說明 |
|------|------|------|
| `detailId` | int | 自動產生 |
| `item` | MenuItem | 餐點物件 |
| `quantity` | int | 數量 |
| `note` | String | 備註（可為空字串） |
| `subtotal` | int | 小計，建立時自動計算（price × quantity） |

### `Order`（訂單）
| 欄位 | 型別 | 說明 |
|------|------|------|
| `orderId` | int | 自動產生 |
| `orderTime` | LocalDateTime | 建立時自動記錄 |
| `status` | OrderStatus | 預設 PENDING |
| `table` | TableNumber | 桌號或外帶 |
| `member` | Member | 可為 null（非會員） |
| `details` | List\<OrderDetail\> | 訂單內所有品項 |
| `totalAmount` | int | 原價（自動計算） |
| `finalAmount` | int | 實付金額（含折扣，由 OrderService 寫入） |

---

## ⚙️ Service 說明

### `MenuService`（菜單管理）

```java
MenuService menuService = new MenuService();

menuService.addMenuItem("牛肉麵", 120, "主餐");   // 新增餐點，回傳 MenuItem
menuService.removeMenuItem(1);                    // 軟刪除
menuService.updatePrice(1, 130);                  // 修改價格
menuService.setSoldOut(1, true);                  // 設定售完
menuService.setSoldOut(1, false);                 // 恢復供應

menuService.getAvailableItems();  // 前台用：只回傳未售完、未刪除的餐點
menuService.getAllItems();         // 後台用：含售完，不含已刪除
menuService.findById(1);          // 用 ID 查餐點
menuService.printMenu();          // 印出完整菜單
```

### `MemberService`（會員管理）

```java
MemberService memberService = new MemberService();

memberService.registerMember("小明", "0912345678", 3);  // 姓名、電話、生日月份
memberService.findMemberByPhone("0912345678");           // 查詢，找不到回傳 null
memberService.updateTotalSpent(member, 500);             // 累加消費，自動觸發升級判斷
memberService.getAllMembers();                            // 取得所有會員
memberService.printAllMembers();                         // 印出會員列表
```

### `OrderService`（訂單管理）

```java
OrderService orderService = new OrderService();

// Step 1：建立明細
OrderDetail d1 = orderService.createDetail(menuItem, 2, "不要辣");
OrderDetail d2 = orderService.createDetail(menuItem2, 1, "");

// Step 2：打包成 List
List<OrderDetail> details = new ArrayList<>();
details.add(d1);
details.add(d2);

// Step 3：建立訂單（member 可傳 null = 非會員）
Order order = orderService.createOrder(TableNumber.TABLE_3, member, details);

// 更新狀態
orderService.updateOrderStatus(1, OrderStatus.PREPARING);
orderService.updateOrderStatus(1, OrderStatus.COMPLETED);

orderService.getAllOrders();                       // 所有訂單
orderService.getOrdersByStatus(OrderStatus.PENDING); // 篩選狀態
orderService.findById(1);                          // 用 ID 查訂單
```

> ⚠️ **注意**：訂單建立後 `OrderService` 會自動計算折扣並寫入 `finalAmount`，不需要手動呼叫折扣計算。

### `ReportService`（報表）

```java
// 初始化時需傳入 orderService
ReportService reportService = new ReportService(orderService);

reportService.calculateTodayRevenue();             // 今日營業額
reportService.calculateRevenue(LocalDate.of(2025, 6, 1)); // 指定日期營業額
reportService.showBestSellingItems();              // 熱銷排行（只算已完成訂單）
```

---

## 🖥️ Main.java 初始化方式

```java
import service.*;

public class Main {
    public static void main(String[] args) {
        MenuService   menuService   = new MenuService();
        MemberService memberService = new MemberService();
        OrderService  orderService  = new OrderService();
        ReportService reportService = new ReportService(orderService); // 注意需傳入 orderService

        // 介面從這裡開始寫
    }
}
```

---

## 📋 前端介面需要實作的功能

### 客人點餐介面
1. **選擇桌號** — 從 `TableNumber` 列舉中選，`TABLE_1`～`TABLE_10` 或 `TAKEOUT`
2. **查看菜單** — 呼叫 `menuService.getAvailableItems()`
3. **加入購物車** — 每個品項呼叫 `orderService.createDetail(item, qty, note)`，加入 `List<OrderDetail>`
4. **選擇是否使用會員** — 呼叫 `memberService.findMemberByPhone(phone)`，找不到則傳 `null`
5. **送出訂單** — 呼叫 `orderService.createOrder(table, member, details)`

### 後台管理介面
1. **查看訂單** — `orderService.getAllOrders()` 或 `getOrdersByStatus()`
2. **更新訂單狀態** — `orderService.updateOrderStatus(orderId, newStatus)`
3. **新增／刪除／修改餐點** — `MenuService` 各方法
4. **設定售完** — `menuService.setSoldOut(itemId, true/false)`
5. **查看今日營業額** — `reportService.calculateTodayRevenue()`
6. **熱銷排行** — `reportService.showBestSellingItems()`
7. **查看會員** — `memberService.getAllMembers()`
