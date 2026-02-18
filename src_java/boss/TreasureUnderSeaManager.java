package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import utils.Functions;
import nro.server.Maintenance;

public class TreasureUnderSeaManager extends BossManager {

    private static TreasureUnderSeaManager instance;

    public static TreasureUnderSeaManager gI() {
        if (instance == null) {
            instance = new TreasureUnderSeaManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();

                for (int i = bosses.size() - 1; i >= 0; i--) {
                    if (i < bosses.size()) {
                        Boss boss = bosses.get(i);
                        try {
                            boss.update();
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                removeBoss(boss);
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

                long elapsed = System.currentTimeMillis() - st;
                long sleep = Math.max(10, 150 - elapsed);
                Thread.sleep(sleep);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
