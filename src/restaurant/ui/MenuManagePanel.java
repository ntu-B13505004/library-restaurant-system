package ui;

import model.MenuItem;
import service.MenuService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MenuManagePanel extends JPanel {

    private final MenuService     menuService;
    private       DefaultTableModel tableModel;

    public MenuManagePanel(MenuService menuService) {
        this.menuService = menuService;
        initializeUI();
        refreshTable();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── 表格 ────────────────────────────
        String[] columns = { "ID", "名稱", "價格", "分類", "售完" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── 按鈕區 ──────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton addBtn      = new JButton("＋ 新增");
        JButton priceBtn    = new JButton("✎ 改價");
        JButton soldOutBtn  = new JButton("✕ 售完 / 供應");
        JButton deleteBtn   = new JButton("🗑 刪除");
        JButton refreshBtn  = new JButton("↻ 重新整理");

        addBtn.addActionListener(e     -> addItem());
        priceBtn.addActionListener(e   -> updatePrice(table));
        soldOutBtn.addActionListener(e -> toggleSoldOut(table));
        deleteBtn.addActionListener(e  -> deleteItem(table));
        refreshBtn.addActionListener(e -> refreshTable());

        btnPanel.add(addBtn);
        btnPanel.add(priceBtn);
        btnPanel.add(soldOutBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);
    }

    // ── 新增餐點 ────────────────────────────
    private void addItem() {
        JTextField nameField  = new JTextField();
        JTextField priceField = new JTextField();
        JTextField catField   = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("名稱：")); form.add(nameField);
        form.add(new JLabel("價格：")); form.add(priceField);
        form.add(new JLabel("分類：")); form.add(catField);

        int result = JOptionPane.showConfirmDialog(this, form, "新增餐點",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        String cat  = catField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "名稱不能為空！", "錯誤", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            int price = Integer.parseInt(priceField.getText().trim());
            menuService.addMenuItem(name, price, cat);
            refreshTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "價格請輸入數字！", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── 改價 ────────────────────────────────
    private void updatePrice(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) { showSelectWarning(); return; }

        int id = (int) tableModel.getValueAt(row, 0);
        String input = JOptionPane.showInputDialog(this, "請輸入新價格：", "改價", JOptionPane.PLAIN_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;

        try {
            int newPrice = Integer.parseInt(input.trim());
            menuService.updatePrice(id, newPrice);
            refreshTable();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "請輸入有效數字！", "錯誤", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── 售完 / 恢復供應 ──────────────────────
    private void toggleSoldOut(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) { showSelectWarning(); return; }

        int id = (int) tableModel.getValueAt(row, 0);
        boolean isSoldOut = tableModel.getValueAt(row, 4).equals("是");
        menuService.setSoldOut(id, !isSoldOut);
        refreshTable();
    }

    // ── 刪除 ────────────────────────────────
    private void deleteItem(JTable table) {
        int row = table.getSelectedRow();
        if (row == -1) { showSelectWarning(); return; }

        int id   = (int) tableModel.getValueAt(row, 0);
        String name = tableModel.getValueAt(row, 1).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
            "確定要刪除「" + name + "」？", "刪除確認", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        menuService.removeMenuItem(id);
        refreshTable();
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<MenuItem> items = menuService.getAllItems();
        for (MenuItem item : items) {
            tableModel.addRow(new Object[]{
                item.getItemId(),
                item.getItemName(),
                "$" + item.getPrice(),
                item.getCategory(),
                item.isSoldOut() ? "是" : "否"
            });
        }
    }

    private void showSelectWarning() {
        JOptionPane.showMessageDialog(this, "請先選擇一筆餐點！", "提示", JOptionPane.WARNING_MESSAGE);
    }
}