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

public class SkillSummonedManager extends BossManager {

    private static SkillSummonedManager instance;

    public static SkillSummonedManager gI() {
        if (instance == null) {
            instance = new SkillSummonedManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();

                for (int i = this.bosses.size() - 1; i >= 0; i--) {
                    if (i < this.bosses.size()) {
                        Boss boss = this.bosses.get(i);
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
                long sleep = Math.max(10, 500 - elapsed); // giữ chu kỳ 500ms
                Thread.sleep(sleep);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
