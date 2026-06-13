package library.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import library.model.*;
import library.service.BorrowService;
import library.service.BookService;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 一般使用者（NORMAL / VIP）主畫面
 *
 * 左側：側邊欄導航
 * 右側：可切換的功能頁面
 * - 首頁（到期提醒 + 快速入口）
 * - 書籍搜尋與借閱
 * - 我的借閱紀錄
 */
public class UserDashboardView {

    private final Stage stage;
    private final Map<String, Object> session;
    private final BorrowService borrowService = new BorrowService();
    private final BookService bookService     = new BookService();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    // 主內容區域（可切換）
    private final StackPane contentArea = new StackPane();

    public UserDashboardView(Stage stage, Map<String, Object> session) {
        this.stage   = stage;
        this.session = session;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(contentArea);

        // 預設顯示首頁
        showHomePage();

        Scene scene = new Scene(root, 1100, 700);
        stage.setTitle("圖書館系統 - " + session.get("name"));
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    // ── 側邊欄 ────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(220);
        sidebar.setStyle(AppStyle.sidebar());

        // 頂部使用者資訊
        VBox userInfo = new VBox(6);
        userInfo.setPadding(new Insets(28, 20, 24, 20));
        userInfo.setStyle("-fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 0 0 1 0;");

        Label icon = new Label("👤");
        icon.setStyle("-fx-font-size: 32px;");

        Label nameLabel = new Label((String) session.get("name"));
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        String role = (String) session.get("roleLevel");
        Label roleChip = new Label("VIP".equalsIgnoreCase(role) ? "⭐ VIP 會員" : "普通會員");
        roleChip.setStyle(
                "VIP".equalsIgnoreCase(role) ? AppStyle.chipVip()
                        : "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;"
                        + "-fx-font-size: 11px; -fx-background-radius: 20; -fx-padding: 3 10 3 10;"
        );

        Label snoLabel = new Label("學號：" + session.get("studentNo"));
        snoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.55);");

        userInfo.getChildren().addAll(icon, nameLabel, roleChip, snoLabel);

        // 導航按鈕
        VBox navButtons = new VBox(4);
        navButtons.setPadding(new Insets(16, 12, 16, 12));
        VBox.setVgrow(navButtons, Priority.ALWAYS);

        Button homeBtn   = navBtn("🏠  首頁");
        Button searchBtn = navBtn("🔍  書籍搜尋 / 借閱");
        Button recordBtn = navBtn("📋  我的借閱紀錄");

        homeBtn.setOnAction(e   -> showHomePage());
        searchBtn.setOnAction(e -> showSearchPage());
        recordBtn.setOnAction(e -> showRecordPage());

        navButtons.getChildren().addAll(homeBtn, searchBtn, recordBtn);

