package library.gui;

import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import library.service.UserService;
import java.util.Map;

public class LoginView {
    private final Stage stage;
    private final UserService userService = new UserService();

    public LoginView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        HBox root = new HBox();
        root.setStyle("-fx-background-color: " + AppStyle.BG_WINDOW + ";");

        // 左側品牌區
        VBox brandPane = new VBox(20);
        brandPane.setAlignment(Pos.CENTER_LEFT);
        brandPane.setPadding(new Insets(50));
        brandPane.setPrefWidth(380);
        brandPane.setStyle("-fx-background-color: " + AppStyle.PRIMARY + ";");

        Label title = new Label("智慧圖書館\n核心系統");
        title.setFont(Font.font("System", FontWeight.BOLD, 32));
        title.setStyle("-fx-text-fill: white; -fx-line-spacing: 10;");
        Label desc = new Label("University Library Management System\n提供更快速、流暢的數位借閱體驗。");
        desc.setStyle("-fx-text-fill: #BBBCDE; -fx-font-size: 14px;");
        brandPane.getChildren().addAll(title, desc);

        // 右側登入表單區
        VBox formPane = new VBox();
        formPane.setAlignment(Pos.CENTER);
        formPane.setPadding(new Insets(40));
        HBox.setHgrow(formPane, Priority.ALWAYS);

        VBox card = new VBox(18);
        // 配合 AppStyle 設定卡片樣式
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 4);");
        card.setPadding(new Insets(40, 45, 40, 45));
        card.setMaxWidth(400);

        Label heading = new Label("歡迎回來");
        heading.setFont(Font.font("System", FontWeight.BOLD, 22));
        heading.setStyle("-fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        TextField usernameField = new TextField();
        usernameField.setPromptText("請輸入學號/帳號");
        usernameField.setStyle(AppStyle.textField());

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("請輸入密碼");
        passwordField.setStyle(AppStyle.textField());

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + "; -fx-font-size: 13px;");

        Button loginBtn = new Button("立即登入");
        loginBtn.setStyle(AppStyle.buttonPrimary());
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setPrefHeight(40);

        HBox registerBox = new HBox(5);
        registerBox.setAlignment(Pos.CENTER);
        Label noAccount = new Label("還沒有帳號嗎？");
        noAccount.setStyle("-fx-text-fill: #7F8C8D;");
        Hyperlink registerLink = new Hyperlink("註冊新帳號");
        registerLink.setStyle("-fx-text-fill: " + AppStyle.ACCENT_LIGHT + "; -fx-underline: false;");
        registerBox.getChildren().addAll(noAccount, registerLink);

        // ==========================================
        // 核心非同步登入邏輯
        // ==========================================
        Runnable handleLogin = () -> {
            String sno = usernameField.getText().trim();
            String pwd = passwordField.getText();

            if (sno.isEmpty() || pwd.isEmpty()) {
                errorLabel.setText("⚠️ 帳號與密碼不得為空！");
                return;
            }

            // 1. UI 狀態防呆，避免重複點擊
            loginBtn.setDisable(true);
            loginBtn.setText("系統登入中...");
            errorLabel.setText("");

            // 2. 開啟背景執行緒與資料庫對話
            Task<Map<String, Object>> loginTask = new Task<>() {
                @Override
                protected Map<String, Object> call() throws Exception {
                    return userService.login(sno, pwd);
                }
            };

            // 3. 執行成功後更新 UI
            loginTask.setOnSucceeded(e -> {
                Map<String, Object> session = loginTask.getValue();
                if (session == null) {
                    errorLabel.setText("❌ 學號或密碼錯誤，請再試一次。");
                    loginBtn.setDisable(false);
                    loginBtn.setText("立即登入");
                } else {
                    String role = (String) session.get("roleLevel");
                    // 根據權限派發不同儀表板
                    if ("ADMIN".equalsIgnoreCase(role) || "SYSTEM_ADMIN".equalsIgnoreCase(role)) {
                        new AdminDashboardView(stage, session).show();
                    } else {
                        new UserDashboardView(stage, session).show();
                    }
                }
            });

            // 異常處理
            loginTask.setOnFailed(e -> {
                errorLabel.setText("❌ 系統發生異常，請檢查資料庫連線。");
                loginBtn.setDisable(false);
                loginBtn.setText("立即登入");
                loginTask.getException().printStackTrace();
            });

            new Thread(loginTask).start();
        };

        loginBtn.setOnAction(e -> handleLogin.run());
        passwordField.setOnAction(e -> handleLogin.run()); // 支援 Enter 登入
        registerLink.setOnAction(e -> new RegisterView(stage).show());

        card.getChildren().addAll(heading, new Separator(), usernameField, passwordField, errorLabel, loginBtn, registerBox);
        formPane.getChildren().add(card);
        root.getChildren().addAll(brandPane, formPane);

        Scene scene = new Scene(root, 900, 580);
        stage.setScene(scene);
        stage.setTitle("圖書館管理系統 - 登入");
        stage.centerOnScreen();
        stage.show();
    }
}