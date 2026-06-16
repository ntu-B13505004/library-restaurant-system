package library.src.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import library.src.model.*;
import library.src.service.*;

import java.util.List;
import java.util.Map;

public class UserDashboardView {
    private final Stage stage;
    private final int userId;
    private String userName;

    private final BookService bookService = new BookService();
    private final BorrowService borrowService = new BorrowService();
    private final FineService fineService = new FineService();
    private final UserService userService = new UserService();

    private final BorderPane mainLayout = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final Label nameBadge = new Label();
    private final Label roleBadge = new Label();

    // 借閱歷史（所有紀錄，含已還）
    private final ObservableList<BorrowRecord> borrowHistoryData = FXCollections.observableArrayList();
    // 未繳罰款
    private final ObservableList<Fine> finesData = FXCollections.observableArrayList();
    // 書籍搜尋結果
    private final ObservableList<Book> searchBooksData = FXCollections.observableArrayList();

    private TableView<BorrowRecord> borrowTable;
    private TableView<Fine> fineTable;
    private TableView<Book> searchTable;
    private Label totalFineLabel;

    // 控制到期提醒彈窗只在初次載入或手動切換時觸發一次，避免頻繁打擾
    private boolean hasShownDueReminder = false;

    public UserDashboardView(Stage stage, Map<String, Object> session) {
        this.stage = stage;
        this.userId = (int) session.get("userId");
        this.userName = (String) session.get("name");
    }

