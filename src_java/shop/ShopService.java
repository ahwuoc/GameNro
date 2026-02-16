package shop;

import player.badges.BadgesData;
import player.badges.BagesTemplate;

import consts.ConstAchievement;
import item.Item;
import npc.specialnpc.MagicTree;
import player.Inventory;
import player.Player;
import network.Message;
import jdbc.daos.PlayerDAO;
import item.Item.ItemOption;

import java.util.ArrayList;

import player.badges.BadgesService;
import player.badges.BagesTemplate;
import server.Manager;
import services.InventoryService;
import services.ItemService;
import services.Service;
import utils.Logger;
import utils.Util;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import models.Achievement.AchievementService;
import services.func.Input;
import services.func.VatPhamDaBan;

public class ShopService {

    private static final byte COST_GOLD = 0;
    private static final byte COST_GEM = 1;
    private static final byte COST_RUBY = 3;
    private static final byte COST_COUPON = 4;

    private static final byte NORMAL_SHOP = 0;
    private static final byte SPEC_SHOP = 3;

    private static ShopService I;

    public static ShopService gI() {
        if (ShopService.I == null) {
            ShopService.I = new ShopService();
        }
        return ShopService.I;
    }

    public void opendShop(Player player, String tagName, boolean allGender) {
        Logger.log("[ShopService] opendShop called - player: " + player.name + ", tagName: " + tagName + ", allGender: "
                + allGender);
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            openShopType4(player, tagName, player.inventory.itemsBoxCrackBall);
            return;
        } else if (tagName.equals("ITEMS_MAIL_BOX")) {
            openShopType4(player, tagName, player.inventory.itemsMailBox);
            return;
        } else if (tagName.equals("ITEMS_BADGES")) {
            openShopDanhHieu(player, tagName, player.dataBadges);
            return;
        } else if (tagName.equals("ITEMS_DABAN")) {
            openShopType8(player, tagName, player.inventory.itemsDaBan);
            return;
        }
        try {
            Shop shop = this.getShop(tagName);
            for (TabShop tabShop : shop.tabShops) {
                for (ItemShop item : tabShop.itemShops) {
                    switch (item.temp.id) {
                        case 1627:// hành trang
                            if (player.inventory.itemsBag.size() >= 35) {
                                item.cost = ((player.inventory.itemsBag.size() - 35) + 1) * 2;
                            } else {
                                item.cost = 1;
                            }
                            break;
                    }
                }
            }
            shop = this.resolveShop(player, shop, allGender);
            switch (shop.typeShop) {
                case NORMAL_SHOP:
                    openShopType0(player, shop);
                    break;
                case SPEC_SHOP:
                    openShopType3(player, shop);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Service.gI().sendThongBao(player, ex.getMessage());
        }
    }

    private Shop getShop(String tagName) throws Exception {
        for (Shop s : Manager.SHOPS) {
            if (s.tagName != null && s.tagName.equals(tagName)) {
                return s;
            }
        }
        throw new Exception("Shop " + tagName + " không tồn tại!");
    }

    private Shop resolveShop(Player player, Shop shop, boolean allGender) {
        if (shop.tagName != null
                && (shop.tagName.equals("BUA_1H") || shop.tagName.equals("BUA_8H") || shop.tagName.equals("BUA_1M"))) {
            return this.resolveShopBua(player, new Shop(shop));
        }
        return allGender ? new Shop(shop) : new Shop(shop, player);
    }

    private Shop resolveShopBua(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                long min = 0;
                switch (item.temp.id) {
                    case 213:
                        long timeTriTue = player.charms.tdTriTue;
                        long current = System.currentTimeMillis();
                        min = (timeTriTue - current) / 60000;

                        break;
                    case 214:
                        min = (player.charms.tdManhMe - System.currentTimeMillis()) / 60000;
                        break;
                    case 215:
                        min = (player.charms.tdDaTrau - System.currentTimeMillis()) / 60000;
                        break;
                    case 216:
                        min = (player.charms.tdOaiHung - System.currentTimeMillis()) / 60000;
                        break;
                    case 217:
                        min = (player.charms.tdBatTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 218:
                        min = (player.charms.tdDeoDai - System.currentTimeMillis()) / 60000;
                        break;
                    case 219:
                        min = (player.charms.tdThuHut - System.currentTimeMillis()) / 60000;
                        break;
                    case 522:
                        min = (player.charms.tdDeTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 671:
                        min = (player.charms.tdTriTue3 - System.currentTimeMillis()) / 60000;
                        break;
                    case 672:
                        min = (player.charms.tdTriTue4 - System.currentTimeMillis()) / 60000;
                        break;
                }
                if (min > 0) {
                    item.options.clear();
                    if (min >= 1440) {
                        item.options.add(new Item.ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new Item.ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new Item.ItemOption(65, (int) min));
                    }
                }
            }
        }
        return s;
    }

