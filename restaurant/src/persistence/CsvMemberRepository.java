package persistence;

import enums.MemberTier;
import model.Member;
import service.MemberService;

import java.io.*;
import java.util.List;

public class CsvMemberRepository {

    private static final String FILE_NAME = "member.csv";

    // 欄位順序：memberId, name, phone, birthMonth, birthDate, totalSpent, tier
    public void save(List<Member> members) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Member m : members) {
                bw.write(
                    m.getMemberId()        + "," +
                    m.getName()            + "," +
                    m.getPhone()           + "," +
                    m.getBirthMonth()      + "," +
                    m.getBirthDate()       + "," +
                    m.getTotalSpent()      + "," +
                    m.getTier().name()              // 存 enum 名稱，例如 "GOLD"
                );
                bw.newLine();
            }
            System.out.println("✅ 會員資料已儲存");
        } catch (Exception e) {
            System.out.println("❌ 會員資料儲存失敗：" + e.getMessage());
        }
    }

    // 直接還原 Member 物件，不走 registerMember()，避免 id 重設、totalSpent 歸零
    public void load(MemberService memberService) {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("（member.csv 不存在，略過載入）");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int maxId = 0;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");

                int    memberId   = Integer.parseInt(data[0]);
                String name       = data[1];
                String phone      = data[2];
                int    birthMonth = Integer.parseInt(data[3]);
                int    birthDate  = Integer.parseInt(data[4]);
                int    totalSpent = Integer.parseInt(data[5]);
                MemberTier tier   = MemberTier.valueOf(data[6]);

                // 直接建物件並還原所有欄位
                Member m = new Member(memberId, name, phone, birthMonth, birthDate);
                m.setTotalSpent(totalSpent);
                m.setTier(tier);

                memberService.loadMember(m);        // 見下方說明
                if (memberId > maxId) maxId = memberId;
            }

            memberService.setNextMemberId(maxId + 1); // 確保下一個 id 不重複
            System.out.println("✅ 會員資料載入完成");

        } catch (Exception e) {
            System.out.println("❌ 會員資料載入失敗：" + e.getMessage());
        }
    }
}