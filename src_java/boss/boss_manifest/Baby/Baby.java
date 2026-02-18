package boss.boss_manifest.Baby;

import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import static boss.BossType.BABY;
import boss.BossesData;
import consts.ConstPlayer;
import item.Item;
import java.util.Random;
import map.ItemMap;
import nro.player.Player;
import nro.server.Manager;
import nro.services.EffectSkillService;
import nro.services.PlayerService;
import nro.services.Service;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class Baby extends Boss {

    public boolean callBaby;

    public Baby() throws Exception {
        super(BABY, BossID.BABY, BossesData.BABY, BossesData.BABY_VEGETA);
    }

    @Override
    protected void resetBase() {
        super.resetBase();
        this.callBaby = false;
    }

    public void callBaby() {
        try {
            this.changeStatus(BossStatus.AFK);
            this.changeToTypeNonPK();
            this.recoverHP();
            this.callBaby = true;
            for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
                if (boss.id == BossID.BABY_MONKEY) {
                    boss.changeStatus(BossStatus.RESPAWN);
                }
            }
            this.setDie(this);
            this.die(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void recoverHP() {
        PlayerService.gI().hoiPhuc(this, this.nPoint.hpMax, 0);
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
            Service.gI().dropItemMap(
                    zone,
                    Util.ratiDTL(zone, -1, 1, x, y, plKill.id)
            );
        } // 40% khả năng rơi 1 trong 2 item hiếm (1901, 1142)
        else if (Util.isTrue(40, 100)) {
            int[] rare1 = {1901, 1142};
            int chosenId = rare1[rnd.nextInt(rare1.length)];

            // Rơi 50 mảnh, mỗi mảnh quantity 5, riêng lẻ
            for (int i = 0; i < 50; i++) {
                Service.gI().dropItemMap(
                        zone,
                        Util.ratiDTL(zone, chosenId, 5, x, y, plKill.id)
                );
            }
        } // 30% khả năng rơi item AWJ
        else if (Util.isTrue(15, 50)) {
            Service.gI().dropItemMap(
                    zone,
                    Util.ratiDTL(zone, Manager.itemIds_tl_AWJ[randomDoAWJ], 1, x, y, plKill.id)
            );
        } // 15% khả năng rơi item 1733 với options
        else if (Util.isTrue(15, 100)) {
            ItemMap it = new ItemMap(zone, 1733, 1, x, y, plKill.id);
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
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.callBaby && this.currentLevel == 1 && damage >= this.nPoint.hp && Util.isTrue(50, 100)) {
            this.callBaby();
            return 0;
        }
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
