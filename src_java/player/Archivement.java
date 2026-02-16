package player;

import item.Item;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import jdbc.DBConnecter;
import jdbc.daos.NDVSqlFetcher;
import lombok.Getter;
import network.Message;
import org.json.simple.JSONObject;
import services.ItemService;
import services.Service;
import utils.Logger;

public class Archivement {

    public String info1;
    public String info2;
    public short money;
    public boolean isFinish;
    public boolean isRecieve;

    @Getter
    private Set<Integer> receivedTieuTienMilestones = new HashSet<>();

    public String getInfo1() {
        return info1;
    }

    public void setInfo1(String info1) {
        this.info1 = info1;
    }

    public String getInfo2() {
        return info2;
    }

    public void setInfo2(String info2) {
        this.info2 = info2;
    }

    public short getMoney() {
        return money;
    }

    public void setMoney(short money) {
        this.money = money;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setFinish(boolean finish) {
        isFinish = finish;
    }

    public boolean isRecieve() {
        return isRecieve;
    }

    public void setRecieve(boolean recieve) {
        isRecieve = recieve;
    }

    public static Archivement gI = null;

    // Load từ database thay vì hardcode
    private static int[] cachedMocNap = null;
    private static int totalAchievements = 0;
    private static final Object LOCK = new Object();

    public static int[] GIADOLACHIADOI() {
        if (cachedMocNap == null) {
            synchronized (LOCK) {
                if (cachedMocNap == null) {
                    loadMocNapFromDB();
                }
            }
        }
        return cachedMocNap;
    }

    public static int getMocNap(int index) {
        int[] mocNap = GIADOLACHIADOI();
        if (index >= 0 && index < mocNap.length) {
            return mocNap[index];
        }
        return 0;
    }

    public static int getTotalAchievements() {
        if (totalAchievements == 0) {
            synchronized (LOCK) {
                if (totalAchievements == 0) {
                    loadMocNapFromDB();
                }
            }
        }
        return totalAchievements;
    }

    private static void loadMocNapFromDB() {
        Logger.success("Starting to load moc_nap from database...\n");
        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement("SELECT required_amount FROM moc_nap ORDER BY id");
                ResultSet rs = ps.executeQuery()) {
            java.util.List<Integer> mocNapList = new java.util.ArrayList<>();
            int count = 0;
            while (rs.next()) {
                int amount = rs.getInt("required_amount");
                mocNapList.add(amount);
                count++;
                Logger.success("Loaded moc_nap #" + count + ": " + amount + "\n");
            }
            cachedMocNap = mocNapList.stream().mapToInt(Integer::intValue).toArray();
            totalAchievements = cachedMocNap.length;
            Logger.success("========================================\n");
            Logger.success("Successfully loaded " + totalAchievements + " moc nap from database\n");
            Logger.success("Moc nap array: " + java.util.Arrays.toString(cachedMocNap) + "\n");
            Logger.success("========================================\n");
        } catch (SQLException e) {
            Logger.logException(Archivement.class, e, "CRITICAL: Cannot load moc_nap from DB!");
            Logger.success("Failed to load moc_nap - Setting empty array\n");
            cachedMocNap = new int[0];
            totalAchievements = 0;
        }
    }

    public static void reloadMocNap() {
        synchronized (LOCK) {
            cachedMocNap = null;
            totalAchievements = 0;
            loadMocNapFromDB();
        }
    }

    public static Archivement gI() {
        if (gI == null) {
            return new Archivement();
        }
        return gI;
    }

    public Archivement() {
    }

    public Archivement(String info1, String info2, short money, boolean isFinish, boolean isRecieve) {
        this.info1 = info1;
        this.info2 = info2;
        this.money = money;
        this.isFinish = isFinish;
        this.isRecieve = isRecieve;
    }

