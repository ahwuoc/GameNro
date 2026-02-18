package models.Combine.manifest;

import consts.ConstFont;
import consts.ConstNpc;
import item.Item;
import models.Combine.CombineService;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.Service;
import utils.Util;

public class NangChiSoBongTaiCap3 {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 3) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 3, 198 mảnh hồn porata và 2 đá xanh lam.");
            return;
        }
        Item bongTai = null;
        Item manhHonBongTai = null;
        Item daXanhLam = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.isNotNullItem()) {
                switch (item.template.id) {
                    case 1810 ->
                        bongTai = item;
                    case 934 ->
                        manhHonBongTai = item;
                    case 935 ->
                        daXanhLam = item;
                }
            }
        }

        if (bongTai == null || manhHonBongTai == null || daXanhLam == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 bông tai cấp 3, 198 mảnh hồn porata và 2 đá xanh lam.");
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Bông tai Porata [+3]\n\n");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: 50%\n");
        text.append(manhHonBongTai.quantity >= 198 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 198 Mảnh hồn bông tai\n");
        text.append(daXanhLam.quantity >= 2 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 2 Đá xanh lam\n");
        text.append(player.inventory.getGemAndRuby() >= 500 ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED).append("Cần 500 ngọc\n");
        text.append(ConstFont.BOLD_GREEN).append("+1 Chỉ số ngẫu nhiên\n");
        if (player.inventory.getGemAndRuby() < 500) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + Util.numberToMoney(500 - player.inventory.getGemAndRuby()) + " ngọc");
            return;
        }
        if (daXanhLam.quantity < 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\nĐá xanh lam");
            return;
        }
        if (manhHonBongTai.quantity < 198) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(), "Còn thiếu\n" + (198 - manhHonBongTai.quantity) + " Mảnh hồn bông tai cấp 3");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Nâng cấp\n500 ngọc", "Từ chối");
    }

    public static void nangChiSoBongTai(Player player) {
        if (player.combine.itemsCombine.size() != 3) {
            return;
        }
        Item bongTai = null;
        Item manhHonBongTai = null;
        Item daXanhLam = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.isNotNullItem()) {
                switch (item.template.id) {
                    case 1810 ->
                        bongTai = item;
                    case 934 ->
                        manhHonBongTai = item;
                    case 935 ->
                        daXanhLam = item;
                }
            }
        }

        if (bongTai == null || manhHonBongTai == null || daXanhLam == null || player.inventory.getGemAndRuby() < 500 || daXanhLam.quantity < 1 || manhHonBongTai.quantity < 198) {
            return;
        }
        if (Util.isTrue(50, 100)) {
            int[] options = {77, 103, 50, 108, 94, 14, 80, 81, 175, 5};
            int option = options[Util.nextInt(options.length)];
            int param = option == 94 || option == 14 ? Util.nextInt(7, 15) : Util.nextInt(8, 20);
            bongTai.itemOptions.clear();
            bongTai.itemOptions.add(new Item.ItemOption(option, param));
            bongTai.itemOptions.add(new Item.ItemOption(38, 0));
            CombineService.gI().sendEffectSuccessCombine(player);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }
        InventoryService.gI().subQuantityItemsBag(player, manhHonBongTai, 198);
        InventoryService.gI().subQuantityItemsBag(player, daXanhLam, 2);
        InventoryService.gI().sendItemBag(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}
