package enums;

public enum OrderStatus {
    PENDING("未接單"),
    PREPARING("製作中"),
    COMPLETED("已完成");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}