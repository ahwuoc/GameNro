package services.func;

import Top.TopPowerManager;
import Top.TopTaskManager;
import consts.ConstSQL;

import java.io.IOException;

import jdbc.DBConnecter;
import player.Player;
import server.Manager;
import network.Message;
import utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import jdbc.daos.NDVSqlFetcher;
import matches.TOP;

import services.ItemService;
import utils.Util;

public class TopService {

    private static TopService instance;

    public static TopService gI() {
        if (instance == null) {
            instance = new TopService();
        }
        return instance;
    }

    public void updateTop(int select) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            switch (select) {
                case 1 -> Manager.topDC = Manager.realTop(ConstSQL.TOP_DC, con);
                case 2 -> Manager.topSM = Manager.realTop(ConstSQL.TOP_SM, con);
                case 3 -> Manager.topWHIS = Manager.realTop(ConstSQL.TOP_WHIS, con);
                case 4 -> Manager.topNap = Manager.realTop(ConstSQL.TOP_NAP, con);
                case 5 -> Manager.topVDST = Manager.realTop(ConstSQL.TOP_VDST, con);
                case 7 -> Manager.topSSM = Manager.realTop(ConstSQL.TOP_SO_SU_MENH, con);
                case 8 -> Manager.topSD = Manager.realTop(ConstSQL.TOP_SD, con);
                case 9 -> Manager.topTet = Manager.realTop(ConstSQL.TOP_TET, con);
                case 10 -> Manager.topbossday = Manager.realTop(ConstSQL.TOP_BOSS_DAY, con);
                case 11 -> Manager.topChienLuc = Manager.realTop(ConstSQL.TOP_CHIEN_LUC, con);
                case 12 -> Manager.topKI = Manager.realTop(ConstSQL.TOP_KI, con);
                default -> Manager.topNV = Manager.realTop(ConstSQL.TOP_NV, con);
            }
        } catch (Exception ignored) {
            Logger.error("Lỗi đọc top");
        }
    }

    public static void showListTopPower(Player player) {
        TopPowerManager.getInstance().load();
        List<Player> list = TopPowerManager.getInstance().getList();
        list.sort((p1, p2) -> Long.compare(p2.nPoint.power, p1.nPoint.power));
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 10");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < 10; i++) {
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
                msg.writer().writeUTF("Sức mạnh: " + Util.numberFormatLouis(top.nPoint.power));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
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
                msg.writer().writeUTF(NDVSqlFetcher.loadById(top.id).playerTask.taskMain.name);
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public static String getTopNap() {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DBConnecter.getConnectionServer();
                PreparedStatement ps = conn.prepareStatement(ConstSQL.TOP_DUA_NAP)) {
            conn.setAutoCommit(false);

            try (ResultSet rs = ps.executeQuery()) {
                byte i = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    long danap = rs.getLong("danap"); // Lấy số tiền nạp dưới dạng long
                    String formattedDanap = String.format(Manager.formatNumber(danap)); // Format số tiền

                    sb.append(i).append(". ").append(name).append(": ")
                            .append(formattedDanap).append(" Đã Nạp\n");
                    i++;
                }
            } // Đóng ResultSet
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getTopSM() {
        StringBuilder sb = new StringBuilder();

        try (Connection conn = DBConnecter.getConnectionServer();
                PreparedStatement ps = conn.prepareStatement(ConstSQL.TOP_DUA_SM)) {
            conn.setAutoCommit(false);

            try (ResultSet rs = ps.executeQuery()) {
                byte i = 1;
                while (rs.next()) {
                    String name = rs.getString("name");
                    long sm = rs.getLong("sm");
                    String formattedSM = String.format(Manager.formatNumber(sm));

                    sb.append(i).append(". ").append(name).append(": ")
                            .append(formattedSM).append(" Sức Mạnh\n");
                    i++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static int getTopBangRankByName(String playerName) {
        int rank = 99;
        try (
                Connection conn = DBConnecter.getConnectionServer();
                PreparedStatement ps = conn.prepareStatement(ConstSQL.TOP_BANG);
                ResultSet rs = ps.executeQuery()) {
            int position = 1;
            while (rs.next()) {
                String name = rs.getString("name");
                if (name.equalsIgnoreCase(playerName)) {
                    rank = position;
                    break;
                }
                position++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rank;
    }

    public static String getTopBang() {
        StringBuilder sb = new StringBuilder();
        PreparedStatement ps = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = DBConnecter.getConnectionServer();
            ps = conn.prepareStatement(ConstSQL.TOP_BANG);
            conn.setAutoCommit(false);

            rs = ps.executeQuery();
            byte i = 1;
            while (rs.next()) {
                String name = rs.getString("name");
                int sm = rs.getInt("Point"); // Lấy sức mạnh dưới dạng số nguyên
                String formattedSM = String.format(Manager.formatNumber(sm)); // Định dạng số

                sb.append(i).append(". ").append(name).append(": ")
                        .append(formattedSM).append(" Điểm\n");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static String getTopQuocVuong() {
        StringBuffer sb = new StringBuffer("");
        try (Connection conn = DBConnecter.getConnectionServer();
                PreparedStatement ps = conn.prepareStatement(ConstSQL.TOP_DUA_QUOC_VUONG)) {
            conn.setAutoCommit(false);
            try (ResultSet rs = ps.executeQuery()) {
                byte i = 1;
                while (rs.next()) {
                    int id = rs.getInt("accountId");
                    String username = rs.getString("name");
                    sb.append(i).append(".").append(id).append("-").append(username).append(": sở hữu ")
                            .append(rs.getString("thoi_vang")).append(" ")
                            .append(ItemService.gI().getTemplate(consts.ConstTranhNgocNamek.ITEM_TRANH_NGOC).name)
                            .append("\b");
                    i++;
                }
            } // Đóng ResultSet
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    public static void showListTop(Player player, int select) {
        TopService.gI().updateTop(select);
        List<TOP> tops = Manager.topNV;
        switch (select) {
            case 1 ->
                tops = Manager.topDC;
            case 2 ->
                tops = Manager.topSM;
            case 3 ->
                tops = Manager.topWHIS;
            case 4 ->
                tops = Manager.topNap;
            case 5 ->
                tops = Manager.topVDST;
            case 7 ->
                tops = Manager.topSSM;
            case 8 ->
                tops = Manager.topSD;
            case 9 ->
                tops = Manager.topTet;
            case 10 ->
                tops = Manager.topbossday;
            case 11 ->
                tops = Manager.topChienLuc;
            case 12 ->
                tops = Manager.topKI;

        }
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 10");
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
                    case 1 -> {
                        msg.writer().writeUTF("Chơi đồ " + top.getDicanh() + " lần");
                        msg.writer().writeUTF("Gia nhập juventus " + top.getJuventus() + " lần");
                    }
                    case 2 -> {
                        msg.writer().writeUTF("" + Util.numberToMoney(top.getPower()) + " Sức mạnh");
                        msg.writer().writeUTF("" + top.getPower() + " Sức mạnh");
                    }
                    case 3 -> {
                        msg.writer().writeUTF("LV:" + top.getLevel() + " với "
                                + Util.roundToTwoDecimals(top.getTime() / 1000d) + " giây");
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
                    // case 6 -> {
                    // msg.writer().writeUTF("" + Util.numberToMoney(top.getCash()) + " VNĐ");
                    // msg.writer().writeUTF("" + top.getCash() + " VNĐ");
                    // }
                    case 7 -> {
                        msg.writer().writeUTF("Số điểm sổ sứ mệnh" + top.getDiemsm());
                        // msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                        msg.writer().writeUTF("" + top.getDiemsm() + " Điểm");
                    }
                    case 8 -> {
                        String sd = String.format(Manager.formatNumber(top.getSd()));
                        msg.writer().writeUTF("Sức Đánh : " + sd);
                        // msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                        msg.writer().writeUTF("" + sd + "  SĐ");
                    }
                    case 9 -> {
                        msg.writer().writeUTF("Điểm Sự Kiện: " + top.getDiemtet());
                        // msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                        msg.writer().writeUTF("" + top.getDiemtet() + "  Điểm");
                    }
                    case 10 -> {
                        msg.writer().writeUTF("Điểm boss Ngày:  " + top.getBossday());
                        // msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                        msg.writer().writeUTF("" + top.getBossday() + "  điểm");
                    }
                    case 11 -> {
                        String lc = String.format(Manager.formatNumber(top.getChienluc()));
                        msg.writer().writeUTF("Lực Chiến:  " + lc);
                        // msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                        msg.writer().writeUTF("" + lc);
                    }
                    case 12 -> {
                        msg.writer().writeUTF("KI:  " + top.getKi());
                        // msg.writer().writeUTF(getTimeLeft(top.getLasttime()));
                        msg.writer().writeUTF("" + top.getKi() + " KI");
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
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
