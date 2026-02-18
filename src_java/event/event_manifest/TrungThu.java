package event.event_manifest;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import boss.BossID;
import event.Event;

public class TrungThu extends Event {

    @Override
    public void boss() {
        createBoss(BossID.NGUYETTHAN, 20);
        createBoss(BossID.THODAICA, 20);
        createBoss(BossID.MEOWTH, 20);
        //nồi bánh
        createNpc(0, 66, 850, 432);
        createNpc(7, 66, 507, 432);
        createNpc(14, 66, 276, 408);
        createNpc(5, 66, 1136, 408);
        //cây treo đèn
        createNpc(0, 85, 956, 432);
        createNpc(7, 85, 855, 432);
        createNpc(14, 85, 956, 408);
        //trung thu
        createNpc(0, 84, 1087, 432);
        createNpc(7, 84, 1067, 432);
        createNpc(14, 84, 1089, 408);
        //thỏ đại ca
        createNpc(5, 69, 315, 288);
        //chichi
        createNpc(5, 82, 231, 288);
    }
}
