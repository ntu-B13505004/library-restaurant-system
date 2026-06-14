package library.gui;

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
import library.model.*;
import library.service.*;

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

    private final ObservableList<BorrowRecord> borrowRecordsData = FXCollections.observableArrayList();
    private final ObservableList<Fine> finesData = FXCollections.observableArrayList();
    private final ObservableList<Book> searchBooksData = FXCollections.observableArrayList();

    private TableView<BorrowRecord> borrowTable;
    private TableView<Fine> fineTable;
    private TableView<Book> searchTable;
    private Label totalFineLabel;

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
            handleAsyncSearch(""); // 預設載入全部書籍
        });
        navBorrowBtn.setOnAction(e -> switchPage(buildBorrowAndFinePage()));
        logoutBtn.setOnAction(e -> new LoginView(stage).show());

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);

        switchPage(buildBorrowAndFinePage());
        asyncRefreshUserData();

        Scene scene = new Scene(mainLayout, 1000, 650);
        stage.setScene(scene);
        stage.setTitle("智慧圖書館 - 學生個人主頁");
        stage.show();
    }

    private void switchPage(Pane page) {
        contentArea.getChildren().setAll(page);
    }

    // ==========================================
    // 我的借還與罰款控制面板頁面
    // ==========================================
    private Pane buildBorrowAndFinePage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label borrowHeading = new Label("📚 您目前的個人借閱狀況");
        borrowHeading.setFont(Font.font("System", FontWeight.BOLD, 18));

        borrowTable = new TableView<>(borrowRecordsData);
        borrowTable.setPrefHeight(220);
        borrowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<BorrowRecord, String> titleCol = new TableColumn<>("書名");
        titleCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBook().getTitle()));

        TableColumn<BorrowRecord, String> dateCol = new TableColumn<>("借閱日期");
        dateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBorrowDate().toString().substring(0, 10)));

        TableColumn<BorrowRecord, String> dueCol = new TableColumn<>("應還期限");
        dueCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDueDate().toString().substring(0, 10)));

        borrowTable.getColumns().addAll(titleCol, dateCol, dueCol);

        Button returnBtn = new Button("↩️ 辦理選定書籍還書");
        returnBtn.setStyle(AppStyle.buttonPrimary());
        returnBtn.setOnAction(e -> handleAsyncReturn());

        Label fineHeading = new Label("⚠️ 逾期未結清帳單明細");
        fineHeading.setFont(Font.font("System", FontWeight.BOLD, 18));
        fineHeading.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        fineTable = new TableView<>(finesData);
        fineTable.setPrefHeight(180);
        fineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Fine, String> fBookCol = new TableColumn<>("違規書籍");
        fBookCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRecord().getBook().getTitle()));

        TableColumn<Fine, String> fAmountCol = new TableColumn<>("累積罰金 (TWD)");
        fAmountCol.setCellValueFactory(cell -> new SimpleStringProperty("$ " + cell.getValue().getAmount()));

        fineTable.getColumns().addAll(fBookCol, fAmountCol);

        totalFineLabel = new Label("未繳總金額：計算中...");
        totalFineLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        totalFineLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        Button payBtn = new Button("💰 立即繳納選定帳單");
        payBtn.setStyle(AppStyle.buttonSecondary());
        payBtn.setOnAction(e -> handleAsyncPayFine());

        HBox fOps = new HBox(20, totalFineLabel, payBtn);
        fOps.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(borrowHeading, borrowTable, returnBtn, new Separator(), fineHeading, fOps, fineTable);
        return box;
    }

    // ==========================================
    // 圖書檢索與虛擬借閱頁面 (實作完成)
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

        searchBox.getChildren().addAll(searchBar, searchBtn);

        searchTable = new TableView<>(searchBooksData);
        searchTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(searchTable, Priority.ALWAYS);

        TableColumn<Book, String> titleCol = new TableColumn<>("書名");
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));

        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAuthors()));

        TableColumn<Book, String> statusCol = new TableColumn<>("狀態");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));

        searchTable.getColumns().addAll(titleCol, authorCol, statusCol);

        HBox borrowBox = new HBox(10);
        borrowBox.setAlignment(Pos.CENTER_LEFT);

        Label daysLabel = new Label("借閱天數：");
        ComboBox<Integer> daysCombo = new ComboBox<>();
        daysCombo.getItems().addAll(7, 14);
        daysCombo.setValue(7);
        daysCombo.setStyle(AppStyle.textField());

        Button borrowBtn = new Button("✅ 借閱選定書籍");
        borrowBtn.setStyle(AppStyle.buttonPrimary());
        borrowBtn.setOnAction(e -> handleAsyncBorrow(daysCombo.getValue()));

        borrowBox.getChildren().addAll(daysLabel, daysCombo, borrowBtn);

        box.getChildren().addAll(title, searchBox, searchTable, borrowBox);
        return box;
    }

    // ==========================================
    // 核心非同步控制邏輯
    // ==========================================
    private void asyncRefreshUserData() {
        borrowTable.setPlaceholder(new ProgressIndicator());
        fineTable.setPlaceholder(new ProgressIndicator());

        Task<DashboardDataPack> refreshTask = new Task<>() {
            @Override
            protected DashboardDataPack call() throws Exception {
                User latestUser = userService.getUserById(userId);
                List<BorrowRecord> borrows = borrowService.getUserBorrowHistory(userId);
                List<Fine> unpaidFines = fineService.getUnpaidFinesByUser(userId);
                int totalFine = fineService.getTotalUnpaidAmount(userId);
                return new DashboardDataPack(latestUser, borrows, unpaidFines, totalFine);
            }
        };

        refreshTask.setOnSucceeded(e -> {
            DashboardDataPack pack = refreshTask.getValue();
            if (pack.user != null) {
                userName = pack.user.getName();
                nameBadge.setText(userName + " 同學");
                roleBadge.setText("身分: " + pack.user.getRoleLevel().name() + " | 狀態: " + pack.user.getStatus().name());
                if (pack.user.getStatus() == UserStatus.SUSPENDED) {
                    roleBadge.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                } else {
                    roleBadge.setStyle("-fx-text-fill: " + AppStyle.SUCCESS + ";");
                }
            }
            borrowRecordsData.setAll(pack.borrowRecords);
            finesData.setAll(pack.unpaidFines);
            totalFineLabel.setText("未繳總金額：NT$ " + pack.totalFineAmount);
            borrowTable.setPlaceholder(new Label("目前無借閱中的書籍。"));
            fineTable.setPlaceholder(new Label("太棒了！您目前沒有任何未繳罰款。"));
        });
        new Thread(refreshTask).start();
    }

    private void handleAsyncSearch(String keyword) {
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
                handleAsyncSearch(""); // 刷新搜尋列表狀態
                asyncRefreshUserData(); // 更新個人借閱清單
            } else {
                showAlert("借閱失敗", result);
            }
        });
        new Thread(borrowTask).start();
    }

    private void handleAsyncReturn() {
        BorrowRecord selectedRecord = borrowTable.getSelectionModel().getSelectedItem();
        if (selectedRecord == null) {
            showAlert("操作提示", "請先從上方列表中選取您要歸還的書籍。");
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
                showAlert("還書成功（逾期）", "⚠️ 歸還成功！由於該書已逾期，系統已自動開立 NT$ " + amount + " 罰單，您的帳號已被暫停借閱權限。");
            } else if ("SUCCESS".equals(result)) {
                showAlert("還書成功", "✅ 書籍已順利歸還！感謝您的配合。");
            } else {
                showAlert("還書失敗", result);
            }
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
                showAlert("繳費成功", "🎉 所有罰款皆已結清！您的借閱權限已自動復權，歡迎繼續借閱。");
            } else if ("SUCCESS".equals(result)) {
                showAlert("繳費成功", "✅ 該筆款項已收訖。由於您名下仍有其他未繳罰單，請全數結清以恢復權限。");
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

        DashboardDataPack(User user, List<BorrowRecord> borrowRecords, List<Fine> unpaidFines, int totalFineAmount) {
            this.user = user;
            this.borrowRecords = borrowRecords;
            this.unpaidFines = unpaidFines;
            this.totalFineAmount = totalFineAmount;
        }
    }
}