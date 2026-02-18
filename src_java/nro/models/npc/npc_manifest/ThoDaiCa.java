package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;

public class ThoDaiCa extends Npc {

    public ThoDaiCa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
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
                    "Hôm nay ta rảnh nên sẽ cho ngươi 1 điều ước",
                    "Ước bằng\n1000 ngọc", "Ước bằng\n99 cà rốt", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || this.mapId != 5) {
            return;
        }

        if (player.iDMark.isBaseMenu() && EventManager.TRUNG_THU) {
            switch (select) {
                case 0 ->
                    createOtherMenu(player, 2,
                            "1000 ngọc cũng được, hãy ước đi thể hiện mình là đại gia.",
                            "Đồng ý", "Từ chối");
                case 1 ->
                    createOtherMenu(player, 3,
                            "99 cà rốt cũng được, loại thượng phẩm à, ngươi ước đi",
                            "Đồng ý", "Từ chối");
            }
        }
        if (player.iDMark.getIndexMenu() == 2) {
            switch (select) {
                case 0 ->
                    handleuoc1000ngoc(player);
            }
        }
        if (player.iDMark.getIndexMenu() == 3) {
            switch (select) {
                case 0 ->
                    handleuoc99carot(player);
            }
        }
    }

    private void handleuoc1000ngoc(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy.");
            return;
        }

        // kiểm tra ruby
        if (player.inventory.ruby < 1000) {
            Service.gI().sendThongBao(player, "Bạn cần 1000 hồng ngọc để ước.");
            return;
        }

        // trừ ruby
        player.inventory.ruby -= 1000;
        Service.gI().sendMoney(player);

        // --- Tỉ lệ item ---
        Map<Short, Integer> itemWeight = Map.of((short) 1047, 80);
        short itemId = getRandomItemByWeight(itemWeight);

        Item newItem = ItemService.gI().createNewItem(itemId, 1);

        // --- Option theo item ---
        Map<Short, List<ItemOptionConfig>> optionMap = Map.of(
                (short) 1047, List.of(
                        new ItemOptionConfig(50, 7, 12, true),
                        new ItemOptionConfig(77, 7, 12, true),
                        new ItemOptionConfig(103, 7, 12, true),
                        new ItemOptionConfig(30, 0, false)
                )
        );

        applyOptions(newItem, optionMap.get(itemId));

        InventoryService.gI().addItemBag(player, newItem);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name);
    }

    private void handleuoc99carot(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy.");
            return;
        }

        Item carot = InventoryService.gI().finditemnguyenlieucarot(player);
        if (carot == null) {
            Service.gI().sendThongBao(player, "Cần 99 cà rốt để ước");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(player, carot, 99);

        // --- Tỉ lệ item ---
        Map<Short, Integer> itemWeight = Map.of((short) 1047, 80);
        short itemId = getRandomItemByWeight(itemWeight);

        Item newItem = ItemService.gI().createNewItem(itemId, 1);

        // --- Option theo item ---
        Map<Short, List<ItemOptionConfig>> optionMap = Map.of(
                (short) 1047, List.of(
                        new ItemOptionConfig(50, 7, 12, true),
                        new ItemOptionConfig(77, 7, 12, true),
                        new ItemOptionConfig(103, 7, 12, true),
                        new ItemOptionConfig(30, 0, false)
                )
        );

        applyOptions(newItem, optionMap.get(itemId));

        InventoryService.gI().addItemBag(player, newItem);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + newItem.template.name);
    }

    /**
     * Random item theo tỉ lệ trọng số
     */
    private short getRandomItemByWeight(Map<Short, Integer> weights) {
        int total = weights.values().stream().mapToInt(i -> i).sum();
        int r = ThreadLocalRandom.current().nextInt(total);
        for (var e : weights.entrySet()) {
            if (r < e.getValue()) {
                return e.getKey();
            }
            r -= e.getValue();
        }
        return 0;
    }

    /**
     * Áp dụng option cho item
     */
    private void applyOptions(Item item, List<ItemOptionConfig> configs) {
        if (configs == null) {
            return;
        }
        for (ItemOptionConfig cfg : configs) {
            int value = cfg.isRandom
                    ? ThreadLocalRandom.current().nextInt(cfg.min, cfg.max + 1)
                    : cfg.min;
            item.itemOptions.add(new ItemOption(cfg.id, value));
        }
    }

    /**
     * Cấu hình option item
     */
    private static class ItemOptionConfig {

        int id, min, max;
        boolean isRandom;

        ItemOptionConfig(int id, int min, int max, boolean isRandom) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.isRandom = isRandom;
        }

        ItemOptionConfig(int id, int value, boolean isRandom) {
            this(id, value, value, isRandom);
        }
    }
}
