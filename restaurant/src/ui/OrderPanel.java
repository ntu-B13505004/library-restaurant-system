package ui;

import enums.OrderStatus;
import model.Order;
import service.OrderService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class OrderPanel extends JPanel {

    private final OrderService    orderService;
    private       DefaultTableModel tableModel;

    public OrderPanel(OrderService orderService) {
        this.orderService = orderService;
        initializeUI();
        refreshTable();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── 表格 ────────────────────────────
        String[] columns = { "訂單編號", "桌號", "會員", "原價", "實付", "狀態" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── 按鈕區 ──────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton preparingBtn = new JButton("▶ 製作中");
        JButton completedBtn = new JButton("✓ 已完成");
        JButton refreshBtn   = new JButton("↻ 重新整理");

        preparingBtn.addActionListener(e -> updateStatus(table, OrderStatus.PREPARING));
        completedBtn.addActionListener(e -> updateStatus(table, OrderStatus.COMPLETED));
        refreshBtn.addActionListener(e   -> refreshTable());

        btnPanel.add(preparingBtn);
        btnPanel.add(completedBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    private void updateStatus(JTable table, OrderStatus newStatus) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "請先選擇一筆訂單！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int orderId = (int) tableModel.getValueAt(row, 0);
        orderService.updateOrderStatus(orderId, newStatus);
        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Order> orders = orderService.getAllOrders();
        for (Order o : orders) {
            String memberName = o.getMember() != null ? o.getMember().getName() : "非會員";
            tableModel.addRow(new Object[]{
                o.getOrderId(),
                o.getTable().getDisplayName(),
                memberName,
                "$" + o.getTotalAmount(),
                "$" + o.getFinalAmount(),
                o.getStatus().getDisplayName()
            });
        }
    }
}