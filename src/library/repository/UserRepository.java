package library.repository;

import library.database.DatabaseManager;
import library.model.User;
import java.sql.*;

public class UserRepository {

    /**
     * 根據使用者內部遞增 ID 尋找使用者
     */
    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // 🎯 呼叫全新 User 建構子，精準對接簡報 Page 18 欄位
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getString("role_level"),
                            rs.getString("status"),
                            rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 根據 ID 查詢使用者失敗: " + userId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * ✨ 實用新增：根據「學號」尋找使用者
     * 💡 這在實作系統登入功能（Login Verification）時非常關鍵！
     */
    public User findByStudentNo(String studentNo) {
        String sql = "SELECT * FROM users WHERE student_no = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            rs.getString("role_level"),
                            rs.getString("status"),
                            rs.getString("created_at")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ 根據學號查詢使用者失敗: " + studentNo);
            e.printStackTrace();
        }
        return null;
    }
}