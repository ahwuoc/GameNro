package boss.boss_manifest.Black;

/*
 *
 *
 * 
 */
import boss.*;
import static boss.BossesData.KUKU;
import consts.ConstPlayer;
import consts.ConstTask;
import consts.ConstTaskBadges;
import map.ItemMap;
import player.Player;
import server.Manager;
import services.*;
import utils.Util;

import java.util.Random;
import mob.CallMob;
import mob.MobMe;
import network.Message;
import task.Badges.BadgesTaskService;


public class BlackGoku extends Boss {

    private long st;
    private int timeLeaveMap;

    public BlackGoku() throws Exception {
        super(BossID.BLACK_GOKU, false, true, BossesData.BLACK_GOKU, BossesData.SUPER_BLACK_GOKU);
     
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss+=12;
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.TRUM_SAN_BOSS, 1);
        if (TaskService.gI().getIdTask(plKill) == ConstTask.TASK_31_0) {
            Service.gI().dropItemMap(this.zone, new ItemMap(zone, 992, 1, this.location.x, this.location.y, plKill.id));
             TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        }
        
        if (Util.isTrue(20, 100)) {
            ItemMap it = ItemService.gI().randDoTL(this.zone, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        }
        if (Util.isTrue(1, 50)) {
            for (int i = 0; i < Util.nextInt(1, 2); i++) {
                ItemMap it = new ItemMap(this.zone, 674, 1, this.location.x + Util.nextInt(-15, 15), this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            }
        }
      
    }
    private boolean check;
    private void CallMob(){
        if (this.callmob != null) {
                    this.callmob.mobMeDie();
                    this.callmob.dispose();
                    this.callmob = null;
                }
                this.callmob = new CallMob(this,6);
                check = true;
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
      if(Util.isTrue(20,100)){
          
          CallMob();
           this.chat("Triệu Hồi Quái Vật!!!");
             try {
            Message msg = new Message(-117);
            msg.writer().writeByte(101);
            this.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
      }
        
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (this.currentLevel != 0) {
                damage /= 1;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage - Util.nextInt(100000));
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
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
            timeLeaveMap = Util.nextInt(600000, 600000);
        }
    }

    @Override
    public void joinMap() {
        this.name = this.data[this.currentLevel].getName() + " " + Util.nextInt(1, 100);
        super.joinMap();
        st = System.currentTimeMillis();
        timeLeaveMap = Util.nextInt(600000, 600000);
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
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                 if(check){
                      Service.gI().sendTitle(pl,28);
                 }
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                } else {
                    if (Util.isTrue(30, 100)) {
                        int move = Util.nextInt(50);
                        move(pl.location.x + (Util.nextInt(0, 1) == 1 ? move : -move), this.location.y);
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
