package library;

import library.database.DatabaseManager;
import library.database.DataLoader;
import library.model.Book;
import library.model.User;
import library.repository.BookRepository;
import library.repository.UserRepository;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== 圖書館系統啟動中 ===");
        System.out.println("📍 Java 目前找尋檔案的目錄是: " + new java.io.File(".").getAbsolutePath());

        // 1. 初始化資料庫表 (會自動建立全新欄位的五張表)
        DatabaseManager.initializeDatabase();

        // 2. 導入 JSON 資料 (若已有資料會自動跳過)
        DataLoader.loadInitialData();

        System.out.println("=== 資料庫就緒，開始進行 Repository 測試 ===");

        // 3. 實例化 Repository
        BookRepository bookRepository = new BookRepository();
        UserRepository userRepository = new UserRepository();

        // 🎯 測試一：撈出前 5 本書來看看 (順便秀出拆表後的 ISBN 成果)
        System.out.println("\n📚 [測試] 撈出前 5 本館藏書籍（含一對多 ISBN）：");
        List<Book> allBooks = bookRepository.findAll();
        for (int i = 0; i < Math.min(5, allBooks.size()); i++) {
            Book b = allBooks.get(i);
            // 除了書名、狀態，我們把 List<String> 形態的 isbns 也印出來瞧瞧
            System.out.println("ID: " + b.getBookId() +
                    " | 書名: " + b.getTitle() +
                    " | 狀態: " + b.getStatus() +
                    " | ISBN 列表: " + b.getIsbns());
        }

        // 🎯 測試二：尋找 ID 為 5 的使用者
        System.out.println("\n👤 [測試] 尋找特定的使用者 (ID: 5)：");
        User user5 = userRepository.findById(5);
        if (user5 != null) {
            // ✨ 修正點：將 getEmail() 改為 getStudentNo()；將 getUserRole() 改為 getRoleLevel()
            System.out.println("找到使用者！姓名: " + user5.getName() +
                    " | 學號: " + user5.getStudentNo() +
                    " | 權限等級: " + user5.getRoleLevel() +
                    " | 帳號狀態: " + user5.getStatus() +
                    " | 借書上限: " + user5.getBorrowLimit() + " 本");
        } else {
            System.err.println("❌ 找不到 ID 為 5 的使用者。");
        }

        // 🎯 測試三：測試剛剛為 GUI 登入功能鋪路寫的「學號搜尋功能」
        System.out.println("\n🔑 [測試] 模擬 GUI 登入：使用學號尋找使用者");
        if (user5 != null) {
            String targetStudentNo = user5.getStudentNo(); // 拿剛才查到的學號來當測試標的
            System.out.println("正在用學號 [" + targetStudentNo + "] 進行檢索...");

            User loginUser = userRepository.findByStudentNo(targetStudentNo);
            if (loginUser != null) {
                System.out.println("🎉 成功！用學號查到使用者: " + loginUser.getName() +
                        " | 密碼雜湊值: " + loginUser.getPassword() + " (可進行比對驗證)");
            } else {
                System.err.println("❌ 檢索失敗，資料庫找不到此學號。");
            }
        }
    }
}