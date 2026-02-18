package nro.player;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstTaskBadges;
import java.util.ArrayList;
import java.util.List;
import item.Item;
import item.Item.ItemOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import task.Badges.BadgesTaskService;

public class Inventory {

    public static final byte TYPE_NORMAL_BOX = 0;
    public static final long LIMIT_GOLD = 1_000_000_000_000L;
    public static final int MAX_ITEMS_BAG = 100;
    public static final int MAX_ITEMS_BOX = 100;
    public static final byte TYPE_COLLECTION_BOX = 1;
    public static final int PRICE_SLOT_COLLECTION_BOX = 1_500_000_000;
    public static final byte MAX_ITEM_BOX_COLLECTION = 40;
    public Item trainArmor;
    public List<String> giftCode;
    public List<Item> itemsBody;
    public List<Item> itemsBag;
    public List<Item> itemsBox;

    public List<Item> itemsMailBox;
    public List<Item> itemsBoxCrackBall;
    public List<Item> itemsDaBan;
    public List<Item> itemsBoxCollection;

    public long gold;
    public int gem;
    public int ruby;
    public int coupon;
    public int event;
    
    public int getGem() {
        return this.gem;
    }

    public Inventory() {
        itemsBody = new ArrayList<>();
        itemsBag = new ArrayList<>();
        itemsBox = new ArrayList<>();
        itemsBoxCrackBall = new ArrayList<>();
        itemsMailBox = new ArrayList<>();
        itemsDaBan = new ArrayList<>();
        giftCode = new ArrayList<>();
        itemsBoxCollection = new ArrayList<>();
    }
    
    public void checkAndUpdateMeRongBadges(Player player) {
        Set<Integer> checkedItemIds = new HashSet<>();

        List<List<Item>> inventories = Arrays.asList(
                this.itemsBag,
                this.itemsBox,
                this.itemsBody
        );

        for (List<Item> inventory : inventories) {
            for (Item item : inventory) {
                if (item != null && isPermanent(item)) {
                    int itemId = item.template.id;
                    if (itemId >= 1765 && itemId <= 1771 && !checkedItemIds.contains(itemId)) {
                        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.ME_RONG, 1);
                        checkedItemIds.add(itemId);
                    }
                }
            }
        }
    }
    
    private boolean isPermanent(Item item) {
        return item != null && item.getOptionById(93) == null;
    }

    public int getGemAndRuby() {
        return this.gem + this.ruby;
    }
    
    public long getGold() {
        return this.gold;
    }

    public int getParam(Item it, int id) {
        for (ItemOption op : it.itemOptions) {
            if (op != null && op.optionTemplate.id == id) {
                return op.param;
            }
        }
        return 0;
    }

    public boolean haveOption(List<Item> l, int index, int id) {
        Item it = l.get(index);
        if (it != null && it.isNotNullItem()) {
            return it.itemOptions.stream().anyMatch(op -> op != null && op.optionTemplate.id == id);
        }
        return false;
    }
    
    public void subGem(int num) {
        this.gem -= num;
    }
    
    public void subGold(int num) {
        this.gold -= num;
    }

    public void subGemAndRuby(int num) {
        this.ruby -= num;
        if (this.ruby < 0) {
            this.gem += this.ruby;
            this.ruby = 0;
        }
    }

    public void addGold(int gold) {
        this.gold += gold;
        if (this.gold > LIMIT_GOLD) {
            this.gold = LIMIT_GOLD;
        }
    }

    public void dispose() {
        if (this.trainArmor != null) {
            this.trainArmor.dispose();
        }
        this.trainArmor = null;
        if (this.itemsBody != null) {
            for (Item it : this.itemsBody) {
                it.dispose();
            }
            this.itemsBody.clear();
        }
        if (this.itemsBag != null) {
            for (Item it : this.itemsBag) {
                it.dispose();
            }
            this.itemsBag.clear();
        }
        if (this.itemsBox != null) {
            for (Item it : this.itemsBox) {
                it.dispose();
            }
            this.itemsBox.clear();
        }
        if (this.itemsBoxCrackBall != null) {
            for (Item it : this.itemsBoxCrackBall) {
                it.dispose();
            }
            this.itemsBoxCrackBall.clear();
        }
        if (this.itemsMailBox != null) {
            for (Item it : this.itemsMailBox) {
                it.dispose();
            }
            this.itemsMailBox.clear();
        }
        if (this.itemsDaBan != null) {
            for (Item it : this.itemsDaBan) {
                it.dispose();
            }
            this.itemsDaBan.clear();
        }
        if (this.itemsBoxCollection != null) {
            this.itemsBoxCollection.forEach(Item::dispose);
            this.itemsBoxCollection.clear();
        }
        this.itemsBody = null;
        this.itemsBag = null;
        this.itemsBox = null;
        this.itemsBoxCrackBall = null;
        this.itemsMailBox = null;
        this.itemsDaBan = null;
    }

}
