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
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

;

public class CayNeu extends Npc {

    private static final int DOI_THUONG_MENU = 1;
    private static final int DOI_VIP_MENU = 2;

    private static final short[] EVENT_ITEMS = {
        1150, 1151, 1152, 1153, 1154, // Hỗ trợ cấp 2
        213, 214, 215, 216, 217, 218, 219, // Bùa 1h
        828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 859, 956, 1901, // Thẻ sưu tầm
        578, 1255, // Cải Trang Bư Mập, Bư Han
        1935, 1145, // Goku SSJ White & Black SSJ3 White
        596, 597, // Túi x30 đậu
        1223, // Pet Khí Gas
        76, // Vàng
        1205, // Drabura Frost
        1945, // Kilin Múa Lân
        1047 // Lồng Đèn 2 Lon
    };

    public CayNeu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
        if (EventManager.TRUNG_THU) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Mỗi lần treo đèn bạn sẽ được tặng 1 món quà xịn sò nhất",
                    "Treo đèn", "Đóng");
        } else if (EventManager.LUNAR_NEW_YEAR) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "|2|Bạn muốn trang trí Cây Nêu phải không",
                    "Trang Trí Thường", "Trang Trí VIP");
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
            if (EventManager.TRUNG_THU) {
                switch (select) {
                    case 0:
                        handleTrungThu(player);
                        break;
                }
            } else if (EventManager.LUNAR_NEW_YEAR) {
                switch (select) {
                    case 0:
                        xuLiTrangTriThuong(player);
                        break;
                    case 1:
                        xuLiTrangTriVIP(player);
                        break;
                }
            }
            return;
        }
        if (EventManager.LUNAR_NEW_YEAR && player.iDMark.getIndexMenu() == DOI_THUONG_MENU) {
            LunarNewYear.gI().cayNeuQuaThuong(player); // Phần thưởng khi trang trí thường
        }
        if (EventManager.LUNAR_NEW_YEAR && player.iDMark.getIndexMenu() == DOI_VIP_MENU) {
            LunarNewYear.gI().cayNeuQuaVip(player); // Phần thưởng khi trang trí VIP
        }
    }

    private void handleTrungThu(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy.");
            return;
        }

        Item den = InventoryService.gI().finditemnguyenlieuDen(player);
        if (den == null) {
            Service.gI().sendThongBao(player, "Cần 1 lồng đèn treo");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, den, 1);

        // --- Tỉ lệ item dựa trên số lượng option ---
        Map<Short, Integer> itemWeight = new HashMap<>();
        itemWeight.put((short) 1047, 80); // Lồng Đèn 2 Lon (1-2 option => dễ ra)
        itemWeight.put((short) 1223, 80); // Pet Khí Gas
        itemWeight.put((short) 578, 70);   // Bư Mập (nhiều option => khó ra)
        itemWeight.put((short) 1255, 70);  // Bư Han
        itemWeight.put((short) 1935, 30);  // Goku SSJ White
        itemWeight.put((short) 1145, 60);  // Black Goku SSJ3
        itemWeight.put((short) 1205, 50);  // Drabura Frost
        itemWeight.put((short) 1945, 30);  // Kilin Múa Lân (nhiều option => rất khó ra)

        for (short itemId : EVENT_ITEMS) {
            if (!itemWeight.containsKey(itemId)) {
                itemWeight.put(itemId, 100); // Item không có option => weight cao, dễ ra
            }
        }

        // Random item theo weight
        int totalWeight = itemWeight.values().stream().mapToInt(i -> i).sum();
        int r = ThreadLocalRandom.current().nextInt(totalWeight);
        short itemId = 0;
        for (Map.Entry<Short, Integer> entry : itemWeight.entrySet()) {
            if (r < entry.getValue()) {
                itemId = entry.getKey();
                break;
            }
            r -= entry.getValue();
        }

        Item newItem = ItemService.gI().createNewItem(itemId, 1);

        // Cấu hình ItemOption theo từng item
        Map<Short, List<ItemOptionConfig>> optionMap = Map.of(
                (short) 1047, List.of(new ItemOptionConfig(50, 7, 12, true),
                        new ItemOptionConfig(77, 7, 12, true),
                        new ItemOptionConfig(103, 7, 12, true),
                        new ItemOptionConfig(30, 0, false)),
                (short) 1223, List.of(new ItemOptionConfig(50, 5, 10, true),
                        new ItemOptionConfig(77, 5, 10, true),
                        new ItemOptionConfig(103, 5, 10, true),
                        new ItemOptionConfig(30, 0, false)),
                (short) 578, List.of(new ItemOptionConfig(50, 20, 27, true),
                        new ItemOptionConfig(77, 17, false),
                        new ItemOptionConfig(103, 17, false),
                        new ItemOptionConfig(30, 0, false)),
                (short) 1255, List.of(new ItemOptionConfig(50, 17, false),
                        new ItemOptionConfig(77, 20, 27, true),
                        new ItemOptionConfig(103, 17, false),
                        new ItemOptionConfig(30, 0, false)),
                (short) 1935, List.of(new ItemOptionConfig(50, 17, false),
                        new ItemOptionConfig(77, 17, false),
                        new ItemOptionConfig(103, 20, 27, true),
                        new ItemOptionConfig(30, 0, false)),
                (short) 1145, List.of(new ItemOptionConfig(50, 20, 27, true),
                        new ItemOptionConfig(77, 13, false),
                        new ItemOptionConfig(103, 13, false),
                        new ItemOptionConfig(30, 0, false)),
                (short) 1205, List.of(new ItemOptionConfig(50, 20, 25, true),
                        new ItemOptionConfig(77, 15, 17, true),
                        new ItemOptionConfig(103, 15, 17, true),
                        new ItemOptionConfig(30, 0, false)),
                (short) 1945, List.of(new ItemOptionConfig(50, 22, 27, true),
                        new ItemOptionConfig(77, 17, 20, true),
                        new ItemOptionConfig(103, 17, 20, true),
                        new ItemOptionConfig(14, 3, 7, true),
                        new ItemOptionConfig(5, 5, 10, true),
                        new ItemOptionConfig(30, 0, false))
        );

        // Thêm ItemOption mặc định
        if ((itemId >= 213 && itemId <= 219)) {
            newItem.itemOptions.add(new ItemOption(185, 1));
        }
        if ((itemId >= 828 && itemId <= 842) || itemId == 859 || itemId == 1901 || itemId == 856) {
            newItem.itemOptions.add(new ItemOption(30, 0));
        }

        // Thêm ItemOption theo cấu hình map
        List<ItemOptionConfig> configs = optionMap.get(itemId);
        if (configs != null) {
            for (ItemOptionConfig cfg : configs) {
                int value = cfg.isRandom ? ThreadLocalRandom.current().nextInt(cfg.min, cfg.max + 1) : cfg.min;
                newItem.itemOptions.add(new ItemOption(cfg.id, value));
            }
        }
        player.longdentreo++;
        Service.gI().sendThongBao(player, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu treo đèn");
        InventoryService.gI().addItemBag(player, newItem);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name);
    }

    // Lớp hỗ trợ cấu hình ItemOption
    private static class ItemOptionConfig {

        int id;
        int min;
        int max;
        boolean isRandom;

        ItemOptionConfig(int id, int value, boolean isRandom) { // giá trị cố định
            this.id = id;
            this.min = value;
            this.max = value;
            this.isRandom = isRandom;
        }

        ItemOptionConfig(int id, int min, int max, boolean isRandom) { // giá trị random
            this.id = id;
            this.min = min;
            this.max = max;
            this.isRandom = isRandom;
        }
    }

    private void xuLiTrangTriThuong(Player player) {

        Item DayPhao = InventoryService.gI().findItemBag(player, 1472); // Dây pháo
        Item CauDoi = InventoryService.gI().findItemBag(player, 1473); // Câu đối
        Item DayTreoBanh = InventoryService.gI().findItemBag(player, 1474); // Dây treo bánh
        Item DenLongTreoCay = InventoryService.gI().findItemBag(player, 1475); // Lồng đèn treo cây

        int xDayPhao = checkItemQuantity(DayPhao, 2);
        int xCauDoi = checkItemQuantity(CauDoi, 4);
        int xDayTreoBanh = checkItemQuantity(DayTreoBanh, 20);
        int xDenLongTreoCay = checkItemQuantity(DenLongTreoCay, 1);

        long goldRequired = 5000000; // 5 triệu vàng
        boolean isGoldSufficient = player.inventory.gold >= goldRequired;

        boolean isMissingItems = (xDayPhao == 7 || xCauDoi == 7 || xDayTreoBanh == 7 || xDenLongTreoCay == 7);

        if (!isGoldSufficient || isMissingItems) {
            taoMenuVatPhamThieu(player, DayPhao, CauDoi, DayTreoBanh, DenLongTreoCay, xDayPhao, xCauDoi, xDayTreoBanh, xDenLongTreoCay);
        } else {
            taoMenuVatPhamDayDu(player, DayPhao, CauDoi, DayTreoBanh, DenLongTreoCay, xDayPhao, xCauDoi, xDayTreoBanh, xDenLongTreoCay);
        }
    }

    private void taoMenuVatPhamThieu(Player player, Item DayPhao, Item CauDoi, Item DayTreoBanh, Item DenLongTreoCay, int xDayPhao, int xCauDoi, int xDayTreoBanh, int xDenLongTreoCay) {
        long goldRequired = 5000000;
        long currentGold = player.inventory.gold;
        String currentGoldFormatted = formatGold(currentGold);
        String goldRequiredFormatted = formatGold(goldRequired);
        int x = (currentGold < goldRequired) ? 7 : 2;
        String menuContent = "|1|Để trang trí thường cần:\n"
                + "|" + xDayPhao + "|" + (DayPhao != null ? DayPhao.quantity : 0) + "/2 dây pháo\n"
                + "|" + xCauDoi + "|" + (CauDoi != null ? CauDoi.quantity : 0) + "/4 câu đối\n"
                + "|" + xDayTreoBanh + "|" + (DayTreoBanh != null ? DayTreoBanh.quantity : 0) + "/20 dây treo bánh\n"
                + "|" + xDenLongTreoCay + "|" + (DenLongTreoCay != null ? DenLongTreoCay.quantity : 0) + "/1 lồng đèn treo cây\n"
                + "|x|Số vàng hiện có: " + currentGoldFormatted + "/" + goldRequiredFormatted;

        menuContent = menuContent.replace("|x|", "|" + x + "|");

        this.createOtherMenu(player, 14, menuContent, "Đóng");
    }

    private void taoMenuVatPhamDayDu(Player player, Item DayPhao, Item CauDoi, Item DayTreoBanh, Item DenLongTreoCay, int xDayPhao, int xCauDoi, int xDayTreoBanh, int xDenLongTreoCay) {
        long goldRequired = 5000000;
        long currentGold = player.inventory.gold;
        String currentGoldFormatted = formatGold(currentGold);
        String goldRequiredFormatted = formatGold(goldRequired);
        int x = (currentGold < goldRequired) ? 7 : 2;
        String menuContent = "|1|Để trang trí thường cần:\n"
                + "|" + xDayPhao + "|" + (DayPhao != null ? DayPhao.quantity : 0) + "/2 dây pháo\n"
                + "|" + xCauDoi + "|" + (CauDoi != null ? CauDoi.quantity : 0) + "/4 câu đối\n"
                + "|" + xDayTreoBanh + "|" + (DayTreoBanh != null ? DayTreoBanh.quantity : 0) + "/20 dây treo bánh\n"
                + "|" + xDenLongTreoCay + "|" + (DenLongTreoCay != null ? DenLongTreoCay.quantity : 0) + "/1 lồng đèn treo cây\n"
                + "|x|Số vàng hiện có: " + currentGoldFormatted + "/" + goldRequiredFormatted;

        // Thay thế |x| bằng giá trị của x
        menuContent = menuContent.replace("|x|", "|" + x + "|");

        this.createOtherMenu(player, DOI_THUONG_MENU, menuContent, "Trang Trí Thường");
    }

    private void xuLiTrangTriVIP(Player player) {
        // Kiểm tra vật phẩm
        Item DayPhao = InventoryService.gI().findItemBag(player, 1472); // Dây pháo
        Item CauDoi = InventoryService.gI().findItemBag(player, 1473); // Câu đối
        Item DayTreoBanh = InventoryService.gI().findItemBag(player, 1474); // Dây treo bánh
        Item DenLongTreoCay = InventoryService.gI().findItemBag(player, 1475); // Lồng đèn treo cây

        // Kiểm tra đủ vật phẩm
        int xDayPhao = checkItemQuantity(DayPhao, 2);
        int xCauDoi = checkItemQuantity(CauDoi, 4);
        int xDayTreoBanh = checkItemQuantity(DayTreoBanh, 20);
        int xDenLongTreoCay = checkItemQuantity(DenLongTreoCay, 1);

        // Kiểm tra số vàng
        long goldRequired = 5000000; // 5 triệu vàng
        boolean isGoldSufficient = player.inventory.gold >= goldRequired;

        // Kiểm tra số gem
        int requiredGems = 50; // Cần ít nhất 5 gem
        boolean isGemsSufficient = player.inventory.getGemAndRuby() >= requiredGems;

        // Kiểm tra nếu thiếu vật phẩm, vàng hoặc gem
        boolean isMissingItems = (xDayPhao == 7 || xCauDoi == 7 || xDayTreoBanh == 7 || xDenLongTreoCay == 7);
        boolean isMissingGems = !isGemsSufficient;

        if (!isGoldSufficient || isMissingItems || isMissingGems) {
            // Nếu thiếu vật phẩm, thiếu vàng hoặc thiếu gem, tạo menu thông báo thiếu
            taoMenuVatPhamThieu1(player, DayPhao, CauDoi, DayTreoBanh, DenLongTreoCay, xDayPhao, xCauDoi, xDayTreoBanh, xDenLongTreoCay);
        } else {
            // Nếu đủ cả vật phẩm, vàng và gem, tạo menu đầy đủ
            taoMenuVatPhamDayDu1(player, DayPhao, CauDoi, DayTreoBanh, DenLongTreoCay, xDayPhao, xCauDoi, xDayTreoBanh, xDenLongTreoCay);
        }
    }

    private int checkItemQuantity(Item item, int requiredQuantity) {
        return (item != null && item.quantity >= requiredQuantity) ? 2 : 7;
    }

    private String formatGold(long gold) {
        if (gold >= 1000000000) { // Nếu vàng lớn hơn hoặc bằng tỷ
            return gold / 1000000000 + " tỷ";
        } else if (gold >= 1000000) { // Nếu vàng lớn hơn hoặc bằng triệu
            return gold / 1000000 + " triệu";
        } else {
            return String.valueOf(gold); // Nếu vàng nhỏ hơn triệu
        }
    }

    private void taoMenuVatPhamThieu1(Player player, Item DayPhao, Item CauDoi, Item DayTreoBanh, Item DenLongTreoCay, int xDayPhao, int xCauDoi, int xDayTreoBanh, int xDenLongTreoCay) {
        long goldRequired = 5000000; // 5 triệu vàng
        long currentGold = player.inventory.gold; // Số vàng hiện tại của người chơi
        int currentGems = player.inventory.getGemAndRuby(); // Số ngọc hiện tại của người chơi

        // Chia số vàng hiện có thành triệu hoặc tỷ
        String currentGoldFormatted = formatGold(currentGold);
        String goldRequiredFormatted = formatGold(goldRequired);

        // Kiểm tra nếu không đủ vàng, gán x = 7 (màu đỏ)
        int xGold = (currentGold < goldRequired) ? 7 : 2;

        // Số ngọc yêu cầu
        int requiredGems = 50; // Cần ít nhất 5 ngọc
        int xGems = (currentGems < requiredGems) ? 7 : 2; // Kiểm tra số ngọc

        // Tạo menu với số vàng, ngọc hiện có và số vàng, ngọc yêu cầu
        String menuContent = "|1|Để trang trí [VIP] cần:\n"
                + "|" + xDayPhao + "|" + (DayPhao != null ? DayPhao.quantity : 0) + "/2 dây pháo\n"
                + "|" + xCauDoi + "|" + (CauDoi != null ? CauDoi.quantity : 0) + "/4 câu đối\n"
                + "|" + xDayTreoBanh + "|" + (DayTreoBanh != null ? DayTreoBanh.quantity : 0) + "/20 dây treo bánh\n"
                + "|" + xDenLongTreoCay + "|" + (DenLongTreoCay != null ? DenLongTreoCay.quantity : 0) + "/1 lồng đèn treo cây\n"
                + "|xGold|Số vàng hiện có: " + currentGoldFormatted + "/" + goldRequiredFormatted + "\n"
                + "|xGems|Số ngọc hiện có: " + currentGems + "/50";

        // Thay thế |xGold| và |xGems| bằng giá trị của xGold và xGems
        menuContent = menuContent.replace("|xGold|", "|" + xGold + "|");
        menuContent = menuContent.replace("|xGems|", "|" + xGems + "|");

        // Tạo menu
        this.createOtherMenu(player, 14, menuContent, "Đóng");
    }

    private void taoMenuVatPhamDayDu1(Player player, Item DayPhao, Item CauDoi, Item DayTreoBanh, Item DenLongTreoCay, int xDayPhao, int xCauDoi, int xDayTreoBanh, int xDenLongTreoCay) {
        long goldRequired = 5000000; // 5 triệu vàng
        long currentGold = player.inventory.gold; // Số vàng hiện tại của người chơi
        int currentGems = player.inventory.getGemAndRuby(); // Số ngọc hiện tại của người chơi

        // Chia số vàng hiện có thành triệu hoặc tỷ
        String currentGoldFormatted = formatGold(currentGold);
        String goldRequiredFormatted = formatGold(goldRequired);

        // Kiểm tra nếu không đủ vàng, gán x = 7 (màu đỏ)
        int xGold = (currentGold < goldRequired) ? 7 : 2;

        // Kiểm tra số ngọc
        int requiredGems = 50; // Cần ít nhất 5 ngọc
        int xGems = (currentGems < requiredGems) ? 7 : 2; // Kiểm tra số ngọc

        // Tạo menu với số vàng, ngọc hiện có và số vàng, ngọc yêu cầu
        String menuContent = "|1|Để trang trí [VIP] cần:\n"
                + "|" + xDayPhao + "|" + (DayPhao != null ? DayPhao.quantity : 0) + "/2 dây pháo\n"
                + "|" + xCauDoi + "|" + (CauDoi != null ? CauDoi.quantity : 0) + "/4 câu đối\n"
                + "|" + xDayTreoBanh + "|" + (DayTreoBanh != null ? DayTreoBanh.quantity : 0) + "/20 dây treo bánh\n"
                + "|" + xDenLongTreoCay + "|" + (DenLongTreoCay != null ? DenLongTreoCay.quantity : 0) + "/1 lồng đèn treo cây\n"
                + "|xGold|Số vàng hiện có: " + currentGoldFormatted + "/" + goldRequiredFormatted + "\n"
                + "|xGems|Số ngọc hiện có: " + currentGems + "/50";

        // Thay thế |xGold| và |xGems| bằng giá trị của xGold và xGems
        menuContent = menuContent.replace("|xGold|", "|" + xGold + "|");
        menuContent = menuContent.replace("|xGems|", "|" + xGems + "|");

        // Tạo menu
        this.createOtherMenu(player, DOI_VIP_MENU, menuContent, "Trang Trí Vip");
    }

}
