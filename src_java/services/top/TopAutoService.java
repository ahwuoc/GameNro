package services.top;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jdbc.DBConnecter;
import nro.player.Player;
import static services.top.TopManager.AUTO_GIFT_TOPS;
import static services.top.TopManager.TOP_TEMPLATE;
import utils.Logger;

public class TopAutoService {

    private static final TopAutoService instance = new TopAutoService();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

    public static TopAutoService gI() {
        return instance;
    }

    private TopAutoService() {
    }

    public void reload() {
        if (AUTO_GIFT_TOPS != null) {
            AUTO_GIFT_TOPS.forEach(TOPAUTO::dispose);
            AUTO_GIFT_TOPS.clear();
        }

        String sql = "SELECT * FROM top_template";
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                try {
                    AUTO_GIFT_TOPS.add(new TOPAUTO(rs, TOP_TEMPLATE.get(rs.getInt("id"))));
                } catch (Exception e) {
                    Logger.logException(TopAutoService.class, e);
                }
            }
        } catch (Exception e) {
            Logger.logException(TopAutoService.class, e);
        }
    }

    public TOPAUTO getTop(Object value) {
        return AUTO_GIFT_TOPS.stream()
                .filter(top -> {
                    try {
                        return top.template.id == Integer.parseInt(value.toString());
                    } catch (NumberFormatException e) {
                        return top.template.name.equals(value);
                    }
                })
                .findFirst()
                .orElse(null);
    }

    public boolean checkTopRunning() {
        return AUTO_GIFT_TOPS.stream().anyMatch(top -> top.isSend);
    }

    public void activeAuto() {
        for (TOPAUTO top : AUTO_GIFT_TOPS) {
            System.out.println("[Auto Gift] Started: " + top.template.name);
            long delay = Math.max(0, top.date.getTime() - System.currentTimeMillis());
            scheduler.schedule(() -> processAutoGift(top), delay, TimeUnit.MILLISECONDS);
        }
    }

    private void processAutoGift(TOPAUTO top) {
        if (!top.isToDate() || !top.isCanGift()) {
            return;
        }

        try (Connection con = DBConnecter.getConnectionServer()) {
            TopService.gI().loadTop(con, top.template);
            Thread.sleep(5000);

            if (checkTopRunning()) {
                Thread.sleep(5500);
            }

            System.err.println("Auto Trao Qua Top " + top.template.name + " Started.........");
            top.isSend = true;

            try {
                int limit = Math.min(top.limit, top.template.tops.size());
                for (int i = 0; i < limit; i++) {
                    try {
                        Player pl = top.template.tops.get(i).getPlayer();
                        if (pl != null && top.isNonReceive((int) pl.id)) {
                            top.addItemAutoGift(con, i + 1, pl);
                        }
                    } catch (Exception e) {
                        Logger.logException(getClass(), e);
                    }
                    Thread.sleep(500);
                }
            } finally {
                System.err.println("Auto Trao Qua Top " + top.template.name + " Completed.........");
                top.isSend = false;
                top.update();
            }

        } catch (Exception e) {
            Logger.logException(getClass(), e, "Lỗi AutoTop: " + top.template.name + " ID: " + top.template.id);
        }
    }
}