/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boss.boss_manifest.Mini;

import boss.Boss;
import boss.BossData;
import boss.BossID;
import static boss.BossType.TRUNGTHU_EVENT;
import boss.BossesData;
import consts.ConstPlayer;
import map.ItemMap;
import nro.player.Player;
import nro.services.EffectSkillService;
import nro.services.ItemTimeService;
import nro.services.Service;
import nro.services.SkillService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

/**
 *
 * @author hoquo
 */
public class ThoDaiCa extends Boss {
    
    public ThoDaiCa() throws Exception {
        super(TRUNGTHU_EVENT, BossID.THODAICA, BossesData.THODAICA);
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 500) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.nPoint.dame = pl.nPoint.hpMax / Util.nextInt(30, 50);
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    if (!pl.itemTime.iscarot) {
                        pl.itemTime.iscarot = true;
                        pl.itemTime.lastTimecarot = System.currentTimeMillis();

                        // Cập nhật ngoại hình, hiệu ứng và chỉ số
                        Service.gI().chat(pl, "Huhuhu... Ta đã bị biến thành cũ cải!");
                        Service.gI().point(pl);
                        ItemTimeService.gI().sendAllItemTime(pl);
                        Service.gI().Send_Caitrang(pl);
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage = this.nPoint.subDameInjureWithDeff(damage / 7);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
            if (damage > 1000) {
                damage = 1000;
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

    @Override
    public void reward(Player plKill) {
        for (int i = 1; i <= 10; i++) {
            ItemMap item = new ItemMap(this.zone, 462, 1, this.location.x + i, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            if (item.itemTemplate != null) {
                Service.gI().dropItemMap(this.zone, item);
            }
        }
    }
}
