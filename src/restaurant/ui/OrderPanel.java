package ui;

import service.OrderService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class OrderPanel extends JPanel {

    private final OrderService orderService;

    private JTable table;

    public OrderPanel(OrderService orderService) {

        this.orderService = orderService;

        initializeUI();
    }

    private void initializeUI() {

        setLayout(new BorderLayout());

        String[] columns = {
                "訂單編號",
                "桌號",
                "會員",
                "金額",
                "狀態"
        };

        table = new JTable(
                new DefaultTableModel(columns,0)
        );

        add(
                new JScrollPane(table),
                BorderLayout.CENTER
        );

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(
                new JButton("製作中")
        );

        buttonPanel.add(
                new JButton("已完成")
        );

        add(buttonPanel, BorderLayout.SOUTH);
    }
}