    public void show() {
        mainLayout.setStyle("-fx-background-color: " + AppStyle.BG_WINDOW + ";");

        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setStyle("-fx-background-color: " + AppStyle.PRIMARY + ";");

        nameBadge.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameBadge.setStyle("-fx-text-fill: white;");
        roleBadge.setStyle("-fx-text-fill: " + AppStyle.ACCENT_LIGHT + "; -fx-font-size: 13px;");

        VBox profileBox = new VBox(5, nameBadge, roleBadge);
        profileBox.setPadding(new Insets(0, 0, 20, 0));

        Button navSearchBtn = new Button("📖 圖書檢索與借閱");
        Button navBorrowBtn = new Button("💳 我的借還與罰款");
        Button logoutBtn = new Button("🚪 登出系統");

        for (Button btn : new Button[]{navSearchBtn, navBorrowBtn, logoutBtn}) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(40);
            btn.setAlignment(Pos.CENTER_LEFT);
            if (btn == logoutBtn) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ECF0F1; -fx-font-size: 14px; -fx-cursor: hand;");
            }
        }

        Region sidebarSpacer = new Region();
        VBox.setVgrow(sidebarSpacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(profileBox, new Separator(), navSearchBtn, navBorrowBtn, sidebarSpacer, logoutBtn);

        navSearchBtn.setOnAction(e -> {
            switchPage(buildBookSearchPage());
            handleAsyncSearch("");
        });
        navBorrowBtn.setOnAction(e -> {
            switchPage(buildBorrowAndFinePage());
            asyncRefreshUserData(); // ✅ 切換頁面時主動刷新
        });
        logoutBtn.setOnAction(e -> new LoginView(stage).show());

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);

        switchPage(buildBorrowAndFinePage());
        asyncRefreshUserData();

        Scene scene = new Scene(mainLayout, 1000, 650);
        stage.setScene(scene);
        stage.setTitle("圖書館-學生個人主頁");
        stage.show();
    }

    private void switchPage(Pane page) {
        contentArea.getChildren().setAll(page);
    }

    // ==========================================
    // 💳 我的借還與罰款
    // ==========================================
    private Pane buildBorrowAndFinePage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        // ---------- 借閱歷史 ----------
        Label borrowHeading = new Label("📚 借閱歷史紀錄");
        borrowHeading.setFont(Font.font("System", FontWeight.BOLD, 18));

        borrowTable = new TableView<>(borrowHistoryData);
        borrowTable.setPrefHeight(230);
        borrowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BorrowRecord, String> titleCol = new TableColumn<>("書名");
        titleCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getBook().getTitle()));

        TableColumn<BorrowRecord, String> dateCol = new TableColumn<>("借閱日期");
        dateCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getBorrowDate().toString().substring(0, 10)));
        dateCol.setMaxWidth(110);

        TableColumn<BorrowRecord, String> dueCol = new TableColumn<>("應還期限");
        dueCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getDueDate().toString().substring(0, 10)));
        dueCol.setMaxWidth(110);

        // ✅ 新增「歸還狀態」欄位，顯示已還/未還/逾期未還
        TableColumn<BorrowRecord, String> returnStatusCol = new TableColumn<>("歸還狀態");
        returnStatusCol.setCellValueFactory(cell -> {
            BorrowRecord r = cell.getValue();
            String status;
            if (r.getReturnDate() != null) {
                status = r.isOverdue() ? "已還（逾期）" : "已還";
            } else {
                status = r.isOverdue() ? "⚠️ 逾期未還" : "借閱中";
            }
            return new SimpleStringProperty(status);
        });
        returnStatusCol.setMaxWidth(110);
        // 逾期未還用紅色標示
        returnStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("逾期")) {
                        setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                    } else if ("借閱中".equals(item)) {
                        setStyle("-fx-text-fill: #2980B9;");
                    } else {
                        setStyle("-fx-text-fill: #27AE60;");
                    }
                }
            }
        });

        // ✨ 功能新增：顯示實際還書時間欄位 (去除秒與毫秒，保留至分鐘 YYYY-MM-DD HH:MM)
        TableColumn<BorrowRecord, String> returnDateCol = new TableColumn<>("實際歸還時間");
        returnDateCol.setCellValueFactory(cell -> {
            java.time.LocalDateTime rDate = cell.getValue().getReturnDate();
            if (rDate != null) {
                return new SimpleStringProperty(rDate.toString().replace("T", " ").substring(0, 16));
            }
            return new SimpleStringProperty("-");
        });
        returnDateCol.setMaxWidth(200);

        borrowTable.getColumns().addAll(titleCol, dateCol, dueCol, returnStatusCol, returnDateCol);

        // 還書按鈕（只對未還書有效）
        Button returnBtn = new Button("↩️ 辦理選定書籍還書");
        returnBtn.setStyle(AppStyle.buttonPrimary());
        returnBtn.setOnAction(e -> handleAsyncReturn());

        // ---------- 罰款明細 ----------
        Label fineHeading = new Label("⚠️ 逾期未結清帳單明細");
        fineHeading.setFont(Font.font("System", FontWeight.BOLD, 18));
        fineHeading.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        fineTable = new TableView<>(finesData);
        fineTable.setPrefHeight(180);
        fineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Fine, String> fBookCol = new TableColumn<>("違規書籍");
        fBookCol.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getRecord().getBook().getTitle()));

        // ✅ 新增逾期天數欄
        TableColumn<Fine, String> fDaysCol = new TableColumn<>("逾期天數");
        fDaysCol.setCellValueFactory(cell -> {
            BorrowRecord r = cell.getValue().getRecord();
            java.time.LocalDateTime endPoint = r.getReturnDate() != null
                    ? r.getReturnDate() : java.time.LocalDateTime.now();
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    r.getDueDate().toLocalDate(), endPoint.toLocalDate());
            return new SimpleStringProperty(days > 0 ? days + " 天" : "< 1 天");
        });
        fDaysCol.setMaxWidth(90);

        TableColumn<Fine, String> fAmountCol = new TableColumn<>("累積罰金 (TWD)");
        fAmountCol.setCellValueFactory(cell ->
                new SimpleStringProperty("$ " + cell.getValue().getAmount()));
        fAmountCol.setMaxWidth(150);

        fineTable.getColumns().addAll(fBookCol, fDaysCol, fAmountCol);

        totalFineLabel = new Label("未繳總金額：計算中...");
        totalFineLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        totalFineLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        Button payBtn = new Button("💰 立即繳納選定帳單");
        payBtn.setStyle(AppStyle.buttonSecondary());
        payBtn.setOnAction(e -> handleAsyncPayFine());

        // ✅ 手動刷新按鈕（罰金是動態的，刷新才能看最新累積金額）
        Button refreshBtn = new Button("🔄 刷新");
        refreshBtn.setStyle(AppStyle.buttonSecondary());
        refreshBtn.setOnAction(e -> asyncRefreshUserData());

        HBox fOps = new HBox(15, totalFineLabel, payBtn, refreshBtn);
        fOps.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(
                borrowHeading, borrowTable, returnBtn,
                new Separator(),
                fineHeading, fOps, fineTable
        );
        return box;
    }

    // ==========================================
    // 📖 圖書檢索與借閱
    // ==========================================
    private Pane buildBookSearchPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label title = new Label("📖 全院館藏圖書線上檢索");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));

        HBox searchBox = new HBox(10);
        TextField searchBar = new TextField();
        searchBar.setPromptText("輸入書名、作者或主題關鍵字...");
        searchBar.setStyle(AppStyle.textField());
        searchBar.setPrefWidth(300);

        Button searchBtn = new Button("🔍 搜尋");
        searchBtn.setStyle(AppStyle.buttonSecondary());
        searchBtn.setOnAction(e -> handleAsyncSearch(searchBar.getText()));
        // 按 Enter 也能搜尋
        searchBar.setOnAction(e -> handleAsyncSearch(searchBar.getText()));

        searchBox.getChildren().addAll(searchBar, searchBtn);

        searchTable = new TableView<>(searchBooksData);
        searchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(searchTable, Priority.ALWAYS);

        TableColumn<Book, String> titleCol = new TableColumn<>("書名");
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));

        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuthors()));

        TableColumn<Book, String> statusCol = new TableColumn<>("狀態");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getStatus() == BookStatus.AVAILABLE ? "✅ 可借" : "❌ 已借出"
        ));
        statusCol.setMaxWidth(90);

        searchTable.getColumns().addAll(titleCol, authorCol, statusCol);

        HBox borrowBox = new HBox(10);
        borrowBox.setAlignment(Pos.CENTER_LEFT);

        Label daysLabel = new Label("借閱天數：");
        ComboBox<Integer> daysCombo = new ComboBox<>();
        daysCombo.getItems().addAll(1,3,7,14);
        daysCombo.setValue(7);
        daysCombo.setStyle(AppStyle.textField());

        Button borrowBtn = new Button("✅ 借閱選定書籍");
        borrowBtn.setStyle(AppStyle.buttonPrimary());
        borrowBtn.setOnAction(e -> handleAsyncBorrow(daysCombo.getValue()));

        // ✨ 功能新增：查看該書籍近期借還歷史紀錄按鈕
        Button historyBtn = new Button("🔍 查看書籍歷史");
        historyBtn.setStyle(AppStyle.buttonSecondary());
        historyBtn.setOnAction(e -> handleAsyncBookHistory());

        borrowBox.getChildren().addAll(daysLabel, daysCombo, borrowBtn, historyBtn);
        box.getChildren().addAll(title, searchBox, searchTable, borrowBox);
        return box;
    }

    // ==========================================
    // 非同步背景任務
    // ==========================================

    /**
     * 一次刷新：個人資訊 + 借閱歷史（含已還） + 未繳罰款 + 總金額
     */
    private void asyncRefreshUserData() {
        // 只在 borrowTable / fineTable 已被建立後設 placeholder
        if (borrowTable != null) borrowTable.setPlaceholder(new ProgressIndicator());
        if (fineTable != null) fineTable.setPlaceholder(new ProgressIndicator());

        Task<DashboardDataPack> refreshTask = new Task<>() {
            @Override
            protected DashboardDataPack call() throws Exception {
                User latestUser = userService.getUserById(userId);
                // ✅ getUserBorrowHistory 取全部歷史（含已還），不只是進行中
                List<BorrowRecord> allHistory = borrowService.getUserBorrowHistory(userId);
                // ✅ getUnpaidFinesByUser 會觸發「逾期即開罰」掃描
                List<Fine> unpaidFines = fineService.getUnpaidFinesByUser(userId);
                int totalFine = fineService.getTotalUnpaidAmount(userId);
                return new DashboardDataPack(latestUser, allHistory, unpaidFines, totalFine);
            }
        };

        refreshTask.setOnSucceeded(e -> {
            DashboardDataPack pack = refreshTask.getValue();
            if (pack.user != null) {
                userName = pack.user.getName();
                nameBadge.setText(userName + " 同學");
                roleBadge.setText("身分: " + pack.user.getRoleLevel().name()
                        + " | 狀態: " + pack.user.getStatus().name());
                if (pack.user.getStatus() == UserStatus.SUSPENDED) {
                    roleBadge.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                } else {
                    roleBadge.setStyle("-fx-text-fill: " + AppStyle.SUCCESS + ";");
                }
            }
            borrowHistoryData.setAll(pack.borrowRecords);
            finesData.setAll(pack.unpaidFines);
            totalFineLabel.setText("未繳總金額：NT$ " + pack.totalFineAmount);
            if (borrowTable != null) borrowTable.setPlaceholder(new Label("目前無任何借閱紀錄。"));
            if (fineTable != null) fineTable.setPlaceholder(new Label("太棒了！您目前沒有任何未繳罰款。"));

            // ✨ 功能新增：借閱到期溫馨提醒機制（自動掃描 3 天內即將到期的未歸還書籍）
            if (!hasShownDueReminder) {
                StringBuilder reminderMsg = new StringBuilder();
                int imminentCount = 0;
                java.time.LocalDateTime now = java.time.LocalDateTime.now();

                for (BorrowRecord record : pack.borrowRecords) {
                    if (record.getReturnDate() == null) { // 篩選借閱中的書籍
                        long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(
                                now.toLocalDate(), record.getDueDate().toLocalDate());
                        // 距離到期 0 至 3 天者納入主動提示範圍
                        if (daysLeft >= 0 && daysLeft <= 3) {
                            imminentCount++;
                            reminderMsg.append(String.format("•《%s》將於 %d 天後到期\n（應還日期：%s）\n",
                                    record.getBook().getTitle(), daysLeft, record.getDueDate().toString().substring(0,10)));
                        }
                    }
                }

                if (imminentCount > 0) {
                    hasShownDueReminder = true; // 鎖定旗標，避免使用者每次手動點擊「刷新」都重覆跳窗打擾
                    showAlert("⏰ 借閱即將到期溫馨提醒",
                            "您目前有 " + imminentCount + " 本書籍即將到期，請留意還書時間以免逾期：\n\n" + reminderMsg.toString());
                }
            }
        });

        refreshTask.setOnFailed(e -> {
            System.err.println("❌ 刷新使用者資料失敗：" + refreshTask.getException().getMessage());
        });

        new Thread(refreshTask).start();
    }

    private void handleAsyncSearch(String keyword) {
        if (searchTable == null) return;
        searchTable.setPlaceholder(new ProgressIndicator());
        Task<List<Book>> searchTask = new Task<>() {
            @Override
            protected List<Book> call() throws Exception {
                return bookService.searchBooks(keyword);
            }
        };
        searchTask.setOnSucceeded(e -> {
            searchBooksData.setAll(searchTask.getValue());
            searchTable.setPlaceholder(new Label("查無符合條件的書籍。"));
        });
        new Thread(searchTask).start();
    }

    private void handleAsyncBorrow(int days) {
        Book selectedBook = searchTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("提示", "請先從列表中選取要借閱的書籍。");
            return;
        }

        Task<String> borrowTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return borrowService.borrowBook(userId, selectedBook.getBookId(), days);
            }
        };

        borrowTask.setOnSucceeded(e -> {
            String result = borrowTask.getValue();
            if ("SUCCESS".equals(result)) {
                showAlert("借閱成功", "✅ 已成功借閱《" + selectedBook.getTitle() + "》！");
                handleAsyncSearch("");        // 刷新庫存狀態
                asyncRefreshUserData();      // 刷新借閱歷史
            } else {
                showAlert("借閱失敗", result);
            }
        });
        new Thread(borrowTask).start();
    }

    // ✨ 功能新增：非同步獲取該書籍的流通借還歷史紀錄
    private void handleAsyncBookHistory() {
        Book selectedBook = searchTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("提示", "請先從列表中選取要查看歷史的書籍。");
            return;
        }

        Task<List<BorrowRecord>> historyTask = new Task<>() {
            @Override
            protected List<BorrowRecord> call() throws Exception {
                return borrowService.getBookBorrowHistory(selectedBook.getBookId());
            }
        };

        historyTask.setOnSucceeded(e -> {
            List<BorrowRecord> history = historyTask.getValue();
            showBookHistoryDialog(selectedBook.getTitle(), history);
        });

        historyTask.setOnFailed(e -> {
            showAlert("系統異常", "無法載入該書籍的歷史流通紀錄。");
        });

        new Thread(historyTask).start();
    }

    // ✨ 功能新增：以 Dialog 彈出視窗漂亮展示單本書歷史，並對其他學生姓名做隱私去識別化遮罩
    private void showBookHistoryDialog(String bookTitle, List<BorrowRecord> history) {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("書籍近期借還紀錄");
            dialog.setHeaderText("書籍：《" + bookTitle + "》的歷史流通流通軌跡");

            ButtonType closeButtonType = new ButtonType("關閉", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButtonType);

            TableView<BorrowRecord> hTable = new TableView<>(FXCollections.observableArrayList(history));
            hTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            hTable.setPrefSize(520, 280);

            // 1. 借閱人欄位（資訊去識別化處理：如 '王大明' -> '王*明', '李小美' -> '李*美'）
            TableColumn<BorrowRecord, String> userCol = new TableColumn<>("借閱人");
            userCol.setCellValueFactory(c -> {
                User u = c.getValue().getUser();
                if (u != null) {
                    String name = u.getName();
                    if (name != null && name.length() > 1) {
                        return new SimpleStringProperty(name.charAt(0) + "*" + (name.length() > 2 ? name.substring(2) : ""));
                    }
                    return new SimpleStringProperty(name);
                }
                return new SimpleStringProperty("未知");
            });
            userCol.setMaxWidth(100);

            // 2. 借出時間欄位
            TableColumn<BorrowRecord, String> bDateCol = new TableColumn<>("借出時間");
            bDateCol.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getBorrowDate() != null ? c.getValue().getBorrowDate().toString().replace("T", " ").substring(0, 16) : "-"));

            // 3. 歸還時間欄位
            TableColumn<BorrowRecord, String> rDateCol = new TableColumn<>("歸還時間");
            rDateCol.setCellValueFactory(c -> new SimpleStringProperty(
                    c.getValue().getReturnDate() != null ? c.getValue().getReturnDate().toString().replace("T", " ").substring(0, 16) : "借閱中（未歸還）"));

            hTable.getColumns().addAll(userCol, bDateCol, rDateCol);
            hTable.setPlaceholder(new Label("該書籍目前尚無任何歷史借閱紀錄。"));

            VBox content = new VBox(hTable);
            content.setPadding(new Insets(10));
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
        });
    }

    private void handleAsyncReturn() {
        BorrowRecord selectedRecord = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            showAlert("操作提示", "請先從上方列表中選取您要歸還的書籍。");
            return;
        }
        // 若已還書則不允許重複操作
        if (selectedRecord.getReturnDate() != null) {
            showAlert("操作提示", "此書籍已經歸還，無需重複操作。");
            return;
        }

        Task<String> returnTask = new Task<>() {
            @Override protected String call() throws Exception {
                return borrowService.returnBook(selectedRecord.getBook().getBookId());
            }
        };

        returnTask.setOnSucceeded(e -> {
            String result = returnTask.getValue();
            if (result.startsWith("SUCCESS_WITH_FINE")) {
                String amount = result.split(":")[1];
                showAlert("還書成功（逾期）",
                        "⚠️ 歸還成功！由於該書已逾期，系統已自動開立 NT$ " + amount
                                + " 罰單，您的帳號已被暫停借閱權限。");
            } else if ("SUCCESS".equals(result)) {
                showAlert("還書成功", "✅ 書籍已順利歸還！感謝您的配合。");
            } else {
                showAlert("還書失敗", result);
            }
            handleAsyncSearch("");
            asyncRefreshUserData();
        });
        new Thread(returnTask).start();
    }

    private void handleAsyncPayFine() {
        Fine selectedFine = fineTable.getSelectionModel().getSelectedItem();
        if (selectedFine == null) {
            showAlert("操作提示", "請從下方帳單列表中選擇一筆要繳納的款項。");
            return;
        }

        Task<String> payTask = new Task<>() {
            @Override protected String call() throws Exception {
                return fineService.payFine(selectedFine.getFineId());
            }
        };

        payTask.setOnSucceeded(e -> {
            String result = payTask.getValue();
            if ("SUCCESS_AND_ACTIVATED".equals(result)) {
                showAlert("繳費成功",
                        "🎉 所有罰款皆已結清！您的借閱權限已自動復權，歡迎繼續借閱。");
            } else if ("SUCCESS".equals(result)) {
                showAlert("繳費成功",
                        "✅ 該筆款項已收訖。由於您名下仍有其他未繳罰單，請全數結清以恢復權限。");
            } else {
                showAlert("繳費失敗", result);
            }
            asyncRefreshUserData();
        });
        new Thread(payTask).start();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    private static class DashboardDataPack {
        final User user;
        final List<BorrowRecord> borrowRecords;
        final List<Fine> unpaidFines;
        final int totalFineAmount;

        DashboardDataPack(User user, List<BorrowRecord> borrowRecords,
                          List<Fine> unpaidFines, int totalFineAmount) {
            this.user = user;
            this.borrowRecords = borrowRecords;
            this.unpaidFines = unpaidFines;
            this.totalFineAmount = totalFineAmount;
        }
    }
}