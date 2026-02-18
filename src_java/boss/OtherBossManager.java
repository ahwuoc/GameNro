package boss;

import utils.Functions;
import nro.server.Maintenance;

public class OtherBossManager extends BossManager {

    private static OtherBossManager instance;

    public static OtherBossManager gI() {
        if (instance == null) {
            instance = new OtherBossManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            long start = System.currentTimeMillis();

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

            long elapsed = System.currentTimeMillis() - start;
            long sleep = Math.max(10, 150 - elapsed);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ignored) {
            }
        }
    }

}
