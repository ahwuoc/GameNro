package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import utils.Functions;
import java.util.ArrayList;
import java.util.List;
import nro.server.Maintenance;

public class GasDestroyManager extends BossManager {

    private static GasDestroyManager instance;

    public static GasDestroyManager gI() {
        if (instance == null) {
            instance = new GasDestroyManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            long start = System.currentTimeMillis();
            List<Boss> toRemove = new ArrayList<>();

            for (int i = this.bosses.size() - 1; i >= 0; i--) {
                try {
                    this.bosses.get(i).update();
                } catch (Exception e) {
                    e.printStackTrace();
                    toRemove.add(this.bosses.get(i));
                }
            }
            for (Boss b : toRemove) {
                try {
                    removeBoss(b);
                } catch (Exception ignored) {
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            long sleep = Math.max(10, 150 - elapsed);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
