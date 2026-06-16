import persistence.CsvMenuRepository;
import service.*;
import ui.OrderingFrame;
import persistence.*;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        MenuService menuService = new MenuService();
        CsvMenuRepository menuRepo = new CsvMenuRepository();
        menuRepo.load(menuService);

        MemberService memberService = new MemberService();
        CsvMemberRepository memberRepo = new CsvMemberRepository();
        memberRepo.load(memberService);

        OrderService orderService = new OrderService();
        ReportService reportService = new ReportService(orderService);


        SwingUtilities.invokeLater(() -> {
            new OrderingFrame(menuService, memberService, orderService, reportService);
        });
    }
}