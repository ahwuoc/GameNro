package services.top;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import jdbc.DBConnecter;
import jdbc.daos.NDVSqlFetcher;
import lombok.Getter;
import nro.player.Player;
import nro.server.ServerManager;
import static services.top.TopManager.TOP_TEMPLATE;
import services.top.TopManager.TopTemplate;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;
import network.Message;

/**
 *
 * @author Ts
 */
public class TopService implements Runnable {

    private static TopService I;

    public static TopService gI() {
        if (I == null) {
            I = new TopService();
        }
        return I;
    }

    @Getter
    public static List<TOP> topMayDam;
    private static long timeRealTop = 0;

    public static final String TOP_MAYDAM
            = "SELECT player.id AS id, CAST(JSON_EXTRACT(loadtimetop, '$[0]') AS UNSIGNED) AS lasttime, "
            + "CAST(JSON_EXTRACT(loadtimetop, '$[1]') AS UNSIGNED) AS maydamtd FROM player LIMIT 100;";

    public TopTemplate getTop(Object value) {
        for (TopTemplate template : TOP_TEMPLATE) {
            try {
                int id = Integer.parseInt(value.toString());
                if (template.id == id) {
                    return template;
                }
            } catch (NumberFormatException e) {
                if (template.name.equals(value)) {
                    return template;
                }
            }
        }
        return null;
    }

    public static List<TOP> realTop(String query, Connection con) {
        List<TOP> tops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TOP t = TOP.builder().id_player(rs.getInt("id")).build();
                t.setLasttime(rs.getLong("lasttime"));
                switch (query) {
                    default -> {
                        String valKey = switch (query) {
                            case TOP_MAYDAM ->
                                "maydamtd";
                            default ->
                                null;
                        };
                        if (valKey != null) {
                            int d = rs.getInt(valKey);
                            t.setInfo1(String.valueOf(d));
                            t.setInfo2(String.valueOf(d));
                        }
                    }
                }
                tops.add(t);
            }
        } catch (Exception e) {
            Logger.logException(TopService.class, e);
        }
        return tops;
    }

    public static void showListTopTrung(Player player) {
        TopTrung.getInstance().load();
        List<Player> list = TopTrung.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top 100");
            msg.writer().writeByte(list.size());
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
//                msg.writer().writeUTF("Top Bóng Master: " + Util.numberToMoney(top.point_pokemon));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showListBuoi(Player player) {
        TopDauBuoi.getInstance().load();
        List<Player> list = TopDauBuoi.getInstance().getList();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Top Sự Kiện");
            msg.writer().writeByte(list.size());
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
//                msg.writer().writeUTF("Điểm săn boss sự kiện : " + Util.numberToMoney(top.daubuoi));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void showListTop(Player p, int type) {
        Message msg = new Message(-96);
        try {
            DataOutputStream w = msg.writer();
            w.writeByte(0);
            w.writeUTF("Top 100");
            List<TOP> tops;
            switch (type) {
                case 0, 1, 2 ->
                    tops = topMayDam;
                default -> {
                    return;
                }
            }
            if (tops == null || tops.isEmpty()) {
                return;
            }
            List<TOP> filt = new ArrayList<>();
            for (TOP t : tops) {
                Player pl = NDVSqlFetcher.loadById(t.getId_player());
                if (pl != null && t.getLasttime() != 0 && !(type <= 2 && pl.gender != type)) {
                    filt.add(t);
                }
            }
            w.writeByte(filt.size());
            int rank = 1;
            for (TOP t : filt) {
                Player pl = NDVSqlFetcher.loadById(t.getId_player());
                if (pl == null) {
                    continue;
                }
                w.writeInt(rank++);
                w.writeInt((int) pl.id);
                w.writeShort(pl.getHead());
                if (p.getSession().version > 214) {
                    w.writeShort(-1);
                }
                w.writeShort(pl.getBody());
                w.writeShort(pl.getLeg());
                w.writeUTF(pl.name);
                String line1 = (type == 3)
                        ? "LV:" + t.getLevel() + " với " + Util.roundToTwoDecimals(t.getTime() / 1000d) + " giây"
                        : TimeUtil.getTimeLefttop(t.getLasttime(), 0);
                w.writeUTF(line1);
                String line2 = (type == 3)
                        ? TimeUtil.getTimeLefttop(t.getLasttime(), 0)
                        : (type <= 2 ? t.getInfo2() : "...");
                w.writeUTF(line2);
            }
            p.sendMessage(msg);
        } catch (IOException ignored) {
        }
    }

    public List<TOP> getListTop(Object value) {
        for (TopTemplate template : TOP_TEMPLATE) {
            try {
                int id = Integer.parseInt(value.toString());
                if (template.id == id) {
                    return template.tops;
                }
            } catch (NumberFormatException e) {
                if (template.name.equals(value)) {
                    return template.tops;
                }
            }
        }
        return null;
    }

    public synchronized void loadListTop(Connection con) {
        try {
            for (TopTemplate temp : TOP_TEMPLATE) {
                loadTop(con, temp);
            }
        } catch (Exception e) {
            Logger.logException(getClass(), e);
        }
    }

    public synchronized void loadListTop() {
        try (Connection con = DBConnecter.getConnectionServer()) {
            for (TopTemplate temp : TOP_TEMPLATE) {
                loadTop(con, temp);
            }
        } catch (Exception e) {
            Logger.logException(getClass(), e);
        }
    }

    public synchronized void loadTop(Connection con, TopTemplate temp) {
        temp.tops.forEach(TOP::dispose);
        temp.tops.clear();
        try (PreparedStatement ps = con.prepareStatement(temp.query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TOP top = TOP.builder().build().findPlayer(con, rs.getInt("id"));
                switch (temp.id) {
                    case 0 ->
                        top.setInfo1(Util.powerToString(rs.getLong("tongnap")) + "$").setInfo2(Util.format(rs.getLong("tongnap")) + "\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                    case 1 ->
                        top.getPoint(rs).setInfo1(Util.powerToString(top.getPlayer().nPoint.power)).setInfo2(Util.format(top.getPlayer().nPoint.power) + "\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                    case 2 ->
                        top.getTask(rs).setInfo1(top.getPlayer().playerTask.taskMain.id + "\n" + top.getPlayer().playerTask.taskMain.name).setInfo2("(" + top.getPlayer().playerTask.taskMain.subTasks.get(top.getPlayer().playerTask.taskMain.index).name + ")\n|5|Hoàn thành " + Util.msToTime(top.getPlayer().playerTask.taskMain.lastTime) + " trước\n|7|Ngọc Rồng Online\n|3|Cập nhật: " + TimeUtil.getTimeNow("HH:mm"));
                }
                temp.tops.add(top);
            }
        } catch (Exception e) {
            Logger.logException(this.getClass(), e, "Lỗi Load Top: " + temp.name + " ID: " + temp.id);
        }
    }

    public int getPlayerRank(Player p, List<TOP> tops) {
        for (int i = 0; i < tops.size(); i++) {
            if (tops.get(i).getId_player() == p.id) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long lastTime = System.currentTimeMillis();
            if (timeRealTop + 5000 < lastTime) {
                timeRealTop = lastTime;
                try (Connection con = DBConnecter.getConnectionServer()) {
                    topMayDam = realTop(TOP_MAYDAM, con);
                } catch (Exception ignored) {
                }
                loadListTop();
            }
            if (Util.canDoWithTime(lastTime, 60000)) {
                loadListTop();
            }
        }
    }
}
