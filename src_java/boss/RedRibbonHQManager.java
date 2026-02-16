package boss;

/*
 *
 *
 * 
 */

import DucPro.Functions;
import server.Maintenance;

public class RedRibbonHQManager extends BossManager {

    private static RedRibbonHQManager instance;

    public static RedRibbonHQManager gI() {
        if (instance == null) {
            instance = new RedRibbonHQManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();
                for (int i = this.ListBoss.size() - 1; i >= 0; i--) {
                    if (i < this.ListBoss.size()) {
                        Boss boss = this.ListBoss.get(i);
                        try {
                            boss.update();
                        } catch (Exception e) {
                            e.printStackTrace();
                            try {
                                removeBoss(boss);
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
                // if (500 - (System.currentTimeMillis() - st) > 0) {
                // Thread.sleep(500 - (System.currentTimeMillis() - st));
                // }
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - st), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
