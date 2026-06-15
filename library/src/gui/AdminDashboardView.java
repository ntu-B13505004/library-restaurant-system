package library.gui;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import library.model.*;
import library.service.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminDashboardView {
    private final Stage stage;
    private final int adminId;
    private final String adminName;

    private final BookService bookService = new BookService();
    private final UserService userService = new UserService();
    private final ReportService reportService = new ReportService();
    private final BorrowService borrowService = new BorrowService();
    private final FineService fineService = new FineService();

    private final BorderPane mainLayout = new BorderPane();
    private final StackPane contentArea = new StackPane();

    private final ObservableList<Book> booksData = FXCollections.observableArrayList();
    private final ObservableList<Map<String, Object>> usersData = FXCollections.observableArrayList();
    private final ObservableList<BorrowRecord> globalBorrowsData = FXCollections.observableArrayList();
    private final ObservableList<Fine> globalFinesData = FXCollections.observableArrayList();

    private TableView<Book> bookTable;
    private TableView<Map<String, Object>> userTable;
    private TableView<BorrowRecord> globalBorrowTable;
    private TableView<Fine> globalFineTable;

    public AdminDashboardView(Stage stage, Map<String, Object> session) {
        this.stage = stage;
        this.adminId = (int) session.get("userId");
        this.adminName = (String) session.get("name");
    }

    public void show() {
        mainLayout.setStyle("-fx-background-color: " + AppStyle.BG_WINDOW + ";");

        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setStyle("-fx-background-color: " + AppStyle.PRIMARY + ";");

        Label nameBadge = new Label(adminName + " 系統長");
        nameBadge.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameBadge.setStyle("-fx-text-fill: white;");
        Label roleBadge = new Label("權限階級: SYSTEM_ADMIN");
        roleBadge.setStyle("-fx-text-fill: " + AppStyle.ACCENT_LIGHT + "; -fx-font-size: 12px;");

        VBox profileBox = new VBox(5, nameBadge, roleBadge);
        profileBox.setPadding(new Insets(0, 0, 20, 0));

        Button navReportBtn  = new Button("📊 營運數據報表");
        Button navBookBtn    = new Button("📚 藏書維護與管理");
        Button navUserBtn    = new Button("👥 讀者權限審查");
        Button navRecordBtn  = new Button("📜 全館借閱與罰款");
        Button logoutBtn     = new Button("🚪 登出系統");

        Button[] navButtons = {navReportBtn, navBookBtn, navUserBtn, navRecordBtn, logoutBtn};
        for (Button btn : navButtons) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(40);
            btn.setAlignment(Pos.CENTER_LEFT);
            if (btn == logoutBtn) {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E74C3C; -fx-font-weight: bold; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ECF0F1; -fx-font-size: 14px; -fx-cursor: hand;");
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(profileBox, new Separator(),
                navReportBtn, navBookBtn, navUserBtn, navRecordBtn, spacer, logoutBtn);

        navReportBtn.setOnAction(e -> switchPage(buildReportPage()));
        navBookBtn.setOnAction(e -> { switchPage(buildBookManagementPage()); asyncLoadBooks(""); });
        navUserBtn.setOnAction(e -> { switchPage(buildUserManagementPage()); asyncLoadUsers(""); });
        navRecordBtn.setOnAction(e -> { switchPage(buildGlobalRecordPage()); asyncLoadRecords(""); });
        logoutBtn.setOnAction(e -> new LoginView(stage).show());

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);

        switchPage(buildReportPage());

        Scene scene = new Scene(mainLayout, 1150, 720);
        stage.setScene(scene);
        stage.setTitle("智慧圖書館 - 最高權限管理後台");
        stage.show();
    }

    private void switchPage(Pane page) {
        contentArea.getChildren().setAll(page);
    }

    // ==========================================
    // 📊 營運數據報表（✨ 介面外觀優化與動態整理改進）
    // ==========================================
    private Pane buildReportPage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        // 頂部抬頭與重新整理按鈕列
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        Label header = new Label("圖書館營運數據與 KPI 看板");
        header.setFont(Font.font("System", FontWeight.BOLD, 22));
        Region springSpacer = new Region();
        HBox.setHgrow(springSpacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 整理數據");
        refreshBtn.setStyle(AppStyle.buttonSecondary());
        refreshBtn.setOnAction(e -> switchPage(buildReportPage())); // 點擊直接無縫刷新本頁數據
        headerBar.getChildren().addAll(header, springSpacer, refreshBtn);

        Map<String, Integer> kpis = reportService.getLibraryGeneralKPIs();
        Map<String, Object> finesSummary = reportService.getLibraryFineSummary();

        // 建立優化後的網格 KPI 佈局，讓兩排卡片視覺完全置中對齊
        GridPane kpiGrid = new GridPane();
        kpiGrid.setHgap(15);
        kpiGrid.setVgap(15);

        // 上排：常態智慧館藏 KPI卡片 (配藍、青、橙色側條)
        kpiGrid.add(createKpiCard("總藏書量", String.valueOf(kpis.getOrDefault("總藏書量", 0)), "#2C3E50", "#3498DB"), 0, 0);
        kpiGrid.add(createKpiCard("目前借出冊數", String.valueOf(kpis.getOrDefault("目前借出冊數", 0)), "#2980B9", "#1ABC9C"), 1, 0);
        kpiGrid.add(createKpiCard("停權人數", String.valueOf(kpis.getOrDefault("停權學生人數", 0)), "#C0392B", "#E67E22"), 2, 0);
        kpiGrid.add(createKpiCard("未收罰金", "$ " + finesSummary.getOrDefault("未繳罰款總金額", 0), "#D35400", "#E74C3C"), 3, 0);

        // 下排：深度罰金財務與違規追蹤 (配紫、深紅、綠色側條)
        kpiGrid.add(createKpiCard("逾期未還筆數", String.valueOf(finesSummary.getOrDefault("逾期未還筆數", 0)), "#8E44AD", "#9B59B6"), 0, 1);
        kpiGrid.add(createKpiCard("未繳罰款件數", String.valueOf(finesSummary.getOrDefault("未繳罰款件數", 0)), "#962D22", "#C0392B"), 1, 1);
        kpiGrid.add(createKpiCard("本月已收罰金", "$ " + finesSummary.getOrDefault("本月已收罰款金額", 0), "#27AE60", "#2ECC71"), 2, 1);
        kpiGrid.add(createKpiCard("累計已收罰金", "$ " + finesSummary.getOrDefault("累計已收罰款金額", 0), "#1E8449", "#27AE60"), 3, 1);

        // 圖表區
        HBox chartBox = new HBox(20);
        VBox.setVgrow(chartBox, Priority.ALWAYS);

        PieChart pieChart = new PieChart();
        pieChart.setTitle("熱門借閱主題分佈");
        pieChart.setLabelsVisible(true);
        Map<String, Integer> subjectData = reportService.getBookSubjectPopularity();
        subjectData.forEach((subject, count) ->
                pieChart.getData().add(new PieChart.Data(subject, count)));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("全校熱門借閱排行榜 Top 5");
        barChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Integer> topBooks = reportService.getTop5BorrowedBooks();

        topBooks.forEach((title, count) -> {
            // ✨ 改善點：若書名過長，截斷顯示防止長條圖底部標籤擠成一團
            String displayName = title.length() > 12 ? title.substring(0, 12) + "..." : title;
            series.getData().add(new XYChart.Data<>(displayName, count));
        });
        barChart.getData().add(series);

        HBox.setHgrow(pieChart, Priority.ALWAYS);
        HBox.setHgrow(barChart, Priority.ALWAYS);
        chartBox.getChildren().addAll(pieChart, barChart);

        box.getChildren().addAll(headerBar, kpiGrid, new Separator(), chartBox);
        return box;
    }

    // ✨ 改善點：新增具有側邊質感色彩條與專屬文字填色的精美 KPI 圖卡
    private VBox createKpiCard(String title, String value, String valueColorHex, String sideBorderColorHex) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12, 15, 12, 15));
        // 使用 CSS 加強陰影，並在左側加上 4px 的高亮主題識別條
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: transparent transparent transparent " + sideBorderColorHex + "; " +
                "-fx-border-width: 0 0 0 4; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 6, 0, 0, 2);");
        card.setPrefWidth(215);

        Label tLabel = new Label(title);
        tLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label vLabel = new Label(value);
        vLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        vLabel.setStyle("-fx-text-fill: " + valueColorHex + ";");

        card.getChildren().addAll(tLabel, vLabel);
        return card;
    }

    // ==========================================
    // 📚 藏書維護與管理
    // ==========================================
    private Pane buildBookManagementPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label header = new Label("📚 館藏圖書維護作業");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("輸入關鍵字搜尋書籍...");
        searchField.setStyle(AppStyle.textField());
        Button searchBtn = new Button("檢索");
        searchBtn.setStyle(AppStyle.buttonSecondary());
        searchBtn.setOnAction(e -> asyncLoadBooks(searchField.getText()));
        searchField.setOnAction(e -> asyncLoadBooks(searchField.getText()));
        searchBox.getChildren().addAll(searchField, searchBtn);

        bookTable = new TableView<>(booksData);
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(bookTable, Priority.ALWAYS);

        TableColumn<Book, Integer> idCol = new TableColumn<>("編號");
        idCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getBookId()).asObject());
        idCol.setMaxWidth(60);
        TableColumn<Book, String> titleCol = new TableColumn<>("書名");
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuthors()));
        TableColumn<Book, String> statusCol = new TableColumn<>("狀態");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        statusCol.setMaxWidth(90);

        bookTable.getColumns().addAll(idCol, titleCol, authorCol, statusCol);

        HBox opsBox = new HBox(10);
        opsBox.setAlignment(Pos.CENTER_LEFT);
        TextField tTitle  = new TextField(); tTitle.setPromptText("新書書名");  tTitle.setStyle(AppStyle.textField());
        TextField tAuthor = new TextField(); tAuthor.setPromptText("作者");     tAuthor.setStyle(AppStyle.textField());
        TextField tSubject= new TextField(); tSubject.setPromptText("主題");    tSubject.setStyle(AppStyle.textField());
        Button addBtn = new Button("➕ 登記上架");
        addBtn.setStyle(AppStyle.buttonPrimary());
        addBtn.setOnAction(e -> handleAsyncAddBook(
                tTitle.getText(), tAuthor.getText(), tSubject.getText(), tTitle, tAuthor, tSubject));
        Button delBtn = new Button("🗑️ 下架選定書籍");
        delBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        delBtn.setOnAction(e -> handleAsyncRemoveBook());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        opsBox.getChildren().addAll(tTitle, tAuthor, tSubject, addBtn, sp, delBtn);
        box.getChildren().addAll(header, searchBox, bookTable, new Separator(), opsBox);
        return box;
    }

    // ==========================================
    // 👥 讀者權限審查
    // ==========================================
    private Pane buildUserManagementPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label header = new Label("👥 全校讀者狀態與權限審查");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField userSearchField = new TextField();
        userSearchField.setPromptText("輸入學號或姓名關鍵字篩選...");
        userSearchField.setStyle(AppStyle.textField());
        userSearchField.setPrefWidth(250);
        Button userSearchBtn = new Button("🔍 搜尋");
        userSearchBtn.setStyle(AppStyle.buttonSecondary());
        userSearchBtn.setOnAction(e -> asyncLoadUsers(userSearchField.getText()));
        userSearchField.setOnAction(e -> asyncLoadUsers(userSearchField.getText()));
        Button clearBtn = new Button("清除");
        clearBtn.setStyle(AppStyle.buttonSecondary());
        clearBtn.setOnAction(e -> { userSearchField.clear(); asyncLoadUsers(""); });
        searchBox.getChildren().addAll(userSearchField, userSearchBtn, clearBtn);

        userTable = new TableView<>(usersData);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        TableColumn<Map<String, Object>, String> snoCol = new TableColumn<>("學號");
        snoCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("studentNo")));
        TableColumn<Map<String, Object>, String> unameCol = new TableColumn<>("姓名");
        unameCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("name")));
        TableColumn<Map<String, Object>, String> uRoleCol = new TableColumn<>("身分別");
        uRoleCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("roleLevel")));
        uRoleCol.setMaxWidth(100);
        TableColumn<Map<String, Object>, String> uStatusCol = new TableColumn<>("帳號狀態");
        uStatusCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("status")));
        uStatusCol.setMaxWidth(100);

        uStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle("SUSPENDED".equals(item)
                            ? "-fx-text-fill: #E74C3C; -fx-font-weight: bold;"
                            : "-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                }
            }
        });

        userTable.getColumns().addAll(snoCol, unameCol, uRoleCol, uStatusCol);

        HBox opsBox = new HBox(10);
        opsBox.setAlignment(Pos.CENTER_LEFT);

        Button toggleStatusBtn = new Button("⚡ 切換停權/復權");
        toggleStatusBtn.setStyle(AppStyle.buttonSecondary());
        toggleStatusBtn.setOnAction(e -> handleAsyncToggleUserStatus());

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("NORMAL", "VIP");
        roleCombo.setValue("VIP");
        roleCombo.setStyle(AppStyle.textField());

        Button changeRoleBtn = new Button("🔁 變更身分");
        changeRoleBtn.setStyle(AppStyle.buttonPrimary());
        changeRoleBtn.setOnAction(e -> handleAsyncChangeUserRole(roleCombo.getValue()));

        Button viewDetailBtn = new Button("🔎 查看讀者詳情");
        viewDetailBtn.setStyle(AppStyle.buttonSecondary());
        viewDetailBtn.setOnAction(e -> handleShowUserDetail());

        opsBox.getChildren().addAll(toggleStatusBtn, new Label("|"), roleCombo, changeRoleBtn,
                new Label("|"), viewDetailBtn);

        box.getChildren().addAll(header, searchBox, userTable, opsBox);
        return box;
    }

    // ==========================================
    // 📜 全館借閱紀錄與罰款（✨ 支援借閱、罰單雙表學號同時連動過濾）
    // ==========================================
    private Pane buildGlobalRecordPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label header = new Label("全館借閱紀錄與未繳罰款追蹤");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        // ✅ 升級過濾列：一鍵篩選借閱歷史與未清罰單
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        TextField filterField = new TextField();
        filterField.setPromptText("輸入學號以連動過濾借閱與罰單...");
        filterField.setStyle(AppStyle.textField());
        filterField.setPrefWidth(260);

        Button filterBtn = new Button("🔍 條件檢索");
        filterBtn.setStyle(AppStyle.buttonSecondary());
        filterBtn.setOnAction(e -> asyncLoadRecords(filterField.getText()));
        filterField.setOnAction(e -> asyncLoadRecords(filterField.getText()));

        Button refreshAllBtn = new Button("🔄 清除篩選");
        refreshAllBtn.setStyle(AppStyle.buttonSecondary());
        refreshAllBtn.setOnAction(e -> { filterField.clear(); asyncLoadRecords(""); });
        filterBox.getChildren().addAll(filterField, filterBtn, refreshAllBtn);

        // ---- 借閱紀錄表格 ----
        Label borrowLabel = new Label("📋 全館借閱狀態歷史");
        borrowLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        globalBorrowTable = new TableView<>(globalBorrowsData);
        globalBorrowTable.setPrefHeight(210);
        globalBorrowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BorrowRecord, String> bUserCol = new TableColumn<>("借閱人 (學號)");
        bUserCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getUser().getStudentNo()));

        TableColumn<BorrowRecord, String> bBookCol = new TableColumn<>("借閱書籍");
        bBookCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getBook().getTitle()));

        TableColumn<BorrowRecord, String> bBorrowDateCol = new TableColumn<>("借閱日期");
        bBorrowDateCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getBorrowDate().toString().substring(0, 10)));
        bBorrowDateCol.setMaxWidth(100);

        TableColumn<BorrowRecord, String> bDueDateCol = new TableColumn<>("應還期限");
        bDueDateCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getDueDate().toString().substring(0, 10)));
        bDueDateCol.setMaxWidth(100);

        TableColumn<BorrowRecord, String> bStatusCol = new TableColumn<>("狀態");
        bStatusCol.setCellValueFactory(c -> {
            BorrowRecord r = c.getValue();
            String s;
            if (r.getReturnDate() != null) {
                s = r.isOverdue() ? "已還（逾期）" : "已還";
            } else {
                s = r.isOverdue() ? "⚠️ 逾期未還" : "借閱中";
            }
            return new SimpleStringProperty(s);
        });
        bStatusCol.setMaxWidth(110);
        bStatusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); }
                else {
                    setText(item);
                    setStyle(item.contains("逾期")
                            ? "-fx-text-fill: #E74C3C; -fx-font-weight: bold;"
                            : item.equals("借閱中") ? "-fx-text-fill: #2980B9;" : "");
                }
            }
        });

        globalBorrowTable.getColumns().addAll(bUserCol, bBookCol, bBorrowDateCol, bDueDateCol, bStatusCol);

        // ---- 罰款表格 ----
        Label fineHeader = new Label("⚠️ 全館未繳清罰單清單");
        fineHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        fineHeader.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        globalFineTable = new TableView<>(globalFinesData);
        globalFineTable.setPrefHeight(190);
        globalFineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Fine, String> fUserCol = new TableColumn<>("違規學號");
        fUserCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRecord().getUser().getStudentNo()));
        fUserCol.setMaxWidth(120);

        TableColumn<Fine, String> fBookCol2 = new TableColumn<>("違規書籍");
        fBookCol2.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getRecord().getBook().getTitle()));

        TableColumn<Fine, String> fDaysCol = new TableColumn<>("逾期天數");
        fDaysCol.setCellValueFactory(c -> {
            BorrowRecord r = c.getValue().getRecord();
            java.time.LocalDateTime endPoint = r.getReturnDate() != null
                    ? r.getReturnDate() : java.time.LocalDateTime.now();
            long days = java.time.temporal.ChronoUnit.DAYS.between(
                    r.getDueDate().toLocalDate(), endPoint.toLocalDate());
            return new SimpleStringProperty(days > 0 ? days + " 天" : "< 1 天");
        });
        fDaysCol.setMaxWidth(90);

        TableColumn<Fine, String> fAmtCol = new TableColumn<>("累積罰金");
        fAmtCol.setCellValueFactory(c ->
                new SimpleStringProperty("$ " + c.getValue().getAmount()));
        fAmtCol.setMaxWidth(100);

        TableColumn<Fine, String> fPaidCol = new TableColumn<>("繳款狀態");
        fPaidCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().isPaid() ? "✅ 已繳清" : "❌ 未繳"));
        fPaidCol.setMaxWidth(90);

        globalFineTable.getColumns().addAll(fUserCol, fBookCol2, fDaysCol, fAmtCol, fPaidCol);

        // 操作列
        HBox fineOpsBox = new HBox(15);
        fineOpsBox.setAlignment(Pos.CENTER_LEFT);

        Button adminPayBtn = new Button("💳 代辦繳款（選定罰單）");
        adminPayBtn.setStyle(AppStyle.buttonPrimary());
        adminPayBtn.setOnAction(e -> handleAsyncAdminPayFine());

        Button scanOverdueBtn = new Button("🔍 掃描逾期並建單");
        scanOverdueBtn.setStyle(AppStyle.buttonSecondary());
        scanOverdueBtn.setOnAction(e -> {
            Task<Void> scanTask = new Task<>() {
                @Override protected Void call() {
                    fineService.getAllUnpaidFines();
                    return null;
                }
            };
            scanTask.setOnSucceeded(ev -> asyncLoadRecords(filterField.getText()));
            new Thread(scanTask).start();
            showAlert("掃描完成", "已掃描全館所有逾期借閱紀錄並自動補建罰單。");
        });

        fineOpsBox.getChildren().addAll(adminPayBtn, scanOverdueBtn);

        box.getChildren().addAll(
                header, filterBox,
                borrowLabel, globalBorrowTable,
                new Separator(),
                fineHeader, fineOpsBox, globalFineTable
        );
        return box;
    }

    // ==========================================
    // 異步背景任務 (✨ 修正：讓罰金清單能接收過濾字串並用 Stream 篩選)
    // ==========================================
    private void asyncLoadBooks(String keyword) {
        if (bookTable == null) return;
        bookTable.setPlaceholder(new ProgressIndicator());
        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() {
                return bookService.searchBooks(keyword);
            }
        };
        task.setOnSucceeded(e -> booksData.setAll(task.getValue()));
        new Thread(task).start();
    }

    private void asyncLoadUsers(String keyword) {
        if (userTable == null) return;
        userTable.setPlaceholder(new ProgressIndicator());
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override protected List<Map<String, Object>> call() {
                return userService.getAllStudents(keyword);
            }
        };
        task.setOnSucceeded(e -> usersData.setAll(task.getValue()));
        new Thread(task).start();
    }

    private void asyncLoadRecords(String studentNoFilter) {
        if (globalBorrowTable != null) globalBorrowTable.setPlaceholder(new ProgressIndicator());
        if (globalFineTable   != null) globalFineTable.setPlaceholder(new ProgressIndicator());

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                // 1. 取得符合學號過濾的借閱歷史
                List<BorrowRecord> borrows = borrowService.getAllBorrowRecords(studentNoFilter);

                // 2. 取得全館未繳罰單
                List<Fine> allFines = fineService.getAllUnpaidFines();

                // ✅ 核心改進：利用 Java Stream API，根據學號關鍵字即時過濾罰單清單
                final List<Fine> filteredFines;
                if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
                    String cleanFilter = studentNoFilter.trim().toLowerCase();
                    filteredFines = allFines.stream()
                            .filter(f -> f.getRecord() != null && f.getRecord().getUser() != null
                                    && f.getRecord().getUser().getStudentNo().toLowerCase().contains(cleanFilter))
                            .collect(Collectors.toList());
                } else {
                    filteredFines = allFines;
                }

                Platform.runLater(() -> {
                    globalBorrowsData.setAll(borrows);
                    globalFinesData.setAll(filteredFines);
                    if (globalBorrowTable != null) globalBorrowTable.setPlaceholder(new Label("無符合條件之借閱紀錄。"));
                    if (globalFineTable   != null) globalFineTable.setPlaceholder(new Label("目前無符合條件之未繳罰款。"));
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    // ==========================================
    // 事件處理
    // ==========================================
    private void handleAsyncAddBook(String title, String author, String subject, TextField... fields) {
        if (title.isEmpty() || author.isEmpty()) {
            showAlert("提示", "書名與作者為必填欄位。"); return;
        }
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                return bookService.addBook(
                        new Book(0, title, author, subject, "", "", "", "", "", ""));
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                showAlert("成功", "新書已建檔入庫！");
                for (TextField f : fields) f.clear();
                asyncLoadBooks("");
            } else showAlert("失敗", "上架作業失敗。");
        });
        new Thread(task).start();
    }

    private void handleAsyncRemoveBook() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("提示", "請選擇要下架的書籍。"); return; }
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                return bookService.removeBook(selected.getBookId());
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) { showAlert("成功", "書籍已成功下架！"); asyncLoadBooks(""); }
            else showAlert("失敗", "下架失敗，可能該書目前正被借出中。");
        });
        new Thread(task).start();
    }

    private void handleAsyncToggleUserStatus() {
        Map<String, Object> selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) { showAlert("提示", "請先選取一名讀者。"); return; }
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                int uid = (Integer) selectedUser.get("userId");
                String current = (String) selectedUser.get("status");
                return userService.updateUserStatus(uid, "ACTIVE".equals(current) ? "SUSPENDED" : "ACTIVE");
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) asyncLoadUsers("");
            else showAlert("失敗", "狀態更新失敗。");
        });
        new Thread(task).start();
    }

    private void handleAsyncChangeUserRole(String newRole) {
        Map<String, Object> selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) { showAlert("提示", "請先選取一名讀者。"); return; }
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                int uid = (Integer) selectedUser.get("userId");
                return userService.updateUserRole(uid, newRole);
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                showAlert("成功", "已變更該讀者身分為 " + newRole);
                asyncLoadUsers("");
            } else showAlert("失敗", "身分變更失敗。");
        });
        new Thread(task).start();
    }

    private void handleShowUserDetail() {
        Map<String, Object> selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) { showAlert("提示", "請先選取一名讀者。"); return; }

        Task<User> task = new Task<>() {
            @Override protected User call() {
                int uid = (Integer) selectedUser.get("userId");
                User u = userService.getUserById(uid);
                if (u == null) {
                    u = userService.getUserByStudentNo((String) selectedUser.get("studentNo"));
                }
                return u;
            }
        };
        task.setOnSucceeded(e -> {
            User u = task.getValue();
            if (u == null) { showAlert("查無此人", "無法取得該讀者詳細資料。"); return; }

            int unpaidTotal = fineService.getTotalUnpaidAmount(u.getUserId());

            String info = String.format(
                    "學號：%s\n姓名：%s\n身分：%s\n帳號狀態：%s\n未繳罰款總額：NT$ %d",
                    u.getStudentNo(), u.getName(),
                    u.getRoleLevel().name(), u.getStatus().name(),
                    unpaidTotal
            );
            showAlert("讀者詳情 — " + u.getName(), info);
        });
        new Thread(task).start();
    }

    private void handleAsyncAdminPayFine() {
        Fine selectedFine = globalFineTable.getSelectionModel().getSelectedItem();
        if (selectedFine == null) { showAlert("提示", "請先從罰款列表中選取一筆。"); return; }
        if (selectedFine.isPaid())  { showAlert("提示", "此筆罰款已繳清。"); return; }

        Task<String> task = new Task<>() {
            @Override protected String call() {
                return fineService.payFine(selectedFine.getFineId());
            }
        };
        task.setOnSucceeded(e -> {
            String result = task.getValue();
            if ("SUCCESS_AND_ACTIVATED".equals(result)) {
                showAlert("代繳成功", "罰款已收訖，該學生帳號已自動復權。");
            } else if ("SUCCESS".equals(result)) {
                showAlert("代繳成功", "罰款已收訖，該學生名下仍有其他未繳罰單。");
            } else {
                showAlert("代繳失敗", result);
            }
            asyncLoadRecords("");
        });
        new Thread(task).start();
    }

    private void showAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }
}