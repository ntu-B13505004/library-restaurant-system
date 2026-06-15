package ui;

import enums.TableNumber;
import model.Member;
import model.MenuItem;
import model.Order;
import model.OrderDetail;
import service.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OrderingFrame extends JFrame {

    // ── Services ──────────────────────────────
    private final MenuService   menuService;
    private final MemberService memberService;
    private final OrderService  orderService;
    private final ReportService reportService;

    // ── 狀態 ──────────────────────────────────
    private TableNumber         selectedTable  = null;
    private Member              selectedMember = null;
    private final List<OrderDetail> cart       = new ArrayList<>();

    // ── UI 元件 ───────────────────────────────
    private JPanel            menuPanel;
    private DefaultTableModel cartModel;
    private JLabel            totalLabel;
    private JLabel            tableLabel;
    private JLabel            memberLabel;

    public OrderingFrame(MenuService menuService, MemberService memberService,
                         OrderService orderService, ReportService reportService) {
        this.menuService   = menuService;
        this.memberService = memberService;
        this.orderService  = orderService;
        this.reportService = reportService;

        setTitle("餐廳點餐系統");
        setSize(1200, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        initializeUI();
        refreshMenu();
        setVisible(true);
    }

    // ══════════════════════════════════════════
    //  建立 UI
    // ══════════════════════════════════════════

    private void initializeUI() {
        setLayout(new BorderLayout());
        add(buildTopPanel(),  BorderLayout.NORTH);
        add(buildMenuPanel(), BorderLayout.CENTER);
        add(buildCartPanel(), BorderLayout.EAST);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    // ── 頂部：後台按鈕 ──────────────────────
    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton adminBtn = new JButton("⚙ 後台管理");
        adminBtn.addActionListener(e ->
            new AdminFrame(menuService, memberService, orderService, reportService)
        );

        panel.add(adminBtn);
        return panel;
    }

    // ── 中央：菜單卡片 ──────────────────────
    private JScrollPane buildMenuPanel() {
        menuPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scroll = new JScrollPane(menuPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void refreshMenu() {
        menuPanel.removeAll();

        List<MenuItem> items = menuService.getAvailableItems();
        for (MenuItem item : items) {
            JButton btn = new JButton(
                "<html><center>" + item.getItemName() + "<br>$" + item.getPrice() + "</center></html>"
            );
            btn.setPreferredSize(new Dimension(120, 80));
            btn.addActionListener(e -> addToCart(item));
            menuPanel.add(btn);
        }

        if (items.isEmpty()) {
            menuPanel.add(new JLabel("目前沒有可供應的餐點"));
        }

        menuPanel.revalidate();
        menuPanel.repaint();
    }

    // ── 右側：購物車 ────────────────────────
    private JPanel buildCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(340, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 10));

        // 標題
        panel.add(new JLabel("🛒 購物車"), BorderLayout.NORTH);

        // 表格（數量與備註可編輯）
        String[] cols = { "餐點", "數量", "備註", "小計" };
        cartModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 1 || col == 2;
            }
        };

        JTable cartTable = new JTable(cartModel);
        cartTable.setRowHeight(28);
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(50);

        // 數量或備註改動後同步 cart
        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == 1 || e.getColumn() == 2) {
                syncCartFromTable();
            }
        });

        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // 合計 + 按鈕
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 6));

        totalLabel = new JLabel("合計：$0");
        totalLabel.setFont(totalLabel.getFont().deriveFont(Font.BOLD, 14f));

        JButton clearBtn    = new JButton("清空購物車");
        JButton checkoutBtn = new JButton("✓ 送出訂單");

        clearBtn.addActionListener(e    -> clearCart());
        checkoutBtn.addActionListener(e -> checkout());

        bottom.add(totalLabel);
        bottom.add(clearBtn);
        bottom.add(checkoutBtn);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ── 底部：桌號 + 會員 ───────────────────
    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));

        tableLabel  = new JLabel("桌號：尚未選擇");
        memberLabel = new JLabel("會員：非會員");

        JButton tableBtn  = new JButton("選擇桌號");
        JButton memberBtn = new JButton("使用會員");

        tableBtn.addActionListener(e  -> selectTable());
        memberBtn.addActionListener(e -> selectMember());

        panel.add(tableLabel);
        panel.add(tableBtn);
        panel.add(new JSeparator(SwingConstants.VERTICAL));
        panel.add(memberLabel);
        panel.add(memberBtn);

        return panel;
    }

    // ══════════════════════════════════════════
    //  點餐邏輯
    // ══════════════════════════════════════════

    private void addToCart(MenuItem item) {
        String input = JOptionPane.showInputDialog(this,
            "「" + item.getItemName() + "」要幾份？", "輸入數量", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        int qty;
        try {
            qty = Integer.parseInt(input.trim());
            if (qty <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效數量！", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        OrderDetail detail = orderService.createDetail(item, qty, "");
        cart.add(detail);
        cartModel.addRow(new Object[]{ item.getItemName(), qty, "", "$" + detail.getSubtotal() });
        updateTotal();
    }

    private void syncCartFromTable() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < cart.size(); i++) {
                try {
                    int qty = Integer.parseInt(cartModel.getValueAt(i, 1).toString().trim());
                    String note = cartModel.getValueAt(i, 2) != null
                        ? cartModel.getValueAt(i, 2).toString() : "";

                    OrderDetail updated = orderService.createDetail(cart.get(i).getItem(), qty, note);
                    cart.set(i, updated);
                    cartModel.setValueAt("$" + updated.getSubtotal(), i, 3);
                } catch (Exception ignored) {}
            }
            updateTotal();
        });
    }

    private void updateTotal() {
        int total = cart.stream().mapToInt(OrderDetail::getSubtotal).sum();
        totalLabel.setText("合計：$" + total);
    }

    private void clearCart() {
        if (cart.isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this,
            "確定要清空購物車？", "清空確認", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        cart.clear();
        cartModel.setRowCount(0);
        updateTotal();
    }

    // ══════════════════════════════════════════
    //  選桌號 / 會員
    // ══════════════════════════════════════════

    private void selectTable() {
        TableNumber[] tables = TableNumber.values();
        String[] options = new String[tables.length];
        for (int i = 0; i < tables.length; i++) options[i] = tables[i].getDisplayName();

        String chosen = (String) JOptionPane.showInputDialog(
            this, "請選擇桌號：", "選擇桌號",
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (chosen == null) return;

        for (TableNumber t : tables) {
            if (t.getDisplayName().equals(chosen)) {
                selectedTable = t;
                tableLabel.setText("桌號：" + t.getDisplayName());
                break;
            }
        }
    }

    private void selectMember() {
        String[] options = { "輸入電話查詢", "當場註冊", "不使用會員" };
        int choice = JOptionPane.showOptionDialog(this, "請選擇會員方式：", "會員",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
            null, options, options[0]);

        switch (choice) {
            case 0 -> lookupMember();
            case 1 -> registerMember();
            default -> {
                selectedMember = null;
                memberLabel.setText("會員：非會員");
            }
        }
    }

    private void lookupMember() {
        String phone = JOptionPane.showInputDialog(this,
            "請輸入電話：", "查詢會員", JOptionPane.PLAIN_MESSAGE);
        if (phone == null || phone.trim().isEmpty()) return;

        Member m = memberService.findMemberByPhone(phone.trim());
        if (m == null) {
            JOptionPane.showMessageDialog(this, "查無此會員", "查詢結果", JOptionPane.WARNING_MESSAGE);
        } else {
            selectedMember = m;
            memberLabel.setText("會員：" + m.getName() + "（" + m.getTier().getDisplayName() + "）");
        }
    }

    private void registerMember() {
        JTextField nameField  = new JTextField();
        JTextField phoneField = new JTextField();
        JSpinner   monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
        JSpinner   dateSpinner  = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("姓名："));  form.add(nameField);
        form.add(new JLabel("電話："));  form.add(phoneField);
        form.add(new JLabel("生日月份：")); form.add(monthSpinner);
        form.add(new JLabel("生日日期：")); form.add(dateSpinner);

        int result = JOptionPane.showConfirmDialog(this, form, "註冊會員",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String name  = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "姓名和電話不能為空！", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Member m = memberService.registerMember(name, phone,
            (int) monthSpinner.getValue(), (int) dateSpinner.getValue());
        if (m != null) {
            selectedMember = m;
            memberLabel.setText("會員：" + m.getName() + "（" + m.getTier().getDisplayName() + "）");
            JOptionPane.showMessageDialog(this, "註冊成功！歡迎 " + m.getName(), "完成",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ══════════════════════════════════════════
    //  結帳
    // ══════════════════════════════════════════

    private void checkout() {
        if (selectedTable == null) {
            JOptionPane.showMessageDialog(this, "請先選擇桌號！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "購物車是空的！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Order order = orderService.createOrder(selectedTable, selectedMember, new ArrayList<>(cart));
        if (order == null) return;

        if (selectedMember != null) {
            memberService.updateTotalSpent(selectedMember, order.getFinalAmount());
        }

        JOptionPane.showMessageDialog(this,
            String.format("訂單 #%d 已送出！\n桌號：%s\n原價：$%d\n實付：$%d",
                order.getOrderId(), selectedTable.getDisplayName(),
                order.getTotalAmount(), order.getFinalAmount()),
            "訂單成功 ✅", JOptionPane.INFORMATION_MESSAGE);

        // 重置狀態
        cart.clear();
        cartModel.setRowCount(0);
        updateTotal();
        selectedTable  = null;
        selectedMember = null;
        tableLabel.setText("桌號：尚未選擇");
        memberLabel.setText("會員：非會員");
    }
}