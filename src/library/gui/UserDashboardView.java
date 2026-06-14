package library.gui;

import javafx.beans.property.*;
import javafx.collections.*;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import library.model.*;
import library.service.*;
import java.util.*;

public class UserDashboardView {
    private final Stage stage;
    private final Map<String, Object> session;

    private final BookService bookService = new BookService();
    private final BorrowService borrowService = new BorrowService();
    private final FineService fineService = new FineService();

    private final BorderPane mainLayout = new BorderPane();
    private final StackPane contentArea = new StackPane();
    private final int userId;
    private final String userName;
    private final String userRole;

    public UserDashboardView(Stage stage, Map<String, Object> session) {
        this.stage = stage;
        this.session = session;
        this.userId = (int) session.get("userId");
        this.userName = (String) session.get("name");
        this.userRole = (String) session.get("roleLevel");
    }

    public void show() {
        mainLayout.setStyle("-fx-background-color: " + AppStyle.BG_WINDOW + ";");

        // --- 左側側邊欄 ---
        VBox sidebar = new VBox(15);
        sidebar.setPrefWidth(240);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setStyle("-fx-background-color: " + AppStyle.PRIMARY + ";");

        Label nameBadge = new Label(userName + " 同學");
        nameBadge.setFont(Font.font("System", FontWeight.BOLD, 18));
        nameBadge.setStyle("-fx-text-fill: white;");

        Label roleBadge = new Label("身分: " + userRole);
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

        navSearchBtn.setOnAction(e -> switchPage(buildBookSearchPage()));
        navBorrowBtn.setOnAction(e -> switchPage(buildBorrowAndFinePage()));
        logoutBtn.setOnAction(e -> new LoginView(stage).show());

        sidebar.getChildren().addAll(profileBox, new Separator(), navSearchBtn, navBorrowBtn, new Spacer(), logoutBtn);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(contentArea);

        // 預設進入第一頁
        switchPage(buildBookSearchPage());

        Scene scene = new Scene(mainLayout, 1100, 680);
        stage.setScene(scene);
        stage.setTitle("圖書館讀者控制台 - " + userName);
        stage.centerOnScreen();
    }

    private void switchPage(VBox page) {
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    // --- 功能頁面 1: 圖書檢索與借閱 ---
    private VBox buildBookSearchPage() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(30));

