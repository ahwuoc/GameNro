package nro.player;

import item.Item;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import jdbc.DBConnecter;
import jdbc.daos.NDVSqlFetcher;

import network.Message;
import nro.services.InventoryService;
import org.json.simple.JSONObject;
import nro.services.ItemService;
import nro.services.Service;
import utils.Logger;

public class ArchivementSanBoss {

    public String info1;
    public String info2;
    public short money;
    public boolean isFinish;
    public boolean isRecieve;
    public static ArchivementSanBoss gI = null;
    
    public static ArchivementSanBoss gI() {
        if (gI == null) {
            return new ArchivementSanBoss();
        }
        return gI;
    }

    public ArchivementSanBoss() {
    }

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

    public final static int DIEM = 500;
    public static int[] DIEMSANBOSS = {
            DIEM * 1, DIEM * 2, DIEM * 3, DIEM * 5, DIEM * 7, DIEM * 10, DIEM * 20,
            DIEM * 40, DIEM * 60, DIEM * 80
    };

    // CACHE moc_san_boss: key = id (int), value = parsed JSONArray(detail)
    private static final Map<Integer, JSONArray> MOC_SAN_BOSS_CACHE = new HashMap<>();

    public ArchivementSanBoss(String info1, String info2, short money, boolean isFinish, boolean isRecieve) {
        this.info1 = info1;
        this.info2 = info2;
        this.money = money;
        this.isFinish = isFinish;
        this.isRecieve = isRecieve;
    }
    
