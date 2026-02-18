package models.DeathOrAliveArena;

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
import lombok.NonNull;
import map.Zone;
import nro.server.Maintenance;
import utils.Util;

public class DeathOrAliveArenaManager implements Runnable {

    private static DeathOrAliveArenaManager instance;
    private volatile long lastUpdate;
    private static final List<DeathOrAliveArena> list = new ArrayList<>();

    public static DeathOrAliveArenaManager gI() {
        if (instance == null) {
            instance = new DeathOrAliveArenaManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long start = System.currentTimeMillis();

                update();

                long elapsed = System.currentTimeMillis() - start;
                long sleepTime = Math.max(1000 - elapsed, 10); // ít nhất 10ms
                Thread.sleep(sleepTime);

            } catch (Exception ignored) {
            }
        }
    }

    public void update() {
        if (Util.canDoWithTime(lastUpdate, 1000)) {
            lastUpdate = System.currentTimeMillis();
            for (int i = list.size() - 1; i >= 0; i--) {
                if (i < list.size()) {
                    list.get(i).update();
                }
            }
        }
    }

    public void add(DeathOrAliveArena vdst) {
        list.add(vdst);
    }

    public void remove(DeathOrAliveArena vdst) {
        list.remove(vdst);
    }

    public DeathOrAliveArena getVDST(@NonNull Zone zone) {
        for (DeathOrAliveArena vdst : list) {
            if (vdst.getZone().equals(zone)) {
                return vdst;
            }
        }
        return null;
    }
}
