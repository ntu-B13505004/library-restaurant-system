package library.gui;

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
        card.setStyle(AppStyle.card());
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
        noAccount.setStyle("-fx-text-fill: " + AppStyle.TEXT_SECONDARY + ";");
        Hyperlink registerLink = new Hyperlink("註冊新帳號");
        registerLink.setStyle("-fx-text-fill: " + AppStyle.ACCENT + "; -fx-underline: false;");
        registerBox.getChildren().addAll(noAccount, registerLink);

        // 登入事件處理
        Runnable handleLogin = () -> {
            String sno = usernameField.getText().trim();
            String pwd = passwordField.getText();
            if (sno.isEmpty() || pwd.isEmpty()) {
                errorLabel.setText("⚠️ 帳號與密碼不得為空！");
                return;
            }
            Map<String, Object> session = userService.login(sno, pwd);
            if (session == null) {
                errorLabel.setText("❌ 學號或密碼錯誤，請再試一次。");
            } else {
                String role = (String) session.get("roleLevel");
                if ("ADMIN".equalsIgnoreCase(role)) {
                    new AdminDashboardView(stage, session).show();
                } else {
                    new UserDashboardView(stage, session).show();
                }
            }
        };

        loginBtn.setOnAction(e -> handleLogin.run());
        passwordField.setOnAction(e -> handleLogin.run());
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