    public void Show(Player pl) {
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(0); // action
            msg.writer().writeByte(pl.archivementList.size());
            for (int i = 0; i < pl.archivementList.size(); i++) {

                Archivement archivement = pl.archivementList.get(i);
                if (pl.getSession().version <= 231 || pl.getSession().version > 235) {
                    msg.writer().writeUTF(archivement.getInfo1());
                    msg.writer().writeUTF(archivement.getInfo2());
                    msg.writer().writeShort(archivement.getMoney()); // money
                    msg.writer().writeBoolean(archivement.isFinish);
                    msg.writer().writeBoolean(archivement.isRecieve);
                } else {
                    msg.writer().writeUTF(archivement.getInfo1());
                    msg.writer().writeUTF(archivement.getInfo2());
                    msg.writer().writeShort(archivement.getMoney()); // money
                    msg.writer().writeUTF("");
                    msg.writer().writeBoolean(archivement.isFinish);
                    msg.writer().writeBoolean(archivement.isRecieve);
                    msg.writer().writeShort(10895);// res icon
                }

            }
            pl.sendMessage(msg);
            msg.cleanup();
            pl.typeRecvieArchiment = 1;
        } catch (IOException e) {

            e.getStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    public boolean checktongnap(Player pl, int index) {
        int requiredAmount = getMocNap(index);
        if (requiredAmount <= 0) {
            return false;
        }
        return pl.getSession().danap >= requiredAmount;
    }

    public void receiveGem(int index, Player pl) {
        Logger.logException(this.getClass(), null, "receiveGem called - Player: " + pl.name + ", Index: " + index
                + ", ListSize: " + pl.archivementList.size());

        if (index < 0 || index >= pl.archivementList.size()) {
            Logger.logException(this.getClass(), null, "Invalid index - Player: " + pl.name + ", Index: " + index);
            Service.gI().sendThongBao(pl, "Không có phần thưởng");
            return;
        }

        Archivement temp = pl.archivementList.get(index);
        if (temp.isRecieve) {
            Logger.logException(this.getClass(), null, "Already received - Player: " + pl.name + ", Index: " + index);
            Service.gI().sendThongBaoOK(pl, "Nhận rồi đừng nhận nữua");
            return;
        }

        Logger.logException(this.getClass(), null, "Processing reward - Player: " + pl.name + ", Index: " + index);

        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(1); // action
            msg.writer().writeByte(index); // index
            pl.sendMessage(msg);
        } catch (IOException e) {
            Logger.logException(this.getClass(), e, "Error sending achievement message");
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }

        pl.archivementList.get(index).setRecieve(true);
        try {
            @SuppressWarnings("unchecked")
            JSONArray dataArray = new JSONArray();

            for (Archivement arr : pl.archivementList) {
                dataArray.add(arr.isRecieve ? "1" : "0");
            }
            String inventory = dataArray.toJSONString();
            dataArray.clear();
            Logger.logException(this.getClass(), null,
                    "Updating DB - Player: " + pl.name + ", Achievement data: " + inventory);
            DBConnecter.executeUpdate("update player set Achievement = ? where id = ?", inventory, pl.id);

            int mocNapId = index;
            Logger.logException(this.getClass(), null,
                    "Calling confirm_mocnap - Player: " + pl.name + ", Index: " + index);
            confirm_mocnap(pl, mocNapId);

            Logger.logException(this.getClass(), null, "Reward completed - Player: " + pl.name);
            System.out.println("Player " + pl.name + " Nhận quà thành công");
            Service.gI().sendThongBao(pl, "Nhận thành công, vui lòng kiểm tra hòm thư ");
        } catch (Exception e) {
            Logger.logException(this.getClass(), e, "Error receiving achievement reward - Player: " + pl.name);
        }
    }

