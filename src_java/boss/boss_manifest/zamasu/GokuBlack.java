package boss.boss_manifest.zamasu;

/*
 *
 *
 *
 */
import boss.Boss;
import boss.BossID;
import boss.BossManager;
import boss.BossStatus;
import boss.BossesData;
import item.Item;
import java.util.List;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import utils.Util;

public class GokuBlack extends Boss {

    private long st;

    public GokuBlack() throws Exception {
        super(BossID.GOKUBLACK, false, true, BossesData.GOKUBLACK);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss+=1;
        int [] pet = {1568,1596,1597,1629,1630,1631};//id danh sach vp
        if(Util.isTrue(20,100)){  //tyr le roi vat pham 20%
                  
                ItemMap it = new ItemMap(this.zone, pet[Util.nextInt(pet.length)], 1, this.location.x + 5, this.zone.map.yPhysicInTop(this.location.x,
                      this.location.y - 24), plKill.id);//random 1 trong nhung id tren
                      it.options.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                      it.options.add(new Item.ItemOption(77, Util.nextInt(5, 15)));//oprion tu chinh vd 77 la hp dang de rando 20-40%
                      it.options.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                      it.options.add(new Item.ItemOption(14, Util.nextInt(3, 7)));
                      if (Util.isTrue(90, 100)) {//tyr le ra hsd 
                           it.options.add(new Item.ItemOption(93, Util.nextInt(1,7)));///93 la hsd 1-7 ngay
                      }
                Service.gI().dropItemMap(this.zone, it);
            }
      
    
    }

    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 600000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if ((boss.id == BossID.ZAMASU) && !boss.isDie()) {
                return;
            }
        }

        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }
}
