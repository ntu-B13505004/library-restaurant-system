import enums.OrderStatus;
import enums.TableNumber;
import model.Member;
import model.MenuItem;
import model.Order;
import model.OrderDetail;
import service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    static Scanner        scanner       = new Scanner(System.in);
    static MenuService    menuService   = new MenuService();
    static MemberService  memberService = new MemberService();
    static OrderService   orderService  = new OrderService();
    static ReportService  reportService = new ReportService(orderService);

    public static void main(String[] args) {
        // 預載一些測試資料
        seedData();

        while (true) {
            System.out.println("\n╔══════════════════════╗");
            System.out.println("║   🍜 餐廳點餐系統    ║");
            System.out.println("╠══════════════════════╣");
            System.out.println("║  1. 客人點餐         ║");
            System.out.println("║  2. 後台管理         ║");
            System.out.println("║  0. 離開             ║");
            System.out.println("╚══════════════════════╝");
            System.out.print("請選擇：");

            int choice = readInt();
            switch (choice) {
                case 1: customerMenu(); break;
                case 2: adminMenu();    break;
                case 0:
                    System.out.println("掰掰！");
                    return;
                default:
                    System.out.println("❌ 請輸入有效選項");
            }
        }
    }

    // ════════════════════════════════
    //  客人點餐流程
    // ════════════════════════════════

    static void customerMenu() {
        // Step 1：選桌號
        TableNumber table = selectTable();
        if (table == null) return;

        // Step 2：點餐
        List<OrderDetail> cart = new ArrayList<>();
        while (true) {
            System.out.println("\n══════ 點餐選單 ══════");
            System.out.println("1. 查看菜單");
            System.out.println("2. 加入餐點");
            System.out.println("3. 查看購物車");
            System.out.println("4. 結帳送出");
            System.out.println("0. 取消返回");
            System.out.print("請選擇：");

            int choice = readInt();
            switch (choice) {
                case 1:
                    menuService.printMenu();
                    break;
                case 2:
                    addToCart(cart);
                    break;
                case 3:
                    printCart(cart);
                    break;
                case 4:
                    if (cart.isEmpty()) {
                        System.out.println("❌ 購物車是空的！");
                    } else {
                        checkout(table, cart);
                        return;
                    }
                    break;
                case 0:
                    return;
                default:
                    System.out.println("❌ 請輸入有效選項");
            }
        }
    }

    // 選桌號
    static TableNumber selectTable() {
        System.out.println("\n══════ 選擇桌號 ══════");
        TableNumber[] tables = TableNumber.values();
        for (int i = 0; i < tables.length; i++) {
            System.out.printf("%2d. %s%n", i + 1, tables[i].getDisplayName());
        }
        System.out.println(" 0. 返回");
        System.out.print("請選擇：");

        int choice = readInt();
        if (choice == 0) return null;
        if (choice < 1 || choice > tables.length) {
            System.out.println("❌ 無效桌號");
            return null;
        }
        TableNumber selected = tables[choice - 1];
        System.out.println("✅ 已選擇：" + selected.getDisplayName());
        return selected;
    }

    // 加入餐點到購物車
    static void addToCart(List<OrderDetail> cart) {
        List<MenuItem> available = menuService.getAvailableItems();
        if (available.isEmpty()) {
            System.out.println("❌ 目前沒有可供應的餐點");
            return;
        }

        System.out.println("\n══════ 選擇餐點 ══════");
        for (int i = 0; i < available.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, available.get(i));
        }
        System.out.println(" 0. 返回");
        System.out.print("請選擇餐點：");

        int choice = readInt();
        if (choice == 0) return;
        if (choice < 1 || choice > available.size()) {
            System.out.println("❌ 無效選項");
            return;
        }

        MenuItem item = available.get(choice - 1);

        System.out.print("數量：");
        int qty = readInt();
        if (qty <= 0) {
            System.out.println("❌ 數量必須大於 0");
            return;
        }

        System.out.print("備註（無請直接按 Enter）：");
        scanner.nextLine(); // 清掉換行符
        String note = scanner.nextLine().trim();

        OrderDetail detail = orderService.createDetail(item, qty, note);
        cart.add(detail);
        System.out.printf("✅ 已加入：%s x%d%n", item.getItemName(), qty);
    }

    // 印出購物車
    static void printCart(List<OrderDetail> cart) {
        if (cart.isEmpty()) {
            System.out.println("（購物車是空的）");
            return;
        }
        System.out.println("\n══════ 購物車 ══════");
        int total = 0;
        for (OrderDetail d : cart) {
            System.out.println(d);
            total += d.getSubtotal();
        }
        System.out.println("原價合計：$" + total);
        System.out.println("════════════════════");
    }

    // 結帳
    static void checkout(TableNumber table, List<OrderDetail> cart) {
        // 查詢或註冊會員
        Member member = selectOrRegisterMember();

        // 建立訂單（自動計算折扣）
        Order order = orderService.createOrder(table, member, cart);

        // 如果有會員，更新累積消費並觸發升級判斷
        if (member != null && order != null) {
            memberService.updateTotalSpent(member, order.getFinalAmount());
        }

        System.out.println("✅ 訂單已送出，請稍候！");
    }

    // 選擇是否使用會員（可當場註冊）
    static Member selectOrRegisterMember() {
        System.out.println("\n══════ 會員 ══════");
        System.out.println("1. 輸入會員電話");
        System.out.println("2. 當場註冊會員");
        System.out.println("3. 不使用會員");
        System.out.print("請選擇：");

        int choice = readInt();
        scanner.nextLine(); // 清掉換行符

        switch (choice) {
            case 1: {
                System.out.print("請輸入電話：");
                String phone = scanner.nextLine().trim();
                Member m = memberService.findMemberByPhone(phone);
                if (m == null) {
                    System.out.println("❌ 查無此會員，以非會員結帳");
                    return null;
                }
                System.out.println("✅ 找到會員：" + m.getName() + "（" + m.getTier().getDisplayName() + "）");
                return m;
            }
            case 2: {
                System.out.print("姓名：");
                String name = scanner.nextLine().trim();
                System.out.print("電話：");
                String phone = scanner.nextLine().trim();
                System.out.print("生日月份（1～12）：");
                int month = readInt();
                System.out.print("生日日期（1～31）：");
                int date = readInt();
                scanner.nextLine();
                Member m = memberService.registerMember(name, phone, month, date);
                return m; // 註冊失敗會回傳 null，自動以非會員結帳
            }
            case 3:
            default:
                return null;
        }
    }

    // ════════════════════════════════
    //  後台管理
    // ════════════════════════════════

    static void adminMenu() {
        while (true) {
            System.out.println("\n╔══════════════════════╗");
            System.out.println("║      後台管理        ║");
            System.out.println("╠══════════════════════╣");
            System.out.println("║  1. 查看／更新訂單   ║");
            System.out.println("║  2. 菜單管理         ║");
            System.out.println("║  3. 查看會員         ║");
            System.out.println("║  4. 今日報表         ║");
            System.out.println("║  0. 返回主選單       ║");
            System.out.println("╚══════════════════════╝");
            System.out.print("請選擇：");

            int choice = readInt();
            switch (choice) {
                case 1: orderManageMenu(); break;
                case 2: menuManageMenu();  break;
                case 3: memberManageMenu(); break;
                case 4: reportMenu();      break;
                case 0: return;
                default: System.out.println("❌ 請輸入有效選項");
            }
        }
    }

    // 訂單管理
    static void orderManageMenu() {
        while (true) {
            System.out.println("\n══════ 訂單管理 ══════");
            System.out.println("1. 查看所有訂單");
            System.out.println("2. 查看未接單");
            System.out.println("3. 查看製作中");
            System.out.println("4. 更新訂單狀態");
            System.out.println("0. 返回");
            System.out.print("請選擇：");

            int choice = readInt();
            switch (choice) {
                case 1:
                    orderService.printAllOrders();
                    break;
                case 2:
                    printOrderList(orderService.getOrdersByStatus(OrderStatus.PENDING));
                    break;
                case 3:
                    printOrderList(orderService.getOrdersByStatus(OrderStatus.PREPARING));
                    break;
                case 4:
                    updateOrderStatus();
                    break;
                case 0:
                    return;
                default:
                    System.out.println("❌ 請輸入有效選項");
            }
        }
    }

    static void printOrderList(List<Order> orders) {
        if (orders.isEmpty()) {
            System.out.println("（沒有符合的訂單）");
            return;
        }
        for (Order o : orders) System.out.println(o);
    }

    static void updateOrderStatus() {
        System.out.print("請輸入訂單 ID：");
        int id = readInt();
        System.out.println("更新為：");
        System.out.println("1. 製作中");
        System.out.println("2. 已完成");
        System.out.print("請選擇：");
        int choice = readInt();
        switch (choice) {
            case 1: orderService.updateOrderStatus(id, OrderStatus.PREPARING); break;
            case 2: orderService.updateOrderStatus(id, OrderStatus.COMPLETED); break;
            default: System.out.println("❌ 無效選項");
        }
    }

    // 菜單管理
    static void menuManageMenu() {
        while (true) {
            System.out.println("\n══════ 菜單管理 ══════");
            System.out.println("1. 查看菜單");
            System.out.println("2. 新增餐點");
            System.out.println("3. 刪除餐點");
            System.out.println("4. 修改價格");
            System.out.println("5. 設定售完／供應");
            System.out.println("0. 返回");
            System.out.print("請選擇：");

            int choice = readInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    menuService.printMenu();
                    break;
                case 2: {
                    System.out.print("餐點名稱：");
                    String name = scanner.nextLine().trim();
                    System.out.print("價格：");
                    int price = readInt();
                    scanner.nextLine();
                    System.out.print("分類：");
                    String cat = scanner.nextLine().trim();
                    menuService.addMenuItem(name, price, cat);
                    break;
                }
                case 3: {
                    System.out.print("請輸入餐點 ID：");
                    int id = readInt();
                    menuService.removeMenuItem(id);
                    break;
                }
                case 4: {
                    System.out.print("請輸入餐點 ID：");
                    int id = readInt();
                    System.out.print("新價格：");
                    int price = readInt();
                    menuService.updatePrice(id, price);
                    break;
                }
                case 5: {
                    System.out.print("請輸入餐點 ID：");
                    int id = readInt();
                    System.out.println("1. 設定售完");
                    System.out.println("2. 恢復供應");
                    System.out.print("請選擇：");
                    int s = readInt();
                    if (s == 1) menuService.setSoldOut(id, true);
                    else if (s == 2) menuService.setSoldOut(id, false);
                    break;
                }
                case 0:
                    return;
                default:
                    System.out.println("❌ 請輸入有效選項");
            }
        }
    }

    // 會員管理
    static void memberManageMenu() {
        System.out.println("\n══════ 會員列表 ══════");
        memberService.printAllMembers();
    }

    // 報表
    static void reportMenu() {
        System.out.println("\n══════ 今日報表 ══════");
        reportService.calculateTodayRevenue();
        reportService.showBestSellingItems();
    }

    // ════════════════════════════════
    //  工具方法
    // ════════════════════════════════

    // 安全讀取整數，輸入非數字時回傳 -1
    static int readInt() {
        try {
            int val = Integer.parseInt(scanner.nextLine().trim());
            return val;
        } catch (Exception e) {
            return -1;
        }
    }

    // 預載測試資料
    static void seedData() {
        // 菜單
        menuService.addMenuItem("牛肉麵",   120, "主餐");
        menuService.addMenuItem("雞腿飯",   100, "主餐");
        menuService.addMenuItem("滷肉飯",    70, "主餐");
        menuService.addMenuItem("珍珠奶茶",  60, "飲料");
        menuService.addMenuItem("紅茶",      40, "飲料");
        menuService.addMenuItem("豆花",      50, "甜點");

        // 會員
        memberService.registerMember("小明", "0911111111", 6, 20);
        memberService.registerMember("小美", "0922222222", 3, 20);
    }
}