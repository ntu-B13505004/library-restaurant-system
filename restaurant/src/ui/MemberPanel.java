package ui;

import model.Member;
import service.MemberService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MemberPanel extends JPanel {

    private final MemberService   memberService;
    private       DefaultTableModel tableModel;

    public MemberPanel(MemberService memberService) {
        this.memberService = memberService;
        initializeUI();
        refreshTable();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── 表格 ────────────────────────────
        String[] columns = { "ID", "姓名", "電話", "生日月/日", "等級", "累積消費" };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // ── 按鈕區 ──────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JButton refreshBtn = new JButton("↻ 重新整理");
        refreshBtn.addActionListener(e -> refreshTable());
        btnPanel.add(refreshBtn);

        add(btnPanel, BorderLayout.SOUTH);
    }

    public void refreshTable() {
        tableModel.setRowCount(0);
        List<Member> members = memberService.getAllMembers();
        for (Member m : members) {
            tableModel.addRow(new Object[]{
                m.getMemberId(),
                m.getName(),
                m.getPhone(),
                m.getBirthMonth() + "/" + m.getBirthDate(),
                m.getTier().getDisplayName(),
                "$" + m.getTotalSpent()
            });
        }
    }
}