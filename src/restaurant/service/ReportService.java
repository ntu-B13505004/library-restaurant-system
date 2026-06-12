package service;

import model.Order;
import model.OrderDetail;
import model.MenuItem;
import enums.OrderStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private OrderService orderService;

    public ReportService(OrderService orderService) {
        this.orderService = orderService;
    }

    // 計算指定日期的營業額（只計算已完成的訂單）
    public int calculateRevenue(LocalDate date) {
        int revenue = 0;
        for (Order o : orderService.getAllOrders()) {
            boolean sameDay    = o.getOrderTime().toLocalDate().equals(date);
            boolean isComplete = o.getStatus() == OrderStatus.COMPLETED;
            if (sameDay && isComplete) {
                revenue += o.getFinalAmount();
            }
        }
        System.out.printf("📊 %s 營業額：$%d%n", date, revenue);
        return revenue;
    }

    // 計算今日營業額（快捷方法）
    public int calculateTodayRevenue() {
        return calculateRevenue(LocalDate.now());
    }

    // 熱銷排行（依銷售數量排序）
    public List<MenuItem> showBestSellingItems() {
        // itemId → 銷售數量
        Map<Integer, Integer> salesCount = new HashMap<>();
        // itemId → MenuItem 物件（方便最後取出）
        Map<Integer, MenuItem> itemMap = new HashMap<>();

        for (Order o : orderService.getAllOrders()) {
            if (o.getStatus() != OrderStatus.COMPLETED) continue; // 只算已完成訂單
            for (OrderDetail d : o.getDetails()) {
                int id = d.getItem().getItemId();
                salesCount.put(id, salesCount.getOrDefault(id, 0) + d.getQuantity());
                itemMap.put(id, d.getItem());
            }
        }

        // 依銷售量排序
        List<Map.Entry<Integer, Integer>> sorted = new ArrayList<>(salesCount.entrySet());
        Collections.sort(sorted, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> a, Map.Entry<Integer, Integer> b) {
                return b.getValue() - a.getValue(); // 由高到低
            }
        });

        // 印出排行
        System.out.println("══════ 熱銷排行 ══════");
        List<MenuItem> result = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Integer, Integer> entry : sorted) {
            MenuItem item = itemMap.get(entry.getKey());
            System.out.printf("第%d名｜%s｜銷售：%d 份%n", rank++, item.getItemName(), entry.getValue());
            result.add(item);
        }
        System.out.println("═════════════════════");

        return result;
    }
}