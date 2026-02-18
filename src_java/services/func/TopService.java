package services.func;

import Top.*;
import clan.Clan;
import consts.ConstSQL;
import java.io.IOException;
import jdbc.DBConnecter;
import nro.player.Player;
import nro.server.Manager;
import network.Message;
import utils.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import jdbc.NDVDB;
import jdbc.daos.NDVSqlFetcher;
import matches.TOP;
import nro.services.ItemService;
import nro.services.TaskService;
import utils.Util;

public class TopService {

    private static TopService instance;

    public static TopService gI() {
        if (instance == null) {
            instance = new TopService();
        }
        return instance;
    }

    public void updateTop() {
        if (Manager.timeRealTop + (10 * 60 * 1000) < System.currentTimeMillis()) {
            Manager.timeRealTop = System.currentTimeMillis();
            try (Connection con = DBConnecter.getConnectionServer()) {
                Manager.topNV = Manager.realTop(ConstSQL.TOP_NV, con);
                Manager.topDC = Manager.realTop(ConstSQL.TOP_DC, con);
                Manager.topVDST = Manager.realTop(ConstSQL.TOP_VDST, con);
                Manager.topWHIS = Manager.realTop(ConstSQL.TOP_WHIS, con);
                Manager.topSM = Manager.realTop(ConstSQL.TOP_SM, con);
                Manager.Topmaydam = Manager.realTop(ConstSQL.queryTopmaydam, con);
                Manager.topNap = Manager.realTop(ConstSQL.TOP_NAP, con);
                Manager.topDuaSM = Manager.realTop(ConstSQL.TOP_DUA_SM, con);
                Manager.topDuaNap = Manager.realTop(ConstSQL.TOP_DUA_NAP, con);
            } catch (Exception ignored) {
                Logger.error("Lỗi đọc top");
            }
        }
    }

