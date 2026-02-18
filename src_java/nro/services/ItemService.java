package nro.services;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import models.Template;
import models.Template.ItemOptionTemplate;
import item.Item;
import map.ItemMap;
import nro.player.Player;
import shop.ItemShop;
import nro.server.Manager;
import utils.TimeUtil;
import utils.Util;
import item.Item.ItemOption;

import java.util.*;
import java.util.stream.Collectors;
import map.Zone;
import models.Combine.CombineService;
import shop.Shop;
import shop.TabShop;

public class ItemService {

    private static ItemService i;

    public static ItemService gI() {
        if (i == null) {
            i = new ItemService();
        }
        return i;
    }

    public short getItemIdByIcon(short IconID) {
        for (int i = 0; i < Manager.ITEM_TEMPLATES.size(); i++) {
            if (Manager.ITEM_TEMPLATES.get(i).iconID == IconID) {
                return Manager.ITEM_TEMPLATES.get(i).id;
            }
        }
        return -1;
    }
    
    public Item copyItemGift(Item item) {
        Item it = new Item();
        it.itemOptions = new ArrayList<>();
        it.template = item.template;
        it.info = item.info;
        it.content = item.content;
        it.quantity = item.quantity;
        it.createTime = item.createTime;
        it.itemOptions.addAll(item.itemOptions);
        it.message = item.message;
        return it;
    }

    public Item createItemNull() {
        Item item = new Item();
        return item;
    }

    public Item createItemFromItemShop(ItemShop itemShop) {
        Item item = new Item();
        item.template = itemShop.temp;
        item.quantity = 1;
        item.content = item.getContent();
        item.info = item.getInfo();
        for (Item.ItemOption io : itemShop.options) {
            item.itemOptions.add(new Item.ItemOption(io));
        }
        return item;
    }

    public Item copyItem(Item item) {
        Item it = new Item();
        it.itemOptions = new ArrayList<>();
        it.template = item.template;
        it.info = item.info;
        it.content = item.content;

        it.quantity = item.quantity;
        it.createTime = item.createTime;
        for (Item.ItemOption io : item.itemOptions) {
            it.itemOptions.add(new Item.ItemOption(io));
        }
        return it;
    }

    public Item createNewItem(short tempId) {
        return createNewItem(tempId, 1);
    }

    public Item otpts(short tempId) {
        return otpts(tempId, 1);
    }

    public Item createNewItemLock(int tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        item.itemOptions.add(new ItemOption(30, 1));
        return item;
    }