    public static void loadMocSanBossCache() {
        MOC_NAP_CACHE.clear();
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement("SELECT id, detail FROM moc_san_boss"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String detail = rs.getString("detail");
                try {
                    JSONArray dataArray = (JSONArray) JSONValue.parse(detail);
                    if (dataArray == null) {
                        dataArray = new JSONArray();
                    }
                    MOC_NAP_CACHE.put(id, dataArray);
                } catch (Exception e) {
                    e.printStackTrace();
                    MOC_NAP_CACHE.put(id, new JSONArray());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void Show(Player pl) {
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(0); // action
            msg.writer().writeByte(pl.archivementListDiem.size());
            for (int i = 0; i < pl.archivementListDiem.size(); i++) {

                ArchivementSanBoss archivement = pl.archivementListDiem.get(i);
                if (pl.getSession().version <= 231 || pl.getSession().version > 235) {
                    msg.writer().writeUTF(archivement.getInfo1());
                    msg.writer().writeUTF(archivement.getInfo2());
                    msg.writer().writeShort(archivement.getMoney()); //money
                    msg.writer().writeBoolean(archivement.isFinish);
                    msg.writer().writeBoolean(archivement.isRecieve);

                } else {
                    msg.writer().writeUTF(archivement.getInfo1());
                    msg.writer().writeUTF(archivement.getInfo2());
                    msg.writer().writeShort(archivement.getMoney()); //money
                    msg.writer().writeUTF("");
                    msg.writer().writeBoolean(archivement.isFinish);
                    msg.writer().writeBoolean(archivement.isRecieve);
                    msg.writer().writeShort(10895);//res icon
                }

            }
            pl.sendMessage(msg);
            msg.cleanup();
            pl.typeRecvieArchiment = 3;
        } catch (IOException e) {

            e.getStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }
    public boolean checkdiemboss(Player pl, int index) {
        if (pl == null || pl.getSession() == null) return false;
        if (index < 0 || index >= DIEMSANBOSS.length) return false;
        return pl.getSession().diemboss >= DIEMSANBOSS[index];
    }
    public void receiveGem(int index, Player pl) {
        ArchivementSanBoss temp = pl.archivementListDiem.get(index);
        if (temp.isRecieve) {
            Service.gI().sendThongBaoOK(pl, "Nhận rồi đừng nhận nữua");
            return;
        }
        if (temp != null) {
            Message msg = null;
            try {
                msg = new Message(-76);
                msg.writer().writeByte(1); // action
                msg.writer().writeByte(index); // index
                pl.sendMessage(msg);
                msg.cleanup();
            } catch (IOException e) {
                e.printStackTrace();
                Logger.logException(this.getClass(), e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                    msg = null;
                }
            }

            pl.archivementListDiem.get(index).setRecieve(true);
            try {
                JSONArray dataArray = new JSONArray();

                for (ArchivementSanBoss arr : pl.archivementListDiem) {
                    dataArray.add(arr.isRecieve ? "1" : "0");
                }
                String inventory = dataArray.toJSONString();
                dataArray.clear();
                DBConnecter.executeUpdate("update player set Achievement_DiemBoss = ? where id = ?", inventory, pl.id);
                nhanDiemBoss(pl, index + 1);
                System.out.println("Player " + pl.name + " Nhận quà thành công");

            } catch (Exception e) {
                e.printStackTrace();
            }
            Service.gI().sendThongBao(pl, "Nhận thành công, vui lòng kiểm tra hòm thư ");
        } else {
            Service.gI().sendThongBao(pl, "Không có phần thưởng");
        }
    }
    private static final Map<Integer, JSONArray> MOC_NAP_CACHE = new HashMap<>();
    private void nhanDiemBoss(Player pl, int index) {
        Item item;
        JSONArray dataArray = MOC_NAP_CACHE.get(index);
        JSONObject dataObject;
        if (dataArray == null) {
            try (Connection con2 = DBConnecter.getConnectionServer(); PreparedStatement ps = con2.prepareStatement("SELECT detail FROM moc_san_boss WHERE id = ?")) {
                ps.setInt(1, index);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                        if (dataArray != null) {
                            MOC_NAP_CACHE.put(index, dataArray);
                        }
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (dataArray == null || dataArray.isEmpty()) {
            Service.gI().sendThongBao(pl, "Không có dữ liệu quà mốc nạp " + index);
            return;
        }
        try {
            for (int i = 0; i < dataArray.size(); i++) {
                Object obj = dataArray.get(i);
                if (obj == null) {
                    continue;
                }
                dataObject = (JSONObject) JSONValue.parse(String.valueOf(obj));
                if (dataObject == null) {
                    continue;
                }

                int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                item = ItemService.gI().createNewItem((short) tempid);
                item.quantity = quantity;
                JSONArray optionsArray = (JSONArray) dataObject.get("options");
                if (optionsArray != null) {
                    for (Object opt : optionsArray) {
                        JSONObject optObj = (JSONObject) JSONValue.parse(String.valueOf(opt));
                        if (optObj == null) {
                            continue;
                        }
                        int param = Integer.parseInt(String.valueOf(optObj.get("param")));
                        int optionId = Integer.parseInt(String.valueOf(optObj.get("id")));
                        item.itemOptions.add(new Item.ItemOption(optionId, param));
                    }
                }

                pl.inventory.itemsMailBox.add(item);
            }
            if (NDVSqlFetcher.updateMailBox(pl)) {
                Service.gI().sendThongBao(pl, "Bạn vừa nhận quà về mail thành công");
            } else {
                Service.gI().sendThongBao(pl, "Lỗi gửi quà, liên hệ admin");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Service.gI().sendThongBao(pl, "Có lỗi khi tạo quà mốc nạp");
        }
    }
    
    public void getMocSanBoss(Player player) {
        try {
            if (player.getSession() == null) {
                return;
            }

            Connection con = null;
            PreparedStatement ps = null;
            JSONValue jv = new JSONValue();
            JSONArray dataArray = null;
            JSONArray dataArrayTemp = null;
            con = DBConnecter.getConnectionServer();
            ps = con.prepareStatement("SELECT `Achievement_DiemBoss` FROM `player` WHERE id = ? LIMIT 1");
            ps.setInt(1, (int) player.id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String achievementData = rs.getString(1);
                    try {
                        dataArray = (JSONArray) jv.parse(achievementData);
                        if (dataArray != null && dataArray.size() != 10) {
                            if (dataArray.size() < 10) {
                                for (int j = dataArray.size(); j < 10; j++) {
                                    dataArray.add(0);
                                }
                            }

                            while (dataArray.size() > 10) {

                                dataArray.remove(10);

                            }

                        }
                        player.archivementList.clear();
                        if (dataArray != null) {

                            for (int i = 0; i < dataArray.size(); i++) {
                                try {
                                    ArchivementSanBoss achievement = new ArchivementSanBoss();
                                    achievement.setInfo1("Mốc Điểm " + getNhiemVu4(i));
                                    achievement.setInfo2("Điểm hiện tại: " + getNhiemVu3(player, i) + "/" + getNhiemVu4(i));
                                    achievement.setFinish(checkdiemboss(player, i));
                                    achievement.setMoney((short) getRuby(i));
                                    achievement.setRecieve(Integer.parseInt(String.valueOf(dataArray.get(i))) != 0);
                                    player.archivementListDiem.add(achievement);

                                } catch (Exception ee) {
                                    ee.printStackTrace();
                                    return;
                                }
                            }

                        }
                        dataArray.clear();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Show(player);
                rs.close();
                ps.close();
                con.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String getNhiemVu4(int index) {
        switch (index) {
            case 0:
                return "" + DIEMSANBOSS[0];
            case 1:
                return "" + DIEMSANBOSS[1];
            case 2:
                return "" + DIEMSANBOSS[2];
            case 3:
                return "" + DIEMSANBOSS[3];
            case 4:
                return "" + DIEMSANBOSS[4];
            case 5:
                return "" + DIEMSANBOSS[5];
            case 6:
                return "" + DIEMSANBOSS[6];
            case 7:
                return "" + DIEMSANBOSS[7];
            case 8:
                return "" + DIEMSANBOSS[8];
            case 9:
                return "" + DIEMSANBOSS[9];
            case 10:
                return "" + DIEMSANBOSS[10];
            case 11:
                return "" + DIEMSANBOSS[11];
            case 12:
                return "" + DIEMSANBOSS[12];
            case 13:
                return "" + DIEMSANBOSS[13];
            default:
                return "";
        }
    }

    public String getNhiemVu3(Player player, int index) {
        switch (index) {
            case 0:
                return " " + player.getSession().diemboss + "";
            case 1:
                return " " + player.getSession().diemboss + "";
            case 2:
                return " " + player.getSession().diemboss + "";
            case 3:
                return " " + player.getSession().diemboss + "";
            case 4:
                return " " + player.getSession().diemboss + "";
            case 5:
                return " " + player.getSession().diemboss + "";
            case 6:
                return " " + player.getSession().diemboss + "";
            case 7:
                return " " + player.getSession().diemboss + "";
            case 8:
                return " " + player.getSession().diemboss + "";
            case 9:
                return " " + player.getSession().diemboss + "";
            case 10:
                return " " + player.getSession().diemboss + "";
            case 11:
                return " " + player.getSession().diemboss + "";
            case 12:
                return " " + player.getSession().diemboss + "";
            case 13:
                return " " + player.getSession().diemboss + "";
            default:
                return "";
        }
    }

    public int getRuby(int index) {
        switch (index) {
            case 0:
                return 0;
            case 1:
                return 0;
            case 2:
                return 0;
            case 3:
                return 0;
            case 4:
                return 0;
            case 5:
                return 0;
            case 6:
                return 0;
            case 7:
                return 0;
            case 8:
                return 0;
            case 9:
                return 0;
            case 10:
                return 0;
            case 11:
                return 0;
            case 12:
                return 0;
            case 13:
                return 0;

            default:
                return -1;
        }
    }
}
