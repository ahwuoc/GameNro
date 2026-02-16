package boss.boss_manifest.Earth;


import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import item.Item;
import java.util.List;
import map.ItemMap;
import player.Player;
import services.ItemService;
import services.Service;
import utils.Util;

public class KOGU extends Boss {

    private long st;

    public KOGU() throws Exception {
        super(BossID.KOGU, false, true, BossesData.KOGU);
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
        if(Util.isTrue(50,100)){
             
                ItemMap it = new ItemMap(this.zone, 457, (int) 1, this.location.x , this.zone.map.yPhysicInTop(this.location.x,
                        this.location.y - 24), plKill.id);
                
                Service.gI().dropItemMap(this.zone, it);
        }
        short itTemp = 424;
        ItemMap it = new ItemMap(zone, itTemp, 1, this.location.x + Util.nextInt(-50, 50), this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
        if (!ops.isEmpty()) {
            it.options = ops;
        }
        Service.gI().dropItemMap(this.zone, it);
         if (Util.isTrue(1, 2)) {
            ItemMap it_coin = new ItemMap(this.zone, 1788, 1, this.location.x + 5,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y), plKill.id);
            it_coin.source = "KOGU";
            Service.gI().dropItemMap(this.zone, it_coin);
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
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if ((boss.id == BossID.BUJIN || boss.id == BossID.ZANGYA || boss.id == BossID.BIDO) && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
