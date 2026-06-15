package ui;

import enums.TableNumber;
import service.*;

import javax.swing.*;
import java.awt.*;

public class OrderingFrame extends JFrame {

    private final MenuService menuService;
    private final MemberService memberService;
    private final OrderService orderService;
    private final ReportService reportService;

    public OrderingFrame(
            MenuService menuService,
            MemberService memberService,
            OrderService orderService,
            ReportService reportService
    ) {

        this.menuService = menuService;
        this.memberService = memberService;
        this.orderService = orderService;
        this.reportService = reportService;

        initializeUI();
    }

    private void initializeUI() {

        setTitle("餐廳點餐系統");
        setSize(1200,700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        //---------------------------------
        // 菜單區
        //---------------------------------

        JPanel menuPanel = new JPanel();

        menuPanel.add(new JLabel("菜單區"));

        add(menuPanel, BorderLayout.CENTER);

        //---------------------------------
        // 購物車區
        //---------------------------------

        JPanel cartPanel = new JPanel();

        cartPanel.add(new JLabel("購物車"));

        cartPanel.setPreferredSize(
                new Dimension(300,0)
        );

        add(cartPanel, BorderLayout.EAST);

        //---------------------------------
        // 底部區
        //---------------------------------

        JPanel bottomPanel = new JPanel();

        JComboBox<TableNumber> tableBox =
                new JComboBox<>(TableNumber.values());

        JTextField phoneField =
                new JTextField(10);

        JButton checkoutBtn =
                new JButton("結帳");

        bottomPanel.add(new JLabel("桌號"));
        bottomPanel.add(tableBox);

        bottomPanel.add(new JLabel("會員"));
        bottomPanel.add(phoneField);

        bottomPanel.add(checkoutBtn);

        add(bottomPanel, BorderLayout.SOUTH);

        //---------------------------------
        // 後台按鈕
        //---------------------------------

        JButton adminButton =
                new JButton("後台管理");

        adminButton.addActionListener(e -> {

            new AdminFrame(
                    menuService,
                    memberService,
                    orderService,
                    reportService
            );

        });

        JPanel topPanel = new JPanel(
                new FlowLayout(FlowLayout.RIGHT)
        );

        topPanel.add(adminButton);

        add(topPanel, BorderLayout.NORTH);

        setVisible(true);
    }
}