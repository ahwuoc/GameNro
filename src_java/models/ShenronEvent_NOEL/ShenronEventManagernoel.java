package models.ShenronEvent_NOEL;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import models.ShenronEvent_NOEL.*;
import utils.Functions;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import nro.server.Maintenance;

public class ShenronEventManagernoel implements Runnable {

    private static ShenronEventManagernoel instance;
    private long lastUpdate;
    private static final List<ShenronEventnoel> list = new ArrayList<>();

    ;

    public static ShenronEventManagernoel gI() {
        if (instance == null) {
            instance = new ShenronEventManagernoel();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long start = System.currentTimeMillis();
                update();
                long timeUpdate = System.currentTimeMillis() - start;
                Functions.sleep(Math.max(1000 - timeUpdate, 10));
            } catch (Exception ex) {
            }
        }
    }

    public void update() {
        if (Util.canDoWithTime(lastUpdate, 1000)) {
            lastUpdate = System.currentTimeMillis();
            List<ShenronEventnoel> listCopy = new ArrayList<>();
            for (ShenronEventnoel se : list) {
                listCopy.add(se);
            }

            for (ShenronEventnoel se : listCopy) {
                try {
                    se.update();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            listCopy.clear();
        }
    }

    public void add(ShenronEventnoel se) {
        list.add(se);
    }

    public void remove(ShenronEventnoel se) {
        list.remove(se);
    }

}
