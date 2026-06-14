package library.gui;

/**
 * 全域 UI 色票與樣式常數
 * 統一從這裡取值，避免散落各處的魔術字串。
 *
 * 設計方向：以深靛藍（#2D2F7B）為主色，對應簡報配色；
 * 搭配乾淨的灰白底色與柔和的紫羅蘭輔色，
 * 傳遞專業、可信賴的圖書館形象。
 */
public final class AppStyle {

    private AppStyle() {}

    // ── 主色 ──────────────────────────────────────
    public static final String PRIMARY        = "#2D2F7B";   // 深靛藍
    public static final String PRIMARY_LIGHT  = "#4547A8";   // 稍亮靛藍（hover）
    public static final String PRIMARY_DARK   = "#1C1E5C";   // 更深靛藍（pressed）

    // ── 輔色 ──────────────────────────────────────
    public static final String ACCENT         = "#7C7FD4";   // 紫羅蘭（VIP 標籤、高亮）
    public static final String ACCENT_LIGHT   = "#E8E9F8";   // 淡紫（背景 chip）

    // ── 語意色 ────────────────────────────────────
    public static final String SUCCESS        = "#27AE60";
    public static final String WARNING        = "#E67E22";
    public static final String DANGER         = "#C0392B";
    public static final String INFO           = "#2980B9";

    // ── 中性色 ────────────────────────────────────
    public static final String BG_PAGE        = "#F5F6FA";   // 頁面底色
    public static final String BG_CARD        = "#FFFFFF";   // 卡片底色
    public static final String BG_SIDEBAR     = "#1E2060";   // 側邊欄底色
    public static final String BORDER         = "#DDE1EE";   // 邊框
    public static final String TEXT_PRIMARY   = "#1A1C42";   // 主要文字
    public static final String TEXT_SECONDARY = "#6B7399";   // 次要文字
    public static final String TEXT_INVERSE   = "#FFFFFF";   // 反底文字

    // ── 字型 ──────────────────────────────────────
    public static final String FONT_FAMILY    = "Noto Sans TC, Microsoft JhengHei, sans-serif";

    // ── 通用 inline style helpers ──────────────────

    /** 主要按鈕 */
    public static String btnPrimary() {
        return "-fx-background-color: " + PRIMARY + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 14px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 10 24 10 24;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;";
    }

    /** 次要（輪廓）按鈕 */
    public static String btnOutline() {
        return "-fx-background-color: transparent;"
                + "-fx-text-fill: " + PRIMARY + ";"
                + "-fx-font-size: 14px;"
                + "-fx-border-color: " + PRIMARY + ";"
                + "-fx-border-width: 1.5;"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 9 22 9 22;"
                + "-fx-cursor: hand;";
    }

    /** 危險按鈕（停權 / 刪除） */
    public static String btnDanger() {
        return "-fx-background-color: " + DANGER + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 8 18 8 18;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;";
    }

    /** 成功按鈕（還書 / 復權） */
    public static String btnSuccess() {
        return "-fx-background-color: " + SUCCESS + ";"
                + "-fx-text-fill: white;"
                + "-fx-font-size: 13px;"
                + "-fx-font-weight: bold;"
                + "-fx-padding: 8 18 8 18;"
                + "-fx-background-radius: 8;"
                + "-fx-cursor: hand;";
    }

    /** 輸入框 */
    public static String textField() {
        return "-fx-background-color: " + BG_PAGE + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;"
                + "-fx-padding: 10 14;"
                + "-fx-font-size: 14px;"
                + "-fx-prompt-text-fill: " + TEXT_SECONDARY + ";";
    }

    /** 卡片容器 */
    public static String card() {
        return "-fx-background-color: " + BG_CARD + ";"
                + "-fx-background-radius: 12;"
                + "-fx-effect: dropshadow(gaussian, rgba(45,47,123,0.08), 12, 0, 0, 3);";
    }

    /** 側邊欄底色 */
    public static String sidebar() {
        return "-fx-background-color: " + BG_SIDEBAR + ";";
    }

    /** 頁面底色 */
    public static String pageBackground() {
        return "-fx-background-color: " + BG_PAGE + ";";
    }

    /** 標題 label */
    public static String labelTitle() {
        return "-fx-font-size: 22px;"
                + "-fx-font-weight: bold;"
                + "-fx-text-fill: " + TEXT_PRIMARY + ";";
    }

    /** 小標 label */
    public static String labelSubtitle() {
        return "-fx-font-size: 14px;"
                + "-fx-text-fill: " + TEXT_SECONDARY + ";";
    }

    /** 狀態 Chip：可借 */
    public static String chipAvailable() {
        return "-fx-background-color: #D5F0E3;"
                + "-fx-text-fill: #1D7A4A;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 20;"
                + "-fx-padding: 3 10 3 10;";
    }

    /** 狀態 Chip：已借出 */
    public static String chipBorrowed() {
        return "-fx-background-color: #FCE8D5;"
                + "-fx-text-fill: #A0440A;"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 20;"
                + "-fx-padding: 3 10 3 10;";
    }

    /** VIP 角色 Chip */
    public static String chipVip() {
        return "-fx-background-color: " + ACCENT_LIGHT + ";"
                + "-fx-text-fill: " + PRIMARY + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 20;"
                + "-fx-padding: 3 10 3 10;";
    }

    /** 停權 Chip */
    public static String chipSuspended() {
        return "-fx-background-color: #FAD7D7;"
                + "-fx-text-fill: " + DANGER + ";"
                + "-fx-font-size: 12px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 20;"
                + "-fx-padding: 3 10 3 10;";
    }

    /** TableView 全域樣式 CSS 字串（用於 scene.getStylesheets 無效時，內嵌到 setStyle） */
    public static String tableStyle() {
        return "-fx-background-color: transparent;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 8;"
                + "-fx-background-radius: 8;";
    }
}