package nro.models.npc.npc_manifest;

import clan.Clan;
import consts.ConstKOL;
import consts.ConstNpc;
import event.EventManager;
import item.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.TreasureUnderSea.TreasureUnderSea;
import models.TreasureUnderSea.TreasureUnderSeaService;
import nro.models.npc.Npc;
import static nro.models.npc.NpcFactory.PLAYERID_OBJECT;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.PlayerService;
import nro.services.RewardService;
import nro.services.Service;
import nro.services.TaskService;
import services.func.ChangeMapService;
import services.func.Input;
import services.func.TopService;
import shop.ShopService;
import skill.Skill;
import utils.Logger;
import utils.SkillUtil;
import utils.TimeUtil;
import utils.Util;

public class QuyLaoKame extends Npc {

    private static class RewardItem {

        int itemId;
        int quantity;

        public RewardItem(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    private static class KOLQuestData {

        int questType;
        int itemId;
        int requiredQuantity;

        List<RewardItem> rewards;
        String description;

        public KOLQuestData(int questType, int itemId, int requiredQuantity, List<RewardItem> rewards, String description) {
            this.questType = questType;
            this.itemId = itemId;
            this.requiredQuantity = requiredQuantity;
            this.rewards = rewards;
            this.description = description;
        }
    }

    private static final Map<Integer, KOLQuestData> KOL_QUESTS = new HashMap<>();

    static {
        KOL_QUESTS.put(1, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION, 1778, 100, Arrays.asList(new RewardItem(1821, 5)), "Nhiệm vụ 1:\nThu thập 100 cuốn chả giò (Quái doanh trại)"));
        KOL_QUESTS.put(2, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION, 1824, 10, Arrays.asList(new RewardItem(1592, 5), new RewardItem(1757, 5)), "Nhiệm vụ 2:\nThu thập 10 chai cuke 2 lít (Boss doanh trại)"));
        KOL_QUESTS.put(3, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_DUNGEON_COMPLETION, -1, 20, Arrays.asList(new RewardItem(1360, 1)), "Nhiệm vụ 3:\nHoàn thành phó bản Destron Gas cấp 70 trên 20 lần"));
        KOL_QUESTS.put(4, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_PVP_WINS, -1, 10, Arrays.asList(new RewardItem(1654, 1)), "Nhiệm vụ 4:\nĐánh bại 10 người trong đại hội võ thuật"));
        KOL_QUESTS.put(5, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION, -1, 30, Arrays.asList(new RewardItem(1822, 10)), "Nhiệm vụ 5:\nHoàn thành 30 nhiệm vụ siêu khó hàng ngày"));
        KOL_QUESTS.put(6, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION, -1, 5, Arrays.asList(new RewardItem(1797, 1), new RewardItem(1592, 5), new RewardItem(1757, 5)), "Nhiệm vụ 6:\nTham gia hạ gục boss baby 5 lần"));
        KOL_QUESTS.put(7, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_MONSTER_KILL_COUNT, -1, 100000, Arrays.asList(new RewardItem(1592, 10), new RewardItem(664, 9999), new RewardItem(1757, 5)), "Nhiệm vụ 7:\nHạ 100.000 quái (dùng tự động luyện tập)"));
    }

    private static final Map<Integer, KOLQuestData> KOL_VIP_QUESTS = new HashMap<>();

    static {
        KOL_VIP_QUESTS.put(1, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION, 1778, 100, Arrays.asList(new RewardItem(1821, 10)), "Nhiệm vụ 1:\nThu thập 100 cuốn chả giò (Quái doanh trại)"));
        KOL_VIP_QUESTS.put(2, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION, 1824, 10, Arrays.asList(new RewardItem(1592, 10), new RewardItem(1757, 10)), "Nhiệm vụ 2:\nThu thập 10 chai cuke 2 lít (Boss doanh trại)"));
        KOL_VIP_QUESTS.put(3, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_DUNGEON_COMPLETION, -1, 20, Arrays.asList(new RewardItem(1360, 1)), "Nhiệm vụ 3:\nHoàn thành phó bản Destron Gas cấp 70 trên 20 lần"));
        KOL_VIP_QUESTS.put(4, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_PVP_WINS, -1, 10, Arrays.asList(new RewardItem(1654, 1)), "Nhiệm vụ 4:\nĐánh bại 10 người trong đại hội võ thuật"));
        KOL_VIP_QUESTS.put(5, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION, -1, 30, Arrays.asList(new RewardItem(1822, 20)), "Nhiệm vụ 5:\nHoàn thành 30 nhiệm vụ siêu khó hàng ngày"));
        KOL_VIP_QUESTS.put(6, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION, -1, 5, Arrays.asList(new RewardItem(1797, 1), new RewardItem(1592, 10), new RewardItem(1757, 10)), "Nhiệm vụ 6:\nTham gia hạ gục boss baby 5 lần"));
        KOL_VIP_QUESTS.put(7, new KOLQuestData(ConstKOL.KOL_QUEST_TYPE_MONSTER_KILL_COUNT, -1, 100000, Arrays.asList(new RewardItem(1592, 20), new RewardItem(1757, 10)), "Nhiệm vụ 7:\nHạ 100.000 quái (dùng tự động luyện tập)"));
    }

    public QuyLaoKame(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        Item mcl = InventoryService.gI().findItemBagByTemp(player, 1705);
        int slMCL = (mcl == null) ? 0 : mcl.quantity;
        Item ruacon = InventoryService.gI().findItemBag(player, 874);

        if (canOpenNpc(player)) {
            ArrayList<String> menu = new ArrayList<>();

            if (!player.canRewardBeNa && !player.canRewardLanCon) {
                menu.add("Nói\nchuyện");
                boolean anyEventActive
                        = EventManager.EVENT_POKEMON
                        || EventManager.TRUNG_THU
                        || EventManager.TEACHERS_DAY
                        || EventManager.HALLOWEEN
                        || EventManager.LUNAR_NEW_YEAR
                        || EventManager.CHRISTMAS;
                if (anyEventActive) {
                    menu.add("Đổi điểm\nsự kiện\n[" + slMCL + "]");
                }
                menu.add("Nhiệm vụ\nKOL");
                menu.add("Kiểm tra lại\nbang hội");
                if (EventManager.TEACHERS_DAY) {
                    menu.add("Tặng Hộp\ntrà");
                    menu.add("Tặng\nHộp trà hoa\ncúc");
                    menu.add("Tặng\n10 Hộp trà\nhoa cúc");
                }
                if (EventManager.LUNAR_NEW_YEAR) {
                    menu.add("Đổi phiếu\nbé ngoan\nlấy quà");
                }
                if (ruacon != null && ruacon.quantity >= 1) {
                    menu.add("Giao\nRùa con");
                }
            } else {
                if (player.canRewardBeNa) {
                    menu.add("Giao\nBé Na");
                }
                if (player.canRewardLanCon) {
                    menu.add("Giao\nLân Con");
                }
                if (player.canRewardTuanLoc) {
                    menu.add("Giao\nTuần Lộc");
                }
            }
            String[] menus = menu.toArray(String[]::new);
            player.iDMark.menuSelect = menus;

            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Con muốn hỏi gì nào?", menus);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        String chosen = player.iDMark.menuSelect[select];

        if (chosen.equals("Giao\nBé Na")) {
            RewardService.gI().rewardBeNa(player);
            return;
        }

        if (chosen.equals("Giao\nLân Con")) {
            RewardService.gI().rewardLanCon(player);
            return;
        }

        if (chosen.equals("Giao\nTuần Lộc")) {
            RewardService.gI().rewardTuanLoc(player);
            return;
        }

        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.BASE_MENU:
                handleBaseMenu(player, select);
                break;
            case 12:
                handleMenu12(player, select);
                break;
            case 0:
                handleMenu0(player, select);
                break;
            case 4:
                handleMenu4(player, select);
                break;
            case ConstNpc.MENU_OPENED_DBKB:
                handleMenuOpenedDBKB(player, select);
                break;
            case ConstNpc.MENU_OPEN_DBKB:
                handleMenuOpenDBKB(player, select);
                break;
            case ConstNpc.MENU_ACCEPT_GO_TO_BDKB:
                handleMenuAcceptGoToBDKB(player, select);
                break;
            case ConstKOL.KOL_QUEST_MENU:
                handleKOLQuestRewardConfirm(player, select, false);
                break;
            case ConstKOL.KOL_VIP_REWARD_MENU:
                handleKOLQuestRewardConfirm(player, select, true);
                break;
            case 13:
                handleHuySkill(player, select);
                break;
            case 15:
                handleMenu15(player, select);
                break;
            case 17:
                handleMenu17(player, select);
                break;
        }
    }

    private void handleBaseMenu(Player player, int select) {
        if (player == null || player.iDMark == null || player.iDMark.menuSelect == null) {
            return;
        }
        if (select < 0 || select >= player.iDMark.menuSelect.length) {
            return;
        }

        String chosen = player.iDMark.menuSelect[select];

        if (chosen.equals("Nói\nchuyện")) {
            handleTalk(player);
            return;
        }

        if (chosen.contains("Đổi điểm\nsự kiện")) {
            boolean anyEventActive = EventManager.EVENT_POKEMON
                    || EventManager.TRUNG_THU
                    || EventManager.TEACHERS_DAY
                    || EventManager.HALLOWEEN
                    || EventManager.LUNAR_NEW_YEAR
                    || EventManager.CHRISTMAS;

            if (!anyEventActive) {
                Service.gI().sendThongBao(player, "Hiện tại không có sự kiện nào diễn ra!");
                return;
            }

            if (EventManager.TEACHERS_DAY) {
                ShopService.gI().opendShop(player, "SHOP_TEACHERS_DAY", false);
            } else if (EventManager.EVENT_POKEMON) {
                ShopService.gI().opendShop(player, "SHOP_QUY_LAO", false);
            } else if (EventManager.TRUNG_THU) {
                ShopService.gI().opendShop(player, "SHOP_TRUNG_THU", false);
            } else if (EventManager.HALLOWEEN) {
                ShopService.gI().opendShop(player, "HALLOWEEN_QUY_LAO", false);
            } else if (EventManager.CHRISTMAS) {
                ShopService.gI().opendShop(player, "CHRISTMAS_QUYLAO", false);
            } else if (EventManager.LUNAR_NEW_YEAR) {
                ShopService.gI().opendShop(player, "LUNAR_NEW_YEAR", false);
            }
            return;
        }

        if (chosen.equals("Nhiệm vụ\nKOL")) {
            this.createOtherMenu(player, 15,
                    "Nhiệm vụ KOL đã diễn ra.\nMua thêm vé Vip để nhận nhiều phần thưởng hơn",
                    "Nhận quà\nKOL", "Nhận quà\nKOL VIP", "Đóng");
            return;
        }

        if (chosen.equals("Kiểm tra lại\nbang hội")) {
            handleMenu16(player);
            return;
        }

        if (chosen.equals("Đổi phiếu\nbé ngoan\nlấy quà")) {
            Item phieu = InventoryService.gI().findItemBag(player, 1194);
            int quantity = (phieu != null) ? phieu.quantity : 0;

            // Nội dung hội thoại NPC y hệt trong ảnh
            String npcSay = "Con đang có " + quantity + " phiếu bé ngoan\n"
                    + "Con muốn đổi quà gì nào ?\n"
                    + "30 phiếu: mèo mun đột biến (đeo lưng) hsd 15 ngày\n"
                    + "50 phiếu: xí muội Hoa đào, xí muội Hoa mai\n"
                    + "70 phiếu: Bóng Heo hồng hsd 30 ngày\n"
                    + "99 phiếu: Lý Tiểu Nương Rực Rỡ hsd 15, 30 ngày";
            this.createOtherMenu(player, 17, npcSay,
                    "30", "50", "70", "99", "Đóng");
            return;
        }

        if (chosen.equals("Tặng Hộp\ntrà")) {
            handleTradeHopTra(player, 4);
            return;
        }

        if (chosen.equals("Tặng\nHộp trà hoa\ncúc")) {
            handleTradeHopTra(player, 5);
            return;
        }

        if (chosen.equals("Tặng\n10 Hộp trà\nhoa cúc")) {
            handleTradeHopTra(player, 6);
            return;
        }

        if (chosen.equals("Giao\nRùa con")) {
            handleTradeRuacon(player);
            return;
        }

        if (chosen.equals("Giao\nBé Na")) {
            RewardService.gI().rewardBeNa(player);
            return;
        }

        if (chosen.equals("Giao\nLân Con")) {
            RewardService.gI().rewardLanCon(player);
            return;
        }

        if (chosen.equals("Giao\nTuần Lộc")) {
            RewardService.gI().rewardTuanLoc(player);
            return;
        }

        // fallback
        Service.gI().sendThongBao(player, "Lựa chọn không hợp lệ!");
    }

    private static long lastCheckTime = 0;

    private void handleMenu16(Player player) {
        long now = System.currentTimeMillis();
        int cooldown = 60;
        long remaining = (lastCheckTime + cooldown * 1000L - now) / 1000L;

        if (remaining > 0) {
            Service.gI().sendThongBao(player, "Vui lòng đợi " + remaining + " giây nữa");
            return;
        }
        lastCheckTime = now;
        Clan.startAutoCheck();
        Service.gI().sendThongBao(player, "Xong");
    }

    private void handleMenu15(Player player, int select) {
        switch (select) {
            case 0 -> {
                handleKOLQuest(player, false);
            }
            case 1 -> {
                handleKOLQuest(player, true);
            }
        }
    }

    private void handleMenu17(Player player, int select) {
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy");
            return;
        }

        Item phieu = InventoryService.gI().findItemBag(player, 1194);
        int cost = 0;
        short itemId = -1;
        int days = 0;
        Item itemReward = null;

        switch (select) {
            case 0:
                cost = 30;
                itemId = 1140;
                days = 15;
                break;
            case 1:
                cost = 50;
                itemId = (short) (Util.nextInt(0, 1) == 0 ? 1195 : 1196);
                days = 30;
                break;
            case 2:
                cost = 70;
                itemId = 1142;
                days = 30;
                break;
            case 3:
                cost = 99;
                itemId = (short) (Util.nextInt(0, 1) == 0 ? 1503 : 1504);
                days = (itemId == 1503) ? 15 : 30;
                break;
            default:
                return; // Đóng menu
        }

        if (phieu == null || phieu.quantity < cost) {
            Service.gI().sendThongBao(player, "Bạn không đủ phiếu bé ngoan để đổi");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, phieu, cost);

        itemReward = ItemService.gI().createNewItem(itemId);

        switch (select) {
            case 0: // Mèo mun đột biến
                itemReward.itemOptions.add(new Item.ItemOption(50, 15));
                itemReward.itemOptions.add(new Item.ItemOption(77, 15));
                itemReward.itemOptions.add(new Item.ItemOption(103, 15));
                itemReward.itemOptions.add(new Item.ItemOption(93, days));
                break;
            case 1: // Xí muội
                itemReward.itemOptions.add(new Item.ItemOption(50, 20));
                itemReward.itemOptions.add(new Item.ItemOption(77, 20));
                itemReward.itemOptions.add(new Item.ItemOption(103, 20));
                itemReward.itemOptions.add(new Item.ItemOption(93, days));
                break;
            case 2: // Bóng heo
                itemReward.itemOptions.add(new Item.ItemOption(50, 25));
                itemReward.itemOptions.add(new Item.ItemOption(77, 25));
                itemReward.itemOptions.add(new Item.ItemOption(103, 25));
                itemReward.itemOptions.add(new Item.ItemOption(93, days));
                break;
            case 3: // Lý tiểu nương
                itemReward.itemOptions.add(new Item.ItemOption(50, 30));
                itemReward.itemOptions.add(new Item.ItemOption(77, 30));
                itemReward.itemOptions.add(new Item.ItemOption(103, 30));
                itemReward.itemOptions.add(new Item.ItemOption(14, 10)); // Chí mạng 10%
                itemReward.itemOptions.add(new Item.ItemOption(93, days));
                break;
        }

        InventoryService.gI().addItemBag(player, itemReward);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + itemReward.template.name);
    }

    private void handleTalk(Player player) {
        if (player.LearnSkill.Time != -1 && player.LearnSkill.Time <= System.currentTimeMillis()) {
            player.LearnSkill.Time = -1;
            try {
                var curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId), SkillUtil.getSkillByItemID(player, player.LearnSkill.ItemTemplateSkillId).point);
                player.BoughtSkill.add((int) player.LearnSkill.ItemTemplateSkillId);
                SkillUtil.setSkill(player, curSkill);
                var msg = Service.gI().messageSubCommand((byte) 62);
                msg.writer().writeShort(curSkill.skillId);
                player.sendMessage(msg);
                msg.cleanup();
                PlayerService.gI().sendInfoHpMpMoney(player);
            } catch (Exception e) {
                Logger.log(e.toString());
            }
        }

        ArrayList<String> menu = new ArrayList<>();
        menu.add("Nhiệm vụ");
        menu.add("Học\nKỹ năng");
        if (player.clan != null) {
            menu.add("Về khu\nvực bang");
            menu.add("Kho báu\ndưới biển");
            if (player.clan.isLeader(player)) {
                menu.add("Giải tán\nBang hội");
            }
        }
        this.createOtherMenu(player, 0, "Chào con, ta rất vui khi gặp con\nCon muốn làm gì nào ?", menu.toArray(new String[0]));
    }

    private void handleTradeHopTra(Player player, int select) {
        try {
            // Danh sách item chia theo cấp độ
            List<Short> itemCap1 = Arrays.asList(
                    (short) 680, (short) 819, (short) 860, (short) 914, (short) 977
            );
            List<Short> itemCap2 = Arrays.asList(
                    (short) 1041, (short) 1042, (short) 1208, (short) 1209, (short) 1210, (short) 1235
            );
            List<Short> itemCap3 = Arrays.asList(
                    (short) 1476, (short) 1557, (short) 1567, (short) 1602, (short) 1772, (short) 1961
            );

            Item itemReward = null;
            short selectedItemId = -1;

            switch (select) {
                case 4: { // Cấp 1 - Hộp trà
                    Item tra = InventoryService.gI().findItemBag(player, 1369); // ID Hộp trà
                    if (tra == null || tra.quantity < 1) {
                        Service.gI().sendThongBao(player, "Bạn không có Hộp trà");
                        return;
                    }
                    InventoryService.gI().subQuantityItemsBag(player, tra, 1);

                    selectedItemId = itemCap1.get(Util.nextInt(0, itemCap1.size()));
                    itemReward = ItemService.gI().createNewItem(selectedItemId);

                    itemReward.itemOptions.add(new Item.ItemOption(50, Util.nextInt(15, 19)));
                    itemReward.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 20)));
                    itemReward.itemOptions.add(new Item.ItemOption(103, Util.nextInt(17, 22)));
                    itemReward.itemOptions.add(new Item.ItemOption(211, Util.nextInt(4, 6)));
                    itemReward.itemOptions.add(new Item.ItemOption(93, 7)); // hạn 7 ngày
                    break;
                }
                case 5: { // Cấp 2 - Hộp trà hoa cúc
                    Item tra = InventoryService.gI().findItemBag(player, 1370); // ID Hộp trà hoa cúc
                    if (tra == null || tra.quantity < 1) {
                        Service.gI().sendThongBao(player, "Bạn không có Hộp trà Hoa Cúc");
                        return;
                    }
                    InventoryService.gI().subQuantityItemsBag(player, tra, 1);

                    selectedItemId = itemCap2.get(Util.nextInt(0, itemCap2.size()));
                    itemReward = ItemService.gI().createNewItem(selectedItemId);

                    itemReward.itemOptions.add(new Item.ItemOption(50, Util.nextInt(19, 24)));
                    itemReward.itemOptions.add(new Item.ItemOption(77, Util.nextInt(20, 25)));
                    itemReward.itemOptions.add(new Item.ItemOption(103, Util.nextInt(22, 27)));
                    itemReward.itemOptions.add(new Item.ItemOption(211, Util.nextInt(5, 8)));
                    itemReward.itemOptions.add(new Item.ItemOption(93, 10)); // hạn 10 ngày
                    break;
                }
                case 6: { // Cấp 3 - Hộp trà hoa cúc x10
                    Item tra = InventoryService.gI().findItemBag(player, 1370); // ID Hộp trà hoa cúc
                    if (tra == null || tra.quantity < 10) {
                        Service.gI().sendThongBao(player, "Bạn cần ít nhất 10 Hộp trà Hoa Cúc");
                        return;
                    }
                    InventoryService.gI().subQuantityItemsBag(player, tra, 10);

                    selectedItemId = itemCap3.get(Util.nextInt(0, itemCap3.size()));
                    itemReward = ItemService.gI().createNewItem(selectedItemId);

                    itemReward.itemOptions.add(new Item.ItemOption(50, Util.nextInt(24, 28)));
                    itemReward.itemOptions.add(new Item.ItemOption(77, Util.nextInt(23, 28)));
                    itemReward.itemOptions.add(new Item.ItemOption(103, Util.nextInt(25, 30)));
                    itemReward.itemOptions.add(new Item.ItemOption(211, Util.nextInt(7, 9)));
                    itemReward.itemOptions.add(new Item.ItemOption(93, 15)); // hạn 15 ngày

                    // Nếu là 1961 (Pan VIP) thì thêm may mắn
                    if (selectedItemId == 1961) {
                        itemReward.itemOptions.add(new Item.ItemOption(95, 100));
                    }
                    break;
                }
                default:
                    Service.gI().sendThongBao(player, "Lựa chọn không hợp lệ");
                    return;
            }

            // Thêm item vào túi
            if (itemReward != null) {
                if (InventoryService.gI().getCountEmptyBag(player) > 0) {
                    InventoryService.gI().addItemBag(player, itemReward);
                    InventoryService.gI().sendItemBag(player);
                    player.thiepchucvip += 1;
                    player.hopqua2010 += 1;
                    Service.gI().sendThongBao(player, "Bạn đã nhận được " + itemReward.template.name);
                } else {
                    Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy");
                }
            }

        } catch (Exception e) {
            Logger.error("Lỗi trong handleTradeHopTra: " + e.getMessage());
        }
    }

    private void handleTradeRuacon(Player player) {
        Item ruacon = InventoryService.gI().findItemBag(player, 874);
        if (ruacon != null && ruacon.quantity >= 1) {
            this.createOtherMenu(player, 1, "Cảm ơn cậu đã cứu con rùa của ta\nĐể cảm ơn ta sẽ tặng cậu món quà.", "Nhận quà", "Đóng");
        }
    }

    private void learnSkill(Player player) {
        try {
            String[] subName = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId).name.split("");
            byte level = Byte.parseByte(subName[subName.length - 1]);
            Skill curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId), level);
            player.BoughtSkill.add((int) player.LearnSkill.ItemTemplateSkillId);
            SkillUtil.setSkill(player, curSkill);
            var msg = Service.gI().messageSubCommand((byte) 62);
            msg.writer().writeShort(curSkill.skillId);
            player.sendMessage(msg);
            msg.cleanup();
            PlayerService.gI().sendInfoHpMpMoney(player);
        } catch (Exception e) {
            Logger.log(e.toString());
        }
    }

    private void handleMenu12(Player player, int select) {
        switch (select) {
            case 0 -> {
                var time = player.LearnSkill.Time - System.currentTimeMillis();
                var ngoc = 5;
                if (time / 600_000 >= 2) {
                    ngoc += time / 600_000;
                }
                if (player.inventory.gem < ngoc) {
                    Service.gI().sendThongBao(player, "Bạn không có đủ ngọc");
                    return;
                }
                player.inventory.subGem(ngoc);
                player.LearnSkill.Time = -1;
                learnSkill(player);
            }
            case 1 -> {
                createOtherMenu(player, 13, "Con có muốn huỷ học kỹ năng này và nhận lại 50% số tiềm năng không ?", "Ok", "Đóng");
            }
        }
    }

    private void handleMenu0(Player player, int select) {
        switch (select) {
            case 0: // Nhiệm vụ
                NpcService.gI().createTutorial(player, tempId, avartar, player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                break;
            case 1: // Học Kỹ năng
                if (player.LearnSkill.Time != -1) {
                    handleSkillLearning(player);
                } else {
                    ShopService.gI().opendShop(player, "QUY_LAO_KY_NANG", false);
                }
                break;
            case 2: // Về khu vực bang
                handleClanMapChange(player);
                break;
            default:
                if (player.clan != null) {
                    if (select == 3) {
                        handleTreasureMap(player);
                    } else if (player.clan.isLeader(player) && select == 4) {
                        handleClanDissolution(player);
                    }
                }
                break;
        }
    }

    private void handleSkillLearning(Player player) {
        if (player.LearnSkill.Time != -1) {
            var ngoc = 5;
            var time = player.LearnSkill.Time - System.currentTimeMillis();
            if (time / 600_000 >= 2) {
                ngoc += time / 600_000;
            }
            String[] subName = ItemService.gI().getTemplate(player.LearnSkill.ItemTemplateSkillId).name.split("");
            byte level = Byte.parseByte(subName[subName.length - 1]);
            createOtherMenu(player, 12,
                    "Con đang học kỹ năng\n" + SkillUtil.findSkillTemplate(SkillUtil.getTempSkillSkillByItemID(player.LearnSkill.ItemTemplateSkillId)).name
                    + " cấp " + level + "\nThời gian còn lại " + TimeUtil.getTime(time),
                    "Học Cấp tốc " + ngoc + " ngọc", "Huỷ", "Bỏ qua");
        } else {
            ShopService.gI().opendShop(player, "QUY_LAO_KY_NANG", false);
        }
    }

    private void handleClanMapChange(Player player) {
        if (player.clan != null) {
            ChangeMapService.gI().changeMapNonSpaceship(player, 153, Util.nextInt(100, 200), 432);
        } else {
            Service.gI().sendThongBao(player, "Bạn cần có bang hội để thực hiện chức năng này.");
        }
    }

    private void handleClanDissolution(Player player) {
        if (player.clan != null && player.clan.isLeader(player)) {
            createOtherMenu(player, 4, "Con có chắc muốn giải tán bang hội không?", "Đồng ý", "Từ chối");
        }
    }

    private void handleTreasureMap(Player player) {
        try {
            // Kiểm tra xem người chơi có clan không
            if (player != null && player.clan != null && player.clan.BanDoKhoBau != null) {
                this.createOtherMenu(
                        player,
                        ConstNpc.MENU_OPENED_DBKB,
                        "Bang hội của con đang ở hang kho báu cấp " + player.clan.BanDoKhoBau.level
                        + "\nCon có muốn đi cùng họ không?",
                        "Top\nBang hội",
                        "Thành tích\nBang",
                        "Đồng ý",
                        "Từ chối"
                );
            } else {
                this.createOtherMenu(
                        player,
                        ConstNpc.MENU_OPEN_DBKB,
                        "Đây là bản đồ kho báu hải tặc tí hon\n"
                        + "Các con cứ yên tâm lên đường, ở đây có ta lo.\n"
                        + "Nhớ chọn cấp độ vừa sức mình nhé.",
                        "Top\nBang hội",
                        "Thành tích\nBang",
                        "Chọn\ncấp độ",
                        "Từ chối"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            Service.gI().sendThongBao(player, "Lỗi khi mở bản đồ kho báu, vui lòng thử lại sau!");
        }
    }

    private void handleMenu4(Player player, int select) {
        if (player.clan != null && player.clan.isLeader(player) && select == 0) {
            Input.gI().createFormGiaiTanBangHoi(player);
        }
    }

    private void handleMenuOpenedDBKB(Player player, int select) {
        if (select == 0) {
            TopService.gI().showTopClanKhoBau(player);
        } else if (select == 1) {
            TopService.gI().showMyTopClanKhoBau(player);
        } else if (select == 2) {
            if (player.clan == null) {
                Service.gI().sendThongBao(player, "Hãy vào bang hội trước");
                return;
            }
            if (player.isAdmin() || player.nPoint.power >= TreasureUnderSea.POWER_CAN_GO_TO_DBKB) {
                Input.gI().createFormChooseLevelBDKB(player);
            } else {
                this.npcChat(player, "Yêu cầu sức mạnh lớn hơn "
                        + Util.numberToMoney(TreasureUnderSea.POWER_CAN_GO_TO_DBKB));
            }
        }
    }

    private void handleMenuOpenDBKB(Player player, int select) {
        if (select == 0) {
            TopService.gI().showTopClanKhoBau(player);
        } else if (select == 1) {
            TopService.gI().showMyTopClanKhoBau(player);
        } else if (select == 2) {
            if (player.clan == null) {
                Service.gI().sendThongBao(player, "Hãy vào bang hội trước");
                return;
            }
            if (player.isAdmin() || player.nPoint.power >= TreasureUnderSea.POWER_CAN_GO_TO_DBKB) {
                Input.gI().createFormChooseLevelBDKB(player);
            } else {
                this.npcChat(player, "Yêu cầu sức mạnh lớn hơn "
                        + Util.numberToMoney(TreasureUnderSea.POWER_CAN_GO_TO_DBKB));
            }
        }
    }

    private void handleHuySkill(Player player, int select) {
        switch (select) {
            case 0 -> { // Hủy kỹ năng đang học
                if (player.LearnSkill.Time == -1) {
                    Service.gI().sendThongBao(player, "Hiện tại bạn không đang học kỹ năng nào.");
                    return;
                }

                long time = player.LearnSkill.Time - System.currentTimeMillis();
                int ngoc = 5;
                if (time > 0 && time / 600_000 >= 2) {
                    ngoc += (int) (time / 600_000);
                }

                if (player.inventory.gem < ngoc) {
                    Service.gI().sendThongBao(player, "Bạn không có đủ ngọc để hủy kỹ năng.");
                    return;
                }

                // Trừ ngọc và hủy thời gian học
                player.inventory.subGem(ngoc);
                int skillId = player.LearnSkill.ItemTemplateSkillId;
                player.LearnSkill.Time = -1;

                try {
                    Skill curSkill = SkillUtil.getSkillByItemID(player, skillId);

                    // Nếu skill chưa có point, tạo skill cơ bản với cấp 1
                    if (curSkill == null) {
                        curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(skillId), (byte) 1);
                    }

                    // Trả lại 50% tiềm năng nhưng không nâng skill
                    int refundPoint = curSkill.point / 2;
                    if (refundPoint > 0) {
                        player.nPoint.tiemNang += refundPoint; // cộng điểm tiềm năng
                    }

                    SkillUtil.setSkill(player, curSkill);
                    PlayerService.gI().sendInfoHpMpMoney(player);
                    Service.gI().sendThongBao(player, "Đã hủy học kỹ năng và nhận lại 50% tiềm năng.");
                } catch (Exception e) {
                    utils.Logger.log(e.toString());
                }
            }
            case 1 -> {
                // Đóng menu
            }
        }
    }

    private void handleMenuAcceptGoToBDKB(Player player, int select) {
        if (select == 0) {
            TreasureUnderSeaService.gI().openBanDoKhoBau(player, Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
        }
    }

    private void handleKOLQuest(Player player, boolean isVIP) {
        int currentStage;
        Map<Integer, KOLQuestData> questsMap;
        int menuType;

        if (isVIP) {
            currentStage = player.kolVIPQuestStage;
            questsMap = KOL_VIP_QUESTS;
            menuType = ConstKOL.KOL_VIP_REWARD_MENU;
        } else {
            currentStage = player.kolQuestStage;
            questsMap = KOL_QUESTS;
            menuType = ConstKOL.KOL_QUEST_MENU;
        }

        if (currentStage < 1) {
            currentStage = 1;
            if (isVIP) {
                player.kolVIPQuestStage = 1;
            } else {
                player.kolQuestStage = 1;
            }
        }

        KOLQuestData questData = questsMap.get(currentStage);

        if (questData == null) {
            this.createOtherMenu(player, menuType, "Con đã hoàn thành tất cả nhiệm vụ " + (isVIP ? "KOL VIP" : "KOL") + " rồi! Chúc mừng con!", "Đóng");
            return;
        }

        long currentProgress = 0;
        switch (questData.questType) {
            case ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION:
                Item requiredItem = InventoryService.gI().findItemBag(player, questData.itemId);
                currentProgress = requiredItem != null ? requiredItem.quantity : 0;
                break;
            case ConstKOL.KOL_QUEST_TYPE_DUNGEON_COMPLETION:
                currentProgress = player.destronGas70CompletionCount;
                break;
            case ConstKOL.KOL_QUEST_TYPE_PVP_WINS:
                currentProgress = player.martialArtsTournamentWins;
                break;
            case ConstKOL.KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION:
                currentProgress = player.dailySuperHardQuestCompletionCount;
                break;
            case ConstKOL.KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION:
                currentProgress = player.bossBabyDefeatParticipationCount;
                break;
            case ConstKOL.KOL_QUEST_TYPE_MONSTER_KILL_COUNT:
                currentProgress = player.monsterKillCountAutoTrain;
                break;
            default:
                Service.gI().sendThongBao(player, "Lỗi: Loại nhiệm vụ không xác định.");
                return;
        }

        int percent = (int) ((currentProgress * 100.0) / questData.requiredQuantity);
        if (percent > 100) {
            percent = 100;
        }

        String rewardDetails = formatRewardDetails(questData.rewards);

        String npcText = questData.description + "\nPhần thưởng: " + rewardDetails + "\nHoàn thành: "
                + currentProgress + "/" + questData.requiredQuantity + " (" + percent + "%)";

        if (currentProgress >= questData.requiredQuantity) {
            this.createOtherMenu(player, menuType, npcText, "Nhận thưởng", "Đóng");
        } else {
            this.createOtherMenu(player, menuType, npcText, "Đóng");
        }
    }

    private void handleKOLQuestRewardConfirm(Player player, int select, boolean isVIP) {
        if (select == 0) {
            int currentStage;
            Map<Integer, KOLQuestData> questsMap;

            if (isVIP) {
                Item vipTicket = InventoryService.gI().findItemBag(player, 1825);
                if (vipTicket == null || vipTicket.quantity < 1) {
                    Service.gI().sendThongBao(player, "Bạn cần có Vé Nhiệm Vụ VIP để nhận thưởng KOL VIP.");
                    openBaseMenu(player);
                    return;
                }
                currentStage = player.kolVIPQuestStage;
                questsMap = KOL_VIP_QUESTS;
            } else {
                currentStage = player.kolQuestStage;
                questsMap = KOL_QUESTS;
            }

            if (currentStage < 1) {
                currentStage = 1;
                if (isVIP) {
                    player.kolVIPQuestStage = 1;
                } else {
                    player.kolQuestStage = 1;
                }
            }

            KOLQuestData questData = questsMap.get(currentStage);

            if (questData == null) {
                Service.gI().sendThongBao(player, "Không tìm thấy nhiệm vụ hiện tại.");
                return;
            }

            long currentProgress = 0;
            switch (questData.questType) {
                case ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION:
                    Item requiredItem = InventoryService.gI().findItemBag(player, questData.itemId);
                    currentProgress = requiredItem != null ? requiredItem.quantity : 0;
                    break;
                case ConstKOL.KOL_QUEST_TYPE_DUNGEON_COMPLETION:
                    currentProgress = player.destronGas70CompletionCount;
                    break;
                case ConstKOL.KOL_QUEST_TYPE_PVP_WINS:
                    currentProgress = player.martialArtsTournamentWins;
                    break;
                case ConstKOL.KOL_QUEST_TYPE_DAILY_QUEST_COMPLETION:
                    currentProgress = player.dailySuperHardQuestCompletionCount;
                    break;
                case ConstKOL.KOL_QUEST_TYPE_BOSS_DEFEAT_PARTICIPATION:
                    currentProgress = player.bossBabyDefeatParticipationCount;
                    break;
                case ConstKOL.KOL_QUEST_TYPE_MONSTER_KILL_COUNT:
                    currentProgress = player.monsterKillCountAutoTrain;
                    break;
                default:
                    Service.gI().sendThongBao(player, "Lỗi: Loại nhiệm vụ không xác định.");
                    return;
            }

            if (currentProgress >= questData.requiredQuantity) {
                if (questData.questType == ConstKOL.KOL_QUEST_TYPE_ITEM_COLLECTION) {
                    Item requiredItem = InventoryService.gI().findItemBag(player, questData.itemId);
                    if (requiredItem != null) {
                        InventoryService.gI().subQuantityItemsBag(player, requiredItem, questData.requiredQuantity);
                    }
                }

                for (RewardItem rewardData : questData.rewards) {
                    Item rewardItem = ItemService.gI().createNewItem((short) rewardData.itemId);
                    rewardItem.quantity = rewardData.quantity;

                    List<Item.ItemOption> options = getPetBackpackOptions(rewardData.itemId);
                    if (options != null) {
                        rewardItem.itemOptions.addAll(options);
                    }

                    InventoryService.gI().addItemBag(player, rewardItem);
                }
                InventoryService.gI().sendItemBag(player);

                Service.gI().sendThongBao(player, "Bạn đã nhận phần thưởng nhiệm vụ " + (isVIP ? "KOL VIP" : "KOL") + " cấp " + currentStage + "!");

                if (isVIP) {
                    player.kolVIPQuestStage++;
                } else {
                    player.kolQuestStage++;
                }

                openBaseMenu(player);

            } else {
                Service.gI().sendThongBao(player, "Bạn không đủ điều kiện hoàn thành nhiệm vụ!");
                openBaseMenu(player);
            }
        } else {
            openBaseMenu(player);
        }
    }

    private List<Item.ItemOption> getPetBackpackOptions(int itemId) {
        List<Item.ItemOption> options = new ArrayList<>();
        options.add(new Item.ItemOption(73, 0));

        switch (itemId) {
            case 1360:
                options.add(new Item.ItemOption(77, 13));
                options.add(new Item.ItemOption(103, 13));
                options.add(new Item.ItemOption(50, 13));
                options.add(new Item.ItemOption(101, 20));
                options.add(new Item.ItemOption(30, 1));
                options.add(new Item.ItemOption(93, 90));
                break;
            case 1654:
                options.add(new Item.ItemOption(50, 16));
                options.add(new Item.ItemOption(77, 15));
                options.add(new Item.ItemOption(103, 15));
                options.add(new Item.ItemOption(106, 0));
                options.add(new Item.ItemOption(30, 1));
                options.add(new Item.ItemOption(93, 30));
                break;
            case 1797:
                options.add(new Item.ItemOption(50, 25));
                options.add(new Item.ItemOption(103, 30));
                options.add(new Item.ItemOption(30, 1));
                options.add(new Item.ItemOption(93, 90));
                break;
            default:
                return null;
        }
        return options;
    }

    private String formatRewardDetails(List<RewardItem> rewards) {
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < rewards.size(); i++) {
            RewardItem rewardData = rewards.get(i);
            String rewardName = ItemService.gI().getTemplate(rewardData.itemId).name;
            details.append(rewardData.quantity).append(" ").append(rewardName);
            if (i < rewards.size() - 1) {
                details.append(", ");
            }
        }
        return details.toString();
    }
}
