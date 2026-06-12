package service;

import model.Member;
import enums.MemberTier;
import java.util.ArrayList;
import java.util.List;

public class MemberService {
    private List<Member> members = new ArrayList<>();
    private int nextMemberId = 1;

    // 註冊會員
    public Member registerMember(String name, String phone, int birthMonth, int birthDate) {
        // 檢查電話是否已註冊
        if (findMemberByPhone(phone) != null) {
            System.out.println("❌ 此電話號碼已註冊：" + phone);
            return null;
        }
        // 檢查生日月份是否合法
        if (!MonthDateValid(birthMonth, birthDate)) {
            System.out.println("❌ 生日無效：" + birthMonth + "/" + birthDate);
            return null;
        }
        Member member = new Member(nextMemberId++, name, phone, birthMonth, birthDate);
        members.add(member);
        System.out.println("✅ 會員註冊成功：" + member);
        return member;
    }

    // 用電話查詢會員
    public Member findMemberByPhone(String phone) {
        for (Member m : members) {
            if (m.getPhone().equals(phone)) {
                return m;
            }
        }
        return null;
    }

    // 累加消費金額，並自動判斷是否升級
    public void updateTotalSpent(Member member, int amount) {
        member.setTotalSpent(member.getTotalSpent() + amount);
        upgradeTier(member);
    }

    // 根據總消費重新判斷等級
    public void upgradeTier(Member member) {
        MemberTier newTier = MemberTier.fromTotalSpent(member.getTotalSpent());
        if (newTier != member.getTier()) {
            MemberTier oldTier = member.getTier();
            member.setTier(newTier);
            System.out.println("🎉 " + member.getName() + " 等級升級：" +
                    oldTier.getDisplayName() + " → " + newTier.getDisplayName());
        }
    }

    // 取得所有會員
    public List<Member> getAllMembers() {
        return members;
    }

    // 印出所有會員資料
    public void printAllMembers() {
        if (members.isEmpty()) {
            System.out.println("（目前沒有會員）");
            return;
        }
        System.out.println("══════ 會員列表 ══════");
        for (Member m : members) {
            System.out.println(m);
        }
        System.out.println("═════════════════════");
    }

    // helper:　確認生日有效
    public boolean MonthDateValid(int m, int d){
        if( m>0 && m<12 && d>0){
            if(m==2){
                if(d<30){
                    return true;
                }
            }else if(m==4||m==6||m==9||m==11){
                if(d<31){
                    return true;
                }
            }else{
                if(d<32){
                    return true;
                }
            }
        }
        return false;
    }
}