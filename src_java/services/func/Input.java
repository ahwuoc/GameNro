package services.func;

import clan.Clan;
import clan.ClanMember;
import jdbc.DBConnecter;
import skill.Skill;
import consts.ConstNpc;
import consts.ConstTaskBadges;
import item.Item;
import item.Item.ItemOption;
import map.Zone;
import minigame.cost.LuckyNumberCost;
import minigame.LuckyNumber.LuckyNumberService;
import npc.Npc;
import minigame.RubyGemGame.RubyGemGame;
import npc.NpcManager;
import player.Player;
import network.Message;
import network.inetwork.ISession;
import server.Client;
import services.Service;
import models.GiftCode.GiftCodeService;
import services.InventoryService;
import services.ItemService;
import services.NpcService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import jdbc.NDVResultSet;
import jdbc.daos.NDVSqlFetcher;
import jdbc.daos.PlayerDAO;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import player.Inventory;
import server.Manager;
import services.ClanService;
import services.PlayerService;
import task.Badges.BadgesTaskService;
import bot.*;
import utils.Util;
import bot.Bot;
import bot.BotData;
import bot.BotManager;
import bot.BotNPC;
import bot.BotType;

public class Input {

    private static final Map<Integer, Object> PLAYER_ID_OBJECT = new HashMap<>();

    public static final int CHANGE_PASSWORD = 500;
    public static final int GIFT_CODE = 501;
    public static final int FIND_PLAYER = 502;
    public static final int CHANGE_NAME = 503;
    public static final int CHOOSE_LEVEL_BDKB = 504;
    public static final int NAP_THE = 505;
    public static final int CHANGE_NAME_BY_ITEM = 506;
    public static final int GIVE_IT = 507;
    public static final int GET_IT = 508;
    public static final int DANGKY = 509;
    public static final int CHOOSE_LEVEL_KGHD = 510;
    public static final int CHOOSE_LEVEL_CDRD = 511;
    public static final int DISSOLUTION_CLAN = 513;

    public static final int SELECT_LUCKYNUMBER = 514;

    public static final int DOI_VND = 515;
    public static final int DOI_THOI_VANG = 516;
    public static final int DOI_NGOC_XANH = 517;
    public static final int DOI_NGOC_HONG = 518;
    public static final int BUFFVND = 519;
    public static final int SEND_ITEM = 520;
    public static final int SEND_NGOC = 521;

    // Bot spawn input types
    public static final int SPAWN_BOT_FARM_MOB = 600;
    public static final int SPAWN_BOT_FARM_BOSS = 601;
    public static final int SPAWN_BOT_NPC = 602;
    public static final int SPAWN_BOT_FARM_DE_TU = 603;
    public static final int REMOVE_ALL_BOTS = 604;
    public static final int BET_RUBY_GAME = 605;
    public static final byte NUMERIC = 0;
    public static final byte ANY = 1;
    public static final byte PASSWORD = 2;
    public static final byte MBV = 23;
    public static final byte BANSLL = 24;
    public static final byte BANGHOI = 25;

    private static Input intance;

    private Input() {

    }

    public static Input gI() {
        if (intance == null) {
            intance = new Input();
        }
        return intance;
    }

