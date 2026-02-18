package clan;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import jdbc.DBConnecter;
import models.RedRibbonHQ.RedRibbonHQ;
import nro.services.ClanService;

import java.util.ArrayList;
import java.util.List;

import nro.player.Player;
import nro.server.Client;
import nro.services.Service;
import network.Message;
import jdbc.daos.NDVSqlFetcher;
import utils.Logger;
import utils.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import models.TreasureUnderSea.TreasureUnderSea;
import models.SnakeWay.SnakeWay;
import models.DestronGas.DestronGas;
import nro.server.Manager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utils.TimeUtil;

public class Clan {

    public static int NEXT_ID = 0;

    public int clanMessageId = 0;
    private final List<ClanMessage> clanMessages;

    public static final byte LEADER = 0;
    public static final byte DEPUTY = 1;
    public static final byte MEMBER = 2;

    public int id;
    public int imgId;
    public String name;
    public String name2;
    public String slogan;
    public int createTime;
    public long powerPoint;
    public byte maxMember;
    public int level;
    public boolean active;
    public int capsuleClan;

    public long lastTimeOpenDoanhTrai;
    public boolean haveGoneDoanhTrai;
    public RedRibbonHQ doanhTrai;
    public Player playerOpenDoanhTrai;

    public final List<ClanMember> members;
    public final List<Player> membersInGame;

    public TreasureUnderSea BanDoKhoBau;
    public long lastTimeOpenBanDoKhoBau;
    public Player playerOpenBanDoKhoBau;

    public SnakeWay ConDuongRanDoc;
    public long lastTimeOpenConDuongRanDoc;
    public Player playerOpenConDuongRanDoc;

    public DestronGas KhiGasHuyDiet;
    public long lastTimeOpenKhiGasHuyDiet;
    public Player playerOpenKhiGasHuyDiet;
    public int timesPerDayKGHD;
    //khí gas
    public long thoiGianHoanThanhKhiGas;
    public int levelDoneKhiGas;
    //bdkb
    public long thoiGianHoanThanhBDKB;
    public int levelDoneBanDoKhoBau;
    //cdrd
    public long thoiGianHoanThanhCDRD;
    public int levelDoneConDuongRanDoc;

    public long timeUpdateClan;

    public Clan() {
        this.id = NEXT_ID++;
        this.name = "";
        this.name2 = "";
        this.slogan = "";
        this.maxMember = 10;
        this.level = 1;
        this.createTime = (int) (System.currentTimeMillis() / 1000);
        this.members = new ArrayList<>();
        this.membersInGame = new ArrayList<>();
        this.clanMessages = new ArrayList<>();
    }

    public boolean canUpdateClan(Player player) {
        if (Util.canDoWithTime(timeUpdateClan, 60000)) {
            timeUpdateClan = System.currentTimeMillis();
            return true;
        }
        Service.gI().sendThongBao(player, "Vui lòng đợi " + TimeUtil.getTimeLeft(timeUpdateClan, 60) + " nữa");
        return false;
    }

    public ClanMember getLeader() {
        for (ClanMember cm : members) {
            if (cm.role == LEADER) {
                return cm;
            }
        }
        ClanMember cm = new ClanMember();
        cm.name = "Bang chủ";
        return cm;
    }

    public byte getRole(Player player) {
        for (ClanMember cm : members) {
            if (cm.id == player.id) {
                return cm.role;
            }
        }
        return -1;
    }

    public boolean isLeader(Player player) {
        for (ClanMember cm : members) {
            if (cm.id == player.id && cm.role == LEADER) {
                return true;
            }
        }
        return false;
    }

    public boolean isDeputy(Player player) {
        for (ClanMember cm : members) {
            if (cm.id == player.id && cm.role == DEPUTY) {
                return true;
            }
        }
        return false;
    }

