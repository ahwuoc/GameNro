package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class NangCapBongTaiCap3 {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 2 và 20000 mảnh vỡ bông tai cấp 3.");
            return;
        }
        Item bongTai2 = null;
        Item manhVo = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 921) {
                bongTai2 = item;
            } else if (item.template.id == 1855) {
                manhVo = item;
            }
        }
        if (bongTai2 == null || manhVo == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 2 và 20000 mảnh vỡ bông tai cấp 3.");
            return;
        }
        int quantityManhVo = manhVo.getOptionParam(31);
        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Bông tai Porata [+2]\n\n");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 50%\n");
        text.append(quantityManhVo >= 20000 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 20000 Mảnh vỡ bông tai cấp 3\n");
        text.append(player.inventory.gold >= 10_000_000 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 10 Tr vàng\n");
        text.append(player.inventory.getGemAndRuby() >= 40 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 40 ngọc\n");
        text.append(ConstFont.BOLD_RED).append("Thất bại -198 mảnh vỡ bông tai\n");

        if (player.inventory.getGemAndRuby() < 40) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(40 - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }
        if (player.inventory.gold < 10_000_000) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(5_000_000 - player.inventory.gold) + " vàng");
            return;
        }
        if (quantityManhVo < 20000) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + (9999 - quantityManhVo) + " Mảnh vỡ bông tai");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Nâng cấp\n5 Tr vàng\n40 ngọc", "Từ chối");
    }

    public static void nangCapBongTai(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }
        Item bongTai = null;
        Item manhVo = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 921) {
                bongTai = item;
            } else if (item.template.id == 1855) {
                manhVo = item;
            }
        }
        if (bongTai == null || manhVo == null) {
            return;
        }
        int quantityManhVo = manhVo.getOptionParam(31);
        if (quantityManhVo < 20000 || player.inventory.gold < 10_000_000 || player.inventory.getGemAndRuby() < 40) {
            return;
        }

        player.inventory.gold -= 10_000_000;
        player.inventory.subGemAndRuby(40);
        if (Util.isTrue(50, 100)) {
            Item btc2 = ItemService.gI().createNewItem((short) 1810);
            btc2.itemOptions.add(new Item.ItemOption(72, 2));
            InventoryService.gI().subQuantityItemsBag(player, bongTai, 1);
            InventoryService.gI().addItemBag(player, btc2);
            CombineService.gI().sendEffectSuccessCombine(player);
            InventoryService.gI().subParamItemsBag(player, 933, 31, 20000);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            InventoryService.gI().subParamItemsBag(player, 933, 31, 198);
        }
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}