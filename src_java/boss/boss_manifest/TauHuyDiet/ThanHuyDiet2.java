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

public class ThanHuyDiet2 extends Boss {

    private long st;
    
    public ThanHuyDiet2() throws Exception {
        super(BossID.THAN_HUY_DIET2, BossesData.THAN_HUY_DIET2);
    }

    @Override
    public void reward(Player plKill) {
        if (!Util.isTrue(100, 100)) {
            return;
        }

        // ===== DANH SÁCH ĐỒ HỦY DIỆT NAMEC =====
        int[] doHD_Namec = {652, 653, 659, 660, 661, 662};

        // Random item Namec
        int id = doHD_Namec[Util.nextInt(doHD_Namec.length)];

        ItemMap itemDrop = new ItemMap(
                this.zone,
                id,
                1,
                this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                plKill.id
        );

        // ===== GÁN OPTION THEO LOẠI =====
        switch (id) {

            case 652: // Áo Namec
            case 653: // Áo kiểu Namec
                itemDrop.options.add(new Item.ItemOption(47, Util.nextInt(1800, 2800)));
                break;

            case 659: // Quần Namec
            case 660: // Quần Namec
                itemDrop.options.add(new Item.ItemOption(22, Util.nextInt(85, 100)));
                break;

            case 661: // Găng Namec
            case 662: // Găng Namec
                itemDrop.options.add(new Item.ItemOption(0, Util.nextInt(8500, 10000)));
                break;
        }

        // Option chung
        itemDrop.options.add(new Item.ItemOption(21, 80)); // yêu cầu sm 80 tỷ
        itemDrop.options.add(new Item.ItemOption(30, 1));  // không thể giao dịch

        // Drop item
        Service.gI().dropItemMap(this.zone, itemDrop);
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
