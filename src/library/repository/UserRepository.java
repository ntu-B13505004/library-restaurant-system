package library.repository;

import library.database.DatabaseManager;
import library.model.*;

import java.sql.*;

public class UserRepository {

    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            UserRole.valueOf(rs.getString("role_level").toUpperCase().trim()),
                            UserStatus.valueOf(rs.getString("status").toUpperCase().trim()),
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

    public User findByStudentNo(String studentNo) {
        String sql = "SELECT * FROM users WHERE student_no = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, studentNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    UserRole role = UserRole.valueOf(rs.getString("role_level").toUpperCase().trim());
                    UserStatus status = UserStatus.valueOf(rs.getString("status").toUpperCase().trim());

                    return new User(
                            rs.getInt("user_id"),
                            rs.getString("student_no"),
                            rs.getString("name"),
                            rs.getString("password"),
                            role,
                            status,
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

    public boolean updateUserStatus(int userId, UserStatus newStatus) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.name());
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 更新使用者狀態失敗，User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * ✨ 新增功能：變更使用者角色等級（用於管理者設定或升級 VIP/ADMIN）
     */
    public boolean updateUserRole(int userId, UserRole newRole) {
        String sql = "UPDATE users SET role_level = ? WHERE user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.name()); // 傳入 "ADMIN", "VIP", 或 "NORMAL"
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ 更新使用者權限等級失敗，User ID: " + userId);
            e.printStackTrace();
            return false;
        }
    }
}