    public void doInput(Player player, Message msg) {
        try {
            String[] text = new String[msg.reader().readByte()];
            for (int i = 0; i < text.length; i++) {
                text[i] = msg.reader().readUTF();
            }
            switch (player.iDMark.getTypeInput()) {
                case BET_RUBY_GAME: {
                    try {
                        int amount = Integer.parseInt(text[0].trim());
                        int side = (int) PLAYER_ID_OBJECT.get((int) player.id);
                        if (amount < 2000) {
                            Service.gI().sendThongBao(player, "Cược tối thiểu 2.000 Coin");
                            return;
                        }
                        if (amount > 2000000000) {
                            Service.gI().sendThongBao(player, "Cược tối đa 2.000.000.000 Coin");
                            return;
                        }
                        RubyGemGame.gI().addBet(player, side, amount);
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Vui lòng nhập số hợp lệ");
                    }
                    break;
                }
                case SEND_NGOC: {
                    int sl = Integer.parseInt(text[1]);
                    if (sl > 500) {
                        Service.gI().sendThongBaoOK(player, "Số ngọc tặng tối đa không vượt quá 500");
                        return;
                    }
                    if (player.inventory.gem < sl) {
                        Service.gI().sendThongBaoOK(player, "Bạn không đủ " + sl + " ngọc");
                        return;
                    }
                    String plName = text[0].trim();
                    Player pBuffItem = NDVSqlFetcher.loadPlayerByName(text[0].trim());
                    if (pBuffItem != null) {
                        pBuffItem.inventory.gem += sl;
                        player.inventory.gem -= sl;
                        PlayerService.gI().sendInfoHpMpMoney(player);
                        PlayerService.gI().sendInfoHpMpMoney(pBuffItem);
                        Service.gI().sendThongBao(player,
                                "Bạn vừa gửi " + sl + " Ngọc xanh thành công cho " + pBuffItem.name);
                        Service.gI().sendThongBao(pBuffItem,
                                "Bạn vừa Nhận Được " + sl + " Ngọc xanh từ " + player.name);
                    } else {
                        Service.gI().sendThongBao(player, "Player không tồn tại");
                    }

                    break;
                }
                case SEND_ITEM: {
                    String itemIds = text[1];
                    String option = text[2];
                    int slItemBuff = Integer.parseInt(text[3]);
                    if (slItemBuff > 9999) {
                        Service.gI().sendThongBaoOK(player, "Buff vượt số lượng giới hạn vui lòng để tối đa sl 9999");
                        return;
                    }
                    String plName = text[0].trim();
                    if (plName.equals("all")) {
                        new Thread(() -> {
                            List<Player> allPlayer = NDVSqlFetcher.getAllPlayer();
                            for (Player pBuffItem : allPlayer) {
                                if (pBuffItem != null) {
                                    String[] itemIdsArray = itemIds.split(",");
                                    for (String itemId : itemIdsArray) {
                                        int idItemBuff = Integer.parseInt(itemId);
                                        Item itembuff = ItemService.gI().createNewItem((short) idItemBuff, slItemBuff);

                                        if (option != null) {
                                            String[] Option = option.split(",");
                                            if (Option.length > 0) {
                                                for (int i = 0; i < Option.length; i++) {
                                                    String[] optItem = Option[i].split("-");
                                                    int optID = Integer.parseInt(optItem[0]);
                                                    int param = Integer.parseInt(optItem[1]);
                                                    itembuff.itemOptions.add(new ItemOption(optID, param));
                                                }
                                            }
                                        }
                                        pBuffItem.inventory.itemsMailBox.add(itembuff);

                                        if (NDVSqlFetcher.updateMailBox(pBuffItem)) {
                                            Service.gI().sendThongBao(player, "Bạn vừa gửi " + itembuff.template.name
                                                    + " thành công cho " + pBuffItem.name);
                                        }
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Player không tồn tại");
                                }
                            }
                        }).start();
                    } else {
                        Player pBuffItem = NDVSqlFetcher.loadPlayerByName(text[0].trim());
                        if (pBuffItem != null) {
                            String[] itemIdsArray = itemIds.split(",");
                            for (String itemId : itemIdsArray) {
                                int idItemBuff = Integer.parseInt(itemId);
                                Item itembuff = ItemService.gI().createNewItem((short) idItemBuff, slItemBuff);
                                if (option != null) {
                                    String[] Option = option.split(",");
                                    if (Option.length > 0) {
                                        for (int i = 0; i < Option.length; i++) {
                                            String[] optItem = Option[i].split("-");
                                            int optID = Integer.parseInt(optItem[0]);
                                            int param = Integer.parseInt(optItem[1]);
                                            itembuff.itemOptions.add(new ItemOption(optID, param));
                                        }
                                    }
                                }
                                pBuffItem.inventory.itemsMailBox.add(itembuff);
                                if (NDVSqlFetcher.updateMailBox(pBuffItem)) {
                                    Service.gI().sendThongBao(player, "Bạn vừa gửi " + itembuff.template.name
                                            + " thành công cho " + pBuffItem.name);
                                }
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Player không tồn tại");
                        }
                    }
                    break;
                }
                case BUFFVND: {
                    try {
                        int idacc = Integer.parseInt(text[0].trim());
                        int addcash = Integer.parseInt(text[1].trim());
                        if (PlayerDAO.addcash(idacc, addcash)) {
                            Service.gI().sendThongBao(player, "Bạn đã buff cho " + idacc + " " + addcash + " VNĐ");
                            if (Client.gI().getPlayerByUser(idacc) != null) {
                                Client.gI().getPlayerByUser(idacc).getSession().cash += addcash;
                                Service.gI().sendThongBao(Client.gI().getPlayerByUser(idacc),
                                        "Bạn vừa được cộng " + addcash + "COIN bởi " + player.name);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Service.gI().sendThongBao(player, "Đã có lỗi xảy ra");
                    }
                    break;
                }
                case DOI_THOI_VANG: {
                    try {
                        long coinLong = Long.parseLong(text[0].trim());
                        // Kiểm tra tràn số và số âm
                        if (coinLong <= 0 || coinLong > Integer.MAX_VALUE) {
                            Service.gI().sendThongBao(player, "Số tiền không hợp lệ");
                            return;
                        }
                        int coin = (int) coinLong;
                        // Kiểm tra giới hạn
                        if (coin < 20000 || coin > 100000000) {
                            Service.gI().sendThongBao(player, "Chọn 1 con số từ 20.000 đến 100.000.000");
                            return;
                        }
                        // Kiểm tra session và cash
                        if (player.getSession() == null || player.getSession().cash < coin) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + coin + " VND");
                            return;
                        }
                        int sl = coin / 200;
                        if (sl <= 0) {
                            Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                            return;
                        }
                        if (PlayerDAO.subcash(player, coin)) {
                            Item thoiVang = ItemService.gI().createNewItem((short) 457, sl);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBag(player);
                            BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, coin);
                            Service.gI().sendThongBao(player, "Bạn nhận được " + sl + " " + thoiVang.template.name);
                        } else {
                            Service.gI().sendThongBao(player, "Giao dịch thất bại, vui lòng thử lại");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng nhập số hợp lệ");
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Có lỗi xảy ra, vui lòng thử lại");
                    }
                }
                    break;
                case DOI_NGOC_XANH: {
                    try {
                        long coinLong = Long.parseLong(text[0].trim());
                        // Kiểm tra tràn số và số âm
                        if (coinLong <= 0 || coinLong > Integer.MAX_VALUE) {
                            Service.gI().sendThongBao(player, "Số tiền không hợp lệ");
                            return;
                        }
                        int coin = (int) coinLong;
                        // Kiểm tra giới hạn
                        if (coin < 1000 || coin > 100000000) {
                            Service.gI().sendThongBao(player, "Chọn 1 con số từ 1.000 đến 100.000.000");
                            return;
                        }
                        // Kiểm tra session và cash
                        if (player.getSession() == null || player.getSession().cash < coin) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + coin + " VND");
                            return;
                        }
                        int sl = coin / 10;
                        if (sl <= 0) {
                            Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                            return;
                        }
                        if (PlayerDAO.subcash(player, coin)) {
                            Item thoiVang = ItemService.gI().createNewItem((short) 77, sl);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBag(player);
                            BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, coin);
                            Service.gI().sendThongBao(player, "Bạn nhận được " + sl + " " + thoiVang.template.name);
                        } else {
                            Service.gI().sendThongBao(player, "Giao dịch thất bại, vui lòng thử lại");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng nhập số hợp lệ");
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Có lỗi xảy ra, vui lòng thử lại");
                    }
                }
                    break;
                case DOI_NGOC_HONG: {
                    try {
                        long coinLong = Long.parseLong(text[0].trim());
                        // Kiểm tra tràn số và số âm
                        if (coinLong <= 0 || coinLong > Integer.MAX_VALUE) {
                            Service.gI().sendThongBao(player, "Số tiền không hợp lệ");
                            return;
                        }
                        int coin = (int) coinLong;
                        // Kiểm tra giới hạn
                        if (coin < 20000 || coin > 100000000) {
                            Service.gI().sendThongBao(player, "Chọn 1 con số từ 20.000 đến 100.000.000");
                            return;
                        }
                        // Kiểm tra session và cash
                        if (player.getSession() == null || player.getSession().cash < coin) {
                            Service.gI().sendThongBao(player, "Bạn không đủ " + coin + " VND");
                            return;
                        }
                        int sl = coin / 2;
                        if (sl <= 0) {
                            Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                            return;
                        }
                        if (PlayerDAO.subcash(player, coin)) {
                            Item thoiVang = ItemService.gI().createNewItem((short) 861, sl);
                            InventoryService.gI().addItemBag(player, thoiVang);
                            InventoryService.gI().sendItemBag(player);
                            Service.gI().sendThongBao(player, "Bạn nhận được " + sl + " " + thoiVang.template.name);
                        } else {
                            Service.gI().sendThongBao(player, "Giao dịch thất bại, vui lòng thử lại");
                        }
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Vui lòng nhập số hợp lệ");
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Có lỗi xảy ra, vui lòng thử lại");
                    }
                }
                    break;
                case GIVE_IT:
                    String name = text[0];
                    int id = Integer.parseInt(text[1]);
                    int op = Integer.parseInt(text[2]);
                    int pr = Integer.parseInt(text[3]);
                    int q = Integer.parseInt(text[4]);

                    if (Client.gI().getPlayer(name) != null) {
                        Item item = ItemService.gI().createNewItem(((short) id));
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) id);
                        if (!ops.isEmpty()) {
                            item.itemOptions = ops;
                        }
                        item.quantity = q;
                        item.itemOptions.add(new Item.ItemOption(op, pr));
                        InventoryService.gI().addItemBag(Client.gI().getPlayer(name), item);
                        InventoryService.gI().sendItemBag(Client.gI().getPlayer(name));
                        Service.gI().sendThongBao(Client.gI().getPlayer(name),
                                "Nhận " + item.template.name + " từ " + player.name);

                    } else {
                        Service.gI().sendThongBao(player, "Không online");
                    }
                    break;
                case GET_IT:
                    id = Integer.parseInt(text[0]);
                    op = Integer.parseInt(text[1]);
                    pr = Integer.parseInt(text[2]);
                    q = Integer.parseInt(text[3]);

                    if (player.isAdmin()) {
                        Item item = ItemService.gI().createNewItem(((short) id));
                        List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) id);
                        if (!ops.isEmpty()) {
                            item.itemOptions = ops;
                        }
                        item.quantity = q;
                        item.itemOptions.add(new Item.ItemOption(op, pr));
                        InventoryService.gI().addItemBag(player, item);
                        InventoryService.gI().sendItemBag(player);
                        Service.gI().sendThongBao(player, "Nhận " + item.template.name + " !");

                    } else {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                    }
                    break;
                case CHANGE_PASSWORD:
                    Service.gI().changePassword(player, text[0], text[1], text[2]);
                    break;
                case GIFT_CODE:
                    GiftCodeService.gI().giftCode(player, text[0]);
                    break;
                case FIND_PLAYER:
                    Player pl = Client.gI().getPlayer(text[0]);
                    if (pl != null) {
                        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_FIND_PLAYER, -1, "Ngài muốn..?",
                                new String[] { "Đi tới\n" + pl.name, "Gọi " + pl.name + "\ntới đây", "Đổi tên", "Ban",
                                        "Kick" },
                                pl);
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                    break;
                case CHANGE_NAME: {
                    Player plChanged = (Player) PLAYER_ID_OBJECT.get((int) player.id);
                    if (plChanged != null) {
                        if (DBConnecter.executeQuery("select * from player where name = ?", text[0]).next()) {
                            Service.gI().sendThongBao(player, "Tên nhân vật đã tồn tại");
                        } else {
                            plChanged.name = text[0];
                            DBConnecter.executeUpdate("update player set name = ? where id = ?", plChanged.name,
                                    plChanged.id);
                            Service.gI().player_loader_info(plChanged);
                            Service.gI().Send_Caitrang(plChanged);
                            Service.gI().sendFlagBag(plChanged);
                            Zone zone = plChanged.zone;
                            ChangeMapService.gI().changeMap(plChanged, zone, plChanged.location.x,
                                    plChanged.location.y);
                            Service.gI().sendThongBao(plChanged,
                                    "Chúc mừng bạn đã có cái tên mới đẹp đẽ hơn tên ban đầu");
                            Service.gI().sendThongBao(player, "Đổi tên người chơi thành công");
                        }
                    }
                }
                    break;
                case CHANGE_NAME_BY_ITEM: {
                    if (player != null) {
                        if (DBConnecter.executeQuery("select * from player where name = ?", text[0]).next()) {
                            Service.gI().sendThongBao(player, "Tên nhân vật đã tồn tại");
                            createFormChangeNameByItem(player);
                        } else if (Util.haveSpecialCharacter(text[0])) {
                            Service.gI().sendThongBaoOK(player, "Tên nhân vật không được chứa ký tự đặc biệt");
                        } else if (text[0].length() < 5) {
                            Service.gI().sendThongBaoOK(player, "Tên nhân vật quá ngắn");
                        } else if (text[0].length() > 10) {
                            Service.gI().sendThongBaoOK(player,
                                    "Tên nhân vật chỉ đồng ý các ký tự a-z, 0-9 và chiều dài từ 5 đến 10 ký tự");
                        } else {
                            Item theDoiTen = InventoryService.gI().findItem(player.inventory.itemsBag, 2006);
                            if (theDoiTen == null) {
                                Service.gI().sendThongBao(player, "Không tìm thấy thẻ đổi tên");
                            } else {
                                InventoryService.gI().subQuantityItemsBag(player, theDoiTen, 1);
                                player.name = text[0].toLowerCase();
                                DBConnecter.executeUpdate("update player set name = ? where id = ?", player.name,
                                        player.id);
                                Service.gI().player_loader_info(player);
                                Service.gI().Send_Caitrang(player);
                                Service.gI().sendFlagBag(player);
                                Zone zone = player.zone;
                                ChangeMapService.gI().changeMap(player, zone, player.location.x, player.location.y);
                                Service.gI().sendThongBao(player,
                                        "Chúc mừng bạn đã có cái tên mới đẹp đẽ hơn tên ban đầu");
                            }
                        }
                    }
                }
                    break;
                case CHOOSE_LEVEL_BDKB:
                    int level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.QUY_LAO_KAME, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, ConstNpc.MENU_ACCEPT_GO_TO_BDKB,
                                    "Con có chắc muốn đến\nhang kho báu cấp độ " + level + " ?",
                                    new String[] { "Đồng ý", "Từ chối" }, level);
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }

                    break;
                case CHOOSE_LEVEL_KGHD:
                    level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.MR_POPO, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, 2,
                                    "Cậu có chắc muốn đến\nDestron Gas cấp độ " + level + " ?",
                                    new String[] { "Đồng ý", "Từ chối" }, level);
                        }
                    }
                    break;
                case CHOOSE_LEVEL_CDRD:
                    level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.THAN_VU_TRU, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, 3,
                                    "Con có chắc muốn đến\ncon đường rắn độc cấp độ " + level + " ?",
                                    new String[] { "Đồng ý", "Từ chối" }, level);
                        }
                    }
                    break;
                case MBV:
                    int mbv = Integer.parseInt(text[0]);
                    int nmbv = Integer.parseInt(text[1]);
                    int rembv = Integer.parseInt(text[2]);
                    if ((mbv + "").length() != 6 || (nmbv + "").length() != 6 || (rembv + "").length() != 6) {
                        Service.gI().sendThongBao(player, "Trêu bố mày à?");
                    } else if (player.mbv == 0) {
                        Service.gI().sendThongBao(player, "Bạn chưa cài mã bảo vệ!");
                    } else if (player.mbv != mbv) {
                        Service.gI().sendThongBao(player, "Mã bảo vệ không đúng");
                    } else if (nmbv != rembv) {
                        Service.gI().sendThongBao(player, "Mã bảo vệ không trùng khớp");
                    } else {
                        player.mbv = nmbv;
                        Service.gI().sendThongBao(player, "Đổi mã bảo vệ thành công!");
                    }
                    break;
                case BANSLL:
                    int sltv;
                    try {
                        sltv = Integer.parseInt(text[0]);
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                        return;
                    }

                    if (sltv <= 0) {
                        Service.gI().sendThongBao(player, "Số lượng phải lớn hơn 0");
                        return;
                    }

                    Item ThoiVang = InventoryService.gI().findItemBag(player, 457);
                    if (ThoiVang == null) {
                        Service.gI().sendThongBao(player, "Bạn không có Thỏi vàng");
                        return;
                    }

                    if (ThoiVang.quantity < sltv) {
                        Service.gI().sendThongBao(player, "Bạn chỉ có " + ThoiVang.quantity + " Thỏi vàng");
                        return;
                    }

                    long cost = (long) sltv * 500_000_000L;
                    if (player.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                        int slban = (int) ((Inventory.LIMIT_GOLD - player.inventory.gold) / 500_000_000L);
                        if (slban < 1) {
                            Service.gI().sendThongBao(player, "Vàng sau khi bán vượt quá giới hạn");
                        } else {
                            Service.gI().sendThongBao(player, "Bạn chỉ có thể bán tối đa " + slban + " Thỏi vàng");
                        }
                        return;
                    }

                    InventoryService.gI().subQuantityItemsBag(player, ThoiVang, sltv);
                    InventoryService.gI().sendItemBag(player);
                    player.inventory.gold += cost;
                    Service.gI().sendMoney(player);
                    Service.gI().sendThongBao(player,
                            "Đã bán " + sltv + " Thỏi vàng thu được " + Util.numberToMoney(cost) + " vàng");
                    TransactionService.gI().cancelTrade(player);
                    break;

                case BANGHOI:
                    Clan clan = player.clan;
                    if (clan != null) {
                        ClanMember cm = clan.getClanMember((int) player.id);
                        if (clan.isLeader(player)) {
                            if (clan.canUpdateClan(player)) {
                                String tenvt = text[0];
                                if (!Util.haveSpecialCharacter(tenvt) && tenvt.length() > 1 && tenvt.length() < 5) {
                                    clan.name2 = tenvt;
                                    clan.update();
                                    Service.gI().sendThongBao(player, "[" + tenvt + "] OK");
                                } else {
                                    Service.gI().sendThongBaoOK(player,
                                            "Chỉ chấp nhận các ký tự a-z, 0-9 và chiều dài từ 2 đến 4 ký tự");
                                }
                            }
                        }
                    }
                    break;
                case DISSOLUTION_CLAN:
                    String xacNhan = text[0];
                    if (xacNhan.equalsIgnoreCase("OK")) {
                        clan = player.clan;
                        if (clan.isLeader(player)) {
                            clan.deleteDB(clan.id);
                            Manager.CLANS.remove(clan);
                            player.clan = null;
                            player.clanMember = null;
                            ClanService.gI().sendMyClan(player);
                            ClanService.gI().sendClanId(player);
                            Service.gI().sendThongBao(player, "Bang hội đã giải tán thành công.");
                        }
                    }
                    break;
                case SELECT_LUCKYNUMBER: {
                    int number = Integer.parseInt(text[0]);
                    LuckyNumberService.addNumber(player, number);
                }
                    break;

                // ==================== Bot Spawn Handlers ====================
                case SPAWN_BOT_FARM_MOB: {
                    System.out.println("DEBUG: Handle input SPAWN_BOT_FARM_MOB");
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                        return;
                    }
                    try {
                        String prefix = "BotFarm";
                        int quantity = Integer.parseInt(text[0].trim());

                        if (quantity <= 0 || quantity > 100) {
                            Service.gI().sendThongBao(player, "Số lượng bot phải từ 1 đến 100");
                            return;
                        }

                        for (int i = 0; i < quantity; i++) {
                            int botId = (int) (System.currentTimeMillis() % 100000) + i;
                            String botName = generateBotName(prefix);
                            BotData data = new BotData();
                            data.setName(botName);
                            byte gender = (byte) Util.nextInt(0, 2);
                            data.setGender(gender);

                            short[][] HEADS = {
                                    { 64, 30, 32 }, // Trai Dat
                                    { 9, 29, 32 }, // Namek
                                    { 6, 27, 28 } // Xayda
                            };
                            short[][] BODIES = {
                                    { 14 }, // Trai Dat
                                    { 10 }, // Namek,3
                                    { 16 } // 0
                            };
                            short[][] LEGS = {
                                    { 15 }, // Trai Dat
                                    { 11 }, // Namek
                                    { 17 } // Xayda
                            };

                            short[] selectedOutfit = new short[6];
                            selectedOutfit[0] = HEADS[gender][Util.nextInt(HEADS[gender].length)];
                            selectedOutfit[1] = BODIES[gender][Util.nextInt(BODIES[gender].length)];
                            selectedOutfit[2] = LEGS[gender][Util.nextInt(LEGS[gender].length)];
                            selectedOutfit[3] = 0;
                            selectedOutfit[4] = 0;
                            selectedOutfit[5] = 0;

                            data.setOutfit(selectedOutfit);
                            data.setDame(Util.nextInt(10000, 50000));
                            data.setHp(new long[] { Util.nextInt(100000, 500000) });
                            data.mp = Util.nextInt(50000, 100000);
                            data.def = Util.nextInt(1000, 5000);
                            data.crit = Util.nextInt(5, 15);
                            data.power = Util.nextInt(1000, 2_000_000);

                            int[][] mapsByGender = BotManager.map;

                            if (gender < 0 || gender >= mapsByGender.length)
                                gender = 0;

                            int[] genderMaps = mapsByGender[gender];
                            int randomMapId = genderMaps[Util.nextInt(0, genderMaps.length - 1)];

                            data.setMapJoin(new int[] { randomMapId });
                            data.setSkillTemp(new int[][] {
                                    { Skill.DRAGON, 1 },

                            });
                            data.setSecondsRest(1);
                            data.setBotType(BotType.FARM_MOB);
                            BotFarmMob bot = BotManager.gI().createBotFarmMob(botId, data);
                            item.Item pet = getRandomItemFromShop("Shop_Vip", 72);
                            if (pet != null) {
                                bot.petFollowId = (short) (pet.template.iconID - 1);
                            }
                            item.Item title = getRandomItemFromShop("Shop_Vip", 73);
                            if (title != null) {
                                bot.titleId = (short) title.template.part;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
                    }
                }
                    break;

                case SPAWN_BOT_FARM_BOSS: {
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                        return;
                    }
                    try {
                        String prefix = "BotBoss";
                        int quantity = Integer.parseInt(text[0].trim());
                        int mapId = 113;

                        if (quantity <= 0 || quantity > 50) {
                            Service.gI().sendThongBao(player, "Số lượng bot phải từ 1 đến 50");
                            return;
                        }

                        for (int i = 0; i < quantity; i++) {
                            int botId = (int) (System.currentTimeMillis() % 100000) + i;
                            String botName = generateBotName(prefix);
                            BotData data = new BotData();
                            data.setName(botName);

                            data.setGender((byte) Util.nextInt(0, 2));
                            data.setOutfit(getRandomOutfitFromShops(data.getGender()));
                            data.setDame(Util.nextInt(50000, 100000));
                            data.setHp(new long[] { Util.nextInt(500000, 2000000) });
                            data.mp = Util.nextInt(200000, 500000);
                            data.def = Util.nextInt(5000, 15000);
                            data.crit = Util.nextInt(10, 25);
                            data.power = Util.nextLong(2_000_000_000L, 100_000_000_000L);
                            data.setMapJoin(new int[] { mapId });
                            data.setSkillTemp(new int[][] {
                                    { Skill.DRAGON, 7 },
                                    { Skill.KAMEJOKO, 7 },
                            });
                            data.setSecondsRest(120);
                            data.setBotType(BotType.FARM_BOSS);

                            BotFarmBoss bot = BotManager.gI().createBotFarmBoss(botId, data);
                            item.Item pet = getRandomItemFromShop("Shop_Vip", 72);
                            if (pet != null) {
                                bot.petFollowId = (short) (pet.template.iconID - 1);
                            }
                            item.Item title = getRandomItemFromShop("Shop_Vip", 73);
                            if (title != null) {
                                bot.titleId = (short) title.template.part;
                            }
                        }
                        Service.gI().sendThongBao(player,
                                "Đã tạo " + quantity + " Bot Farm Quái (Map Random theo Gender)");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
                    }
                }
                    break;

                case SPAWN_BOT_NPC: {
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                        return;
                    }
                    try {
                        String prefix = "BotNPC";
                        int quantity = Integer.parseInt(text[0].trim());
                        int targetMapId = 5;
                        int targetNpcId = 39;

                        if (quantity <= 0 || quantity > 50) {
                            Service.gI().sendThongBao(player, "Số lượng bot phải từ 1 đến 50");
                            return;
                        }

                        for (int i = 0; i < quantity; i++) {
                            int botId = (int) (System.currentTimeMillis() % 100000) + i;
                            String botName = generateBotName(prefix);
                            byte gender = (byte) Util.nextInt(0, 2);

                            short[] selectedOutfit = getRandomOutfitFromShops(gender);

                            BotData data = new BotData(
                                    botName,
                                    gender,
                                    selectedOutfit,
                                    Util.nextInt(5000, 20000),
                                    new long[] { Util.nextInt(50000, 200000) },
                                    new int[] { targetMapId },
                                    new int[][] { { 1, 1 } },
                                    60,
                                    targetNpcId,
                                    targetMapId);

                            BotNPC bot = BotManager.gI().createBotNPC(botId, data);
                            data.mp = Util.nextInt(25000, 100000);
                            data.def = Util.nextInt(500, 2000);
                            data.crit = Util.nextInt(5, 15);
                            data.power = Util.nextLong(2_000_000_000L, 100_000_000_000L);
                            item.Item pet = getRandomItemFromShop("Shop_Vip", 72);
                            if (pet != null) {
                                bot.petFollowId = (short) (pet.template.iconID - 1);
                            }
                            item.Item title = getRandomItemFromShop("Shop_Vip", 73);
                            if (title != null) {
                                bot.titleId = (short) title.template.part;
                            }
                        }
                        Service.gI().sendThongBao(player, "Đã tạo " + quantity + " Bot NPC tới NPC " + targetNpcId);
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
                    }
                }
                    break;

                case SPAWN_BOT_FARM_DE_TU: {
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                        return;
                    }
                    try {
                        String prefix = "BotDeTu";
                        int quantity = Integer.parseInt(text[0].trim());
                        int[] farmMaps = bot.BotFarmDeTu.FARM_MAPS;

                        if (quantity <= 0 || quantity > 50) {
                            Service.gI().sendThongBao(player, "Số lượng bot phải từ 1 đến 50");
                            return;
                        }

                        for (int i = 0; i < quantity; i++) {
                            int mapId = farmMaps[utils.Util.nextInt(0, farmMaps.length - 1)];
                            int gender = Util.nextInt(0, 2);
                            short[] selectedOutfit = getRandomOutfitFromShops(gender);

                            int botId = (int) (System.currentTimeMillis() % 100000) + i;
                            String botName = generateBotName(prefix);
                            BotData data = new BotData();
                            data.setName(botName);
                            data.setGender((byte) gender);
                            data.setOutfit(selectedOutfit);
                            data.setDame(5000L);
                            data.power = 1000000L;
                            data.setHp(new long[] { Util.nextInt(10000, 200000) });
                            data.setMapJoin(new int[] { mapId });
                            data.setSkillTemp(new int[][] { { 1, 1 } });
                            data.setSecondsRest(60);
                            data.setBotType(BotType.FARM_DE_TU);

                            BotFarmDeTu bot = BotManager.gI().createBotFarmDeTu(botId, data);
                            item.Item pet = getRandomItemFromShop("Shop_Vip", 72);
                            if (pet != null) {
                                bot.petFollowId = (short) (pet.template.iconID - 1);
                            }
                            item.Item title = getRandomItemFromShop("Shop_Vip", 73);
                            if (title != null) {
                                bot.titleId = (short) title.template.part;
                            }
                        }
                        Service.gI().sendThongBao(player, "Đã tạo " + quantity + " Bot Farm Đệ Tử (Random Maps)");
                    } catch (Exception e) {
                        Service.gI().sendThongBao(player, "Lỗi: " + e.getMessage());
                    }
                }
                    break;

                case REMOVE_ALL_BOTS: {
                    if (!player.isAdmin()) {
                        Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
                        return;
                    }
                    String confirm = text[0].trim();
                    if (confirm.equalsIgnoreCase("OK")) {
                        java.util.List<Bot> allBots = BotManager.gI().getAllBots();
                        int count = allBots.size();
                        for (Bot bot : allBots) {
                            bot.leaveMap();
                            BotManager.gI().removeBot(bot);
                        }
                        Service.gI().sendThongBao(player, "Đã xóa " + count + " bot");
                    }
                }
                    break;
            }
        } catch (

        Exception e) {
        }
    }

    public void createForm(Player pl, int typeInput, String title, SubInput... subInputs) {
        pl.iDMark.setTypeInput(typeInput);
        Message msg = null;
        try {
            msg = new Message(-125);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(subInputs.length);
            for (SubInput si : subInputs) {
                msg.writer().writeUTF(si.name);
                msg.writer().writeByte(si.typeInput);
            }
            pl.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void createForm(ISession session, int typeInput, String title, SubInput... subInputs) {
        Message msg = null;
        try {
            msg = new Message(-125);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(subInputs.length);
            for (SubInput si : subInputs) {
                msg.writer().writeUTF(si.name);
                msg.writer().writeByte(si.typeInput);
            }
            session.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void createFormChangePassword(Player pl) {
        createForm(pl, CHANGE_PASSWORD, "Đổi mật khẩu", new SubInput("Mật khẩu cũ", PASSWORD),
                new SubInput("Mật khẩu mới", PASSWORD),
                new SubInput("Nhập lại mật khẩu mới", PASSWORD));
    }

    public void createFormGiveItem(Player pl) {
        createForm(pl, GIVE_IT, "Tặng vật phẩm", new SubInput("Tên", ANY), new SubInput("Id Item", ANY),
                new SubInput("ID OPTION", ANY), new SubInput("PARAM", ANY), new SubInput("Số lượng", ANY));
    }

    public void createFormGetItem(Player pl) {
        createForm(pl, GET_IT, "Get vật phẩm", new SubInput("Id Item", ANY), new SubInput("ID OPTION", ANY),
                new SubInput("PARAM", ANY), new SubInput("Số lượng", ANY));
    }

    public void createFormGiftCode(Player pl) {
        createForm(pl, GIFT_CODE, "GiftCode", new SubInput("Giftcode", ANY));
    }

    public void createFormMBV(Player pl) {
        createForm(pl, MBV, "Đồ ngu! Đồ ăn hại! Cút mẹ mày đi!", new SubInput("Nhập Mã Bảo Vệ Đã Quên", NUMERIC),
                new SubInput("Nhập Mã Bảo Vệ Mới", NUMERIC), new SubInput("Nhập Lại Mã Bảo Vệ Mới", NUMERIC));
    }

    public void createFormBangHoi(Player pl) {
        createForm(pl, BANGHOI, "Nhập tên viết tắt bang hội", new SubInput("Tên viết tắt từ 2 đến 4 kí tự", ANY));
    }

    public void createFormFindPlayer(Player pl) {
        createForm(pl, FIND_PLAYER, "Tìm kiếm người chơi", new SubInput("Tên người chơi", ANY));
    }

    public void createFormNapThe(Player pl, byte loaiThe) {
        pl.iDMark.setLoaiThe(loaiThe);
        createForm(pl, NAP_THE, "Nạp thẻ", new SubInput("Mã thẻ", ANY), new SubInput("Seri", ANY));
    }

    public void createFormChangeName(Player pl, Player plChanged) {
        PLAYER_ID_OBJECT.put((int) pl.id, plChanged);
        createForm(pl, CHANGE_NAME, "Đổi tên " + plChanged.name, new SubInput("Tên mới", ANY));
    }

    public void createFormChangeNameByItem(Player pl) {
        createForm(pl, CHANGE_NAME_BY_ITEM, "Đổi tên " + pl.name, new SubInput("Tên mới", ANY));
    }

    public void createFormChooseLevelBDKB(Player pl) {
        createForm(pl, CHOOSE_LEVEL_BDKB, "Hãy chọn cấp độ hang kho báu từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormChooseLevelCDRD(Player pl) {
        createForm(pl, CHOOSE_LEVEL_CDRD, "Hãy chọn cấp độ từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormChooseLevelKGHD(Player pl) {
        createForm(pl, CHOOSE_LEVEL_KGHD, "Hãy chọn cấp độ từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormBanSLL(Player pl) {
        createForm(pl, BANSLL, "Bạn muốn bán bao nhiêu [Thỏi vàng] ?", new SubInput("Số lượng", NUMERIC));
    }

    public void createFormGiaiTanBangHoi(Player pl) {
        createForm(pl, DISSOLUTION_CLAN, "Nhập OK để xác nhận giải tán bang hội.", new SubInput("", ANY));
    }

    public void createFormDoiVND(Player pl) {

        createForm(pl, DOI_VND, "Đổi VND --> VND < VND x 0.9 >",
                new SubInput("Nhập số lượng VND muốn đổi ra VND", NUMERIC));
    }

    public void createFormDoiThoiVang(Player pl) {

        createForm(pl, DOI_THOI_VANG, "Đổi VND --> Thỏi vàng < Mỗi 10K được 50 thỏi >",
                new SubInput("Nhập số lượng VND muốn đổi ra thỏi vàng", NUMERIC));
    }

    public void createFormDoiNgocXanh(Player pl) {

        createForm(pl, DOI_NGOC_XANH, "Đổi VND --> Ngọc xanh < Mỗi 10K được 1000 ngọc xanh >",
                new SubInput("Nhập số lượng VND muốn đổi ra ngọc xanh", NUMERIC));
    }

    public void createFormDoiNgocHong(Player pl) {

        createForm(pl, DOI_NGOC_HONG, "Đổi VND --> Ngọc hồng < Mỗi 10K được 5.000 ngọc hồng >",
                new SubInput("Nhập số lượng VND muốn đổi ra ngọc hồng", NUMERIC));
    }

    public void createFormSelectOneNumberLuckyNumber(Player pl, boolean isGem) {
        Item tv = InventoryService.gI().findItemBag(pl, 457);
        if (tv == null || tv.quantity < 10) {
            Service.gI().sendThongBao(pl, "Kiếm 10 thỏi vàng rồi anh chó chú chơi");
            return;
        }
        String text = "";
        if (isGem) {
            text = "Hãy chọn 1 số từ 0 đến 99 giá " + Util.numberFormatLouis(LuckyNumberCost.costPlayGem) + " ngọc";
        } else {
            text = "Hãy chọn 1 số từ 0 đến 99 giá 10 thỏi vàng";
        }

        createForm(pl, SELECT_LUCKYNUMBER, text, new SubInput("Số bạn chọn", NUMERIC));
        InventoryService.gI().subQuantityItemsBag(pl, tv, 10);
        InventoryService.gI().sendItemBag(pl);
    }

    public void createFromMailBox(Player pl) {
        createForm(pl, SEND_ITEM, "Hộp thư gửi đến người chơi",
                new SubInput("Tên người chơi", ANY),
                new SubInput("ID Trang Bị", ANY),
                new SubInput("Chuỗi option", ANY),
                new SubInput("Số lượng", NUMERIC));
    }

    public void createTangNgoc(Player pl) {
        createForm(pl, SEND_NGOC, "Nhập Tên người chơi muốn tặng ngọc",
                new SubInput("Tên người chơi", ANY),
                new SubInput("Số lượng ngọc", NUMERIC));
    }

    public void createFormBuffVND(Player player) {
        createForm(player, BUFFVND, "Buff VNĐ",
                new SubInput("id acc người chơi", NUMERIC),
                new SubInput("VNĐ CẦN BUFF", ANY));
    }

    // ==================== Bot Spawn Forms ====================

    public void createFormSpawnBotFarmMob(Player pl) {
        createForm(pl, SPAWN_BOT_FARM_MOB, "Tạo Bot Farm Quái",
                new SubInput("Số lượng (1-100)", NUMERIC));
    }

    public void createFormSpawnBotFarmBoss(Player pl) {
        createForm(pl, SPAWN_BOT_FARM_BOSS, "Tạo Bot Farm Boss",
                new SubInput("Số lượng (1-50)", NUMERIC));
    }

    public void createFormSpawnBotNPC(Player pl) {
        createForm(pl, SPAWN_BOT_NPC, "Tạo Bot NPC",
                new SubInput("Số lượng (1-50)", NUMERIC));
    }

    public void createFormSpawnBotFarmDeTu(Player pl) {
        createForm(pl, SPAWN_BOT_FARM_DE_TU, "Tạo Bot Farm Đệ Tử",
                new SubInput("Số lượng (1-50)", NUMERIC));
    }

    private String generateBotName(String prefix) {
        String p = PREFIXES.get(Util.nextInt(0, PREFIXES.size() - 1));
        String n = NAMES.get(Util.nextInt(0, NAMES.size() - 1));
        return p + n;
    }

    public void createFormRemoveAllBots(Player pl) {
        createForm(pl, REMOVE_ALL_BOTS, "Xóa tất cả Bot",
                new SubInput("Nhập OK để xác nhận", ANY));
    }

    private short[] getRandomOutfitFromShops(int gender) {
        models.Template.ItemTemplate temp = BotManager.gI().getRandomOutfitTemplate(gender);
        if (temp != null) {
            return new short[] { (short) temp.head, (short) temp.body, (short) temp.leg, 0, 0, 0 };
        }
        short head = -1;
        short body = -1;
        short leg = -1;

        switch (gender) {
            case 0:
                head = 64;
                body = 14;
                leg = 15;
                break;
            case 1:
                head = 9;
                body = 10;
                leg = 11;
                break;
            default:
                head = 6;
                body = 16;
                leg = 17;
                break;
        }
        return new short[] { head, body, leg, 0, 0, 0 };
    }

    private item.Item getRandomItemFromShop(String shopName, int type) {
        try {
            for (shop.Shop shop : server.Manager.SHOPS) {
                if (shop.tagName.equals(shopName)) {
                    java.util.List<item.Item> items = new java.util.ArrayList<>();
                    for (shop.TabShop tab : shop.tabShops) {
                        for (shop.ItemShop itemShop : tab.itemShops) {
                            if (itemShop.temp.type == type) {
                                items.add(services.ItemService.gI().createNewItem(((short) itemShop.temp.id)));
                            }
                        }
                    }
                    if (!items.isEmpty()) {
                        return items.get(utils.Util.nextInt(0, items.size() - 1));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class SubInput {

        private String name;
        private byte typeInput;

        public SubInput(String name, byte typeInput) {
            this.name = name;
            this.typeInput = typeInput;
        }
    }

    public static final java.util.List<String> PREFIXES = Arrays.asList(
            "top", "top1", "top2", "top3", "top5", "top10", "top20", "top50", "top100",
            "rank1", "rank2", "rank3",
            "1st", "2nd", "3rd",

            "ss", "ss1", "ss2", "ss3", "ss4", "ss5",
            "ssj", "ssj1", "ssj2", "ssj3", "ssj4",
            "ssgod", "ssblue", "ssrose",
            "ui", "mui", "ego",

            "trum", "boss", "bigboss", "mini", "elite",
            "king", "god", "lord", "master",
            "deity", "overlord", "supreme",

            "vip", "vip1", "vip2", "vip3", "vip4", "vip5",
            "svip", "svip1", "svip2",
            "vippro", "ultravip",
            "pro", "pro1", "pro2",
            "max", "maxlv",

            "pk", "pk1", "pk2",
            "1hit", "2hit", "3hit",
            "onehit", "oneshot", "onekill",
            "crit", "critdmg",

            "sanboss", "boss1", "boss2", "boss3",
            "farm", "farmer", "autofarm",
            "treo", "treolevel",

            "clan", "guild", "team",
            "leader", "coleader", "officer",

            "event", "test", "beta",
            "sv", "sv1", "sv2", "sv3",
            "new", "old",

            "dz", "vjp", "vipdz", "prodz",
            "tryhard", "hardcore",
            "clone", "clone1", "clone2",
            "fake", "real",
            "noob", "newbie",

            "gg", "ez", "afk",
            "chill", "lol", "haha",

            "td", "nm", "xayda",
            "namec", "traidat",

            "auto", "bot", "tool");

    public void createFormBetRuby(Player pl, int side) {
        PLAYER_ID_OBJECT.put((int) pl.id, side);
        String sideStr = (side == RubyGemGame.TAI) ? "Tài" : "Xỉu";
        createForm(pl, BET_RUBY_GAME, "Cược vào " + sideStr, new SubInput("Số tiền cược", NUMERIC));
    }

    public static final java.util.List<String> NAMES = Arrays.asList(
            "admin123",
            "portable",
            "songoku",
            "cadic",
            "iamqiang",
            "kakami",
            "sophan",
            "khayongnoi",
            "luxii",
            "siunamec",
            "xayda",
            "gohan",
            "magiczeus",
            "hai2k1",
            "coi82",
            "1ngay0xaem",
            "anha2",
            "olalal",
            "superbroly",
            "dewar",
            "123bom",
            "vuatrochoi",
            "hakaishin",
            "ngocrong",
            "sieunhan",
            "shinichi",
            "sugucu",
            "xdbom34m",
            "hoang",
            "kamuiz",
            "uranus",
            "octieu",
            "broly",
            "darkwolf",
            "baovy",
            "nmori",
            "doggy",
            "dragon",
            "nguvanngoc",
            "ankaku",
            "solue",
            "nhocway43",
            "thien",
            "obito",
            "uyennhi",
            "nobita",
            "chuberong",
            "vuonglam",
            "datnm",
            "vanthien",
            "emquag",
            "namsisi",
            "vegito",
            "thanhdanh",
            "black",
            "heeee",
            "facebook",
            "bucukhong",
            "lienhoan",
            "chiller",
            "onestar",
            "goten",
            "gaobac",
            "vegeta",
            "mitom",
            "miuxinh",
            "campuchia",
            "mihaohao",
            "trangxinh",
            "phucpikolo",
            "choso",
            "luaga",
            "traidatkun",
            "duykame",
            "lution",
            "anhdaxanh",
            "hdpengon",
            "anzyvyvy",
            "kehuydiet",
            "nhixinh",
            "dautuhdpe",
            "hdpecc",
            "simohayha",
            "ocbuden",
            "hanoi");
}