    public Item createNewItem(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();

        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item otpts(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        if (item.template.type == 0) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(47, Util.nextInt(2000, 2500)));
        }
        if (item.template.type == 1) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(22, Util.nextInt(150, 200)));
        }
        if (item.template.type == 2) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(0, Util.nextInt(18000, 20000)));
        }
        if (item.template.type == 3) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(23, Util.nextInt(150, 200)));
        }
        if (item.template.type == 4) {
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(14, Util.nextInt(20, 25)));
        }
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item createItemSetKichHoat(int tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.itemOptions = createItemNull().itemOptions;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item createItemDoHuyDiet(int tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.itemOptions = createItemNull().itemOptions;
        item.createTime = System.currentTimeMillis();
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item createItemFromItemMap(ItemMap itemMap) {
        Item item = createNewItem(itemMap.itemTemplate.id, itemMap.quantity);
        item.itemOptions = itemMap.options;
        return item;
    }

    public ItemOptionTemplate getItemOptionTemplate(int id) {
        return Manager.ITEM_OPTION_TEMPLATES.get(id);
    }

    public Template.ItemTemplate getTemplate(int id) {
        return Manager.ITEM_TEMPLATES.get(id);
    }

    public boolean isItemActivation(Item item) {
        return false;
    }

    public int getPercentTrainArmor(Item item) {
        if (item != null) {
            switch (item.template.id) {
                case 529:
                case 534:
                    return 10;
                case 530:
                case 535:
                    return 20;
                case 531:
                case 536:
                    return 30;
                case 1751:
                    return 40;
                default:
                    return 0;
            }
        } else {
            return 0;
        }
    }

    public boolean isTrainArmor(Item item) {
        if (item != null) {
            switch (item.template.id) {
                case 529:
                case 534:
                case 530:
                case 535:
                case 531:
                case 536:
                case 1751:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    public boolean isOutOfDateTime(Item item) {
        if (item != null) {
            for (Item.ItemOption io : item.itemOptions) {
                if (io.optionTemplate.id == 93) {
                    int dayPass = (int) TimeUtil.diffDate(new Date(), new Date(item.createTime), TimeUtil.DAY);
                    if (dayPass != 0) {
                        io.param -= dayPass;
                        if (io.param <= 0) {
                            return true;
                        } else {
                            item.createTime = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
        return false;
    }

    public int randomSKHId(byte gender) {
        if (gender == 3) {
            gender = 2;
        }
        int[][] options = {{128, 129, 127}, {131, 132, 130}, {133, 135, 134}};
        int skhv1 = 25;
        int skhv2 = 35;
        int skhc = 40;
        int skhId = -1;
        int rd = Util.nextInt(1, 100);
        if (rd <= skhv1) {
            skhId = 0;
        } else if (rd <= skhv1 + skhv2) {
            skhId = 1;
        } else if (rd <= skhv1 + skhv2 + skhc) {
            skhId = 2;
        }
        if (gender == 0 && skhId == 0 && Util.isTrue(50, 100)) {
            return 214;
        }
        return options[gender][skhId];
    }

    public void OpenItem736(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 50;
            int ruby = 20;
            int dbv = 10;
            int vb = 10;
            int bh = 5;
            int ct = 5;
            Item item = randomRac();
            if (rd <= rac) {
                item = randomRac();
            } else if (rd <= rac + ruby) {
                item = createItemSetKichHoat(861, 1);
            } else if (rd <= rac + ruby + dbv) {
                item = daBaoVe();
            } else if (rd <= rac + ruby + dbv + vb) {
                item = vanBay2011(true);
            } else if (rd <= rac + ruby + dbv + vb + bh) {
                item = phuKien2011(true);
            } else if (rd <= rac + ruby + dbv + vb + bh + ct) {
                item = caitrang2011(true);
            }
            if (item.template.id == 861) {
                item.quantity = Util.nextInt(10, 30);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            player.inventory.event++;
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void OpenItem648(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 50;
            int ruby = 20;
            int dbv = 10;
            int vb = 10;
            int bh = 5;
            int ct = 5;
            Item item = randomRac();
            if (rd <= rac) {
                item = randomRac2();
            } else if (rd <= rac + ruby) {
                item = createItemSetKichHoat(861, 1);
            } else if (rd <= rac + ruby + dbv) {
                item = vatphamsk(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = vanBayChrimas(true);
            } else if (rd <= rac + ruby + dbv + vb + bh) {
                item = phuKienChristmas(true);
            } else if (rd <= rac + ruby + dbv + vb + bh + ct) {
                item = caitrangChristmas(true);
            }
            if (item.template.id == 861) {
                item.quantity = Util.nextInt(10, 30);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            player.inventory.event++;
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Item itemSKH(int itemId, int skhId) {
        Item item = createItemSetKichHoat(itemId, 1);
        if (item != null) {
            item.itemOptions.addAll(ItemService.gI().getListOptionItemShop((short) itemId));
            item.itemOptions.add(new Item.ItemOption(skhId, 1));
            item.itemOptions.add(new Item.ItemOption(optionIdSKH(skhId), 1));
            item.itemOptions.add(new Item.ItemOption(30, 1));
        }
        return item;
    }

    public Item itemSKHCU(int itemId, int skhId) {
        Item item = createItemSetKichHoat(itemId, 1);
        if (item != null) {
            item.itemOptions.addAll(ItemService.gI().getListOptionItemShop((short) itemId));
            item.itemOptions.add(new Item.ItemOption(skhId, 1));
            item.itemOptions.add(new Item.ItemOption(optionIdSKHCU(skhId), 1));
            item.itemOptions.add(new Item.ItemOption(30, 1));
        }
        return item;
    }

    public int optionItemSKH(int typeItem) {
        switch (typeItem) {
            case 0:
                return 47;
            case 1:
                return 6;
            case 2:
                return 0;
            case 3:
                return 7;
            default:
                return 14;
        }
    }

    public int pagramItemSKH(int typeItem) {
        switch (typeItem) {
            case 0:
            case 2:
                return Util.nextInt(5);
            case 1:
            case 3:
                return Util.nextInt(20, 30);
            default:
                return Util.nextInt(3);
        }
    }

    public int optionIdSKH(int skhId) {
        switch (skhId) {
            case 127: //Set Taiyoken
                return 139;
            case 128: //Set Genki
                return 140;
            case 129: //Set Kamejoko
                return 141;
            case 130: //Set KI
                return 142;
            case 131: //Set Dame
                return 143;
            case 132: //Set Summon
                return 144;
            case 133: //Set Galick
                return 136;
            case 134: //Set Monkey
                return 137;
            case 135: //Set HP
                return 138;
            case 250: //Set Kaioken
                return 253;
            case 251: //Set Lien Hoàn
                return 254;

        }
        return 0;
    }

    public Item itemDHD(int itemId, int dhdId) {
        Item item = createItemSetKichHoat(itemId, 1);
        if (item != null) {
            item.itemOptions.add(new Item.ItemOption(dhdId, 1));
            item.itemOptions.add(new Item.ItemOption(optionIdDHD(dhdId), 1));
            item.itemOptions.add(new Item.ItemOption(30, 1));
        }
        return item;
    }

    public int optionIdDHD(int skhId) {
        switch (skhId) {
            case 127: //Set Taiyoken
                return 139;
            case 128: //Set Genki
                return 140;
            case 129: //Set Kamejoko
                return 141;
            case 130: //Set KI
                return 142;
            case 131: //Set Dame
                return 143;
            case 132: //Set Summon
                return 144;
            case 133: //Set Galick
                return 136;
            case 134: //Set Monkey
                return 137;
            case 135: //Set HP
                return 138;
            case 250: //Set Kaioken
                return 253;
            case 251: //Set Lien Hoàn
                return 254;

        }
        return 0;
    }

    public Item randomCS_DHD(int itemId, int gender) {
        Item it = createItemSetKichHoat(itemId, 1);
        List<Integer> ao = Arrays.asList(650, 652, 654);
        List<Integer> quan = Arrays.asList(651, 653, 655);
        List<Integer> gang = Arrays.asList(657, 659, 661);
        List<Integer> giay = Arrays.asList(658, 660, 662);
        int nhd = 656;
        if (ao.contains(itemId)) {
            it.itemOptions.add(new Item.ItemOption(47, Util.highlightsItem(gender == 2, new Random().nextInt(1001) + 1800))); // áo từ 1800-2800 giáp
        }
        if (quan.contains(itemId)) {
            it.itemOptions.add(new Item.ItemOption(22, Util.highlightsItem(gender == 0, new Random().nextInt(16) + 85))); // hp 85-100k
        }
        if (gang.contains(itemId)) {
            it.itemOptions.add(new Item.ItemOption(0, Util.highlightsItem(gender == 2, new Random().nextInt(150) + 8500))); // 8500-10000
        }
        if (giay.contains(itemId)) {
            it.itemOptions.add(new Item.ItemOption(23, Util.highlightsItem(gender == 1, new Random().nextInt(11) + 80))); // ki 80-90k
        }
        if (nhd == itemId) {
            it.itemOptions.add(new Item.ItemOption(14, new Random().nextInt(3) + 17)); //chí mạng 17-19%
        }
        it.itemOptions.add(new Item.ItemOption(21, 80));// yêu cầu sm 80 tỉ
        it.itemOptions.add(new Item.ItemOption(30, 1));// ko the gd
        return it;
    }

    //Cải trang sự kiện 20/11
    public Item caitrang2011(boolean rating) {
        Item item = createItemSetKichHoat(680, 1);
        item.itemOptions.add(new Item.ItemOption(76, 1));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 28));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 25));//ki 25%
        item.itemOptions.add(new Item.ItemOption(147, 24));//sd 26%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    //Cải trang sự kiện giáng sinh
    public Item caitrangChristmas(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(386, 394), 1);
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 51)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(15, 51)));
        item.itemOptions.add(new Item.ItemOption(147, Util.nextInt(15, 20)));
        item.itemOptions.add(new Item.ItemOption(95, Util.nextInt(15, 51)));
        item.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 30)));
        item.itemOptions.add(new Item.ItemOption(106, 0));//sd 26%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    //610 - bong hoa
    //Phụ kiện bó hoa 20/11
    public Item phuKien2011(boolean rating) {
        Item item = createItemSetKichHoat(954, 1);
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(5) + 5));
        item.itemOptions.add(new Item.ItemOption(147, new Random().nextInt(5) + 5));
        if (Util.isTrue(1, 100)) {
            item.itemOptions.get(Util.nextInt(item.itemOptions.size() - 1)).param = 10;
        }
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public void OpenCapsuleCaiTrang(Player player, Item itemUse) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        Item item = ItemService.gI().createNewItem((short) Util.nextInt(423, 433));
        item.itemOptions.add(new Item.ItemOption(50, Util.nextInt(10, 15)));
        item.itemOptions.add(new Item.ItemOption(77, Util.nextInt(10, 15)));
        item.itemOptions.add(new Item.ItemOption(103, Util.nextInt(10, 15)));
        item.itemOptions.add(new Item.ItemOption(93, itemUse.template.id == 962 ? 5 : 7));
        CombineService.gI().sendEffectOpenItem(player, itemUse.template.iconID, item.template.iconID);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
        InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
    }

    public void OpenItem1805(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] babyThree = {1802, 1803, 1804};
            short[] babyThreeSecret = {1801};
            byte index = (byte) Util.nextInt(0, babyThree.length - 1);
            byte indexbabyThreeSecret = (byte) Util.nextInt(0, babyThreeSecret.length - 1);
            short[] icon = new short[2];
            icon[0] = itemUse.template.iconID;
            if (Util.isTrue(98, 100)) {
                Item it = ItemService.gI().createNewItem(babyThree[index]);
                it.itemOptions.add(new ItemOption(77, Util.nextInt(13, 20)));
                it.itemOptions.add(new ItemOption(5, Util.nextInt(13, 20)));
                it.itemOptions.add(new ItemOption(14, Util.nextInt(13, 20)));
                if (Util.isTrue(99, 100)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(player, it);
                Service.gI().sendThongBao(player, "Bạn Nhận Được " + it.template.name);
                icon[1] = it.template.iconID;
            } else {
                Item it = ItemService.gI().createNewItem(babyThreeSecret[indexbabyThreeSecret]);
                it.itemOptions.add(new ItemOption(77, Util.nextInt(20, 22)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(20, 22)));
                it.itemOptions.add(new ItemOption(94, Util.nextInt(5, 15)));
                it.itemOptions.add(new ItemOption(5, Util.nextInt(5, 15)));
                it.itemOptions.add(new ItemOption(14, Util.nextInt(5, 15)));
                if (Util.isTrue(99, 100)) {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
                }
                InventoryService.gI().addItemBag(player, it);
                Service.gI().sendThongBao(player, "Bạn Nhận Được " + it.template.name);
                icon[1] = it.template.iconID;
            }
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Item phuKienChristmas(boolean rating) {
        Item item = createItemSetKichHoat(745, 1);
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(25) + 5));
        item.itemOptions.add(new Item.ItemOption(147, new Random().nextInt(25) + 5));
        if (Util.isTrue(1, 100)) {
            item.itemOptions.get(Util.nextInt(item.itemOptions.size() - 1)).param = 10;
        }
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public Item vanBay2011(boolean rating) {
        Item item = createItemSetKichHoat(795, 1);
        item.itemOptions.add(new Item.ItemOption(89, 1));
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(950, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public void OpenItem1576(Player player, Item itemUse) {
        try {
            if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
                return;
            }
            short[] icon = new short[2];
            int rd = Util.nextInt(1, 100);
            int rac = 20;
            int ruby = 20;
            int dbv = 20;
            int vb = 20;
            int bh = 20;
            Item item = daolong(true);
            if (rd <= rac) {
                item = daolong(true);
            } else if (rd <= rac + ruby) {
                item = bena(true);
            } else if (rd <= rac + ruby + dbv) {
                item = cacheprong(true);
            } else if (rd <= rac + ruby + dbv + vb) {
                item = caitrangtet(true);
            }
            icon[0] = itemUse.template.iconID;
            icon[1] = item.template.iconID;
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            player.event.addEventPointPhaobong(1);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Item bena(boolean rating) {
        Item item = createItemSetKichHoat(1760, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(15) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item cacheprong(boolean rating) {
        Item item = createItemSetKichHoat(1487, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(84, new Random().nextInt(15) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item caitrangtet(boolean rating) {
        Item item = createItemSetKichHoat(Util.nextInt(1498, 1500), 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(30) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(30) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(30) + 5));
        item.itemOptions.add(new Item.ItemOption(14, new Random().nextInt(1) + 10));
        item.itemOptions.add(new Item.ItemOption(101, new Random().nextInt(10) + 40));
        item.itemOptions.add(new Item.ItemOption(95, new Random().nextInt(10) + 20));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item daolong(boolean rating) {
        Item item = createItemSetKichHoat(1502, 1);
        item.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(15) + 5));
        item.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(15) + 5));
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));// hsd
        }
        return item;
    }

    public Item daBaoVe() {
        Item item = createItemSetKichHoat(987, 1);
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        return item;
    }

    public Item randomRac() {
        short[] racs = {20, 19, 18, 17};
        Item item = createItemSetKichHoat(racs[Util.nextInt(racs.length - 1)], 1);
        if (optionRac(item.template.id) != 0) {
            item.itemOptions.add(new Item.ItemOption(optionRac(item.template.id), 1));
        }
        return item;
    }

    public Item randomRac2() {
        short[] racs = {585, 704, 379, 384, 385, 381, 828, 829, 830, 831, 832, 833, 834, 835, 836, 837, 838, 839, 840, 841, 842, 934, 935};
        int idItem = racs[Util.nextInt(racs.length - 1)];
        if (Util.isTrue(1, 100)) {
            idItem = 956;
        }
        Item item = createItemSetKichHoat(idItem, 1);
        if (optionRac(item.template.id) != 0) {
            item.itemOptions.add(new Item.ItemOption(optionRac(item.template.id), 1));
        }
        return item;
    }

    public Item vanBayChrimas(boolean rating) {
        Item item = createItemSetKichHoat(746, 1);
        item.itemOptions.add(new Item.ItemOption(89, 1));
        item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
        if (Util.isTrue(950, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(3) + 1));//hsd
        }
        return item;
    }

    public byte optionRac(short itemId) {
        switch (itemId) {
            case 220:
                return 71;
            case 221:
                return 70;
            case 222:
                return 69;
            case 224:
                return 67;
            case 223:
                return 68;
            default:
                return 0;
        }
    }

    public Item vatphamsk(boolean hsd) {
        int[] itemId = {954, 955, 952, 953, 924, 860, 742};
        byte[] option = {77, 80, 81, 103, 50, 94, 5};
        byte[] option_v2 = {14, 16, 17, 19, 27, 28, 47, 87}; //77 %hp // 80 //81 //103 //50 //94 //5 % sdcm
        byte optionid = 0;
        byte optionid_v2 = 0;
        byte param = 0;
        Item lt = ItemService.gI().createNewItem((short) itemId[Util.nextInt(itemId.length)]);
        lt.itemOptions.clear();
        optionid = option[Util.nextInt(0, 6)];
        param = (byte) Util.nextInt(5, 15);
        lt.itemOptions.add(new Item.ItemOption(optionid, param));
        if (Util.isTrue(1, 100)) {
            optionid_v2 = option_v2[Util.nextInt(option_v2.length)];
            lt.itemOptions.add(new Item.ItemOption(optionid_v2, param));
        }
        if (Util.isTrue(999, 1000) && hsd) {
            lt.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 7)));
        }
        lt.itemOptions.add(new Item.ItemOption(30, 0));
        return lt;
    }

    public void openBoxVip(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) <= 1) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 2 ô trống hành trang");
            return;
        }
        if (player.inventory.event < 3000) {
            Service.gI().sendThongBao(player, "Bạn không đủ bông...");
            return;
        }
        Item item;
        if (Util.isTrue(45, 100)) {
            item = caitrang2011(false);
        } else {
            item = phuKien2011(false);
        }
        short[] icon = new short[2];
        icon[0] = 6983;
        icon[1] = item.template.iconID;
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        player.inventory.event -= 3000;
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
        CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
    }

    public void giaobong(Player player, int quantity) {
        if (quantity > 10000) {
            return;
        }
        try {
            Item itemUse = InventoryService.gI().findItem(player.inventory.itemsBag, 610);
            if (itemUse.quantity < quantity) {
                Service.gI().sendThongBao(player, "Bạn không đủ bông...");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, itemUse, quantity);
            Item item = createItemSetKichHoat(736, (quantity / 100));
            item.itemOptions.add(new Item.ItemOption(30, 1));//ko the gd
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được x" + (quantity / 100) + " " + item.template.name);
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Bạn không đủ bông...");
        }
    }

    public Item PK_WC(int itemId) {
        Item phukien = createItemSetKichHoat(itemId, 1);
        int co = 983;
        int cup = 982;
        int bong = 966;
        if (cup == itemId) {
            phukien.itemOptions.add(new Item.ItemOption(77, new Random().nextInt(6) + 5)); // hp 5-10%
        }
        if (co == itemId) {
            phukien.itemOptions.add(new Item.ItemOption(103, new Random().nextInt(6) + 5)); // ki 5-10%
        }
        if (bong == itemId) {
            phukien.itemOptions.add(new Item.ItemOption(50, new Random().nextInt(6) + 5)); // sd 5- 10%
        }
        phukien.itemOptions.add(new Item.ItemOption(192, 1));//WORLDCUP
        phukien.itemOptions.add(new Item.ItemOption(193, 1));//(2 món kích hoạt ....)
        if (Util.isTrue(99, 100)) {// tỉ lệ ra hsd
            phukien.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(2) + 1));//hsd
        }
        return phukien;
    }

    //Cải trang Gohan WC
    public Item CT_WC(boolean rating) {
        Item caitrang = createItemSetKichHoat(883, 1);
        caitrang.itemOptions.add(new Item.ItemOption(77, 30));// hp 30%
        caitrang.itemOptions.add(new Item.ItemOption(103, 15));// ki 15%
        caitrang.itemOptions.add(new Item.ItemOption(50, 20));// sd 20%
        caitrang.itemOptions.add(new Item.ItemOption(192, 1));//WORLDCUP
        caitrang.itemOptions.add(new Item.ItemOption(193, 1));//(2 món kích hoạt ....)
        if (Util.isTrue(99, 100) && rating) {// tỉ lệ ra hsd
            caitrang.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(2) + 1));//hsd
        }
        return caitrang;
    }

    public void openDTS(Player player) {
        //check sl đồ tl, đồ hd
        if (player.combine.itemsCombine.stream().filter(item -> item.template.id >= 555 && item.template.id <= 567).count() < 1) {
            Service.gI().sendThongBao(player, "Thiếu đồ thần linh");
            return;
        }
        if (player.combine.itemsCombine.stream().filter(item -> item.template.id >= 650 && item.template.id <= 662).count() < 2) {
            Service.gI().sendThongBao(player, "Thiếu đồ hủy diệt");
            return;
        }
        if (player.combine.itemsCombine.size() != 3) {
            Service.gI().sendThongBao(player, "Thiếu đồ");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemTL = player.combine.itemsCombine.stream().filter(item -> item.template.id >= 555 && item.template.id <= 567).findFirst().get();
            List<Item> itemHDs = player.combine.itemsCombine.stream().filter(item -> item.template.id >= 650 && item.template.id <= 662).collect(Collectors.toList());
            short[][] itemIds = {{1048, 1051, 1054, 1057, 1060}, {1049, 1052, 1055, 1058, 1061}, {1050, 1053, 1056, 1059, 1062}}; // thứ tự td - 0,nm - 1, xd - 2

            Item itemTS = DoThienSu(itemIds[player.gender][itemTL.template.type], player.gender);
            InventoryService.gI().addItemBag(player, itemTS);

            InventoryService.gI().subQuantityItemsBag(player, itemTL, 1);
            itemHDs.forEach(item -> InventoryService.gI().subQuantityItemsBag(player, item, 1));

            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + itemTS.template.name);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
        }
    }

    public Item DoThienSu(int itemId, int gender) {
        Item dots = createItemSetKichHoat(itemId, 1);
        List<Integer> ao = Arrays.asList(1048, 1049, 1050);
        List<Integer> quan = Arrays.asList(1051, 1052, 1053);
        List<Integer> gang = Arrays.asList(1054, 1055, 1056);
        List<Integer> giay = Arrays.asList(1057, 1058, 1059);
        List<Integer> nhan = Arrays.asList(1060, 1061, 1062);
        //áo
        if (ao.contains(itemId)) {
            dots.itemOptions.add(new Item.ItemOption(47, Util.highlightsItem(gender == 2, new Random().nextInt(1201) + 2800))); // áo từ 2800-4000 giáp
        }
        //quần
        if (Util.isTrue(80, 100)) {
            if (quan.contains(itemId)) {
                dots.itemOptions.add(new Item.ItemOption(22, Util.highlightsItem(gender == 0, new Random().nextInt(11) + 120))); // hp 120k-130k
            }
        } else {
            if (quan.contains(itemId)) {
                dots.itemOptions.add(new Item.ItemOption(22, Util.highlightsItem(gender == 0, new Random().nextInt(21) + 130))); // hp 130-150k 15%
            }
        }
        //găng
        if (Util.isTrue(80, 100)) {
            if (gang.contains(itemId)) {
                dots.itemOptions.add(new Item.ItemOption(0, Util.highlightsItem(gender == 2, new Random().nextInt(651) + 9350))); // 9350-10000
            }
        } else {
            if (gang.contains(itemId)) {
                dots.itemOptions.add(new Item.ItemOption(0, Util.highlightsItem(gender == 2, new Random().nextInt(1001) + 10000))); // gang 15% 10-11k -xayda 12k1
            }
        }
        //giày
        if (Util.isTrue(80, 100)) {
            if (giay.contains(itemId)) {
                dots.itemOptions.add(new Item.ItemOption(23, Util.highlightsItem(gender == 1, new Random().nextInt(21) + 90))); // ki 90-110k
            }
        } else {
            if (giay.contains(itemId)) {
                dots.itemOptions.add(new Item.ItemOption(23, Util.highlightsItem(gender == 1, new Random().nextInt(21) + 110))); // ki 110-130k
            }
        }

        if (nhan.contains(itemId)) {
            dots.itemOptions.add(new Item.ItemOption(14, Util.highlightsItem(gender == 1, new Random().nextInt(3) + 18))); // nhẫn 18-20%
        }
        dots.itemOptions.add(new Item.ItemOption(21, 120));
        dots.itemOptions.add(new Item.ItemOption(30, 1));
        return dots;
    }

    public List<Item.ItemOption> getListOptionItemShop(short id) {
        List<Item.ItemOption> list = new ArrayList<>();
        Manager.SHOPS.forEach(shop -> shop.tabShops.forEach(tabShop -> tabShop.itemShops.forEach(itemShop -> {
            if (itemShop.temp.id == id && list.isEmpty()) {
                list.addAll(itemShop.options);
            }
        })));
        return list;
    }

    public int randTempItemKichHoat(int gender) {
        int[][][] items = {{{0, 33}, {1, 41}, {2, 49}}, {{6, 35}, {7, 43}, {8, 51}}, {{27, 30}, {28, 47}, {29, 55}}, {{21, 24}, {22, 46}, {23, 53}}, {{12, 57}, {12, 57}, {12, 57}}};
        // a w j g rd
        int type;
        if (Util.isTrue(10, 100)) {
            type = 4; // rada
        } else if (Util.isTrue(30, 100)) {
            type = 3; // gang
        } else if (Util.isTrue(50, 100)) {
            type = 1; // quan
        } else if (Util.isTrue(70, 100)) {
            type = 0; // ao
        } else {
            type = 2; // giay
        }

        return items[type][gender][Util.nextInt(1)];
    }

    public int[] randOptionItemKichHoat(int gender) {
        int op1;
        int op2;
        switch (gender) {
            case 0 -> {
                if (Util.isTrue(70, 100)) {
                    op1 = 128;
                    op2 = 140;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 127;
                    op2 = 139;
                } else if (Util.isTrue(40, 100)) {
                    op1 = 129;
                    op2 = 141;
                } else {
                    op1 = 250;
                    op2 = 253;
                }
            }
            case 1 -> {
                if (Util.isTrue(70, 100)) {
                    op1 = 130;
                    op2 = 142;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 131;
                    op2 = 143;
                } else if (Util.isTrue(40, 100)) {
                    op1 = 251;
                    op2 = 254;
                } else {
                    op1 = 132;
                    op2 = 144;
                }
            }
            default -> {
                if (Util.isTrue(70, 100)) {
                    op1 = 134;
                    op2 = 137;
                } else if (Util.isTrue(50, 100)) {
                    op1 = 135;
                    op2 = 138;
                } else {
                    op1 = 133;
                    op2 = 136;
                }
            }
        }
        int[] options = {op1, op2};
        return options;
    }

    public int[] randOptionItemKichHoatNew(int gender) {
        int op1;
        int op2;
        int op3;
        int op4;
        switch (gender) {
            case 0 -> {

                op1 = 245;
                op2 = 246;
                op3 = 247;
                op4 = 248;

            }
            case 1 -> {

                op1 = 237;
                op2 = 238;
                op3 = 239;
                op4 = 240;

            }
            default -> {

                op1 = 241;
                op2 = 242;
                op3 = 243;
                op4 = 244;

            }
        }
        int[] options = {op1, op2, op3, op4};
        return options;
    }

    public int optionIdSKHCU(int skhId) {
        switch (skhId) {
            case 127: //Set KAMI Taiyoken
                return 140;
            case 128: //Set KAMI Genki
                return 139;
            case 129: //Set KAMI Kamejoko
                return 141;
            case 130: //Set KAMI KI
                return 142;
            case 131: //Set KAMI Dame
                return 143;
            case 132: //Set KAMI Summon
                return 144;
            case 133: //Set KAMI Galick
                return 136;
            case 134: //Set KAMI Monkey
                return 137;
            case 135: //Set KAMI HP
                return 138;
        }
        return 0;
    }

    public ItemMap randDoTL(Zone zone, int quantity, int x, int y, long id) {
        short idTempTL, type;
        short[] ao = {555, 557, 559};
        short[] quan = {556, 558, 560};
        short[] gang = {562, 564, 566};
        short[] giay = {563, 565, 567};
        short[] nhan = {561};
        short[] options = {30, 34, 35, 36, 86, 87, 208};
        if (Util.isTrue(10, 100)) {
            idTempTL = nhan[0];
            type = 4; // rada
        } else if (Util.isTrue(30, 100)) {
            idTempTL = gang[Util.nextInt(3)];
            type = 2; // gang
        } else if (Util.isTrue(50, 100)) {
            idTempTL = quan[Util.nextInt(3)];
            type = 1; // quan
        } else if (Util.isTrue(70, 100)) {
            idTempTL = ao[Util.nextInt(3)];
            type = 0; // ao
        } else {
            idTempTL = giay[Util.nextInt(3)];
            type = 3; // giay
        }
        int tiLe = Util.nextInt(100, 115);
        List<ItemOption> itemoptions = new ArrayList<>();
        switch (type) {
            case 0 ->
                itemoptions.add(new ItemOption(47, Util.nextInt(800, 900) * tiLe / 100));
            case 1 -> {
                int chiso = Util.nextInt(46000, 49000) * tiLe / 100;
                itemoptions.add(new ItemOption(22, chiso / 1000));
                itemoptions.add(new ItemOption(27, chiso * 125 / 1000));
            }
            case 2 ->
                itemoptions.add(new ItemOption(0, Util.nextInt(4300, 4500) * tiLe / 100));
            case 3 -> {
                int chiso = Util.nextInt(46000, 49000) * tiLe / 100;
                itemoptions.add(new ItemOption(23, chiso / 1000));
                itemoptions.add(new ItemOption(28, chiso * 125 / 1000));
            }
            case 4 ->
                itemoptions.add(new ItemOption(14, Util.nextInt(14, 17) * tiLe / 100));
        }
        if (Util.isTrue(90, 100)) {
            itemoptions.add(new ItemOption(options[Util.nextInt(options.length)], 0));
        }
        itemoptions.add(new ItemOption(21, Util.nextInt(15, 40)));
        ItemMap it = new ItemMap(zone, idTempTL, quantity, x, y, id);
        it.options.clear();
        it.options.addAll(itemoptions);
        return it;
    }

    public int getOptionParamItemShop(short id, int optionId) {
        for (Shop shop : Manager.SHOPS) {
            for (TabShop tabShop : shop.tabShops) {
                for (ItemShop itemShop : tabShop.itemShops) {
                    if (itemShop.temp.id != id) {
                        continue;
                    }
                    for (ItemOption itemOption : itemShop.options) {
                        if (itemOption.optionTemplate.id == optionId) {
                            return itemOption.param;
                        }
                    }
                }
            }
        }
        return -1;
    }

    public List<Item.ItemOption> getListOptionItemShop(int level, int gender, int type) {
        List<Item.ItemOption> list = new ArrayList<>();
        Manager.SHOPS.forEach(shop -> shop.tabShops.forEach(tabShop -> tabShop.itemShops.forEach(itemShop -> {
            if (itemShop.temp.level == level && itemShop.temp.gender == gender && itemShop.temp.type == type && list.isEmpty()) {
                for (ItemOption io : itemShop.options) {
                    list.add(new ItemOption(io.optionTemplate.id, io.param));
                }
            }
        })));
        return list;
    }

    public Item getAngelItem(int gender, int type) {
        int tempId = 1048 + type * 3 + gender;
        Item angelItem = createNewItem((short) tempId);
        for (Item.ItemOption io : getListOptionItemShop(14, type == 4 ? 3 : gender, type)) {
            if (io.isOptionCanUpgrade()) {
                int param = (int) (io.param * 1.2);
                angelItem.itemOptions.add(new ItemOption(io.optionTemplate.id, param));
            }
        }
        int param = switch (type) {
            case 0 ->
                62;
            case 1 ->
                66;
            case 2 ->
                70;
            case 3 ->
                64;
            default ->
                68;
        };
        angelItem.itemOptions.add(new ItemOption(21, param));
        angelItem.itemOptions.add(new ItemOption(30, 0));
        return angelItem;
    }

    public Item otptl(short tempId) {
        return otptl(tempId, 1);
    }

    public Item otphd(short tempId) {
        return otphd(tempId, 1);
    }

    public Item otpKH(short tempId) {
        return otpKH(tempId, 1);
    }

    public Item otptl(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        if (item.template.type == 0) {
            item.itemOptions.add(new ItemOption(21, 19));
            item.itemOptions.add(new ItemOption(47, Util.nextInt(800, 1200)));
        }
        if (item.template.type == 1) {
            item.itemOptions.add(new ItemOption(21, 19));
            item.itemOptions.add(new ItemOption(22, Util.nextInt(24, 28)));
        }
        if (item.template.type == 2) {
            item.itemOptions.add(new ItemOption(21, 19));
            item.itemOptions.add(new ItemOption(0, Util.nextInt(5500, 7800)));
        }
        if (item.template.type == 3) {
            item.itemOptions.add(new ItemOption(21, 19));
            item.itemOptions.add(new ItemOption(23, Util.nextInt(23, 29)));
        }
        if (item.template.type == 4) {
            item.itemOptions.add(new ItemOption(21, 19));
            item.itemOptions.add(new ItemOption(14, Util.nextInt(1, 14)));
        }
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public Item otpKH(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();
        switch (item.template.type) {
            case 0:
                item.itemOptions.add(new ItemOption(47, Util.nextInt(1, 4)));
                break;
            case 1:
                item.itemOptions.add(new ItemOption(6, Util.nextInt(150, 200)));
                break;
            case 2:
                item.itemOptions.add(new ItemOption(0, Util.nextInt(7, 29)));
                break;
            case 3:
                item.itemOptions.add(new ItemOption(7, Util.nextInt(2, 12)));
                break;
            case 4:
                item.itemOptions.add(new ItemOption(14, 1));
                break;
            default:
                break;
        }
        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    private int randomPercentHD() {
        if (Util.isTrue(25, 100)) {
            return Util.nextInt(11, 16); // 11–15%
        } else if (Util.isTrue(25, 75)) {
            return Util.nextInt(5, 11);  // 5–10%
        } else {
            return Util.nextInt(0, 5);   // 0–4%
        }
    }

    public Item otphd(short tempId, int quantity) {
        Item item = new Item();
        item.template = getTemplate(tempId);
        item.quantity = quantity;
        item.createTime = System.currentTimeMillis();

        int percent = randomPercentHD(); // % tăng thêm

        if (item.template.type == 0) { // áo
            int base = 2100;
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(47, base + base * percent / 100));
        }

        if (item.template.type == 1) { // quần
            int base = 80;
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(22, base + base * percent / 100));
        }

        if (item.template.type == 2) { // găng
            int base = 11000;
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(0, base + base * percent / 100));
        }

        if (item.template.type == 3) { // giày
            int base = 82;
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(23, base + base * percent / 100));
        }

        if (item.template.type == 4) { // nhẫn
            int base = 18;
            item.itemOptions.add(new ItemOption(21, 80));
            item.itemOptions.add(new ItemOption(14, base + base * percent / 100));
        }

        item.content = item.getContent();
        item.info = item.getInfo();
        return item;
    }

    public void settlkaio(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{555, 556, 562, 563, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);

            ao.itemOptions.add(new Item.ItemOption(127, 0));

            ao.itemOptions.add(new Item.ItemOption(139, 0));

            ao.itemOptions.add(new Item.ItemOption(30, 0));

            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);

                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được 1 món Thần Linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }

        }
    }

    public void settlgenki(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{555, 556, 562, 563, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(128, 0));

            ao.itemOptions.add(new Item.ItemOption(140, 0));

            ao.itemOptions.add(new Item.ItemOption(30, 0));

            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);

                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Thần Linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }

    }

    public void settlson(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{555, 556, 562, 563, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(129, 0));

            ao.itemOptions.add(new Item.ItemOption(141, 0));

            ao.itemOptions.add(new Item.ItemOption(30, 0));

            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);

                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Thần Linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void settlpico(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);

            int[] dotl = new int[]{557, 558, 564, 565, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(130, 0));

            ao.itemOptions.add(new Item.ItemOption(142, 0));

            ao.itemOptions.add(new Item.ItemOption(30, 0));

            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);

                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Thần Linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void settloctieu(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{557, 558, 564, 565, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(131, 0));
            ao.itemOptions.add(new Item.ItemOption(143, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 4) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Thần Linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void settlpiko(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{557, 558, 564, 565, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(132, 0));
            ao.itemOptions.add(new Item.ItemOption(144, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Thần Linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public Item cadicssj2m(boolean rating) {
        Item item = createItemSetKichHoat(1762, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicssj3(boolean rating) {
        Item item = createItemSetKichHoat(1763, 1);
        item.itemOptions.add(new Item.ItemOption(50, 24));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 24));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 24));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 24));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(19, 20));//ki 25%
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public Item cadicblue(boolean rating) {
        Item item = createItemSetKichHoat(1764, 1);
        item.itemOptions.add(new Item.ItemOption(50, 25));//VIP
        item.itemOptions.add(new Item.ItemOption(77, 25));//hp 28%
        item.itemOptions.add(new Item.ItemOption(103, 25));//ki 25%
        item.itemOptions.add(new Item.ItemOption(106, 0));//ki 25%
        item.itemOptions.add(new Item.ItemOption(5, 25));//stcm 22%
        item.itemOptions.add(new Item.ItemOption(19, 20));//ki 25%
        item.itemOptions.add(new Item.ItemOption(108, Util.nextInt(1, 10)));//ki 25%
        item.itemOptions.add(new Item.ItemOption(14, Util.nextInt(1, 10)));//ki 25%
        if (Util.isTrue(995, 1000) && rating) {// tỉ lệ ra hsd
            item.itemOptions.add(new Item.ItemOption(93, new Random().nextInt(15) + 7));//hsd
        }
        return item;
    }

    public void settlgalick(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{559, 560, 566, 567, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(133, 0));
            ao.itemOptions.add(new Item.ItemOption(136, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món thần linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void settlcadick(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
            int[] dotl = new int[]{559, 560, 566, 567, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(134, 0));
            ao.itemOptions.add(new Item.ItemOption(137, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Thàn linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void settlnappa(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1703 + i);
//            Item ao = ItemService.gI().otptl((short) 559);
//            Item quan = ItemService.gI().otptl((short) 560);
//            Item gang = ItemService.gI().otptl((short) 566);
//            Item giay = ItemService.gI().otptl((short) 567);
//            Item nhan = ItemService.gI().otptl((short) 561);
            int[] dotl = new int[]{559, 560, 566, 567, 561};

            int ramdom = new Random().nextInt(dotl.length);

            Item ao = ItemService.gI().otptl((short) dotl[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(135, 0));
            ao.itemOptions.add(new Item.ItemOption(138, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món thần linh ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdkaio(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{650, 651, 657, 658, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(127, 0));
            ao.itemOptions.add(new Item.ItemOption(139, 0));

            ao.itemOptions.add(new Item.ItemOption(30, 0));

            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);

                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdgenki(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{650, 651, 657, 658, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(128, 0));
            ao.itemOptions.add(new Item.ItemOption(140, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdson(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{650, 651, 657, 658, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(129, 0));
            ao.itemOptions.add(new Item.ItemOption(141, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdpico(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{652, 653, 659, 660, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(130, 0));
            ao.itemOptions.add(new Item.ItemOption(142, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdoctieu(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{652, 653, 659, 660, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(131, 0));
            ao.itemOptions.add(new Item.ItemOption(143, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdpiko(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{652, 653, 659, 660, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(132, 0));
            ao.itemOptions.add(new Item.ItemOption(144, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 4) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy DIệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdcadick(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{654, 655, 661, 662, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(133, 0));
            ao.itemOptions.add(new Item.ItemOption(136, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdcadic(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{654, 655, 661, 662, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(134, 0));
            ao.itemOptions.add(new Item.ItemOption(137, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void sethdnappa(Player player) {
        for (int i = 0; i < 1; i++) {
            Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1704 + i);
            int[] dohd = new int[]{654, 655, 661, 662, 656};

            int ramdom = new Random().nextInt(dohd.length);

            Item ao = ItemService.gI().otphd((short) dohd[ramdom]);
            ao.itemOptions.add(new Item.ItemOption(135, 0));
            ao.itemOptions.add(new Item.ItemOption(138, 0));
            ao.itemOptions.add(new Item.ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(player) > 1) {
                InventoryService.gI().addItemBag(player, ao);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Bạn đã nhận được món Hủy Diệt ");
                InventoryService.gI().subQuantityItemsBag(player, hq, 1);
                InventoryService.gI().sendItemBag(player);
            } else {
                Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            }
        }
    }

    public void setTiemNang(Player player) throws Exception {

        Item ao = ItemService.gI().otpKH((short) ((short) player.gender + 0));
        Item quan = ItemService.gI().otpKH((short) ((short) player.gender + 6));
        Item gang = ItemService.gI().otpKH((short) ((short) player.gender + 21));
        Item giay = ItemService.gI().otpKH((short) ((short) player.gender + 27));
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(255, 0));//129
        quan.itemOptions.add(new ItemOption(255, 0));
        gang.itemOptions.add(new ItemOption(255, 0));
        giay.itemOptions.add(new ItemOption(255, 0));
        rd.itemOptions.add(new ItemOption(255, 0));
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt tiềm năng 100%");
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void OpenSKH(Player player, int itemUseId, int select) throws Exception {
        if (select < 0 || select > 4) {
            return;
        }
        Item itemUse = InventoryService.gI().findItem(player.inventory.itemsBag, itemUseId);
        int[][] items = {{Manager.aotd[Util.nextInt(0, 6)], Manager.quantd[Util.nextInt(0, 6)], Manager.gangtd[Util.nextInt(0, 6)], Manager.giaytd[Util.nextInt(0, 6)], Manager.radaSKHVip[Util.nextInt(0, 6)]},
        {Manager.aonm[Util.nextInt(0, 6)], Manager.quannm[Util.nextInt(0, 6)], Manager.gangnm[Util.nextInt(0, 6)], Manager.giaynm[Util.nextInt(0, 6)], Manager.radaSKHVip[Util.nextInt(0, 6)]},
        {Manager.aoxd[Util.nextInt(0, 6)], Manager.quanxd[Util.nextInt(0, 6)], Manager.gangxd[Util.nextInt(0, 6)], Manager.giayxd[Util.nextInt(0, 6)], Manager.radaSKHVip[Util.nextInt(0, 6)]}};
        int[][] options = {{128, 129, 127}, {130, 131, 132}, {133, 135, 134}};
        int skhv1 = 25;// ti le
        int skhv2 = 35;//ti le
        int skhc = 40;//ti le
        int skhId = -1;

        int rd = Util.nextInt(1, 100);
        if (rd <= skhv1) {
            skhId = 0;
        } else if (rd <= skhv1 + skhv2) {
            skhId = 1;
        } else if (rd <= skhv1 + skhv2 + skhc) {
            skhId = 2;
        }
        Item item = null;
        switch (itemUseId) {
            case 1806:
                item = itemSKHCU(items[0][select], options[0][skhId]);
                break;
            case 1807:
                item = itemSKHCU(items[1][select], options[1][skhId]);
                break;
            case 1808:
                item = itemSKHCU(items[2][select], options[2][skhId]);
                break;
        }
        if (item != null && InventoryService.gI().getCountEmptyBag(player) > 0) {
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
            InventoryService.gI().subQuantityItemsBag(player, itemUse, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
        }
    }
    
    public int randTempItemDoSao(int gender) {
        // Mảng chứa các item theo từng loại (type)
        int[][] ao = {{3, 34, 136, 137, 138, 139}, {4, 42, 152, 153, 154, 155}, {5, 50, 168, 169, 170, 171}};
        int[][] quan = {{9, 36, 140, 141, 142, 143}, {10, 44, 156, 157, 158, 159}, {11, 52, 172, 173, 174, 175}};
        int[][] gang = {{37, 38, 144, 145, 146, 147}, {25, 45, 160, 161, 162, 163}, {26, 54, 176, 177, 178, 179}};
        int[][] giay = {{39, 40, 148, 149, 150, 151}, {31, 48, 164, 165, 166, 167}, {32, 56, 180, 181, 182, 183}};
        int[][] rada = {{58, 59, 184, 185, 186, 187}, {58, 59, 184, 185, 186, 187}, {58, 59, 184, 185, 186, 187}};
        int[][][] item = {ao, gang, quan, giay, rada};

        // Khởi tạo đối tượng Random
        Random random = new Random();

        // Xác định type
        int type;
        if (Util.isTrue(10, 100)) {
            type = 4; // rada
        } else if (Util.isTrue(23, 100)) {
            type = 3; // giay
        } else if (Util.isTrue(23, 100)) {
            type = 1; // ao
        } else if (Util.isTrue(23, 100)) {
            type = 0; // gang
        } else {
            type = 2; // quan
        }

        // Lấy chỉ số ngẫu nhiên từ 0 đến 5 bằng Random
        int index = random.nextInt(6); // Lấy giá trị ngẫu nhiên từ 0 đến 5

        // Trả về phần tử tương ứng
        return item[type][gender][index];
    }
    
    public int randDoSao(int gender) {
        int[][][] items = {
            {{0, 33}, {1, 41}, {2, 49}},
            {{6, 35}, {7, 43}, {8, 51}},
            {{27, 30}, {28, 47}, {29, 55}},
            {{21, 24}, {22, 46}, {23, 53}},
            {{12, 57}, {12, 57}, {12, 57}}
        };

        int rand = Util.nextInt(100);

        int type;
        if (rand < 10) {
            type = 4; // rada (10%)
        } else if (rand < 32) {
            type = 0; // ao (22.5%)
        } else if (rand < 55) {
            type = 1; // quan (22.5%)
        } else if (rand < 77) {
            type = 2; // giày (22.5%)
        } else {
            type = 3; // găng (22.5%)
        }

        return items[type][gender][Util.nextInt(2)];
    }
    public void setSongoku(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 0);
        Item quan = ItemService.gI().otpKH((short) 6);
        Item gang = ItemService.gI().otpKH((short) 21);
        Item giay = ItemService.gI().otpKH((short) 27);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(129, 0));//129
        quan.itemOptions.add(new ItemOption(129, 0));
        gang.itemOptions.add(new ItemOption(129, 0));
        giay.itemOptions.add(new ItemOption(129, 0));
        rd.itemOptions.add(new ItemOption(129, 0));
        
        ao.itemOptions.add(new ItemOption(141, 100));
        quan.itemOptions.add(new ItemOption(141, 100));
        gang.itemOptions.add(new ItemOption(141, 100));
        giay.itemOptions.add(new ItemOption(141, 100));
        rd.itemOptions.add(new ItemOption(141, 100));
        
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Kame");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void setThenXinHang(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 0);
        Item quan = ItemService.gI().otpKH((short) 6);
        Item gang = ItemService.gI().otpKH((short) 21);
        Item giay = ItemService.gI().otpKH((short) 27);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(127, 0));
        quan.itemOptions.add(new ItemOption(127, 0));
        gang.itemOptions.add(new ItemOption(127, 0));
        giay.itemOptions.add(new ItemOption(127, 0));
        rd.itemOptions.add(new ItemOption(127, 0));
        
        ao.itemOptions.add(new ItemOption(139, 100));
        quan.itemOptions.add(new ItemOption(139, 100));
        gang.itemOptions.add(new ItemOption(139, 100));
        giay.itemOptions.add(new ItemOption(139, 100));
        rd.itemOptions.add(new ItemOption(139, 100));
        
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Thên Xin Hăng");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void setKaioKen(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 0);
        Item quan = ItemService.gI().otpKH((short) 6);
        Item gang = ItemService.gI().otpKH((short) 21);
        Item giay = ItemService.gI().otpKH((short) 27);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(128, 0));
        quan.itemOptions.add(new ItemOption(128, 0));
        gang.itemOptions.add(new ItemOption(128, 0));
        giay.itemOptions.add(new ItemOption(128, 0));
        rd.itemOptions.add(new ItemOption(128, 0));
        
        ao.itemOptions.add(new ItemOption(140, 100));
        quan.itemOptions.add(new ItemOption(140, 100));
        gang.itemOptions.add(new ItemOption(140, 100));
        giay.itemOptions.add(new ItemOption(140, 100));
        rd.itemOptions.add(new ItemOption(140, 100));
        
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Kaioken");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }
    public void setLienHoan(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 1);
        Item quan = ItemService.gI().otpKH((short) 7);
        Item gang = ItemService.gI().otpKH((short) 22);
        Item giay = ItemService.gI().otpKH((short) 28);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(131, 0));
        quan.itemOptions.add(new ItemOption(131, 0));
        gang.itemOptions.add(new ItemOption(131, 0));
        giay.itemOptions.add(new ItemOption(131, 0));
        rd.itemOptions.add(new ItemOption(131, 0));

        ao.itemOptions.add(new ItemOption(143, 100));
        quan.itemOptions.add(new ItemOption(143, 100));
        gang.itemOptions.add(new ItemOption(143, 100));
        giay.itemOptions.add(new ItemOption(143, 100));
        rd.itemOptions.add(new ItemOption(143, 100));
        
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Liên hoàn");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void setPicolo(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 1);
        Item quan = ItemService.gI().otpKH((short) 7);
        Item gang = ItemService.gI().otpKH((short) 22);
        Item giay = ItemService.gI().otpKH((short) 28);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(130, 0));
        quan.itemOptions.add(new ItemOption(130, 0));
        gang.itemOptions.add(new ItemOption(130, 0));
        giay.itemOptions.add(new ItemOption(130, 0));
        rd.itemOptions.add(new ItemOption(130, 0));

        ao.itemOptions.add(new ItemOption(142, 100));
        quan.itemOptions.add(new ItemOption(142, 100));
        gang.itemOptions.add(new ItemOption(142, 100));
        giay.itemOptions.add(new ItemOption(142, 100));
        rd.itemOptions.add(new ItemOption(142, 100));
        
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Picolo");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void setPikkoroDaimao(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 1);
        Item quan = ItemService.gI().otpKH((short) 7);
        Item gang = ItemService.gI().otpKH((short) 22);
        Item giay = ItemService.gI().otpKH((short) 28);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(132, 0));
        quan.itemOptions.add(new ItemOption(132, 0));
        gang.itemOptions.add(new ItemOption(132, 0));
        giay.itemOptions.add(new ItemOption(132, 0));
        rd.itemOptions.add(new ItemOption(132, 0));

        ao.itemOptions.add(new ItemOption(144, 100));
        quan.itemOptions.add(new ItemOption(144, 100));
        gang.itemOptions.add(new ItemOption(144, 100));
        giay.itemOptions.add(new ItemOption(144, 100));
        rd.itemOptions.add(new ItemOption(144, 100));
        
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Picolo");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }
    public void setKakarot(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 2);
        Item quan = ItemService.gI().otpKH((short) 8);
        Item gang = ItemService.gI().otpKH((short) 23);
        Item giay = ItemService.gI().otpKH((short) 29);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(133, 0));
        quan.itemOptions.add(new ItemOption(133, 0));
        gang.itemOptions.add(new ItemOption(133, 0));
        giay.itemOptions.add(new ItemOption(133, 0));
        rd.itemOptions.add(new ItemOption(133, 0));

        ao.itemOptions.add(new ItemOption(136, 100));
        quan.itemOptions.add(new ItemOption(136, 100));
        gang.itemOptions.add(new ItemOption(136, 100));
        giay.itemOptions.add(new ItemOption(136, 100));
        rd.itemOptions.add(new ItemOption(136, 100));
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Kakarot");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void setCadic(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 2);
        Item quan = ItemService.gI().otpKH((short) 8);
        Item gang = ItemService.gI().otpKH((short) 23);
        Item giay = ItemService.gI().otpKH((short) 29);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(134, 0));
        quan.itemOptions.add(new ItemOption(134, 0));
        gang.itemOptions.add(new ItemOption(134, 0));
        giay.itemOptions.add(new ItemOption(134, 0));
        rd.itemOptions.add(new ItemOption(134, 0));

        ao.itemOptions.add(new ItemOption(137, 1));
        quan.itemOptions.add(new ItemOption(137, 1));
        gang.itemOptions.add(new ItemOption(137, 1));
        giay.itemOptions.add(new ItemOption(137, 1));
        rd.itemOptions.add(new ItemOption(137, 1));
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Ca đíc");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }

    public void setNappa(Player player) throws Exception {
        Item hq = InventoryService.gI().findItem(player.inventory.itemsBag, 1655);
        Item ao = ItemService.gI().otpKH((short) 2);
        Item quan = ItemService.gI().otpKH((short) 8);
        Item gang = ItemService.gI().otpKH((short) 23);
        Item giay = ItemService.gI().otpKH((short) 29);
        Item rd = ItemService.gI().otpKH((short) 12);
        ao.itemOptions.add(new ItemOption(135, 0));
        quan.itemOptions.add(new ItemOption(135, 0));
        gang.itemOptions.add(new ItemOption(135, 0));
        giay.itemOptions.add(new ItemOption(135, 0));
        rd.itemOptions.add(new ItemOption(135, 0));

        ao.itemOptions.add(new ItemOption(138, 80));
        quan.itemOptions.add(new ItemOption(138, 80));
        gang.itemOptions.add(new ItemOption(138, 80));
        giay.itemOptions.add(new ItemOption(138, 80));
        rd.itemOptions.add(new ItemOption(138, 80));
        ao.itemOptions.add(new ItemOption(30, 0));
        quan.itemOptions.add(new ItemOption(30, 0));
        gang.itemOptions.add(new ItemOption(30, 0));
        giay.itemOptions.add(new ItemOption(30, 0));
        rd.itemOptions.add(new ItemOption(30, 0));
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            InventoryService.gI().addItemBag(player, ao);
            InventoryService.gI().addItemBag(player, quan);
            InventoryService.gI().addItemBag(player, gang);
            InventoryService.gI().addItemBag(player, giay);
            InventoryService.gI().addItemBag(player, rd);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn đã nhận được set Kích hoạt Nappa");
            InventoryService.gI().subQuantityItemsBag(player, hq, 1);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 5 ô trống hành trang");
        }
    }
}