        Label heading = new Label("圖書館藏檢索中心");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));
        heading.setStyle("-fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        HBox searchBar = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("輸入書名、作者、出版社或主題關鍵字...");
        searchField.setStyle(AppStyle.textField());
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("開始檢索");
        searchBtn.setStyle(AppStyle.buttonPrimary());
        searchBtn.setPrefWidth(100);

        Button borrowBtn = new Button("⚡ 辦理借閱");
        borrowBtn.setStyle(AppStyle.buttonSecondary());

        searchBar.getChildren().addAll(searchField, searchBtn, borrowBtn);

        // 書籍表格
        TableView<Book> table = new TableView<>();
        table.setStyle("-fx-background-radius: 6;");

        TableColumn<Book, Integer> idCol = new TableColumn<>("書籍ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        idCol.setPrefWidth(70);

        TableColumn<Book, String> titleCol = new TableColumn<>("書名");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(220);

        TableColumn<Book, String> authorCol = new TableColumn<>("作者");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("authors"));
        authorCol.setPrefWidth(150);

        TableColumn<Book, String> subjectCol = new TableColumn<>("主題");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subjects"));
        subjectCol.setPrefWidth(100);

        TableColumn<Book, String> statusCol = new TableColumn<>("目前狀態");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); }
                else {
                    Label label = new Label(item);
                    label.setStyle("AVAILABLE".equals(item) ? AppStyle.chipAvailable() : AppStyle.chipBorrowed());
                    setGraphic(label);
                }
            }
        });

        table.getColumns().addAll(idCol, titleCol, authorCol, subjectCol, statusCol);
        VBox.setVgrow(table, Priority.ALWAYS);

        // 非同步載入數據
        Runnable loadBooks = () -> {
            Task<List<Book>> task = new Task<>() {
                @Override protected List<Book> call() {
                    return bookService.searchBooks(searchField.getText());
                }
            };
            task.setOnSucceeded(e -> table.setItems(FXCollections.observableArrayList(task.getValue())));
            new Thread(task).start();
        };

        searchBtn.setOnAction(e -> loadBooks.run());
        loadBooks.run(); // 初始加載

        // 借書按鈕事件
        borrowBtn.setOnAction(e -> {
            Book selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("提示", "請先從表格中選擇一本您想借閱的書。");
                return;
            }

            // 跳出彈窗詢問借閱天數
            TextInputDialog dialog = new TextInputDialog("7");
            dialog.setTitle("辦理圖書借閱");
            dialog.setHeaderText("書名: " + selected.getTitle());
            dialog.setContentText("請輸入預計借閱天數 (NORMAL上限7天，VIP上限14天):");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(daysStr -> {
                try {
                    int days = Integer.parseInt(daysStr.trim());
                    String res = borrowService.borrowBook(userId, selected.getBookId(), days);
                    if ("SUCCESS".equals(res)) {
                        showAlert("成功", "🎉 借閱成功！請於期限內歸還。");
                        loadBooks.run();
                    } else {
                        showAlert("借閱受阻", res);
                    }
                } catch (NumberFormatException ex) {
                    showAlert("錯誤", "輸入的天數格式不正確。");
                }
            });
        });

        box.getChildren().addAll(heading, searchBar, table);
        return box;
    }

    // --- 功能頁面 2: 我的借還與罰款 ---
    private VBox buildBorrowAndFinePage() {
        VBox box = new VBox(25);
        box.setPadding(new Insets(30));

        // --- 上半部：個人借閱歷史紀錄與歸還 ---
        Label borrowHeading = new Label("我的借閱歷史與未還清單");
        borrowHeading.setFont(Font.font("System", FontWeight.BOLD, 18));
        borrowHeading.setStyle("-fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        TableView<BorrowRecord> borrowTable = new TableView<>();
        TableColumn<BorrowRecord, Integer> recIdCol = new TableColumn<>("紀錄ID");
        recIdCol.setCellValueFactory(new PropertyValueFactory<>("recordId"));

        TableColumn<BorrowRecord, String> bTitleCol = new TableColumn<>("借閱書名");
        bTitleCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBook().getTitle()));
        bTitleCol.setPrefWidth(200);

        TableColumn<BorrowRecord, String> bDateCol = new TableColumn<>("借閱日期");
        bDateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBorrowDate().toString().substring(0,10)));

        TableColumn<BorrowRecord, String> dDateCol = new TableColumn<>("應還期限");
        dDateCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDueDate().toString().substring(0,10)));

        TableColumn<BorrowRecord, String> rDateCol = new TableColumn<>("實際歸還日期");
        rDateCol.setCellValueFactory(cell -> {
            var date = cell.getValue().getReturnDate();
            return new SimpleStringProperty(date == null ? "尚未歸還" : date.toString().substring(0,10));
        });

        borrowTable.getColumns().addAll(recIdCol, bTitleCol, bDateCol, dDateCol, rDateCol);
        borrowTable.setPrefHeight(200);

        Button returnBtn = new Button("↩️ 辦理線上還書");
        returnBtn.setStyle(AppStyle.buttonPrimary());

        // --- 下半部：罰款帳單中心 ---
        Label fineHeading = new Label("未繳罰款帳單明細");
        fineHeading.setFont(Font.font("System", FontWeight.BOLD, 18));
        fineHeading.setStyle("-fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        Label totalFineLabel = new Label("目前累積未繳總金額: $0 元");
        totalFineLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        totalFineLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");

        TableView<Fine> fineTable = new TableView<>();
        TableColumn<Fine, Integer> fineIdCol = new TableColumn<>("罰單ID");
        fineIdCol.setCellValueFactory(new PropertyValueFactory<>("fineId"));

        TableColumn<Fine, Integer> amountCol = new TableColumn<>("累積金額");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Fine, String> fBookCol = new TableColumn<>("對應逾期圖書");
        fBookCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRecord().getBook().getTitle()));
        fBookCol.setPrefWidth(220);

        fineTable.getColumns().addAll(fineIdCol, amountCol, fBookCol);
        fineTable.setPrefHeight(150);

        Button payBtn = new Button("💰 繳納選中罰款");
        payBtn.setStyle(AppStyle.buttonPrimary());
        payBtn.setStyle("-fx-background-color: " + AppStyle.SUCCESS + "; -fx-text-fill: white; -fx-font-weight: bold;");

        // 刷新整體數據的方法
        Runnable refreshData = () -> {
            // 刷借閱紀錄
            Task<List<BorrowRecord>> bTask = new Task<>() {
                @Override protected List<BorrowRecord> call() { return borrowService.getUserBorrowHistory(userId); }
            };
            bTask.setOnSucceeded(e -> borrowTable.setItems(FXCollections.observableArrayList(bTask.getValue())));
            new Thread(bTask).start();

            // 刷未繳罰款
            Task<List<Fine>> fTask = new Task<>() {
                @Override protected List<Fine> call() { return fineService.getUnpaidFinesByUser(userId); }
            };
            fTask.setOnSucceeded(e -> fineTable.setItems(FXCollections.observableArrayList(fTask.getValue())));
            new Thread(fTask).start();

            // 刷總額
            Task<Integer> totalTask = new Task<>() {
                @Override protected Integer call() { return fineService.getTotalUnpaidAmount(userId); }
            };
            totalTask.setOnSucceeded(e -> totalFineLabel.setText("目前累積未繳總金額: $" + totalTask.getValue() + " 元"));
            new Thread(totalTask).start();
        };

        // 還書事件
        returnBtn.setOnAction(e -> {
            BorrowRecord sel = borrowTable.getSelectionModel().getSelectedItem();
            if (sel == null || sel.getReturnDate() != null) {
                showAlert("提示", "請選擇一筆「尚未歸還」的借閱紀錄進行歸還。");
                return;
            }
            String res = borrowService.returnBook(sel.getBook().getBookId());
            if (res.startsWith("SUCCESS_WITH_FINE")) {
                String amt = res.split(":")[1];
                showAlert("逾期提醒", "⚠️ 還書成功，但由於已超出借閱期限，系統已自動開立 $" + amt + " 元罰單，帳號已被同步停權！");
            } else if ("SUCCESS".equals(res)) {
                showAlert("成功", "✅ 順利歸還！書籍狀態已切換為可借閱。");
            } else {
                showAlert("錯誤", res);
            }
            refreshData.run();
        });

        // 繳費事件
        payBtn.setOnAction(e -> {
            Fine selFine = fineTable.getSelectionModel().getSelectedItem();
            if (selFine == null) {
                showAlert("提示", "請從帳單明細中選擇一筆進行繳納。");
                return;
            }
            String res = fineService.payFine(selFine.getFineId());
            if ("SUCCESS_AND_ACTIVATED".equals(res)) {
                showAlert("繳費成功", "🎉 所有罰款皆已結清！您的帳號已自動恢復權限（ACTIVE），歡迎繼續借書。");
            } else if ("SUCCESS".equals(res)) {
                showAlert("繳費成功", "✅ 該筆罰款已繳清。由於您仍有其他未繳罰單，請全數結清以恢復權限。");
            } else {
                showAlert("錯誤", res);
            }
            refreshData.run();
        });

        refreshData.run(); // 首次載入

        HBox bOps = new HBox(returnBtn);
        HBox fOps = new HBox(15, totalFineLabel, payBtn);
        fOps.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(borrowHeading, borrowTable, bOps, new Separator(), fineHeading, fOps, fineTable);
        return box;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // 實用邊界填充小元件
    private static class Spacer extends Region {
        public Spacer() { VBox.setVgrow(this, Priority.ALWAYS); HBox.setHgrow(this, Priority.ALWAYS); }
    }
}