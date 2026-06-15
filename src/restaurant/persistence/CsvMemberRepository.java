package persistence;

import service.MemberService;

import java.io.BufferedReader;
import java.io.FileReader;

import model.Member;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

public class CsvMemberRepository {

    private static final String FILE_NAME = "member.csv";

    public void save(List<Member> members) {

        try ( BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))){

            for (Member member : members) {
                bw.write(
                        member.getName() + "," +
                        member.getPhone() + "," +
                        member.getBirthMonth() + "," +
                        member.getBirthDate()
                );
                bw.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load(MemberService memberService) {

        try ( BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))){

            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                String name = data[0];
                String phone = data[1];
                int birthMonth = Integer.parseInt(data[2]);
                int birthDate = Integer.parseInt(data[3]);
                memberService.registerMember(
                        name,
                        phone,
                        birthMonth,
                        birthDate
                );
            }
        } catch (Exception e) {
            System.out.println("member.csv不存在或讀取失敗");
        }
    }
}