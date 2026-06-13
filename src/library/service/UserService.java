package library.service;

import library.model.User;
import library.model.UserRole;
import library.model.UserStatus;
import library.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserService {

    private final UserRepository userRepository;

    public UserService() {
        this.userRepository = new UserRepository();
    }

    /**
     * 使用者登入
     */
    public Map<String, Object> login(String studentNo, String password) {

        if (studentNo == null || password == null) {
            return null;
        }

        User user =
                userRepository.findByStudentNo(
                        studentNo.trim()
                );

        if (user == null) {
            return null;
        }

        if (!user.getPassword().equals(password)) {
            return null;
        }

        Map<String, Object> userSession =
                new HashMap<>();

        userSession.put(
                "userId",
                user.getUserId()
        );

        userSession.put(
                "studentNo",
                user.getStudentNo()
        );

        userSession.put(
                "name",
                user.getName()
        );

        userSession.put(
                "roleLevel",
                user.getRoleLevel().name()
        );

        userSession.put(
                "status",
                user.getStatus().name()
        );

        return userSession;
    }

    /**
     * 使用者註冊
     */
    public String registerUser(
            String studentNo,
            String name,
            String password,
            String roleLevel
    ) {

        if (studentNo == null || studentNo.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || password == null || password.isEmpty()) {

            return "⚠️ 註冊失敗：所有欄位皆為必填！";
        }

        if (!"NORMAL".equalsIgnoreCase(roleLevel)
                && !"VIP".equalsIgnoreCase(roleLevel)) {

            return "❌ 系統錯誤：不合法的會員等級。";
        }

        User existingUser =
                userRepository.findByStudentNo(
                        studentNo.trim()
                );

        if (existingUser != null) {

            return "⚠️ 註冊失敗：此學號（"
                    + studentNo
                    + "）已被註冊！";
        }

        UserRole role =
                UserRole.valueOf(
                        roleLevel.toUpperCase()
                );

        User newUser =
                new User(
                        0,
                        studentNo.trim(),
                        name.trim(),
                        password,
                        role,
                        UserStatus.ACTIVE,
                        null
                );

        boolean success =
                userRepository.save(newUser);

        return success
                ? "SUCCESS"
                : "❌ 資料庫寫入錯誤，註冊未完成。";
    }

    /**
     * 管理員修改使用者狀態
     */
    public boolean updateUserStatus(
            int userId,
            String status
    ) {

        try {

            UserStatus newStatus =
                    UserStatus.valueOf(
                            status.toUpperCase()
                    );

            return userRepository.updateUserStatus(
                    userId,
                    newStatus
            );

        } catch (Exception e) {

            return false;

        }
    }

    /**
     * 管理員修改使用者角色
     */
    public boolean updateUserRole(
            int userId,
            String role
    ) {

        try {

            UserRole newRole =
                    UserRole.valueOf(
                            role.toUpperCase()
                    );

            return userRepository.updateUserRole(
                    userId,
                    newRole
            );

        } catch (Exception e) {

            return false;

        }
    }

    /**
     * 查詢單一使用者
     */
    public User getUserById(int userId) {

        return userRepository.findById(
                userId
        );
    }

    /**
     * 依學號查詢
     */
    public User getUserByStudentNo(
            String studentNo
    ) {

        return userRepository.findByStudentNo(
                studentNo
        );
    }

    /**
     * 管理員查詢學生列表
     */
    public List<Map<String, Object>>
    getAllStudents(String studentNoFilter) {

        List<Map<String, Object>> result =
                new ArrayList<>();

        List<User> users =
                userRepository.findAll();

        for (User user : users) {

            if (studentNoFilter != null
                    && !studentNoFilter.trim().isEmpty()) {

                if (!user.getStudentNo()
                        .contains(studentNoFilter)) {

                    continue;
                }
            }

            Map<String, Object> row =
                    new HashMap<>();

            row.put(
                    "userId",
                    user.getUserId()
            );

            row.put(
                    "studentNo",
                    user.getStudentNo()
            );

            row.put(
                    "name",
                    user.getName()
            );

            row.put(
                    "roleLevel",
                    user.getRoleLevel().name()
            );

            row.put(
                    "status",
                    user.getStatus().name()
            );

            result.add(row);
        }

        return result;
    }
}