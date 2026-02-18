package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import map.Map;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.MapService;
import nro.services.Service;
import services.func.EffectMapService;
import utils.Util;

public class HaiHoaHong extends Npc {

    private final byte COUNT_CHANGE = 7;
    private int count;

    public HaiHoaHong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    private void haiHoaHong(Player player) {
        // 🔹 Kiểm tra kéo trong túi
        Item keoCatHoa = InventoryService.gI().findItemBag(player, 1387);        // Kéo tỉa hoa thường
        Item keoCatHoaDacBiet = InventoryService.gI().findItemBag(player, 1382); // Kéo tỉa hoa đặc biệt

        // 🔹 Không có cả 2 loại kéo => không thể hái
        if ((keoCatHoa == null || keoCatHoa.quantity < 1) && (keoCatHoaDacBiet == null || keoCatHoaDacBiet.quantity < 1)) {
            Service.gI().sendThongBao(player, "Bạn cần có 'Kéo tỉa hoa' hoặc 'Kéo tỉa hoa đặc biệt' để hái hoa!");
            return;
        }

        // 🔹 Kiểm tra ô trống hành trang
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBaoOK(player, "Cần ít nhất 1 ô trống trong hành trang!");
            return;
        }

        Item hoaNhanDuoc;

        // 🔹 Nếu dùng kéo đặc biệt → hoa đặc biệt
        if (keoCatHoaDacBiet != null && keoCatHoaDacBiet.quantity > 0) {
            hoaNhanDuoc = ItemService.gI().createNewItem((short) 1388); // Hoa đặc biệt
            hoaNhanDuoc.quantity = Util.nextInt(1, 3);
            InventoryService.gI().subQuantityItemsBag(player, keoCatHoaDacBiet, 1);
        } // 🔹 Nếu dùng kéo thường → hoa thường
        else {
            hoaNhanDuoc = ItemService.gI().createNewItem((short) 1530); // Hoa thường
            hoaNhanDuoc.quantity = Util.nextInt(1, 5);
            InventoryService.gI().subQuantityItemsBag(player, keoCatHoa, 1);
        }

        // 🔹 Cập nhật hành trang sau khi trừ kéo
        InventoryService.gI().sendItemBag(player);

        // 🔹 Thêm hoa vào túi
        InventoryService.gI().addItemBag(player, hoaNhanDuoc);
        InventoryService.gI().sendItemBag(player);

        // 🔹 Gửi hiệu ứng và thông báo
        activeHoahong(player);
        Service.gI().sendThongBao(player, "Bạn nhận được 1 đóa" + hoaNhanDuoc.template.name);

        // 🔹 Đếm số lần hái, sau 7 lần thì đổi map
        count++;
        if (count >= COUNT_CHANGE) {
            count = 0;
            this.map.npcs.remove(this);
            Map mapHoahong = MapService.gI().getMapForHoaHong();
            this.mapId = mapHoahong.mapId;
            this.cx = Util.nextInt(100, mapHoahong.mapWidth - 100);
            this.cy = mapHoahong.yPhysicInTop(this.cx, 0);
            this.map = mapHoahong;
            this.map.npcs.add(this);
            System.out.println("Hoa Hồng mọc lại tại map " + mapHoahong.mapName);
        }
    }

    public static void activeHoahong(Player pl) {
        for (int i = 0; i < 1; i++) {
            EffectMapService.gI().sendEffectMapToAllInMap(pl.zone, 241, 1, 1, pl.location.x - Util.nextInt(-50, 50), pl.location.y, 1);

        }
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            this.createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Muốn tỉa mình, xem trình bạn thế nào?",
                    "Đồng ý", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                if (select == 0) {
                    haiHoaHong(player);
                }
            }
        }
    }
}
