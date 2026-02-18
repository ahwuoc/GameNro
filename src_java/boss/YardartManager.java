package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public class YardartManager extends BossManager {

    private static YardartManager instance;

    public static YardartManager gI() {
        if (instance == null) {
            instance = new YardartManager();
        }
        return instance;
    }

}
