package persistence;

import model.MenuItem;
import service.MenuService;

import java.io.*;
import java.util.List;

public class CsvMenuRepository {

    private static final String FILE_NAME = "menu.csv";

    // 欄位順序：itemId, itemName, price, category, isSoldOut, isDeleted
    public void save(List<MenuItem> items) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (MenuItem item : items) {
                bw.write(
                    item.getItemId()   + "," +
                    item.getItemName() + "," +
                    item.getPrice()    + "," +
                    item.getCategory() + "," +
                    item.isSoldOut()   + "," +
                    item.isDeleted()
                );
                bw.newLine();
            }
            System.out.println("✅ 菜單資料已儲存");
        } catch (Exception e) {
            System.out.println("❌ 菜單資料儲存失敗：" + e.getMessage());
        }
    }

    // 直接還原 MenuItem，不走 addMenuItem()，避免 id 重設
    public void load(MenuService menuService) {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println("（menu.csv 不存在，略過載入）");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int maxId = 0;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] data = line.split(",");

                int     itemId    = Integer.parseInt(data[0]);
                String  itemName  = data[1];
                int     price     = Integer.parseInt(data[2]);
                String  category  = data[3];
                boolean isSoldOut = Boolean.parseBoolean(data[4]);
                boolean isDeleted = Boolean.parseBoolean(data[5]);

                // 直接建物件並還原所有欄位
                MenuItem item = new MenuItem(itemId, itemName, price, category);
                item.setSoldOut(isSoldOut);
                item.setDeleted(isDeleted);

                menuService.loadMenuItem(item);     // 見下方說明
                if (itemId > maxId) maxId = itemId;
            }

            menuService.setNextItemId(maxId + 1);   // 確保下一個 id 不重複
            System.out.println("✅ 菜單資料載入完成");

        } catch (Exception e) {
            System.out.println("❌ 菜單資料載入失敗：" + e.getMessage());
        }
    }
}