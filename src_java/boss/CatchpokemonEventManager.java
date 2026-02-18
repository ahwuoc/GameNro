package boss;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

public class CatchpokemonEventManager extends BossManager {

    private static CatchpokemonEventManager instance;

    public static CatchpokemonEventManager gI() {
        if (instance == null) {
            instance = new CatchpokemonEventManager();
        }
        return instance;
    }

}