    public void addSMTNClan(Player plOri, long param) {
        for (int i = this.membersInGame.size() - 1; i >= 0; i--) {
            Player pl = this.membersInGame.get(i);
            if (!plOri.equals(pl) && pl != null && pl.zone != null && plOri.zone.equals(pl.zone)) {
                long tnsm = (param / (Math.abs(Service.gI().getCurrLevel(pl) - Service.gI().getCurrLevel(plOri)) + 1));
                Service.gI().addSMTN(pl, (byte) 1, tnsm, false);
            }
        }
    }

    public void sendMessageClan(ClanMessage cmg) {
        Message msg;
        try {
            msg = new Message(-51);
            msg.writer().writeByte(cmg.type);
            msg.writer().writeInt(cmg.id);
            msg.writer().writeInt(cmg.playerId);
            if (cmg.type == 2) {
                msg.writer().writeUTF(cmg.playerName + " (" + Util.numberToMoney(cmg.playerPower) + ")");
            } else {
                msg.writer().writeUTF(cmg.playerName);
            }
            msg.writer().writeByte(cmg.role);
            msg.writer().writeInt(cmg.time);
            if (cmg.type == 0) {
                msg.writer().writeUTF(cmg.text);
                msg.writer().writeByte(cmg.color);
            } else if (cmg.type == 1) {
                msg.writer().writeByte(cmg.receiveDonate);
                msg.writer().writeByte(cmg.maxDonate);
                msg.writer().writeByte(cmg.isNewMessage);
            }
            for (Player pl : this.membersInGame) {
                pl.sendMessage(msg);
            }
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void addClanMessage(ClanMessage cmg) {
        this.clanMessages.add(0, cmg);
        if (clanMessages.size() > 20) {
            for (int i = clanMessages.size() - 1; i >= 20; i--) {
                clanMessages.remove(i).dispose();
            }
        }
    }

    public ClanMessage getClanMessage(int clanMessageId) {
        for (ClanMessage cmg : this.clanMessages) {
            if (cmg.id == clanMessageId) {
                return cmg;
            }
        }
        return null;
    }

    public List<ClanMessage> getCurrClanMessages() {
        List<ClanMessage> list = new ArrayList();
        if (this.clanMessages.size() <= 20) {
            list.addAll(this.clanMessages);
        } else {
            for (int i = 0; i < 20; i++) {
                list.add(this.clanMessages.get(i));
            }
        }
        return list;
    }

    public void sendMyClanForAllMember() {
        for (Player pl : this.membersInGame) {
            if (pl != null) {
                ClanService.gI().sendMyClan(pl);
            }
        }
    }

    public void sendFlagBagForAllMember() {
        for (Player pl : this.membersInGame) {
            if (pl != null) {
                Service.gI().sendFlagBag(pl);
            }
        }
    }

    public void addMemberOnline(Player player) {
        this.membersInGame.add(player);
    }

    public void removeMemberOnline(ClanMember cm, Player player) {
        if (player != null) {
            this.membersInGame.remove(player);
        }
        if (cm != null) {
            for (int i = this.membersInGame.size() - 1; i >= 0; i--) {
                if (this.membersInGame.get(i).id == cm.id) {
                    this.membersInGame.remove(i);
                    break;
                }
            }
        }
    }

    public Player getPlayerOnline(int playerId) {
        for (Player player : this.membersInGame) {
            if (player.id == playerId) {
                return player;
            }
        }

        Player playeroffline = NDVSqlFetcher.loadById(playerId);
        if (playeroffline != null) {
            return playeroffline;
        }

        return null;
    }

    //load db danh sách member
    public void addClanMember(ClanMember cm) {
        this.members.add(cm);
    }

    //thêm vào khi player tạo mới clan or mới vào clan
    public void addClanMember(Player player, byte role) {
        ClanMember cm = new ClanMember(player, this, role);
        this.members.add(cm);
        player.clanMember = cm;
    }

    //xóa khi member rời clan or bị kích
    public void removeClanMember(ClanMember cm) {
        this.members.remove(cm);
        cm.dispose();
    }

    public byte getCurrMembers() {
        return (byte) this.members.size();
    }

    public List<ClanMember> getMembers() {
        return this.members;
    }

    public ClanMember getClanMember(int memberId) {
        for (ClanMember cm : members) {
            if (cm.id == memberId) {
                return cm;
            }
        }
        return null;
    }

    public void reloadClanMember() {
        for (ClanMember cm : this.members) {
            Player pl = Client.gI().getPlayer(cm.id);
            if (pl != null) {
                cm.powerPoint = pl.nPoint.power;
            }
        }
    }

    public void insert() {
        JSONArray dataArray = new JSONArray();
        JSONObject dataObject = new JSONObject();
        for (ClanMember cm : this.members) {
            dataObject.put("id", cm.id);
            dataObject.put("name", cm.name);
            dataObject.put("head", cm.head);
            dataObject.put("body", cm.body);
            dataObject.put("leg", cm.leg);
            dataObject.put("role", cm.role);
            dataObject.put("donate", cm.donate);
            dataObject.put("receive_donate", cm.receiveDonate);
            dataObject.put("member_point", cm.memberPoint);
            dataObject.put("clan_point", cm.clanPoint);
            dataObject.put("join_time", cm.joinTime);
            dataObject.put("ask_pea_time", cm.timeAskPea);
            dataObject.put("power", cm.powerPoint);
            dataArray.add(dataObject.toJSONString());
            dataObject.clear();
        }
        String member = dataArray.toJSONString();
        String topBanDoKhoBau = "[" + levelDoneBanDoKhoBau + "," + thoiGianHoanThanhBDKB + "]";
        String thongTinLeader = "[" + getLeader().id + "," + getLeader().name + "," + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        String topKhiGas = "[" + levelDoneKhiGas + "," + thoiGianHoanThanhKhiGas + "]";
        String topConDuongRanDoc = "[" + levelDoneConDuongRanDoc + "," + thoiGianHoanThanhCDRD + "]";
        String thongTinLeader2 = "[" + getLeader().id + "," + getLeader().name + "," + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        String thongTinLeader3 = "[" + getLeader().id + "," + getLeader().name + "," + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        dataArray.clear();

        String top = dataArray.toJSONString();

        // Lấy ID mới từ DB
        this.id = DBConnecter.gI().getNextClanId();

        PreparedStatement ps = null;
        try (Connection con = DBConnecter.getConnectionServer();) {
            ps = con.prepareStatement("insert into clan (id, name, name_2, slogan, img_id, power_point, max_member, clan_point, level, members, tops, thanhTichBDKB, thongTinLeader, thanhTichKhiGas, thanhTichCDRD, thongTinLeader2, thongTinLeader3) "
                    + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            ps.setInt(1, this.id);
            ps.setString(2, this.name);
            ps.setString(3, this.name2);
            ps.setString(4, this.slogan);
            ps.setInt(5, this.imgId);
            ps.setLong(6, this.powerPoint);
            ps.setByte(7, this.maxMember);
            ps.setInt(8, this.capsuleClan);
            ps.setInt(9, this.level);
            ps.setString(10, member);
            ps.setString(11, top);
            ps.setString(12, topBanDoKhoBau);
            ps.setString(13, thongTinLeader);
            ps.setString(14, topKhiGas);
            ps.setString(15, topConDuongRanDoc);
            ps.setString(16, thongTinLeader2);
            ps.setString(17, thongTinLeader3);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            Logger.logException(Clan.class, e, "Có lỗi khi insert clan vào db");
        } finally {
            try {
                ps.close();
            } catch (Exception e) {
            }
        }

    }

    public void update() {
        JSONArray dataArray = new JSONArray();
        JSONObject dataObject = new JSONObject();
        for (ClanMember cm : this.members) {
            dataObject.put("id", cm.id);
            dataObject.put("name", cm.name);
            dataObject.put("head", cm.head);
            dataObject.put("body", cm.body);
            dataObject.put("leg", cm.leg);
            dataObject.put("role", cm.role);
            dataObject.put("donate", cm.donate);
            dataObject.put("receive_donate", cm.receiveDonate);
            dataObject.put("member_point", cm.memberPoint);
            dataObject.put("clan_point", cm.clanPoint);
            dataObject.put("join_time", cm.joinTime);
            dataObject.put("ask_pea_time", cm.timeAskPea);
            dataObject.put("power", cm.powerPoint);
            dataArray.add(dataObject.toJSONString());
            dataObject.clear();
        }
        String member = dataArray.toJSONString();
        String topBanDoKhoBau = "[" + levelDoneBanDoKhoBau + "," + thoiGianHoanThanhBDKB + "]";
        String thongTinLeader = "[" + getLeader().id + "," + getLeader().name + "," + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        String topKhiGas = "[" + levelDoneKhiGas + "," + thoiGianHoanThanhKhiGas + "]";
        String topConDuongRanDoc = "[" + levelDoneConDuongRanDoc + "," + thoiGianHoanThanhCDRD + "]";
        String thongTinLeader2 = "[" + getLeader().id + "," + getLeader().name + "," + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        String thongTinLeader3 = "[" + getLeader().id + "," + getLeader().name + "," + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        dataArray.clear();

        PreparedStatement ps = null;
        try (Connection con = DBConnecter.getConnectionServer();) {
            ps = con.prepareStatement("update clan set slogan = ?, img_id = ?, power_point = ?, max_member = ?, clan_point = ?, "
                    + "level = ?, members = ?, name_2 = ?, tops = ?, thanhTichBDKB = ?, thongTinLeader = ?, thanhTichKhiGas = ?, thanhTichCDRD = ?, thongTinLeader2 = ?, thongTinLeader3 = ? where id = ? limit 1");
            ps.setString(1, this.slogan);
            ps.setInt(2, this.imgId);
            ps.setLong(3, this.powerPoint);
            ps.setByte(4, this.maxMember);
            ps.setInt(5, this.capsuleClan);
            ps.setInt(6, this.level);
            ps.setString(7, member);
            ps.setString(8, this.name2);
            ps.setString(9, "BARCOLL");
            ps.setString(10, topBanDoKhoBau);
            ps.setString(11, thongTinLeader);
            ps.setString(12, topKhiGas);
            ps.setString(13, topConDuongRanDoc);
            ps.setString(14, thongTinLeader2);
            ps.setString(15, thongTinLeader3);
            ps.setInt(16, this.id);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            Logger.logException(Clan.class, e, "Có lỗi khi insert clan vào db");
        } finally {
            try {
                ps.close();
            } catch (Exception e) {
            }
        }
    }

    public void updatethanhTichKG(int clanId) {
        String topKhiGas = "[" + levelDoneKhiGas + "," + thoiGianHoanThanhKhiGas + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE clan SET thanhTichKhiGas = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, topKhiGas);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updatethanhTichKG");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÀNH TÍCH BANG 2");
        }
    }

