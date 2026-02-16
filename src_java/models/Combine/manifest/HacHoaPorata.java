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

public class HacHoaPorata {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 3 và 99 Đá hắc hóa");
            return;
        }
        Item bongTai = null;
        Item manhVo = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 1604) {
                bongTai = item;
            } else if (item.template.id == 1882) {
                manhVo = item;
            }
        }
        if (bongTai == null || manhVo == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 3 và 99 Đá hắc hóa");
            return;
        }
        int quantityManhVo = manhVo.quantity;
        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Bông tai Porata Hắc Hóa\n\n");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 30%\n");
        text.append(quantityManhVo >= 99 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 99 Đá hắc hóa\n");
        text.append(player.inventory.gold >= 5_000_000 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 5 Tr vàng\n");
        text.append(player.inventory.getGemAndRuby() >= 20 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 20 ngọc\n");
        text.append(ConstFont.BOLD_RED).append("Thất bại -99 Đá Hắc Hóa\n");

        if (player.inventory.getGemAndRuby() < 20) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(20 - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }
        if (player.inventory.gold < 5_000_000) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(5_000_000 - player.inventory.gold) + " vàng");
            return;
        }
        if (quantityManhVo < 99) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + (99 - quantityManhVo) + " Đá hắc hóa");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Hắc Hóa\n5 Tr vàng\n20 ngọc", "Từ chối");
    }

    public static void nangCapBongTai(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
         
            return;
        }
        Item bongTai = null;
        Item manhVo = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 1604) {
                bongTai = item;
            } else if (item.template.id == 1882) {
                manhVo = item;
            }
        }
        if (bongTai == null || manhVo == null) {
          
            return;
        }
        int quantityManhVo = manhVo.quantity;
        if (quantityManhVo < 99 || player.inventory.gold < 5_000_000 || player.inventory.getGemAndRuby() < 20) {
            
            return;
        }

        player.inventory.gold -= 5_000_000;
        player.inventory.subGemAndRuby(20);
        if (Util.isTrue(30, 100)) {
            int[] option = {73,73,73,73,38};
            int[] param =  {1,1,1,1,1};
            int count = 0;
                    for (Item.ItemOption io : bongTai.itemOptions) {
                            option[count] = io.optionTemplate.id;
                            param[count] = io.param ;
                            if(io.optionTemplate.id==72){
                                param[count] = 10;
                            }
                            count++;
                    }
            Item btc2 = ItemService.gI().createNewItem((short) 1605);
            for(int i = 0;i<5;i++){
                if(i==3){
                    btc2.itemOptions.add(new Item.ItemOption(149+player.gender, 1));
                }else{
                     btc2.itemOptions.add(new Item.ItemOption(option[i], param[i]));
                }
               
            }
          
            InventoryService.gI().subQuantityItemsBag(player, bongTai, 1);
            InventoryService.gI().addItemBag(player, btc2);
            CombineService.gI().sendEffectSuccessCombine(player);
             InventoryService.gI().subQuantityItemsBag(player, manhVo, 99);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
           InventoryService.gI().subQuantityItemsBag(player, manhVo, 99);
        }
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}