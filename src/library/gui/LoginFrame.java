package library.gui;

import library.model.User;
import library.model.UserRole;
import library.repository.UserRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class LoginFrame extends JFrame {

    private final JTextField txtStudentNo;
    private final JPasswordField txtPassword;
    private final UserRepository userRepository = new UserRepository();

    public LoginFrame() {
        setTitle("圖書館管理系統 - 登入");
        setSize(380, 220);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 視窗居中
        setResizable(false);

        // 主面板採用 GridBagLayout 或 BorderLayout 讓畫面看起來更對稱
        JPanel panel = new JPanel(new GridBorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblTitle = new JLabel("圖書館借還書系統", JLabel.CENTER);
        lblTitle.setFont(new Font("微軟正黑體", Font.BOLD, 18));

        JLabel lblUser = new JLabel("學號/帳號:");
        JLabel lblPwd = new JLabel("密碼:");

        txtStudentNo = new JTextField(15);
        txtPassword = new JPasswordField(15);
        // Demo 方便：預設幫填剛才內建的管理員帳密
        txtStudentNo.setText("ADMIN001");
        txtPassword.setText("admin123");

        JButton btnLogin = new JButton("確認登入");
        btnLogin.setFont(new Font("微軟正黑體", Font.BOLD, 12));

        // 佈局排版
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.add(lblUser);
        formPanel.add(txtStudentNo);
        formPanel.add(lblPwd);
        formPanel.add(txtPassword);

        setLayout(new BorderLayout(10, 10));
        add(lblTitle, BorderLayout.NORTH);
        add(formPanel, BorderLayout.CENTER);
        add(btnLogin, BorderLayout.SOUTH);

        // 監聽登入事件
        btnLogin.addActionListener(this::handleLogin);
    }

    private void handleLogin(ActionEvent e) {
        String studentNo = txtStudentNo.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (studentNo.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ 請輸入學號與密碼！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 透過 Repository 驗證帳號
        User user = userRepository.findByStudentNo(studentNo);

        if (user != null && user.getPassword().equals(password)) {
            JOptionPane.showMessageDialog(this, "🎉 登入成功！歡迎回來，" + user.getName(), "成功", JOptionPane.INFORMATION_MESSAGE);

            // 關閉登入視窗，打開主控制台（傳入當前登入的 User 物件）
            this.dispose();
            SwingUtilities.invokeLater(() -> {
                MainDashboardFrame dashboard = new MainDashboardFrame(user);
                dashboard.setVisible(true);
            });
        } else {
            JOptionPane.showMessageDialog(this, "❌ 學號或密碼錯誤，請重新輸入。", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 提供一個簡單的 Layout 輔助
    private static class GridBorderLayout extends BorderLayout {}
}