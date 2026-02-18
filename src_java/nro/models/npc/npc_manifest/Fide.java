package nro.models.npc.npc_manifest;

/**
 *
 * Box ZALO: https://zalo.me/g/ifjict764 SĐT ZALO: 0358176187 Chuyên chỉnh sửa,
 * mua bán source NRO,...
 */
import consts.ConstNpc;
import consts.ConstTranhNgocNamek;
import models.DragonNamecWar.TranhNgocService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.NpcService;
import nro.services.Service;
import nro.services.TaskService;
import services.func.ChangeMapService;
import shop.ShopService;
import utils.Util;

public class Fide extends Npc {

    public Fide(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        TaskService.gI().checkDoneTaskTalkNpc(player, this);

        if (mapId == 5) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|0| Ta bán đồ cho người giàu, người có tiền không đó ???\n|4|"
                    + "Số tiền ngươi đang có: " + player.getSession().cash + " VND",
                    "Cửa hàng", "Từ chối");

        } else if (mapId == ConstTranhNgocNamek.MAP_ID) {
            if (player.iDMark.getTranhNgoc() == 1) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Phắn đê! Ta không nói chuyện với sinh vật hạ đẳng", "Đóng");
                return;
            }
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Hãy mang ngọc rồng về cho ta", "Đưa ngọc", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.getIndexMenu() != ConstNpc.BASE_MENU) {
            return;
        }

        if (this.mapId == 5) {
            switch (select) {
                case 0 ->
                    ShopService.gI().opendShop(player, "SHOP_VND", false);
                default -> {
                }
            }

        } else if (this.mapId == ConstTranhNgocNamek.MAP_ID) {
            switch (select) {
                case 0 -> {
                    if (player.iDMark.getTranhNgoc() == 2 && player.isHoldNamecBallTranhDoat) {
                        if (!Util.canDoWithTime(player.lastTimePickItem, 20000)) {
                            long waitTime = (player.lastTimePickItem + 20000 - System.currentTimeMillis()) / 1000;
                            Service.gI().sendThongBao(player, "Vui lòng đợi " + waitTime + " giây để có thể trả");
                            return;
                        }
                        TranhNgocService.getInstance().dropBall(player, (byte) 2);
                        player.zone.pointRed++;
                        if (player.zone.pointRed > ConstTranhNgocNamek.MAX_POINT) {
                            player.zone.pointRed = ConstTranhNgocNamek.MAX_POINT;
                        }
                        TranhNgocService.getInstance().sendUpdatePoint(player);
                    }
                }
                default -> {
                }
            }
        }
    }
}
