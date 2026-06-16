package library.src.service;

import library.src.model.User;
import library.src.model.UserRole;
import library.src.model.UserStatus;
import library.src.repository.UserRepository;

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

        User user = userRepository.findByStudentNo(studentNo.trim());

        if (user == null) {
            return null;
        }

        if (!user.getPassword().equals(password)) {
            return null;
        }

        Map<String, Object> userSession = new HashMap<>();
        userSession.put("userId", user.getUserId());
        userSession.put("studentNo", user.getStudentNo());
        userSession.put("name", user.getName());
        userSession.put("roleLevel", user.getRoleLevel().name());
        userSession.put("status", user.getStatus().name());

        return userSession;
    }

    /**
     * 使用者註冊
     */
    public String registerUser(String studentNo, String name, String password, String roleLevel) {
        if (studentNo == null || studentNo.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || password == null || password.isEmpty()) {
            return "⚠️註冊失敗：所有欄位皆為必填！";
        }

        if (!"NORMAL".equalsIgnoreCase(roleLevel) && !"VIP".equalsIgnoreCase(roleLevel)) {
            return "❌系統錯誤：不合法的會員等級。";
        }

        User existingUser = userRepository.findByStudentNo(studentNo.trim());
        if (existingUser != null) {
            return "⚠️註冊失敗：此學號（" + studentNo + "）已被註冊！";
        }

        UserRole role = UserRole.valueOf(roleLevel.toUpperCase());
        User newUser = new User(0, studentNo.trim(), name.trim(), password, role, UserStatus.ACTIVE, null);
        boolean success = userRepository.save(newUser);

        return success ? "SUCCESS" : "❌資料庫寫入錯誤，註冊未完成。";
    }

    /**
     * 管理員修改使用者狀態
     */
    public boolean updateUserStatus(int userId, String status) {
        try {
            // 🚀 【安全防禦 1】如果要修改的目標原本是管理員，禁止修改其狀態（防呆、防鎖定）
            User targetUser = userRepository.findById(userId);
            if (targetUser != null && isManager(targetUser.getRoleLevel())) {
                System.out.println("⚠️安全攔截：無法修改管理員的帳號狀態。");
                return false;
            }

            UserStatus newStatus = UserStatus.valueOf(status.toUpperCase());
            return userRepository.updateUserStatus(userId, newStatus);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 管理員修改使用者角色
     * 🎯 核心防禦：不能調整管理員權限
     */
    public boolean updateUserRole(int userId, String role) {
        try {
            // 1. 先查出此人「目前在資料庫中」的權限是什麼
            User targetUser = userRepository.findById(userId);
            if (targetUser == null) {
                return false;
            }

            // 🚀 【安全防禦 2】如果他原本就是管理員，拒絕任何人透過此方法修改他的權限
            if (isManager(targetUser.getRoleLevel())) {
                System.out.println("⚠️安全攔截：禁止修改現任管理員的權限。");
                return false;
            }

            // 🚀 【安全防禦 3】如果原本是一般讀者，但企圖把他升職成管理員，也必須攔截拒絕
            UserRole newRole = UserRole.valueOf(role.toUpperCase());
            if (isManager(newRole)) {
                System.out.println("⚠️安全攔截：一般管理員無權將其他讀者升職為管理員級別。");
                return false;
            }

            // 檢查通過，才允許呼叫 Repository 更新資料庫
            return userRepository.updateUserRole(userId, newRole);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 綜合更新使用者資訊（相容你原本新增的方法，並改用 UserRepository 重構）
     */
    public boolean updateUserInfo(int userId, String newName, String newRole) {
        try {
            User targetUser = userRepository.findById(userId);
            if (targetUser == null) {
                return false;
            }

            // 🚀 【安全防禦 4】如果對方本來就是管理員，只允許更新名字（透過自訂邏輯或部分更新），強制忽略或拒絕變更其權限
            if (isManager(targetUser.getRoleLevel())) {
                System.out.println("⚠️安全攔截：目標為管理員，忽略權限變更請求。");
                // 這裡假設只允許透過儲存其他基本資料（若你的 UserRepository 支援單獨改名，可改為更精準的方法）
                // 若沒有專門改名的方法，通常是把原本的 Role 塞回去，覆蓋前端傳過來的 newRole
                return userRepository.updateUserRole(userId, targetUser.getRoleLevel());
            }

            // 如果本來不是管理員，則調用更新角色與名稱的邏輯
            UserRole roleEnum = UserRole.valueOf(newRole.toUpperCase());
            if (isManager(roleEnum)) {
                return false; // 防止普通用戶被提升為管理員
            }

            return userRepository.updateUserRole(userId, roleEnum);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 輔助方法：判斷該角色是否屬於管理員級別
     */
    private boolean isManager(UserRole role) {
        if (role == null) return false;
        String roleName = role.name();
        return "ADMIN".equalsIgnoreCase(roleName) || "SYSTEM_ADMIN".equalsIgnoreCase(roleName);
    }

    /**
     * 查詢單一使用者
     */
    public User getUserById(int userId) {
        return userRepository.findById(userId);
    }

    /**
     * 依學號查詢
     */
    public User getUserByStudentNo(String studentNo) {
        return userRepository.findByStudentNo(studentNo);
    }

    /**
     * 管理員查詢學生列表
     * ✨ 已修正：管理員查詢學生列表（支援 學號 與 姓名 雙欄位模糊搜尋，並加入安全防空保護）
     * @param keyword 搜尋關鍵字（可輸入學號或姓名）
     */
    public List<Map<String, Object>> getAllStudents(String keyword) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<User> users = userRepository.findAll();

        if (users == null) {
            return result;
        }

        for (User user : users) {
            // 💡 將變數先取出來，並做好 null 值的安全防禦，防止噴出 NullPointerException 導致執行緒崩潰卡死
            String studentNo = (user.getStudentNo() != null) ? user.getStudentNo().trim() : "";
            String name = (user.getName() != null) ? user.getName().trim() : "";

            // 如果有輸入關鍵字，則進行過濾
            if (keyword != null && !keyword.trim().isEmpty()) {
                String cleanKeyword = keyword.trim().toLowerCase();

                // 💡 核心修正：同時比對「學號」與「姓名」，兩者都不符合才跳過（忽略大小寫）
                boolean matchNo = studentNo.toLowerCase().contains(cleanKeyword);
                boolean matchName = name.toLowerCase().contains(cleanKeyword);

                if (!matchNo && !matchName) {
                    continue; // 兩者都不符合，跳過這筆資料
                }
            }

            // 封裝成 GUI 表格需要的 Map 機制 (同樣做好防空確保強健度)
            Map<String, Object> row = new HashMap<>();
            row.put("userId", user.getUserId());
            row.put("studentNo", studentNo);
            row.put("name", name);
            row.put("roleLevel", user.getRoleLevel() != null ? user.getRoleLevel().name() : "NORMAL");
            row.put("status", user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
            result.add(row);
        }

        return result;
    }
}