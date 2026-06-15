package library;

import javafx.application.Application;
import javafx.stage.Stage;
import library.gui.*;

/**
 * 圖書館借還書系統 - JavaFX 入口點
 * 啟動後先初始化資料庫，再顯示登入畫面。
 */
public class LibraryApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 初始化資料庫與載入初始資料
        library.database.DatabaseManager.initializeDatabase();
        library.database.DataLoader.loadInitialData();

        // 顯示登入畫面
        LoginView loginView = new LoginView(primaryStage);
        loginView.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}