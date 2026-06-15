package ui;

import service.MemberService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MemberPanel extends JPanel {

    private final MemberService memberService;

    public MemberPanel(MemberService memberService) {

        this.memberService = memberService;

        initializeUI();
    }

    private void initializeUI() {

        setLayout(new BorderLayout());

        String[] columns = {
                "ID",
                "姓名",
                "電話",
                "等級",
                "累積消費"
        };

        JTable table = new JTable(
                new DefaultTableModel(columns,0)
        );

        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );
    }
}