package services.func;

import boss.Boss;
import boss.BossID;
import boss.boss_manifest.Mini.SoiHecQuyn;
import boss.boss_manifest.Mini.Xinbato;
import nro.services.RewardService;
import nro.services.Service;
import nro.services.TaskService;
import nro.services.InventoryService;
import nro.services.MapService;
import nro.services.ItemService;
import nro.services.SkillService;
import nro.services.ItemTimeService;
import nro.services.PlayerService;
import nro.services.PetService;
import nro.services.NgocRongNamecService;
import nro.services.NpcService;
import consts.ConstItem;
import models.Combine.CombineService;
import models.ShenronEvent.ShenronEventService;
import models.Card.Card;
import models.Card.RadarService;
import models.Card.RadarCard;
import consts.ConstMap;
import item.Item;
import consts.ConstNpc;
import consts.ConstPlayer;
import consts.ConstTaskBadges;
import data.RandomCollection;
import item.Item.ItemOption;
import item.ItemTime;
import map.Zone;
import nro.player.Inventory;
import nro.player.Player;
import skill.Skill;
import network.Message;
import utils.SkillUtil;
import utils.TimeUtil;
import utils.Util;
import nro.server.io.MySession;
import utils.Logger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import map.ItemMap;
import models.ShenronEvent_NOEL.ShenronEventServicenoel;
import nro.services.ItemMapService;
import task.Badges.BadgesTaskService;

public class UseItem {

    private static final int ITEM_BOX_TO_BODY_OR_BAG = 0;
    private static final int ITEM_BAG_TO_BOX = 1;
    private static final int ITEM_BODY_TO_BOX = 3;
    private static final int ITEM_BAG_TO_BODY = 4;
    private static final int ITEM_BODY_TO_BAG = 5;
    private static final int ITEM_BAG_TO_PET_BODY = 6;
    private static final int ITEM_BODY_PET_TO_BAG = 7;

    private static final byte DO_USE_ITEM = 0;
    private static final byte DO_THROW_ITEM = 1;
    private static final byte ACCEPT_THROW_ITEM = 2;
    private static final byte ACCEPT_USE_ITEM = 3;

    private static UseItem instance;
    private static final Random rand = new Random();

    private int randClothes(int level) {
        return ConstItem.LIST_ITEM_CLOTHES[Util.nextInt(0, 2)][Util.nextInt(0, 4)][level - 1];
    }

    private UseItem() {
    }

    public static UseItem gI() {
        if (instance == null) {
            instance = new UseItem();
        }
        return instance;
    }

