package player;

import item.Item;
import item.Item.ItemOption;
import jdbc.DBConnecter;
import jdbc.daos.NDVSqlFetcher;
import network.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import player.Player;
import services.ItemService;
import services.Service;
import utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Achievement_TieuTien {

    private static Achievement_TieuTien instance;

    public static Achievement_TieuTien gI() {
        if (instance == null) {
            instance = new Achievement_TieuTien();
        }
        return instance;
    }

    private static final Map<Integer, TieuTienMilestone> TIEUTIEN_MILESTONES = new HashMap<>();
    private static int[] milestoneAmounts = new int[0];
    private static final Object LOCK = new Object();

    public static class TieuTienMilestone {
        public int id;
        public int requiredAmount;
        public String name;
        public String detail;

        public TieuTienMilestone(int id, int requiredAmount, String name, String detail) {
            this.id = id;
            this.requiredAmount = requiredAmount;
            this.name = name;
            this.detail = detail;
        }

        public void addRewardsToMailbox(Player player) {
            try {
                JSONArray dataArray = (JSONArray) JSONValue.parse(detail);
                if (dataArray != null) {
                    for (int i = 0; i < dataArray.size(); i++) {
                        JSONObject dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                        int tempId = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                        int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));

                        Item item = ItemService.gI().createNewItem((short) tempId);
                        item.quantity = quantity;

                        JSONArray optionsArray = (JSONArray) dataObject.get("options");
                        if (optionsArray != null) {
                            for (int j = 0; j < optionsArray.size(); j++) {
                                JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                int param = Integer.parseInt(String.valueOf(optionObject.get("param")));
                                int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                item.itemOptions.add(new ItemOption(optionId, param));
                            }
                        }
                        player.inventory.itemsMailBox.add(item);
                    }
                }
            } catch (Exception e) {
                Logger.logException(Achievement_TieuTien.class, e, "Error adding rewards to mailbox");
            }
        }
    }

    static {
        loadTieuTienMilestonesFromDB();
    }

    private static void loadTieuTienMilestonesFromDB() {
        Logger.success("Starting to load moc_tieutien from database...\n");
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement("SELECT id, info, required_amount, detail FROM moc_tieutien ORDER BY required_amount");
             ResultSet rs = ps.executeQuery()) {

            TIEUTIEN_MILESTONES.clear();
            List<Integer> amountsList = new ArrayList<>();

            while (rs.next()) {
                int id = rs.getInt("id");
                String info = rs.getString("info");
                int requiredAmount = rs.getInt("required_amount");
                String detail = rs.getString("detail");

                TieuTienMilestone milestone = new TieuTienMilestone(id, requiredAmount, info, detail);
                TIEUTIEN_MILESTONES.put(requiredAmount, milestone);
                amountsList.add(requiredAmount);

                Logger.success("Loaded moc_tieutien #" + id + ": " + info + " (" + requiredAmount + " vàng)\n");
            }

            milestoneAmounts = amountsList.stream().mapToInt(Integer::intValue).toArray();
            Logger.success("Successfully loaded " + TIEUTIEN_MILESTONES.size() + " moc_tieutien from database\n");

        } catch (SQLException e) {
            Logger.logException(Achievement_TieuTien.class, e, "CRITICAL: Cannot load moc_tieutien from DB!");
        }
    }

    public static void reloadTieuTienMilestones() {
        synchronized (LOCK) {
            loadTieuTienMilestonesFromDB();
        }
    }

    public static int[] getMilestoneAmounts() {
        return milestoneAmounts;
    }

    public static Map<Integer, TieuTienMilestone> getTieuTienMilestones() {
        return TIEUTIEN_MILESTONES;
    }

    public static String formatMoney(long amount) {
        if (amount >= 1000000000) {
            return (amount / 1000000000) + " tỷ";
        } else if (amount >= 1000000) {
            return (amount / 1000000) + " triệu";
        } else if (amount >= 1000) {
            return (amount / 1000) + "k";
        }
        return amount + "";
    }

    public static void openTieuTienMilestoneUI(Player player) {
        Message msg = null;
        try {
            long currentTieuTien = player.tieutien;

            msg = new Message(-76);
            msg.writer().writeByte(0);
            msg.writer().writeByte(milestoneAmounts.length);

            for (int amount : milestoneAmounts) {
                TieuTienMilestone m = TIEUTIEN_MILESTONES.get(amount);
                if (m != null) {
                    boolean reached = currentTieuTien >= m.requiredAmount;
                    boolean received = player.achievement.isRecieveTieuTienMilestone(amount);
                    msg.writer().writeUTF(m.name);
                    msg.writer().writeUTF("Đã tiêu: " + formatMoney(currentTieuTien) + "/" + formatMoney(m.requiredAmount));
                    msg.writer().writeShort(0);
                    msg.writer().writeBoolean(reached);
                    msg.writer().writeBoolean(received);
                }
            }
            player.sendMessage(msg);
            player.typeRecvieArchiment = 6;
        } catch (Exception e) {
            Logger.logException(Achievement_TieuTien.class, e, "Error opening tieutien milestone UI");
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void confirmTieuTienMilestone(Player player, byte select) {
        if (player.achievement == null) {
            return;
        }

        if (select < 0 || select >= milestoneAmounts.length) {
            Service.gI().sendThongBao(player, "Mốc không hợp lệ");
            return;
        }

        int amount = milestoneAmounts[select];
        TieuTienMilestone milestone = TIEUTIEN_MILESTONES.get(amount);

        if (milestone == null) {
            Service.gI().sendThongBao(player, "Mốc không tồn tại");
            return;
        }

        if (player.achievement.isRecieveTieuTienMilestone(amount)) {
            Service.gI().sendThongBao(player, "Bạn đã nhận phần thưởng mốc này rồi");
            return;
        }

        if (player.tieutien < amount) {
            Service.gI().sendThongBao(player, "Chưa đủ tiêu tiền. Cần " + formatMoney(amount));
            return;
        }

        milestone.addRewardsToMailbox(player);

        if (NDVSqlFetcher.updateMailBox(player)) {
            player.achievement.receiveTieuTienMilestone(amount);

            Service.gI().sendThongBao(player, "Nhận thưởng mốc " + milestone.name + "\nVui lòng kiểm tra hòm thư");

            Message msg = null;
            try {
                msg = new Message(-76);
                msg.writer().writeByte(1);
                msg.writer().writeByte(select);
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(Achievement_TieuTien.class, e, "Error sending milestone confirm message");
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        } else {
            Service.gI().sendThongBao(player, "Lỗi cập nhật hòm thư");
        }
    }
}
