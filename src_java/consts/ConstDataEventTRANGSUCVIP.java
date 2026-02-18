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
 * Quản lý sự kiện Top Trang Sức VIP (Clean Code Version)
 */
public class ConstDataEventTRANGSUCVIP {

    private static ConstDataEventTRANGSUCVIP instance;

    public static ConstDataEventTRANGSUCVIP gI() {
        if (instance == null) {
            instance = new ConstDataEventTRANGSUCVIP();
        }
        return instance;
    }

    // ================= CONFIGURATION =================
    // Cờ báo hiệu sự kiện đang chạy (Quan trọng cho Thread check)
    public static boolean isRunningSK = false;
    public static boolean isTraoQua = true;
    public static boolean initsukien = false;

    public static short MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN;
    public static short MONTH_END, DATE_END, HOUR_END, MIN_END;

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
        short year = ConstDataEventSM.YEAR_EVENT;

        startEvent = createCalendar(year, MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
        endEvents = createCalendar(year, MONTH_END, DATE_END, HOUR_END, MIN_END);

        System.out.println("VIP Accessories TOP Event started at: " + startEvent.getTime());
        System.out.println("VIP Accessories TOP Event ends at: " + endEvents.getTime());
    }

    private static Calendar createCalendar(int year, int month, int day, int hour, int min) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day, hour, min);
        return cal;
    }

    private static String getStartDateTimeSQL() {
        return String.format("%04d-%02d-%02d %02d:%02d:00",
                ConstDataEventSM.YEAR_EVENT, MONTH_OPEN, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
    }

    // ================= REWARD DISTRIBUTION SYSTEM =================

    private static void traoQuaHangLoat() {
        Logger.log("Bắt đầu trao quà Top Trang Sức VIP...");
        
        // Tải phần thưởng trước để tránh query lặp lại
        List<JSONArray> allRewards = loadAllRewards(); 
        if (allRewards.isEmpty()) {
            Logger.error("Không tìm thấy dữ liệu phần thưởng Top Trang Sức VIP!");
            return;
        }

        List<Integer> topPlayerIds = getTopCapsuleVipPlayerIds();

        for (int i = 0; i < topPlayerIds.size(); i++) {
            // Kiểm tra nếu rank vượt quá số lượng phần thưởng
            if (i >= allRewards.size()) break; 

            int playerId = topPlayerIds.get(i);
            Player player = NDVSqlFetcher.loadPlayerByID(playerId);

            if (player != null) {
                // Trao quà dựa trên rank (i)
                traoQuaSuKien(player, allRewards.get(i));
                Logger.log("Đã trao quà Top Trang Sức VIP hạng " + (i + 1) + " cho: " + player.name);
                Functions.sleep(100); // Delay nhẹ tránh quá tải
            } else {
                Logger.error("Không thể tải thông tin người chơi ID: " + playerId);
            }
        }
        Logger.log("Hoàn tất trao quà Top Trang Sức VIP.");
    }

    private static List<Integer> getTopCapsuleVipPlayerIds() {
        List<Integer> ids = new ArrayList<>();
        // Query tối ưu lấy Top dựa trên 'capsuvip'
        String sql = "SELECT player.id FROM account " +
                     "JOIN player ON account.id = player.account_id " +
                     "WHERE account.create_time > ? AND account.capsuvip >= 100000 " +
                     "ORDER BY account.capsuvip DESC LIMIT 10";

        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, getStartDateTimeSQL());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            Logger.error("Lỗi lấy danh sách Top Trang Sức VIP: " + e.getMessage());
        }
        return ids;
    }

    private static List<JSONArray> loadAllRewards() {
        List<JSONArray> rewards = new ArrayList<>();
        // Load toàn bộ bảng thưởng (giả sử ID tương ứng với Rank 1 -> 10)
        String sql = "SELECT detail FROM moc_capsule_trang_suc ORDER BY id ASC";

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
            Logger.error("Lỗi load phần thưởng Top Trang Sức VIP: " + e.getMessage());
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
                Service.gI().sendThongBao(pl, "Bạn vừa nhận thưởng Top Trang Sức VIP về hòm thư!");
            } else {
                Logger.error("Lỗi lưu quà Top Trang Sức VIP vào hòm thư cho: " + pl.name);
            }

        } catch (Exception e) {
            Logger.error("Lỗi xử lý quà Top Trang Sức VIP cho " + pl.name + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}