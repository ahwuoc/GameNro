package boss.boss_manifest.Broly;

/*
 *
 *
 * 
 */
import boss.Boss;
import boss.BossData;
import boss.BossID;
import boss.BossStatus;
import static boss.BossType.BROLY;
import boss.BossesData;
import consts.ConstPlayer;
import java.util.logging.Level;
import java.util.logging.Logger;
import map.Zone;
import player.Player;
import services.Service;
import services.SkillService;
import services.func.ChangeMapService;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class Broly extends Boss {
private long realHpMax = 5000;
    public Broly() throws Exception {
         super(BROLY, BossID.BROLY, false, true, BossesData.BROLY);
    }

  
   
    @Override
public void joinMap() {
    this.name = "Broly " + Util.nextInt(10, 100);
    if (this.nPoint.hpMax < 1000) {
        this.nPoint.hpMax = 1000;
    }
    this.nPoint.dame = this.nPoint.hpMax / 100;
    this.nPoint.crit = Util.nextInt(50);
    this.joinMap2();
    st = System.currentTimeMillis();
}

    public void joinMap2() {
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone != null) {
            try {

                int zoneid = Util.nextInt(2, this.zone.map.zones.size() - 1);

                while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getBosses().size() > 0) {
                    zoneid++;
                }

                if (zoneid < this.zone.map.zones.size()) {
                    this.zone = this.zone.map.zones.get(zoneid);
                } else {
                    if (this.id == BossID.BROLY) {
                        this.changeStatus(BossStatus.DIE);
                        return;
                    }
                    this.zone = this.zone.map.zones.get(Util.nextInt(2, this.zone.map.zones.size() - 1));
                }

                if (this.zone.zoneId < 2) {
                    this.leaveMap();
                }

                ChangeMapService.gI().changeMap(this, this.zone, -1, -1);
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    private long st;

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (Util.isTrue(10, 30)) {
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
               realHpMax += realHpMax * (Util.nextInt(1, 30)) / 100L;
                           if (realHpMax > 16_777_777) {
                               realHpMax = 16_777_777;
                           }
                           this.nPoint.hpMax = realHpMax;      
                SkillService.gI().useSkill(this, null, null, -1, null);
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (!piercing && plAtt.playerSkill.skillSelect.template.id != Skill.TU_SAT && damage > this.nPoint.hpMax / 100) {
                damage = this.nPoint.hpMax / 100;
            }
            this.nPoint.subHP(damage);
            
            return damage;
        } else {
            return 0;
        }
    }

    private long lastTimeAttack;

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(7, this.playerSkill.skills.size() - 1));
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
                    if (Util.isTrue(10, 100)) {
                        this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                         realHpMax += realHpMax * (Util.nextInt(1, 30)) / 100L;
                           if (realHpMax > 16_777_777) {
                               realHpMax = 16_777_777;
                           }
                           this.nPoint.hpMax = realHpMax;                       
                    }

                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void die(Player plKill) {
        this.reward(plKill);
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss+=1;
        Zone zone = this.zone;
        int x = this.location.x;
        int y = this.location.y;
        if (this.nPoint.hpMax > 1_000_000) {
            try {
                SuperBuVip superBroly = new SuperBuVip(zone, x, y);
                System.out.println("Create Super suscess in map " + zone.map.mapName);
            } catch (Exception ex) {
                Logger.getLogger(Broly.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

//        super.reward(plKill);
    }

    

private void tangChiSo() {
    realHpMax += realHpMax * (Util.nextInt(1, 30)) / 100L;
    if (realHpMax > 16_777_777) {
        realHpMax = 16_777_777;
    }
    this.nPoint.hpMax = realHpMax;
   
}



    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }
}