    // --- HELPER METHOD: Xử lý logic chung cho các Top Player ---
    private static void sendCommonTopPlayer(Player player, List<Player> list, Function<Player, String> infoGenerator) {
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long currentTime = System.currentTimeMillis();

            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1); // top
                msg.writer().writeInt(i + 1); // rank
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);

                // Tối ưu hóa xử lý ngày tháng
                try {
                    String inputDateString = top.firstTimeLogin.toString();
                    Date inputDate = dateFormat.parse(inputDateString);
                    long timeDifferenceInMillis = currentTime - inputDate.getTime();
                    long giay = timeDifferenceInMillis / 1000;
                    msg.writer().writeUTF(Util.convertSecondsToTime(giay));
                } catch (ParseException e) {
                    msg.writer().writeUTF(""); // Hoặc xử lý mặc định
                }

                // Ghi thông tin đặc thù của từng loại top
                msg.writer().writeUTF(infoGenerator.apply(top));
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    // --- Các hàm Show List Top gọi về Helper ---

    public static void showListTopVnd(Player player) {
        TopVnd.getInstance().load();
        sendCommonTopPlayer(player, TopVnd.getInstance().getList(), 
            top -> "Tổng nạp: " + Util.numberFormatLouis(top.danap));
    }

    public static void showListTopbongmaster(Player player) {
        Topbongmaster.getInstance().load();
        sendCommonTopPlayer(player, Topbongmaster.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.bongmaster));
    }

    public static void showListTophopquathang9vip(Player player) {
        Tophopquathang9vip.getInstance().load();
        sendCommonTopPlayer(player, Tophopquathang9vip.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hopquathang9vip));
    }

    public static void showListTophopquathang9(Player player) {
        Tophopquathang9.getInstance().load();
        sendCommonTopPlayer(player, Tophopquathang9.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hopquathang9));
    }

    public static void showListTophopquatrungthuvip(Player player) {
        Tophopquatrungthuvip.getInstance().load();
        sendCommonTopPlayer(player, Tophopquatrungthuvip.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hopquatrungthuvip));
    }

    public static void showListToplongdentreo(Player player) {
        Toplongdentreo.getInstance().load();
        sendCommonTopPlayer(player, Toplongdentreo.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.longdentreo));
    }

    public static void showListTophoptrahoacuc(Player player) {
        Tophoptrahoacuc.getInstance().load();
        sendCommonTopPlayer(player, Tophoptrahoacuc.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hoptrahoacuc));
    }

    public static void showListTophopkeomaquy(Player player) {
        Tophopkeomaquy.getInstance().load();
        sendCommonTopPlayer(player, Tophopkeomaquy.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hopkeomaquy));
    }

    public static void showListTopthiep_halloween(Player player) {
        Topthiep_halloween.getInstance().load();
        sendCommonTopPlayer(player, Topthiep_halloween.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.thiep_halloween));
    }

    public static void showListTophopdiem(Player player) {
        Tophopdiem.getInstance().load();
        sendCommonTopPlayer(player, Tophopdiem.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hopdiem));
    }

    public static void showListTopvongquayvang(Player player) {
        Topvongquayvang.getInstance().load();
        sendCommonTopPlayer(player, Topvongquayvang.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.vongquayvang));
    }

    public static void showListTopvongquaydacbiet(Player player) {
        Topvongquaydacbiet.getInstance().load();
        sendCommonTopPlayer(player, Topvongquaydacbiet.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.vongquaydacbiet));
    }

    public static void showListTopphaobong(Player player) {
        Topphaobong.getInstance().load();
        sendCommonTopPlayer(player, Topphaobong.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.phaobong));
    }

    public static void showListToplixi(Player player) {
        Toplixi.getInstance().load();
        sendCommonTopPlayer(player, Toplixi.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.lixi));
    }

    public static void showListTopkeo_halloween(Player player) {
        Topkeo_halloween.getInstance().load();
        sendCommonTopPlayer(player, Topkeo_halloween.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.keo_halloween));
    }

    public static void showListTophalloween_master(Player player) {
        Tophalloween_master.getInstance().load();
        sendCommonTopPlayer(player, Tophalloween_master.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.halloween_master));
    }

    public static void showListTopcapsuvip(Player player) {
        Topcapsuvip.getInstance().load();
        sendCommonTopPlayer(player, Topcapsuvip.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.capsuvip));
    }

    public static void showListTopthiepchucvip(Player player) {
        Topthiepchucvip.getInstance().load();
        sendCommonTopPlayer(player, Topthiepchucvip.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.thiepchucvip));
    }

    public static void showListTophopqua2010(Player player) {
        Tophopqua2010.getInstance().load();
        sendCommonTopPlayer(player, Tophopqua2010.getInstance().getList(), 
            top -> "Điểm: " + Util.numberFormatLouis(top.hopqua2010));
    }

    public static void showListTopPower(Player player) {
        TopPowerManager.getInstance().load();
        List<Player> list = TopPowerManager.getInstance().getList();
        list.sort((p1, p2) -> Long.compare(p2.nPoint.power, p1.nPoint.power));
        if (list.size() > 100) {
            list = list.subList(0, 100);
        }
        sendCommonTopPlayer(player, list, 
            top -> "Sức mạnh: " + Util.numberFormatLouis(top.nPoint.power));
    }

    public void showTopClanKhoBau(Player player) {
        TopKhoBau.getInstance().load();
        List<Clan> list = TopKhoBau.getInstance().getList();
        sendCommonClanList(player, list, "BDKB");
    }

    public void showTopClanCDRD(Player player) {
        TopCDRD.getInstance().load();
        List<Clan> list = TopCDRD.getInstance().getList();
        sendCommonClanList(player, list, "CDRD");
    }

    public void showTopClanKhiGas(Player player) {
        TopKhiGas.getInstance().load();
        List<Clan> list = TopKhiGas.getInstance().getList();
        sendCommonClanList(player, list, "KhiGas");
    }

    private void sendCommonClanList(Player player, List<Clan> list, String type) {
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Clan clan = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt((int) clan.id);
                msg.writer().writeShort(player.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(player.getBody());
                msg.writer().writeShort(player.getLeg());
                msg.writer().writeUTF(clan.name);

                if (type.equals("BDKB")) {
                    msg.writer().writeUTF("Lv: " + clan.levelDoneBanDoKhoBau + " Trong " + Util.convertMillisecondsToSeconds(clan.thoiGianHoanThanhBDKB) + " giây");
                } else if (type.equals("CDRD")) {
                    msg.writer().writeUTF("Lv: " + clan.levelDoneConDuongRanDoc + " Trong " + Util.convertMillisecondsToSeconds(clan.thoiGianHoanThanhCDRD) + " giây");
                } else if (type.equals("KhiGas")) {
                    msg.writer().writeUTF("Lv: " + clan.levelDoneKhiGas + " Trong " + Util.convertMillisecondsToSeconds(clan.thoiGianHoanThanhKhiGas) + " giây");
                }

                msg.writer().writeUTF("Bang chủ " + clan.getLeader().name);
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    // --- Các hàm My Clan Top ---

    public void showMyTopClanKhoBau(Player player) {
        sendCommonMyClanTop(player, 
            () -> {
                MyClanTopKhoBau.getInstance().load(player.clan.getLeader().id);
                return MyClanTopKhoBau.getInstance().getList();
            }, "BDKB");
    }

    public void showMyTopClanCDRD(Player player) {
        sendCommonMyClanTop(player, 
            () -> {
                MyClanTopCDRD.getInstance().load(player.clan.getLeader().id);
                return MyClanTopCDRD.getInstance().getList();
            }, "CDRD");
    }

    public void showMyTopClanKhiGas(Player player) {
        sendCommonMyClanTop(player, 
            () -> {
                MyClanTopKhiGas.getInstance().load(player.clan.getLeader().id);
                return MyClanTopKhiGas.getInstance().getList();
            }, "KhiGas");
    }

    private void sendCommonMyClanTop(Player player, java.util.function.Supplier<List<Player>> listSupplier, String type) {
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Thành tích bang");
            
            if (player.clan != null) {
                List<Player> list = listSupplier.get();
                msg.writer().writeByte(list.size());
                for (int i = 0; i < list.size(); i++) {
                    Player pl = list.get(i);
                    msg.writer().writeInt(i + 1);
                    msg.writer().writeInt((int) pl.id);
                    msg.writer().writeShort(pl.getHead());
                    if (player.getSession().version >= 214) {
                        msg.writer().writeShort(-1);
                    }
                    msg.writer().writeShort(pl.getBody());
                    msg.writer().writeShort(pl.getLeg());
                    msg.writer().writeUTF(pl.nameClan);
                    
                    if (type.equals("BDKB")) {
                         msg.writer().writeUTF("Lv: " + pl.levelBDKBDone + " (" + Util.convertSecondsToTime(pl.lastTimeUpdateTopBDKB) + ")");
                         msg.writer().writeUTF("Bang chủ: " + pl.name + "\n[" + Util.convertMilliseconds(pl.timeBDKBDone) + "]");
                    } else if (type.equals("CDRD")) {
                         msg.writer().writeUTF("Lv: " + pl.levelCDRDDone + " (" + Util.convertSecondsToTime(pl.lastTimeUpdateTopCDRD) + ")");
                         msg.writer().writeUTF("Bang chủ: " + pl.name + "\n[" + Util.convertMilliseconds(pl.timeCDRDDone) + "]");
                    } else if (type.equals("KhiGas")) {
                         msg.writer().writeUTF("Lv: " + pl.levelKhiGasDone + " (" + Util.convertSecondsToTime(pl.lastTimeUpdateTopKhiGas) + ")");
                         msg.writer().writeUTF("Bang chủ: " + pl.name + "\n[" + Util.convertMilliseconds(pl.timeKhiGasDone) + "]");
                    }
                }
            } else {
                msg.writer().writeByte(0);
                msg.writer().writeInt(0);
                msg.writer().writeInt(0);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeShort(-1);
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");
                msg.writer().writeUTF("Chưa có");
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) msg.cleanup();
        }
    }

    // --- Các hàm Filter theo Hệ (Trái Đất, Namếc, Xayda) ---

    public void showListTopTraiDat(Player player, List<TOP> tops) {
        showListTopRace(player, tops, 0);
    }

    public void showListTopNamek(Player player, List<TOP> tops) {
        showListTopRace(player, tops, 1);
    }

    public void showListTopXayda(Player player, List<TOP> tops) {
        showListTopRace(player, tops, 2);
    }

    private void showListTopRace(Player player, List<TOP> tops, int gender) {
        Message msg = null;
        try {
            List<TOP> filteredTops = new ArrayList<>();
            for (TOP top : tops) {
                Player pl = NDVSqlFetcher.loadById(top.getId_player());
                if (pl != null && pl.gender == gender) {
                    filteredTops.add(top);
                }
            }

            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Bảng Xếp Hạng");
            msg.writer().writeByte(filteredTops.size());
            for (int i = 0; i < filteredTops.size(); i++) {
                TOP top = filteredTops.get(i);
                Player pl = NDVSqlFetcher.loadById(top.getId_player());
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt((int) pl.id);
                msg.writer().writeShort(pl.getHead());
                if (player.getSession().version > 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(pl.getBody());
                msg.writer().writeShort(pl.getLeg());
                msg.writer().writeUTF(pl.name);
                msg.writer().writeUTF(top.getInfo1());
                msg.writer().writeUTF(top.getInfo2());
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
             if (msg != null) msg.cleanup();
        }
    }

    public static void showListTopTask(Player player) {
        TopTaskManager.getInstance().load();
        List<Player> list = TopTaskManager.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Player top = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.name);
                
                // Logic lấy tên nhiệm vụ
                Player taskPl = NDVSqlFetcher.loadById(top.id);
                String taskName = (taskPl != null && taskPl.playerTask != null && taskPl.playerTask.taskMain != null) 
                        ? taskPl.playerTask.taskMain.name : "Không rõ";
                        
                msg.writer().writeUTF(taskName);
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static String getTopNap() {
        StringBuilder sb = new StringBuilder();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_DUA_NAP);
            conn.setAutoCommit(false);
            rs = ps.executeQuery();
            byte i = 1;
            while (rs.next()) {
                sb.append(i).append(".").append(rs.getString("name"))
                  .append(": ").append(rs.getString("danap")).append(" Đã Nạp\b");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception ex) {}
        }
        return sb.toString();
    }

    public static String getTopSM() {
        StringBuilder sb = new StringBuilder();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_DUA_SM);
            conn.setAutoCommit(false);
            rs = ps.executeQuery();
            byte i = 1;
            while (rs.next()) {
                sb.append(i).append(".").append(rs.getString("name"))
                  .append(": ").append(rs.getString("sm")).append(" Sức Mạnh\b");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
             try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception ex) {}
        }
        return sb.toString();
    }

    public static String getTopQuocVuong() {
        StringBuilder sb = new StringBuilder();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_DUA_QUOC_VUONG);
            conn.setAutoCommit(false);
            rs = ps.executeQuery();
            byte i = 1;
            String itemName = ItemService.gI().getTemplate(consts.ConstTranhNgocNamek.ITEM_TRANH_NGOC).name;
            while (rs.next()) {
                int id = rs.getInt("accountId");
                String username = rs.getString("name");
                sb.append(i).append(".").append(id).append("-").append(username)
                  .append(": sở hữu ").append(rs.getString("thoi_vang"))
                  .append(" ").append(itemName).append("\b");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
             try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (Exception ex) {}
        }
        return sb.toString();
    }

    public static void showListTop(Player player, int select) {
        List<TOP> tops = Manager.topNV;
        switch (select) {
            case 0 -> tops = Manager.topNV;
            case 1 -> tops = Manager.topDC;
            case 2 -> tops = Manager.topSM;
            case 3 -> tops = Manager.topWHIS;
            // case 4 -> tops = Manager.topNap;
            case 4 -> tops = Manager.topVDST;
            case 5 -> tops = Manager.topDuaSM;
            // case 6 -> tops = Manager.topDuaNap;
        }
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(tops.size());
            for (int i = 0; i < tops.size(); i++) {
                TOP top = tops.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(i + 1);
                msg.writer().writeShort(top.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(top.getBody());
                msg.writer().writeShort(top.getLeg());
                msg.writer().writeUTF(top.getName());
                
                switch (select) {
                    case 0 -> {
                        String taskName = TaskService.gI().getTaskMainById(player, top.getNv()).name;
                        String subTaskName = TaskService.gI().getTaskMainById(player, top.getNv()).subTasks.get(top.getSubnv()).name;
                        msg.writer().writeUTF(taskName.substring(0, Math.min(taskName.length(), 20)) + "...");
                        msg.writer().writeUTF(subTaskName + " - " + getTimeLeft(top.getLasttime()));
                    }
                    case 1 -> {
                        msg.writer().writeUTF("Chơi đồ " + top.getDicanh() + " lần");
                        msg.writer().writeUTF("Gia nhập juventus " + top.getJuventus() + " lần");
                    }
                    case 2 -> {
                        msg.writer().writeUTF("" + Util.numberToMoney(top.getPower()) + " Sức mạnh");
                        msg.writer().writeUTF("" + top.getPower() + " Sức mạnh");
                    }
                    case 3 -> {
                        msg.writer().writeUTF("LV:" + top.getLevel() + " với " + Util.roundToTwoDecimals(top.getTime() / 1000d) + " giây");
                        msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                    }
                    case 4 -> {
                        msg.writer().writeUTF("" + Util.numberToMoney(top.getCash()) + " VNĐ");
                        msg.writer().writeUTF("" + top.getCash() + " VNĐ");
                    }
                    case 5 -> {
                        msg.writer().writeUTF("Đã thử thách " + top.getDivdst() + " Lần");
                        msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                    }
                }
            }
            player.sendMessage(msg);
        } catch (IOException e) {
            // Ignored
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public static String getTimeLeft(long lastTime) {
        int secondsPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);

        if (secondsPassed > 86400) {
            return (secondsPassed / 86400) + " ngày trước";
        } else if (secondsPassed > 3600) {
            return (secondsPassed / 3600) + " giờ trước";
        } else if (secondsPassed > 60) {
            return (secondsPassed / 60) + " phút trước";
        } else {
            return secondsPassed + " giây trước";
        }
    }
}