package library.gui;

public final class AppStyle {
    private AppStyle() {}

    // 主色調
    public static final String PRIMARY        = "#2D2F7B";   // 深靛藍
    public static final String PRIMARY_LIGHT  = "#4547A8";   // Hover 亮藍
    public static final String PRIMARY_DARK   = "#1C1E5C";   // Pressed 深藍
    public static final String ACCENT         = "#7C7FD4";   // 紫羅蘭 (VIP/高亮)
    public static final String ACCENT_LIGHT   = "#E8E9F8";   // 淡紫背景

    // 語意色
    public static final String SUCCESS        = "#27AE60";   // 成功綠
    public static final String WARNING        = "#E67E22";   // 警告橘
    public static final String DANGER         = "#C0392B";   // 錯誤紅
    public static final String BG_WINDOW      = "#F4F5F9";   // 視窗主背景
    public static final String TEXT_PRIMARY   = "#2C3E50";   // 主要文字
    public static final String TEXT_SECONDARY = "#7F8C8D";   // 次要文字

    // 常用 UI 元件風格化 CSS
    public static String buttonPrimary() {
        return "-fx-background-color: " + PRIMARY + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    public static String buttonSecondary() {
        return "-fx-background-color: white; -fx-text-fill: " + PRIMARY + "; -fx-border-color: " + PRIMARY + "; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    public static String textField() {
        return "-fx-background-color: white; -fx-border-color: #DCDDE1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 12; -fx-text-fill: " + TEXT_PRIMARY + ";";
    }

    public static String card() {
        return "-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.06), 10, 0, 0, 4);";
    }

    // 狀態晶片（Chips）樣式
    public static String chipAvailable() {
        return "-fx-background-color: #E8F8F5; -fx-text-fill: " + SUCCESS + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;";
    }

    public static String chipBorrowed() {
        return "-fx-background-color: #FEF9E7; -fx-text-fill: " + WARNING + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;";
    }

    public static String chipSuspended() {
        return "-fx-background-color: #FDEDEC; -fx-text-fill: " + DANGER + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 3 10;";
    }
}