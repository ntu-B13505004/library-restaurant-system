package library.src.model;

import java.time.LocalDateTime;

public class User {
    private int userId;
    private String studentNo;    // 學號
    private String name;
    private String password;     // 密碼
    private UserRole roleLevel;    // 權限等級：NORMAL / VIP / ADMIN
    private UserStatus status;       // 狀態：ACTIVE / SUSPENDED
    private LocalDateTime createdAt;    // 帳號建立時間

    // ✨ 完整欄位建構子：供新版 UserRepository 使用
    public User(int userId, String studentNo, String name, String password, UserRole roleLevel, UserStatus status, LocalDateTime createdAt) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.name = name;
        this.password = password;
        this.roleLevel = roleLevel;
        this.status = status;
        this.createdAt = createdAt;
    }

    // 🎯 核心邏輯優化：依據簡報規範之身份，動態回傳借書上限
    public int getBorrowLimit() {
        switch (roleLevel) {
            case VIP:
                return 5;
            case ADMIN:
                return 999;
            default:
                return 3;
        }
    }

    // 🎯 核心邏輯優化：不只要看數量有沒有超標，還要看這個使用者有沒有被停權（SUSPENDED）！
    public boolean canBorrow(int currentBorrowCount) {
        // 只有處於 ACTIVE 狀態，且目前借閱量小於該身份上限的人才可以借書
        return status == UserStatus.ACTIVE
                && currentBorrowCount < getBorrowLimit();    }

    // Getters & Setters
    public int getUserId() { return userId; }
    public String getStudentNo() { return studentNo; }
    public String getName() { return name; }
    public String getPassword() { return password; }
    public UserRole getRoleLevel() { return roleLevel; }
    public void setRoleLevel(UserRole roleLevel) { this.roleLevel = roleLevel; }
    public LocalDateTime  getCreatedAt() { return createdAt; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }}