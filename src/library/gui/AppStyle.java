package library.gui;

public class AppStyle {
    // 色彩代碼定義 (Hex)
    public static final String PRIMARY = "#2C3E50";       // 深藍灰色（側邊欄）
    public static final String ACCENT_LIGHT = "#1ABC9C";  // 湖水綠（點綴色）
    public static final String BG_WINDOW = "#F8F9FA";     // 淺灰背景
    public static final String TEXT_PRIMARY = "#2C3E50";  // 主要文字
    public static final String SUCCESS = "#2ECC71";       // 復權綠
    public static final String DANGER = "#E74C3C";        // 停權/罰金紅

    /**
     * 主要按鈕 CSS (Lake Green)
     */
    public static final String buttonPrimary() {
        return "-fx-background-color: #1ABC9C;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 4px;" +
                "-fx-cursor: hand;";
    }

    /**
     * 次要按鈕 CSS (Deep Blue Gray)
     */
    public static final String buttonSecondary() {
        return "-fx-background-color: #34495E;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 4px;" +
                "-fx-cursor: hand;";
    }

    /**
     * 輸入框統一 CSS
     */
    public static final String textField() {
        return "-fx-background-color: white;" +
                "-fx-border-color: #BDC3C7;" +
                "-fx-border-radius: 4px;" +
                "-fx-background-radius: 4px;" +
                "-fx-padding: 6px 10px;" +
                "-fx-text-fill: #34495E;";
    }
}