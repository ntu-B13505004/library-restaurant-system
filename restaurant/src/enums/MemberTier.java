package enums;

public enum MemberTier {
    // 為方便先改成   銅：0~499   銀：500~999     金：999+
    //              等級名稱                折扣率              升級門檻(總消費)
    BRONZE     ("銅卡會員", 0.95,   0),
    SILVER     ("銀卡會員", 0.90,   500),
    GOLD       ("金卡會員", 0.85,   1000);

    private final String displayName;
    private final double discountRate;    // 一般折扣率
    private final int    upgradeThreshold; // 升級所需累積消費

    // 生日當月額外折扣（銀/金限定，銅卡不適用）
    // e.g. 銀卡+當月生日= 0.9*0.95= 0.855
    public static final double BIRTHDAY_DISCOUNT = 0.95;

    MemberTier(String displayName, double discountRate, int upgradeThreshold) {
        this.displayName       = displayName;
        this.discountRate      = discountRate;
        this.upgradeThreshold  = upgradeThreshold;
    }

    public String getDisplayName()    { return displayName; }
    public double getDiscountRate()   { return discountRate; }
    public int getUpgradeThreshold()  { return upgradeThreshold; }

    // 計算實際折扣率（考慮生日優惠）
    // isBirthdayMonth：當前月份是否為會員生日月
    public double getFinalDiscount(boolean isBirthdayMonth) {
        if (isBirthdayMonth && this != BRONZE) {
            return discountRate * BIRTHDAY_DISCOUNT;
        }
        return discountRate;
    }

    // 根據總消費金額決定應有的等級
    public static MemberTier fromTotalSpent(int totalSpent) {
        if (totalSpent >= GOLD.upgradeThreshold)   return GOLD;
        if (totalSpent >= SILVER.upgradeThreshold) return SILVER;
        return BRONZE;
    }
}