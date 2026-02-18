package nro.models.npc.npc_manifest;

/*
 * @Zalo : 0372.665.345
 */
import consts.ConstNpc;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.Service;
import services.top.TopAutoService;
import services.top.TopManager;
import services.top.TopService;

public class DaiThienSu2 extends Npc {

    public DaiThienSu2(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (player.zone.map.mapId == 7 || player.zone.map.mapId == 14 || player.zone.map.mapId == 0) {
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|2|Người mún gì ở em?",
                        "Bảng\nXếp Hạng", "Danh Sách\nQuà Top\nMùa Này");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU -> {
                    switch (select) {
                        case 0 ->
                            createOtherMenu(player, ConstNpc.IGNORE_MENU, "|2|BXH Đua Top Ngọc Rồng.", TopManager.SELECT);
                        case 1 ->
                            createOtherMenu(player, ConstNpc.REWARD_TOP, "|2|BXH Đua Top Ngọc Rồng.", TopManager.SELECT);
                    }
                }
                case ConstNpc.IGNORE_MENU -> { 
                    nro.services.Service.gI().showListTop(player, TopService.gI().getTop(select));
                }
                case ConstNpc.REWARD_TOP ->
                    Service.gI().sendThongBaoFromAdmin(player, TopAutoService.gI().getTop(select).info);
            }
        }
    }
}
