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

        // 側邊欄設計
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

        Button navReportBtn = new Button("營運數據報表");
        Button navBookBtn = new Button("藏書維護與管理");
        Button navUserBtn = new Button("讀者權限審查");
        Button navRecordBtn = new Button("全館借閱與罰款");
        Button logoutBtn = new Button("登出系統");

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
        sidebar.getChildren().addAll(profileBox, new Separator(), navReportBtn, navBookBtn, navUserBtn, navRecordBtn, spacer, logoutBtn);

        // 路由綁定
        navReportBtn.setOnAction(e -> switchPage(buildReportPage()));
        navBookBtn.setOnAction(e -> { switchPage(buildBookManagementPage()); asyncLoadBooks(""); });
        navUserBtn.setOnAction(e -> { switchPage(buildUserManagementPage()); asyncLoadUsers(); });
        navRecordBtn.setOnAction(e -> { switchPage(buildGlobalRecordPage()); asyncLoadRecords(); });
        logoutBtn.setOnAction(e -> new LoginView(stage).show());

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);

        // 預設進入報表頁面
        switchPage(buildReportPage());

        Scene scene = new Scene(mainLayout, 1100, 700);
        stage.setScene(scene);
        stage.setTitle("智慧圖書館 - 最高權限管理後台");
        stage.show();
    }

    private void switchPage(Pane page) {
        contentArea.getChildren().setAll(page);
    }

    // ==========================================
    // 📊 營運數據報表頁面 (完全串接 ReportService)
    // ==========================================
    private Pane buildReportPage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label header = new Label("圖書館營運數據與 KPI 看板");
        header.setFont(Font.font("System", FontWeight.BOLD, 22));

        // KPI 看板區
        HBox kpiBox = new HBox(20);
        kpiBox.setAlignment(Pos.CENTER_LEFT);
        Map<String, Integer> kpis = reportService.getLibraryGeneralKPIs();
        Map<String, Object> finesSummary = reportService.getLibraryFineSummary();

        kpiBox.getChildren().addAll(
                createKpiCard("總藏書量", String.valueOf(kpis.getOrDefault("總藏書量", 0))),
                createKpiCard("在外借出", String.valueOf(kpis.getOrDefault("目前借出冊數", 0))),
                createKpiCard("停權人數", String.valueOf(kpis.getOrDefault("停權學生人數", 0))),
                createKpiCard("未收罰金", "$ " + finesSummary.getOrDefault("未繳罰款總金額", 0))
        );

        // 圖表區 (主題分佈圓餅圖 + 熱門書籍長條圖)
        HBox chartBox = new HBox(20);
        VBox.setVgrow(chartBox, Priority.ALWAYS);

        // 圓餅圖：書籍主題受歡迎度
        PieChart pieChart = new PieChart();
        pieChart.setTitle("熱門借閱主題分佈");
        Map<String, Integer> subjectData = reportService.getBookSubjectPopularity();
        subjectData.forEach((subject, count) -> pieChart.getData().add(new PieChart.Data(subject, count)));

        // 長條圖：Top 5 借閱書籍
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("全校熱門借閱排行榜 Top 5");
        barChart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Integer> topBooks = reportService.getTop5BorrowedBooks();
        topBooks.forEach((title, count) -> series.getData().add(new XYChart.Data<>(title, count)));
        barChart.getData().add(series);

        HBox.setHgrow(pieChart, Priority.ALWAYS);
        HBox.setHgrow(barChart, Priority.ALWAYS);
        chartBox.getChildren().addAll(pieChart, barChart);

        box.getChildren().addAll(header, kpiBox, new Separator(), chartBox);
        return box;
    }

    private VBox createKpiCard(String title, String value) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setPrefWidth(180);
        Label tLabel = new Label(title);
        tLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-size: 14px;");
        Label vLabel = new Label(value);
        vLabel.setFont(Font.font("System", FontWeight.BOLD, 24));
        vLabel.setStyle("-fx-text-fill: " + AppStyle.PRIMARY + ";");
        card.getChildren().addAll(tLabel, vLabel);
        return card;
    }

    // ==========================================
    // 📚 藏書維護與管理 (包含檢索與下架)
    // ==========================================
    private Pane buildBookManagementPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label header = new Label("📚 館藏圖書維護作業");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        // 搜尋列
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("輸入關鍵字搜尋書籍...");
        searchField.setStyle(AppStyle.textField());
        Button searchBtn = new Button("檢索");
        searchBtn.setStyle(AppStyle.buttonSecondary());
        searchBtn.setOnAction(e -> asyncLoadBooks(searchField.getText()));
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

        bookTable.getColumns().addAll(idCol, titleCol, authorCol, statusCol);

        // 管理操作區 (上架與下架)
        HBox opsBox = new HBox(10);
        opsBox.setAlignment(Pos.CENTER_LEFT);

        TextField tTitle = new TextField(); tTitle.setPromptText("新書書名"); tTitle.setStyle(AppStyle.textField());
        TextField tAuthor = new TextField(); tAuthor.setPromptText("作者"); tAuthor.setStyle(AppStyle.textField());
        TextField tSubject = new TextField(); tSubject.setPromptText("主題"); tSubject.setStyle(AppStyle.textField());

        Button addBtn = new Button("➕ 登記上架");
        addBtn.setStyle(AppStyle.buttonPrimary());
        addBtn.setOnAction(e -> handleAsyncAddBook(tTitle.getText(), tAuthor.getText(), tSubject.getText(), tTitle, tAuthor, tSubject));

        Button delBtn = new Button("🗑️ 下架選定書籍");
        delBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        delBtn.setOnAction(e -> handleAsyncRemoveBook());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        opsBox.getChildren().addAll(tTitle, tAuthor, tSubject, addBtn, spacer, delBtn);

        box.getChildren().addAll(header, searchBox, bookTable, new Separator(), opsBox);
        return box;
    }

    // ==========================================
    // 👥 讀者權限審查 (增加會員等級調整)
    // ==========================================
    private Pane buildUserManagementPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label header = new Label("👥 全校讀者狀態與權限審查");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        userTable = new TableView<>(usersData);
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(userTable, Priority.ALWAYS);

        TableColumn<Map<String, Object>, String> snoCol = new TableColumn<>("學號");
        snoCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("studentNo")));

        TableColumn<Map<String, Object>, String> unameCol = new TableColumn<>("姓名");
        unameCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("name")));

        TableColumn<Map<String, Object>, String> uRoleCol = new TableColumn<>("身分別");
        uRoleCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("roleLevel")));

        TableColumn<Map<String, Object>, String> uStatusCol = new TableColumn<>("帳號狀態");
        uStatusCol.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("status")));

        userTable.getColumns().addAll(snoCol, unameCol, uRoleCol, uStatusCol);

        HBox opsBox = new HBox(10);
        opsBox.setAlignment(Pos.CENTER_LEFT);

        Button toggleStatusBtn = new Button("⚡ 切換帳號狀態 (停權/復權)");
        toggleStatusBtn.setStyle(AppStyle.buttonSecondary());
        toggleStatusBtn.setOnAction(e -> handleAsyncToggleUserStatus());

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("NORMAL", "VIP");
        roleCombo.setValue("VIP");
        roleCombo.setStyle(AppStyle.textField());

        Button changeRoleBtn = new Button("變更選定讀者身分");
        changeRoleBtn.setStyle(AppStyle.buttonPrimary());
        changeRoleBtn.setOnAction(e -> handleAsyncChangeUserRole(roleCombo.getValue()));

        opsBox.getChildren().addAll(toggleStatusBtn, new Label(" | "), roleCombo, changeRoleBtn);

        box.getChildren().addAll(header, userTable, opsBox);
        return box;
    }

    // ==========================================
    // 📜 全館借閱與罰款總覽
    // ==========================================
    private Pane buildGlobalRecordPage() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(30));

        Label header = new Label("全館借閱紀錄與未繳罰款追蹤");
        header.setFont(Font.font("System", FontWeight.BOLD, 20));

        globalBorrowTable = new TableView<>(globalBorrowsData);
        globalBorrowTable.setPrefHeight(200);
        globalBorrowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BorrowRecord, String> bUserCol = new TableColumn<>("借閱人 (學號)");
        bUserCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUser().getStudentNo()));
        TableColumn<BorrowRecord, String> bBookCol = new TableColumn<>("借閱書籍");
        bBookCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBook().getTitle()));
        TableColumn<BorrowRecord, String> bStatusCol = new TableColumn<>("歸還狀態");
        bStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReturnDate() == null ? "未歸還" : "已歸還"));

        globalBorrowTable.getColumns().addAll(bUserCol, bBookCol, bStatusCol);

        Label fineHeader = new Label("全館未繳清罰單總覽");
        fineHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        fineHeader.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        globalFineTable = new TableView<>(globalFinesData);
        globalFineTable.setPrefHeight(200);
        globalFineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Fine, String> fUserCol = new TableColumn<>("違規學號");
        fUserCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRecord().getUser().getStudentNo()));
        TableColumn<Fine, String> fAmtCol = new TableColumn<>("未繳金額");
        fAmtCol.setCellValueFactory(c -> new SimpleStringProperty("$ " + c.getValue().getAmount()));

        globalFineTable.getColumns().addAll(fUserCol, fAmtCol);

        box.getChildren().addAll(header, new Label("所有借閱紀錄："), globalBorrowTable, new Separator(), fineHeader, globalFineTable);
        return box;
    }

    // ==========================================
    // 異步背景任務與事件綁定
    // ==========================================
    private void asyncLoadBooks(String keyword) {
        bookTable.setPlaceholder(new ProgressIndicator());
        Task<List<Book>> task = new Task<>() {
            @Override protected List<Book> call() throws Exception {
                return bookService.searchBooks(keyword);
            }
        };
        task.setOnSucceeded(e -> booksData.setAll(task.getValue()));
        new Thread(task).start();
    }

    private void asyncLoadUsers() {
        userTable.setPlaceholder(new ProgressIndicator());
        Task<List<Map<String, Object>>> task = new Task<>() {
            @Override protected List<Map<String, Object>> call() throws Exception {
                return userService.getAllStudents("");
            }
        };
        task.setOnSucceeded(e -> usersData.setAll(task.getValue()));
        new Thread(task).start();
    }

    private void asyncLoadRecords() {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                List<BorrowRecord> borrows = borrowService.getAllBorrowRecords("");
                List<Fine> fines = fineService.getAllUnpaidFines();
                Platform.runLater(() -> {
                    globalBorrowsData.setAll(borrows);
                    globalFinesData.setAll(fines);
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    private void handleAsyncAddBook(String title, String author, String subject, TextField... fields) {
        if (title.isEmpty() || author.isEmpty()) {
            showAlert("提示", "書名與作者為必填欄位。"); return;
        }
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() {
                return bookService.addBook(new Book(0, title, author, subject, "", "", "", "", "", ""));
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
            @Override protected Boolean call() { return bookService.removeBook(selected.getBookId()); }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                showAlert("成功", "書籍已成功下架！");
                asyncLoadBooks("");
            } else showAlert("失敗", "下架失敗，可能該書目前正被借出中。");
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
            if (task.getValue()) asyncLoadUsers();
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
                asyncLoadUsers();
            } else showAlert("失敗", "身分變更失敗。");
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