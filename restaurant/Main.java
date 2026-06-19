import persistence.CsvMemberRepository;
import persistence.CsvMenuRepository;
import service.*;
import ui.OrderingFrame;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {

        // ── Services ──────────────────────────
        MenuService   menuService   = new MenuService();
        MemberService memberService = new MemberService();
        OrderService  orderService  = new OrderService();
        ReportService reportService = new ReportService(orderService);

        // ── 載入 CSV ──────────────────────────
        CsvMenuRepository   menuRepo   = new CsvMenuRepository();
        CsvMemberRepository memberRepo = new CsvMemberRepository();

        menuRepo.load(menuService);
        memberRepo.load(memberService);

        // ── 關視窗時自動存檔 ──────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            menuRepo.save(menuService.getAllItemsIncludeDeleted());
            memberRepo.save(memberService.getAllMembers());
            System.out.println("✅ 資料已儲存，程式結束");
        }));

        // ── 啟動 Swing 介面 ───────────────────
        SwingUtilities.invokeLater(() ->
            new OrderingFrame(menuService, memberService, orderService, reportService)
        );
    }
}