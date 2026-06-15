package ui;

import service.*;

import javax.swing.*;

public class AdminFrame extends JFrame {

    public AdminFrame(MenuService menuService, MemberService memberService,
                      OrderService orderService, ReportService reportService) {

        setTitle("後台管理系統");
        setSize(1000, 700);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("訂單管理", new OrderPanel(orderService));
        tabs.addTab("菜單管理", new MenuManagePanel(menuService));
        tabs.addTab("會員管理", new MemberPanel(memberService));
        tabs.addTab("今日報表", new ReportPanel(reportService));

        add(tabs);
        setVisible(true);
    }
}