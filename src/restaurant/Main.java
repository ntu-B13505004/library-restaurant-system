import enums.*;
import model.*;
import service.*;
import ui.OrderingFrame;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        MenuService menuService = new MenuService();
        MemberService memberService = new MemberService();
        OrderService orderService = new OrderService();
        ReportService reportService = new ReportService(orderService);

        SwingUtilities.invokeLater(() -> {

            new OrderingFrame(
                    menuService,
                    memberService,
                    orderService,
                    reportService
            );

        });
    }
}