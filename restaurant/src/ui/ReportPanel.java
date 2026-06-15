package ui;

import model.MenuItem;
import service.ReportService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ReportPanel extends JPanel {

    private final ReportService reportService;
    private       JLabel        revenueLabel;
    private       JTextArea     bestSellerArea;

    public ReportPanel(ReportService reportService) {
        this.reportService = reportService;
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ── 頂部：營業額 ────────────────────
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));

        revenueLabel = new JLabel("今日營業額：---");
        revenueLabel.setFont(revenueLabel.getFont().deriveFont(Font.BOLD, 15f));

        JButton refreshBtn = new JButton("↻ 重新整理");
        refreshBtn.addActionListener(e -> refreshReport());

        topPanel.add(revenueLabel);
        topPanel.add(refreshBtn);
        add(topPanel, BorderLayout.NORTH);

        // ── 中央：熱銷排行 ──────────────────
        bestSellerArea = new JTextArea();
        bestSellerArea.setEditable(false);
        bestSellerArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        bestSellerArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(new JScrollPane(bestSellerArea), BorderLayout.CENTER);
    }

    public void refreshReport() {
        // 營業額
        int revenue = reportService.calculateTodayRevenue();
        revenueLabel.setText("今日營業額：$" + revenue);

        // 熱銷排行
        List<MenuItem> best = reportService.showBestSellingItems();
        StringBuilder sb = new StringBuilder("══ 熱銷排行 ══\n\n");
        if (best.isEmpty()) {
            sb.append("（目前還沒有已完成的訂單）");
        } else {
            for (int i = 0; i < best.size(); i++) {
                sb.append(String.format("第%d名｜%s%n", i + 1, best.get(i).getItemName()));
            }
        }
        bestSellerArea.setText(sb.toString());
    }
}