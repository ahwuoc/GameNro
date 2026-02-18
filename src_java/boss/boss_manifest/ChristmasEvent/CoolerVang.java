package boss.boss_manifest.ChristmasEvent;

/*
 * @Author: DienCoLamCoi
 * @Description: Điện Cơ Lâm Còi - Chuyên cung cấp thiết bị điện cơ uy tín chất lượng cao.
 * @Group Zalo: Giao lưu chia sẻ kinh nghiệm code - https://zalo.me/g/lsqfzx907
 */
import boss.Boss;
import boss.BossID;
import boss.BossStatus;
import boss.BossesData;
import item.Item.ItemOption;
import map.ItemMap;
import nro.player.Player;
import nro.services.Service;
import utils.Util;

public class CoolerVang extends Boss {

    private long st;

    public CoolerVang() throws Exception {
        super(BossID.COLLER_GOLD, false, true, BossesData.COLLER_VANG);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void reward(Player plKill) {
        if (Util.isTrue(1, 10)) {
            ItemMap it = new ItemMap(this.zone, 878, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), playerReward.id);
            it.options.add(new ItemOption(50, 23));
            it.options.add(new ItemOption(103, 19));
            it.options.add(new ItemOption(178, 3));
            it.options.add(new ItemOption(179, 0));
            it.options.add(new ItemOption(106, 0));
            it.options.add(new ItemOption(93, Util.nextInt(1, 7)));
            Service.gI().dropItemMap(this.zone, it);
        }
        if (Util.isTrue(1, 5)) {
            ItemMap it1 = new ItemMap(this.zone, 1206, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), playerReward.id);
            it1.options.add(new ItemOption(30, 0));
            Service.gI().dropItemMap(this.zone, it1);
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
//        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
//            st = System.currentTimeMillis();
//        }
    }
}
