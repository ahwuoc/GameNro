package boss.boss_manifest.zamasu;

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

public class Zamasu extends Boss {

    private long st;

    public Zamasu() throws Exception {
        super(BossID.ZAMASU, false, true, BossesData.ZAMASU);
    }

     @Override
    public void reward(Player plKill) {
        plKill.pointboss+=1;
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

   
}
