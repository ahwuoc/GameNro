package boss.boss_manifest.TauHuyDiet;

/**
 *
 * @author HBT
 */

import boss.Boss;
import boss.BossID;
import boss.BossesData;
import item.Item;
import map.ItemMap;
import utils.Util;

import java.util.Random;
import nro.player.Player;
import nro.services.Service;

public class JackyChun extends Boss {
    
    private long st;

    public JackyChun() throws Exception {
        super(BossID.JACKY_CHUN2, BossesData.JACKY_CHUN2);
    }

    @Override
    public void reward(Player plKill) {
        if (Util.isTrue(100, 100)) {
            ItemMap caitrangjacky = new ItemMap(this.zone, 711, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            caitrangjacky.options.add(new Item.ItemOption(50, 23));
            caitrangjacky.options.add(new Item.ItemOption(77, 21));
            caitrangjacky.options.add(new Item.ItemOption(103, 21));
            caitrangjacky.options.add(new Item.ItemOption(159, 4));
            caitrangjacky.options.add(new Item.ItemOption(160, 50));
            caitrangjacky.options.add(new Item.ItemOption(93,  new Random().nextInt(3) + 4));
            Service.gI().dropItemMap(this.zone, caitrangjacky);
        }if (Util.isTrue(5, 100)) {
            ItemMap caitrangjacky2 = new ItemMap(this.zone, 711, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            caitrangjacky2.options.add(new Item.ItemOption(50, 23));
            caitrangjacky2.options.add(new Item.ItemOption(77, 21));
            caitrangjacky2.options.add(new Item.ItemOption(103, 21));
            caitrangjacky2.options.add(new Item.ItemOption(160, 50));
            Service.gI().dropItemMap(this.zone, caitrangjacky2);
        }if (Util.isTrue(100, 100)) {
            ItemMap ngocrong3s = new ItemMap(this.zone, 16, 1, this.location.x - 20, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
//            ngocrong3s.options.add(new Item.ItemOption(30, 1));
//            ngocrong3s.options.add(new Item.ItemOption(86, 1));
            Service.gI().dropItemMap(this.zone, ngocrong3s);
        }if (Util.isTrue(15, 100)) {
            ItemMap ngocrong2s = new ItemMap(this.zone, 15, 1, this.location.x - 40, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
//            ngocrong2s.options.add(new Item.ItemOption(30, 1));
//            ngocrong2s.options.add(new Item.ItemOption(86, 1));
            Service.gI().dropItemMap(this.zone, ngocrong2s);
        }if (Util.isTrue(5, 100)) {
            ItemMap ngocrong1s = new ItemMap(this.zone, 14, 1, this.location.x - 80, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
//            ngocrong1s.options.add(new Item.ItemOption(30, 1));
//            ngocrong1s.options.add(new Item.ItemOption(86, 1));
            Service.gI().dropItemMap(this.zone, ngocrong1s);
        }if (Util.isTrue(100, 100)) {
            ItemMap vang = new ItemMap(this.zone, 190, 30000, this.location.x + 20, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, vang);
        }if (Util.isTrue(100, 100)) {
            ItemMap vang2 = new ItemMap(this.zone, 190, 30000, this.location.x + 40, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, vang2);
        }if (Util.isTrue(100, 100)) {
            ItemMap vang3 = new ItemMap(this.zone, 190, 30000, this.location.x + 60, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, vang3);
        }if (Util.isTrue(100, 100)) {
            ItemMap vang4 = new ItemMap(this.zone, 190, 30000, this.location.x + 80, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, vang4);
        }
    }

    @Override
    public void active() {
        super.active(); //To change body of generated methods, choose Tools | Templates.
//        if (Util.canDoWithTime(st, 1500)) {
//            this.changeStatus(BossStatus.LEAVE_MAP);
//        }
    }


//    @Override
//    public void doneChatS() {
//        if (this.bossAppearTogether == null || this.bossAppearTogether[this.currentLevel] == null) {
//            return;
//        }
//        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
//            if(boss.id == BossID.POC && !boss.isDie()){
//                boss.changeToTypePK();
//                break;
//            }
//        }
//    }
    

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }
}
