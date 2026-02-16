package npc.npc_manifest;

import consts.ConstNpc;
import consts.ConstTask;
import item.Item;
import npc.Npc;
import player.Player;
import services.*;
import shop.ShopService;
import utils.Util;

public class BulmaTuongLai extends Npc {

    public BulmaTuongLai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                if (TaskService.gI().getIdTask(player) >= ConstTask.TASK_29_1) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Cám ơn bạn đã đến đây giúp chúng tôi",
                            "Kể chuyện", "Cửa hàng");
                } else {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU, "Cám ơn bạn đã đến đây giúp chúng tôi",
                            "Kể chuyện");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {

            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, this.tempId, this.avartar, ConstNpc.CALICK_KE_CHUYEN);
                    case 1 -> ShopService.gI().opendShop(player, "BUNMA_FUTURE", true);
                    default -> {
                    }
                }
            }
        }
    }
}
