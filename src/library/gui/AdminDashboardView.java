package library.gui;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import library.model.*;
import library.service.*;
import java.util.*;

public class AdminDashboardView {
    private final Stage stage;
    private final Map<String, Object> session;

    private final BookService bookService = new BookService();
    private final UserService userService = new UserService();
    private final BorrowService borrowService = new BorrowService();
    private final FineService fineService = new FineService();
    private final ReportService reportService = new ReportService();

    private final BorderPane mainLayout = new BorderPane();
    private final StackPane contentArea = new StackPane();

    public AdminDashboardView(Stage stage, Map<String, Object> session) {
        this.stage = stage;
        this.session = session;
    }

    public void show() {
        mainLayout.setStyle("-fx-background-color: " + AppStyle.BG_WINDOW + ";");

        // --- 左側管理導覽列 ---
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setStyle("-fx-background-color: #1A1B41;"); // 更深色專業管理者藍

        Label adminTitle = new Label("中央管理系統");
        adminTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        adminTitle.setStyle("-fx-text-fill: white;");

        Label statusLbl = new Label("權限: 系統管理員");
        statusLbl.setStyle("-fx-text-fill: " + AppStyle.SUCCESS + "; -fx-font-size: 12px;");
        VBox titleBox = new VBox(5, adminTitle, statusLbl);
        titleBox.setPadding(new Insets(0, 0, 20, 0));

        Button navKpiBtn = new Button("📊 營運指標與圖表");
        Button navBookBtn = new Button("📚 全院館藏管理");
        Button navUserBtn = new Button("👥 學生權限稽核");
        Button navLogBtn = new Button("🔍 全校借閱審查");
        Button logoutBtn = new Button("🚪 登出控制台");

        for (Button btn : new Button[]{navKpiBtn, navBookBtn, navUserBtn, navLogBtn, logoutBtn}) {
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setPrefHeight(40);
            btn.setAlignment(Pos.CENTER_LEFT);
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ECF0F1; -fx-font-size: 14px; -fx-cursor: hand;");
        }

        navKpiBtn.setOnAction(e -> switchPage(buildKpiPage()));
        navBookBtn.setOnAction(e -> switchPage(buildBookManagerPage()));
        navUserBtn.setOnAction(e -> switchPage(buildUserManagerPage()));
        navLogBtn.setOnAction(e -> switchPage(buildAuditPage()));
        logoutBtn.setOnAction(e -> new LoginView(stage).show());

        sidebar.getChildren().addAll(titleBox, new Separator(), navKpiBtn, navBookBtn, navUserBtn, navLogBtn, new Spacer(), logoutBtn);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);

        switchPage(buildKpiPage()); // 預設頁

