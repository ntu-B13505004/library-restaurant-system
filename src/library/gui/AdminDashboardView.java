package library.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.*;
import library.model.*;
import library.service.UserService;
import library.service.BorrowService;
import library.service.BookService;
import library.service.ReportService; // ✨ 引入 ReportService 處理儀表板數據

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理員後台主畫面
 */
public class AdminDashboardView {

    private final Stage stage;
    private final Map<String, Object> session;

    // ✨ 精準對接現有的 Service 模組
    private final UserService    userService   = new UserService();
    private final BorrowService  borrowService = new BorrowService();
    private final BookService    bookService   = new BookService();
    private final ReportService  reportService = new ReportService();

    private final StackPane contentArea = new StackPane();
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    public AdminDashboardView(Stage stage, Map<String, Object> session) {
        this.stage   = stage;
        this.session = session;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(contentArea);

        showDashboard();

        Scene scene = new Scene(root, 1200, 720);
        stage.setTitle("圖書館管理後台");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    // ── 側邊欄 ────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(230);
        sidebar.setStyle(AppStyle.sidebar());

        VBox adminInfo = new VBox(6);
        adminInfo.setPadding(new Insets(28, 20, 24, 20));
        adminInfo.setStyle("-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 0 0 1 0;");

        Label icon = new Label("🛡");
        icon.setStyle("-fx-font-size: 36px;");
        Label name = new Label("系統管理員");
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label adminChip = new Label("ADMIN");
        adminChip.setStyle(
                "-fx-background-color: " + AppStyle.WARNING + ";"
                        + "-fx-text-fill: white;"
                        + "-fx-font-size: 11px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-background-radius: 20;"
                        + "-fx-padding: 3 10 3 10;"
        );
        adminInfo.getChildren().addAll(icon, name, adminChip);

        VBox nav = new VBox(4);
        nav.setPadding(new Insets(16, 12, 16, 12));
        VBox.setVgrow(nav, Priority.ALWAYS);

        Button dashBtn   = navBtn("📊  儀表板");
        Button booksBtn  = navBtn("📚  書籍管理");
        Button recordBtn = navBtn("📋  借還紀錄");
        Button userBtn   = navBtn("👥  使用者管理");

        dashBtn.setOnAction(e   -> showDashboard());
        booksBtn.setOnAction(e  -> showBookManagement());
        recordBtn.setOnAction(e -> showRecordQuery());
        userBtn.setOnAction(e   -> showUserManagement());

        nav.getChildren().addAll(dashBtn, booksBtn, recordBtn, userBtn);

        VBox bottomBar = new VBox();
        bottomBar.setPadding(new Insets(12));
        Button logoutBtn = new Button("登出");
        logoutBtn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.1);"
                        + "-fx-text-fill: rgba(255,255,255,0.7);"
                        + "-fx-font-size: 13px;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 10 16;"
                        + "-fx-cursor: hand;"
        );
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> new LoginView(stage).show());
        bottomBar.getChildren().add(logoutBtn);

        sidebar.getChildren().addAll(adminInfo, nav, bottomBar);
        return sidebar;
    }

    private Button navBtn(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-text-fill: rgba(255,255,255,0.85);"
                        + "-fx-font-size: 14px;"
                        + "-fx-alignment: CENTER_LEFT;"
                        + "-fx-background-radius: 8;"
                        + "-fx-padding: 12 16;"
                        + "-fx-cursor: hand;"
        );
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    // ── 儀表板 KPI ────────────────────────────────────
    private void showDashboard() {
        VBox page = new VBox(24);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        Label heading = new Label("📊  圖書館儀表板");
        heading.setStyle(AppStyle.labelTitle());

        // ✨ 修正：改由 ReportService 獲取 KPI 數據
        Map<String, Integer> kpis = reportService.getLibraryGeneralKPIs();
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
                kpiCard("總藏書量",    kpis.getOrDefault("總藏書量",    0) + " 本", AppStyle.INFO),
                kpiCard("目前借出",    kpis.getOrDefault("目前借出冊數",0) + " 冊", AppStyle.WARNING),
                kpiCard("停權學生數",  kpis.getOrDefault("停權學生人數",0) + " 人", AppStyle.DANGER)
        );

        // ✨ 修正：改由 ReportService 獲取罰款摘要數據
        Map<String, Object> fines = reportService.getLibraryFineSummary();
        HBox fineRow = new HBox(16);
        fineRow.getChildren().addAll(
                kpiCard("歷史罰單總數",  fines.getOrDefault("歷史罰單總總數",  0) + " 件", AppStyle.TEXT_SECONDARY),
                kpiCard("未繳罰款總額",  fines.getOrDefault("未繳罰款總金額",  0) + " 元", AppStyle.DANGER)
        );

        page.getChildren().addAll(heading, kpiRow, fineRow);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.getChildren().setAll(scroll);
    }

    private VBox kpiCard(String title, String value, String color) {
        VBox card = new VBox(8);
        card.setStyle(AppStyle.card());
        card.setPadding(new Insets(20, 24, 20, 24));
        card.setPrefWidth(220);

        Label val = new Label(value);
        val.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.TEXT_SECONDARY + ";");

        card.getChildren().addAll(val, lbl);
        return card;
    }

    // ── 書籍管理 ──────────────────────────────────────
    private void showBookManagement() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        Label heading = new Label("📚  書籍管理");
        heading.setStyle(AppStyle.labelTitle());

        Button addBtn = new Button("＋ 新增書籍");
        addBtn.setStyle(AppStyle.btnPrimary());
        addBtn.setOnAction(e -> showAddBookDialog());

        HBox topBar = new HBox(12, addBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);

        TableView<Book> table = buildAdminBookTable();
        List<Book> books = bookService.getAllBooks();
        table.setItems(FXCollections.observableArrayList(books));
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox actionBar = new HBox(12);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        Button removeBtn = new Button("🗑 下架選中書籍");
        removeBtn.setStyle(AppStyle.btnDanger());
        Label removeMsg = new Label("");
        removeMsg.setStyle("-fx-font-size:13px;");

        removeBtn.setOnAction(e -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { setMsg(removeMsg, "請先選取書籍。", false); return; }
            boolean ok = bookService.removeBook(selected.getBookId());
            if (ok) {
                setMsg(removeMsg, "✅ 書籍已成功下架。", true);
                table.setItems(FXCollections.observableArrayList(bookService.getAllBooks()));
            } else {
                setMsg(removeMsg, "❌ 下架失敗：該書可能仍在借出中。", false);
            }
        });
        actionBar.getChildren().addAll(removeBtn, removeMsg);

        page.getChildren().addAll(heading, topBar, table, actionBar);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.getChildren().setAll(scroll);
    }

    private TableView<Book> buildAdminBookTable() {
        TableView<Book> table = new TableView<>();
        table.setStyle(AppStyle.tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(400);

        TableColumn<Book, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        colId.setMaxWidth(60);

        TableColumn<Book, String> colTitle = new TableColumn<>("書名");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> colAuthor = new TableColumn<>("作者");
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("authors"));
        colAuthor.setPrefWidth(120);

        TableColumn<Book, String> colPublisher = new TableColumn<>("出版者");
        colPublisher.setCellValueFactory(new PropertyValueFactory<>("publisher"));
        colPublisher.setPrefWidth(120);

        TableColumn<Book, String> colStatus = new TableColumn<>("狀態");
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getStatus() == BookStatus.AVAILABLE ? "可借閱" : "已借出")
        );
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                chip.setStyle("可借閱".equals(item) ? AppStyle.chipAvailable() : AppStyle.chipBorrowed());
                setGraphic(chip); setText(null);
            }
        });
        colStatus.setMaxWidth(90);

        table.getColumns().addAll(colId, colTitle, colAuthor, colPublisher, colStatus);
        return table;
    }

    private void showAddBookDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("新增書籍");

        VBox box = new VBox(14);
        box.setPadding(new Insets(28));
        box.setStyle(AppStyle.pageBackground());
        box.setMinWidth(400);

        Label heading = new Label("新增書籍");
        heading.setStyle(AppStyle.labelTitle());

        TextField titleField     = dialogField("書名*", "必填");
        TextField authorsField   = dialogField("作者", "多位作者以逗號分隔");
        TextField subjectsField  = dialogField("主題", "");
        TextField publisherField = dialogField("出版者", "");
        TextField yearField      = dialogField("出版年", "");
        TextField isbnField      = dialogField("ISBN", "");

        Label msg = new Label("");
        msg.setStyle("-fx-font-size:13px;");

        Button saveBtn = new Button("儲存書籍");
        saveBtn.setStyle(AppStyle.btnPrimary());
        Button cancelBtn = new Button("取消");
        cancelBtn.setStyle(AppStyle.btnOutline());
        HBox btnRow = new HBox(12, saveBtn, cancelBtn);

        saveBtn.setOnAction(e -> {
            String titleVal = titleField.getText().trim();
            if (titleVal.isEmpty()) { setMsg(msg, "書名為必填欄位。", false); return; }

            Book newBook = new Book(
                    0, titleVal,
                    authorsField.getText().trim(),
                    subjectsField.getText().trim(),
                    publisherField.getText().trim(),
                    yearField.getText().trim(),
                    "", "", "", ""
            );
            // 若需要連同 ISBN 一起存入，您可以在這裡設定 newBook.addIsbn(isbnField.getText().trim());

            boolean ok = bookService.addBook(newBook);
            if (ok) {
                setMsg(msg, "✅ 書籍新增成功！", true);
                saveBtn.setDisable(true);
                new Thread(() -> {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> { dialog.close(); showBookManagement(); });
                }).start();
            } else {
                setMsg(msg, "❌ 新增失敗，請確認欄位格式。", false);
            }
        });
        cancelBtn.setOnAction(e -> dialog.close());

        box.getChildren().addAll(
                heading,
                labeledField("書名*", titleField),
                labeledField("作者", authorsField),
                labeledField("主題", subjectsField),
                labeledField("出版者", publisherField),
                labeledField("出版年", yearField),
                labeledField("ISBN", isbnField),
                msg, btnRow
        );

        dialog.setScene(new Scene(box));
        dialog.show();
    }

    private TextField dialogField(String label, String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(AppStyle.textField());
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private VBox labeledField(String label, javafx.scene.Node field) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");
        return new VBox(4, lbl, field);
    }

    // ── 借還紀錄查詢 ──────────────────────────────────
    private void showRecordQuery() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        Label heading = new Label("📋  全體借還紀錄查詢");
        heading.setStyle(AppStyle.labelTitle());

        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        TextField snoFilter = new TextField();
        snoFilter.setPromptText("輸入學號篩選（留空顯示全部）");
        snoFilter.setStyle(AppStyle.textField());
        snoFilter.setPrefWidth(300);
        Button queryBtn = new Button("查詢");
        queryBtn.setStyle(AppStyle.btnPrimary());
        filterBar.getChildren().addAll(snoFilter, queryBtn);

        TableView<BorrowRecord> table = buildAllRecordTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable doQuery = () -> {
            String sno = snoFilter.getText().trim();
            // ⚠️ 備註：此處需在 BorrowService 中新增 getAllBorrowRecords 方法
            // List<BorrowRecord> records = borrowService.getAllBorrowRecords(sno.isEmpty() ? null : sno);
            // table.setItems(FXCollections.observableArrayList(records));
        };

        queryBtn.setOnAction(e -> doQuery.run());
        doQuery.run();

        page.getChildren().addAll(heading, filterBar, table);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.getChildren().setAll(scroll);
    }

    private TableView<BorrowRecord> buildAllRecordTable() {
        TableView<BorrowRecord> table = new TableView<>();
        table.setStyle(AppStyle.tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(420);
        table.setPlaceholder(new Label("目前沒有借還紀錄"));

        TableColumn<BorrowRecord, String> colUser = new TableColumn<>("借閱者");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getUser().getName() + "（" + d.getValue().getUser().getStudentNo() + "）"
        ));

        TableColumn<BorrowRecord, String> colBook = new TableColumn<>("書名");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBook().getTitle()));

        TableColumn<BorrowRecord, String> colBorrow = new TableColumn<>("借出");
        colBorrow.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getBorrowDate() != null ? d.getValue().getBorrowDate().format(dtf) : "—")
        );
        colBorrow.setPrefWidth(140);

        TableColumn<BorrowRecord, String> colDue = new TableColumn<>("到期");
        colDue.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDueDate() != null ? d.getValue().getDueDate().format(dtf) : "—")
        );
        colDue.setPrefWidth(140);

        TableColumn<BorrowRecord, String> colReturn = new TableColumn<>("歸還");
        colReturn.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getReturnDate() != null ? d.getValue().getReturnDate().format(dtf) : "（未還）")
        );
        colReturn.setPrefWidth(140);

        TableColumn<BorrowRecord, String> colStatus = new TableColumn<>("狀態");
        colStatus.setCellValueFactory(d -> {
            BorrowRecord r = d.getValue();
            if (r.isOverdue()) return new SimpleStringProperty("逾期");
            if (r.getReturnDate() != null) return new SimpleStringProperty("已還");
            return new SimpleStringProperty("借閱中");
        });
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                switch (item) {
                    case "逾期"   -> chip.setStyle(AppStyle.chipSuspended());
                    case "已還"   -> chip.setStyle(AppStyle.chipAvailable());
                    default       -> chip.setStyle(AppStyle.chipBorrowed());
                }
                setGraphic(chip); setText(null);
            }
        });
        colStatus.setMaxWidth(90);

        table.getColumns().addAll(colUser, colBook, colBorrow, colDue, colReturn, colStatus);
        return table;
    }

    // ── 使用者管理 ────────────────────────────────────
    private void showUserManagement() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        Label heading = new Label("👥  使用者管理");
        heading.setStyle(AppStyle.labelTitle());

        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        TextField filter = new TextField();
        filter.setPromptText("依學號搜尋（留空顯示全部）");
        filter.setStyle(AppStyle.textField());
        filter.setPrefWidth(280);
        Button searchBtn = new Button("搜尋");
        searchBtn.setStyle(AppStyle.btnPrimary());
        searchBar.getChildren().addAll(filter, searchBtn);

        TableView<Map<String, Object>> table = buildUserTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable doSearch = () -> {
            String sno = filter.getText().trim();
            // ✨ 修正：改由 UserService 負責調用使用者列表
            List<Map<String, Object>> users = userService.getAllStudents(sno.isEmpty() ? null : sno);
            table.setItems(FXCollections.observableArrayList(users));
        };
        searchBtn.setOnAction(e -> doSearch.run());
        doSearch.run();

        HBox actionBar = new HBox(12);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        Button suspendBtn = new Button("🔒 停權");
        suspendBtn.setStyle(AppStyle.btnDanger());
        Button activateBtn = new Button("🔓 復權");
        activateBtn.setStyle(AppStyle.btnSuccess());

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("NORMAL", "VIP");
        roleCombo.setValue("NORMAL");
        roleCombo.setStyle("-fx-font-size: 13px;");

        Button changeRoleBtn = new Button("變更等級");
        changeRoleBtn.setStyle(AppStyle.btnOutline());

        Label actionMsg = new Label("");
        actionMsg.setStyle("-fx-font-size:13px;");

        suspendBtn.setOnAction(e -> {
            Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { setMsg(actionMsg, "請先選取一位使用者。", false); return; }
            int uid = (int) selected.get("userId");
            // ✨ 修正：使用 UserService 變更狀態
            boolean ok = userService.updateUserStatus(uid, "SUSPENDED");
            setMsg(actionMsg, ok ? "✅ 已停權" : "❌ 操作失敗", ok);
            if (ok) doSearch.run();
        });

        activateBtn.setOnAction(e -> {
            Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { setMsg(actionMsg, "請先選取一位使用者。", false); return; }
            int uid = (int) selected.get("userId");
            boolean ok = userService.updateUserStatus(uid, "ACTIVE");
            setMsg(actionMsg, ok ? "✅ 已復權" : "❌ 操作失敗", ok);
            if (ok) doSearch.run();
        });

        changeRoleBtn.setOnAction(e -> {
            Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { setMsg(actionMsg, "請先選取一位使用者。", false); return; }
            int uid = (int) selected.get("userId");
            UserRole newRole = UserRole.valueOf(roleCombo.getValue());

            // ⚠️ 備註：UserService 中需封裝一層呼叫 UserRepository.updateUserRole 的方法
            // boolean ok = userService.updateUserRole(uid, newRole);
            // setMsg(actionMsg, ok ? "✅ 等級已更新為 " + newRole : "❌ 操作失敗", ok);
            // if (ok) doSearch.run();
        });

        actionBar.getChildren().addAll(suspendBtn, activateBtn, roleCombo, changeRoleBtn, actionMsg);
        page.getChildren().addAll(heading, searchBar, table, actionBar);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.getChildren().setAll(scroll);
    }

    private TableView<Map<String, Object>> buildUserTable() {
        TableView<Map<String, Object>> table = new TableView<>();
        table.setStyle(AppStyle.tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(380);
        table.setPlaceholder(new Label("查無使用者"));

        TableColumn<Map<String, Object>, String> colSno = new TableColumn<>("學號");
        colSno.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("studentNo")));

        TableColumn<Map<String, Object>, String> colName = new TableColumn<>("姓名");
        colName.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("name")));

        TableColumn<Map<String, Object>, String> colRole = new TableColumn<>("等級");
        colRole.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("roleLevel")));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                chip.setStyle("VIP".equalsIgnoreCase(item) ? AppStyle.chipVip()
                        : "-fx-background-color: " + AppStyle.BORDER + "; -fx-text-fill: " + AppStyle.TEXT_SECONDARY
                        + "; -fx-font-size: 12px; -fx-background-radius: 20; -fx-padding: 3 10 3 10;");
                setGraphic(chip); setText(null);
            }
        });
        colRole.setMaxWidth(90);

        TableColumn<Map<String, Object>, String> colStatus = new TableColumn<>("狀態");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty((String) d.getValue().get("status")));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                chip.setStyle("ACTIVE".equalsIgnoreCase(item) ? AppStyle.chipAvailable() : AppStyle.chipSuspended());
                setGraphic(chip); setText(null);
            }
        });
        colStatus.setMaxWidth(100);

        table.getColumns().addAll(colSno, colName, colRole, colStatus);
        return table;
    }

    // ── 工具 ──────────────────────────────────────────
    private void setMsg(Label lbl, String text, boolean success) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (success ? AppStyle.SUCCESS : AppStyle.DANGER) + ";");
    }
}