    private void confirm_mocnap(Player pl, int index) {
        Logger.logException(this.getClass(), null,
                "confirm_mocnap called - Player: " + pl.name + ", MocNap ID: " + index);

        Item item = null;
        JSONArray dataArray;
        JSONObject dataObject;
        try (Connection con2 = DBConnecter.getConnectionServer();
                PreparedStatement ps = con2.prepareStatement("SELECT detail FROM moc_nap WHERE id = ?")) {
            ps.setInt(1, index);
            Logger.logException(this.getClass(), null, "Querying moc_nap - ID: " + index);

            try (ResultSet rs = ps.executeQuery()) {
                int itemCount = 0;
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    Logger.logException(this.getClass(), null, "Found moc_nap record - ID: " + index);
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                    Logger.logException(this.getClass(), null, "Parsed detail JSON - Items count: " + dataArray.size());

                    for (int i = 0; i < dataArray.size(); i++) {
                        dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                        int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                        int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                        Logger.logException(this.getClass(), null,
                                "Creating item - TempID: " + tempid + ", Quantity: " + quantity);

                        item = ItemService.gI().createNewItem((short) tempid);
                        item.quantity = quantity;
                        JSONArray optionsArray = (JSONArray) dataObject.get("options");

                        for (int j = 0; j < optionsArray.size(); j++) {
                            JSONObject optionObject = (JSONObject) optionsArray.get(j);
                            int param = Integer.parseInt(String.valueOf(optionObject.get("param")));
                            int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                            item.itemOptions.add(new Item.ItemOption(optionId, param));
                        }
                        pl.inventory.itemsMailBox.add(item);
                        itemCount++;
                    }

                    Logger.logException(this.getClass(), null,
                            "Added " + itemCount + " items to mailbox - Player: " + pl.name);
                    if (NDVSqlFetcher.updateMailBox(pl)) {
                        Logger.logException(this.getClass(), null, "Mailbox updated successfully - Player: " + pl.name);
                        Service.gI().sendThongBao(pl, "Bạn vừa nhận quà về mail thành công");
                    } else {
                        Logger.logException(this.getClass(), null, "Failed to update mailbox - Player: " + pl.name);
                    }
                }

                if (!found) {
                    Logger.logException(this.getClass(), null, "WARNING: No moc_nap record found - ID: " + index
                            + " (This is OK if it's the last milestone)");
                }
            }

        } catch (SQLException e) {
            Logger.logException(this.getClass(), e,
                    "SQL Error in confirm_mocnap - Player: " + pl.name + ", MocNap ID: " + index);
        } catch (Exception e) {
            Logger.logException(this.getClass(), e,
                    "Error in confirm_mocnap - Player: " + pl.name + ", MocNap ID: " + index);
        }
    }

    public void getAchievement(Player player) {
        try {
            if (player.getSession() == null) {
                return;
            }

            int totalAchievements = getTotalAchievements();
            if (totalAchievements == 0) {
                Logger.logException(Archivement.class, null, "No achievements loaded from database");
                return;
            }

            JSONArray dataArray = null;
            try (Connection con = DBConnecter.getConnectionServer();
                    PreparedStatement ps = con
                            .prepareStatement("SELECT `Achievement` FROM `player` WHERE id = ? LIMIT 1")) {
                ps.setInt(1, (int) player.id);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String achievementData = rs.getString(1);
                        try {
                            // Check null TRƯỚC khi parse
                            if (achievementData == null || achievementData.isEmpty()) {
                                dataArray = new JSONArray();
                                for (int j = 0; j < totalAchievements; j++) {
                                    dataArray.add(0);
                                }
                            } else {
                                dataArray = (JSONArray) JSONValue.parse(achievementData);
                                if (dataArray == null) {
                                    dataArray = new JSONArray();
                                    for (int j = 0; j < totalAchievements; j++) {
                                        dataArray.add(0);
                                    }
                                } else if (dataArray.size() != totalAchievements) {
                                    if (dataArray.size() < totalAchievements) {
                                        for (int j = dataArray.size(); j < totalAchievements; j++) {
                                            dataArray.add(0);
                                        }
                                    } else if (dataArray.size() > totalAchievements) {
                                        while (dataArray.size() > totalAchievements) {
                                            dataArray.remove(totalAchievements);
                                        }
                                    }
                                }
                            }

                            player.archivementList.clear();
                            for (int i = 0; i < dataArray.size(); i++) {
                                Archivement achievement = new Archivement();
                                achievement.setInfo1("Mốc nạp " + getNhiemVu(i));
                                achievement.setInfo2("Đã nạp: " + getNhiemVu2(player, i) + "/" + getNhiemVu(i));
                                achievement.setFinish(checktongnap(player, i));
                                achievement.setMoney((short) 0);
                                achievement.setRecieve(Integer.parseInt(String.valueOf(dataArray.get(i))) != 0);
                                player.archivementList.add(achievement);
                            }
                            dataArray.clear();
                        } catch (Exception e) {
                            Logger.logException(this.getClass(), e,
                                    "Error parsing achievement data for player " + player.id);
                        }
                    }
                }
            }
            Show(player);
        } catch (Exception e) {
            Logger.logException(this.getClass(), e, "Error loading achievements for player " + player.id);
        }
    }

  
    public String getNhiemVu(int index) {
        int amount = getMocNap(index);
        return amount > 0 ? String.valueOf(amount) : "";
    }

    public String getNhiemVu2(Player player, int index) {
        return " " + player.getSession().danap + "";
    }

}
