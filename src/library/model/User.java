package library.model;

public class User {
    private int userId;
    private String studentNo;    // 學號
    private String name;
    private String password;     // 密碼
    private String roleLevel;    // 權限等級：NORMAL / VIP / ADMIN
    private String status;       // 狀態：ACTIVE / SUSPENDED
    private String createdAt;    // 帳號建立時間

    // ✨ 完整欄位建構子：供新版 UserRepository 使用
    public User(int userId, String studentNo, String name, String password, UserRole roleLevel, String status, String createdAt) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.name = name;
        this.password = password;
        this.roleLevel = roleLevel;
        this.status = status;
        this.createdAt = createdAt;
    }

    // 💡 保留舊版建構子以防其他地方開天窗，並自動將舊欄位安全地對接到新架構中
    public User(int userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.studentNo = email; // 暫時以傳入的 email 當作代稱
        this.roleLevel = "NORMAL";
        this.status = "ACTIVE";
    }

    // 🎯 核心邏輯優化：依據簡報規範之身份，動態回傳借書上限
    public int getBorrowLimit() {
        if ("VIP".equalsIgnoreCase(roleLevel)) {
            return 5; // 舉例：VIP 可借 5 本
        } else if ("ADMIN".equalsIgnoreCase(roleLevel)) {
            return 999; // 管理員不限
        }
        return 3; // NORMAL 預設 3 本
    }

    // 🎯 核心邏輯優化：不只要看數量有沒有超標，還要看這個使用者有沒有被停權（SUSPENDED）！
    public boolean canBorrow(int currentBorrowCount) {
        // 只有處於 ACTIVE 狀態，且目前借閱量小於該身份上限的人才可以借書
        return "ACTIVE".equalsIgnoreCase(this.status) && currentBorrowCount < getBorrowLimit();
    }

    // Getters & Setters
    public int getUserId() { return userId; }
    public String getStudentNo() { return studentNo; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getRoleLevel() { return roleLevel; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setRoleLevel(String roleLevel) { this.roleLevel = roleLevel; }
}