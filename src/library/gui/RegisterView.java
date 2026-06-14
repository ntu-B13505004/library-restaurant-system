package library.gui;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import library.service.UserService;

/**
 * 使用者註冊畫面
 * 欄位：學號、姓名、密碼、確認密碼、會員等級（NORMAL / VIP）
 */
public class RegisterView {

    private final Stage stage;
    private final UserService userService = new UserService();

    public RegisterView(Stage stage) {
        this.stage = stage;
    }

    public void show() {
        VBox outer = new VBox();
        outer.setAlignment(Pos.CENTER);
        outer.setStyle(AppStyle.pageBackground());
        outer.setPadding(new Insets(40));

        VBox card = new VBox(16);
        card.setStyle(AppStyle.card());
        card.setPadding(new Insets(40, 48, 40, 48));
        card.setMaxWidth(420);

        // 標題
        Label heading = new Label("建立新帳號");
        heading.setStyle(AppStyle.labelTitle());
        Label subheading = new Label("填寫以下資料完成註冊");
        subheading.setStyle(AppStyle.labelSubtitle());

        // ── 欄位 ──────────────────────────────────────
        TextField studentNoField = styledField("學號", "例：B11234567");
        TextField nameField      = styledField("姓名", "請輸入真實姓名");
        PasswordField pwdField   = styledPassword("密碼", "至少6碼");
        PasswordField confirmPwd = styledPassword("確認密碼", "再輸入一次密碼");

        Label roleLabel = new Label("會員等級");
        roleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        ToggleGroup roleGroup = new ToggleGroup();
        RadioButton normalRadio = new RadioButton("普通會員（最多借3本，7天）");
        normalRadio.setToggleGroup(roleGroup);
        normalRadio.setSelected(true);
        normalRadio.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");

        RadioButton vipRadio = new RadioButton("VIP 會員（最多借5本，14天）");
        vipRadio.setToggleGroup(roleGroup);
        vipRadio.setStyle("-fx-font-size: 13px; -fx-text-fill: " + AppStyle.PRIMARY + "; -fx-font-weight: bold;");

        HBox roleBox = new HBox(20, normalRadio, vipRadio);
        roleBox.setAlignment(Pos.CENTER_LEFT);

        // 錯誤 / 成功訊息
        Label msgLabel = new Label("");
        msgLabel.setStyle("-fx-font-size: 13px;");
        msgLabel.setWrapText(true);

        // 按鈕列
        Button registerBtn = new Button("確認註冊");
        registerBtn.setStyle(AppStyle.btnPrimary());
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        Button backBtn = new Button("返回登入");
        backBtn.setStyle(AppStyle.btnOutline());
        backBtn.setMaxWidth(Double.MAX_VALUE);

        // 事件：註冊
        registerBtn.setOnAction(e -> {
            String sno  = studentNoField.getText().trim();
            String name = nameField.getText().trim();
            String pwd  = pwdField.getText();
            String cpwd = confirmPwd.getText();
            String role = vipRadio.isSelected() ? "VIP" : "NORMAL";

            if (!pwd.equals(cpwd)) {
                setMsg(msgLabel, "兩次密碼輸入不一致。", false);
                return;
            }
            if (pwd.length() < 6) {
                setMsg(msgLabel, "密碼至少需6碼。", false);
                return;
            }

            String result = userService.registerUser(sno, name, pwd, role);
            if ("SUCCESS".equals(result)) {
                setMsg(msgLabel, "✅ 註冊成功！即將返回登入頁...", true);
                registerBtn.setDisable(true);
                // 延遲 1.5 秒跳回登入
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> new LoginView(stage).show());
                }).start();
            } else {
                setMsg(msgLabel, result, false);
            }
        });

        backBtn.setOnAction(e -> new LoginView(stage).show());

        card.getChildren().addAll(
                heading, subheading,
                labeledNode("學號", studentNoField),
                labeledNode("姓名", nameField),
                labeledNode("密碼", pwdField),
                labeledNode("確認密碼", confirmPwd),
                roleLabel, roleBox,
                msgLabel,
                registerBtn, backBtn
        );

        outer.getChildren().add(card);

        Scene scene = new Scene(outer, 880, 640);
        stage.setScene(scene);
    }

    // ── 輔助方法 ──────────────────────────────────────

    private TextField styledField(String label, String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle(AppStyle.textField());
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private PasswordField styledPassword(String label, String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle(AppStyle.textField());
        f.setMaxWidth(Double.MAX_VALUE);
        return f;
    }

    private VBox labeledNode(String labelText, javafx.scene.Node field) {
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + AppStyle.TEXT_PRIMARY + ";");
        VBox box = new VBox(5, lbl, field);
        return box;
    }

    private void setMsg(Label lbl, String text, boolean success) {
        lbl.setText(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (success ? AppStyle.SUCCESS : AppStyle.DANGER) + ";");
    }
}