        Scene scene = new Scene(mainLayout, 1150, 720);
        stage.setScene(scene);
        stage.setTitle("圖書館管理員核心後台");
        stage.centerOnScreen();
    }

    private void switchPage(VBox page) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    // --- 頁面 1: 營運指標與視覺化圖表 ---
    private VBox buildKpiPage() {
        VBox box = new VBox(25);
        box.setPadding(new Insets(30));

        Label heading = new Label("圖書館關鍵績效指標 (KPI)");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);

        // 動態從資料庫撈取 KPI 摘要卡片
        Map<String, Integer> kpis = reportService.getLibraryGeneralKPIs();
        Map<String, Object> fines = reportService.getLibraryFineSummary();

        int i = 0;
        for (var entry : kpis.entrySet()) {
            VBox card = createKpiCard(entry.getKey(), String.valueOf(entry.getValue()), AppStyle.PRIMARY);
            grid.add(card, i++, 0);
        }
        if (fines.containsKey("未繳罰款總金額")) {
            grid.add(createKpiCard("未收逾期罰金", "$" + fines.get("未繳罰款總金額") + " 元", AppStyle.DANGER), i, 0);
        }

        // 核心亮點功能：加入分析書籍主題受歡迎程度的圓餅圖 (PieChart)
        Label chartTitle = new Label("📊 各主題文獻借閱熱度分佈統計");
        chartTitle.setFont(Font.font("System", FontWeight.BOLD, 16));

        PieChart pieChart = new PieChart();
        pieChart.setLegendSide(Side.RIGHT);

        Map<String, Integer> subjectData = reportService.getBookSubjectPopularity();
        for (var entry : subjectData.entrySet()) {
            pieChart.getData().add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + "次)", entry.getValue()));
        }

        VBox chartBox = new VBox(10, chartTitle, pieChart);
        chartBox.setStyle(AppStyle.card());
        chartBox.setPadding(new Insets(20));
        VBox.setVgrow(chartBox, Priority.ALWAYS);

        box.getChildren().addAll(heading, grid, chartBox);
        return box;
    }

    private VBox createKpiCard(String title, String value, String colorHex) {
        VBox card = new VBox(10);
        card.setStyle(AppStyle.card());
        card.setPadding(new Insets(20));
        card.setPrefWidth(200);

        Label tLbl = new Label(title);
        tLbl.setStyle("-fx-text-fill: " + AppStyle.TEXT_SECONDARY + "; -fx-font-size: 14px;");
        Label vLbl = new Label(value);
        vLbl.setFont(Font.font("System", FontWeight.BOLD, 24));
        vLbl.setStyle("-fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(tLbl, vLbl);
        return card;
    }

    // --- 頁面 2: 全院館藏管理 (上架/下架) ---
    private VBox buildBookManagerPage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label heading = new Label("全館文獻資產管理中心");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));

        // 書籍表格
        TableView<Book> table = new TableView<>();
        TableColumn<Book, Integer> idCol = new TableColumn<>("書籍ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        TableColumn<Book, String> tCol = new TableColumn<>("書名");
        tCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        tCol.setPrefWidth(200);
        TableColumn<Book, String> aCol = new TableColumn<>("作者");
        aCol.setCellValueFactory(new PropertyValueFactory<>("authors"));
        TableColumn<Book, String> sCol = new TableColumn<>("主題分類");
        sCol.setCellValueFactory(new PropertyValueFactory<>("subjects"));
        TableColumn<Book, String> stCol = new TableColumn<>("狀態");
        stCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));

        table.getColumns().addAll(idCol, tCol, aCol, sCol, stCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable refreshBooks = () -> table.setItems(FXCollections.observableArrayList(bookService.getAllBooks()));
        refreshBooks.run();

        // 表單：新書上架
        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField tIn = new TextField(); tIn.setPromptText("書名 (必填)"); tIn.setStyle(AppStyle.textField());
        TextField aIn = new TextField(); aIn.setPromptText("作者"); aIn.setStyle(AppStyle.textField());
        TextField sIn = new TextField(); sIn.setPromptText("主題"); sIn.setStyle(AppStyle.textField());
        Button addBtn = new Button("📥 新書採購上架"); addBtn.setStyle(AppStyle.buttonPrimary());
        Button delBtn = new Button("❌ 強制登錄下架"); delBtn.setStyle(AppStyle.buttonSecondary());

        form.getChildren().addAll(tIn, aIn, sIn, addBtn, delBtn);

        // 上架事件
        addBtn.setOnAction(e -> {
            String title = tIn.getText().trim();
            if (title.isEmpty()) { showAlert("錯誤", "書名為必填欄位！"); return; }
            Book b = new Book(0, title, aIn.getText(), sIn.getText(), "", "", "", "", "", "");
            if (bookService.addBook(b)) {
                showAlert("成功", "書籍《" + title + "》已成功登錄入庫。");
                refreshBooks.run();
                tIn.clear(); aIn.clear(); sIn.clear();
            }
        });

        // 下架事件 (包含借出中不能刪除的安全檢查)
        delBtn.setOnAction(e -> {
            Book sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { showAlert("提示", "請先選擇要下架的書籍。"); return; }

            boolean res = bookService.removeBook(sel.getBookId());
            if (res) {
                showAlert("成功", "該圖書館藏已安全除籍。");
                refreshBooks.run();
            } else {
                showAlert("除籍被拒", "❌ 無法下架該書籍！該圖書目前正被學生借閱中，移除將破壞內部稽核完整性。");
            }
        });

        box.getChildren().addAll(heading, table, form);
        return box;
    }

    // --- 頁面 3: 學生權限稽核 (狀態修改/角色調整) ---
    private VBox buildUserManagerPage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label heading = new Label("全校讀者權限與身分稽核");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));

        HBox filterBar = new HBox(10);
        TextField fIn = new TextField(); fIn.setPromptText("依學號精準過濾..."); fIn.setStyle(AppStyle.textField());
        Button filterBtn = new Button("過濾篩選"); filterBtn.setStyle(AppStyle.buttonPrimary());
        filterBar.getChildren().addAll(fIn, filterBtn);

        TableView<Map<String, Object>> table = new TableView<>();
        TableColumn<Map<String, Object>, Integer> idCol = new TableColumn<>("使用者ID");
        idCol.setCellValueFactory(cell -> new SimpleObjectProperty<>((Integer) cell.getValue().get("userId")));

        TableColumn<Map<String, Object>, String> snoCol = new TableColumn<>("學號");
        snoCol.setCellValueFactory(cell -> new SimpleStringProperty((String) cell.getValue().get("studentNo")));

        TableColumn<Map<String, Object>, String> nCol = new TableColumn<>("姓名");
        nCol.setCellValueFactory(cell -> new SimpleStringProperty((String) cell.getValue().get("name")));

        TableColumn<Map<String, Object>, String> rCol = new TableColumn<>("會員級別");
        rCol.setCellValueFactory(cell -> new SimpleStringProperty((String) cell.getValue().get("roleLevel")));

        TableColumn<Map<String, Object>, String> sCol = new TableColumn<>("權限狀態");
        sCol.setCellValueFactory(cell -> new SimpleStringProperty((String) cell.getValue().get("status")));
        sCol.setCellFactory(column -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label l = new Label(item);
                    l.setStyle("ACTIVE".equals(item) ? AppStyle.chipAvailable() : AppStyle.chipSuspended());
                    setGraphic(l);
                }
            }
        });

        table.getColumns().addAll(idCol, snoCol, nCol, rCol, sCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        Runnable loadUsers = () -> table.setItems(FXCollections.observableArrayList(userService.getAllStudents(fIn.getText())));
        filterBtn.setOnAction(e -> loadUsers.run());
        loadUsers.run();

        // 權限操作面板
        HBox ops = new HBox(15);
        ops.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> statusCombo = new ComboBox<>(); statusCombo.getItems().addAll("ACTIVE", "SUSPENDED"); statusCombo.setPromptText("變更狀態");
        Button updateStatusBtn = new Button("修改狀態"); updateStatusBtn.setStyle(AppStyle.buttonPrimary());

        ComboBox<String> roleCombo = new ComboBox<>(); roleCombo.getItems().addAll("NORMAL", "VIP"); roleCombo.setPromptText("升降身分");
        Button updateRoleBtn = new Button("調整級別"); updateRoleBtn.setStyle(AppStyle.buttonSecondary());

        ops.getChildren().addAll(new Label("狀態稽核:"), statusCombo, updateStatusBtn, new Separator(), new Label("級別調配:"), roleCombo, updateRoleBtn);

        updateStatusBtn.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel == null || statusCombo.getValue() == null) return;
            int uid = (int) sel.get("userId");
            if (userService.updateUserStatus(uid, statusCombo.getValue())) {
                showAlert("變更成功", "讀者狀態已切換為 " + statusCombo.getValue());
                loadUsers.run();
            }
        });

        updateRoleBtn.setOnAction(e -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel == null || roleCombo.getValue() == null) return;
            int uid = (int) sel.get("userId");
            if (userService.updateUserRole(uid, roleCombo.getValue())) {
                showAlert("變更成功", "讀者身分已成功轉發為 " + roleCombo.getValue());
                loadUsers.run();
            }
        });

        box.getChildren().addAll(heading, filterBar, table, ops);
        return box;
    }

    // --- 頁面 4: 全校借閱歷史審查 ---
    private VBox buildAuditPage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label heading = new Label("全校借閱交易流水審查紀錄");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));

        TableView<BorrowRecord> table = new TableView<>();
        TableColumn<BorrowRecord, Integer> idCol = new TableColumn<>("紀錄ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));

        TableColumn<BorrowRecord, String> stuCol = new TableColumn<>("借閱學生");
        stuCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getUser().getName() + " (" + cell.getValue().getUser().getStudentNo() + ")"));
        stuCol.setPrefWidth(160);

        TableColumn<BorrowRecord, String> bkCol = new TableColumn<>("借閱書名");
        bkCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBook().getTitle()));
        bkCol.setPrefWidth(180);

        TableColumn<BorrowRecord, String> bdCol = new TableColumn<>("借出日期");
        bdCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBorrowDate().toString().substring(0,16)));

        TableColumn<BorrowRecord, String> rdCol = new TableColumn<>("歸還狀態");
        rdCol.setCellValueFactory(cell -> {
            var d = cell.getValue().getReturnDate();
            return new SimpleStringProperty(d == null ? "⚠️ 在外未還" : "已歸還 (" + d.toString().substring(0,10) + ")");
        });
        rdCol.setPrefWidth(150);

        table.getColumns().addAll(idCol, stuCol, bkCol, bdCol, rdCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        table.setItems(FXCollections.observableArrayList(borrowService.getAllBorrowRecords(null)));

        box.getChildren().addAll(heading, table);
        return box;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static class Spacer extends Region {
        public Spacer() { VBox.setVgrow(this, Priority.ALWAYS); }
    }
}