    public void getItem(MySession session, Message msg) {
        Player player = session.player;
        if (player == null) {
            return;
        }
        TransactionService.gI().cancelTrade(player);
        try {
            int type = msg.reader().readByte();
            int index = msg.reader().readByte();
            if (index == -1) {
                return;
            }
            switch (type) {
                case ITEM_BOX_TO_BODY_OR_BAG:
                    InventoryService.gI().itemBoxToBodyOrBag(player, index);
                    TaskService.gI().checkDoneTaskGetItemBox(player);
                    break;
                case ITEM_BAG_TO_BOX:
                    InventoryService.gI().itemBagToBox(player, index);
                    break;
                case ITEM_BODY_TO_BOX:
                    InventoryService.gI().itemBodyToBox(player, index);
                    break;
                case ITEM_BAG_TO_BODY:
                    InventoryService.gI().itemBagToBody(player, index);
                    break;
                case ITEM_BODY_TO_BAG:
                    InventoryService.gI().itemBodyToBag(player, index);
                    break;
                case ITEM_BAG_TO_PET_BODY:
                    InventoryService.gI().itemBagToPetBody(player, index);
                    break;
                case ITEM_BODY_PET_TO_BAG:
                    InventoryService.gI().itemPetBodyToBag(player, index);
                    break;
            }
            if (player.setClothes != null) {
                player.setClothes.setup();
            }
            if (player.pet != null) {
                player.pet.setClothes.setup();
            }
            player.setClanMember();
            Service.gI().sendFlagBag(player);
            Service.gI().point(player);
            Service.gI().sendSpeedPlayer(player, -1);
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
    }

    public Item finditem(Player player, int iditem) {
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.id == iditem) {
                return item;
            }
        }
        return null;
    }

    public void doItem(Player player, Message _msg) {
        TransactionService.gI().cancelTrade(player);
        Message msg = null;
        byte type;
        try {
            type = _msg.reader().readByte();
            int where = _msg.reader().readByte();
            int index = _msg.reader().readByte();
            switch (type) {
                case DO_USE_ITEM:
                    if (player != null && player.inventory != null) {
                        if (index != -1) {
                            if (index < 0) {
                                return;
                            }
                            Item item = player.inventory.itemsBag.get(index);
                            if (item.isNotNullItem()) {
                                if (item.template.type == 7) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc chắn học " + player.inventory.itemsBag.get(index).template.name + "?");
                                    player.sendMessage(msg);
                                } else if (item.template.id == 570) {
                                    if (!Util.isAfterMidnight(player.lastTimeRewardWoodChest)) {
                                        Service.gI().sendThongBao(player, "Hãy chờ đến ngày mai");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn mở\n" + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else if (item.template.type == 22) {
                                    if (player.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22).count() > 2) {
                                        Service.gI().sendThongBaoOK(player, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn dùng\n" + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else {
                                    UseItem.gI().useItem(player, item, index);
                                }
                            }
                        } else {
                            int iditem = _msg.reader().readShort();
                            Item item = finditem(player, iditem);
                            UseItem.gI().useItem(player, item, index);
                        }
                    }
                    break;
                case DO_THROW_ITEM:
                    if (!(player.zone.map.mapId == 21 || player.zone.map.mapId == 22 || player.zone.map.mapId == 23)) {
                        Item item = null;
                        if (index < 0) {
                            return;
                        }
                        if (where == 0) {
                            item = player.inventory.itemsBody.get(index);
                        } else {
                            item = player.inventory.itemsBag.get(index);
                        }
                        if (item.isNotNullItem() && item.template.id == 570) {
                            Service.gI().sendThongBao(player, "Không thể bỏ vật phẩm này.");
                            return;
                        }
                        if (!item.isNotNullItem()) {
                            return;
                        }
                        msg = new Message(-43);
                        msg.writer().writeByte(type);
                        msg.writer().writeByte(where);
                        msg.writer().writeByte(index);
                        msg.writer().writeUTF("Bạn chắc chắn muốn vứt " + item.template.name + "?");
                        player.sendMessage(msg);
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                    break;
                case ACCEPT_THROW_ITEM:
                    InventoryService.gI().throwItem(player, where, index);
                    Service.gI().point(player);
                    InventoryService.gI().sendItemBag(player);
                    break;
                case ACCEPT_USE_ITEM:
                    UseItem.gI().useItem(player, player.inventory.itemsBag.get(index), index);
                    break;
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void useItem(Player pl, Item item, int indexBag) throws Exception {
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 570) {
                int time = (int) TimeUtil.diffDate(new Date(), new Date(item.createTime), TimeUtil.DAY);
                if (time == 0) {
                    Service.gI().sendThongBao(pl, "Hãy chờ đến ngày mai");
                } else {
                    openRuongGo(pl);
                }
                return;
            }
            if (item.template.strRequire <= pl.nPoint.power) {
                switch (item.template.type) {
                    case 21:
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        PetService.Pet2(pl, pl.getHeadThuCung(), pl.getBodyThuCung(), pl.getLegThuCung());
                        Service.gI().point(pl);
                        break;
                    case 33:
                        UseCard(pl, item);
                        break;
                    case 7:
                        learnSkill(pl, item);
                        break;
                    case 6:
                        this.eatPea(pl);
                        break;
                    case 12:
                        controllerCallRongThan(pl, item);
                        break;
                    case 23: // thú cưỡi mới
                    case 24: // thú cưỡi cũ
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    case 11: // item bag
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendFlagBag(pl);
                        break;
                    case 98:
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendEffPlayer(pl);
                        break;
                    default:
                        switch (item.template.id) {
                            case 992:
                                pl.type = 2;
                                pl.maxTime = 5;
                                Service.gI().Transport(pl);
                                break;
                            case 361:
                                pl.idGo = (short) Util.nextInt(0, 6);
                                NgocRongNamecService.gI().menuCheckTeleNamekBall(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);
                                break;
                            case 211:
                            case 212:
                                eatGrapes(pl, item);
                                break;
                            case 342:
                            case 343:
                            case 344:
                            case 345:
                                if (pl.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22).count() < 3) {
                                Service.gI().dropSatellite(pl, item, pl.zone, pl.location.x, pl.location.y);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                } else {
                                    Service.gI().sendThongBaoOK(pl, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                }
                                break;
                            case 380:
                                openCSKB(pl, item);
                                break;
                            case 381:
                            case 382:
                            case 383:
                            case 384:
                            case 385:
                            case 379:
                            case 1109:
                            case 638:
                            case 2160:
                            case 579:
                            case 1045:
                            case 663:
                            case 664:
                            case 665:
                            case 666:
                            case 667:
                            case 1150:
                            case 1151:
                            case 1152:
                            case 1153:
                            case 1154:
                            case 1978:
                            case 1979:
                            case 1980:
                            case 465:
                            case 466:
                            case 472:
                            case 473:
                            case 1628:
                            case 764:
                            case 1731:
                            case 1727:
                            case 1728:
                            case 1729:
                            case 1730:
                            case 1635:
                            case 1533:
                                useItemTime(pl, item);
                                break;
                            case 1809:
                                long now = System.currentTimeMillis();
                                long addTime = ItemTime.TIME_VE_VANG;
                                if (!pl.itemTime.isUsevevang) {
                                    pl.itemTime.isUsevevang = true;
                                    pl.itemTime.lastTimevevang = now;
                                    pl.itemTime.totalTimeVeVang = addTime;
                                } else {
                                    pl.itemTime.totalTimeVeVang += addTime;
                                }
                                if (pl.zone == null || pl.zone.map.mapId != 186) {
                                    ChangeMapService.gI().changeMap(pl, 186, -1, 100, 84);
                                }
                                useItemTime(pl, item);
                                break;
                            case 1540:
                                ChangeMapService.gI().changeMap(pl, 194, -1, 100, 84);
                                break;
                            case 1560:
                                if (InventoryService.gI().findItem(pl.inventory.itemsBag, 1561) != null) {
                                    UseItem.gI().openRuongNgocRong(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn không có chía khoá vàng!");
                                }
                                break;
                            case 2000:
                                if (InventoryService.gI().findItem(pl.inventory.itemsBag, 2001) != null) {
                                    UseItem.gI().openhopdiem(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn không có Que diêm!");
                                }
                                break;
                            case 460:
                                CucXuong(pl, item);
                                break;
                            case 456:
                                BinhNuoc(pl, item);
                                break;
                            case 1787:
                                Item trungRong = InventoryService.gI().findItemBag(pl, 1787);
                                if (trungRong != null && trungRong.quantity >= 99) {
                                    open1787(pl, item);
                                } else {
                                    Service.gI().sendThongBao(pl, "Bạn cần x99 Mảnh Trứng");
                                }
                                break;
                            case 1786:
                                open1786(pl, item);
                                break;
                            case 1788:
                                open1788(pl, item);
                                break;
                            case 1798:
                                open1798(pl, item);
                                break;
                            case 1805:
                                ItemService.gI().OpenItem1805(pl, item);
                                break;
                            case 1305:
                                if (pl.zone.map.mapId != 18 && pl.zone.map.mapId != 19 && pl.zone.map.mapId != 20) {
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    Service.gI().sendThongBao(pl, "Vui lòng tới hành tình xayda như : Vách núi đen,Thung lũng đen,..");
                                    return;
                                }
                                NpcService.gI().createMenuConMeo(pl, 3213, -1, "Ngươi có chắc muốn dùng Ánh trăng tròn để dụ Khỉ Đột không lồ không ?", "Đồng ý", "Từ chối");
                                break;
                            case 962:
                            case 963:
                                ItemService.gI().OpenCapsuleCaiTrang(pl, item);
                                break;
                            case 627:
                                open627(pl, item);
                                break;
                            case 1807:
                            case 1808:
                                UseItem.gI().ItemSKH(pl, item);
                                break;
                            case 880:
                            case 881:
                            case 882:
                                if (pl.itemTime.isEatMeal2) {
                                    Service.gI().sendThongBao(pl, "Chỉ được sử dụng 1 cái");
                                    break;
                                }
                                useItemTime(pl, item);
                                break;
                            case 899:
                            case 900:
                            case 902:
                            case 903:
                                if (pl.itemTime.isEatMeal3) {
                                    Service.gI().sendThongBao(pl, "Chỉ được sử dụng 1 cái");
                                    break;
                                }
                                useItemTime(pl, item);
                                break;
                            case 521:
                                useTDLT(pl, item);
                                break;
                            case 454:
                                usePorata(pl);
                                break;
                            case 921:
                                usePorata2(pl, item);
                                break;
                            case 1810:
                                usePorata3(pl, item);
                                break;
                            case 193:
                                openCapsuleUI(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 194:
                                openCapsuleUI(pl);
                                break;
                            case 401:
                                changePet(pl, item);
                                break;
                            case 402:
                            case 403:
                            case 404:
                            case 759:
                                upSkillPet(pl, item);
                                break;
                            case 726:
                                UseItem.gI().ItemManhGiay(pl, item);
                                break;
                            case 727:
                            case 728:
                                UseItem.gI().ItemSieuThanThuy(pl, item);
                                break;
                            case 648:
                                ItemService.gI().OpenItem648(pl, item);
                                break;
                            case 736:
                                ItemService.gI().OpenItem736(pl, item);
                                break;
                            case 987:
                                Service.gI().sendThongBao(pl, "Bảo vệ trang bị không bị rớt cấp");
                                break;
                            case 1955:
                                Input.gI().createFormChangeNameByItem(pl);
                                break;
                            case 1623:
                                TaskService.gI().sendNextTaskMain(pl);
                                break;
                            case 1228:
                                NpcService.gI().createMenuConMeo(pl, ConstNpc.HOP_QUA_THAN_LINH, -1,
                                        "Chọn hành tinh của đồ thần linh muốn nhận.",
                                        "Trái đất", "Namek", "Xayda");
                                break;
                            case 1626:
                                int[] listItem = {856, 943, 942};
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Item phuKien = ItemService.gI().createNewItem((short) listItem[Util.nextInt(2)]);
                                if (phuKien.template.id == 856) {
                                    phuKien.itemOptions.add(new Item.ItemOption(50, 10));
                                    phuKien.itemOptions.add(new Item.ItemOption(77, 10));
                                    phuKien.itemOptions.add(new Item.ItemOption(103, 10));
                                } else if (phuKien.template.id == 943) {
                                    phuKien.itemOptions.add(new Item.ItemOption(50, 10));
                                } else if (phuKien.template.id == 942) {
                                    phuKien.itemOptions.add(new Item.ItemOption(77, 10));
                                    phuKien.itemOptions.add(new Item.ItemOption(103, 10));
                                }
                                if (Util.isTrue(95, 100)) {
                                    phuKien.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 5)));
                                }
                                InventoryService.gI().addItemBag(pl, phuKien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + phuKien.template.name);
                                break;
                            case 1629:
                                if (pl.pet != null) {
                                    if (pl.pet.playerSkill.skills.get(2).skillId != -1) {
                                        pl.pet.openSkill3();
                                    } else {
                                        Service.gI().sendThongBao(pl, "Ít nhất đệ tử ngươi phải có chiêu 3 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                                break;
                            case 1630:
                                if (pl.pet != null) {
                                    if (pl.pet.playerSkill.skills.get(3).skillId != -1) {
                                        pl.pet.openSkill4();
                                    } else {
                                        Service.gI().sendThongBao(pl, "Ít nhất đệ tử ngươi phải có chiêu 4 chứ!");
                                        return;
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
                                    return;
                                }
                                break;
                            case 628:
                                int ct = Util.nextInt(618, 626);
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(50, 15));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(77, 15));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(103, 15));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(149, 1));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được cải trang " + caiTrangHaiTac.template.name);
                                break;
                            case 1440:
                                ct = Util.nextInt(441, 447);
                                caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + caiTrangHaiTac.template.name);
                                break;
                            case 1453:
                                ct = Util.nextInt(1416, 1422);
                                caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + caiTrangHaiTac.template.name);
                                break;
                            case 1536:
                                break;
                            case 1592:
                                UseItem.gI().Gokudayvip(pl, item);
                                break;
                            case 1377:
                                thiepchucvip(pl, item);
                                break;
                            case 1957:
                                hop2010(pl, item);
                                break;
                            case 1964:
                                CapsuleTrangSucVIP(pl, item);
                                break;
                            case 1873:
                                open1873(pl, item);
                                break;
                            case 1874:
                                open1874(pl, item);
                                break;
                            case 1875:
                                open1875(pl, item);
                                break;
                            case 1758:
                                UseItem.gI().Cadicvip(pl, item);
                                break;
                            case 1898:
                                UseItem.gI().RuongRongThan(pl, item);
                                break;
                            case 1703:
                                UseItem.gI().Hopdothanlinh(pl, item);
                                break;
                            case 1806:
                                UseItem.gI().Hopdovaitho(pl, item);
                                break;
                            case 1704:
                                UseItem.gI().Hopdohuydiet(pl, item);
                                break;
                            case 1938:
                                hopQuaTanThu(pl, item);
                                break;
                            case 718:
                                if (!pl.getSession().actived) {
                                    Service.gI().sendThongBao(pl, "Vui lòng kích hoạt tài khoản để có thể sử dụng");
                                    return;
                                }
                                Input.gI().createFormTangRuby(pl);
                                break;
                            case 1939:
                                ItemService.gI().setTiemNang(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1570:
                                opkhoabac(pl, item);
                                break;
                            case 1561:
                                opkhoavang(pl, item);
                                break;
                            case 1982:
                                openfa(pl, item);
                                break;
                            case 554:
                                dotPhaox100(pl, item);
                                break;
                            case 553:
                                dotPhao(pl, item);
                                break;
                            case 1314:
                                banhtrungthucaocap(pl, item);
                                break;
                            case 1981:
                                openthonglong(pl, item);
                                break;
                            case 1315:
                                opHopBanhNho(pl, item);
                                break;
                            case 1316:
                                banhtrungthuchay(pl, item);
                                break;
                            case 1317:
                                banhtrungthu(pl, item);
                                break;
                            case 1822:
                                changePetRamdom(pl, item);
                                break;
                            case 1952:
                                UseItem.gI().RadaNgocRong(pl, item.template.id);
                                break;
                            case 1947:
                            case 1948:
                            case 1949:
                            case 1950:
                            case 1951:
                                ThucAnChoThan(pl, item);
                                break;
                            case 1655:
                                hopQuaKichHoat(pl, item);
                                break;
                            case 1954:
                                OpenHopThanlinh(pl, item.template.id);
                                break;
                            case 1575:
                                UseItem.gI().PhaoBong(pl, item);
                                break;
                            case 1576:
                                UseItem.gI().PhaoBongVip(pl, item);
                                break;
                        }
                        break;
                }
                TaskService.gI().checkDoneTaskUseItem(pl, item);
                InventoryService.gI().sendItemBag(pl);
            } else {
                Service.gI().sendThongBaoOK(pl, "Sức mạnh không đủ yêu cầu");
            }
        }
    }

    private void hopQuaKichHoat(Player player, Item item) {
        NpcService.gI().createMenuConMeo(player,
                ConstNpc.MENU_OPTION_USE_ITEM1655, -1, "Chọn hành tinh của cậu đi",
                "Set trái đất",
                "Set namec",
                "Set xayda",
                "Từ chổi");
    }

    public void OpenHopThanlinh(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, itemUseiD);
            int[][] itemsByGenderAndType = {
                {555, 556, 562, 563, 561},
                {557, 558, 564, 565, 561},
                {559, 560, 566, 567, 561}
            };
            List<Item> allPreInitializedItems = new ArrayList<>();
            for (int[] genderItems : itemsByGenderAndType) {
                for (int itemId : genderItems) {
                    Item it = ItemService.gI().createNewItem((short) itemId);
                    RewardService.gI().initChiSoItem(it);
                    it.itemOptions.add(new ItemOption(30, 1));
                    allPreInitializedItems.add(it);
                }
            }
            Random random = new Random();
            Item chosenItem = allPreInitializedItems.get(random.nextInt(allPreInitializedItems.size()));
            InventoryService.gI().addItemBag(player, chosenItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            InventoryService.gI().sendItemBag(player);
            Service.gI().sendThongBao(player, "Bạn vừa nhận được 1 " + chosenItem.template.name + " Thần linh!");
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu có ít nhất 1 ô trống hành trang");
        }
    }

    private void ThucAnChoThan(Player player, Item item) {
        if (InventoryService.gI().getCountEmptyBag(player) > 4) {
            Item itemUsed = InventoryService.gI().findItemBag(player, item.template.id);
            if (itemUsed == null || itemUsed.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }
            int id = itemUsed.template.id;
            if ((id == 1747 || id == 1816 || id == 1817 || id == 1818 || id == 1819 || id == 1820 || id == 1821) && itemUsed.quantity >= 99) {
                InventoryService.gI().subQuantityItemsBag(player, itemUsed, 99);
                Item newItem = ItemService.gI().createNewItem((short) 1946);
                InventoryService.gI().addItemBag(player, newItem);
                Service.gI().sendThongBao(player, "Bạn đã nhận được 1 " + newItem.template.name + "!");
            } else {
                Service.gI().sendThongBao(player, "Số lượng vật phẩm không đủ hoặc không đúng loại!");
                return;
            }
            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBag(player);
        } else {
            Service.gI().sendThongBao(player, "Hành trang không đủ chỗ trống!");
        }
    }

    private int randomInRange(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    public void RadaNgocRong(Player player, int itemUseiD) {
        if (InventoryService.gI().getCountEmptyBag(player) > 0) {
            Item itemused = InventoryService.gI().findItemBag(player, 1952);
            if (itemused == null || itemused.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có vật phẩm cần dùng!");
                return;
            }
            short[] itemIds = {1579, 1580, 1581, 1582, 1583, 1584, 1585};
            short randomItemId = itemIds[(int) (Math.random() * itemIds.length)];
            Item newItem = ItemService.gI().createNewItem(randomItemId);
            RewardService.gI().initChiSoItem(newItem);
            newItem.itemOptions.add(new ItemOption(50, randomInRange(10, 17)));
            newItem.itemOptions.add(new ItemOption(77, randomInRange(10, 17)));
            newItem.itemOptions.add(new ItemOption(103, randomInRange(10, 17)));
            newItem.itemOptions.add(new ItemOption(30, 0));
            newItem.itemOptions.add(new ItemOption(93, (int) (Math.random() * 100) < 0.5 ? 0 : 15));
            InventoryService.gI().addItemBag(player, newItem);
            InventoryService.gI().subQuantityItemsBag(player, itemused, 1);
            PlayerService.gI().sendInfoHpMpMoney(player);
            InventoryService.gI().sendItemBag(player);
        }
    }

    private void changePetRamdom(Player player, Item item) {
        short[] icon = new short[2];
        icon[0] = item.template.iconID;
        if (item.template.id != 1822) {
            Service.gI().sendThongBao(player, "Vật phẩm không hợp lệ!");
            return;
        }
        if (!item.hasOption(249, 3000)) {
            Service.gI().sendThongBao(player, "Cần ít nhất 3000 sức mạnh Kilis để mở!");
            return;
        }
        if (player.pet == null || player.pet.typePet != 1 || player.pet.nPoint.power < 40_000_000_000L) {
            Service.gI().sendThongBao(player, "Cần có đệ Mabư đạt 40 tỷ sức mạnh để thực hiện!");
            return;
        }
        int[] petTypes = {2, 3, 4};
        int randomType = petTypes[rand.nextInt(petTypes.length)];
        switch (randomType) {
            case 2:
                PetService.gI().createUubPet(player);
                break;
            case 3:
                PetService.gI().createKidBeerPet(player);
                break;
            case 4:
                PetService.gI().createJirenPet(player);
                break;
        }
        InventoryService.gI().removeItemBag(player, item);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được đệ tử mới!");
        CombineService.gI().sendEffectOpenItem(player, icon[0], icon[1]);
    }

    private void banhtrungthucaocap(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            Item Pet = null;
            if (color == 2) {
                int[] vatpham = new int[]{1944};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(20, 25)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(15, 17)));
                Pet.itemOptions.add(new ItemOption(14, Util.nextInt(1, 7)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(15, 20)));
                Pet.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
            } else if (color == 3) {
                int[] vatpham = new int[]{1765, 1766, 1767};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(17, 23)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(17, 20)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(17, 20)));
                Pet.itemOptions.add(new ItemOption(83, Util.nextInt(10, 20)));
                Pet.itemOptions.add(new ItemOption(5, Util.nextInt(10, 15)));
                Pet.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
            } else if (color == 2) {
                int[] vatpham = new int[]{1700, 1943, 1945};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
            } else if (color == 1) {
                int[] vatpham = new int[]{1901, 1204, 1066, 1067, 1068, 1069, 1070, 1173};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
            } else {
                int[] vatpham = new int[]{1071, 1072, 1073, 1084, 1085, 1086, 1074, 1075, 1076, 1077, 1078, 1079, 1080, 1081, 1082, 1083, 1440};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
            }
            if (Pet != null) {
                pl.hopquatrungthuvip++;
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, Pet);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
            }
        }
    }

    private void openthonglong(Player pl, Item item) {
        if (pl.zone.map.mapId != 0 && pl.zone.map.mapId != 7 && pl.zone.map.mapId != 14) {
            Service.gI().sendThongBao(pl, "Không tìm thấy cậu vàng!");
            return;
        }
        banhtrungthucaocap(pl, item);
    }

    private void opHopBanhNho(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item it;
            if (Util.isTrue(25, 100)) {
                int[] ngocrong = new int[]{17, 17, 1143, 17, 16, 17, 17, 17, 17};
                it = ItemService.gI().createNewItem((short) ngocrong[rand.nextInt(ngocrong.length)]);
            } else if (Util.isTrue(50, 100)) {
                short[] temp = {381, 382, 383, 384};
                it = ItemService.gI().createNewItem(temp[rand.nextInt(temp.length)]);
                it.quantity = 1;
            } else {
                short[] temp = {1204, 859, 956};
                it = ItemService.gI().createNewItem(temp[rand.nextInt(temp.length)]);
                it.quantity = 1;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().addItemBag(pl, it);
            InventoryService.gI().sendItemBag(pl);
            Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void banhtrungthu(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            Item Pet = null;
            int ruby = 0;
            if (color == 5) {
                int[] vatpham = new int[]{730, 731, 732};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(165, 10));
                Pet.itemOptions.add(new ItemOption(50, 20));
                Pet.itemOptions.add(new ItemOption(103, 17));
                Pet.itemOptions.add(new ItemOption(77, 17));
                Pet.itemOptions.add(new ItemOption(1540, 0));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                ruby = Util.nextInt(500, 1000);
            } else if (color == 4) {
                int[] vatpham = new int[]{467, 468, 469, 470, 471};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                ruby = Util.nextInt(300, 500);
            } else if (color == 3) {
                int[] vatpham = new int[]{1765, 1766, 1767};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                ruby = Util.nextInt(200, 300);
            } else if (color == 2) {
                int[] vatpham = new int[]{1926, 1927};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                ruby = Util.nextInt(100, 200);
            } else if (color == 1) {
                int[] vatpham = new int[]{1921, 1922, 1923, 1924, 1925};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                ruby = Util.nextInt(50, 100);
            } else {
                int[] vatpham = new int[]{987, 16, 1173};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                ruby = Util.nextInt(1, 50);
            }
            pl.inventory.ruby += ruby;
            Service.gI().sendThongBao(pl, "Bạn nhận được " + ruby + " Hồng Ngọc");
            pl.hopquatrungthuvip++;
            Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().addItemBag(pl, Pet);
            InventoryService.gI().sendItemBag(pl);
            Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
        }
    }

    private void banhtrungthuchay(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            RandomCollection<Integer> rd = new RandomCollection<>();
            rd.add(15, 4);
            rd.add(15, 3);
            rd.add(15, 2);
            rd.add(30, 1);
            rd.add(10, 5);
            int color = rd.next();
            Item Pet = null;
            int gold = 0;
            if (color == 5) {
                int[] vatpham = new int[]{730, 731, 732};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(165, 10));
                Pet.itemOptions.add(new ItemOption(50, 20));
                Pet.itemOptions.add(new ItemOption(103, 17));
                Pet.itemOptions.add(new ItemOption(77, 17));
                Pet.itemOptions.add(new ItemOption(1540, 0));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                gold = Util.nextInt(500_000_000, 1_000_000_000);
            } else if (color == 4) {
                int[] vatpham = new int[]{467, 468, 469, 470, 471};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                gold = Util.nextInt(300_000_000, 500_000_000);
            } else if (color == 3) {
                int[] vatpham = new int[]{1765, 1766, 1767};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                gold = Util.nextInt(100_000_000, 300_000_000);
            } else if (color == 2) {
                int[] vatpham = new int[]{1926, 1927};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 6)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 6)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                gold = Util.nextInt(50_000_000, 100_000_000);
            } else if (color == 1) {
                int[] vatpham = new int[]{1921, 1922, 1923, 1924, 1925};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                Pet.itemOptions.add(new ItemOption(50, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(103, Util.nextInt(2, 10)));
                Pet.itemOptions.add(new ItemOption(77, Util.nextInt(2, 10)));
                if (Util.isTrue(90, 100)) Pet.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                gold = Util.nextInt(10_000_000, 50_000_000);
            } else {
                int[] vatpham = new int[]{987, 16, 1173};
                Pet = ItemService.gI().createNewItem((short) vatpham[rand.nextInt(vatpham.length)]);
                gold = Util.nextInt(1_000_000, 10_000_000);
            }
            pl.inventory.gold += gold;
            Service.gI().sendThongBao(pl, "Bạn nhận được " + gold + " vàng");
            pl.hopquatrungthuvip++;
            Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được 1 điểm sự kiện trung thu");
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().addItemBag(pl, Pet);
            InventoryService.gI().sendItemBag(pl);
            Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Pet.template.name);
        }
    }

    private void dotPhao(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int[] temp = {Util.nextInt(15, 16), 1281, 1282, 1283, 674};
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            Item it = ItemService.gI().createNewItem((short) temp[index]);
            if (temp[index] == 1278) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(30, 0));
                it.itemOptions.add(new ItemOption(93, Util.isTrue(1, 30) ? Util.nextInt(1, 7) : Util.nextInt(1, 30)));
            } else if (temp[index] == 1104 || temp[index] == 1105 || temp[index] == 1106) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(20, 30)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(20, 30)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(20, 30)));
                if (Util.isTrue(1, 50)) {
                    it.itemOptions.add(new ItemOption(74, 0));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else if (temp[index] == 380) {
                it.quantity = 5;
            } else if ((temp[index] >= 1185 && temp[index] <= 1186) || temp[index] == 1687) {
                setOptionItem(it, 5, 12);
            } else if (temp[index] >= 1202 && temp[index] <= 1203) {
                setOptionItem(it, 1, 8);
            } else if (temp[index] == 743) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 12)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 12)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 10)));
                it.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(10, 100)) {
                    it.itemOptions.add(new ItemOption(74, 0));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else {
                it.itemOptions.add(new ItemOption(73, 0));
            }
            InventoryService.gI().addItemBag(pl, it);
            icon[1] = it.template.iconID;
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
            pl.phaobong += 1;
            activePhaoHoa(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hành trang của bạn không đủ ô trống");
        }
    }

    private void dotPhaox100(Player pl, Item item) {
        int soLuongLam = 100;
        if (item.quantity < soLuongLam) {
            Service.gI().sendThongBao(pl, "Bạn cần ít nhất " + soLuongLam + " pháo.");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(pl) < 5) {
            Service.gI().sendThongBao(pl, "Bạn cần ít nhất 5 ô trống hành trang.");
            return;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, soLuongLam);
        int[] itemHiem = {1281, 1282, 1283, 674};
        for (int i = 0; i < soLuongLam; i++) {
            short idItem;
            if (Util.isTrue(95, 100)) {
                idItem = 15;
            } else {
                idItem = (short) itemHiem[Util.nextInt(0, itemHiem.length - 1)];
            }
            Item it = ItemService.gI().createNewItem(idItem);
            if (idItem == 1278) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 8)));
                it.itemOptions.add(new ItemOption(30, 0));
                it.itemOptions.add(new ItemOption(93, Util.isTrue(1, 30) ? Util.nextInt(1, 7) : Util.nextInt(1, 30)));
            } else if (idItem == 1104 || idItem == 1105 || idItem == 1106) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(20, 30)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(20, 30)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(20, 30)));
                if (Util.isTrue(1, 50)) {
                    it.itemOptions.add(new ItemOption(74, 0));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else if (idItem == 380) {
                it.quantity = 5;
            } else if ((idItem >= 1185 && idItem <= 1186) || idItem == 1687) {
                setOptionItem(it, 5, 12);
            } else if (idItem >= 1202 && idItem <= 1203) {
                setOptionItem(it, 1, 8);
            } else if (idItem == 743) {
                it.itemOptions.add(new ItemOption(77, Util.nextInt(1, 12)));
                it.itemOptions.add(new ItemOption(103, Util.nextInt(1, 12)));
                it.itemOptions.add(new ItemOption(50, Util.nextInt(1, 10)));
                it.itemOptions.add(new ItemOption(30, 0));
                if (Util.isTrue(10, 100)) {
                    it.itemOptions.add(new ItemOption(74, 0));
                } else {
                    it.itemOptions.add(new ItemOption(93, Util.nextInt(1, 30)));
                }
            } else {
                it.itemOptions.add(new ItemOption(73, 0));
            }
            InventoryService.gI().addItemBag(pl, it);
        }
        InventoryService.gI().sendItemBag(pl);
        pl.phaobong += 100;
        Service.gI().sendThongBao(pl, "Đã đốt 100 pháo hoa!");
        activePhaoHoa(pl);
    }

    public static void activePhaoHoa(Player pl) {
        for (int i = 0; i < 20; i++) {
            EffectMapService.gI().sendEffectMapToAllInMap(
                    pl.zone,
                    Util.nextInt(64, 65),
                    1, 1,
                    pl.location.x + Util.nextInt(-100, 100),
                    pl.location.y - Util.nextInt(20, 70),
                    Util.nextInt(1, 12)
            );
        }
    }

    private void setOptionItem(Item item, int min, int max) {
        int[] temp = {50, 77, 103};
        int ops = temp[Util.nextInt(0, temp.length - 1)];
        int param = Util.nextInt(min, max);
        if (ops == 101) param = Util.nextInt(min, max * 2);
        if (ops == 14) param = Util.nextInt(1, 10);
        if (ops == 94) param = Util.nextInt(5, 15);
        if (Util.isTrue(95, 100)) item.itemOptions.add(new ItemOption(93, Util.nextInt(1, 5)));
        item.itemOptions.add(new ItemOption(ops, param));
    }

    private void opkhoabac(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item ruongkhobau = InventoryService.gI().findItemBag(pl, 1569);
            if (ruongkhobau != null) {
                RandomCollection<Integer> rd = new RandomCollection<>();
                rd.add(40, 5);
                rd.add(35, 4);
                rd.add(25, 3);
                int color = rd.next();
                if (color == 4) {
                    short[] temp = {441, 442, 447, 381, 382, 383, 384};
                    short id = temp[Util.nextInt(0, temp.length - 1)];
                    Item it = ItemService.gI().createNewItem(id);
                    if (id == 441) {
                        it.itemOptions.add(new ItemOption(95, 5));
                        it.quantity = 2;
                    } else if (id == 442) {
                        it.itemOptions.add(new ItemOption(96, 5));
                        it.quantity = 2;
                    } else if (id == 447) {
                        it.itemOptions.add(new ItemOption(101, 5));
                        it.quantity = 2;
                    } else {
                        it.quantity = 1;
                    }
                    InventoryService.gI().addItemBag(pl, it);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
                } else if (color == 3) {
                    int[] ngocrong = new int[]{18, 17, 18};
                    Item pet = ItemService.gI().createNewItem((short) ngocrong[Util.nextInt(0, ngocrong.length - 1)]);
                    InventoryService.gI().addItemBag(pl, pet);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
                } else {
                    int[] itdeolung = new int[]{1554, 1555};
                    Item Itdeolung = ItemService.gI().createNewItem((short) itdeolung[Util.nextInt(0, itdeolung.length - 1)]);
                    Itdeolung.itemOptions.add(new ItemOption(50, Util.nextInt(2, 8)));
                    Itdeolung.itemOptions.add(new ItemOption(77, Util.nextInt(3, 6)));
                    Itdeolung.itemOptions.add(new ItemOption(103, Util.nextInt(3, 6)));
                    if (Util.isTrue(95, 100)) Itdeolung.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    InventoryService.gI().addItemBag(pl, Itdeolung);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Itdeolung.template.name);
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Bạn nhận được 1 điểm kho báu");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void opkhoavang(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item ruongkhobau = InventoryService.gI().findItemBag(pl, 1569);
            if (ruongkhobau != null) {
                RandomCollection<Integer> rd = new RandomCollection<>();
                rd.add(60, 4);
                rd.add(30, 5);
                rd.add(10, 3);
                rd.add(1, 2);
                rd.add(1, 1);
                int color = rd.next();
                if (color == 4) {
                    short[] temp = {1150, 1151, 1152, 1153};
                    Item it = ItemService.gI().createNewItem(temp[Util.nextInt(0, temp.length - 1)]);
                    InventoryService.gI().addItemBag(pl, it);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
                } else if (color == 2) {
                    short[] set2 = {555, 556, 563, 557, 558, 565, 559, 567, 560};
                    Item itemReward = ItemService.gI().createNewItem(set2[Util.nextInt(0, set2.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type, itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward, new RewardService.RatioStar[]{new RewardService.RatioStar((byte) 1, 1, 2), new RewardService.RatioStar((byte) 2, 1, 3), new RewardService.RatioStar((byte) 3, 1, 4), new RewardService.RatioStar((byte) 4, 1, 5)});
                    InventoryService.gI().addItemBag(pl, itemReward);
                    Service.gI().sendMoney(pl);
                } else if (color == 3) {
                    int[] ngocrong = new int[]{16, 17};
                    Item pet = ItemService.gI().createNewItem((short) ngocrong[Util.nextInt(0, ngocrong.length - 1)]);
                    InventoryService.gI().addItemBag(pl, pet);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
                } else if (color == 1) {
                    short[] set1 = {562, 564, 566, 561};
                    Item itemReward = ItemService.gI().createNewItem(set1[Util.nextInt(0, set1.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type, itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward, new RewardService.RatioStar[]{new RewardService.RatioStar((byte) 1, 1, 2), new RewardService.RatioStar((byte) 2, 1, 3), new RewardService.RatioStar((byte) 3, 1, 4), new RewardService.RatioStar((byte) 4, 1, 5)});
                    InventoryService.gI().addItemBag(pl, itemReward);
                    Service.gI().sendMoney(pl);
                } else if (color == 5) {
                    short[] itdeolung = {1578, 1563, 1603};
                    Item Itdeolung = ItemService.gI().createNewItem(itdeolung[Util.nextInt(0, itdeolung.length - 1)]);
                    Itdeolung.itemOptions.add(new ItemOption(50, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(77, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(103, Util.nextInt(5, 12)));
                    if (Util.isTrue(950, 1000)) Itdeolung.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    InventoryService.gI().addItemBag(pl, Itdeolung);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Itdeolung.template.name);
                }
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Bạn nhận được 1 điểm kho báu");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void PhaoBong(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int[][] gold = {{5000, 20000}};
        pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
        if (pl.inventory.gold > Inventory.LIMIT_GOLD) pl.inventory.gold = Inventory.LIMIT_GOLD;
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Item removeItem = InventoryService.gI().findItemBag(pl, 1575);
        if (removeItem != null) InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBag(pl);
    }

    private void PhaoBongVip(Player pl, Item item) {
        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.XSMAX, 1);
        int[][] gold = {{500000, 2000000}};
        pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
        if (pl.inventory.gold > Inventory.LIMIT_GOLD) pl.inventory.gold = Inventory.LIMIT_GOLD;
        Service.gI().LogicEffect(pl, 62, 1, -1, 1, 1, 15000);
        Service.gI().LogicEffect(pl, 63, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 64, 1, -1, 1, 1, 5000);
        Service.gI().LogicEffect(pl, 65, 1, -1, 1, 1, 5000);
        Item removeItem = InventoryService.gI().findItemBag(pl, 1576);
        if (removeItem != null) InventoryService.gI().subQuantityItemsBag(pl, removeItem, 1);
        PlayerService.gI().sendInfoHpMpMoney(pl);
        InventoryService.gI().sendItemBag(pl);
    }

    public void hopQuaTanThu(Player pl, Item it) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 23) {
            int gender = pl.gender;
            int soluongitem = ConstItem.LIST_ITEM_CLOTHES[0][0].length;
            int[] id = {gender, 6 + gender, 21 + gender, 27 + gender, 12, 194, 441, 442, 443, 444, 445, 446, 447, 381, 382, 383, 384, 385, 16, 17, 18, 19, 20};
            int[] soluong = {1, 1, 1, 1, 1, 1, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999, 999};
            int[] option = {0, 0, 0, 0, 0, 73, 95, 96, 97, 98, 99, 100, 101, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30};
            int[] param = {0, 0, 0, 0, 0, 0, 5, 5, 5, 3, 3, 5, 5, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
            for (int i = 0; i < id.length - 1; i++) {
                Item item = ItemService.gI().createNewItem((short) id[i]);
                if (i < 5) {
                    RewardService.gI().initBaseOptionClothes(item.template.id, item.template.type, item.itemOptions);
                    item.itemOptions.add(new ItemOption(107, 4));
                } else {
                    item.quantity = soluong[i];
                    item.itemOptions.add(new ItemOption(option[i], param[i]));
                }
                InventoryService.gI().addItemBag(pl, item);
            }
            for (int j = 0; j < 5; j++) {
                Item item = ItemService.gI().createNewItem((short) ConstItem.LIST_ITEM_CLOTHES[gender][j][soluongitem - 1]);
                RewardService.gI().initBaseOptionClothes(item.template.id, item.template.type, item.itemOptions);
                item.itemOptions.add(new ItemOption(30, 1));
                InventoryService.gI().addItemBag(pl, item);
            }
            InventoryService.gI().subQuantityItemsBag(pl, it, 1);
            InventoryService.gI().sendItemBag(pl);
            Service.gI().sendThongBao(pl, "Chúc bạn chơi game vui vẻ");
        } else {
            Service.gI().sendThongBao(pl, "Cần tối thiểu 14 ô trống để nhận thưởng");
        }
    }

    public void openRuongGo(Player player) {
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE1, 1);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.GO_DAU_TRE2, 1);
        Item ruongGo = InventoryService.gI().findItemBag(player, 570);
        if (ruongGo != null) {
            int level = InventoryService.gI().getParam(player, 72, 570);
            int requiredSlots = calculateRequiredEmptySlots(level);
            if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
                Service.gI().sendThongBao(player, "Cần ít nhất " + (requiredSlots - InventoryService.gI().getCountEmptyBag(player)) + " ô trống trong hành trang");
            } else {
                player.itemsWoodChest.clear();
                if (level == 0) {
                    InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                    InventoryService.gI().sendItemBag(player);
                    Item item = ItemService.gI().createNewItem((short) 190);
                    item.quantity = 1;
                    InventoryService.gI().addItemBag(player, item);
                    InventoryService.gI().sendItemBag(player);
                    Service.gI().sendThongBao(player, "reward");
                    return;
                }
                int baseGoldAmount = 100 * level;
                int goldAmount = baseGoldAmount + (baseGoldAmount * Util.nextInt(-15, 15) / 100);
                Item itemGold = ItemService.gI().createNewItem((short) 190);
                itemGold.quantity = goldAmount * 1000;
                player.itemsWoodChest.add(itemGold);
                if (level >= 9) {
                    Item item77 = ItemService.gI().createNewItem((short) 77);
                    item77.quantity = 100 + (level - 9) * 20;
                    player.itemsWoodChest.add(item77);
                }
                int clothesCount = 1;
                if (level >= 5 && level <= 8) clothesCount = 2;
                else if (level >= 10 && level <= 12) clothesCount = 3;
                for (int i = 0; i < clothesCount; i++) {
                    int randItemId = randClothes(level);
                    Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) randItemId);
                    if (ops != null && !ops.isEmpty()) rewardItem.itemOptions.addAll(ops);
                    rewardItem.quantity = 1;
                    player.itemsWoodChest.add(rewardItem);
                }
                int[] rewardItems = {17, 18, 19, 20, 380, 381, 382, 383, 384, 385, 1229};
                int rewardCount = 2;
                if (level >= 5 && level <= 8) rewardCount = 3;
                else if (level >= 10 && level <= 12) rewardCount = 4;
                Set<Integer> selectedItems = new HashSet<>();
                while (selectedItems.size() < rewardCount) {
                    int randItemId = rewardItems[Util.nextInt(0, rewardItems.length - 1)];
                    if (!selectedItems.contains(randItemId)) {
                        selectedItems.add(randItemId);
                        Item rewardItem = ItemService.gI().createNewItem((short) randItemId);
                        rewardItem.quantity = Util.nextInt(1, level);
                        player.itemsWoodChest.add(rewardItem);
                    }
                }
                int saoPhaLeCount = (level > 9) ? 2 : 1;
                for (int i = 0; i < saoPhaLeCount; i++) {
                    int rand = Util.nextInt(0, 6);
                    Item level1 = ItemService.gI().createNewItem((short) (441 + rand));
                    level1.itemOptions.add(new ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));
                    level1.quantity = Util.nextInt(1, 3);
                    player.itemsWoodChest.add(level1);
                }
                int dncCount = (level > 9) ? 2 : 1;
                for (int i = 0; i < dncCount; i++) {
                    int rand = Util.nextInt(0, 4);
                    Item dnc = ItemService.gI().createNewItem((short) (220 + rand));
                    dnc.itemOptions.add(new ItemOption(71 - rand, 0));
                    dnc.quantity = Util.nextInt(1, level * 2);
                    player.itemsWoodChest.add(dnc);
                }
                InventoryService.gI().subQuantityItemsBag(player, ruongGo, 1);
                InventoryService.gI().sendItemBag(player);
                for (Item it : player.itemsWoodChest) InventoryService.gI().addItemBag(player, it);
                InventoryService.gI().sendItemBag(player);
                player.indexWoodChest = player.itemsWoodChest.size() - 1;
                int i = player.indexWoodChest;
                if (i < 0) return;
                Item itemWoodChest = player.itemsWoodChest.get(i);
                player.indexWoodChest--;
                String info = "|1|" + itemWoodChest.template.name;
                if (itemWoodChest.quantity > 1) info += " (x" + itemWoodChest.quantity + ")";
                StringBuilder info2 = new StringBuilder("\n|2|");
                if (!itemWoodChest.itemOptions.isEmpty()) {
                    for (Item.ItemOption io : itemWoodChest.itemOptions) {
                        if (io.optionTemplate.id != 102 && io.optionTemplate.id != 73) info2.append(io.getOptionString()).append("\n");
                    }
                }
                info = (info2.length() > "\n|2|".length() ? (info + info2).trim() : info.trim()) + "\n|0|" + itemWoodChest.template.description;
                NpcService.gI().createMenuConMeo(player, ConstNpc.RUONG_GO, -1, "Bạn nhận được\n" + info.trim(), "OK" + (i > 0 ? " [" + i + "]" : ""));
            }
        }
    }

    public int calculateRequiredEmptySlots(int level) {
        int requiredSlots = 0;
        int baseGoldAmount = 100 * level;
        int goldAmount = baseGoldAmount + (baseGoldAmount * Util.nextInt(-15, 15) / 100);
        if (goldAmount > 0) requiredSlots++;
        int clothesCount = 1;
        if (level >= 5 && level <= 8) clothesCount = 2;
        else if (level >= 10 && level <= 12) clothesCount = 3;
        requiredSlots += clothesCount;
        int rewardCount = 2;
        if (level >= 5 && level <= 8) rewardCount = 3;
        else if (level >= 10 && level <= 12) rewardCount = 4;
        requiredSlots += rewardCount;
        requiredSlots += (level > 9) ? 2 : 1; 
        requiredSlots += (level > 9) ? 2 : 1; 
        return requiredSlots;
    }

    private void changePet(Player player, Item item) {
        if (player.pet != null) {
            int gender = player.pet.gender + 1;
            if (gender > 2) gender = 0;
            PetService.gI().changeNormalPet(player, gender);
            InventoryService.gI().subQuantityItemsBag(player, item, 1);
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
    }

    private void eatGrapes(Player pl, Item item) {
        int percentCurrentStatima = pl.nPoint.stamina * 100 / pl.nPoint.maxStamina;
        if (percentCurrentStatima > 50) {
            Service.gI().sendThongBao(pl, "Thể lực vẫn còn trên 50%");
            return;
        } else if (item.template.id == 211) {
            pl.nPoint.stamina = pl.nPoint.maxStamina;
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 100%");
        } else if (item.template.id == 212) {
            pl.nPoint.stamina += (pl.nPoint.maxStamina * 20 / 100);
            Service.gI().sendThongBao(pl, "Thể lực của bạn đã được hồi phục 20%");
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
        PlayerService.gI().sendCurrentStamina(pl);
    }

    private void openCSKB(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = {76, 188, 189, 190, 381, 382, 383, 384, 385};
            int[][] gold = {{5000, 20000}};
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (index <= 3) {
                pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) pl.inventory.gold = Inventory.LIMIT_GOLD;
                PlayerService.gI().sendInfoHpMpMoney(pl);
                icon[1] = 930;
            } else {
                Item it = ItemService.gI().createNewItem(temp[index]);
                it.itemOptions.add(new ItemOption(73, 0));
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;
            }
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private boolean isUsingSameTypeBuff(Player pl, int type) {
        return switch (type) {
            case 382 ->
                pl.itemTime.isUseBoHuyet || pl.itemTime.isUseBoHuyet2;
            case 383 ->
                pl.itemTime.isUseBoKhi || pl.itemTime.isUseBoKhi2;
            case 384 ->
                pl.itemTime.isUseGiapXen || pl.itemTime.isUseGiapXen2;
            case 381 ->
                pl.itemTime.isUseCuongNo || pl.itemTime.isUseCuongNo2;
            case 385 ->
                pl.itemTime.isUseAnDanh || pl.itemTime.isUseAnDanh2;
            default ->
                false;
        };
    }

    private void useItemTime(Player pl, Item item) {
        long now = System.currentTimeMillis();
        switch (item.template.id) {
            case 379 -> {
                pl.itemTime.lastTimeUseMayDo = System.currentTimeMillis();
                pl.itemTime.isUseMayDo = true;
            }
            case 1809 -> {
                pl.itemTime.lastTimevevang = now;
                pl.itemTime.isUsevevang = true;
            }
            case 1731 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX10 || pl.itemTime.isUseLoX15) {
                    return;
                }
                pl.itemTime.lastTimeLoX2 = now;
                pl.itemTime.isUseLoX2 = true;
            }
            case 1727 -> {
                if (pl.itemTime.isUseLoX2 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX10 || pl.itemTime.isUseLoX15) {
                    return;
                }
                pl.itemTime.lastTimeLoX5 = now;
                pl.itemTime.isUseLoX5 = true;
            }
            case 1728 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX2 || pl.itemTime.isUseLoX10 || pl.itemTime.isUseLoX15) {
                    return;
                }
                pl.itemTime.lastTimeLoX7 = now;
                pl.itemTime.isUseLoX7 = true;
            }
            case 1729 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX2 || pl.itemTime.isUseLoX15) {
                    return;
                }
                pl.itemTime.lastTimeLoX10 = now;
                pl.itemTime.isUseLoX10 = true;
            }
            case 1730 -> {
                if (pl.itemTime.isUseLoX5 || pl.itemTime.isUseLoX7 || pl.itemTime.isUseLoX10 || pl.itemTime.isUseLoX2) {
                    return;
                }
                pl.itemTime.lastTimeLoX15 = now;
                pl.itemTime.isUseLoX15 = true;
            }
            case 764 -> {
                pl.itemTime.lastTimeKhauTrang = now;
                pl.itemTime.isUseKhauTrang = true;
                Service.gI().Send_Caitrang(pl);
            }
            case 1628 -> {
                pl.itemTime.lastTimeBuax2DeTu = now;
                pl.itemTime.isUseBuax2DeTu = true;
            }
            case 382 -> {
                if (isUsingSameTypeBuff(pl, 382)) {
                    return;
                }
                pl.itemTime.lastTimeBoHuyet = now;
                pl.itemTime.isUseBoHuyet = true;
            }
            case 383 -> {
                if (isUsingSameTypeBuff(pl, 383)) {
                    return;
                }
                pl.itemTime.lastTimeBoKhi = now;
                pl.itemTime.isUseBoKhi = true;
            }
            case 384 -> {
                if (isUsingSameTypeBuff(pl, 384)) {
                    return;
                }
                pl.itemTime.lastTimeGiapXen = now;
                pl.itemTime.isUseGiapXen = true;
            }
            case 381 -> {
                if (isUsingSameTypeBuff(pl, 381)) {
                    return;
                }
                pl.itemTime.lastTimeCuongNo = now;
                pl.itemTime.isUseCuongNo = true;
                Service.gI().point(pl);
            }
            case 385 -> {
                if (isUsingSameTypeBuff(pl, 385)) {
                    return;
                }
                pl.itemTime.lastTimeAnDanh = now;
                pl.itemTime.isUseAnDanh = true;
            }
            case 1151 -> {
                if (isUsingSameTypeBuff(pl, 382)) {
                    return;
                }
                pl.itemTime.lastTimeBoHuyet2 = now;
                pl.itemTime.isUseBoHuyet2 = true;
            }
            case 1152 -> {
                if (isUsingSameTypeBuff(pl, 383)) {
                    return;
                }
                pl.itemTime.lastTimeBoKhi2 = now;
                pl.itemTime.isUseBoKhi2 = true;
            }
            case 1153 -> {
                if (isUsingSameTypeBuff(pl, 384)) {
                    return;
                }
                pl.itemTime.lastTimeGiapXen2 = now;
                pl.itemTime.isUseGiapXen2 = true;
            }
            case 1150 -> {
                if (isUsingSameTypeBuff(pl, 381)) {
                    return;
                }
                pl.itemTime.lastTimeCuongNo2 = now;
                pl.itemTime.isUseCuongNo2 = true;
                Service.gI().point(pl);
            }
            case 1154 -> {
                if (isUsingSameTypeBuff(pl, 385)) {
                    return;
                }
                pl.itemTime.lastTimeAnDanh2 = now;
                pl.itemTime.isUseAnDanh2 = true;
            }
            case 1980 -> {
                if (pl.itemTime.ispho2 || pl.itemTime.ispho3) {
                    return;
                }
                pl.itemTime.lastTimepho1 = now;
                pl.itemTime.ispho1 = true;
            }
            case 1979 -> {
                if (pl.itemTime.ispho1 || pl.itemTime.ispho3) {
                    return;
                }
                pl.itemTime.lastTimepho2 = now;
                pl.itemTime.ispho2 = true;
            }
            case 1978 -> {
                if (pl.itemTime.ispho1 || pl.itemTime.ispho2) {
                    return;
                }
                pl.itemTime.lastTimepho3 = now;
                pl.itemTime.ispho3 = true;
            }
            case 465 -> {
                if (pl.itemTime.isBanhTrungThu2Trung) {
                    return;
                }
                pl.itemTime.lastTimeBanhTrungThu1Trung = now;
                pl.itemTime.isBanhTrungThu1Trung = true;
            }
            case 466 -> {
                if (pl.itemTime.isBanhTrungThu1Trung) {
                    return;
                }
                pl.itemTime.lastTimeBanhTrungThu2Trung = now;
                pl.itemTime.isBanhTrungThu2Trung = true;
            }
            case 472 -> {
                pl.itemTime.lastTimeBanhTrungThuDb = now;
                pl.itemTime.isBanhTrungThuDb = true;
            }
            case 473 -> {
                pl.itemTime.lastTimeBanhTrungThuHop = now;
                pl.itemTime.isBanhTrungHop = true;
            }
            case 638 -> {
                pl.itemTime.lastTimeUseCMS = now;
                pl.itemTime.isUseCMS = true;
            }
            case 2160 -> {
                pl.itemTime.lastTimeUseNCD = now;
                pl.itemTime.isUseNCD = true;
            }
            case 579, 1045 -> {
                pl.itemTime.lastTimeUseDK = now;
                pl.itemTime.isUseDK = true;
            }
            case 663, 664, 665, 666, 667 -> {
                pl.itemTime.lastTimeEatMeal = now;
                pl.itemTime.isEatMeal = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal);
                pl.itemTime.iconMeal = item.template.iconID;
            }
            case 880, 881, 882 -> {
                pl.itemTime.lastTimeEatMeal2 = now;
                pl.itemTime.isEatMeal2 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal2);
                pl.itemTime.iconMeal2 = item.template.iconID;
            }
            case 899, 900, 902, 903 -> {
                pl.itemTime.lastTimeEatMeal3 = now;
                pl.itemTime.isEatMeal3 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal3);
                pl.itemTime.iconMeal3 = item.template.iconID;
            }
            case 1109 -> {
                pl.itemTime.lastTimeUseMayDo2 = now;
                pl.itemTime.isUseMayDo2 = true;
            }
            case 1635 -> {
                long remaining = 0;
                if (pl.itemTime.isCoBonLa) {
                    remaining = ItemTime.TIME_CO_BON_LA - (now - pl.itemTime.lastTimeCoBonLa);
                    if (remaining < 0) {
                        remaining = 0;
                    }
                } else {
                    pl.itemTime.lastTimeCoBonLa = now;
                }
                long total = remaining + ItemTime.TIME_CO_BON_LA;
                pl.itemTime.lastTimeCoBonLa = now - (ItemTime.TIME_CO_BON_LA - total);
                pl.itemTime.isCoBonLa = true;
            }
            case 1533 -> {
                long remaining = 0;
                if (pl.itemTime.ischuotmap) {
                    remaining = ItemTime.TIME_CHUOT_MAP - (now - pl.itemTime.lastTimechuotmap);
                    if (remaining < 0) {
                        remaining = 0;
                    }
                } else {
                    pl.itemTime.lastTimechuotmap = now;
                }
                long total = remaining + ItemTime.TIME_CHUOT_MAP;
                pl.itemTime.lastTimechuotmap = now - (ItemTime.TIME_CHUOT_MAP - total);
                pl.itemTime.ischuotmap = true;
            }
        }
        Service.gI().point(pl);
        ItemTimeService.gI().sendAllItemTime(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
    }

    private void controllerCallRongThan(Player pl, Item item) {
        int tempId = item.template.id;
        if (tempId >= SummonDragon.NGOC_RONG_1_SAO && tempId <= SummonDragon.NGOC_RONG_7_SAO) {
            switch (tempId) {
                case SummonDragon.NGOC_RONG_1_SAO:
                case SummonDragon.NGOC_RONG_2_SAO:
                case SummonDragon.NGOC_RONG_3_SAO:
                    SummonDragon.gI().openMenuSummonShenron(pl, (byte) (tempId - 13));
                    break;
                default:
                    NpcService.gI().createMenuConMeo(pl, ConstNpc.TUTORIAL_SUMMON_DRAGON, -1, "Bạn chỉ có thể gọi rồng từ ngọc 3 sao, 2 sao, 1 sao", "Hướng\ndẫn thêm\n(mới)", "OK");
                    break;
            }
        } else if (tempId >= ShenronEventService.NGOC_RONG_1_SAO && tempId <= ShenronEventService.NGOC_RONG_7_SAO) {
            ShenronEventService.gI().openMenuSummonShenron(pl, 0);
        } else if (tempId >= ShenronEventServicenoel.NGOC_RONG_1_SAO && tempId <= ShenronEventServicenoel.NGOC_RONG_7_SAO) {
            ShenronEventServicenoel.gI().openMenuSummonShenron(pl, 0);
        }
    }

    private void learnSkill(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                String[] subName = item.template.name.split("");
                byte level = Byte.parseByte(subName[subName.length - 1]);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill.point == 7) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill.point == 0) {
                        if (level == 1) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            Skill skillNeed = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " cấp " + skillNeed.point + " trước!");
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            pl.BoughtSkill.add((int) item.template.id);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp " + (curSkill.point + 1) + " trước!");
                        }
                    }
                    InventoryService.gI().sendItemBag(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            Logger.logException(UseItem.class, e);
        }
    }

    private void useTDLT(Player pl, Item item) {
        if (pl.itemTime.isUseTDLT) {
            ItemTimeService.gI().turnOffTDLT(pl, item);
        } else {
            ItemTimeService.gI().turnOnTDLT(pl, item);
        }
    }

    private void usePorata(Player pl) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                pl.pet.fusion(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata2(Player pl, Item item) {
        if (pl.pet == null || pl.fusion.typeFusion == 4 || pl.fusion.typeFusion == 6 || pl.fusion.typeFusion == 10 || pl.fusion.typeFusion == 12) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                for (ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 50 || io.optionTemplate.id == 77 || io.optionTemplate.id == 103) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    }
                }
                pl.pet.fusion2(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void usePorata3(Player pl, Item item) {
        if (pl.pet == null || pl.fusion.typeFusion == 4 || pl.fusion.typeFusion == 6 || pl.fusion.typeFusion == 8) {
            Service.gI().sendThongBao(pl, "Dạng hợp thể không phù hợp");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                for (ItemOption io : item.itemOptions) {
                    if (io.optionTemplate.id == 50 || io.optionTemplate.id == 77 || io.optionTemplate.id == 103) {
                        pl.idOtPorata = io.optionTemplate.id;
                        pl.paramPorata = io.param;
                    }
                }
                pl.pet.fusion3(true);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void BinhNuoc(Player pl, Item item) {
        List<Player> bosses = pl.zone.getBosses();
        boolean checkSoi = false;
        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.XINBATO_1 && !pl.isDie()) {
                    checkSoi = true;
                    Boss xinbato = (Boss) bossPlayer;
                    if (!((Xinbato) xinbato).Check()) {
                        ((Xinbato) xinbato).NhatXuong1();
                        Service.gI().chat(xinbato, "Cảm ơn " + pl.name + " đã cho ta bình nước");
                    }
                    ItemMap itemMap = new ItemMap(pl.zone, 456, 99, pl.location.x, pl.zone.map.yPhysicInTop(pl.location.x, pl.location.y - 24), pl.id);
                    itemMap.isPickedUp = true;
                    itemMap.createTime -= 23000;
                    Service.gI().dropItemMap(pl.zone, itemMap);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    if (Util.nextInt(4) < 3) {
                        short idItem = (short) (Util.nextInt(0, 6) + 441);
                        Item it = ItemService.gI().createNewItem(idItem);
                        it.itemOptions.add(new Item.ItemOption(95 + (idItem - 441), (idItem == 444 || idItem == 445) ? 3 : 5));
                        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                            InventoryService.gI().addItemBag(pl, it);
                            Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                        } else {
                            Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                        }
                    } else {
                        Item it = ItemService.gI().createNewItem((short) 459);
                        it.itemOptions.add(new Item.ItemOption(112, 80));
                        it.itemOptions.add(new Item.ItemOption(93, 90));
                        it.itemOptions.add(new Item.ItemOption(20, Util.nextInt(10000)));
                        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                            InventoryService.gI().addItemBag(pl, it);
                            Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                        } else {
                            Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                        }
                    }
                    ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                    ((Xinbato) xinbato).leaveMapNew();
                }
            }
        }
        if (!checkSoi) Service.gI().sendThongBao(pl, "");
        InventoryService.gI().sendItemBag(pl);
    }

    private void CucXuong(Player pl, Item item) {
        List<Player> bosses = pl.zone.getBosses();
        boolean checkSoi = false;
        synchronized (bosses) {
            for (Player bossPlayer : bosses) {
                if (bossPlayer.id == BossID.SOI_HEC_QUYN_1 && !pl.isDie()) {
                    checkSoi = true;
                    Boss soihecQuyn = (Boss) bossPlayer;
                    if (((SoiHecQuyn) soihecQuyn).KiemTraNhatXuong()) {
                        Service.gI().sendThongBao(pl, "Sói đã no rồi");
                        continue;
                    } else {
                        ((SoiHecQuyn) soihecQuyn).NhatXuong();
                        Service.gI().chat(soihecQuyn, "Ê, Cục xương ngon quá");
                    }
                    ItemMap itemMap = new ItemMap(pl.zone, 460, 1, pl.location.x, pl.zone.map.yPhysicInTop(pl.location.x, pl.location.y - 24), pl.id);
                    itemMap.isPickedUp = true;
                    itemMap.createTime -= 23000;
                    Service.gI().dropItemMap(pl.zone, itemMap);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    if (Util.nextInt(4) < 3) {
                        short idItem = (short) (Util.nextInt(0, 6) + 441);
                        Item it = ItemService.gI().createNewItem(idItem);
                        it.itemOptions.add(new Item.ItemOption(95 + (idItem - 441), (idItem == 444 || idItem == 445) ? 3 : 5));
                        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                            InventoryService.gI().addItemBag(pl, it);
                            Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                        } else {
                            Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                        }
                    } else {
                        Item it = ItemService.gI().createNewItem((short) 459);
                        it.itemOptions.add(new Item.ItemOption(112, 80));
                        it.itemOptions.add(new Item.ItemOption(93, 90));
                        it.itemOptions.add(new Item.ItemOption(20, Util.nextInt(10000)));
                        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                            InventoryService.gI().addItemBag(pl, it);
                            Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
                        } else {
                            Service.gI().sendThongBao(pl, "Hành trang không đủ chỗ trống.");
                        }
                    }
                    ItemMapService.gI().removeItemMapAndSendClient(itemMap);
                    ((SoiHecQuyn) soihecQuyn).leaveMapNew();
                }
            }
        }
        if (!checkSoi) Service.gI().sendThongBao(pl, "Không tìm thấy Sói hẹc quyn");
        InventoryService.gI().sendItemBag(pl);
    }

    private void openCapsuleUI(Player pl) {
        pl.iDMark.setTypeChangeMap(ConstMap.CHANGE_CAPSULE);
        ChangeMapService.gI().openChangeMapTab(pl);
    }

    private static void open1798(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item1;
            if (Util.nextInt(100) < 50) {
                item1 = ItemService.gI().createNewItem((short) 1799);
                item1.itemOptions.add(new ItemOption(50, 18));
                item1.itemOptions.add(new ItemOption(77, 7));
                item1.itemOptions.add(new ItemOption(103, 7));
                item1.itemOptions.add(new ItemOption(5, 11));
            } else {
                item1 = ItemService.gI().createNewItem((short) 1800);
                item1.itemOptions.add(new ItemOption(50, 18));
                item1.itemOptions.add(new ItemOption(5, 8));
                item1.itemOptions.add(new ItemOption(14, 5));
            }
            item1.itemOptions.add(new ItemOption(30, 0));
            if (Util.nextInt(100) < 99) item1.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            InventoryService.gI().addItemBag(pl, item1);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1788(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            int r = Util.nextInt(100);
            if (r < 20) item2 = ItemService.gI().createNewItem((short) 1790);
            else if (r < 40) item2 = ItemService.gI().createNewItem((short) 1791);
            else if (r < 60) item2 = ItemService.gI().createNewItem((short) 1792);
            else if (r < 80) item2 = ItemService.gI().createNewItem((short) 1793);
            else item2 = ItemService.gI().createNewItem((short) 1794);
            int baseStat = (item2.template.id == 1790) ? 20 : (item2.template.id == 1791) ? 18 : (item2.template.id == 1792) ? 17 : (item2.template.id == 1793) ? 16 : 15;
            item2.itemOptions.add(new ItemOption(50, baseStat));
            item2.itemOptions.add(new ItemOption(77, baseStat));
            item2.itemOptions.add(new ItemOption(103, baseStat));
            item2.itemOptions.add(new ItemOption(108, (baseStat > 18) ? 12 : (baseStat == 18) ? 10 : 5));
            item2.itemOptions.add(new ItemOption(94, (baseStat > 18) ? 12 : (baseStat == 18) ? 10 : 5));
            item2.itemOptions.add(new ItemOption(30, 0));
            if (Util.nextInt(100) < 99) item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open627(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 7) {
            short baseCaiTrang = 1470;
            short genderOffset = (short) ((pl.gender == 0) ? -1 : ((pl.gender == 2) ? 1 : 0));
            Item ao = ItemService.gI().createNewItem((short) (1 + genderOffset));
            Item quan = ItemService.gI().createNewItem((short) (7 + genderOffset));
            Item gang = ItemService.gI().createNewItem((short) (22 + genderOffset));
            Item giay = ItemService.gI().createNewItem((short) (28 + genderOffset));
            Item nhan = ItemService.gI().createNewItem((short) 12);
            Item caiTrang = ItemService.gI().createNewItem((short) (baseCaiTrang + genderOffset));
            ao.itemOptions.add(new ItemOption(47, 3));
            quan.itemOptions.add(new ItemOption(6, 30));
            gang.itemOptions.add(new ItemOption(0, 4));
            giay.itemOptions.add(new ItemOption(7, 10));
            nhan.itemOptions.add(new ItemOption(14, 1));
            ao.itemOptions.add(new ItemOption(107, 3));
            quan.itemOptions.add(new ItemOption(107, 3));
            gang.itemOptions.add(new ItemOption(107, 3));
            giay.itemOptions.add(new ItemOption(107, 3));
            nhan.itemOptions.add(new ItemOption(107, 3));
            caiTrang.itemOptions.add(new ItemOption(50, 25));
            caiTrang.itemOptions.add(new ItemOption(77, 25));
            caiTrang.itemOptions.add(new ItemOption(103, 25));
            caiTrang.itemOptions.add(new ItemOption(101, 100));
            caiTrang.itemOptions.add(new ItemOption(93, 2));
            InventoryService.gI().addItemBag(pl, ao);
            InventoryService.gI().addItemBag(pl, quan);
            InventoryService.gI().addItemBag(pl, gang);
            InventoryService.gI().addItemBag(pl, giay);
            InventoryService.gI().addItemBag(pl, nhan);
            InventoryService.gI().addItemBag(pl, caiTrang);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            CombineService.gI().sendEffectOpenItem(pl, item.template.iconID, caiTrang.template.iconID);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1786(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            int rand = Util.nextInt(1000);
            if (rand < 15) {
                item2 = ItemService.gI().createNewItem((short) 1778);
                item2.itemOptions.add(new ItemOption(77, 22));
                item2.itemOptions.add(new ItemOption(50, 22));
                item2.itemOptions.add(new ItemOption(94, 8));
                item2.itemOptions.add(new ItemOption(5, 11));
                item2.itemOptions.add(new ItemOption(14, 8));
                item2.itemOptions.add(new ItemOption(106, 0));
            } else if (rand < 35) {
                item2 = ItemService.gI().createNewItem((short) 1779);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(103, 18));
                item2.itemOptions.add(new ItemOption(108, 10));
                item2.itemOptions.add(new ItemOption(94, 10));
            } else if (rand < 65) {
                item2 = ItemService.gI().createNewItem((short) 1780);
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(94, 5));
                item2.itemOptions.add(new ItemOption(108, 7));
            } else if (rand < 135) {
                item2 = ItemService.gI().createNewItem((short) 1781);
                item2.itemOptions.add(new ItemOption(77, 18));
                item2.itemOptions.add(new ItemOption(5, 7));
                item2.itemOptions.add(new ItemOption(14, 5));
            } else if (rand < 385) {
                item2 = ItemService.gI().createNewItem((short) 1782);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(94, 15));
                item2.itemOptions.add(new ItemOption(108, 7));
            } else if (rand < 685) {
                item2 = ItemService.gI().createNewItem((short) 1783);
                item2.itemOptions.add(new ItemOption(50, 18));
                item2.itemOptions.add(new ItemOption(5, 7));
                item2.itemOptions.add(new ItemOption(14, 5));
            } else if (rand < 985) {
                item2 = ItemService.gI().createNewItem((short) 1784);
                item2.itemOptions.add(new ItemOption(77, 16));
                item2.itemOptions.add(new ItemOption(50, 16));
                item2.itemOptions.add(new ItemOption(103, 16));
                item2.itemOptions.add(new ItemOption(236, 20));
            } else {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Service.gI().sendThongBao(pl, "Bạn không nhận được vật phẩm nào.");
                return;
            }
            item2.itemOptions.add(new ItemOption(30, 0));
            if (Util.nextInt(100) < 99) item2.itemOptions.add(new ItemOption(93, Util.nextInt(1, 7)));
            InventoryService.gI().addItemBag(pl, item2);
            Service.gI().sendThongBao(pl, "Bạn nhận được " + item2.template.name);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private static void open1787(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            Item it = ItemService.gI().createNewItem((short) 1785);
            InventoryService.gI().addItemBag(pl, it);
            Service.gI().sendThongBao(pl, "Bạn Nhận Được Quả Trứng Rồng Nhí");
            icon[1] = 15127;
            InventoryService.gI().subQuantityItemsBag(pl, item, 99);
            InventoryService.gI().sendItemBag(pl);
            CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void openRuongNgocRong(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int random = Util.nextInt(0, 100);
            int itemwhis;
            if (random < 85) {
                int[] itemList = {20, 19, 18, 17};
                itemwhis = itemList[Util.nextInt(0, itemList.length - 1)];
            } else if (random < 95) {
                itemwhis = 16;
            } else {
                itemwhis = Util.nextInt(14, 15);
            }
            Item it = ItemService.gI().createNewItem((short) itemwhis);
            Item item1561 = InventoryService.gI().findItem(pl.inventory.itemsBag, 1561);
            if (item1561 != null) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().subQuantityItemsBag(pl, item1561, 1);
                InventoryService.gI().addItemBag(pl, it);
                InventoryService.gI().sendItemBag(pl);
                Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + it.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Bạn không có chìa khoá vàng");
            }
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void openhopdiem(Player pl, Item hopDiem) {
        if (InventoryService.gI().getCountEmptyBag(pl) <= 0) {
            Service.gI().sendThongBao(pl, "Hành trang đã đầy");
            return;
        }
        Item queDiem = InventoryService.gI().findItem(pl.inventory.itemsBag, 2001);
        if (queDiem == null || queDiem.quantity < 1) {
            Service.gI().sendThongBao(pl, "Cần có que diêm mới quẹt được hộp điểm");
            return;
        }
        int random = Util.nextInt(0, 100);
        Item it;
        if (random < 35) {
            it = ItemService.gI().createNewItem((short) 2002);
            it.itemOptions.add(new ItemOption(93, 35));
            it.itemOptions.add(new ItemOption(30, 0));
        } else if (random < 50) {
            it = ItemService.gI().createNewItem((short) 1456);
            int percent = Util.nextInt(5, 15);
            it.itemOptions.add(new ItemOption(50, percent));
            it.itemOptions.add(new ItemOption(77, percent));
            it.itemOptions.add(new ItemOption(103, percent));
            it.itemOptions.add(new ItemOption(30, 0));
        } else if (random < 65) {
            it = ItemService.gI().createNewItem((short) (Util.isTrue(1, 2) ? 1452 : 1458));
            int percent = Util.nextInt(5, 10);
            it.itemOptions.add(new ItemOption(50, percent));
            it.itemOptions.add(new ItemOption(77, percent));
            it.itemOptions.add(new ItemOption(103, percent));
            it.itemOptions.add(new ItemOption(30, 0));
        } else if (random < 80) {
            it = ItemService.gI().createNewItem((short) 1453);
            it.quantity = 1;
        } else if (random < 90) {
            it = ItemService.gI().createNewItem((short) 1451);
            it.itemOptions.add(new ItemOption(106, 0));
            it.itemOptions.add(new ItemOption(80, 10));
            it.itemOptions.add(new ItemOption(30, 0));
        } else {
            it = ItemService.gI().createNewItem((short) 1466);
            it.itemOptions.add(new ItemOption(84, 0));
            it.itemOptions.add(new ItemOption(50, 10));
            it.itemOptions.add(new ItemOption(30, 0));
        }
        InventoryService.gI().subQuantityItemsBag(pl, hopDiem, 1);
        InventoryService.gI().subQuantityItemsBag(pl, queDiem, 1);
        InventoryService.gI().addItemBag(pl, it);
        InventoryService.gI().sendItemBag(pl);
        Service.gI().sendThongBao(pl, "Bạn nhận được " + it.template.name);
    }

    public void choseMapCapsule(Player pl, int index) {
        if (pl.idNRNM != -1) {
            Service.gI().sendThongBao(pl, "Không thể mang ngọc rồng này lên Phi thuyền");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        int zoneId = -1;
        if (index > pl.mapCapsule.size() - 1 || index < 0) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            Service.gI().hideWaitDialog(pl);
            return;
        }
        Zone zoneChose = pl.mapCapsule.get(index);
        if (zoneChose.getNumOfPlayers() > 25 || MapService.gI().isMapDoanhTrai(zoneChose.map.mapId) || MapService.gI().isMapMaBu(zoneChose.map.mapId) || MapService.gI().isMapHuyDiet(zoneChose.map.mapId)) {
            Service.gI().sendThongBao(pl, "Hiện tại không thể vào được khu!");
            return;
        }
        if (index != 0 || zoneChose.map.mapId == 21 || zoneChose.map.mapId == 22 || zoneChose.map.mapId == 23) {
            pl.mapBeforeCapsule = pl.zone;
        } else {
            zoneId = pl.mapBeforeCapsule != null ? pl.mapBeforeCapsule.zoneId : -1;
            pl.mapBeforeCapsule = null;
        }
        pl.changeMapVIP = true;
        ChangeMapService.gI().changeMapBySpaceShip(pl, pl.mapCapsule.get(index).map.mapId, zoneId, -1);
    }

    public void eatPea(Player player) {
        if (!Util.canDoWithTime(player.lastTimeEatPea, 1000)) return;
        player.lastTimeEatPea = System.currentTimeMillis();
        Item pea = null;
        for (Item item : player.inventory.itemsBag) {
            if (item.isNotNullItem() && item.template.type == 6) {
                pea = item;
                break;
            }
        }
        if (pea != null) {
            long hpKiHoiPhuc = 0;
            int lvPea = Integer.parseInt(pea.template.name.substring(13));
            for (ItemOption io : pea.itemOptions) {
                if (io.optionTemplate.id == 2) {
                    hpKiHoiPhuc = io.param * 1000;
                    break;
                }
                if (io.optionTemplate.id == 48) {
                    hpKiHoiPhuc = io.param;
                    break;
                }
            }
            player.nPoint.setHp(Util.maxIntValue(player.nPoint.hp + hpKiHoiPhuc));
            player.nPoint.setMp(Util.maxIntValue(player.nPoint.mp + hpKiHoiPhuc));
            PlayerService.gI().sendInfoHpMp(player);
            Service.gI().sendInfoPlayerEatPea(player);
            if (player.pet != null && player.zone.equals(player.pet.zone) && !player.pet.isDie()) {
                int statima = 100 * lvPea;
                player.pet.nPoint.stamina += statima;
                if (player.pet.nPoint.stamina > player.pet.nPoint.maxStamina) player.pet.nPoint.stamina = player.pet.nPoint.maxStamina;
                player.pet.nPoint.setHp(Util.maxIntValue(player.pet.nPoint.hp + hpKiHoiPhuc));
                player.pet.nPoint.setMp(Util.maxIntValue(player.pet.nPoint.mp + hpKiHoiPhuc));
                Service.gI().sendInfoPlayerEatPea(player.pet);
                Service.gI().chatJustForMe(player, player.pet, "Cám ơn sư phụ");
            }
            InventoryService.gI().subQuantityItemsBag(player, pea, 1);
            InventoryService.gI().sendItemBag(player);
        }
    }

    private void upSkillPet(Player pl, Item item) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        try {
            switch (item.template.id) {
                case 402:
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 0)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    break;
                case 403:
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 1)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    break;
                case 404:
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 2)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    break;
                case 759:
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 3)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    break;
            }
        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void ItemManhGiay(Player pl, Item item) {
        if (pl.winSTT && !Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            Service.gI().sendThongBao(pl, "Hãy gặp thần mèo Karin để sử dụng");
            return;
        } else if (pl.winSTT && Util.isAfterMidnight(pl.lastTimeWinSTT)) {
            pl.winSTT = false;
            pl.callBossPocolo = false;
            pl.zoneSieuThanhThuy = null;
        }
        NpcService.gI().createMenuConMeo(pl, item.template.id, 564, "Đây chính là dấu hiệu riêng của...\nĐại Ma Vương Pôcôlô\nĐó là một tên quỷ dữ đội lốt người, một kẻ đại gian ác\ncó sức mạnh vô địch và lòng tham không đáy...\nĐối phó với hắn không phải dễ\nCon có chắc chắn muốn tìm hắn không?", "Đồng ý", "Từ chối");
    }

    private void ItemSieuThanThuy(Player pl, Item item) {
        long tnsm = 5_000_000;
        int n = (item.template.id == 727) ? 2 : 10;
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
        if (Util.isTrue(50, 100)) {
            Service.gI().sendThongBao(pl, "Bạn đã bị chết vì độc của thuốc tăng lực siêu thần thủy.");
            pl.setDie();
        } else {
            for (int i = 0; i < n; i++) Service.gI().addSMTN(pl, (byte) 2, tnsm, true);
        }
    }

    private void Cadicvip(Player pl, Item item) {
        try {
            short[] itemList = {1759, 1760, 1761, 1762, 1763, 1764};
            short selectedItemId = itemList[Util.nextInt(0, itemList.length - 1)];
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            int atk = 21, hp = 21, ki = 21, opt210 = 1;
            if (selectedItemId == 1760) { atk = 22; hp = 22; ki = 22; opt210 = 2; }
            else if (selectedItemId == 1761 || selectedItemId == 1762) { atk = 23; hp = 23; ki = 23; opt210 = 3; }
            else if (selectedItemId == 1763) { atk = 25; hp = 25; ki = 25; opt210 = 3; }
            else if (selectedItemId == 1764) { atk = 27; hp = 27; ki = 27; opt210 = 4; }
            selectedItem.itemOptions.add(new ItemOption(50, atk));
            selectedItem.itemOptions.add(new ItemOption(77, hp));
            selectedItem.itemOptions.add(new ItemOption(103, ki));
            selectedItem.itemOptions.add(new ItemOption(210, opt210));
            if (selectedItemId == 1761 || selectedItemId == 1762) selectedItem.itemOptions.add(new ItemOption(93, 30));
            else if (Util.nextInt(0, 100) < 99) selectedItem.itemOptions.add(new ItemOption(93, Util.nextInt(1, 14)));
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Cadicvip: " + e.getMessage());
        }
    }

    private void RuongRongThan(Player pl, Item item) {
        try {
            short[] itemList = {1895, 1902, 1903, 1904};
            short selectedItemId = itemList[Util.nextInt(0, itemList.length - 1)];
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            selectedItem.itemOptions.add(new ItemOption(50, Util.nextInt(10, 15)));
            selectedItem.itemOptions.add(new ItemOption(95, Util.nextInt(10, 15)));
            selectedItem.itemOptions.add(new ItemOption(85, 0));
            if (Util.nextInt(0, 100) < 99) selectedItem.itemOptions.add(new ItemOption(93, Util.nextInt(15, 30)));
            selectedItem.itemOptions.add(new ItemOption(30, 0));
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Cadicvip: " + e.getMessage());
        }
    }

    private void Gokudayvip(Player pl, Item item) {
        try {
            short[] itemList = {1588, 1589, 1590, 1593, 955};
            short selectedItemId = itemList[Util.nextInt(0, itemList.length - 1)];
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            int atk = 21, hp = 21, ki = 21, opt210 = 1;
            if (selectedItemId == 1589) { atk = 22; hp = 22; ki = 22; }
            else if (selectedItemId == 1590) { atk = 27; hp = 27; ki = 27; opt210 = 4; }
            else if (selectedItemId == 1593) { atk = 25; hp = 25; ki = 25; opt210 = Util.nextInt(3, 4); }
            else if (selectedItemId == 1595) { atk = 23; hp = 23; ki = 23; opt210 = Util.nextInt(2, 3); }
            selectedItem.itemOptions.add(new ItemOption(50, atk));
            selectedItem.itemOptions.add(new ItemOption(77, hp));
            selectedItem.itemOptions.add(new ItemOption(103, ki));
            selectedItem.itemOptions.add(new ItemOption(210, opt210));
            if (Util.nextInt(0, 100) < 99) selectedItem.itemOptions.add(new ItemOption(93, Util.nextInt(1, 14)));
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Gokudayvip: " + e.getMessage());
        }
    }

    private void hop2010(Player pl, Item item) {
        try {
            if (pl.thiepchucvip < 0) pl.thiepchucvip = 0;
            short[] itemListMain = {1961, 1602, 680, 819, 914, 977, 1041, 1042, 1208, 1209, 1210, 1235, 1567, 1476, 1557, 860, 1772};
            List<Short> itemListBonus = new ArrayList<>();
            itemListBonus.add((short) 956);
            for (short i = 1074; i <= 1083; i++) itemListBonus.add(i);
            for (short i = 1150; i <= 1154; i++) itemListBonus.add(i);
            for (short i = 381; i <= 385; i++) itemListBonus.add(i);
            int roll = Util.nextInt(0, 100);
            boolean isMain = roll < 10;
            if (roll >= 10 && roll < 70) {
                int goldAmount = Util.nextInt(100_000, 1_500_000);
                pl.inventory.gold += goldAmount;
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) pl.inventory.gold = Inventory.LIMIT_GOLD;
                Service.gI().sendMoney(pl);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Service.gI().sendThongBao(pl, "Bạn nhận được " + Util.numberFormat(goldAmount) + " vàng");
                if (goldAmount >= 1_000_000) Service.gI().sendThongBaoAllPlayer(pl.name + " vừa mở Thiệp Chúc VIP nhận " + Util.numberFormat(goldAmount) + " vàng");
                pl.hopqua2010 += 5;
                Service.gI().sendThongBao(pl, "Bạn được cộng 5 điểm Thiệp Chúc VIP (Tổng: " + pl.hopqua2010 + ")");
                return;
            }
            short selectedItemId = isMain ? itemListMain[Util.nextInt(0, itemListMain.length - 1)] : itemListBonus.get(Util.nextInt(0, itemListBonus.size() - 1));
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            if (isMain) {
                int random93 = Util.nextInt(1, 14);
                if (selectedItemId == 1961) {
                    selectedItem.itemOptions.add(new ItemOption(50, 24));
                    selectedItem.itemOptions.add(new ItemOption(77, 23));
                    selectedItem.itemOptions.add(new ItemOption(103, 29));
                    selectedItem.itemOptions.add(new ItemOption(95, 100));
                    selectedItem.itemOptions.add(new ItemOption(93, 15));
                    Service.gI().sendThongBaoAllPlayer(pl.name + " vừa nhận được cải trang hiếm " + selectedItem.template.name);
                } else {
                    selectedItem.itemOptions.add(new ItemOption(50, Util.nextInt(20, 28)));
                    selectedItem.itemOptions.add(new ItemOption(77, Util.nextInt(20, 26)));
                    selectedItem.itemOptions.add(new ItemOption(103, Util.nextInt(22, 29)));
                    selectedItem.itemOptions.add(new ItemOption(211, Util.nextInt(5, 9)));
                    selectedItem.itemOptions.add(new ItemOption(93, 15));
                }
            }
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
                pl.hopqua2010 += 5;
                Service.gI().sendThongBao(pl, "Bạn được cộng thêm 5 điểm Thiệp Chúc VIP (Tổng: " + pl.hopqua2010 + ")");
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm thiepchucvip: " + e.getMessage());
        }
    }

    private void thiepchucvip(Player pl, Item item) {
        try {
            short[] itemListMain = {1503, 1504, 1512, 1884, 1960, 1961};
            List<Short> itemListBonus = new ArrayList<>();
            itemListBonus.add((short) 956);
            for (short i = 1074; i <= 1083; i++) itemListBonus.add(i);
            for (short i = 1150; i <= 1154; i++) itemListBonus.add(i);
            for (short i = 381; i <= 385; i++) itemListBonus.add(i);
            int roll = Util.nextInt(0, 100);
            boolean isMain = roll < 10;
            if (roll >= 10 && roll < 70) {
                int goldAmount = Util.nextInt(100_000, 1_500_000);
                pl.inventory.gold += goldAmount;
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) pl.inventory.gold = Inventory.LIMIT_GOLD;
                Service.gI().sendMoney(pl);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                Service.gI().sendThongBao(pl, "Bạn nhận được " + Util.numberFormat(goldAmount) + " vàng!");
                if (goldAmount >= 1_000_000) Service.gI().sendThongBaoAllPlayer("" + pl.name + " vừa mở thiệp chúc VIP nhận " + Util.numberFormat(goldAmount) + " vàng!");
                return;
            }
            short selectedItemId = isMain ? itemListMain[Util.nextInt(0, itemListMain.length - 1)] : itemListBonus.get(Util.nextInt(0, itemListBonus.size() - 1));
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            if (isMain) {
                int random93 = Util.nextInt(1, 14);
                int atk = Util.nextInt(15, 21), hp = Util.nextInt(15, 21), ki = Util.nextInt(15, 21), opt210 = 1;
                if (selectedItemId == 1512) { atk = Util.nextInt(17, 23); hp = Util.nextInt(17, 23); ki = Util.nextInt(17, 23); opt210 = 4; }
                else if (selectedItemId == 1884) { atk = Util.nextInt(20, 24); hp = Util.nextInt(20, 24); ki = Util.nextInt(20, 24); opt210 = Util.nextInt(3, 4); }
                else if (selectedItemId == 1960) {
                    selectedItem.itemOptions.add(new ItemOption(50, 24));
                    selectedItem.itemOptions.add(new ItemOption(77, 23));
                    selectedItem.itemOptions.add(new ItemOption(103, 29));
                    selectedItem.itemOptions.add(new ItemOption(226, 21));
                    selectedItem.itemOptions.add(new ItemOption(14, 7));
                    selectedItem.itemOptions.add(new ItemOption(93, random93));
                    selectedItem.itemOptions.add(new ItemOption(93, 15));
                } else if (selectedItemId == 1961) {
                    selectedItem.itemOptions.add(new ItemOption(95, 14));
                    selectedItem.itemOptions.add(new ItemOption(236, 100));
                    selectedItem.itemOptions.add(new ItemOption(30, 0));
                    selectedItem.itemOptions.add(new ItemOption(93, random93));
                    selectedItem.itemOptions.add(new ItemOption(93, 15));
                }
                if (selectedItemId != 1960 && selectedItemId != 1961) {
                    selectedItem.itemOptions.add(new ItemOption(50, atk));
                    selectedItem.itemOptions.add(new ItemOption(77, hp));
                    selectedItem.itemOptions.add(new ItemOption(103, ki));
                    selectedItem.itemOptions.add(new ItemOption(210, opt210));
                }
                if (selectedItemId == 1960 || selectedItemId == 1961) Service.gI().sendThongBaoAllPlayer("" + pl.name + " vừa nhận được cải trang cực hiếm " + selectedItem.template.name + "!");
            }
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                pl.thiepchucvip += 5;
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm thiepchucvip: " + e.getMessage());
        }
    }

    private void open1873(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1866);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1868);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                item2.itemOptions.add(new ItemOption(162, 2));
                if (Util.isTrue(95, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                item2 = ItemService.gI().createNewItem((short) 1867);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                item2.itemOptions.add(new ItemOption(30, 0));
            }
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open1874(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1866);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1868);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                item2.itemOptions.add(new ItemOption(162, 2));
                if (Util.isTrue(95, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                item2 = ItemService.gI().createNewItem((short) 1867);
                item2.itemOptions.add(new ItemOption(50, 23));
                item2.itemOptions.add(new ItemOption(77, 23));
                item2.itemOptions.add(new ItemOption(103, 23));
                if (Util.isTrue(95, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7)));
                item2.itemOptions.add(new ItemOption(30, 0));
            }
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void open1875(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item item2;
            if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1865);
                item2.itemOptions.add(new ItemOption(0, 7500));
                item2.itemOptions.add(new ItemOption(6, 10000));
                item2.itemOptions.add(new ItemOption(7, 10000));
                if (Util.isTrue(50, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1868);
                item2.itemOptions.add(new ItemOption(50, 25));
                item2.itemOptions.add(new ItemOption(14, 25));
                item2.itemOptions.add(new ItemOption(160, 20));
                if (Util.isTrue(50, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else if (Util.isTrue(35, 100)) {
                item2 = ItemService.gI().createNewItem((short) 1866);
                item2.itemOptions.add(new ItemOption(5, 25));
                item2.itemOptions.add(new ItemOption(14, 15));
                item2.itemOptions.add(new ItemOption(94, 20));
                if (Util.isTrue(50, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                item2.itemOptions.add(new ItemOption(30, 0));
            } else {
                item2 = ItemService.gI().createNewItem((short) 1867);
                item2.itemOptions.add(new ItemOption(103, 25));
                item2.itemOptions.add(new ItemOption(77, 25));
                item2.itemOptions.add(new ItemOption(83, 0));
                if (Util.isTrue(50, 100)) item2.itemOptions.add(new ItemOption(93, Util.nextInt(7, 30)));
                item2.itemOptions.add(new ItemOption(30, 0));
            }
            InventoryService.gI().addItemBag(pl, item2);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        } else {
            Service.gI().sendThongBao(pl, "Hàng trang đã đầy");
        }
    }

    private void CapsuleTrangSucVIP(Player pl, Item item) {
        try {
            short[] itemList = {1100, 1700, 1669, 1344, 954, 1587, 1588, 1142, 1197, 1206, 1519, 1520, 1595, 1962, 1963};
            short selectedItemId = itemList[Util.nextInt(0, itemList.length - 1)];
            Item selectedItem = ItemService.gI().createNewItem(selectedItemId);
            int[] randomValues = getRandomPowerSet();
            selectedItem.itemOptions.add(new ItemOption(50, randomValues[0]));
            selectedItem.itemOptions.add(new ItemOption(77, randomValues[1]));
            selectedItem.itemOptions.add(new ItemOption(103, randomValues[2]));
            selectedItem.itemOptions.add(new ItemOption(210, Util.nextInt(1, 4)));
            if (Util.nextInt(0, 100) < 99) selectedItem.itemOptions.add(new ItemOption(93, Util.nextInt(1, 14)));
            if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().addItemBag(pl, selectedItem);
                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + selectedItem.template.name);
            } else {
                Service.gI().sendThongBao(pl, "Hành trang của bạn đã đầy, không thể nhận vật phẩm!");
            }
        } catch (Exception e) {
            Logger.error("Lỗi khi tạo vật phẩm Trang Sức VIP: " + e.getMessage());
        }
    }

    private int[] getRandomPowerSet() {
        int strong = Util.nextInt(9, 11);
        int mid = Util.nextInt(7, 9);
        int weak = Util.nextInt(5, 7);
        int[][] patterns = {{strong, weak, weak}, {weak, strong, weak}, {weak, weak, strong}, {strong, mid, weak}, {mid, strong, weak}, {weak, mid, strong}};
        return patterns[Util.nextInt(0, patterns.length)];
    }

    private void Hopdothanlinh(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất", "Set namec", "Set xayda", "Từ chổi");
    }

    private void Hopdovaitho(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất", "Set namec", "Set xayda", "Từ chổi");
    }

    private void Hopdohuydiet(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất", "Set namec", "Set xayda", "Từ chổi");
    }

    public void UseCard(Player pl, Item item) {
        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(c -> c.Id == item.template.id).findFirst().orElse(null);
        if (radarTemplate == null) return;
        if (radarTemplate.Require != -1) {
            RadarCard radarRequireTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(r -> r.Id == radarTemplate.Require).findFirst().orElse(null);
            if (radarRequireTemplate == null) return;
            Card cardRequire = pl.Cards.stream().filter(r -> r.Id == radarRequireTemplate.Id).findFirst().orElse(null);
            if (cardRequire == null || cardRequire.Level < radarTemplate.RequireLevel) {
                Service.gI().sendThongBao(pl, "Bạn cần sưu tầm " + radarRequireTemplate.Name + " ở cấp độ " + radarTemplate.RequireLevel + " mới có thể sử dụng thẻ này");
                return;
            }
        }
        Card card = pl.Cards.stream().filter(r -> r.Id == item.template.id).findFirst().orElse(null);
        if (card == null) {
            Card newCard = new Card(item.template.id, (byte) 1, radarTemplate.Max, (byte) -1, radarTemplate.Options);
            if (pl.Cards.add(newCard)) {
                RadarService.gI().RadarSetAmount(pl, newCard.Id, newCard.Amount, newCard.MaxAmount);
                RadarService.gI().RadarSetLevel(pl, newCard.Id, newCard.Level);
                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                InventoryService.gI().sendItemBag(pl);
            }
        } else {
            if (card.Level >= 2) {
                Service.gI().sendThongBao(pl, "Thẻ này đã đạt cấp tối đa");
                return;
            }
            card.Amount++;
            if (card.Amount >= card.MaxAmount) {
                card.Amount = 0;
                if (card.Level == -1) card.Level = 1;
                else card.Level++;
                Service.gI().point(pl);
            }
            RadarService.gI().RadarSetAmount(pl, card.Id, card.Amount, card.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, card.Id, card.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        }
    }
    
    private void openfa(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            Item ruongkhobau = InventoryService.gI().findItemBag(pl, 1569);
            if (ruongkhobau != null) {
                Item itemReward = null;
                RandomCollection<Integer> rd = new RandomCollection<>();

                rd.add(60, 4);
                rd.add(30, 5);

                rd.add(10, 3);
                rd.add(1, 2);
                rd.add(1, 1);
                int color = rd.next();
                if (color == 4) {
                    short[] temp = {1150, 1151, 1152, 1153};
                    byte index = (byte) Util.nextInt(0, temp.length - 1);
                    Item it = ItemService.gI().createNewItem(temp[index]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, it);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + it.template.name);
                } else if (color == 2) {
                    int[] set2 = {555, 556, 563, 557, 558, 565, 559, 567, 560};
                    itemReward = ItemService.gI().createNewItem((short) set2[Util.nextInt(0, set2.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type, itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward, new RewardService.RatioStar[]{new RewardService.RatioStar((byte) 1, 1, 2), new RewardService.RatioStar((byte) 2, 1, 3), new RewardService.RatioStar((byte) 3, 1, 4), new RewardService.RatioStar((byte) 4, 1, 5),});
                    InventoryService.gI().addItemBag(pl, itemReward);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendMoney(pl);
                } else if (color == 3) {
                    int[] ngocrong = new int[]{16, 17};
                    int randomtrungpet = new Random().nextInt(ngocrong.length);
                    Item pet = ItemService.gI().createNewItem((short) ngocrong[randomtrungpet]);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, pet);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + pet.template.name);
                } else if (color == 1) {
                    int[] set1 = {562, 564, 566, 561};
                    itemReward = ItemService.gI().createNewItem((short) set1[Util.nextInt(0, set1.length - 1)]);
                    RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type, itemReward.itemOptions);
                    RewardService.gI().initStarOption(itemReward, new RewardService.RatioStar[]{new RewardService.RatioStar((byte) 1, 1, 2), new RewardService.RatioStar((byte) 2, 1, 3), new RewardService.RatioStar((byte) 3, 1, 4), new RewardService.RatioStar((byte) 4, 1, 5),});
                    InventoryService.gI().addItemBag(pl, itemReward);
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendMoney(pl);
                } else if (color == 5) {
                    int[] itdeolung = new int[]{1578, 1563, 1603};
                    int randomIMDEOLUNG = new Random().nextInt(itdeolung.length);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    Item Itdeolung = ItemService.gI().createNewItem((short) itdeolung[randomIMDEOLUNG]);
                    Itdeolung.itemOptions.add(new ItemOption(50, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(77, Util.nextInt(5, 12)));
                    Itdeolung.itemOptions.add(new ItemOption(103, Util.nextInt(5, 12)));
                    if (Util.isTrue(950, 1000)) {
                        Itdeolung.itemOptions.add(new ItemOption(93, Util.nextInt(1, 3)));
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, ruongkhobau, 1);
                    InventoryService.gI().addItemBag(pl, Itdeolung);
                    InventoryService.gI().sendItemBag(pl);
                    Service.gI().sendThongBao(pl, "Chúc mừng bạn nhận được " + Itdeolung.template.name);
                }
            }

        } else {
            Service.gI().sendThongBao(pl, "Hãy chừa 1 ô trống để mở.");
        }
    }

    private void ItemSKH(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Hãy chọn 1 trong các trang bị", "Áo", "Quần", "Găng", "Giày", "Rađa");
    }
}