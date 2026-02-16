package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import player.Player;
import services.InventoryService;
import services.ItemService;
import services.Service;
import utils.Util;

public class NangCapBongTai2 {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 2 và 9999 mảnh vỡ bông tai 3.");
            return;
        }
        Item bongTai = null;
        Item manhVo = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 921) {
                bongTai = item;
            } else if (item.template.id == 1609) {
                manhVo = item;
            }
        }
        if (bongTai == null || manhVo == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 2 và 9999 mảnh vỡ bông tai 3.");
            return;
        }
        int quantityManhVo = manhVo.quantity;
        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Bông tai Porata [+3]\n\n");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 50%\n");
        text.append(quantityManhVo >= 9999 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 9999 Mảnh vỡ bông tai 3\n");
        text.append(player.inventory.gold >= 5_000_000 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 5 Tr vàng\n");
        text.append(player.inventory.getGemAndRuby() >= 20 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 20 ngọc\n");
        text.append(ConstFont.BOLD_RED).append("Thất bại -99 mảnh vỡ bông tai 2\n");

        if (player.inventory.getGemAndRuby() < 20) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(20 - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }
        if (player.inventory.gold < 5_000_000) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(5_000_000 - player.inventory.gold) + " vàng");
            return;
        }
        if (quantityManhVo < 9999) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + (9999 - quantityManhVo) + " Mảnh vỡ bông tai 3");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Nâng cấp\n5 Tr vàng\n20 ngọc", "Từ chối");
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
            } else if (item.template.id == 1609) {
                manhVo = item;
            }
        }
        if (bongTai == null || manhVo == null) {
          
            return;
        }
        int quantityManhVo = manhVo.quantity;
        if (quantityManhVo < 9999 || player.inventory.gold < 5_000_000 || player.inventory.getGemAndRuby() < 20) {
            
            return;
        }

        player.inventory.gold -= 5_000_000;
        player.inventory.subGemAndRuby(20);
        if (Util.isTrue(50, 100)) {
            int option = 0;
            int param = 0;
                    for (Item.ItemOption io : bongTai.itemOptions) {
                        if (io.optionTemplate.id != 72
                                ) {
                            option = io.optionTemplate.id;
                            param = io.param ;
                           
                            break;
                        }
                    }
            Item btc2 = ItemService.gI().createNewItem((short) 1604);
            btc2.itemOptions.add(new Item.ItemOption(72, 3));
           if(option!=0&&param!=0){
                btc2.itemOptions.add(new Item.ItemOption(option, param));
           }
            InventoryService.gI().subQuantityItemsBag(player, bongTai, 1);
            InventoryService.gI().addItemBag(player, btc2);
            CombineService.gI().sendEffectSuccessCombine(player);
             InventoryService.gI().subQuantityItemsBag(player, manhVo, 9999);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
           InventoryService.gI().subQuantityItemsBag(player, manhVo, 99);
        }
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}