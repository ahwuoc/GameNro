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

public class NangCapChanMenh {

    private static final int DA_THIEN_TU_ID = 1905;
    private static final int REQUIRED_DA = 99;
    private static final double SUCCESS_RATE = 10;

    // ID các Chân Mệnh từ cấp 1 đến cấp 9
    private static final int[] CHAN_MENH_IDS = {1885, 1886, 1887, 1888, 1889, 1890, 1891, 1892, 1893};
    private static final int MAX_LEVEL = CHAN_MENH_IDS.length - 1;

    private static boolean isChanMenh(Item item) {
        if (item == null || item.template == null) {
            return false;
        }
        int itemId = item.template.id;
        for (int chanMenhId : CHAN_MENH_IDS) {
            if (itemId == chanMenhId) {
                return true;
            }
        }
        return false;
    }

    private static int getChanMenhLevel(Item item) {
        if (!isChanMenh(item)) {
            return -1;
        }
        int itemId = item.template.id;
        for (int i = 0; i < CHAN_MENH_IDS.length; i++) {
            if (itemId == CHAN_MENH_IDS[i]) {
                return i;
            }
        }
        return -1;
    }

    public static void showInfoCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Chân Mệnh và 99 Sao Thiên Tử", "Đóng");
            return;
        }

        Item chanMenh = null, daThienTu = null;
        for (Item item : player.combine.itemsCombine) {
            if (isChanMenh(item)) {
                chanMenh = item;
            } else if (item.template.id == DA_THIEN_TU_ID) {
                daThienTu = item;
            }
        }

        if (chanMenh == null) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Thiếu Chân Mệnh", "Đóng");
            return;
        }

        int currentLevel = getChanMenhLevel(chanMenh);
        if (currentLevel >= MAX_LEVEL) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Chân Mệnh đã đạt cấp tối đa", "Đóng");
            return;
        }

        long vang = 500_000_000L;
        StringBuilder text = new StringBuilder("|0|Nâng cấp Chân Mệnh\n");
        text.append(chanMenh.template.name).append("\n");
        text.append(daThienTu != null && daThienTu.quantity >= REQUIRED_DA ? ConstFont.BOLD_GREEN : ConstFont.BOLD_RED)
                .append("Cần: 99 Sao Thiên Tử\n");
        text.append(player.inventory.gold >= vang ? ConstFont.BOLD_GREEN : ConstFont.BOLD_RED)
                .append("Cần: ").append(Util.numberFormat(vang)).append(" vàng\n");
        text.append(ConstFont.BOLD_BLUE)
                .append("Tỉ lệ thành công: ").append((int) SUCCESS_RATE).append("%");

        CombineService.gI().baHatMit.createOtherMenu(
                player,
                ConstNpc.MENU_START_COMBINE,
                text.toString(),
                "Nâng cấp"
        );
    }

    public static void startCombine(Player player) {
        if (player.combine.itemsCombine.size() != 2) {
            Service.gI().sendThongBao(player, "Vui lòng chọn đủ 2 vật phẩm");
            return;
        }

        Item chanMenh = null, daThienTu = null;
        for (Item item : player.combine.itemsCombine) {
            if (isChanMenh(item)) {
                chanMenh = item;
            } else if (item.template.id == DA_THIEN_TU_ID) {
                daThienTu = item;
            }
        }

        long vang = 500_000_000L;
        int currentLevel = getChanMenhLevel(chanMenh);

        if (chanMenh == null || daThienTu == null) {
            Service.gI().sendThongBao(player, "Thiếu vật phẩm cần thiết");
            return;
        }

        if (daThienTu.quantity < REQUIRED_DA) {
            Service.gI().sendThongBao(player, "Không đủ 99 Sao Thiên Tử");
            return;
        }

        if (player.inventory.gold < vang) {
            Service.gI().sendThongBao(player, "Không đủ vàng");
            return;
        }

        if (currentLevel >= MAX_LEVEL) {
            Service.gI().sendThongBao(player, "Chân Mệnh đã đạt cấp tối đa");
            return;
        }

        // Trừ nguyên liệu
        InventoryService.gI().subQuantityItemsBag(player, daThienTu, REQUIRED_DA);
        player.inventory.gold -= vang;

        boolean isSuccess = Util.isTrue((int) SUCCESS_RATE, 100);
        if (isSuccess) {
            // Tạo Chân Mệnh mới với ID tiếp theo
            int nextLevel = currentLevel + 1;
            int nextId = CHAN_MENH_IDS[nextLevel];
            Item chanMenhMoi = ItemService.gI().createNewItem((short) nextId);

            // Copy và tăng cường các Option quan trọng
            chanMenhMoi.itemOptions.clear();
                for (Item.ItemOption oldOpt : chanMenh.itemOptions) {
                    if (oldOpt == null) continue;
                    int newParam = oldOpt.param;
                    if (oldOpt.optionTemplate.id == 50 
                        || oldOpt.optionTemplate.id == 77 
                        || oldOpt.optionTemplate.id == 103) {
                        int percent = Util.nextInt(1, 3);
                        newParam = (int) Math.ceil(oldOpt.param * (1 + percent / 100.0));
                    }
                    chanMenhMoi.itemOptions.add(
                        new Item.ItemOption(oldOpt.optionTemplate.id, newParam)
                    );
                }

            InventoryService.gI().subQuantityItemsBag(player, chanMenh, 1);
            InventoryService.gI().addItemBag(player, chanMenhMoi);
            CombineService.gI().sendEffectSuccessCombine(player);
            Service.gI().sendThongBao(player, "Nâng cấp Chân Mệnh thành công!");
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Nâng cấp Chân Mệnh thất bại!");
        }

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
