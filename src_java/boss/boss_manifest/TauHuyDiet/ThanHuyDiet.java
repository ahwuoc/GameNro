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

public class ThanHuyDiet extends Boss {

    private long st;
    
    public ThanHuyDiet() throws Exception {
        super(BossID.THAN_HUY_DIET, BossesData.THAN_HUY_DIET);
    }

    @Override
    public void reward(Player plKill) {
        if (Util.isTrue(100, 100)) {

            // ===== DANH SÁCH ĐỒ TRÁI ĐẤT =====
            int[] doHD_TraiDat = {650, 651, 657, 658, 656};

            // Random item Trái Đất
            int id = doHD_TraiDat[Util.nextInt(doHD_TraiDat.length)];

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

                case 650: // Áo
                    itemDrop.options.add(new Item.ItemOption(47, Util.nextInt(1200, 2100)));
                    break;

                case 651: // Quần
                    itemDrop.options.add(new Item.ItemOption(22, Util.nextInt(60, 80)));
                    break;

                case 657: // Găng
                    itemDrop.options.add(new Item.ItemOption(0, Util.nextInt(8500, 11000)));
                    break;

                case 658: // Giày
                    itemDrop.options.add(new Item.ItemOption(23, Util.nextInt(59, 82)));
                    break;

                case 656: // Nhẫn
                    itemDrop.options.add(new Item.ItemOption(14, Util.nextInt(5, 18)));
                    break;
            }

            // Option chung
            itemDrop.options.add(new Item.ItemOption(21, 80)); // yêu cầu SM 80 tỷ
            itemDrop.options.add(new Item.ItemOption(30, 1));  // không thể giao dịch

            // Drop item
            Service.gI().dropItemMap(this.zone, itemDrop);
        }
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