    private void openShopType0(Player player, Shop shop) {
        if (shop != null) {
            player.iDMark.setShopOpen(shop);
            player.iDMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt(itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType3(Player player, Shop shop) {
        player.iDMark.setShopOpen(shop);
        player.iDMark.setTagNameShop(shop.tagName);
        if (shop != null) {
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(SPEC_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer()
                                .writeShort(ItemService.gI().createNewItem((short) itemShop.iconSpec).template.iconID);
                        msg.writer().writeInt(itemShop.cost);
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopDanhHieu(Player player, String tagName, List<BadgesData> badges) {
        Logger.log("[ShopService] openShopDanhHieu called - player: " + player.name + ", tagName: " + tagName
                + ", badges: " + (badges == null ? "null" : badges.size()));
        if (badges == null || badges.isEmpty()) {
            Logger.log("[ShopService] openShopDanhHieu - badges is NULL or EMPTY for player: " + player.name);
            Service.gI().sendThongBao(player, "Bạn chưa có danh hiệu nào");
            return;
        }

        // Đếm số badges hợp lệ trước
        List<BadgesData> validBadges = new ArrayList<>();
        for (BadgesData badge : badges) {
            int idItem = BagesTemplate.findIdItemByIdIdEffect(badge.idBadGes);
            if (idItem == -1) {
                Logger.log("[ShopService] Badge idBadGes=" + badge.idBadGes + " -> idItem=-1, skipped");
                continue;
            }
            Item item = ItemService.gI().createNewItem((short) idItem);
            if (item == null || item.template == null) {
                Logger.log("[ShopService] Badge idBadGes=" + badge.idBadGes + " -> item=null, skipped");
                continue;
            }
            validBadges.add(badge);
        }

        Logger.log("[ShopService] validBadges count: " + validBadges.size() + " / " + badges.size());

        if (validBadges.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có danh hiệu hợp lệ");
            return;
        }

        player.iDMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF(validBadges.size() + "\n Danh hiệu");
            msg.writer().writeByte(validBadges.size()); // Dùng validBadges.size() thay vì badges.size()

            for (BadgesData badge : validBadges) {
                int idItem = BagesTemplate.findIdItemByIdIdEffect(badge.idBadGes);
                Item item = ItemService.gI().createNewItem((short) idItem);

                List<Item.ItemOption> badgeOptions = new ArrayList<>();
                for (BagesTemplate temp : Manager.BAGES_TEMPLATES) {
                    if (temp.idEffect == badge.idBadGes) {
                        badgeOptions = temp.options;
                        break;
                    }
                }

                String expirationText;
                if (badge.timeofUseBadges == -1) {
                    expirationText = "Vĩnh viễn";
                } else {
                    long remainingMs = badge.timeofUseBadges - System.currentTimeMillis();
                    if (remainingMs <= 0) {
                        expirationText = "Hết hạn";
                    } else {
                        long remainingDays = remainingMs / (24L * 60L * 60L * 1000L);
                        if (remainingDays > 0) {
                            expirationText = "Còn " + remainingDays + " ngày";
                        } else {
                            long remainingHours = remainingMs / (60L * 60L * 1000L);
                            expirationText = "Còn " + remainingHours + " giờ";
                        }
                    }
                }

                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF(expirationText);
                msg.writer().writeByte(badgeOptions.size());
                for (Item.ItemOption io : badgeOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            Logger.log("[ShopService] Sending shop message to player: " + player.name);
            player.sendMessage(msg);
        } catch (Exception e) {
            Logger.log("[ShopService] Exception in openShopDanhHieu: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void openShopType4(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.iDMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF(items.size() + "Vật\nphẩm");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF("LUCKY DRAGON BALL");
                msg.writer().writeByte(item.itemOptions.size() + 1);
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                if (item.quantity > 1) {
                    msg.writer().writeByte(31);
                    msg.writer().writeShort(item.quantity);
                } else {
                    msg.writer().writeByte(73);
                    msg.writer().writeShort(0);
                }
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void openShopType8(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.iDMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(8);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Mua lại\n[" + items.size() + "/20]");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                int giamualaingoc = item.template.gem / 2;
                int giamualaivang = giamualaingoc == 0
                        ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100
                        : 0;
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(giamualaivang);
                msg.writer().writeInt(giamualaingoc);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeByte(item.itemOptions.size());
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                msg.writer().writeByte(0);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void takeItem(Player player, byte type, int tempId) {
        String tagName = player.iDMark.getTagNameShop();
        if (tagName == null || tagName.length() <= 0) {
            return;
        }
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            getItemSideBoxLuckyRound(player, player.inventory.itemsBoxCrackBall, type, tempId);
            return;
        } else if (tagName.equals("ITEMS_MAIL_BOX")) {
            getItemSideMailsBox(player, player.inventory.itemsMailBox, type, tempId);
            return;
        } else if (tagName.equals("ITEMS_BADGES")) {
            List<Item> list = new ArrayList<>();
            if (player.dataBadges != null) {
                for (BadgesData data : player.dataBadges) {
                    int idItem = BagesTemplate.findIdItemByIdIdEffect(data.idBadGes);
                    if (idItem != -1) {
                        list.add(ItemService.gI().createNewItem((short) idItem));
                    }
                }
            }
            if (tempId >= 0 && tempId < list.size()) {
                Item item = list.get(tempId);
                int idBadge = BagesTemplate.fineIdEffectbyIdItem(item.template.id);
                if (idBadge != -1) {
                    if (player.lastTimeChangeBadges - System.currentTimeMillis() > 0) {
                        Service.gI().sendThongBao(player, "Vui lòng đợi "
                                + (player.lastTimeChangeBadges - System.currentTimeMillis()) / 1000 + " giây nữa");
                        return;
                    }
                    if (player.badges != null && player.badges.idBadges == idBadge) {
                        Service.gI().sendThongBao(player, "Danh hiệu đang được sử dụng");
                        return;
                    }
                    BadgesService.turnOnBadges(player, idBadge);
                    Service.gI().sendThongBao(player, "Đã đổi danh hiệu thành công");
                    player.lastTimeChangeBadges = System.currentTimeMillis() + 30000;
                }
            }
            return;
        } else if (tagName.equals("ITEMS_REWARD")) {

            return;
        } else if (tagName.equals("ITEMS_DABAN")) {
            buyItemDaBan(player, player.inventory.itemsDaBan, type, tempId);
            return;
        } else if (tagName.equals("BILL")) {
            buyItemHD(player, tempId);
            return;
        } else if (tagName.equals("SANTAGG")) {
            buyItemGG(player, tempId);
            return;
        }

        if (player.iDMark.getShopOpen() == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (tagName.equals("BUA_1H") || tagName.equals("BUA_8H") || tagName.equals("BUA_1M")) {
            buyItemBua(player, tempId);
        } else if (tagName.equals("SHOP_VND")) {
            buyItemVND(player, tempId);
        } else if (tagName.equals("SHOP_NHS")) {
            buyItemNHS(player, tempId);
        } else if (tagName.equals("SHOP_BHM")) {
            buyItemBHM(player, tempId);
        } else if (tagName.equals("SHOP_QUY_LAO")) {
            buyItemQuyLao(player, tempId);
        } else if (tagName.equals("SANTA_HEAD")) {
            Item itS = ItemService.gI().createNewItem((short) tempId);
            player.head = (short) itS.template.head;
            Service.gI().Send_Caitrang(player);
            Service.gI().sendThongBao(player, "Đổi kiểu tóc thành công");
        } else {
            buyItem(player, tempId);
        }
        Service.gI().sendMoney(player);
    }

    private boolean subMoneyByItemShop(Player player, ItemShop is) {
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;

        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng");
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc");
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBao(player, "Bạn không có đủ hồng ngọc");
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBao(player, "Bạn không có đủ điểm");
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;

        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        return true;
    }

    private boolean subMoneyByItemShopV2(Player player, ItemShop is) {
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;

        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBaoOK(player,
                    "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(player.inventory.gold - gold));
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBaoOK(player,
                    "Bạn không đủ ngọc, còn thiếu " + Util.numberToMoney(player.inventory.gem - gem));
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBaoOK(player,
                    "Bạn không đủ hồng ngọc, còn thiếu " + Util.numberToMoney(player.inventory.ruby - ruby));
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBaoOK(player,
                    "Bạn không đủ điểm, còn thiếu " + Util.numberToMoney(player.inventory.coupon - coupon));
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;

        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        Service.gI().sendMoney(player);
        return true;
    }

    /**
     * Mua bùa
     *
     * @param player     người chơi
     * @param itemTempId id template vật phẩm
     */
    private void buyItemBua(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (!subMoneyByItemShop(player, is)) {
            return;
        }
        InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
        InventoryService.gI().sendItemBag(player);
        opendShop(player, shop.tagName, true);
    }

    private void buyItemVND(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        int pointExchange = 0;
        long evPoint = player.getSession().cash;
        if (is == null) {
            Service.gI().sendThongBao(player, "Item shop bị lỗi vui lòng báo admin");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đầy rồi dọn bớt đi");
            return;
        }
        for (ItemOption io : is.options) {
            if (io.optionTemplate.id == 249) {
                pointExchange = io.param;
            }
        }
        if (pointExchange > 0) {
            long cost = (long) pointExchange * 1000;
            if (evPoint >= cost) {
                PlayerDAO.subcash(player, (int) cost);
                InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player,
                        "Bạn đã đổi thành công " + ItemService.gI().createItemFromItemShop(is).template.name);
                opendShop(player, shop.tagName, true);
            } else {
                Service.gI().sendThongBao(player, "Bạn còn thiếu " + (cost - evPoint) + " VND");
            }
        }
    }

    private void buyItemNHS(Player player, int itemTempId) {// zl 0822992003 Đức dz
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        int pointExchange = 0;
        int evPoint = player.event.getEventPointNHS();
        if (is == null) {// zl 0822992003 Đức dz
            Service.gI().sendThongBao(player, "Item shop bị lỗi vui lòng báo admin");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đầy rồi dọn bớt đi");
            return;
        }
        for (ItemOption io : is.options) {
            if (io.optionTemplate.id == 76) {
                pointExchange = io.param;
            }
        }
        if (pointExchange > 0) {
            if (evPoint >= pointExchange) {
                player.event.subEventPointNHS(pointExchange);
                InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player,
                        "Bạn đã đổi thành công " + ItemService.gI().createItemFromItemShop(is).template.name);
                opendShop(player, shop.tagName, true);
            } else {
                Service.gI().sendThongBao(player, "Bạn còn thiếu " + (pointExchange - evPoint) + " điểm");
            }
        }
    }

    private void buyItemBHM(Player player, int itemTempId) {// zl 0822992003 Đức dz
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        int pointExchange = 0;
        int evPoint = player.event.getEventPointBHM();
        if (is == null) {// zl 0822992003 Đức dz
            Service.gI().sendThongBao(player, "Item shop bị lỗi vui lòng báo admin");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đầy rồi dọn bớt đi");
            return;
        }
        for (ItemOption io : is.options) {
            if (io.optionTemplate.id == 76) {
                pointExchange = io.param;
            }
        }
        if (pointExchange > 0) {
            if (evPoint >= pointExchange) {
                player.event.subEventPointBHM(pointExchange);
                InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player,
                        "Bạn đã đổi thành công " + ItemService.gI().createItemFromItemShop(is).template.name);
                opendShop(player, shop.tagName, true);
            } else {
                Service.gI().sendThongBao(player, "Bạn còn thiếu " + (pointExchange - evPoint) + " điểm");
            }
        }
    }

    private void buyItemQuyLao(Player player, int itemTempId) {// zl 0822992003 Đức dz
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        int pointExchange = 0;
        int evPoint = player.event.getEventPointQuyLao();
        if (is == null) {// Zzl 0822992003 Đức dz
            Service.gI().sendThongBao(player, "Item shop bị lỗi vui lòng báo admin");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đầy rồi dọn bớt đi");
            return;
        }
        for (ItemOption io : is.options) {
            if (io.optionTemplate.id == 76) {
                pointExchange = io.param;
            }
        }
        if (pointExchange > 0) {
            if (evPoint >= pointExchange) {
                player.event.subEventPointQuyLao(pointExchange);
                InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player,
                        "Bạn đã đổi thành công " + ItemService.gI().createItemFromItemShop(is).template.name);
                opendShop(player, shop.tagName, true);
            } else {
                Service.gI().sendThongBao(player, "Bạn còn thiếu " + (pointExchange - evPoint) + " điểm");
            }
        }
    }

    /**
     * Mua vật phẩm trong cửa hàng
     *
     * @param player     người chơi
     * @param itemTempId id template vật phẩm
     */
    public void buyItem(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        int[][] listDauThan = { { 13, 293 }, { 60, 294 }, { 61, 295 }, { 62, 296 }, { 63, 297 }, { 64, 298 },
                { 65, 299 }, { 352, 596 }, { 523, 597 } };
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }

        if (itemTempId == 711 && !InventoryService.gI().findItemSkinQuyLaoKame(player)) {
            Service.gI().sendThongBao(player, "Bạn phải có cải trang thành Quy Lão Kame mới có thể đổi.");
            return;
        }

        if (buyMoRongHanhTrang(player, is)) {
            return;
        }

        if (shop.tagName.equals("SANTA_DANH_HIEU")) {
            int idBadge = BagesTemplate.fineIdEffectbyIdItem(is.temp.id);
            buyDanhHieu(player, is);
            return;
        }

        if (shop.typeShop == ShopService.NORMAL_SHOP) {
            if (!subMoneyByItemShop(player, is)) {
                return;
            }
        } else if (shop.typeShop == ShopService.SPEC_SHOP) {
            if (!this.subIemByItemShop(player, is)) {
                return;
            }
        } else if (shop.tagName.equals("SHOP_VND")) {// Zzl 0822992003 Đức dz
            if (!subMoneyByItemShop(player, is)) {// zl 0822992003 Đức dz
                return;
            }

        } else if (shop.tagName.equals("SHOP_NHS")) {// zl 0822992003 Đức dz
            if (!subMoneyByItemShop(player, is)) {// zl 0822992003 Đức dz
                return;
            }

        } else if (shop.tagName.equals("SHOP_BHM")) {// zl 0822992003 Đức dz
            if (!subMoneyByItemShop(player, is)) {// zl 0822992003 Đức dz
                return;
            }

        } else if (shop.tagName.equals("SHOP_QUY_LAO")) {// zl 0822992003 Đức dz
            if (!subMoneyByItemShop(player, is)) {// zl 0822992003 Đức dz
                return;
            }

        }
        Item item = ItemService.gI().createItemFromItemShop(is);
        item = buyMagicPean(player, listDauThan, item);
        if (item.template.id == 1523 || item.template.id == 1524) {
            item = ItemService.gI().createNewItem((short) 521);
            item.itemOptions.addAll(is.options);
        }
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
    }

    private void buyDanhHieu(Player pl, ItemShop is) {
        synchronized (pl) {
            int idBadge = BagesTemplate.fineIdEffectbyIdItem(is.temp.id);
            if (pl.dataBadges != null) {
                for (BadgesData badge : pl.dataBadges) {
                    if (badge.idBadGes == idBadge) {
                        Service.gI().sendThongBao(pl, "Bạn đã sở hữu danh hiệu này");
                        return;
                    }
                }
            } else {
                pl.dataBadges = new ArrayList<>();
            }

            if (subMoneyByItemShop(pl, is)) {
                long timeofUseBadges = -1;
                for (Item.ItemOption opt : is.options) {
                    if (opt.optionTemplate.id == 93) {
                        int days = opt.param;
                        timeofUseBadges = System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L);
                        System.out.println("Badge has HSD: " + days + " days, expires at: " + timeofUseBadges);
                        break;
                    }
                }

                pl.dataBadges.add(new BadgesData(idBadge, timeofUseBadges, false));
                Service.gI().sendThongBao(pl, "Mua thành công " + is.temp.name);
                System.out.println("Mua danh hieu: " + is.temp.name + " ID: " + idBadge + " ItemID: " + is.temp.id
                        + " timeofUseBadges: " + timeofUseBadges);
                PlayerDAO.updatePlayer(pl);
            }
        }
    }

    private void changeDanhHieu(Player pl, ItemShop is) {
        if (pl.lastTimeChangeBadges - System.currentTimeMillis() > 0) {
            Service.gI().sendThongBao(pl,
                    "Vui lòng đợi " + (pl.lastTimeChangeBadges - System.currentTimeMillis()) / 1000 + " giây nữa");
            return;
        }
        if (pl.badges.idBadges == BagesTemplate.fineIdEffectbyIdItem(is.temp.id)) {
            Service.gI().sendThongBao(pl, "Danh hiệu đang được sữ dụng, hãy chọn danh hiệu khác");
            pl.lastTimeChangeBadges = System.currentTimeMillis() + 30000;
            return;
        }
        BadgesService.turnOnBadges(pl, BagesTemplate.fineIdEffectbyIdItem(is.temp.id));
        Service.gI().sendThongBao(pl, "Đã đổi danh hiệu sang " + is.temp.name);
        pl.lastTimeChangeBadges = System.currentTimeMillis() + 30000;
    }

    private boolean buyMoRongHanhTrang(Player player, ItemShop itemShop) {
        boolean isBuy = false;
        if (itemShop.temp.id == 518 || itemShop.temp.id == 517 || itemShop.temp.id == 1627) {
            if (itemShop.temp.id == 1627 && player.inventory.itemsBag.size() >= 150) {
                Service.gI().sendThongBao(player, "Đã đạt mức tối đa");
                Service.gI().sendMoney(player);
                return true;
            }
            if (itemShop.temp.id == 517 && player.inventory.itemsBag.size() >= 100) {
                Service.gI().sendThongBao(player, "Đã đạt mức tối đa");
                Service.gI().sendMoney(player);
                return true;
            }
            if (itemShop.temp.id == 518 && player.inventory.itemsBox.size() >= 100) {
                Service.gI().sendThongBao(player, "Đã đạt mức tối đa");
                Service.gI().sendMoney(player);
                return true;
            }
            if (subMoneyByItemShop(player, itemShop)) {
                Item item = ItemService.gI().createItemFromItemShop(itemShop);
                InventoryService.gI().addItemBag(player, item);
                InventoryService.gI().sendItemBag(player);
                opendShop(player, itemShop.tabShop.shop.tagName, true);
                Service.gI().sendThongBao(player, "Bạn đã mua thành công");
            }
            isBuy = true;
        }
        return isBuy;
    }

    private Item buyMagicPean(Player player, int[][] listDauThan, Item item) {
        for (int i = 0; i < listDauThan.length; i++) {
            if (item.template.id == listDauThan[i][1]) {
                item = ItemService.gI().createNewItem((short) listDauThan[i][0]);
                item.itemOptions.add(new Item.ItemOption(player.magicTree.level - 1 > 1 ? 2 : 48,
                        MagicTree.PEA_PARAM[player.magicTree.level - 1]));
                item.quantity = 30;
                return item;
            }
        }
        return item;
    }

    private boolean subIemByItemShop(Player pl, ItemShop itemShop) {
        boolean isBuy = false;
        short itSpec = (short) itemShop.iconSpec;
        int buySpec = itemShop.cost;
        Item itS = ItemService.gI().createNewItem((short) itemShop.iconSpec);
        switch (itS.template.id) {
            case 76:
            case 188:
            case 189:
            case 190:
                if (pl.inventory.gold >= buySpec) {
                    pl.inventory.gold -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Vàng Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 861:
                if (pl.inventory.ruby >= buySpec) {
                    pl.inventory.ruby -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Hồng Ngọc Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            default:
                if (InventoryService.gI().findItemBag(pl, itSpec) == null
                        || !InventoryService.gI().findItemBag(pl, itSpec).isNotNullItem()) {
                    Service.gI().sendThongBao(pl, "Không tìm thấy " + itS.template.name);
                    isBuy = false;
                } else if (InventoryService.gI().findItemBag(pl, itSpec).quantity < buySpec) {
                    Service.gI().sendThongBao(pl, "Bạn không có đủ " + buySpec + " " + itS.template.name);
                    isBuy = false;
                } else {
                    InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, itSpec),
                            buySpec);
                    isBuy = true;
                }
                break;
        }
        return isBuy;
    }

    public void showConfirmSellItem(Player pl, int where, int index) {
        Item item = null;
        if (where == 0) {
            if (index < 0) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            item = pl.inventory.itemsBody.get(index);
        } else {
            if (pl.getSession().version < 220) {
                index -= (pl.inventory.itemsBody.size() - 7);
            }
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                Input.gI().createFormBanSLL(pl);
                return;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            String text = "Bạn có muốn bán\nx" + quantity
                    + " " + item.template.name + "\nvới giá là " + Util.numberToMoney(cost) + " vàng?";
            Message msg = null;
            try {
                msg = new Message(7);
                msg.writer().writeByte(where);
                msg.writer().writeShort(index);
                msg.writer().writeUTF(text);
                pl.sendMessage(msg);
            } catch (Exception e) {
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    public void sellItem(Player pl, int where, int index) {
        if (pl.iDMark.getShopOpen() == null || pl.iDMark.getTagNameShop() == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        if (index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        Item item = null;
        if (where == 0) {
            item = pl.inventory.itemsBody.get(index);
        } else {
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null) {
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            if (InventoryService.gI().getParam(pl, 93, item.template.id) > 0) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm có hạn sử dụng");
                return;
            }
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            if (pl.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                Service.gI().sendThongBao(pl, "Vàng sau khi bán vượt quá giới hạn");
                return;
            }
            pl.inventory.gold += cost;
            Service.gI().sendMoney(pl);
            Service.gI().sendThongBao(pl, "Đã bán " + item.template.name
                    + " thu được " + Util.numberToMoney(cost) + " vàng");

            if (item.template.id != 457) {
                VatPhamDaBan.gI().addItem(pl, item);
            }
            if (where == 0) {
                InventoryService.gI().subQuantityItemsBody(pl, item, quantity);
                InventoryService.gI().sendItemBody(pl);
                Service.gI().Send_Caitrang(pl);
            } else {
                InventoryService.gI().subQuantityItemsBag(pl, item, quantity);
                InventoryService.gI().sendItemBag(pl);
            }
            if ("BUNMA".equals(pl.iDMark.getTagNameShop())
                    || "DENDE".equals(pl.iDMark.getTagNameShop())
                    || "APPULE".equals(pl.iDMark.getTagNameShop())) {
                AchievementService.gI().checkDoneTask(pl, ConstAchievement.TRUM_NHAT_VE_CHAI);
            }
        } else {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void getItemSideBoxLuckyRound(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index < 0 || index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: // nhận
                if (item.isNotNullItem()) {
                    if (InventoryService.gI().getCountEmptyBag(player) != 0) {
                        InventoryService.gI().addItemBag(player, item);
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng"
                                        : item.template.name));
                        InventoryService.gI().sendItemBag(player);
                        items.remove(index);
                    } else {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
                break;
            case 1: // xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: // nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (InventoryService.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng"
                                        : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryService.gI().sendItemBag(player);
                break;
        }
        openShopType4(player, player.iDMark.getTagNameShop(), items);
    }

    private void getItemSideMailsBox(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index < 0 || index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: // nhận
                if (item.isNotNullItem()) {
                    if (InventoryService.gI().getCountEmptyBag(player) != 0) {
                        InventoryService.gI().addItemBag(player, item);
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng"
                                        : item.template.name));
                        InventoryService.gI().sendItemBag(player);
                        items.remove(index);
                    } else {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
                break;
            case 1: // xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: // nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (InventoryService.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng"
                                        : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryService.gI().sendItemBag(player);
                break;
        }
        openShopType4(player, player.iDMark.getTagNameShop(), items);
    }

    private void buyItemDaBan(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        int giamualaingoc = item.template.gem / 2;
        int giamualaivang = giamualaingoc == 0
                ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100
                : 0;
        if (giamualaivang > 0 && player.inventory.gold < giamualaivang) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng!");
            return;
        }
        if (giamualaingoc > 0 && player.inventory.gem < giamualaingoc) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc xanh!");
            return;
        }
        player.inventory.gem -= giamualaingoc;

        player.inventory.gold -= giamualaivang;
        Service.gI().sendMoney(player);
        if (item.isNotNullItem()) {
            if (InventoryService.gI().getCountEmptyBag(player) != 0) {
                InventoryService.gI().addItemBag(player, item);
                Service.gI().sendThongBao(player,
                        "Bạn nhận được " + (item.template.id == 189
                                ? Util.numberToMoney(item.quantity) + " vàng"
                                : item.template.name));
                InventoryService.gI().sendItemBag(player);
                items.remove(index);
            } else {
                Service.gI().sendThongBao(player, "Hành trang đã đầy");
            }
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
        openShopType8(player, player.iDMark.getTagNameShop(), items);
    }

    private void buyItemGG(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();

        // Chỉ cho phép mua từ shop có tagName = "Santagg"
        if (shop == null || !shop.tagName.equals("SANTAGG")) {
            Service.gI().sendThongBao(player, "Cần có phiếu giảm giá");
            return;
        }

        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }

        // Kiểm tra người chơi có vật phẩm ID 459 không
        Item item459 = player.inventory.itemsBag.stream()
                .filter(it -> it != null && it.template != null && it.template.id == 459)
                .findFirst()
                .orElse(null);

        if (item459 == null || item459.quantity < 1) {
            Service.gI().sendThongBao(player, "Cần có Phiếu giảm giá để mua.");
            return;
        }

        // Kiểm tra hành trang có chỗ trống không
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, không thể chứa thêm.");
            return;
        }

        // Kiểm tra người chơi có đủ tiền không
        if (!subMoneyByItemShopV2(player, is)) {
            Service.gI().sendThongBao(player, "Không đủ tiền để mua vật phẩm.");
            return;
        }

        // Trừ đi 1 vật phẩm ID 459
        InventoryService.gI().subQuantityItemsBag(player, item459, 1);

        // Tạo vật phẩm từ shop
        Item item = ItemService.gI().createItemFromItemShop(is);

        // Thêm vật phẩm vào hành trang người chơi
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
    }

    private void buyItemHD(Player player, int itemTempId) {
        Shop shop = player.iDMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = ItemService.gI().createItemFromItemShop(is);
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy, không thể chứa thêm.");
            return;
        }
        if (shop.tagName.equals("BILL")) {
            if (!this.subIemByItemShop(player, is)) {
                return;
            }
        } else {
            if (!subMoneyByItemShopV2(player, is)) {
                return;
            }
        }
        if (item.template.level == 14) {
            Item doAn = player.inventory.itemsBag.stream()
                    .filter(it -> it != null && it.template != null
                            && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665
                                    || it.template.id == 666 || it.template.id == 667)
                            && it.quantity >= 99)
                    .findFirst().orElse(null);
            if (doAn != null) {
                InventoryService.gI().subQuantityItemsBag(player, doAn, 99);
            } else {
                Service.gI().sendThongBao(player, "Không có đủ thức ăn");
                return;
            }
        }
        if (player.inventory.itemsBody.get(0) != null || player.inventory.itemsBody.get(1) != null
                || player.inventory.itemsBody.get(2) != null || player.inventory.itemsBody.get(3) != null
                || player.inventory.itemsBody.get(4) != null || player.inventory.itemsBody.get(5) != null) {
            Item dothan = player.inventory.itemsBody.stream()
                    .filter(it -> it != null && it.template != null && it.template.level == 13).findFirst()
                    .orElse(null);
            if (dothan == null) {
                Service.gI().sendThongBao(player, "Không có đủ set thần");
                return;
            }
        }
        int param = 0;
        if (item.template.level == 14) {
            if (Util.isTrue(25, 100)) {
                param = Util.nextInt(11, 15);
            } else if (Util.isTrue(25, 75)) {
                param = Util.nextInt(5, 10);
            } else {
                param = Util.nextInt(0, 4);
            }
        }
        List<ItemOption> itemoptions = new ArrayList<>();
        if (!item.itemOptions.isEmpty()) {
            for (ItemOption ios : item.itemOptions) {
                if (item.template.level == 14 && InventoryService.gI().optionCanUpgrade(ios.optionTemplate.id)
                        && param > 0) {
                    int id = ios.optionTemplate.id;
                    int param1 = ios.param + (ios.param * param) / 100;
                    itemoptions.add(new ItemOption(id, param1));
                } else if (ios.optionTemplate.id != 164) {
                    itemoptions.add(new ItemOption(ios.optionTemplate.id, ios.param));
                }
            }
        } else {
            itemoptions.add(new ItemOption(73, (short) 0));
        }
        item.itemOptions.clear();
        item.itemOptions.addAll(itemoptions);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
    }
}
