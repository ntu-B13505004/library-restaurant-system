package library.src.gui;

import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import library.src.service.UserService;

public class RegisterView {
    private final Stage stage;
    private final UserService userService = new UserService();

    public RegisterView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + AppStyle.BG_WINDOW + ";");

        VBox card = new VBox(15);
        // 卡片樣式
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 4);");
        card.setPadding(new Insets(35, 45, 35, 45));
        card.setMaxWidth(420);

        Label title = new Label("建立新帳號");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        TextField snoField = new TextField();
        snoField.setPromptText("學號 (例如: B13505019)");
        snoField.setStyle(AppStyle.textField());

        TextField nameField = new TextField();
        nameField.setPromptText("真實姓名");
        nameField.setStyle(AppStyle.textField());

        PasswordField pwdField = new PasswordField();
        pwdField.setPromptText("設定密碼");
        pwdField.setStyle(AppStyle.textField());

        PasswordField confirmPwdField = new PasswordField();
        confirmPwdField.setPromptText("再次輸入確認密碼");
        confirmPwdField.setStyle(AppStyle.textField());

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("NORMAL", "VIP");
        roleCombo.setValue("NORMAL");
        roleCombo.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setStyle(AppStyle.textField());

        Label msgLabel = new Label();
        msgLabel.setWrapText(true);

        Button regBtn = new Button("完成註冊");
        regBtn.setStyle(AppStyle.buttonPrimary());
        regBtn.setMaxWidth(Double.MAX_VALUE);
        regBtn.setPrefHeight(38);

        Button backBtn = new Button("返回登入");
        backBtn.setStyle(AppStyle.buttonSecondary());
        backBtn.setMaxWidth(Double.MAX_VALUE);
        backBtn.setPrefHeight(38);

        // ==========================================
        // 核心非同步註冊邏輯
        // ==========================================
        regBtn.setOnAction(e -> {
            String sno = snoField.getText().trim();
            String name = nameField.getText().trim();
            String pwd = pwdField.getText();
            String cpwd = confirmPwdField.getText();
            String role = roleCombo.getValue();

            // 前端基本驗證
            if (!pwd.equals(cpwd)) {
                msgLabel.setText("❌ 兩次密碼輸入不一致！");
                msgLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");
                return;
            }

            // 1. UI 防呆
            regBtn.setDisable(true);
            regBtn.setText("註冊處理中...");
            msgLabel.setText("");

            // 2. 建立非同步註冊任務
            Task<String> registerTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    // 呼叫您寫好的 UserService 方法
                    return userService.registerUser(sno, name, pwd, role);
                }
            };

            // 3. 處理 Service 回傳結果
            registerTask.setOnSucceeded(ev -> {
                String res = registerTask.getValue();

                // 恢復按鈕狀態
                regBtn.setDisable(false);
                regBtn.setText("完成註冊");

                // 配合 UserService 邏輯："SUCCESS" 代表成功，其他字串代表錯誤訊息
                if ("SUCCESS".equals(res)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "🎉 註冊成功！將為您返回登入頁面。", ButtonType.OK);
                    alert.showAndWait();
                    new LoginView(stage).show();
                } else {
                    msgLabel.setText(res);
                    msgLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");
                }
            });

            // 異常處理
            registerTask.setOnFailed(ev -> {
                regBtn.setDisable(false);
                regBtn.setText("完成註冊");
                msgLabel.setText("❌ 發生預期外錯誤，請稍後再試。");
                msgLabel.setStyle("-fx-text-fill: " + AppStyle.DANGER + ";");
            });

            new Thread(registerTask).start();
        });

        backBtn.setOnAction(e -> new LoginView(stage).show());

        card.getChildren().addAll(title, new Separator(),
                new Label("學號"), snoField,
                new Label("姓名"), nameField,
                new Label("密碼"), pwdField,
                new Label("確認密碼"), confirmPwdField,
                new Label("身分級別"), roleCombo,
                msgLabel, regBtn, backBtn);

        root.getChildren().add(card);
        Scene scene = new Scene(root, 900, 650);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}