        // 底部登出
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
                        + "-fx-max-width: Infinity;"
        );
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> new LoginView(stage).show());
        bottomBar.getChildren().add(logoutBtn);

        sidebar.getChildren().addAll(userInfo, navButtons, bottomBar);
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
                        + "-fx-max-width: Infinity;"
        );
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace("transparent", "rgba(255,255,255,0.1)")));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle().replace("rgba(255,255,255,0.1)", "transparent")));
        return btn;
    }

    // ── 首頁 ──────────────────────────────────────────
    private void showHomePage() {
        VBox page = new VBox(24);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        // 歡迎標題
        Label welcome = new Label("歡迎，" + session.get("name") + " 👋");
        welcome.setStyle(AppStyle.labelTitle());

        // 到期提醒卡片
        VBox dueCard = buildCard("⏰  即將到期提醒（3天內）", buildDueReminderContent());

        // 快速入口
        HBox quickLinks = new HBox(16);
        quickLinks.getChildren().addAll(
                quickCard("🔍 搜尋書籍", "查詢並借閱館藏書目", () -> showSearchPage()),
                quickCard("📋 借閱紀錄", "查看個人歷史紀錄",   () -> showRecordPage())
        );

        page.getChildren().addAll(welcome, dueCard, quickLinks);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contentArea.getChildren().setAll(scroll);
    }

    private VBox buildCard(String title, javafx.scene.Node content) {
        VBox card = new VBox(12);
        card.setStyle(AppStyle.card());
        card.setPadding(new Insets(20, 24, 20, 24));

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";"
        );
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + AppStyle.BORDER + ";");

        card.getChildren().addAll(titleLbl, sep, content);
        return card;
    }

    private javafx.scene.Node buildDueReminderContent() {
        int userId = (int) session.get("userId");

        // 修正：透過 BorrowService 撈取歷史紀錄，並利用 BorrowRecord 內建的 isUpcomingDue 進行本地過濾
        List<BorrowRecord> history = borrowService.getUserBorrowHistory(userId);
        List<BorrowRecord> records = history.stream()
                .filter(r -> r.getReturnDate() == null && r.isUpcomingDue(3))
                .collect(Collectors.toList());

        if (records.isEmpty()) {
            Label ok = new Label("✅ 目前沒有即將到期的書籍，請繼續保持！");
            ok.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.SUCCESS + ";");
            return ok;
        }

        VBox list = new VBox(10);
        for (BorrowRecord r : records) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle(
                    "-fx-background-color: #FFF8E1;"
                            + "-fx-background-radius: 8;"
                            + "-fx-padding: 10 14;"
            );
            Label bookName = new Label("《" + r.getBook().getTitle() + "》");
            bookName.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
            Label due = new Label("到期：" + (r.getDueDate() != null ? r.getDueDate().format(dtf) : "—"));
            due.setStyle("-fx-font-size: 12px; -fx-text-fill: " + AppStyle.WARNING + ";");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(bookName, spacer, due);
            list.getChildren().add(row);
        }
        return list;
    }

    private VBox quickCard(String title, String desc, Runnable action) {
        VBox card = new VBox(8);
        card.setStyle(AppStyle.card());
        card.setPadding(new Insets(20));
        card.setPrefWidth(220);
        card.setOnMouseClicked(e -> action.run());
        card.setStyle(
                AppStyle.card()
                        + "-fx-cursor: hand;"
        );

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.PRIMARY + ";");
        Label d = new Label(desc);
        d.setStyle(AppStyle.labelSubtitle());
        d.setWrapText(true);

        card.getChildren().addAll(t, d);
        return card;
    }

    // ── 書籍搜尋頁 ────────────────────────────────────
    private void showSearchPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        Label heading = new Label("🔍  書籍搜尋與借閱");
        heading.setStyle(AppStyle.labelTitle());

        // 搜尋列
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        TextField kw = new TextField();
        kw.setPromptText("輸入書名、作者、ISBN、主題…");
        kw.setStyle(AppStyle.textField());
        kw.setPrefWidth(360);
        Button searchBtn = new Button("搜尋");
        searchBtn.setStyle(AppStyle.btnPrimary());
        searchBar.getChildren().addAll(kw, searchBtn);

        // 書籍表格
        TableView<Book> table = buildBookTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        ObservableList<Book> data = FXCollections.observableArrayList(bookService.getAllBooks());
        table.setItems(data);

        // 底部借閱面板
        HBox borrowBar = new HBox(12);
        borrowBar.setAlignment(Pos.CENTER_LEFT);
        borrowBar.setPadding(new Insets(10, 0, 0, 0));

        Label daysLabel = new Label("租借天數：");
        daysLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        ComboBox<Integer> daysCombo = new ComboBox<>();
        daysCombo.getItems().addAll(1, 3, 7, 14);
        daysCombo.setValue(7);
        daysCombo.setStyle("-fx-font-size: 13px;");

        // 如果是普通會員，14天選項不可用
        String role = (String) session.get("roleLevel");
        if (!"VIP".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            daysCombo.getItems().remove(Integer.valueOf(14));
        }

        Button borrowBtn = new Button("借閱選中的書");
        borrowBtn.setStyle(AppStyle.btnPrimary());

        Label resultMsg = new Label("");
        resultMsg.setStyle("-fx-font-size: 13px;");
        resultMsg.setWrapText(true);

        borrowBar.getChildren().addAll(daysLabel, daysCombo, borrowBtn, resultMsg);

        // 事件：搜尋
        Runnable doSearch = () -> {
            String keyword = kw.getText().trim();
            List<Book> results = bookService.searchBooks(keyword);
            data.setAll(results);
            if (results.isEmpty()) {
                resultMsg.setText("❌ 未找到符合「" + keyword + "」的書籍。");
                resultMsg.setStyle("-fx-font-size:13px; -fx-text-fill:" + AppStyle.DANGER + ";");
            } else {
                resultMsg.setText("共找到 " + results.size() + " 筆結果。");
                resultMsg.setStyle("-fx-font-size:13px; -fx-text-fill:" + AppStyle.TEXT_SECONDARY + ";");
            }
        };
        searchBtn.setOnAction(e -> doSearch.run());
        kw.setOnAction(e -> doSearch.run());

        // 事件：借閱
        borrowBtn.setOnAction(e -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setMsg(resultMsg, "請先在表格中選取一本書。", false);
                return;
            }
            int numDays = daysCombo.getValue();
            int userId  = (int) session.get("userId");
            String res  = borrowService.borrowBook(userId, selected.getBookId(), numDays);

            if ("SUCCESS".equals(res)) {
                setMsg(resultMsg, "✅ 借閱成功！《" + selected.getTitle() + "》，天數：" + numDays + "天", true);
                doSearch.run(); // 重新整理列表
            } else {
                setMsg(resultMsg, res, false);
            }
        });

        page.getChildren().addAll(heading, searchBar, table, borrowBar);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.getChildren().setAll(scroll);
    }

    private TableView<Book> buildBookTable() {
        TableView<Book> table = new TableView<>();
        table.setStyle(AppStyle.tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(380);

        TableColumn<Book, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        colId.setMaxWidth(60);

        TableColumn<Book, String> colTitle = new TableColumn<>("書名");
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> colAuthor = new TableColumn<>("作者");
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("authors"));
        colAuthor.setPrefWidth(120);

        TableColumn<Book, String> colSubject = new TableColumn<>("主題");
        colSubject.setCellValueFactory(new PropertyValueFactory<>("subjects"));
        colSubject.setPrefWidth(120);

        TableColumn<Book, String> colYear = new TableColumn<>("出版年");
        colYear.setCellValueFactory(new PropertyValueFactory<>("publishYear"));
        colYear.setMaxWidth(80);

        TableColumn<Book, String> colStatus = new TableColumn<>("狀態");
        colStatus.setCellValueFactory(d ->
                new SimpleStringProperty(
                        d.getValue().getStatus() == BookStatus.AVAILABLE ? "可借閱" : "已借出"
                )
        );
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                chip.setStyle("可借閱".equals(item) ? AppStyle.chipAvailable() : AppStyle.chipBorrowed());
                setGraphic(chip);
                setText(null);
            }
        });
        colStatus.setMaxWidth(90);

        table.getColumns().addAll(colId, colTitle, colAuthor, colSubject, colYear, colStatus);
        return table;
    }

    // ── 借閱紀錄頁 ────────────────────────────────────
    private void showRecordPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(32));
        page.setStyle(AppStyle.pageBackground());

        Label heading = new Label("📋  我的借閱紀錄");
        heading.setStyle(AppStyle.labelTitle());

        int userId = (int) session.get("userId");
        List<BorrowRecord> records = borrowService.getUserBorrowHistory(userId);

        TableView<BorrowRecord> table = buildRecordTable();
        table.setItems(FXCollections.observableArrayList(records));
        VBox.setVgrow(table, Priority.ALWAYS);

        // 還書操作區
        HBox returnBar = new HBox(12);
        returnBar.setAlignment(Pos.CENTER_LEFT);
        Button returnBtn = new Button("歸還選中的書");
        returnBtn.setStyle(AppStyle.btnSuccess());
        Label returnMsg = new Label("");
        returnMsg.setStyle("-fx-font-size:13px;");
        returnMsg.setWrapText(true);

        returnBtn.setOnAction(e -> {
            BorrowRecord selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                setMsg(returnMsg, "請先在表格中選取要歸還的紀錄。", false);
                return;
            }
            if (selected.getReturnDate() != null) {
                setMsg(returnMsg, "該書籍已歸還，無需重複操作。", false);
                return;
            }

            // 修正：依據您目前的 BorrowService 架構，returnBook 只需要書籍 ID 即可處理完交易與罰款
            String res = borrowService.returnBook(selected.getBook().getBookId());

            if (res != null && res.startsWith("SUCCESS")) {
                String extra = res.contains("FINE:") ? "⚠️ 書籍已逾期，罰款：" + res.split(":")[1] + " 元" : "";
                setMsg(returnMsg, "✅ 還書成功！《" + selected.getBook().getTitle() + "》 " + extra, true);
                List<BorrowRecord> updated = borrowService.getUserBorrowHistory(userId);
                table.setItems(FXCollections.observableArrayList(updated));
            } else {
                setMsg(returnMsg, res != null ? res : "❌ 還書失敗，請再試。", false);
            }
        });

        returnBar.getChildren().addAll(returnBtn, returnMsg);
        page.getChildren().addAll(heading, table, returnBar);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentArea.getChildren().setAll(scroll);
    }

    private TableView<BorrowRecord> buildRecordTable() {
        TableView<BorrowRecord> table = new TableView<>();
        table.setStyle(AppStyle.tableStyle());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMinHeight(360);
        table.setPlaceholder(new Label("目前沒有借閱紀錄"));

        TableColumn<BorrowRecord, String> colBook = new TableColumn<>("書名");
        colBook.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBook().getTitle()));

        TableColumn<BorrowRecord, String> colBorrow = new TableColumn<>("借出時間");
        colBorrow.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getBorrowDate() != null ? d.getValue().getBorrowDate().format(dtf) : "—")
        );
        colBorrow.setPrefWidth(150);

        TableColumn<BorrowRecord, String> colDue = new TableColumn<>("到期日");
        colDue.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getDueDate() != null ? d.getValue().getDueDate().format(dtf) : "—")
        );
        colDue.setPrefWidth(150);

        TableColumn<BorrowRecord, String> colReturn = new TableColumn<>("歸還時間");
        colReturn.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getReturnDate() != null ? d.getValue().getReturnDate().format(dtf) : "（未還）")
        );
        colReturn.setPrefWidth(150);

        TableColumn<BorrowRecord, String> colOverdue = new TableColumn<>("狀態");
        colOverdue.setCellValueFactory(d -> {
            BorrowRecord r = d.getValue();
            if (r.getReturnDate() != null && !r.isOverdue()) return new SimpleStringProperty("已還清");
            if (r.isOverdue()) return new SimpleStringProperty("逾期");
            return new SimpleStringProperty("借閱中");
        });
        colOverdue.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label chip = new Label(item);
                switch (item) {
                    case "逾期"  -> chip.setStyle(AppStyle.chipSuspended());
                    case "已還清" -> chip.setStyle(AppStyle.chipAvailable());
                    default      -> chip.setStyle(AppStyle.chipBorrowed());
                }
                setGraphic(chip);
                setText(null);
            }
        });
        colOverdue.setMaxWidth(90);

        table.getColumns().addAll(colBook, colBorrow, colDue, colReturn, colOverdue);
        return table;
    }

    // ── 工具方法 ──────────────────────────────────────
    private void setMsg(Label lbl, String text, boolean success) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (success ? AppStyle.SUCCESS : AppStyle.DANGER) + ";");
    }
}