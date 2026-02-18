package boss.boss_manifest.Mini;

import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import map.ItemMap;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

public class ubu extends Boss {

    private long st;

    // --- CẤU HÌNH DROP ITEM TẠI ĐÂY ---
    private int itemID = 1520;      // ID vật phẩm muốn rơi
    private int itemQuantity = 1;   // Số lượng rơi
    private int dropRate = 10;      // Tỉ lệ rơi (10 = 10%, 100 = 100%)
    // ----------------------------------

    public ubu() throws Exception {
        // BossID.UBU và BossesData.UBU phải được khai báo như hướng dẫn của bạn
        super(BossID.UBU, false, true, BossesData.UBU);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void reward(Player plKill) {
        // Logic rơi vật phẩm theo tỉ lệ
        if (Util.isTrue(dropRate, 100)) {
            ItemMap item = new ItemMap(this.zone, itemID, itemQuantity, 
                    this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            if (item.itemTemplate != null) {
                Service.gI().dropItemMap(this.zone, item);
            }
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) { // 15 phút tự rời
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }
}