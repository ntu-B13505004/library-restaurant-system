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
├── OrderStatus.java          訂單狀態
├── TableNumber.java          桌號／外帶
└── Main.java                 程式進入點
```

---

## 系統架構

```
客人點餐介面 (OrderingFrame)
        │
        ▼
   OrderService ──► MemberService（消費後自動升級）
        │
        ▼
後台管理介面 (AdminFrame)
   ├── OrderPanel      查看訂單、更新狀態
   ├── MenuManagePanel 新增／刪除／改價／售完
   ├── MemberPanel     查看所有會員
   └── ReportPanel     今日營業額、熱銷排行
```

---

## 功能說明

### 客人點餐（OrderingFrame）

| 功能 | 說明 |
|------|------|
| 菜單卡片 | 顯示所有供應中的餐點，點擊即可加入購物車 |
| 購物車 | 可直接在表格內修改數量與備註，小計自動更新 |
| 選擇桌號 | 從 1～10 號桌或外帶中選擇 |
| 會員查詢 | 輸入電話查詢，查無資料可當場註冊 |
| 結帳 | 自動套用會員折扣，送出後清空購物車 |

### 後台管理（AdminFrame）

| 分頁 | 功能 |
|------|------|
| 訂單管理 | 查看所有訂單，選取後可更新為「製作中」或「已完成」 |
| 菜單管理 | 新增餐點、修改價格、設定售完／恢復供應、刪除餐點 |
| 會員管理 | 查看所有會員的等級與累積消費 |
| 今日報表 | 計算今日營業額（已完成訂單）與熱銷排行 |

---

## 會員制度

### 等級與折扣

| 等級 | 升級門檻（累積消費） | 一般折扣 | 生日當月 |
|------|-------------------|---------|---------|
| 銅卡 | $0 起 | 95折 | 無額外優惠 |
| 銀卡 | $500 起 | 9折 | 再疊加 95折 → 85.5折 |
| 金卡 | $1000 起 | 85折 | 再疊加 95折 → 80.75折 |

### 規則說明
- 新會員加入即為銅卡，等級依**累積消費總金額**自動升級
- 生日優惠限銀卡、金卡，以**生日月份**判定
- 折扣計算後以 `Math.round()` 四捨五入至整數

---

## 資料類別說明

### `MenuItem`（餐點）
| 欄位 | 型別 | 說明 |
|------|------|------|
| itemId | int | 自動產生，不可修改 |
| itemName | String | 餐點名稱 |
| price | int | 價格 |
| category | String | 分類（例：主餐、飲料） |
| isSoldOut | boolean | 售完狀態，預設 false |
| isDeleted | boolean | 軟刪除，預設 false |

### `Member`（會員）
| 欄位 | 型別 | 說明 |
|------|------|------|
| memberId | int | 自動產生 |
| name | String | 姓名 |
| phone | String | 電話（不可重複） |
| birthMonth | int | 生日月份 1～12 |
| birthDate | int | 生日日期 1～31 |
| totalSpent | int | 累積消費金額 |
| tier | MemberTier | 等級，自動升級 |

### `Order`（訂單）
| 欄位 | 型別 | 說明 |
|------|------|------|
| orderId | int | 自動產生 |
| orderTime | LocalDateTime | 建立時自動記錄 |
| status | OrderStatus | 預設 PENDING |
| table | TableNumber | 桌號或外帶 |
| member | Member | 可為 null（非會員） |
| details | List\<OrderDetail\> | 訂單內所有品項 |
| totalAmount | int | 原價（自動計算） |
| finalAmount | int | 實付金額（含折扣） |

---

## 啟動方式

### 初始化（Main.java）

```java
MenuService   menuService   = new MenuService();
MemberService memberService = new MemberService();
OrderService  orderService  = new OrderService();
ReportService reportService = new ReportService(orderService);

// 啟動點餐主視窗
new OrderingFrame(menuService, memberService, orderService, reportService);
```

> ⚠️ `ReportService` 需傳入 `orderService` 才能讀取訂單資料。

---

## 注意事項

- **軟刪除**：餐點刪除後資料仍保留，不影響歷史訂單的顯示
- **報表只計算已完成訂單**：`OrderStatus.COMPLETED` 的訂單才納入營業額與熱銷排行
- **會員升級自動觸發**：每次結帳後呼叫 `memberService.updateTotalSpent()`，內部自動判斷是否升級