    public void updatethanhTichKGForLeader() {
        String topKhiGas = "[" + this.name + "," + this.levelDoneKhiGas + "," + thoiGianHoanThanhKhiGas + "," + System.currentTimeMillis() + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE player SET thanhTichBang2 = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, topKhiGas);
                ps.setInt(2, this.getLeader().id);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updatethanhTichKGForLeader");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÀNH TÍCH BANG 2");
        }
    }

    public void updatethanhTichBDKB(int clanId) {
        String topBDKB = "[" + levelDoneBanDoKhoBau + "," + thoiGianHoanThanhBDKB + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE clan SET thanhTichBDKB = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, topBDKB);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updatethanhTichBDKB");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÀNH TÍCH BANG");
        }
    }

    public void updatethanhTichBDKBForLeader() {
        String topBDKB = "[" + this.name + "," + this.levelDoneBanDoKhoBau + "," + thoiGianHoanThanhBDKB + "," + System.currentTimeMillis() + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE player SET thanhTichBang = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, topBDKB);
                ps.setInt(2, this.getLeader().id);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updatethanhTichBDKBForLeader");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÀNH TÍCH BANG");
        }
    }

    public void updatethanhTichCDRD(int clanId) {
        String topKhiGas = "[" + levelDoneConDuongRanDoc + "," + thoiGianHoanThanhCDRD + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE clan SET thanhTichCDRD = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, topKhiGas);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updatethanhTichCDRD");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE updatethanhTichCDRD");
        }
    }

    public void updatethanhTichCDRDForLeader() {
        String topKhiGas = "[" + this.name + "," + this.levelDoneConDuongRanDoc + "," + thoiGianHoanThanhCDRD + "," + System.currentTimeMillis() + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE player SET thanhTichBang3 = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, topKhiGas);
                ps.setInt(2, this.getLeader().id);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updatethanhTichCDRDForLeader");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÀNH TÍCH BANG 3");
        }
    }

    public void updateThongTinLeader(int clanId) {
        String thongTinLeader = "[" + getLeader().id + "," + getLeader().name + ","
                + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE clan SET thongTinLeader = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, thongTinLeader);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updateThongTinLeader");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÔNG TIN LEADER");
        }
    }

    public void updateThongTinLeader2(int clanId) {
        String thongTinLeader2 = "[" + getLeader().id + "," + getLeader().name + ","
                + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE clan SET thongTinLeader2 = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, thongTinLeader2);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updateThongTinLeader2");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÔNG TIN LEADER");
        }
    }

    public void updateThongTinLeader3(int clanId) {
        String thongTinLeader3 = "[" + getLeader().id + "," + getLeader().name + ","
                + getLeader().head + "," + getLeader().body + "," + getLeader().leg + "]";
        try (Connection con = DBConnecter.gI().getConnectionServer(); PreparedStatement ps = con != null ? con.prepareStatement(
                "UPDATE clan SET thongTinLeader3 = ? WHERE id = ? LIMIT 1"
        ) : null) {

            if (ps != null) {
                ps.setString(1, thongTinLeader3);
                ps.setInt(2, clanId);
                ps.executeUpdate();
            } else {
                Logger.error("Connection is null in updateThongTinLeader3");
            }

        } catch (Exception e) {
            Logger.logException(Clan.class, e, "ERROR KHI UPDATE THÔNG TIN LEADER");
        }
    }

    public void deleteDB(int id) {
        PreparedStatement ps;
        try (Connection con = DBConnecter.getConnectionServer();) {
            ps = con.prepareStatement("delete from clan where id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
            Logger.logException(Clan.class, e, "Có lỗi khi delete clan");
        }
    }

    public static void startAutoClanCheck() {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                for (Clan clan : Manager.CLANS) {
                    clan.checkClanStatus();
                }
            } catch (Exception e) {
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private static final java.util.concurrent.ScheduledExecutorService clanCheckExecutor
            = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();

    public static void startAutoCheck() {
        clanCheckExecutor.scheduleAtFixedRate(() -> {
            try {
                for (Clan clan : Manager.CLANS) { // nếu bạn lưu tất cả bang hội ở Manager
                    clan.checkClanStatus();
                }
            } catch (Exception e) {
            }
        }, 0, 1, java.util.concurrent.TimeUnit.MINUTES); // 1 phút 1 lần
    }

    // Thêm hàm kiểm tra riêng cho từng bang
    public void checkClanStatus() {
        try {
            // Kiểm tra số lượng thành viên thực tế
            if (this.members.size() > this.maxMember) {
            }
            // Kiểm tra trưởng bang còn tồn tại không
            ClanMember leader = getLeader();
            if (leader == null || NDVSqlFetcher.loadById(leader.id) == null) {
            }
            if (!this.active) {
            }

        } catch (Exception e) {
        }
    }
}
