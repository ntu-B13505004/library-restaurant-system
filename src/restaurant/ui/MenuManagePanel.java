package ui;

import service.MenuService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MenuManagePanel extends JPanel {

    private final MenuService menuService;

    public MenuManagePanel(MenuService menuService) {

        this.menuService = menuService;

        initializeUI();
    }

    private void initializeUI() {

        setLayout(new BorderLayout());

        String[] columns = {
                "ID",
                "名稱",
                "價格",
                "分類",
                "售完"
        };

        JTable table = new JTable(
                new DefaultTableModel(columns,0)
        );

        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(new JButton("新增"));
        buttonPanel.add(new JButton("改價"));
        buttonPanel.add(new JButton("售完"));
        buttonPanel.add(new JButton("刪除"));

        add(buttonPanel, BorderLayout.SOUTH);
    }
}