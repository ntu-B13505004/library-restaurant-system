package library.repository;

import library.database.DatabaseManager;
import library.model.User;
import library.model.UserRole;
import library.model.UserStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    /**
     * ResultSet → User
     */
    private User mapResultSetToUser(ResultSet rs)
            throws SQLException {

        return new User(
                rs.getInt("user_id"),
                rs.getString("student_no"),
                rs.getString("name"),
                rs.getString("password"),
                UserRole.valueOf(
                        rs.getString("role_level")
                                .toUpperCase()
                                .trim()
                ),
                UserStatus.valueOf(
                        rs.getString("status")
                                .toUpperCase()
                                .trim()
                ),
                rs.getTimestamp("created_at").toLocalDateTime()        );
    }

    /**
     * 依 ID 查詢
     */
    public User findById(int userId) {

        String sql =
                "SELECT * FROM users " +
                        "WHERE user_id = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "❌ 根據 ID 查詢使用者失敗: "
                            + userId
            );

            e.printStackTrace();
        }

        return null;
    }

    /**
     * 依學號查詢
     */
    public User findByStudentNo(String studentNo) {

        String sql =
                "SELECT * FROM users " +
                        "WHERE student_no = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, studentNo);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {

            System.err.println(
                    "❌ 根據學號查詢使用者失敗: "
                            + studentNo
            );

            e.printStackTrace();
        }

        return null;
    }

    /**
     * 登入驗證
     */
    public User findByStudentNoAndPassword(
            String studentNo,
            String password
    ) {

        String sql =
                "SELECT * FROM users " +
                        "WHERE student_no = ? " +
                        "AND password = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, studentNo);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }

        } catch (SQLException e) {

            System.err.println("❌ 登入查詢失敗");
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 新增使用者
     */
    public boolean save(User user) {

        String sql =
                "INSERT INTO users " +
                        "(student_no, name, password, role_level, status) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(
                    1,
                    user.getStudentNo()
            );

            pstmt.setString(
                    2,
                    user.getName()
            );

            pstmt.setString(
                    3,
                    user.getPassword()
            );

            pstmt.setString(
                    4,
                    user.getRoleLevel().name()
            );

            pstmt.setString(
                    5,
                    user.getStatus().name()
            );

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("❌ 新增使用者失敗");
            e.printStackTrace();

            return false;
        }
    }

    /**
     * 更新使用者狀態
     */
    public boolean updateUserStatus(
            int userId,
            UserStatus newStatus
    ) {

        String sql =
                "UPDATE users " +
                        "SET status = ? " +
                        "WHERE user_id = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(
                    1,
                    newStatus.name()
            );

            pstmt.setInt(
                    2,
                    userId
            );

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "❌ 更新使用者狀態失敗，User ID: "
                            + userId
            );

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 更新角色
     */
    public boolean updateUserRole(
            int userId,
            UserRole newRole
    ) {

        String sql =
                "UPDATE users " +
                        "SET role_level = ? " +
                        "WHERE user_id = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(
                    1,
                    newRole.name()
            );

            pstmt.setInt(
                    2,
                    userId
            );

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println(
                    "❌ 更新使用者權限失敗，User ID: "
                            + userId
            );

            e.printStackTrace();

            return false;
        }
    }

    /**
     * 查詢全部使用者
     */
    public List<User> findAll() {

        List<User> users =
                new ArrayList<>();

        String sql =
                "SELECT * FROM users " +
                        "ORDER BY user_id";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {

            while (rs.next()) {

                users.add(
                        mapResultSetToUser(rs)
                );
            }

        } catch (SQLException e) {

            System.err.println(
                    "❌ 查詢全部使用者失敗"
            );

            e.printStackTrace();
        }

        return users;
    }

    /**
     * 判斷學號是否存在
     */
    public boolean existsByStudentNo(
            String studentNo
    ) {

        String sql =
                "SELECT COUNT(*) " +
                        "FROM users " +
                        "WHERE student_no = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, studentNo);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return false;
    }
}