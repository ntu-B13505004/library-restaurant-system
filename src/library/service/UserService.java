package library.service;

import library.database.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 以下為概念性 import，視你實際的 Model 與 Repository 調整
// import library.model.User;
// import library.repository.UserRepository;

public class UserService {

    // private final UserRepository userRepository;

    public UserService() {
        // this.userRepository = new UserRepository();
    }

    /**
     * ✨ 核心功能 1：使用者登入驗證 (為 Login 畫面量身打造)
     * * @param studentNo 學號（登入帳號）
     * @param password  密碼
     * @return 登入成功回傳包含使用者資訊的 Map（或 User 物件），失敗則回傳 null
     */
    public Map<String, Object> login(String studentNo, String password) {
        if (studentNo == null || password == null) return null;

        String sql = "SELECT * FROM users WHERE student_no = ? AND password = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentNo.trim());
            pstmt.setString(2, password); // 實務上密碼可做雜湊加密，Demo 專案用明文比對即可

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> userSession = new HashMap<>();
                    userSession.put("userId", rs.getInt("user_id"));
                    userSession.put("studentNo", rs.getString("student_no"));
                    userSession.put("name", rs.getString("name"));
                    userSession.put("roleLevel", rs.getString("role_level")); // NORMAL or VIP
                    userSession.put("status", rs.getString("status"));         // ACTIVE or SUSPENDED
                    return userSession;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 登入驗證查詢失敗");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 核心功能 2：學生帳號註冊 (Register 畫面使用)
     * 【簡報規格：欄位需包含學號、姓名、密碼，並可自由選擇 NORMAL 或 VIP】
     */
    public String registerUser(String studentNo, String name, String password, String roleLevel) {
        // 1️⃣ 防禦性驗證：檢查欄位是否留白
        if (studentNo == null || studentNo.trim().isEmpty() ||
                name == null || name.trim().isEmpty() ||
                password == null || password.isEmpty()) {
            return "⚠️ 註冊失敗：所有欄位皆為必填！";
        }

        // 驗證角色等級格式是否正確
        if (!"NORMAL".equalsIgnoreCase(roleLevel) && !"VIP".equalsIgnoreCase(roleLevel)) {
            return "❌ 系統錯誤：不合法的會員等級。";
        }

        // 2️⃣ 檢查學號是否重複 (學號具有唯一性)
        String checkSql = "SELECT COUNT(*) FROM users WHERE student_no = ?";
        String insertSql = "INSERT INTO users (student_no, name, password, role_level, status) VALUES (?, ?, ?, ?, 'ACTIVE')";

        try (Connection conn = DatabaseManager.getConnection()) {

            try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                checkPstmt.setString(1, studentNo.trim());
                try (ResultSet rs = checkPstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return "⚠️ 註冊失敗：此學號（" + studentNo + "）已被註冊！";
                    }
                }
            }

            // 3️⃣ 執行寫入
            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql)) {
                insertPstmt.setString(1, studentNo.trim());
                insertPstmt.setString(2, name.trim());
                insertPstmt.setString(3, password);
                insertPstmt.setString(4, roleLevel.toUpperCase());
                insertPstmt.executeUpdate();
                return "SUCCESS";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "❌ 資料庫寫入錯誤，註冊未完成。";
        }
    }

    /**
     * ✨ 管理者功能 3：手動將學生「停權」或「復權」 (管理員後台一鍵切換)
     * 【簡報規格：管理員可手動停權/復權，被停權者無法借書】
     * * @param userId 使用者 ID
     * @param status 新的狀態 ("ACTIVE" 或 "SUSPENDED")
     */
    public boolean updateUserStatus(int userId, String status) {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"SUSPENDED".equalsIgnoreCase(status)) {
            return false;
        }

        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.toUpperCase());
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ 更新使用者狀態失敗，User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 管理者功能 4：撈取全校學生名單 (管理員後台表格數據源)
     * 支援透過學號進行關鍵字模糊過濾。
     */
    public List<Map<String, Object>> getAllStudents(String studentNoFilter) {
        List<Map<String, Object>> students = new ArrayList<>();

        String sql = "SELECT user_id, student_no, name, role_level, status FROM users";
        if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
            sql += " WHERE student_no LIKE ?";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (studentNoFilter != null && !studentNoFilter.trim().isEmpty()) {
                pstmt.setString(1, "%" + studentNoFilter.trim() + "%");
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> student = new HashMap<>();
                    student.put("userId", rs.getInt("user_id"));
                    student.put("studentNo", rs.getString("student_no"));
                    student.put("name", rs.getString("name"));
                    student.put("roleLevel", rs.getString("role_level"));
                    student.put("status", rs.getString("status"));
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 獲取學生列表失敗");
            e.printStackTrace();
        }
        return students;
    }
}