package consts;

import item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import jdbc.DBConnecter;
import jdbc.daos.NDVSqlFetcher;
import nro.player.Player;
import nro.services.ItemService;
import nro.services.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import utils.Functions;
import utils.Logger;

/**
 * Quản lý sự kiện Top Sức Mạnh (Clean Code - Fixed)
 */
public class ConstDataEventSM {

    private static ConstDataEventSM instance;

    public static ConstDataEventSM gI() {
        if (instance == null) {
            instance = new ConstDataEventSM();
        }
        return instance;
    }

    // ================= CONFIGURATION =================
    
    // --- [FIX] Thêm lại biến này để luồng server gọi không bị lỗi ---
    public static boolean isRunningSK = false; 
    // ---------------------------------------------------------------

    public static boolean isTraoQua = true;
    public static boolean initsukien = false;
    
    public static short MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN;
    public static short MONTH_END, DATE_END, HOUR_END, MIN_END;
    public static short YEAR_EVENT;

    private static Calendar startEvent;
    private static Calendar endEvents;

    // ================= EVENT LOGIC =================

    public static boolean isActiveEvent() {
        if (!initsukien) {
            initEventTime();
        }

        long currentMillis = System.currentTimeMillis();
        long startMillis = startEvent.getTimeInMillis();
        long endMillis = endEvents.getTimeInMillis();

        if (currentMillis >= startMillis && currentMillis <= endMillis) {
            // Kiểm tra trao quà khi sự kiện sắp kết thúc (còn 60s)
            if (isTraoQua && currentMillis + 60000 >= endMillis) {
                traoQuaHangLoat();
                isTraoQua = false;
            }
            return true;
        }
        return false;
    }

    private static void initEventTime() {
        initsukien = true;
        startEvent = createCalendar(YEAR_EVENT, MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
        endEvents = createCalendar(YEAR_EVENT, MONTH_END, DATE_END, HOUR_END, MIN_END);
        
        System.out.println("Star Event TOP POWER: " + startEvent.getTime());
        System.out.println("End Event TOP POWER: " + endEvents.getTime());
    }

    private static Calendar createCalendar(int year, int month, int day, int hour, int min) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, hour, min);
        return cal;
    }

    public static String getStartDateTimeSQL() {
        return String.format("%04d-%02d-%02d %02d:%02d:00",
                YEAR_EVENT, MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
    }

    // ================= REWARD DISTRIBUTION SYSTEM =================

    private static void traoQuaHangLoat() {
        Logger.log("Bắt đầu trao quà Top Sức Mạnh...");
        List<JSONArray> allRewards = loadAllRewards();
        
        if (allRewards.isEmpty()) {
            Logger.error("Không tìm thấy dữ liệu phần thưởng trong database!");
            return;
        }

        List<Integer> topPlayerIds = getTopPowerPlayerIds();
        
        for (int i = 0; i < topPlayerIds.size(); i++) {
            if (i >= allRewards.size()) break; // Hết quà

            int playerId = topPlayerIds.get(i);
            Player player = NDVSqlFetcher.loadPlayerByID(playerId);

            if (player != null) {
                traoQuaSuKien(player, allRewards.get(i));
                Logger.log("Đã trao quà Top " + (i + 1) + " cho: " + player.name);
                Functions.sleep(100); // Delay nhẹ tránh quá tải
            } else {
                Logger.error("Không thể tải thông tin người chơi ID: " + playerId);
            }
        }
        Logger.log("Hoàn tất trao quà Top Sức Mạnh.");
    }

    private static List<Integer> getTopPowerPlayerIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM player WHERE create_time > ? " +
                     "ORDER BY CAST(split_str(data_point,',',2) AS UNSIGNED) DESC LIMIT 10";

        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, getStartDateTimeSQL());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            Logger.error("Lỗi lấy danh sách Top SM: " + e.getMessage());
        }
        return ids;
    }

    private static List<JSONArray> loadAllRewards() {
        List<JSONArray> rewards = new ArrayList<>();
        String sql = "SELECT detail FROM moc_suc_manh_top ORDER BY id ASC";

        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object json = JSONValue.parse(rs.getString("detail"));
                if (json instanceof JSONArray) {
                    rewards.add((JSONArray) json);
                }
            }
        } catch (SQLException e) {
            Logger.error("Lỗi load phần thưởng Top SM: " + e.getMessage());
        }
        return rewards;
    }

    public static void traoQuaSuKien(Player pl, JSONArray rewardDetail) {
        if (pl == null || rewardDetail == null) return;

        try {
            for (Object obj : rewardDetail) {
                JSONObject itemData = (JSONObject) obj;
                int tempId = Integer.parseInt(itemData.get("temp_id").toString());
                int quantity = Integer.parseInt(itemData.get("quantity").toString());

                Item item = ItemService.gI().createNewItem((short) tempId);
                item.quantity = quantity;

                JSONArray options = (JSONArray) itemData.get("options");
                if (options != null) {
                    for (Object opt : options) {
                        JSONObject optData = (JSONObject) opt;
                        int optId = Integer.parseInt(optData.get("id").toString());
                        int param = Integer.parseInt(optData.get("param").toString());
                        item.itemOptions.add(new Item.ItemOption(optId, param));
                    }
                }
                pl.inventory.itemsMailBox.add(item);
            }

            if (NDVSqlFetcher.updateMailBox(pl)) {
                Service.gI().sendThongBao(pl, "Bạn vừa nhận thưởng Top Sức Mạnh về hòm thư!");
            } else {
                Logger.error("Lỗi lưu quà vào hòm thư cho: " + pl.name);
            }

        } catch (Exception e) {
            Logger.error("Lỗi xử lý quà cho " + pl.name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}