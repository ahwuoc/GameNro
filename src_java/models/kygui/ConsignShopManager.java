package models.kygui;

import item.Item.ItemOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import jdbc.DBConnecter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ConsignShopManager {

    private static ConsignShopManager instance;

    public static ConsignShopManager gI() {
        if (instance == null) {
            instance = new ConsignShopManager();
        }
        return instance;
    }

    public long lastTimeUpdate;

    public String[] tabName = {"Áo Quần", "Găng Tay", "Phụ Kiện", "Linh tinh", ""};

    public List<ConsignItem> listItem = new ArrayList<>();

    public ConsignShopManager() {
        this.load(); // Load data when instance is created
    }

    public void load() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            Statement s = con.createStatement();
            ResultSet rs = s.executeQuery("SELECT * FROM `shop_ky_gui`");
            while (rs.next()) {
                try {
                    int id = rs.getInt("id");
                    int player_id = rs.getInt("player_id");
                    byte tab = rs.getByte("tab");
                    short item_id = rs.getShort("item_id");
                    int gold = rs.getInt("gold");
                    int gem = rs.getInt("gem");
                    int quantity = rs.getInt("quantity");
                    String optionsStr = rs.getString("itemOption");
                    byte isUpTop = rs.getByte("isUpTop");
                    boolean isBuy = rs.getBoolean("isBuy");
                    
                    List<ItemOption> itemOptions = new ArrayList<>();
                    
                    // Safely parse JSON options
                    Object parsedOptions = JSONValue.parse(optionsStr);
                    if (parsedOptions != null) {
                        if (parsedOptions instanceof JSONArray) {
                            JSONArray jOptions = (JSONArray) parsedOptions;
                            for (int i = 0; i < jOptions.size(); i++) {
                                Object optObj = jOptions.get(i);
                                if (optObj instanceof JSONArray) {
                                    JSONArray option = (JSONArray) optObj;
                                    if (option.size() >= 2) {
                                        int optionId = Integer.parseInt(option.get(0).toString());
                                        int param = Integer.parseInt(option.get(1).toString());
                                        itemOptions.add(new ItemOption(optionId, param));
                                    }
                                } else if (optObj instanceof JSONObject) {
                                    JSONObject option = (JSONObject) optObj;
                                    if (option.containsKey("id") && option.containsKey("param")) {
                                        int optionId = Integer.parseInt(option.get("id").toString());
                                        int param = Integer.parseInt(option.get("param").toString());
                                        itemOptions.add(new ItemOption(optionId, param));
                                    }
                                }
                            }
                        } else if (parsedOptions instanceof JSONObject) {
                            // If top level is a JSONObject, try to extract options from it
                            JSONObject jsonObj = (JSONObject) parsedOptions;
                            if (jsonObj.containsKey("options") && jsonObj.get("options") instanceof JSONArray) {
                                JSONArray optionsArray = (JSONArray) jsonObj.get("options");
                                for (int i = 0; i < optionsArray.size(); i++) {
                                    Object optObj = optionsArray.get(i);
                                    if (optObj instanceof JSONArray) {
                                        JSONArray option = (JSONArray) optObj;
                                        if (option.size() >= 2) {
                                            int optionId = Integer.parseInt(option.get(0).toString());
                                            int param = Integer.parseInt(option.get(1).toString());
                                            itemOptions.add(new ItemOption(optionId, param));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    ConsignItem item = new ConsignItem(id, item_id, player_id, tab, gold, gem, quantity, isUpTop, itemOptions, isBuy);
                    listItem.add(item);
                } catch (Exception e) {
//                     System.out.println("Error parsing item row: " + e.getMessage());
                    // Continue to the next item if one fails
                }
            }
            rs.close();
//             System.out.println("Loaded " + listItem.size() + " consign items!");
        } catch (Exception e) {
//             System.out.println("Error loading consign shop: " + e.getMessage());
//             e.printStackTrace();
        }
    }

    public void save() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            Statement s = con.createStatement();
            s.execute("TRUNCATE shop_ky_gui");
            int savedCount = 0;
            for (ConsignItem it : this.listItem) {
                if (it != null) {
                    try {
                        // Format the JSON options properly to ensure consistent format
                        String optionsJson = formatOptionsJson(it.options);
                        
                        String sql = String.format("INSERT INTO `shop_ky_gui`(`id`, `player_id`, `tab`, `item_id`,`gold`, `gem`, `quantity`, `itemOption`, `isUpTop`, `isBuy`) VALUES ('%s','%s','%s','%s','%s','%s','%s','%s','%s','%s')",
                                it.id, it.player_sell, it.tab, it.itemId, it.goldSell, it.gemSell, it.quantity, optionsJson, it.isUpTop, it.isBuy ? 1 : 0);
                        s.execute(sql);
                        savedCount++;
                    } catch (Exception e) {
//                         System.out.println("Error saving item id=" + it.id + ": " + e.getMessage());
                    }
                }
            }
//             System.out.println("Saved " + savedCount + " consign items!");
        } catch (Exception e) {
//             System.out.println("Error saving consign shop: " + e.getMessage());
//             e.printStackTrace();
        }
    }
    
    private String formatOptionsJson(List<ItemOption> options) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < options.size(); i++) {
            ItemOption opt = options.get(i);
            sb.append("[").append(opt.optionTemplate.id).append(",").append(opt.param).append("]");
            if (i < options.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
