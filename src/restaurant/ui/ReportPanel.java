package ui;

import service.ReportService;

import javax.swing.*;
import java.awt.*;

public class ReportPanel extends JPanel {

    private final ReportService reportService;

    public ReportPanel(ReportService reportService) {

        this.reportService = reportService;

        initializeUI();
    }

    private void initializeUI() {

        setLayout(new BorderLayout());

        JLabel revenueLabel =
                new JLabel("今日營業額：0");

        JTextArea bestSellerArea =
                new JTextArea();

        bestSellerArea.setEditable(false);

        add(
                revenueLabel,
                BorderLayout.NORTH
        );

        add(
                new JScrollPane(bestSellerArea),
                BorderLayout.CENTER
        );
    }
}