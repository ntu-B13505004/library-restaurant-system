# 餐廳點餐管理系統

> 第10組｜工海二 張馨予 陳釗熒 劉如恩  
> 使用語言：Java｜介面：Java Swing

---

## 專案簡介

以物件導向設計（OOP）實作的餐廳點餐管理系統，支援客人點餐與後台管理兩大功能，並整合會員折扣制度與消費報表。

---

## 專案結構

```
restaurant/
├── src/
│   ├── enums/
│   │   ├── MemberTier.java       會員等級與折扣規則
│   │   ├── OrderStatus.java      訂單狀態
│   │   └── TableNumber.java      桌號與外帶
│   ├── model/
│   │   ├── MenuItem.java         餐點資料
│   │   ├── Member.java           會員資料
│   │   ├── OrderDetail.java      訂單明細（單項餐點）
│   │   └── Order.java            訂單
│   ├── service/
│   │   ├── MenuService.java      菜單管理邏輯
│   │   ├── MemberService.java    會員管理邏輯
│   │   ├── OrderService.java     訂單管理邏輯
│   │   └── ReportService.java    報表邏輯
│   ├── ui/
│   │   ├── OrderingFrame.java    客人點餐視窗（主視窗）
│   │   ├── AdminFrame.java       後台管理視窗
│   │   ├── OrderPanel.java       後台－訂單管理分頁
│   │   ├── MenuManagePanel.java  後台－菜單管理分頁
│   │   ├── MemberPanel.java      後台－會員列表分頁
│   │   └── ReportPanel.java      後台－今日報表分頁
│   └── persistence/
│       ├── CsvMemberRepository.java    會員CSV讀取儲存
│       └── CsvMenuRepository.java      菜單CSV讀取儲存
├── member.csv                    會員資料
├── menu.csv                      菜單資料
└── Main.java                     程式進入點
```

---

## 系統架構

```
啟動
 │
 ├── CSV 載入（menu.csv / member.csv）
 │
 ▼
OrderingFrame（客人點餐主視窗）
 ├── 菜單卡片（點擊加入購物車）
 ├── 購物車（可編輯數量與備註）
 ├── 選桌號 / 查詢或註冊會員
 ├── 結帳（自動套用折扣、更新累積消費）
 └── ⚙ 後台管理 ──► AdminFrame
                      ├── 訂單管理
                      ├── 菜單管理
                      ├── 會員列表
                      └── 今日報表
 │
關閉視窗
 │
 └── CSV 自動存檔（menu.csv / member.csv）
```

---

## 功能說明

### 客人點餐（OrderingFrame）

| 功能 | 說明 |
|------|------|
| 菜單卡片 | 顯示所有供應中的餐點，點擊輸入數量後加入購物車 |
| 購物車 | 可直接在表格內修改數量與備註，小計自動更新 |
| 重新整理菜單 | 手動刷新菜單卡片，同步後台對菜單的修改 |
| 選擇桌號 | 從 1～10 號桌或外帶中選擇 |
| 會員查詢 | 輸入電話查詢，查無資料可當場註冊 |
| 結帳 | 自動套用會員折扣，顯示原價與實付，送出後清空購物車 |

### 後台管理（AdminFrame）

| 分頁 | 功能 |
|------|------|
| 訂單管理 | 查看所有訂單，選取後可更新為「製作中」或「已完成」 |
| 菜單管理 | 新增餐點、修改價格、設定售完／恢復供應、刪除餐點 |
| 會員管理 | 查看所有會員的等級、生日與累積消費 |
| 今日報表 | 計算今日營業額（已完成訂單）與熱銷排行 |

---

## 會員制度

### 等級與折扣

| 等級 | 升級門檻（累積消費） | 一般折扣 | 生日當月 |
|------|-------------------|---------|---------|
| 銅卡 | $0 起 | 95折 | 無額外優惠 |
| 銀卡 | $500 起 | 9折 | 再疊加 95折 → **85.5折** |
| 金卡 | $1000 起 | 85折 | 再疊加 95折 → **80.75折** |

### 規則
- 新會員加入即為銅卡，依**累積消費總金額**自動升級，無法降級
- 生日優惠限銀卡、金卡，以**生日月份**判定
- 折扣計算後以 `Math.round()` 四捨五入至整數元

---

## 資料儲存（CSV）

程式啟動時自動從 CSV 載入資料，關閉視窗時自動存檔，不需手動操作。

### `menu.csv` 欄位順序
```
itemId, itemName, price, category, isSoldOut, isDeleted
```

### `member.csv` 欄位順序
```
memberId, name, phone, birthMonth, birthDate, totalSpent, tier
```

> ⚠️ 不建議手動編輯 CSV 檔案，格式錯誤會導致載入失敗。

---

## 啟動方式

### 環境需求
- Java 17 以上

### 編譯與執行
```bash
# 編譯（在 src/ 目錄下）
javac -d out enums/*.java model/*.java service/*.java persistence/*.java ui/*.java Main.java

# 執行
java -cp out Main
```

### Main.java 初始化流程
```java
// 1. 建立 Service
MenuService   menuService   = new MenuService();
MemberService memberService = new MemberService();
OrderService  orderService  = new OrderService();
ReportService reportService = new ReportService(orderService);

// 2. 載入 CSV
CsvMenuRepository   menuRepo   = new CsvMenuRepository();
CsvMemberRepository memberRepo = new CsvMemberRepository();
menuRepo.load(menuService);
memberRepo.load(memberService);

// 3. 關閉時自動存檔
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    menuRepo.save(menuService.getAllItemsIncludeDeleted());
    memberRepo.save(memberService.getAllMembers());
}));

// 4. 啟動介面
SwingUtilities.invokeLater(() ->
    new OrderingFrame(menuService, memberService, orderService, reportService)
);
```

---

## 注意事項

- **軟刪除**：餐點刪除後資料仍保留於 CSV，不影響歷史訂單顯示
- **報表只計算已完成訂單**：需將訂單狀態更新至 `COMPLETED` 才會納入營業額與熱銷排行
- **訂單不持久化**：訂單資料僅存於記憶體，重新啟動程式後會清空
- **前台菜單需手動刷新**：後台修改菜單後，點擊前台「重新整理菜單」按鈕才會同步
