package persistence;

import service.MenuService;

import java.io.BufferedReader;
import java.io.FileReader;

public class CsvMenuRepository {

    private static final String FILE_NAME = "menu.csv";

    public void load(MenuService menuService) {

        try ( BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))){
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                String itemName = data[0];
                int price = Integer.parseInt(data[1]);
                String category = data[2];

                menuService.addMenuItem( itemName, price, category );
            }
        } catch (Exception e) {
            System.out.println("menu.csv不存在或讀取失敗");
        }
    }
}