package services.func;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import Bot.NewBot;
import Bot.ShopBot;
import clan.Clan;
import clan.ClanMember;
import jdbc.DBConnecter;
import consts.ConstNpc;
import consts.ConstTaskBadges;
import item.Item;
import item.Item.ItemOption;
import map.Zone;
import minigame.cost.LuckyNumberCost;
import minigame.LuckyNumber.LuckyNumberService;
import nro.models.npc.Npc;
import nro.models.npc.NpcManager;
import nro.player.Player;
import network.Message;
import network.inetwork.ISession;
import nro.server.Client;
import nro.services.Service;
import models.GiftCode.GiftCodeService;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdbc.NDVResultSet;
import jdbc.daos.NDVSqlFetcher;
import jdbc.daos.PlayerDAO;
import minigame.TX.TaiXiu;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import nro.player.Inventory;
import nro.server.ChuyenKhoanManager;
import nro.server.Manager;
import nro.services.ClanService;
import nro.services.NapThe;
import nro.services.PlayerService;
import task.Badges.BadgesTaskService;
import utils.Util;

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
    public static final int BOTQUAI = 206783;
    public static final int CHUYEN_KHOAN = 569;
    public static String LOAI_THE;
    public static final int NAP_THE_2 = 570;
    public static String MENH_GIA;

    public static final int BOTITEM = 206762;

    public static final int BOTBOSS = 2067683;
    public static final int BOTUPDE = 2067684;
    public static final int BOTCHAT = 2067685;
    public static final int BOTQUAI_NAPPA = 206786;
    public static final int BOTQUAI_TUONGLAI = 206787;
    public static final int BOTQUAI_COLD = 206788;

    public static final int SELECT_LUCKYNUMBER = 514;

    public static final int DOI_VND = 515;
    public static final int DOI_THOI_VANG = 516;
    public static final int DOI_NGOC_XANH = 517;
    public static final int DOI_NGOC_HONG = 518;
    public static final int BUFFVND = 519;
    public static final int SEND_ITEM = 520;
    public static final byte NUMERIC = 0;
    public static final byte ANY = 1;
    public static final byte PASSWORD = 2;
    public static final byte MBV = 23;
    public static final byte BANSLL = 24;
    public static final byte BANGHOI = 25;
    public static final int TAIXIU_TAI = 5172;
    public static final int TAIXIU_XIU = 5173;
    public static final int TANG_NGOC_HONG = 5174;

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
                case NAP_THE_2:{
                    NapThe.SendCard(player, LOAI_THE, MENH_GIA, text[0], text[1]);
                    break;
                }
                case TANG_NGOC_HONG: {
                    String senderName = text[0].trim();
                    String receiverName = text[1].trim();
                    int numruby = Integer.parseInt(text[2]);

                    Player sender = Client.gI().getPlayer(senderName);
                    Player receiver = Client.gI().getPlayer(receiverName);

                    if (numruby <= 0) {
                        Service.gI().sendThongBaoOK(sender, "Giá trị không hợp lệ");
                        return;
                    }

                    if (sender == null) {
                        Service.gI().sendThongBao(player, "Người tặng không tồn tại hoặc offline");
                        return;
                    }
                    if (receiver == null) {
                        Service.gI().sendThongBao(sender, "Người nhận không tồn tại hoặc offline");
                        return;
                    }

                    if (sender.inventory.ruby < numruby) {
                        Service.gI().sendThongBao(sender, "Không đủ Hồng ngọc để tặng");
                        return;
                    }

                    sender.inventory.subGemAndRuby(numruby);
                    PlayerService.gI().sendInfoHpMpMoney(sender);

                    receiver.inventory.ruby += numruby;
                    PlayerService.gI().sendInfoHpMpMoney(receiver);

                    Service.gI().sendThongBao(sender, "Bạn đã tặng " + numruby + " Hồng ngọc cho " + receiver.name);
                    Service.gI().sendThongBao(receiver, "Bạn vừa nhận được " + numruby + " Hồng ngọc từ " + sender.name);
                    break;
                }

                case TAIXIU_TAI: {
                    try {
                        long tienCuoc = Long.parseLong(text[0]);
                        if (tienCuoc < 1_000_000L || tienCuoc > 200_000_000_000L) {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1Tr - nhiều nhất 200Tỉ");
                            break;
                        }
                        if (player.inventory.gold < tienCuoc) {
                            Service.gI().sendThongBaoOK(player, "Bạn không đủ tiền cược.");
                            break;
                        }
                        long tongNhan = player.inventory.gold + (player.goldTai * 80 / 100) + tienCuoc;
                        if (tongNhan > 200_000_000_000L) {
                            Service.gI().sendThongBaoOK(player, "Số vàng nhận sau khi cược vượt quá giới hạn vàng.");
                            break;
                        }
                        if (Util.canDoWithTime(TaiXiu.gI().lastTimeEnd, TaiXiu.TIME_END)) {
                            Service.gI().sendThongBaoOK(player, "Đã qua lượt mới, vui lòng đặt cược lại.");
                            break;
                        }
                        TaiXiu.gI().addTai(player, tienCuoc);
                        Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.numberToMoney(tienCuoc) + " vàng vào cửa Tài");
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số tiền nhập không hợp lệ.");
                    }
                    break;
                }

                case TAIXIU_XIU: {
                    try {
                        long tienCuoc = Long.parseLong(text[0]);
                        if (tienCuoc < 1_000_000L || tienCuoc > 200_000_000_000L) {
                            Service.gI().sendThongBao(player, "Cược ít nhất 1Tr - nhiều nhất 200Tỉ");
                            break;
                        }
                        if (player.inventory.gold < tienCuoc) {
                            Service.gI().sendThongBaoOK(player, "Bạn không đủ tiền cược.");
                            break;
                        }
                        long tongNhan = player.inventory.gold + (player.goldXiu * 80 / 100) + tienCuoc;
                        if (tongNhan > 200_000_000_000L) {
                            Service.gI().sendThongBaoOK(player, "Số vàng nhận sau khi cược vượt quá giới hạn vàng.");
                            break;
                        }
                        if (Util.canDoWithTime(TaiXiu.gI().lastTimeEnd, TaiXiu.TIME_END)) {
                            Service.gI().sendThongBaoOK(player, "Đã qua lượt mới, vui lòng đặt cược lại.");
                            break;
                        }
                        TaiXiu.gI().addXiu(player, tienCuoc);
                        Service.gI().sendThongBao(player, "Bạn đã đặt " + Util.numberToMoney(tienCuoc) + " vàng vào cửa Xỉu");
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số tiền nhập không hợp lệ.");
                    }
                    break;
                }

                case CHUYEN_KHOAN:{
                    try {
                    long money = Long.parseLong(text[0]);
                    String description = Util.generateRandomString();
                    ChuyenKhoanManager.InsertTransaction(player.id, money, description);
                    if (money < 1000 || money > 1_000_000) {
                        Service.gI().sendThongBao(player, "Tối thiểu 1000 và tối đa 1000000");
                        break;
                    }
                    Npc npc = NpcManager.getByIdAndMap(ConstNpc.ONG_GOHAN, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, ConstNpc.CONTENT_CHUYEN_KHOAN,
                                    "Con đã tạo thành công giao dịch Với Mệnh Giá Là: " + Util.mumberToLouis(money) + " VNĐ\n"
                                    + "Vui Lòng Chuyển Khoản Theo Cú Pháp Như Sau:\n"
                                    + "Ngân Hàng: MBBANK\n"
                                    + "Chủ TK: HO QUOC KHANG\n"
                                    + "STK: 0000331855198\n"
                                    + "Nội Dung: " + description + "\n"
                                    + "|7|HOẶC CÓ THỂ QUÉT QR BÊN DƯỚI\n"
                                    + "|1|LƯU Ý: ĐỢI 1-3 PHÚT TIỀN SẼ TỰ ĐỘNG CỘNG VÀO TÀI KHOẢN CỦA BẠN",
                                      "Quét Mã\nQR","Từ chối");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    Service.gI().sendThongBao(player, "Đã có lỗi xảy ra liên hệ với ADMIN để được hỗ trợ");
                }
                break;  
                }
                case BOTITEM: {
                    int slot = Integer.parseInt(text[0]);
                    int idBan = Integer.parseInt(text[1]);
                    int idTraoDoi = Integer.parseInt(text[2]);
                    int slot_TraoDoi = Integer.parseInt(text[3]);
                    ShopBot bs = new ShopBot(idBan, idTraoDoi, slot_TraoDoi);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(1, bs, slot);
                    });
                    break;
                }

                case BOTQUAI_NAPPA: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(2, null, slot);
                    });
                    break;
                }

                case BOTQUAI_TUONGLAI: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(3, null, slot);
                    });
                    break;
                }

                case BOTQUAI_COLD: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(4, null, slot);
                    });
                    break;
                }

                case BOTBOSS: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(5, null, slot);
                    });
                    break;
                }

                case BOTUPDE: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(99, null, slot);
                    });
                    break;
                }

                case BOTCHAT: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(6, null, slot);
                    });
                    break;
                }

                case BOTQUAI: {
                    int slot = Integer.parseInt(text[0]);
                    Thread.startVirtualThread(() -> {
                        NewBot.gI().runBot(0, null, slot);
                    });
                    break;
                }

                case SEND_ITEM: {
                    String itemIds = text[1];
                    String option = text[2];
                    int slItemBuff = Integer.parseInt(text[3]);
                    if (slItemBuff > 999) {
                        Service.gI().sendThongBaoOK(player, "Buff vượt số lượng giới hạn vui lòng để tối đa sl 999");
                        return;
                    }
                    String plName = text[0].trim();
                    if (plName.equals("all")) {
                        Thread.startVirtualThread(() -> {
                            List<Player> allPlayer = NDVSqlFetcher.getAllPlayer();
                            for (Player pBuffItem : allPlayer) {
                                if (pBuffItem != null) {
                                    sendItemsToPlayer(pBuffItem, itemIds, option, slItemBuff, player);
                                }
                            }
                        });
                    } else {
                        Player pBuffItem = NDVSqlFetcher.loadPlayerByName(text[0].trim());
                        if (pBuffItem != null) {
                            sendItemsToPlayer(pBuffItem, itemIds, option, slItemBuff, player);
                        } else {
                            Service.gI().sendThongBao(player, "Player không tồn tại");
                        }
                    }
                    break;
                }

                case BUFFVND: {
                    try {
                        String playerName = text[0].trim();
                        int addcash = Integer.parseInt(text[1].trim());

                        if (addcash <= 0) {
                            Service.gI().sendThongBao(player, "Số tiền phải lớn hơn 0");
                            break;
                        }

                        NDVResultSet rs = DBConnecter.executeQuery("SELECT account_id, id FROM player WHERE name = ?", playerName);
                        try {
                            if (rs.next()) {
                                int accountId = rs.getInt("account_id");

                                if (PlayerDAO.addcash(accountId, addcash)) {
                                    Service.gI().sendThongBao(player, "Bạn đã buff cho " + playerName + " " + addcash + " VNĐ");

                                    Player targetPlayer = Client.gI().getPlayer(playerName);
                                    if (targetPlayer != null && targetPlayer.getSession() != null) {
                                        targetPlayer.getSession().cash += addcash;
                                        Service.gI().sendThongBao(targetPlayer, "Bạn vừa được cộng " + addcash + " VNĐ bởi " + player.name);
                                    }
                                } else {
                                    Service.gI().sendThongBao(player, "Lỗi khi lưu vào database!");
                                }
                            } else {
                                Service.gI().sendThongBao(player, "Không tìm thấy người chơi '" + playerName + "'");
                            }
                        } finally {
                            if (rs != null) {
                                rs.dispose();
                            }
                        }

                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số tiền nhập vào không hợp lệ!");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Service.gI().sendThongBao(player, "Đã có lỗi xảy ra");
                    }
                    break;
                }
                
                case DOI_THOI_VANG: {
                    handleDoiVatPham(player, text[0], 457, "Thỏi vàng", 200, true);
                    break;
                }

                case DOI_NGOC_XANH: {
                    handleDoiVatPham(player, text[0], 77, "Ngọc xanh", 1, false);
                    break;
                }

                case DOI_NGOC_HONG: {
                    handleDoiVatPham(player, text[0], 861, "Ngọc hồng", 1, false);
                    break;
                }

                case GIVE_IT:
                    handleGiveItem(player, text[0], text[1], text[2], text[3], text[4]);
                    break;

                case GET_IT:
                    handleGetItem(player, text[0], text[1], text[2], text[3]);
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
                                new String[]{"Đi tới\n" + pl.name, "Gọi " + pl.name + "\ntới đây", "Đổi tên", "Ban", "Kick"},
                                pl);
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                    break;

                case CHANGE_NAME: {
                    Player plChanged = (Player) PLAYER_ID_OBJECT.get((int) player.id);
                    if (plChanged != null) {
                        handleChangeName(player, plChanged, text[0]);
                    }
                    break;
                }

                case CHANGE_NAME_BY_ITEM: {
                    handleChangeNameByItem(player, text[0]);
                    break;
                }

                case CHOOSE_LEVEL_BDKB:
                    handleChooseLevel(player, text[0], ConstNpc.QUY_LAO_KAME, ConstNpc.MENU_ACCEPT_GO_TO_BDKB, "hang kho báu");
                    break;

                case CHOOSE_LEVEL_KGHD:
                    handleChooseLevel(player, text[0], ConstNpc.MR_POPO, 2, "Destron Gas");
                    break;

                case CHOOSE_LEVEL_CDRD:
                    handleChooseLevel(player, text[0], ConstNpc.THAN_VU_TRU, 3, "con đường rắn độc");
                    break;

                case MBV:
                    handleChangeMBV(player, text[0], text[1], text[2]);
                    break;

                case BANSLL:
                    handleBanThoiVang(player, text[0]);
                    break;

                case BANGHOI:
                    handleBangHoi(player, text[0]);
                    break;

                case DISSOLUTION_CLAN:
                    handleDissolutionClan(player, text[0]);
                    break;

                case SELECT_LUCKYNUMBER: {
                    int number = Integer.parseInt(text[0]);
                    LuckyNumberService.addNumber(player, number);
                    break;
                }
            }
        } catch (Exception e) {
            // Log lỗi nhưng không hiển thị cho người dùng
            System.err.println("Lỗi doInput: " + e.getMessage());
        }
    }

    // ==================== CÁC PHƯƠNG THỨC HỖ TRỢ ====================
    private void sendItemsToPlayer(Player target, String itemIds, String option, int quantity, Player sender) {
        String[] itemIdsArray = itemIds.split(",");
        for (String itemId : itemIdsArray) {
            int idItemBuff = Integer.parseInt(itemId);
            Item itembuff = ItemService.gI().createNewItem((short) idItemBuff, quantity);

            if (option != null && !option.isEmpty()) {
                String[] Option = option.split(",");
                if (Option.length > 0) {
                    for (int i = 0; i < Option.length; i++) {
                        String[] optItem = Option[i].split("-");
                        if (optItem.length == 2) {
                            int optID = Integer.parseInt(optItem[0]);
                            int param = Integer.parseInt(optItem[1]);
                            itembuff.itemOptions.add(new ItemOption(optID, param));
                        }
                    }
                }
            }
            target.inventory.itemsMailBox.add(itembuff);

            if (NDVSqlFetcher.updateMailBox(target)) {
                Service.gI().sendThongBao(sender, "Bạn vừa gửi " + itembuff.template.name + " thành công cho " + target.name);
            }
        }
    }

    private void handleDoiVatPham(Player player, String coinText, int itemId, String itemName, int rate, boolean updateBadges) {
        try {
            int coin = Integer.parseInt(coinText);
            if (player.getSession() != null && player.getSession().cash < coin) {
                Service.gI().sendThongBao(player, "Bạn không đủ " + coin + " VND");
                return;
            }
            if (coin < 0) {
                Service.gI().sendThongBao(player, "Bạn không được phép nhập số âm");
                return;
            }
            if (coin >= 20000 && coin <= 100000000) {
                PlayerDAO.subcash(player, coin);
                int sl = (itemId == 457) ? coin / rate : coin;
                Item item = ItemService.gI().createNewItem((short) itemId, sl);
                InventoryService.gI().addItemBag(player, item);
                InventoryService.gI().sendItemBag(player);

                if (updateBadges) {
                    BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, coin);
                    BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.EM_XINH_EM_DEP, coin);
                }

                Service.gI().sendThongBao(player, "Bạn nhận được " + sl + " " + itemName);
            } else {
                Service.gI().sendThongBao(player, "Chọn 1 con số từ 20000 đến 100000000");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Số tiền không hợp lệ");
        }
    }

    private void handleGiveItem(Player player, String name, String id, String op, String pr, String q) {
        Player target = Client.gI().getPlayer(name);
        if (target != null) {
            try {
                int itemId = Integer.parseInt(id);
                int optionId = Integer.parseInt(op);
                int param = Integer.parseInt(pr);
                int quantity = Integer.parseInt(q);

                Item item = ItemService.gI().createNewItem((short) itemId);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                if (!ops.isEmpty()) {
                    item.itemOptions = ops;
                }
                item.quantity = quantity;
                item.itemOptions.add(new Item.ItemOption(optionId, param));
                InventoryService.gI().addItemBag(target, item);
                InventoryService.gI().sendItemBag(target);
                Service.gI().sendThongBao(target, "Nhận " + item.template.name + " từ " + player.name);
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Dữ liệu không hợp lệ");
            }
        } else {
            Service.gI().sendThongBao(player, "Người chơi không online");
        }
    }

    private void handleGetItem(Player player, String id, String op, String pr, String q) {
        if (player.isAdmin()) {
            try {
                int itemId = Integer.parseInt(id);
                int optionId = Integer.parseInt(op);
                int param = Integer.parseInt(pr);
                int quantity = Integer.parseInt(q);

                Item item = ItemService.gI().createNewItem((short) itemId);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                if (!ops.isEmpty()) {
                    item.itemOptions = ops;
                }
                item.quantity = quantity;
                item.itemOptions.add(new Item.ItemOption(optionId, param));
                InventoryService.gI().addItemBag(player, item);
                InventoryService.gI().sendItemBag(player);
                Service.gI().sendThongBao(player, "Nhận " + item.template.name + " !");
            } catch (NumberFormatException e) {
                Service.gI().sendThongBao(player, "Dữ liệu không hợp lệ");
            }
        } else {
            Service.gI().sendThongBao(player, "Không đủ quyền hạn!");
        }
    }

    private void handleChangeName(Player player, Player target, String newName) {
        try {
            if (DBConnecter.executeQuery("select * from player where name = ?", newName).next()) {
                Service.gI().sendThongBao(player, "Tên nhân vật đã tồn tại");
            } else {
                target.name = newName;
                DBConnecter.executeUpdate("update player set name = ? where id = ?", target.name, target.id);
                updatePlayerData(target);
                Service.gI().sendThongBao(target, "Chúc mừng bạn đã có cái tên mới đẹp đẽ hơn tên ban đầu");
                Service.gI().sendThongBao(player, "Đổi tên người chơi thành công");
            }
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi đổi tên");
        }
    }

    private void handleChangeNameByItem(Player player, String newName) {
        try {
            if (DBConnecter.executeQuery("SELECT * FROM player WHERE name = ?", newName).next()) {
                Service.gI().sendThongBao(player, "Tên nhân vật đã tồn tại");
                createFormChangeNameByItem(player);
                return;
            }

            if (newName.length() < 6 || newName.length() > 20) {
                Service.gI().sendThongBaoOK(player, "Tên nhân vật phải từ 6 đến 20 ký tự");
                return;
            }

            Item theDoiTen = InventoryService.gI().findItem(player.inventory.itemsBag, 1955);
            if (theDoiTen == null) {
                Service.gI().sendThongBao(player, "Không tìm thấy thẻ đổi tên");
                return;
            }

            InventoryService.gI().subQuantityItemsBag(player, theDoiTen, 1);
            player.name = newName;
            DBConnecter.executeUpdate("UPDATE player SET name = ? WHERE id = ?", player.name, player.id);
            updatePlayerData(player);
            Service.gI().sendThongBao(player, "Chúc mừng! Bạn đã đổi tên thành công.");
        } catch (Exception e) {
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi đổi tên!");
        }
    }

    private void handleChooseLevel(Player player, String levelText, int npcId, int menuId, String locationName) {
        try {
            int level = Integer.parseInt(levelText);
            if (level >= 1 && level <= 110) {
                Npc npc = NpcManager.getByIdAndMap(npcId, player.zone.map.mapId);
                if (npc != null) {
                    npc.createOtherMenu(player, menuId,
                            "Con có chắc muốn đến\n" + locationName + " cấp độ " + level + " ?",
                            new String[]{"Đồng ý", "Từ chối"}, level);
                }
            } else {
                Service.gI().sendThongBao(player, "Cấp độ phải từ 1 đến 110");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Cấp độ không hợp lệ");
        }
    }

    private void handleChangeMBV(Player player, String mbv, String nmbv, String rembv) {
        try {
            if ((mbv + "").length() != 6 || (nmbv + "").length() != 6 || (rembv + "").length() != 6) {
                Service.gI().sendThongBao(player, "Mã bảo vệ phải có 6 chữ số");
            } else if (player.mbv == 0) {
                Service.gI().sendThongBao(player, "Bạn chưa cài mã bảo vệ!");
            } else if (player.mbv != Integer.parseInt(mbv)) {
                Service.gI().sendThongBao(player, "Mã bảo vệ không đúng");
            } else if (!nmbv.equals(rembv)) {
                Service.gI().sendThongBao(player, "Mã bảo vệ không trùng khớp");
            } else {
                player.mbv = Integer.parseInt(nmbv);
                Service.gI().sendThongBao(player, "Đổi mã bảo vệ thành công!");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Mã bảo vệ không hợp lệ");
        }
    }

    private void handleBanThoiVang(Player player, String slText) {
        try {
            int sltv = Integer.parseInt(slText);
            long cost = (long) sltv * 500000000;

            if (sltv < 0) {
                Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
                return;
            }

            Item ThoiVang = InventoryService.gI().findItemBag(player, 457);
            if (ThoiVang != null) {
                if (ThoiVang.quantity < sltv) {
                    Service.gI().sendThongBao(player, "Bạn chỉ có " + ThoiVang.quantity + " Thỏi vàng");
                } else {
                    if (player.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                        int slban = (int) ((Inventory.LIMIT_GOLD - player.inventory.gold) / 500000000);
                        if (slban < 1) {
                            Service.gI().sendThongBao(player, "Vàng sau khi bán vượt quá giới hạn");
                        } else if (slban < 2) {
                            Service.gI().sendThongBao(player, "Bạn chỉ có thể bán 1 Thỏi vàng");
                        } else {
                            Service.gI().sendThongBao(player, "Số lượng trong khoảng 1 tới " + slban);
                        }
                    } else {
                        InventoryService.gI().subQuantityItemsBag(player, ThoiVang, sltv);
                        InventoryService.gI().sendItemBag(player);
                        player.inventory.gold += cost;
                        Service.gI().sendMoney(player);
                        Service.gI().sendThongBao(player, "Đã bán " + sltv + " Thỏi vàng thu được " + Util.numberToMoney(cost) + " vàng");
                        TransactionService.gI().cancelTrade(player);
                    }
                }
            } else {
                Service.gI().sendThongBao(player, "Bạn không có Thỏi vàng");
            }
        } catch (NumberFormatException e) {
            Service.gI().sendThongBao(player, "Số lượng không hợp lệ");
        }
    }

    private void handleBangHoi(Player player, String tenvt) {
        Clan clan = player.clan;
        if (clan != null) {
            ClanMember cm = clan.getClanMember((int) player.id);
            if (clan.isLeader(player)) {
                if (clan.canUpdateClan(player)) {
                    if (!Util.haveSpecialCharacter(tenvt) && tenvt.length() > 1 && tenvt.length() < 5) {
                        clan.name2 = tenvt;
                        clan.update();
                        Service.gI().sendThongBao(player, "[" + tenvt + "] OK");
                    } else {
                        Service.gI().sendThongBaoOK(player, "Chỉ chấp nhận các ký tự a-z, 0-9 và chiều dài từ 2 đến 4 ký tự");
                    }
                }
            }
        }
    }

    private void handleDissolutionClan(Player player, String xacNhan) {
        if (xacNhan.equalsIgnoreCase("OK")) {
            Clan clan = player.clan;
            if (clan != null && clan.isLeader(player)) {
                clan.deleteDB(clan.id);
                Manager.CLANS.remove(clan);
                player.clan = null;
                player.clanMember = null;
                ClanService.gI().sendMyClan(player);
                ClanService.gI().sendClanId(player);
                Service.gI().sendThongBao(player, "Bang hội đã giải tán thành công.");
            }
        }
    }

    private void updatePlayerData(Player player) {
        Service.gI().player(player);
        Service.gI().Send_Caitrang(player);
        Service.gI().sendFlagBag(player);
        Zone zone = player.zone;
        ChangeMapService.gI().changeMap(player, zone, player.location.x, player.location.y);
    }

    // ==================== CÁC PHƯƠNG THỨC TẠO FORM ====================
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

    // Các phương thức createForm khác giữ nguyên...
    public void createFormBotQuai(Player pl) {
        createForm(pl, BOTQUAI, "Buff Bot Quái Thường", new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotQuaiNappa(Player pl) {
        createForm(pl, BOTQUAI_NAPPA, "Buff Bot Quái Nappa", new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotQuaiTuonglai(Player pl) {
        createForm(pl, BOTQUAI_TUONGLAI, "Buff Bot Quái Tương Lai", new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotQuaiCold(Player pl) {
        createForm(pl, BOTQUAI_COLD, "Buff Bot Quái Cold", new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotBoss(Player pl) {
        createForm(pl, BOTBOSS, "Buff Bot Boss", new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotUpDe(Player pl) {
        int totalBots = 0;
        int updeBots = 0;
        try {
            for (Bot.Bot b : Bot.BotManager.gI().bot) {
                totalBots++;
                if (b instanceof Bot.BotUpDe) {
                    updeBots++;
                }
            }
        } catch (Exception e) {
        }
        String title = "Buff Bot Up Đệ\n"
                + "Tổng bot đang tồn tại: " + totalBots + "\n"
                + "Bot up đệ đang hoạt động: " + updeBots + "\n"
                + "Nhập số lượng cần tạo";
        createForm(pl, BOTUPDE, title, new SubInput("Số lượng bot", NUMERIC));
    }

    public void createFormBotChat(Player pl) {
        int totalBots = 0;
        try {
            totalBots = Bot.BotManager.gI().bot.size();
        } catch (Exception e) {
        }
        String title = "Buff Bot Chat\n"
                + "Tổng bot đang tồn tại: " + totalBots + "\n"
                + "(Bot chat sẽ chat ngẫu nhiên theo hệ thống)\n"
                + "Nhập số lượng cần tạo";
        createForm(pl, BOTCHAT, title, new SubInput("Số lượng bot", NUMERIC));
    }

    public void createFormBotItem(Player pl) {
        createForm(pl, BOTITEM, "Buff Bot Item",
                new SubInput("số lượng bot", NUMERIC),
                new SubInput("id item cần bán", NUMERIC),
                new SubInput("id item trao đổi", NUMERIC),
                new SubInput("số lượng yêu cầu trao đổi", NUMERIC));
    }

    public void createFormChangePassword(Player pl) {
        createForm(pl, CHANGE_PASSWORD, "Đổi mật khẩu",
                new SubInput("Mật khẩu cũ", PASSWORD),
                new SubInput("Mật khẩu mới", PASSWORD),
                new SubInput("Nhập lại mật khẩu mới", PASSWORD));
    }

    public void createFormGiveItem(Player pl) {
        createForm(pl, GIVE_IT, "Tặng vật phẩm",
                new SubInput("Tên", ANY),
                new SubInput("Id Item", ANY),
                new SubInput("ID OPTION", ANY),
                new SubInput("PARAM", ANY),
                new SubInput("Số lượng", ANY));
    }

    public void createFormGetItem(Player pl) {
        createForm(pl, GET_IT, "Get vật phẩm",
                new SubInput("Id Item", ANY),
                new SubInput("ID OPTION", ANY),
                new SubInput("PARAM", ANY),
                new SubInput("Số lượng", ANY));
    }

    public void createFormGiftCode(Player pl) {
        createForm(pl, GIFT_CODE, "Nhập Gift Code gồm 16 chữ số", new SubInput("Nhập Gift Code gồm 16 chữ số", ANY));
    }

    public void createFormMBV(Player pl) {
        createForm(pl, MBV, "Đổi mã bảo vệ",
                new SubInput("Nhập Mã Bảo Vệ Cũ", NUMERIC),
                new SubInput("Nhập Mã Bảo Vệ Mới", NUMERIC),
                new SubInput("Nhập Lại Mã Bảo Vệ Mới", NUMERIC));
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
    
    public void createFormNapThe2(Player pl, String loaiThe, String menhGia) {
        LOAI_THE = loaiThe;
        MENH_GIA = menhGia;
        createForm(pl, NAP_THE_2, "Nạp thẻ", new SubInput("Số Seri", ANY), new SubInput("Mã thẻ", ANY));
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
        createForm(pl, BANSLL, "Bán Thỏi vàng - Nhập số lượng (1 → tối đa, trong giới hạn 200,000,000,000 vàng)", new SubInput("Số lượng", NUMERIC));
    }

    public void createFormGiaiTanBangHoi(Player pl) {
        createForm(pl, DISSOLUTION_CLAN, "Nhập OK để xác nhận giải tán bang hội.", new SubInput("", ANY));
    }

    public void createFormDoiVND(Player pl) {
        createForm(pl, DOI_VND, "Đổi VND --> VND < VND x 0.9 >", new SubInput("Nhập số lượng VND muốn đổi ra VND", NUMERIC));
    }

    public void createFormDoiThoiVang(Player pl) {
        createForm(pl, DOI_THOI_VANG, "Đổi VND --> Thỏi vàng < Mỗi 20K được 100 thỏi >", new SubInput("Nhập số lượng VND muốn đổi ra thỏi vàng", NUMERIC));
    }

    public void createFormDoiNgocXanh(Player pl) {
        createForm(pl, DOI_NGOC_XANH, "Đổi VND --> Ngọc xanh < Mỗi 20K được 20.000 ngọc xanh >", new SubInput("Nhập số lượng VND muốn đổi ra ngọc xanh", NUMERIC));
    }

    public void createFormDoiNgocHong(Player pl) {
        createForm(pl, DOI_NGOC_HONG, "Đổi VND --> Ngọc hồng < Mỗi 20K được 20.000 ngọc hồng >", new SubInput("Nhập số lượng VND muốn đổi ra ngọc hồng", NUMERIC));
    }

    public void createFormSelectOneNumberLuckyNumber(Player pl, boolean isGem) {
        String text = "";
        if (isGem) {
            text = "Hãy chọn 1 số từ 0 đến 99 giá " + Util.numberFormatLouis(LuckyNumberCost.costPlayGem) + " ngọc";
        } else {
            text = "Hãy chọn 1 số từ 0 đến 99 giá " + Util.numberFormatLouis(LuckyNumberCost.costPlayGold) + " vàng";
        }
        createForm(pl, SELECT_LUCKYNUMBER, text, new SubInput("Số bạn chọn", NUMERIC));
    }

    public void createFromMailBox(Player pl) {
        createForm(
                pl,
                SEND_ITEM,
                "Gửi Item Cho Người Chơi",
                new SubInput("Tên người chơi (hoặc 'all')", ANY),
                new SubInput("ID Item (vd: 457)", ANY),
                new SubInput("Option (vd: 50,10;77,20)", ANY),
                new SubInput("Số lượng (1 - 999)", NUMERIC)
        );
    }

    public void createFormBuffVND(Player player) {
        createForm(player, BUFFVND, "Buff VNĐ",
                new SubInput("Tên người chơi", ANY),
                new SubInput("VNĐ CẦN BUFF", NUMERIC));
    }

    public void createFormChuyenKhoan(Player pl) {
        createForm(pl, CHUYEN_KHOAN, "Nhập số tiền muốn nạp\n",
                new SubInput("Số tiền - Ví dụ 10000 = 10k VND muốn nạp, Mệnh giá cao BONUS càng cao", NUMERIC));
    }

    public void taixiu_Tai(Player pl) {
        createForm(pl, TAIXIU_TAI, "Đặt cược Tài Xỉu (Cửa Tài)", new SubInput("Số vàng cược (từ 1Tr -> 200Tỉ)", NUMERIC));
    }

    public void taixiu_Xiu(Player pl) {
        createForm(pl, TAIXIU_XIU, "Đặt cược Tài Xỉu (Cửa Xỉu)", new SubInput("Số vàng cược (từ 1Tr -> 200Tỉ)", NUMERIC));
    }

    public void createFormTangRuby(Player pl) {
        createForm(pl, TANG_NGOC_HONG, "Tặng ngọc",
                new SubInput("Tên nhân vật", ANY),
                new SubInput("Số Hồng Ngọc Muốn Tặng", NUMERIC));
    }

    public static class SubInput {

        private String name;
        private byte typeInput;

        public SubInput(String name, byte typeInput) {
            this.name = name;
            this.typeInput = typeInput;
        }
    }
}
