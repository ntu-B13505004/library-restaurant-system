package service;

import model.Member;
import model.Order;
import model.OrderDetail;
import enums.OrderStatus;
import enums.TableNumber;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderService {
    private List<Order> orders = new ArrayList<>();
    private int nextOrderId   = 1;
    private int nextDetailId  = 1;

    // 建立訂單
    public Order createOrder(TableNumber table, Member member, List<OrderDetail> details) {
        if (details == null || details.isEmpty()) {
            System.out.println("❌ 訂單明細不能為空");
            return null;
        }
        Order order = new Order(nextOrderId++, table, member, details);

        // 計算折扣後金額
        int finalAmount = calculateFinalAmount(order);
        order.setFinalAmount(finalAmount);

        orders.add(order);
        System.out.println("✅ 訂單建立成功！");
        System.out.println(order);
        return order;
    }

    // 建立單筆 OrderDetail（給前端點餐用）
    public OrderDetail createDetail(model.MenuItem item, int quantity, String note) {
        return new OrderDetail(nextDetailId++, item, quantity, note);
    }

    // 計算折扣後實付金額
    public int calculateFinalAmount(Order order) {
        int total = order.getTotalAmount();
        Member member = order.getMember();

        // 非會員：不打折
        if (member == null) return total;

        // 取得當前月份，判斷是否為生日月
        int currentMonth = LocalDate.now().getMonthValue();
        boolean isBirthday = member.isBirthdayMonth(currentMonth);

        // 從 MemberTier 取得最終折扣率
        double discount = member.getTier().getFinalDiscount(isBirthday);

        int finalAmount = (int) Math.round(total * discount);

        // 印出折扣資訊
        if (isBirthday && member.getTier() != enums.MemberTier.BRONZE) {
            System.out.printf("🎂 生日當月優惠！折扣率：%.4f（原價 $%d → 實付 $%d）%n",
                    discount, total, finalAmount);
        } else {
            System.out.printf("💳 會員折扣（%s）：%.2f折（原價 $%d → 實付 $%d）%n",
                    member.getTier().getDisplayName(), discount * 10, total, finalAmount);
        }

        return finalAmount;
    }

    // 更新訂單狀態（PENDING → PREPARING → COMPLETED）
    public boolean updateOrderStatus(int orderId, OrderStatus newStatus) {
        Order order = findById(orderId);
        if (order == null) {
            System.out.println("❌ 找不到訂單 ID：" + orderId);
            return false;
        }
        OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);
        System.out.println("✅ 訂單 #" + orderId + " 狀態更新：" +
                oldStatus.getDisplayName() + " → " + newStatus.getDisplayName());
        return true;
    }

    // 取得所有訂單
    public List<Order> getAllOrders() {
        return orders;
    }

    // 取得指定狀態的訂單
    public List<Order> getOrdersByStatus(OrderStatus status) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders) {
            if (o.getStatus() == status) {
                result.add(o);
            }
        }
        return result;
    }

    // 內部用：用 ID 找訂單
    public Order findById(int orderId) {
        for (Order o : orders) {
            if (o.getOrderId() == orderId) {
                return o;
            }
        }
        return null;
    }

    // 印出所有訂單
    public void printAllOrders() {
        if (orders.isEmpty()) {
            System.out.println("（目前沒有訂單）");
            return;
        }
        for (Order o : orders) {
            System.out.println(o);
        }
    }
}