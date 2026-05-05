package library.model;

public class User {
    private int userId;
    private String name;
    private String email;
    private UserRole userRole;
    private int maxBooks; // 最多可借幾本

    public User(int userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.userRole = UserRole.READER;
        this.maxBooks = 3; // 預設上限3本
    }

    public boolean canBorrow(int currentBorrowCount) {
        return currentBorrowCount < this.maxBooks;
    }

    public int getBorrowLimit() {
        return maxBooks;
    }

    // Getters & Setters
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserRole getUserRole() { return userRole; }
    public void setUserRole(UserRole userRole) { this.userRole = userRole; }
    public void setMaxBooks(int maxBooks) { this.maxBooks = maxBooks; }
}