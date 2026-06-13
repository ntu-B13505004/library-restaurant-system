package library;

import library.database.DatabaseManager;
import library.database.DataLoader;
import library.gui.LoginFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // 1. 設定外觀為系統預設外觀（讓 Swing 畫面看起來更好看，不會太過過時）
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. 初始化資料庫結構並自動配置預設管理員（ADMIN001）
        DatabaseManager.initializeDatabase();

        // 3. 匯入測試用 JSON 數據
        DataLoader.loadInitialData();

        // 4. 在事件發送執行緒（Event Dispatch Thread）中開啟登入 GUI 視窗
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}