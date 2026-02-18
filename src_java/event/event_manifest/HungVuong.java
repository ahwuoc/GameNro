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

public class HungVuong extends Event {

    @Override
    public void boss() {
        createBoss(BossID.THUY_TINH, 10);
        this.createBoss(BossID.RONG_1_SAO,5);
        this.createBoss(BossID.RONG_2_SAO,5);
        this.createBoss(BossID.RONG_3_SAO,5);
        this.createBoss(BossID.RONG_4_SAO,5);
        this.createBoss(BossID.RONG_5_SAO,5);
        this.createBoss(BossID.RONG_6_SAO,5);
        this.createBoss(BossID.RONG_7_SAO,5);
    }
}
