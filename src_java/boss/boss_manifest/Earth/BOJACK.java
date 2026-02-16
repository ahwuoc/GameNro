package boss.boss_manifest.Earth;

/*
 *
 *
 * 
 */

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

public class BOJACK extends Boss {

    private long st;

    public BOJACK() throws Exception {
        super(BossID.BOJACK, false, true, BossesData.BOJACK, BossesData.SUPER_BOJACK);
    }

    @Override
    public void reward(Player plKill) {
        plKill.pointboss+=1;
        if(Util.isTrue(50,100)){
             
                ItemMap it = new ItemMap(this.zone, 457, (int) 1, this.location.x , this.zone.map.yPhysicInTop(this.location.x,
                        this.location.y - 24), plKill.id);
                
                Service.gI().dropItemMap(this.zone, it);
        }        
        short itTemp = 427;
        ItemMap it = new ItemMap(zone, itTemp, 1, this.location.x + Util.nextInt(-50, 50), this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
        if (!ops.isEmpty()) {
            it.options = ops;
        }
        Service.gI().dropItemMap(this.zone, it);
         if (Util.isTrue(1, 2)) {
            ItemMap it_coin = new ItemMap(this.zone, 1788, 1, this.location.x + 5,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y), plKill.id);
            it_coin.source = "BOJACK";
            Service.gI().dropItemMap(this.zone, it_coin);
        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
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
    public void doneChatS() {
        if (this.currentLevel == 1) {
            return;
        }
        this.changeStatus(BossStatus.AFK);
    }
}
