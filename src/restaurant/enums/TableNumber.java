package enums;

public enum TableNumber {
    // 先10張桌子就好
    TABLE_1("1號桌"),
    TABLE_2("2號桌"),
    TABLE_3("3號桌"),
    TABLE_4("4號桌"),
    TABLE_5("5號桌"),
    TABLE_6("6號桌"),
    TABLE_7("7號桌"),
    TABLE_8("8號桌"),
    TABLE_9("9號桌"),
    TABLE_10("10號桌"),

    TAKEOUT("外帶");

    private final String displayName;

    TableNumber(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // 判斷是否為內用
    public boolean isDineIn() {
        return this != TAKEOUT;
    }
}