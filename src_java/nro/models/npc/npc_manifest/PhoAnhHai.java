package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import event.event_manifest.LunarNewYear;
import item.Item;
import item.Item.ItemOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.server.Client;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;
import static org.apache.commons.lang.CharSetUtils.count;
import shop.ShopService;
import utils.Util;

;

public class PhoAnhHai extends Npc {

    public PhoAnhHai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
        if (EventManager.PHO_ANH_HAI) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, đá bát phở không bạn ơi?",
                    "Đá bát phở", "Sờ cậu vàng", "Hốt cậu vàng", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId != 0 && this.mapId != 5 && this.mapId != 7 && this.mapId != 14) {
            return;
        }

        // Menu cơ bản
        if (player.iDMark.isBaseMenu()) {
            if (EventManager.PHO_ANH_HAI) {
                switch (select) {
                    case 0:
                        ShopService.gI().opendShop(player, "DA_BAC_PHO", false);
                        break;
                    case 1:
                        this.createOtherMenu(player, 111,
                                "Sờ cậu vàng mất 50 triệu vàng\n"
                                + "Có quà bất người chờ cậu.",
                                "Đồng ý", "Từ chối");
                        break;
                    case 2:
                        this.createOtherMenu(player, 222,
                                "Vào hành trang chọn 1 thông long (Chi Chi có bán) để hốt",
                                "Đóng");
                        break;
                }
            }
        }
        // Menu 111: Sờ cậu vàng
        if (player.iDMark.getIndexMenu() == 111) {
            switch (select) {
                case 0: // Đồng ý
                    long cost = 50_000_000; // 50 triệu vàng

                    if (player.inventory.gold < cost) {
                        Service.gI().sendThongBao(player, "Không đủ 50.000.000 vàng!");
                        return;
                    }

                    // Trừ vàng
                    player.inventory.gold -= cost;
                    Service.gI().sendMoney(player);

                    // Random thưởng: 80% trượt, 20% trúng
                    int rand = Util.nextInt(1, 100);

                    if (rand <= 70) {
                        Service.gI().sendThongBao(player, "Chúc bạn may mắn lần sau!");
                    } else {

                        // ✅ mỗi lần trúng là tặng 3 item
                        for (int i = 0; i < 3; i++) {
                            Item itemRare = ItemService.gI().createNewItem((short) 1982);
                            itemRare.itemOptions.add(new ItemOption(93, 30));
                            InventoryService.gI().addItemBag(player, itemRare);
                        }

                        InventoryService.gI().sendItemBag(player);

                        Service.gI().sendThongBao(player,
                                "Cậu vàng thích bạn! Bạn nhận được 3 hộp quà cực hiếm!");
                    }

                    break;
            }
            return;
        }
    }
}
