package library.gui;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import library.service.UserService;

import java.util.Map;

/**
 * 登入畫面
 * 左半邊：品牌介紹區（靛藍底）
 * 右半邊：登入表單卡片
 *
 * 根據角色導向不同主畫面：
 *   ADMIN  → AdminDashboardView
 *   其他   → UserDashboardView
 */
public class LoginView {

    private final Stage stage;
    private final UserService userService = new UserService();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        // ── 左側品牌區 ────────────────────────────────
        VBox brandPane = buildBrandPane();

        // ── 右側表單區 ────────────────────────────────
        VBox formPane = buildFormPane();

        // ── 組合 ──────────────────────────────────────
        HBox root = new HBox(brandPane, formPane);
        HBox.setHgrow(brandPane, Priority.NEVER);
        HBox.setHgrow(formPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 880, 580);
        stage.setTitle("圖書館借還書系統");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    // ── 左側品牌區 ────────────────────────────────────
    private VBox buildBrandPane() {
        VBox pane = new VBox(18);
        pane.setPrefWidth(340);
        pane.setAlignment(Pos.CENTER_LEFT);
        pane.setPadding(new Insets(60, 50, 60, 50));
        pane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " + AppStyle.PRIMARY + ", " + AppStyle.BG_SIDEBAR + ");"
        );

        Label icon = new Label("📚");
        icon.setStyle("-fx-font-size: 52px;");

        Label title = new Label("圖書館\n借還書系統");
        title.setStyle(
                "-fx-font-size: 28px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: white;"
                        + "-fx-line-spacing: 6;"
        );
        title.setWrapText(true);

        Label sub = new Label("Library Management System");
        sub.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-text-fill: rgba(255,255,255,0.65);"
        );

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.2);");
        VBox.setMargin(sep, new Insets(12, 0, 12, 0));

        // 功能提示列表
        String[] features = {
                "🔍  多條件書籍搜尋",
                "📖  快速借閱與歸還",
                "⏰  逾期提醒機制",
                "🛡  管理員後台管理"
        };
        VBox featureList = new VBox(10);
        for (String f : features) {
            Label lbl = new Label(f);
            lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.82);");
            featureList.getChildren().add(lbl);
        }

        pane.getChildren().addAll(icon, title, sub, sep, featureList);
        return pane;
    }

    // ── 右側登入表單 ──────────────────────────────────
    private VBox buildFormPane() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle(AppStyle.pageBackground());
        outer.setPadding(new Insets(40));

        VBox card = new VBox(20);
        card.setStyle(AppStyle.card());
        card.setPadding(new Insets(40, 48, 40, 48));
        card.setMaxWidth(380);

        // 標題
        Label heading = new Label("歡迎回來");
        heading.setStyle(AppStyle.labelTitle());
        Label hint = new Label("請輸入您的學號與密碼登入");
        hint.setStyle(AppStyle.labelSubtitle());

        // 學號
        Label studentNoLabel = new Label("學號");
        studentNoLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");
        TextField studentNoField = new TextField();
        studentNoField.setPromptText("例：B11234567");
        studentNoField.setStyle(AppStyle.textField());
        studentNoField.setMaxWidth(Double.MAX_VALUE);

        // 密碼
        Label passwordLabel = new Label("密碼");
        passwordLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("請輸入密碼");
        passwordField.setStyle(AppStyle.textField());
        passwordField.setMaxWidth(Double.MAX_VALUE);

        // 錯誤訊息
        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.DANGER + ";");
        errorLabel.setWrapText(true);

        // 登入按鈕
        Button loginBtn = new Button("登入");
        loginBtn.setStyle(AppStyle.btnPrimary());
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        // 前往註冊
        HBox registerRow = new HBox(6);
        registerRow.setAlignment(Pos.CENTER);
        Label noAccount = new Label("還沒有帳號？");
        noAccount.setStyle(AppStyle.labelSubtitle());
        Hyperlink registerLink = new Hyperlink("立即註冊");
        registerLink.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.PRIMARY + ";");
        registerRow.getChildren().addAll(noAccount, registerLink);

        // 事件：登入
        Runnable doLogin = () -> {
            String sno = studentNoField.getText().trim();
            String pwd = passwordField.getText();
            if (sno.isEmpty() || pwd.isEmpty()) {
                errorLabel.setText("學號與密碼不得空白。");
                return;
            }
            Map<String, Object> session = userService.login(sno, pwd);
            if (session == null) {
                errorLabel.setText("學號或密碼錯誤，請再試一次。");
                passwordField.clear();
                return;
            }
            String role = (String) session.get("roleLevel");
            if ("ADMIN".equalsIgnoreCase(role)) {
                new AdminDashboardView(stage, session).show();
            } else {
                new UserDashboardView(stage, session).show();
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passwordField.setOnAction(e -> doLogin.run());

        // 事件：前往註冊
        registerLink.setOnAction(e -> new RegisterView(stage).show());

        card.getChildren().addAll(
                heading, hint,
                studentNoLabel, studentNoField,
                passwordLabel, passwordField,
                errorLabel,
                loginBtn,
                registerRow
        );

        outer.getChildren().add(card);
        VBox.setVgrow(card, Priority.NEVER);
        return outer;
    }
}