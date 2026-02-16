package models.Combine.manifest;

import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import player.Player;
import services.InventoryService;
import services.Service;
import utils.Util;

public class Tayphapsuhoa {
    public static void showInfoCombine(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.combine.itemsCombine.size() == 1) {
                Item itemTay = null;
                for (Item item_ : player.combine.itemsCombine) {
                    if (item_.isTrangBiPSH() && item_.haveOption(57)) {
                        itemTay = item_;
                    }
                }
                if (itemTay == null) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Bạn cần đặt trang bị có chỉ số pháp sư", "Đóng");
                    return;
                }
                
                String npcSay = "|2|Tẩy pháp sư: " + itemTay.template.name + "\n|0|";
                for (Item.ItemOption io : itemTay.itemOptions) {
                    npcSay += io.getOptionString() + "\n";
                }
                npcSay += "|7|\n|7|Sau khi tẩy sẽ xóa toàn bộ chỉ số pháp sư\n"
                        + "Cần " + Util.numberToMoney(500000000) + " vàng";

                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                        npcSay, "Tẩy pháp sư\n" + Util.numberToMoney(500000000) + " vàng", "Từ chối");
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Cần đặt 1 trang bị có chỉ số pháp sư để tẩy", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hành trang cần ít nhất 1 chỗ trống", "Đóng");
        }
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 1) {
            Service.gI().sendThongBao(player, "Cần đặt 1 trang bị");
            return;
        }
        Item itemTay = player.combine.itemsCombine.stream()
                .filter(item -> item.isNotNullItem() && item.isTrangBiPSH() && item.haveOption(57))
                .findFirst().orElse(null);
        
        if (itemTay == null) {
            Service.gI().sendThongBao(player, "Trang bị không có chỉ số pháp sư");
            return;
        }
        
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            if (player.inventory.gold < 500000000) {
                Service.gI().sendThongBao(player, "Cần 500 triệu vàng để tẩy pháp sư");
                return;
            }
            player.inventory.gold -= 500000000;
            
            java.util.Iterator<Item.ItemOption> iterator = itemTay.itemOptions.iterator();
            while (iterator.hasNext()) {
                Item.ItemOption io = iterator.next();
                if (io.optionTemplate.id == 57) {
                    iterator.remove();
                }
            }
            
            CombineService.gI().sendEffectSuccessCombine(player);
            Service.gI().sendThongBao(player, "Tẩy pháp sư thành công");
            
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendMoney(player);
            player.combine.itemsCombine.clear();
            CombineService.gI().reOpenItemCombine(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
        }
    }
}
