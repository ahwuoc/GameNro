/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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

/**
 *
 * @author Administrator
 */
public class TaiTaoTrangBiKichHoat {

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 Cápsule vỡ và 3 Khoáng Tái Chế.");
            return;
        }
        Item capSule = null;
        Item khoangTaiChe = null;
        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 1634) {
                capSule = item;
            } else if (item.template.id == 1656) {
                khoangTaiChe = item;
            }
        }
        if (capSule == null || khoangTaiChe == null || khoangTaiChe.quantity < 3) {
            Service.gI().sendDialogMessage(player, "Cần 1 Cápsule vỡ và 3 Khoáng Tái Chế.");
            return;
        }
        if (player.inventory.ruby < 500) {
            Service.gI().sendThongBao(player, "Không đủ 500 ngọc xanh.");
            return;
        }
        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                "Con có chắc không ?", "Đồng ý\n500\nngọc xanh", "Từ chối");
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            return;
        }

        Item capSule = null;
        Item khoangTaiChe = null;

        for (Item item : player.combine.itemsCombine) {
            if (item.template.id == 1634) {
                capSule = item;
            } else if (item.template.id == 1656) {
                khoangTaiChe = item;
            }
        }

        if (capSule == null || khoangTaiChe == null || khoangTaiChe.quantity < 3 || player.inventory.ruby < 500) {
            return;
        }

        if (player.gender < 0 || player.gender > 2) {
            return;
        }

        short[] genderToItemId = {1806, 1807, 1808};
        short itemId = genderToItemId[player.gender];

        InventoryService.gI().subQuantityItemsBag(player, capSule, 1);
        InventoryService.gI().subQuantityItemsBag(player, khoangTaiChe, 3);
        player.inventory.ruby -= 500;

        CombineService.gI().sendEffectSuccessCombine(player);

        Item capSuleTuChon = ItemService.gI().createNewItem(itemId);
        capSuleTuChon.itemOptions.add(new Item.ItemOption(30, 0));
        InventoryService.gI().addItemBag(player, capSuleTuChon);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }

}
