package library;

import library.database.DatabaseManager;
import library.database.DataLoader;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== 圖書館系統啟動中 ===");

        // 1. 初始化資料庫表
        DatabaseManager.initializeDatabase();

        // 2. 導入 JSON 資料
        DataLoader.loadInitialData();

        System.out.println("=== 系統就緒，主檔案 library.db 已生成 ===");
    }
}