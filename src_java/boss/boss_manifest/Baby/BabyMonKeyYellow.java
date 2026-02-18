/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss.boss_manifest.Baby;

import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import static boss.BossType.BABY;
import boss.BossesData;
import item.Item;
import java.util.Random;
import map.ItemMap;
import nro.player.Player;
import nro.server.Manager;
import nro.services.EffectSkillService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class BabyMonKeyYellow extends Boss {

    private long st;

    public BabyMonKeyYellow() throws Exception {
        super(BABY, BossID.BABY_MONKEY, BossesData.BABY_MONKEY);
    }

    @Override
    public void reward(Player plKill) {
        Random rnd = new Random();
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

        // Cộng điểm cho người chơi
        plKill.effect.addPointTrumSanBoss();
        plKill.playerTask.kolTask.addCount();

        // Random index cho TL_GN và TL_AWJ
        byte randomDoGN = (byte) rnd.nextInt(Manager.itemIds_tl_GN.length);
        byte randomDoAWJ = (byte) rnd.nextInt(Manager.itemIds_tl_AWJ.length);

        // 5% khả năng rơi item đặc biệt (-1)
        if (Util.isTrue(5, 100)) {
            Service.gI().dropItemMap(zone, Util.ratiDTL(zone, -1, 1, x, y, plKill.id));
        } // 40% khả năng rơi 1 trong 2 item hiếm (1901, 1142)
        else if (Util.isTrue(40, 100)) {
            int[] rare1 = {1901, 1142};
            int chosenId = rare1[rnd.nextInt(rare1.length)];

            // Rơi 50 mảnh, mỗi mảnh quantity 5, riêng lẻ
            for (int i = 0; i < 50; i++) {
                Service.gI().dropItemMap(zone, Util.ratiDTL(zone, chosenId, 5, x, y, plKill.id));
            }
        } // 15% khả năng rơi item AWJ
        else if (Util.isTrue(15, 50)) {
            Service.gI().dropItemMap(zone, Util.ratiDTL(zone, Manager.itemIds_tl_AWJ[randomDoAWJ], 1, x, y, plKill.id));
        } // 15% khả năng rơi item 1734 với các options
        else if (Util.isTrue(15, 100)) {
            ItemMap it = new ItemMap(zone, 1734, 1, x, y, plKill.id);
            it.options.add(new Item.ItemOption(50, 28));
            it.options.add(new Item.ItemOption(77, 28));
            it.options.add(new Item.ItemOption(103, 28));
            it.options.add(new Item.ItemOption(94, 12));
            it.options.add(new Item.ItemOption(5, 12));
            it.options.add(new Item.ItemOption(204, 17));

            // Thả item ra map
            Service.gI().dropItemMap(zone, it);
        }
    }

    @Override
    public void joinMap() {
        st = System.currentTimeMillis();
        this.zone = this.parentBoss.zone;
        ChangeMapService.gI().changeMap(this, this.zone,
                this.parentBoss.location.x + Util.nextInt(-100, 100), this.parentBoss.location.y);
        Service.gI().sendFlagBag(this);
        this.notifyJoinMap();
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
//        BossManager.gI().removeBoss(this);
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {

        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage / 7);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 7;
            }

            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return (int) damage;
        } else {
            return 0;
        }
    }

}
