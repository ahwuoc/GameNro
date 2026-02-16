package boss.boss_manifest.BacConSoi;


import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import item.Item;
import map.ItemMap;
import network.Message;
import player.Player;
import services.EffectSkillService;
import services.InventoryService;
import services.Service;
import services.SkillService;
import services.TaskService;
import utils.Util;

public class BaConSoi extends Boss {

    private long st;

    public BaConSoi() throws Exception {
        super(BossID.BACONSOi, BossesData.SOI_BASIL, BossesData.SOI_LAVENDER, BossesData.SOI_Bergamo);
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss+=5;
         plKill.event.addEventPointBHM(1);
        
        Service.gI().sendThongBao(plKill, "Bạn đã Đã tiêu diệt được " + this.name + " và nhận 1 điểm Bà Hạt Mít");
//        for (int i = 0; i < Util.nextInt(1,2); i++) {
//                
//                ItemMap it = new ItemMap(this.zone, 457, (int) 1, this.location.x + i * 10, this.zone.map.yPhysicInTop(this.location.x,
//                        this.location.y - 24), plKill.id);
//                
//                Service.gI().dropItemMap(this.zone, it);
//            }
            plKill.pointtet+=1;
            ItemMap it = new ItemMap(this.zone, 191, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, it);
        
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void joinMap() {
        super.joinMap(); //To change body of generated methods, choose Tools | Templates.
        st = System.currentTimeMillis();
    }
 @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
      
        if (!this.isDie()) {
            
            
            if (this.currentLevel != 0) {
                damage /= 1;
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
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

}
