package boss.boss_manifest.TauHuyDiet;

/**
 *
 * @author Administrator
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

public class ThanThienSu3 extends Boss {

    private long st;
    
    public ThanThienSu3() throws Exception {
        super(BossID.THAN_THIEN_SU3, BossesData.THAN_THIEN_SU3);
    }
    
    @Override
    public void reward(Player plKill) {
        if (!Util.isTrue(100, 100)) {
            return;
        }

        // ===== DANH SÁCH ĐỒ HỦY DIỆT XAYDA =====
        int[] doHD_Xayda = {654, 655, 661, 662, 658, 660};

        // Random item
        int id = doHD_Xayda[Util.nextInt(doHD_Xayda.length)];

        ItemMap itemDrop = new ItemMap(
                this.zone,
                id,
                1,
                this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                plKill.id
        );

        // ===== GÁN OPTION THEO TYPE =====
        switch (id) {

            // Áo Xayda
            case 654:
                itemDrop.options.add(new Item.ItemOption(47, Util.nextInt(1800, 2800)));
                break;

            // Quần Xayda
            case 655:
                itemDrop.options.add(new Item.ItemOption(22, Util.nextInt(85, 100)));
                break;

            // Găng Xayda
            case 661:
            case 662:
                itemDrop.options.add(new Item.ItemOption(0, Util.nextInt(8500, 11000)));
                break;

            // Giày Xayda
            case 658:
            case 660:
                itemDrop.options.add(new Item.ItemOption(23, Util.nextInt(59, 82)));
                break;
        }

        // Option chung
        itemDrop.options.add(new Item.ItemOption(21, 80)); // yêu cầu sm 80 tỷ
        itemDrop.options.add(new Item.ItemOption(30, 1));  // không thể giao dịch

        // Drop item ra map
        Service.gI().dropItemMap(this.zone, itemDrop);
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
        }
//        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
//            if (boss.id == BossID.THAN_HUY_DIET && !boss.isDie()) {
//                super.active();
//                break;
//            }
//        }
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }
}

