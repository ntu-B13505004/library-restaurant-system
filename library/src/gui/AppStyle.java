package library.src.gui;

public class AppStyle {
    // 色彩代碼定義 (Hex)\

    public static final String PRIMARY = "#4A1525";       // 深酒紅（豪華書香側邊欄）
    public static final String ACCENT_LIGHT = "#C5A059";  // 香檳金（高雅點綴色）
    public static final String SECONDARY = "#5C2637";     // 淺酒紅（次要按鈕）
    public static final String BG_WINDOW = "#FAF6F0";     // 暖米白（舒適不傷眼背景）
    public static final String TEXT_PRIMARY = "#2B1A22";  // 焦糖深褐（主要文字）
    public static final String SUCCESS = "#01814A";       // 復古草綠（復權/成功）
    public static final String DANGER = "#A93226";        // 磚紅（停權/罰金）

    /**
     * 主要按鈕 CSS (Champagne Gold)
     */
    public static final String buttonPrimary() {
        return "-fx-background-color: " + ACCENT_LIGHT + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 13px;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8px 16px;" +
                "-fx-background-radius: 4px;" +
                "-fx-cursor: hand;";
    }

    /**
     * 次要按鈕 CSS (Muted Burgundy)
     */
    public static final String buttonSecondary() {
        return "-fx-background-color: " + SECONDARY + ";" +
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
                "-fx-border-color: #D5C7BC;" +
                "-fx-border-radius: 4px;" +
                "-fx-background-radius: 4px;" +
                "-fx-padding: 6px 10px;" +
                "-fx-text-fill: " + TEXT_PRIMARY + ";";
    }
}