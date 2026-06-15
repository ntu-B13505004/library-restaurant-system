package model;

import enums.MemberTier;

public class Member {
    private int        memberId;
    private String     name;
    private String     phone;
    private int        birthMonth;   // 1~12，用於生日優惠判斷
    private int        birthDate;
    private int        totalSpent;
    private MemberTier tier;

    // 建構子
    public Member(int memberId, String name, String phone, int birthMonth, int birthDate) {
        this.memberId   = memberId;
        this.name       = name;
        this.phone      = phone;
        this.birthMonth = birthMonth;
        this.birthDate  = birthDate;
        this.totalSpent = 0;
        this.tier       = MemberTier.BRONZE; // 新會員預設銅卡
    }

    // Getters
    public int        getMemberId()   { return memberId; }
    public String     getName()       { return name; }
    public String     getPhone()      { return phone; }
    public int        getBirthMonth() { return birthMonth; }
    public int        getBirthDate() { return birthDate; }
    public int        getTotalSpent() { return totalSpent; }
    public MemberTier getTier()       { return tier; }

    // Setters
    public void setTotalSpent(int totalSpent) { this.totalSpent = totalSpent; }
    public void setTier(MemberTier tier)      { this.tier = tier; }

    // 判斷當前月份是否為生日月（給 OrderService 計算折扣用）
    public boolean isBirthdayMonth(int currentMonth) {
        return this.birthMonth == currentMonth;
    }

    @Override
    public String toString() {
        return String.format("[%d] %s｜電話：%s｜等級：%s｜累積消費：$%d",
                memberId, name, phone, tier.getDisplayName(), totalSpent);
    }
}