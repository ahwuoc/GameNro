package boss.boss_manifest.Rongnhi;

import boss.Boss;
import boss.BossID;
import static boss.BossType.RONG_1_SAO;
import boss.BossesData;
import consts.ConstPlayer;
import item.Item.ItemOption;
import map.ItemMap;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.Service;
import utils.Util;


public class RongNhi5Sao extends Boss {

    private long st;
    private int timeLeaveMap;

    public RongNhi5Sao() throws Exception {
        super(RONG_1_SAO,BossID.RONG_5_SAO, true, false, BossesData.RONG_5_SAO);
    }

    @Override
    public void reward(Player plKill) {
        plKill.effect.addPointTrumSanBoss();
        ItemMap nhan = new ItemMap(this.zone, 1822, Util.nextInt(1,3), this.location.x + Util.nextInt(-15, 15),
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        nhan.options.add(new ItemOption(30, 0));
        Service.gI().dropItemMap(this.zone, nhan);
    }

    @Override
    public long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (this.currentLevel != 0) {
                damage /= 2;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage - Util.nextInt(100000));
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            if (damage > 25000) {
                damage = 25000;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeaveMap)) {
            if (Util.isTrue(1, 2)) {
                this.leaveMap();
            } else {
                this.leaveMapNew();
            }
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
            timeLeaveMap = Util.nextInt(300000, 900000);
        }
    }

    @Override
    public void joinMap() {
        this.name = this.data[this.currentLevel].getName() + " " + Util.nextInt(1, 100);
        super.joinMap();
        st = System.currentTimeMillis();
        timeLeaveMap = Util.nextInt(600000, 900000);
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills
                        .get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                }
            } catch (Exception ex) {
//                 ex.printStackTrace();
            }
        }
    }
}
