package services.func;

import boss.BossManager;
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
import item.Item.ItemOption;
import java.io.IOException;
import map.Zone;
import player.Inventory;
import services.*;
import player.Player;
import skill.Skill;
import network.Message;
import utils.SkillUtil;
import utils.TimeUtil;
import utils.Util;
import server.io.MySession;
import utils.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import jdbc.daos.PlayerDAO;
import map.ItemMap;
import server.Manager;
import task.Badges.BadgesTaskService;
import task.Pet.PetTaskService;

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
                                    msg.writer().writeUTF("Bạn chắc chắn học "
                                            + player.inventory.itemsBag.get(index).template.name + "?");
                                    player.sendMessage(msg);
                                } else if (item.template.id == 570) {
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn mở\n"
                                            + player.inventory.itemsBag.get(index).template.name + " ?");
                                    player.sendMessage(msg);
                                } else if (item.template.type == 22) {
                                    if (player.zone.items.stream()
                                            .filter(it -> it != null && it.itemTemplate.type == 22).count() > 2) {
                                        Service.gI().sendThongBaoOK(player, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                        return;
                                    }
                                    msg = new Message(-43);
                                    msg.writer().writeByte(type);
                                    msg.writer().writeByte(where);
                                    msg.writer().writeByte(index);
                                    msg.writer().writeUTF("Bạn chắc muốn dùng\n"
                                            + player.inventory.itemsBag.get(index).template.name + " ?");
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

    private void useItem(Player pl, Item item, int indexBag) {
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 570) {
                int time = (int) TimeUtil.diffDate(new Date(), new Date(item.createTime), TimeUtil.DAY);
                if (time == 0) {
                    Service.gI().sendThongBao(pl, "Hãy chờ đến ngày mai");
                } else {
                    openRuongGo(pl, item);
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
                    case 33: // card
                        UseCard(pl, item);
                        break;
                    case 7: // sách học, nâng skill
                        learnSkill(pl, item);
                        break;
                    case 6: // đậu thần
                        this.eatPea(pl);
                        break;
                    case 12: // ngọc rồng các loại
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
                    case 72: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendPetFollow(pl, (short) (item.template.iconID - 1));
                        break;
                    }
                    case 73: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        break;
                    }
                    case 68: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendEffAllPlayerMapToMe(pl);
                        break;
                    }
                    case 69: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        Service.gI().sendEffPlayer(pl);
                        break;
                    }
                    case 97: {
                        InventoryService.gI().itemBagToBody(pl, indexBag);
                        InventoryService.gI().sendItemBag(pl);
                        break;
                    }
                    case 98: {
                        PetService.gI().createPet(pl, (byte) item.template.part);
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                        InventoryService.gI().sendItemBag(pl);
                        break;
                    }
                    default:
                        switch (item.template.id) {
                            case 992: // Nhan thoi khong
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
                            case 211: // nho tím
                            case 212: // nho xanh
                                eatGrapes(pl, item);
                                break;
                            case 342:
                            case 343:
                            case 344:
                            case 345:
                                if (pl.zone.items.stream().filter(it -> it != null && it.itemTemplate.type == 22)
                                        .count() < 3) {
                                    Service.gI().dropSatellite(pl, item, pl.zone, pl.location.x, pl.location.y);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                } else {
                                    Service.gI().sendThongBaoOK(pl, "Mỗi map chỉ đặt được 3 Vệ Tinh");
                                }
                                break;
                            case 380: // cskb
                                openCSKB(pl, item);
                                break;
                            case 1640: // cskb
                                openLinhThu(pl, item);
                                break;
                            case 1045: // đuôi khỉ
                                EffectSkillService.gI().sendEffectMonkey(pl);
                                pl.effectSkill.isMonkey = true;
                                pl.effectSkill.timeMonkey = 300 * 1000;
                                // vậy
                                pl.effectSkill.lastTimeUpMonkey = System.currentTimeMillis();
                                pl.effectSkill.levelMonkey = 7;
                                pl.nPoint.setHp(Util.toIntOrLong(pl.nPoint.hp * 2));
                                Service.gI().Send_Caitrang(pl);
                                if (!pl.isDeTu) {
                                    PlayerService.gI().sendInfoHpMp(pl);
                                }
                                Service.gI().point(pl);
                                Service.gI().Send_Info_NV(pl);
                                Service.gI().sendInfoPlayerEatPea(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1855: // đuôi khỉ
                                if (pl.pet == null) {
                                    Service.gI().sendThongBaoOK(pl, "Cần sở hữu Đệ tử VIP");
                                    return;
                                }
                                // if(pl.pet.typePet !=2&&pl.pet.typePet!=3&&pl.pet.typePet!=4){
                                // Service.gI().sendThongBaoOK(pl, "Cần sở hữu Đệ tử VIP");
                                // return;
                                // }

                                Service.gI().chatJustForMe(pl, pl.pet, " Khỉ Lên !!!");
                                // ChangeMapService.gI().exitMap(pl.pet);
                                EffectSkillService.gI().sendEffectMonkey(pl.pet);
                                pl.pet.effectSkill.isMonkey = true;
                                ItemTimeService.gI().sendItemTime(pl, 14247, 300);
                                pl.pet.effectSkill.timeMonkey = 300 * 1000;
                                pl.pet.effectSkill.lastTimeUpMonkey = System.currentTimeMillis();
                                pl.pet.effectSkill.levelMonkey = 7;
                                pl.pet.nPoint.setHp(Util.toIntOrLong(pl.pet.nPoint.hp * 2));
                                Service.gI().Send_Caitrang(pl.pet);
                                if (!pl.isDeTu) {
                                    PlayerService.gI().sendInfoHpMp(pl.pet);
                                }
                                Service.gI().point(pl.pet);
                                Service.gI().Send_Info_NV(pl.pet);
                                Service.gI().sendInfoPlayerEatPea(pl.pet);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1781: // hoa than
                                EffectSkillService.gI().sendEffectMonkey(pl);

                                pl.effectSkill.isMucoi = true;
                                pl.effectSkill.timeMonkey = 1200 * 250;
                                pl.effectSkill.lastTimeUpMonkey = System.currentTimeMillis();
                                pl.nPoint.setHp(Util.toIntOrLong(pl.nPoint.hp * 2));
                                Message msg;
                                msg = new Message(-90);
                                try {
                                    msg.writer().writeByte(1);// check type
                                    msg.writer().writeInt((int) pl.id); // id player
                                    msg.writer().writeShort(1610);// set head
                                    msg.writer().writeShort(1611);// setbody
                                    msg.writer().writeShort(1612);// set leg
                                    msg.writer().writeByte(pl.effectSkill.isMonkey ? 1 : 0);// set khỉ
                                    Service.gI().sendMessAllPlayerInMap(pl, msg);
                                    msg.cleanup();
                                } catch (IOException ex) {
                                    java.util.logging.Logger.getLogger(UseItem.class.getName()).log(Level.SEVERE, null,
                                            ex);
                                }

                                if (!pl.isDeTu) {
                                    PlayerService.gI().sendInfoHpMp(pl);
                                }
                                Service.gI().point(pl);
                                Service.gI().Send_Info_NV(pl);
                                Service.gI().sendInfoPlayerEatPea(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1173: {
                                int ct = Util.nextInt(1067, 1070);
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct, 99);

                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được x99  " + caiTrangHaiTac.template.name);
                            }
                                break;
                            case 1819: // nhaaxn thuat
                                if (!pl.nPoint.isObitold) {
                                    Service.gI().sendThongBaoOK(pl, "Cần mặc cải trang obito để sử dụng");
                                } else {
                                    EffectSkillService.gI().sendEffectMonkey(pl);

                                    pl.effectSkill.isObito = true;
                                    pl.effectSkill.timeMonkey = 1200 * 250;
                                    pl.effectSkill.lastTimeUpMonkey = System.currentTimeMillis();
                                    pl.nPoint.setHp(Util.toIntOrLong(pl.nPoint.hp * 2));
                                    Message obt;
                                    obt = new Message(-90);
                                    try {
                                        obt.writer().writeByte(1);// check type
                                        obt.writer().writeInt((int) pl.id); // id player
                                        obt.writer().writeShort(1649);// set head
                                        obt.writer().writeShort(1650);// setbody
                                        obt.writer().writeShort(1651);// set leg
                                        obt.writer().writeByte(pl.effectSkill.isMonkey ? 1 : 0);// set khỉ
                                        Service.gI().sendMessAllPlayerInMap(pl, obt);
                                        obt.cleanup();
                                    } catch (IOException ex) {
                                        java.util.logging.Logger.getLogger(UseItem.class.getName()).log(Level.SEVERE,
                                                null, ex);
                                    }

                                    if (!pl.isDeTu) {
                                        PlayerService.gI().sendInfoHpMp(pl);
                                    }
                                    Service.gI().point(pl);
                                    Service.gI().Send_Info_NV(pl);
                                    Service.gI().sendInfoPlayerEatPea(pl);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                }
                                break;

                            case 1635:
                            case 1518:

                            case 1517:

                            case 381: // cuồng nộ
                            case 382: // bổ huyết
                            case 383: // bổ khí
                            case 384: // giáp xên
                            case 385: // ẩn danh
                            case 379: // máy dò capsule
                            case 638: // commeson
                            case 2075: // rocket
                            case 2160: // Nồi cơm điện
                            case 579:

                            case 663: // bánh pudding
                            case 664: // xúc xíc
                            case 665: // kem dâu
                            case 666: // mì ly
                            case 667: // sushi
                            case 1099:
                            case 1100:
                            case 1101:
                            case 1102:
                            case 1103:
                            case 1150: // cn2
                            case 1151:// bk2
                            case 1152: // bh2
                            case 1153: // gx2
                            case 1154: // anhdanh2
                            case 1628:
                            case 764:
                            case 1731:
                            case 1727:
                            case 1728:
                            case 1729:
                            case 1730:
                                useItemTime(pl, item);
                                break;
                            case 1560:
                                openRuongNgocRong(pl, item);
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
                            case 752:
                            case 753:
                            case 899:
                            case 900:
                            case 902:
                            case 903:
                            case 1016:
                                if (pl.itemTime.isEatMeal3) {
                                    Service.gI().sendThongBao(pl, "Chỉ được sử dụng 1 cái");
                                    break;
                                }
                                useItemTime(pl, item);
                                break;
                            case 521: // tdlt
                                useTDLT(pl, item);
                                break;
                            case 454: // bông tai
                                UseItem.gI().usePorata(pl);
                                break;
                            case 921: // bông tai
                                UseItem.gI().usePorata2(pl);
                                break;
                            case 1604: // bông tai
                                UseItem.gI().usePorata3(pl);
                                break;
                            case 1605: // bông tai
                                UseItem.gI().usePorataHacHoa(pl);
                                break;
                            case 193: // gói 10 viên capsule
                                openCapsuleUI(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 194: // capsule đặc biệt
                                openCapsuleUI(pl);
                                break;
                            case 401: // đổi đệ tử
                                changePet(pl, item);
                                break;
                            case 402: // sách nâng chiêu 1 đệ tử
                            case 403: // sách nâng chiêu 2 đệ tử
                            case 404: // sách nâng chiêu 3 đệ tử
                            case 759: // sách nâng chiêu 4 đệ tử
                                upSkillPet(pl, item);
                                break;
                            case 726:
                                UseItem.gI().ItemManhGiay(pl, item);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
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
                                Service.gI().sendThongBao(pl, "Bảo vệ trang bị không bị rớt cấp"); // đá bảo vệ
                                break;
                            case 2006:
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

                            case 1629:
                                handleOpenPetSkill(pl, 2, pet -> pet.openSkill3());
                                break;
                            case 1630:
                                handleOpenPetSkill(pl, 3, pet -> pet.openSkill4());
                                break;
                            case 1822: {
                                // if(pl.nPoint.isNaruto&&pl.nPoint.isMinato){
                                pl.itemTime.isUsept = true;
                                pl.itemTime.lastTimeUsept = System.currentTimeMillis();
                                SkillService.gI().startSkillClone(pl);
                                pl.clone.joinMapPlayer();
                                ItemTimeService.gI().sendItemTime(pl, 32346, 60);
                                // SkillService.gI().affterUseSkill(pl, pl.playerSkill.skillSelect.template.id);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);

                                // }else{
                                // Service.gI().sendThongBao(pl, "Cần sở hữu pet Minato và cải trang naruto để
                                // xử dụng!!!");
                                // }

                                break;
                            }

                            case 628: {
                                int ct = Util.nextInt(618, 626);
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(50, Util.nextInt(15, 30)));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(77, Util.nextInt(15, 30)));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(103, Util.nextInt(15, 30)));
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(149, 1));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl,
                                        "Bạn đã nhận được cải trang " + caiTrangHaiTac.template.name);
                            }
                                break;
                            case 1637:
                                Skill skill = null;
                                if (pl.pet == null) {
                                    Service.gI().sendThongBao(pl, "Làm gì có đệ? ");
                                    return;
                                }

                                skill = SkillUtil.createSkill(Skill.LIEN_HOAN, 1);
                                pl.pet.playerSkill.skills.set(0, skill);
                                Service.gI().sendThongBao(pl, "Đổi thành công Skill Liên Hoàn cho đệ ");
                                Service.gI().chatJustForMe(pl, pl.pet, "Đù ngon nha, pem quả này sướng!!...");
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1639:
                                if (pl.pet == null) {
                                    Service.gI().sendThongBao(pl, "Làm gì có đệ? ");
                                    return;
                                }
                                if (pl.pet.nPoint.power < 150_000_000) {
                                    Service.gI().sendThongBao(pl, "Đệ Cần phải có skill 2 ");
                                    return;
                                }
                                skill = SkillUtil.createSkill(Skill.KAMEJOKO, 1);
                                pl.pet.playerSkill.skills.set(1, skill);
                                Service.gI().sendThongBao(pl, "Đổi thành công Skill Kamejoko cho đệ ");
                                Service.gI().chatJustForMe(pl, pl.pet, "Omg em ơi chưởng đã quá!!...");
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                break;
                            case 1859:
                            case 1788:
                            case 1789:
                            case 1790:
                            case 1791:
                            case 1792:
                            case 1793:
                            case 1794:
                            case 1795:
                                useCoinItem(pl, item, getCoinAmount(item.template.id));
                                break;
                            case 1825: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// thạch anh tím
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Random random = new Random();
                                int rand = random.nextInt(1000);
                                int ct = 0;
                                if (rand > 1) {
                                    ct = 1827;
                                } else {
                                    ct = 1826;
                                }
                                Item tien = ItemService.gI().createNewItem((short) ct);
                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + tien.template.name);
                            }

                                break;
                            case 1857: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// thạch anh tím
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Item tien = ItemService.gI().createNewItem((short) 1799);
                                tien.itemOptions.add(new Item.ItemOption(50, 25));
                                tien.itemOptions.add(new Item.ItemOption(77, 35));
                                tien.itemOptions.add(new Item.ItemOption(103, 25));
                                tien.itemOptions.add(new Item.ItemOption(161, 20));
                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + tien.template.name);

                            }
                                break;
                            case 456: {
                                if (pl.nPoint.isxinbato) {
                                    Item bn = InventoryService.gI().findItemBag(pl, 456);

                                    if (bn == null || bn.quantity < 99) {
                                        Service.gI().sendThongBao(pl, "Cần 99 bình nước!");
                                        return;
                                    } else {
                                        Random random = new Random();
                                        int rand = random.nextInt(100);
                                        int it = 0;
                                        if (rand >= 0 && rand <= 50) {
                                            it = Util.nextInt(441, 447);
                                        } else if (rand > 50 && rand <= 70) {
                                            it = Util.nextInt(16, 20);
                                        } else if (rand > 80 && rand <= 90) {
                                            it = 15;
                                        } else {
                                            it = 459;
                                        }
                                        Item qua = ItemService.gI().createNewItem((short) it);
                                        if (it == 459) {
                                            qua.itemOptions.add(new Item.ItemOption(112, 80));
                                        }
                                        pl.nPoint.diexinbato = true;
                                        BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.NUOC_ANH_BAO, 1);
                                        InventoryService.gI().addItemBag(pl, qua);
                                        InventoryService.gI().subQuantityItemsBag(pl, item, 99);
                                        InventoryService.gI().sendItemBag(pl);
                                        Service.gI().sendThongBao(pl, "Bạn đã nhận được " + qua.template.name);
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Cần đến gần Xinbato để sự dúng");
                                }
                            }
                                break;
                            case 460:
                                if (pl.nPoint.xoihecquen) {
                                    // Kiểm tra nếu đã sử dụng 1 lần rồi thì không cho dùng tiếp
                                    if (pl.nPoint.usedXoihecquen) {
                                        Service.gI().sendThongBao(pl,
                                                "Bạn đã sử dụng xương trước đó! Không thể dùng tiếp.");
                                        return;
                                    }

                                    Item bn = InventoryService.gI().findItemBag(pl, 460);
                                    if (bn == null || bn.quantity < 1) {
                                        Service.gI().sendThongBao(pl, "Không tìm thấy xương trong hành trang");
                                        return;
                                    }

                                    Random random = new Random();
                                    int rand = random.nextInt(100);
                                    int it = 0;
                                    if (rand >= 0 && rand <= 50) {
                                        it = Util.nextInt(441, 447);
                                    } else if (rand > 50 && rand <= 70) {
                                        it = Util.nextInt(16, 20);
                                    } else if (rand > 80 && rand <= 96) {
                                        it = 15;
                                    } else {
                                        it = 459;
                                    }

                                    Item qua = ItemService.gI().createNewItem((short) it);
                                    if (it == 459) {
                                        qua.itemOptions.add(new Item.ItemOption(112, 80));
                                    }

                                    // Đánh dấu đã sử dụng
                                    pl.nPoint.diexoihecquen = true;
                                    pl.nPoint.usedXoihecquen = true; //

                                    BadgesTaskService.updateCountBagesTask(pl, ConstTaskBadges.KE_THAO_TUNG_SOI, 1);
                                    InventoryService.gI().addItemBag(pl, qua);
                                    InventoryService.gI().subQuantityItemsBag(pl, bn, 1);
                                    InventoryService.gI().sendItemBag(pl);
                                    Service.gI().sendThongBao(pl, "Bạn đã nhận được " + qua.template.name);
                                } else {
                                    Service.gI().sendThongBao(pl, "Cần đứng gần SÓI HẸC QUYN");
                                }
                                break;

                            case 1115: {
                                BossManager.gI().showListBossNomar(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);

                            }
                                break;
                            case 1881: {
                                int rank = Manager.getRankByNamelc(pl.name);
                                if (rank > 20 || rank < 0) {
                                    rank = 99;
                                }
                                Service.gI().sendThongBaoLucChien(pl, "|0|Rank sever hiện tại: " + rank + "\n"
                                        + String.format("%-8s %-10s\n", "|3|Lực Chiến:",
                                                Manager.formatNumber(pl.lucchien))
                                        + String.format("%-8s %-10s\n", "|5|Power:",
                                                Manager.formatNumber(pl.nPoint.power))
                                        + String.format("%-8s %-10s\n", "|5|Dame:",
                                                Manager.formatNumber(pl.nPoint.dame))
                                        + String.format("%-8s %-13s\n", "|3|HP:", Manager.formatNumber(pl.nPoint.hp))
                                        + String.format("%-8s %-13s\n", "|5|MP:", Manager.formatNumber(pl.nPoint.mp))
                                        + String.format("%-8s %-18s\n", "|3|Crit:",
                                                Manager.formatNumber(pl.nPoint.crit))
                                        + String.format("%-8s %-15s\n", "|5|Def:", Manager.formatNumber(pl.nPoint.def))
                                        + String.format("%-8s %-10s\n", "|3|Hút HP:",
                                                Manager.formatNumber(pl.nPoint.tlHutHp) + "%")
                                        + String.format("%-8s %-10s\n", "|5|Hút MP:",
                                                Manager.formatNumber(pl.nPoint.tlHutMp) + "%")
                                        + String.format("%-8s %-10s\n", "|3|Dame Crit:",
                                                Manager.formatNumber(pl.nPoint.tlSDCM) + "%"));

                            }
                                break;
                            case 1860: {
                                if (pl.pet == null) {
                                    Service.gI().sendThongBao(pl, "Cần Sở Hữu pet Để xem  ");
                                    return;
                                }
                                int id = pl.PetTask.id;
                                if (id <= 0) {
                                    id = 1;
                                    pl.PetTask.id = 1;
                                    PetTaskService.UpdateTask(pl, 1);
                                }
                                Service.gI().sendThongBaoFromAdmin(pl,
                                        "|0|Làm Nhiệm vụ giúp nhận Đá nâng Đệ để nâng cấp đệ\n"
                                                + "\b|1| Nếu là Nhiệm vụ tiêu diệt boss thì vào lúc 18-19H hàng ngày Chỉ Đệ tử mới có thể gây sát thương lên boss\n"
                                                + "\b|5|Nhiệm vụ hiện tại: " + id + ": " + PetTaskService.nameTask(id)
                                                + "\n"
                                                + " \b|3|Tiến Độ: " + pl.PetTask.count + "/" + pl.PetTask.countMax
                                                + " (" + pl.PetTask.getPercentProcess() + "%)\n"
                                                + "Hoàn Thành Nhận: " + PetTaskService.Reward(id)
                                                + " Đá Nâng Cấp Đệ\n");

                            }
                                break;
                            case 1191: {
                                Service.gI().sendTitle(pl, 207);
                            }
                                break;
                            case 397: {
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                pl.point_lucky_dapdo += 30;
                                Service.gI().sendThongBao(pl,
                                        "Tăng x2 tỷ lệ đập đồ 30 lần, số lần hiện tại + " + pl.point_lucky_dapdo);

                            }
                                break;
                            case 718: {
                                Input.gI().createTangNgoc(pl);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            }
                                break;
                            case 1796: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// thạch anh tím
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Random random = new Random();
                                int rand = random.nextInt(100);
                                int ct = 0;
                                if (rand < 50) {
                                    ct = 1788;
                                } else if (rand < 80) {
                                    ct = 1789;
                                } else if (rand < 95) {
                                    ct = 1790;
                                } else {
                                    ct = 1791;
                                }
                                Item tien = ItemService.gI().createNewItem((short) ct);
                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                InventoryService.gI().sendItemBag(pl);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + tien.template.name);
                            }
                                break;
                            case 457:
                                Input.gI().createFormBanSLL(pl);
                                break;
                            case 1880: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// thạch anh tím
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                short[] icon = new short[2];
                                icon[0] = item.template.iconID;
                                Item lt = ItemService.gI().createNewItem((short) Util.nextInt(1870, 1879), 1);
                                lt.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                                lt.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
                                lt.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                                lt.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 10)));

                                lt.itemOptions.add(new Item.ItemOption(72, 6));
                                icon[1] = lt.template.iconID;
                                if (Util.isTrue(30, 100)) {
                                    lt.itemOptions
                                            .add(new Item.ItemOption(Util.nextInt(156, 158), Util.nextInt(5, 15)));
                                }
                                if (Util.isTrue(90, 100)) {
                                    lt.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 3)));
                                }
                                CombineService.gI().sendEffectOpenItem(pl, icon[0], icon[1]);
                                InventoryService.gI().addItemBag(pl, lt);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + lt.template.name);
                            }
                                break;
                            case 699: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// thạch anh tím
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Random random = new Random();
                                int rand = random.nextInt(100);
                                int ct = 0;
                                int sl = 1;
                                if (rand >= 90) {
                                    ct = 457;// 2k
                                    sl = Util.nextInt(1, 5);
                                } else if (rand >= 80) {
                                    ct = 720;// 5k
                                    Util.nextInt(1, 3);
                                } else if (rand >= 70) {
                                    ct = 1398;// 10k
                                    Util.nextInt(1, 5);
                                } else if (rand >= 59) {
                                    ct = 16;// 20k
                                } else if (rand > 190) {
                                    ct = Util.nextInt(663, 667);
                                    sl = Util.nextInt(1, 5);
                                } else if (rand >= 10) {
                                    ct = 15;

                                } else {
                                    ct = 628;
                                }
                                Item tien = ItemService.gI().createNewItem((short) ct, sl);
                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được x" + sl + " " + tien.template.name);
                            }
                                break;
                            case 700: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// thạch anh tím
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Random random = new Random();
                                int rand = random.nextInt(100);
                                int ct = 0;
                                int sl = 1;
                                if (rand >= 90) {
                                    ct = 16;// 2k
                                    sl = Util.nextInt(1, 5);
                                } else if (rand >= 80) {
                                    ct = 720;// 5k
                                    Util.nextInt(1, 15);
                                } else if (rand >= 70) {
                                    ct = 1398;// 10k
                                    Util.nextInt(1, 25);
                                } else if (rand >= 59) {
                                    ct = 457;// 20k
                                    sl = Util.nextInt(1, 25);
                                } else if (rand > 15) {
                                    ct = Util.nextInt(663, 667);
                                    sl = Util.nextInt(1, 25);
                                } else if (rand >= 10) {
                                    ct = 14;

                                } else {
                                    ct = 1430;
                                }
                                Item tien = ItemService.gI().createNewItem((short) ct, sl);
                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được x" + sl + " " + tien.template.name);
                            }
                                break;
                            case 1440: {
                                int ct = Util.nextInt(441, 447);
                                Item caiTrangHaiTac = ItemService.gI().createNewItem((short) ct);
                                caiTrangHaiTac.itemOptions.add(new Item.ItemOption(93, 30));
                                InventoryService.gI().addItemBag(pl, caiTrangHaiTac);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + caiTrangHaiTac.template.name);
                            }
                                break;
                            case 1430: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// capsulebac
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Random random = new Random();
                                int rand = random.nextInt(100);
                                int id = Util.nextInt(1426, 1429);

                                Item tien = ItemService.gI().createNewItem((short) id);
                                if (rand < 70) {
                                    if (Util.isTrue(80, 100)) {
                                        tien.itemOptions.add(new Item.ItemOption(50, Util.nextInt(1, 8)));
                                        tien.itemOptions.add(new Item.ItemOption(77, Util.nextInt(1, 8)));
                                        tien.itemOptions.add(new Item.ItemOption(103, Util.nextInt(1, 8)));
                                        tien.itemOptions.add(new Item.ItemOption(160, Util.nextInt(1, 20)));
                                        tien.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 4)));
                                        tien.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 5)));
                                    } else {
                                        tien.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                                        tien.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
                                        tien.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                                        tien.itemOptions.add(new Item.ItemOption(160, Util.nextInt(10, 40)));
                                        tien.itemOptions.add(new Item.ItemOption(5, Util.nextInt(2, 10)));
                                        tien.itemOptions.add(new Item.ItemOption(93, Util.nextInt(1, 5)));
                                    }
                                } else {
                                    if (Util.isTrue(80, 100)) {
                                        tien.itemOptions.add(new Item.ItemOption(50, Util.nextInt(1, 8)));
                                        tien.itemOptions.add(new Item.ItemOption(77, Util.nextInt(1, 8)));
                                        tien.itemOptions.add(new Item.ItemOption(103, Util.nextInt(1, 8)));
                                        tien.itemOptions.add(new Item.ItemOption(160, Util.nextInt(1, 20)));
                                        tien.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 4)));

                                    } else {
                                        tien.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                                        tien.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
                                        tien.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                                        tien.itemOptions.add(new Item.ItemOption(160, Util.nextInt(10, 40)));
                                        tien.itemOptions.add(new Item.ItemOption(5, Util.nextInt(2, 10)));

                                    }
                                }

                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + tien.template.name);
                            }
                                break;
                            case 1883: {
                                if (InventoryService.gI().getCountEmptyBag(pl) == 0) {// capsulebac
                                    Service.gI().sendThongBaoOK(pl, "Cần 1 ô hành trang để mở");
                                    return;
                                }
                                Random random = new Random();
                                int rand = random.nextInt(100);
                                int id = Util.nextInt(1364, 1370);

                                Item tien = ItemService.gI().createNewItem((short) id);

                                if (Util.isTrue(80, 100)) {
                                    tien.itemOptions.add(new Item.ItemOption(50, Util.nextInt(1, 8)));
                                    tien.itemOptions.add(new Item.ItemOption(77, Util.nextInt(1, 8)));
                                    tien.itemOptions.add(new Item.ItemOption(103, Util.nextInt(1, 8)));
                                    tien.itemOptions.add(new Item.ItemOption(160, Util.nextInt(1, 20)));
                                    tien.itemOptions.add(new Item.ItemOption(5, Util.nextInt(1, 4)));

                                } else {
                                    tien.itemOptions.add(new Item.ItemOption(50, Util.nextInt(5, 15)));
                                    tien.itemOptions.add(new Item.ItemOption(77, Util.nextInt(5, 15)));
                                    tien.itemOptions.add(new Item.ItemOption(103, Util.nextInt(5, 15)));
                                    tien.itemOptions.add(new Item.ItemOption(160, Util.nextInt(10, 40)));
                                    tien.itemOptions.add(new Item.ItemOption(5, Util.nextInt(2, 10)));

                                }

                                InventoryService.gI().addItemBag(pl, tien);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                Service.gI().sendThongBao(pl, "Bạn đã nhận được " + tien.template.name);
                            }
                                break;
                            case 1536:
                                UseItem.gI().Hopdokichhoat(pl, item);
                                break;
                            case 1703:// set tl kh

                                UseItem.gI().Hopdothanlinh(pl, item);
                                break; // zl 0822992003 Đức dz
                            case 1704:// set hd kh

                                UseItem.gI().Hopdohuydiet(pl, item);
                                break; // zl 0822992003 Đức dz
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

    public void openRuongGo(Player pl, Item item) {
        List<String> textRuongGo = new ArrayList<>();
        int time = (int) TimeUtil.diffDate(new Date(), new Date(item.createTime), TimeUtil.DAY);
        if (time != 0) {
            Item itemReward = null;
            int param = item.itemOptions.get(0).param;
            int gold = 0;
            int[] listItem = { 441, 442, 443, 444, 445, 446, 447, 220, 221, 222, 223, 224, 225 };
            int[] listClothesReward;
            int[] listItemReward;
            String text = "Bạn nhận được\n";
            if (param < 8) {
                gold = 100000 * param;
                listClothesReward = new int[] { randClothes(param) };
                listItemReward = Util.pickNRandInArr(listItem, 3);
            } else if (param < 10) {
                gold = 250000 * param;
                listClothesReward = new int[] { randClothes(param), randClothes(param) };
                listItemReward = Util.pickNRandInArr(listItem, 4);
            } else {
                gold = 500000 * param;
                listClothesReward = new int[] { randClothes(param), randClothes(param), randClothes(param) };
                listItemReward = Util.pickNRandInArr(listItem, 5);
                int ruby = Util.nextInt(1, 5);
                pl.inventory.gem += ruby;
                textRuongGo.add(text + "|1| " + ruby + " Ngọc Xanh");
            }
            for (var i : listClothesReward) {
                itemReward = ItemService.gI().createNewItem((short) i);
                RewardService.gI().initBaseOptionClothes(itemReward.template.id, itemReward.template.type,
                        itemReward.itemOptions);
                RewardService.gI().initStarOption(itemReward, new RewardService.RatioStar[] {
                        new RewardService.RatioStar((byte) 1, 1, 2), new RewardService.RatioStar((byte) 2, 1, 3),
                        new RewardService.RatioStar((byte) 3, 1, 4), new RewardService.RatioStar((byte) 4, 1, 5), });
                InventoryService.gI().addItemBag(pl, itemReward);
                textRuongGo.add(text + itemReward.info);
            }
            for (var i : listItemReward) {
                itemReward = ItemService.gI().createNewItem((short) i);
                RewardService.gI().initBaseOptionSaoPhaLe(itemReward);
                itemReward.quantity = Util.nextInt(1, 5);
                InventoryService.gI().addItemBag(pl, itemReward);
                textRuongGo.add(text + itemReward.info);
            }
            if (param == 11) {
                itemReward = ItemService.gI().createNewItem((short) ConstItem.MANH_NHAN);
                itemReward.quantity = Util.nextInt(1, 3);
                InventoryService.gI().addItemBag(pl, itemReward);
                textRuongGo.add(text + itemReward.info);
            }
            NpcService.gI().createMenuConMeo(pl, ConstNpc.RUONG_GO, -1,
                    "Bạn nhận được\n|1|+" + Util.numberToMoney(gold) + " vàng", "OK [" + textRuongGo.size() + "]");
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            pl.inventory.addGold(gold);
            InventoryService.gI().sendItemBag(pl);
            PlayerService.gI().sendInfoHpMpMoney(pl);
        }
    }

    private void changePet(Player player, Item item) {
        if (player.pet != null) {
            int gender = player.pet.gender + 1;
            if (gender > 2) {
                gender = 0;
            }
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

    private void openLinhThu(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            int[][] ltthu = { { 1811, 1642, 1643, 1652, 1655, 1664, 1695, 1490, 1492, 1493, 1494 }, // linh thú
                                                                                                    // thường
                    { 1778, 1776, 1644, 1645, 1646, 1647, 1807, 1742 }, // tinh anh
                    { 1779, 1648, 1651, 1653, 1812 }// huyền thoại
            };
            int[][] OPTION = { { 0, 6, 7, 5, 27, 28, 50, 77, 103 }, { 500, 5000, 5000, 5, 5000, 5000, 5, 5, 5 }, // Thường
                    { 0, 6, 7, 5, 27, 28, 50, 77, 103 }, { 800, 8000, 8000, 8, 8000, 8000, 8, 8, 8 }, // tinh anh
                    { 0, 6, 7, 5, 27, 28, 50, 77, 103, 156, 157, 158 },
                    { 1200, 12000, 12000, 12, 12000, 12000, 12, 12, 12, 10, 10, 10 }, // huyen thoại
                    { 0, 6, 7, 5, 27, 28, 50, 77, 103, 156, 157, 158, 163, 181, 161, 189, 190 },
                    { 2000, 20000, 20000, 20, 20000, 20000, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20 }// TruyenKi
            };
            short id = 0;
            Random random = new Random();
            int rand = random.nextInt(100);

            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (rand < 60) {
                id = (short) ltthu[0][Util.nextInt(0, ltthu[0].length - 1)];
                int x = Util.nextInt(0, OPTION[0].length - 1);
                Item it = ItemService.gI().createNewItem(id);
                it.itemOptions.add(new ItemOption(OPTION[0][x], Util.nextInt(1, OPTION[1][x])));
                it.itemOptions.add(new ItemOption(72, 1));
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;

            } else if (rand < 95) {
                id = (short) ltthu[1][Util.nextInt(0, ltthu[1].length - 1)];
                int x = Util.nextInt(0, OPTION[2].length - 1);
                int y = x;
                while (y == x) {
                    y = Util.nextInt(0, OPTION[2].length - 1);
                }

                Item it = ItemService.gI().createNewItem(id);
                it.itemOptions.add(new ItemOption(OPTION[2][x], Util.nextInt(1, OPTION[3][x])));
                it.itemOptions.add(new ItemOption(OPTION[2][y], Util.nextInt(1, OPTION[3][y])));
                it.itemOptions.add(new ItemOption(72, 5));
                InventoryService.gI().addItemBag(pl, it);
                icon[1] = it.template.iconID;
            } else {
                id = (short) ltthu[2][Util.nextInt(0, ltthu[2].length - 1)];
                int x = Util.nextInt(0, OPTION[4].length - 1);
                int y = x;
                while (y == x) {
                    y = Util.nextInt(0, OPTION[4].length - 1);
                }
                int z = y;
                while (z == y || z == x) {
                    z = Util.nextInt(0, OPTION[4].length - 1);
                }
                Item it = ItemService.gI().createNewItem(id);
                it.itemOptions.add(new ItemOption(OPTION[4][x], Util.nextInt(1, OPTION[5][x])));
                it.itemOptions.add(new ItemOption(OPTION[4][y], Util.nextInt(1, OPTION[5][y])));
                it.itemOptions.add(new ItemOption(OPTION[4][z], Util.nextInt(1, OPTION[5][z])));
                it.itemOptions.add(new ItemOption(72, 7));
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

    private void openCSKB(Player pl, Item item) {
        if (InventoryService.gI().getCountEmptyBag(pl) > 0) {
            short[] temp = { 76, 188, 189, 190, 381, 382, 383, 384, 385 };
            int[][] gold = { { 5000, 20000 } };
            byte index = (byte) Util.nextInt(0, temp.length - 1);
            short[] icon = new short[2];
            icon[0] = item.template.iconID;
            if (index <= 3) {
                pl.inventory.gold += Util.nextInt(gold[0][0], gold[0][1]);
                if (pl.inventory.gold > Inventory.LIMIT_GOLD) {
                    pl.inventory.gold = Inventory.LIMIT_GOLD;
                }
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

    private void useItemTime(Player pl, Item item) {
        switch (item.template.id) {
            case 1731:
                if (pl.itemTime.isUseLoX5 == true || pl.itemTime.isUseLoX7 == true || pl.itemTime.isUseLoX10 == true
                        || pl.itemTime.isUseLoX15 == true) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX2 = System.currentTimeMillis();
                pl.itemTime.isUseLoX2 = true;
                break;
            case 1727:
                if (pl.itemTime.isUseLoX2 == true || pl.itemTime.isUseLoX7 == true || pl.itemTime.isUseLoX10 == true
                        || pl.itemTime.isUseLoX15 == true) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX5 = System.currentTimeMillis();
                pl.itemTime.isUseLoX5 = true;
                break;
            case 1728:
                if (pl.itemTime.isUseLoX5 == true || pl.itemTime.isUseLoX2 == true || pl.itemTime.isUseLoX10 == true
                        || pl.itemTime.isUseLoX15 == true) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX7 = System.currentTimeMillis();
                pl.itemTime.isUseLoX7 = true;
                break;
            case 1729:
                if (pl.itemTime.isUseLoX5 == true || pl.itemTime.isUseLoX7 == true || pl.itemTime.isUseLoX2 == true
                        || pl.itemTime.isUseLoX15 == true) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX10 = System.currentTimeMillis();
                pl.itemTime.isUseLoX10 = true;
                break;
            case 1730:
                if (pl.itemTime.isUseLoX5 == true || pl.itemTime.isUseLoX7 == true || pl.itemTime.isUseLoX10 == true
                        || pl.itemTime.isUseLoX2 == true) {
                    Service.gI().sendThongBao(pl, "Bạn đang sử dụng nước thánh rồi");
                    return;
                }
                pl.itemTime.lastTimeLoX15 = System.currentTimeMillis();
                pl.itemTime.isUseLoX15 = true;
                break;
            case 764:
                pl.itemTime.lastTimeKhauTrang = System.currentTimeMillis();
                pl.itemTime.isUseKhauTrang = true;
                break;
            case 1628:
                pl.itemTime.lastTimeBuax2DeTu = System.currentTimeMillis();
                pl.itemTime.isUseBuax2DeTu = true;
                break;
            case 382: // bổ huyết
                pl.itemTime.lastTimeBoHuyet = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet = true;
                break;
            case 383: // bổ khí
                pl.itemTime.lastTimeBoKhi = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi = true;
                break;
            case 384: // giáp xên
                pl.itemTime.lastTimeGiapXen = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen = true;
                break;
            case 381: // cuồng nộ
                pl.itemTime.lastTimeCuongNo = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo = true;
                Service.gI().point(pl);
                break;
            case 385: // ẩn danh
                pl.itemTime.lastTimeAnDanh = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh = true;
                break;
            case 379: // máy dò capsule
                pl.itemTime.lastTimeUseMayDo = System.currentTimeMillis();
                pl.itemTime.isUseMayDo = true;
                break;
            case 1150:// cn
                pl.itemTime.lastTimeCuongNo2 = System.currentTimeMillis();
                pl.itemTime.isUseCuongNo2 = true;
                Service.gI().point(pl);

                break;
            case 1152:// bo huyet
                pl.itemTime.lastTimeBoHuyet2 = System.currentTimeMillis();
                pl.itemTime.isUseBoHuyet2 = true;
                break;
            case 1151:// bo khi
                pl.itemTime.lastTimeBoKhi2 = System.currentTimeMillis();
                pl.itemTime.isUseBoKhi2 = true;
                break;
            case 1153:// gx
                pl.itemTime.lastTimeGiapXen2 = System.currentTimeMillis();
                pl.itemTime.isUseGiapXen2 = true;
                break;
            case 1154:// an danh
                pl.itemTime.lastTimeAnDanh2 = System.currentTimeMillis();
                pl.itemTime.isUseAnDanh2 = true;
                break;
            case 638: // Commeson
                pl.itemTime.lastTimeUseCMS = System.currentTimeMillis();
                pl.itemTime.isUseCMS = true;
                break;
            case 2160: // Nồi cơm điện
                pl.itemTime.lastTimeUseNCD = System.currentTimeMillis();
                pl.itemTime.isUseNCD = true;
                break;
            case 579:
            case 1045: // Đuôi khỉ
                pl.itemTime.lastTimeUseDK = System.currentTimeMillis();
                pl.itemTime.isUseDK = true;
                break;

            case 1517:// stc

            case 1518:

            case 663: // bánh pudding
            case 664: // xúc xíc
            case 665: // kem dâu
            case 666: // mì ly
            case 667: // sushi
                pl.itemTime.lastTimeEatMeal = System.currentTimeMillis();
                pl.itemTime.isEatMeal = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal);
                pl.itemTime.iconMeal = item.template.iconID;
                break;
            case 1787:
            case 880:
            case 881:
            case 882:
            case 1635:
                pl.itemTime.lastTimeEatMeal2 = System.currentTimeMillis();
                pl.itemTime.isEatMeal2 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal2);
                pl.itemTime.iconMeal2 = item.template.iconID;
                break;
            case 752:
            case 1016:
            case 753:
            case 889:
            case 900:
            case 902:
            case 903:
                pl.itemTime.lastTimeEatMeal3 = System.currentTimeMillis();
                pl.itemTime.isEatMeal3 = true;
                ItemTimeService.gI().removeItemTime(pl, pl.itemTime.iconMeal3);
                pl.itemTime.iconMeal3 = item.template.iconID;
                break;
            case 1109: // máy dò đồ
                pl.itemTime.lastTimeUseMayDo2 = System.currentTimeMillis();
                pl.itemTime.isUseMayDo2 = true;
                break;
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
                    NpcService.gI().createMenuConMeo(pl, ConstNpc.TUTORIAL_SUMMON_DRAGON,
                            -1, "Bạn chỉ có thể gọi rồng từ ngọc 3 sao, 2 sao, 1 sao", "Hướng\ndẫn thêm\n(mới)", "OK");
                    break;
            }
        } else if (tempId >= ShenronEventService.NGOC_RONG_1_SAO && tempId <= ShenronEventService.NGOC_RONG_7_SAO) {
            ShenronEventService.gI().openMenuSummonShenron(pl, 0);
        }
    }

    private void learnSkill(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.id >= 1334 && item.template.id <= 1351) {
                learnSkillSuperNew(pl, item);
            } else if (item.template.id >= 1356 && item.template.id <= 1376) {
                if (item.template.id == 1356 || item.template.id == 1363 || item.template.id == 1370) {
                    if (item.template.id >= 1363 && item.template.id <= 1369) {
                        SkillService.gI().learSkillSpecial(pl, Skill.PHAN_THAN);
                    }
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    return;
                }
                learnSkillNew2(pl, item);
            } else {
                if (item.template.gender == pl.gender || item.template.gender == 3) {
                    String[] subName = item.template.name.split("");
                    byte level = Byte.parseByte(subName[subName.length - 1]);
                    Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                    if (curSkill.point == 7) {
                        Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                    } else {
                        if (curSkill.point == 0) {
                            if (level == 1) {
                                curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                        level);
                                SkillUtil.setSkill(pl, curSkill);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                msg = Service.gI().messageSubCommand30((byte) 23);
                                msg.writer().writeShort(curSkill.skillId);
                                pl.sendMessage(msg);
                                msg.cleanup();
                            } else {
                                Skill skillNeed = SkillUtil
                                        .createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                                Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " cấp "
                                        + skillNeed.point + " trước!");
                            }
                        } else {
                            if (curSkill.point + 1 == level) {
                                curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                        level);
                                // System.out.println(curSkill.template.name + " - " + curSkill.point);
                                SkillUtil.setSkill(pl, curSkill);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                msg = Service.gI().messageSubCommand30((byte) 62);
                                msg.writer().writeShort(curSkill.skillId);
                                pl.sendMessage(msg);
                                msg.cleanup();
                            } else {
                                Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp "
                                        + (curSkill.point + 1) + " trước!");
                            }
                        }
                        InventoryService.gI().sendItemBag(pl);
                    }
                } else {
                    Service.gI().sendThongBao(pl, "Không thể thực hiện");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void learnSkillNew2(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                byte level = SkillUtil.getLevelSkillByItemID(item.template.id);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill == null) {
                    SkillService.gI().learSkillSpecial(pl,
                            (byte) SkillUtil.getSkillByItemID(pl, item.template.id).skillId);
                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    return;
                } else {
                    if (curSkill.point == 5) {
                        Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                    } else {
                        if (curSkill.point == 0) {
                            if (level == 1) {
                                curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                        level);
                                SkillUtil.setSkill(pl, curSkill);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                msg = Service.gI().messageSubCommand30((byte) 23);
                                msg.writer().writeShort(curSkill.skillId);
                                pl.sendMessage(msg);
                                msg.cleanup();
                                if (curSkill.template.id == Skill.SUPER_BROLY) {
                                    curSkill = SkillUtil.createSkill(Skill.GONG, level);
                                    SkillUtil.setSkill(pl, curSkill);
                                    InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                    msg = Service.gI().messageSubCommand30((byte) 23);
                                    msg.writer().writeShort(curSkill.skillId);
                                    pl.sendMessage(msg);
                                    msg.cleanup();
                                }
                            } else {
                                Skill skillNeed = SkillUtil
                                        .createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                                Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " cấp "
                                        + skillNeed.point + " trước!");
                            }
                        } else {
                            if (curSkill.point + 1 == level) {
                                curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                        level);
                                // System.out.println(curSkill.template.name + " - " + curSkill.point);
                                SkillUtil.setSkill(pl, curSkill);
                                InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                                msg = Service.gI().messageSubCommand30((byte) 62);
                                msg.writer().writeShort(curSkill.skillId);
                                pl.sendMessage(msg);
                                msg.cleanup();
                            } else {
                                Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " cấp "
                                        + (curSkill.point + 1) + " trước!");
                            }
                        }
                        InventoryService.gI().sendItemBag(pl);
                    }
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {

        }
    }

    private void learnSkillSuperNew(Player pl, Item item) {
        Message msg;
        try {
            if (item.template.gender == pl.gender || item.template.gender == 3) {
                byte level = SkillUtil.getLevelSkillByItemID(item.template.id);
                Skill curSkill = SkillUtil.getSkillByItemID(pl, item.template.id);
                if (curSkill.point == 6) {
                    Service.gI().sendThongBao(pl, "Kỹ năng đã đạt tối đa!");
                } else {
                    if (curSkill.point == 0) {
                        if (level == 1) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand30((byte) 23);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                            SkillService.gI().learSkillSpecial(pl, (byte) 30);
                        } else {
                            Skill skillNeed = SkillUtil
                                    .createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id), level);
                            if (level > 1) {
                                Item itemNew = ItemService.gI().createNewItem((short) (item.template.id - 1));
                                String name = itemNew.template.name;
                                String desiredName = name.substring(5);
                                Service.gI().sendThongBao(pl, "Vui lòng học " + desiredName + " trước!");
                            } else {
                                Service.gI().sendThongBao(pl, "Vui lòng học " + skillNeed.template.name + " trước!");
                            }
                        }
                    } else {
                        if (curSkill.point + 1 == level) {
                            curSkill = SkillUtil.createSkill(SkillUtil.getTempSkillSkillByItemID(item.template.id),
                                    level);
                            SkillUtil.setSkill(pl, curSkill);
                            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                            msg = Service.gI().messageSubCommand30((byte) 62);
                            msg.writer().writeShort(curSkill.skillId);
                            pl.sendMessage(msg);
                            msg.cleanup();
                        } else {
                            if (level > 1) {
                                Item itemNew = ItemService.gI().createNewItem((short) (item.template.id - 1));
                                String name = itemNew.template.name;
                                String desiredName = name.substring(5);
                                Service.gI().sendThongBao(pl, "Vui lòng học " + desiredName + " trước!");
                            } else {
                                Service.gI().sendThongBao(pl, "Vui lòng học " + curSkill.template.name + " trước!");
                            }
                        }
                    }
                    InventoryService.gI().sendItemBag(pl);
                }
            } else {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleFusion(Player pl, java.util.function.Consumer<player.Pet> fusionAction) {
        if (pl.pet == null || pl.fusion.typeFusion == 4) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        } else {
            if (pl.fusion.typeFusion == ConstPlayer.NON_FUSION) {
                fusionAction.accept(pl.pet);
            } else {
                pl.pet.unFusion();
            }
        }
    }

    private void handleOpenPetSkill(Player pl, int skillIndex, java.util.function.Consumer<player.Pet> openAction) {
        if (pl.pet == null) {
            Service.gI().sendThongBao(pl, "Ngươi làm gì có đệ tử?");
            return;
        }
        if (pl.pet.playerSkill.skills.get(skillIndex).skillId != -1) {
            openAction.accept(pl.pet);
        } else {
            Service.gI().sendThongBao(pl, "Ít nhất đệ tử ngươi phải có chiêu " + (skillIndex + 1) + " chứ!");
        }
    }

    private void useCoinItem(Player pl, Item item, int coinAmount) {
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
        if (PlayerDAO.addcash(pl.getSession().userId, coinAmount)) {
            pl.getSession().cash += coinAmount;
            Service.gI().sendThongBao(pl, "Bạn đã nhận được " + String.format("%,d", coinAmount) + " coin");
        }
    }

    private void useTDLT(Player pl, Item item) {
        if (pl.itemTime.isUseTDLT) {
            ItemTimeService.gI().turnOffTDLT(pl, item);
        } else {
            ItemTimeService.gI().turnOnTDLT(pl, item);
        }
    }

    private void usePorata2(Player pl) {
        handleFusion(pl, pet -> pet.fusion2(true));
    }

    private void usePorata3(Player pl) {
        handleFusion(pl, pet -> pet.fusion3(true));
    }

    private void usePorataHacHoa(Player pl) {
        handleFusion(pl, pet -> pet.fusionHacHoa(true));
    }

    private void usePorata(Player pl) {
        handleFusion(pl, pet -> pet.fusion(true));
    }

    private void openCapsuleUI(Player pl) {
        pl.iDMark.setTypeChangeMap(ConstMap.CHANGE_CAPSULE);
        ChangeMapService.gI().openChangeMapTab(pl);
    }

    public void openRuongNgocRong(Player pl, Item item) {
        int nr = Util.nextInt(16, 20);
        int[] vp = { 220, 221, 222, 223, 224, 225 };
        int[] vpVip = { 1682, 1683, 1688, 1689, 1677, 1678, 1013, 1021, 1022 };
        Item item2 = null;

        item2 = ItemService.gI().createNewItem((short) nr);
        item2.quantity = 1;
        item2.itemOptions.add(new Item.ItemOption(30, 1));

        InventoryService.gI().addItemBag(pl, item2);
        InventoryService.gI().sendItemBag(pl);
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        Service.gI().sendThongBao(pl, "Bạn mở rương nhận được " + item2.template.name);
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
        // Kiểm tra số lượng người trong khu

        if (zoneChose.getNumOfPlayers() > 25
                || MapService.gI().isMapDoanhTrai(zoneChose.map.mapId)
                || MapService.gI().isMapMaBu(zoneChose.map.mapId)
                || MapService.gI().isMapHuyDiet(zoneChose.map.mapId)) {
            Service.gI().sendThongBao(pl, "Hiện tại không thể vào được khu!");
            return;
        }
        if (index != 0 || zoneChose.map.mapId == 21
                || zoneChose.map.mapId == 22
                || zoneChose.map.mapId == 23) {
            pl.mapBeforeCapsule = pl.zone;
        } else {
            zoneId = pl.mapBeforeCapsule != null ? pl.mapBeforeCapsule.zoneId : -1;
            pl.mapBeforeCapsule = null;
        }
        pl.changeMapVIP = true;
        ChangeMapService.gI().changeMapBySpaceShip(pl, pl.mapCapsule.get(index).map.mapId, zoneId, -1);
    }

    public void eatPea(Player player) {
        if (!Util.canDoWithTime(player.lastTimeEatPea, 1000)) {
            return;
        }
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
            for (Item.ItemOption io : pea.itemOptions) {
                if (io.optionTemplate.id == 2) {
                    hpKiHoiPhuc = io.param * 1000;
                    break;
                }
                if (io.optionTemplate.id == 48) {
                    hpKiHoiPhuc = io.param;
                    break;
                }
            }
            player.nPoint.setHp(Util.toIntOrLong(player.nPoint.hp + hpKiHoiPhuc));
            player.nPoint.setMp(Util.toIntOrLong(player.nPoint.mp + hpKiHoiPhuc));
            PlayerService.gI().sendInfoHpMp(player);
            Service.gI().sendInfoPlayerEatPea(player);
            if (player.pet != null && player.zone.equals(player.pet.zone) && !player.pet.isDie()) {
                int statima = 100 * lvPea;
                player.pet.nPoint.stamina += statima;
                if (player.pet.nPoint.stamina > player.pet.nPoint.maxStamina) {
                    player.pet.nPoint.stamina = player.pet.nPoint.maxStamina;
                }
                player.pet.nPoint.setHp(Util.toIntOrLong(player.pet.nPoint.hp + hpKiHoiPhuc));
                player.pet.nPoint.setMp(Util.toIntOrLong(player.pet.nPoint.mp + hpKiHoiPhuc));
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
                case 402: // skill 1
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 0)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 403: // skill 2
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 1)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 404: // skill 3
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 2)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
                    break;
                case 759: // skill 4
                    if (SkillUtil.upSkillPet(pl.pet.playerSkill.skills, 3)) {
                        Service.gI().chatJustForMe(pl, pl.pet, "Cám ơn sư phụ");
                        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
                    } else {
                        Service.gI().sendThongBao(pl, "Không thể thực hiện");
                    }
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
        NpcService.gI().createMenuConMeo(pl, item.template.id, 564,
                "Đây chính là dấu hiệu riêng của...\nĐại Ma Vương Pôcôlô\nĐó là một tên quỷ dữ đội lốt người, một kẻ đại gian ác\ncó sức mạnh vô địch và lòng tham không đáy...\nĐối phó với hắn không phải dễ\nCon có chắc chắn muốn tìm hắn không?",
                "Đồng ý", "Từ chối");
    }

    private void ItemSieuThanThuy(Player pl, Item item) {
        long tnsm = 5_000_000;
        int n = 0;
        switch (item.template.id) {
            case 727:
                n = 2;
                break;
            case 728:
                n = 10;
                break;
        }
        InventoryService.gI().subQuantityItemsBag(pl, item, 1);
        InventoryService.gI().sendItemBag(pl);
        if (Util.isTrue(50, 100)) {
            Service.gI().sendThongBao(pl, "Bạn đã bị chết vì độc của thuốc tăng lực siêu thần thủy.");
            pl.setDie();
        } else {
            for (int i = 0; i < n; i++) {
                Service.gI().addSMTN(pl, (byte) 2, tnsm, true);
            }
        }
    }

    private void Hopdothanlinh(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất",
                "Set namec", "Set xayda", "Từ chổi");
    }

    private void Hopdohuydiet(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất",
                "Set namec", "Set xayda", "Từ chổi");
    }

    private void Hopdokichhoat(Player pl, Item item) {
        NpcService.gI().createMenuConMeo(pl, item.template.id, -1, "Chọn hành tinh của Bạn đi", "Set trái đất",
                "Set namec", "Set xayda", "Từ chổi");
    }

    public void UseCard(Player pl, Item item) {
        RadarCard radarTemplate = RadarService.gI().RADAR_TEMPLATE.stream().filter(c -> c.Id == item.template.id)
                .findFirst().orElse(null);
        if (radarTemplate == null) {
            return;
        }
        if (radarTemplate.Require != -1) {
            RadarCard radarRequireTemplate = RadarService.gI().RADAR_TEMPLATE.stream()
                    .filter(r -> r.Id == radarTemplate.Require).findFirst().orElse(null);
            if (radarRequireTemplate == null) {
                return;
            }
            Card cardRequire = pl.Cards.stream().filter(r -> r.Id == radarRequireTemplate.Id).findFirst().orElse(null);
            if (cardRequire == null || cardRequire.Level < radarTemplate.RequireLevel) {
                Service.gI().sendThongBao(pl, "Bạn cần sưu tầm " + radarRequireTemplate.Name + " ở cấp độ "
                        + radarTemplate.RequireLevel + " mới có thể sử dụng thẻ này");
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
                if (card.Level == -1) {
                    card.Level = 1;
                } else {
                    card.Level++;
                }
                Service.gI().point(pl);
            }
            RadarService.gI().RadarSetAmount(pl, card.Id, card.Amount, card.MaxAmount);
            RadarService.gI().RadarSetLevel(pl, card.Id, card.Level);
            InventoryService.gI().subQuantityItemsBag(pl, item, 1);
            InventoryService.gI().sendItemBag(pl);
        }
    }

    private static int getCoinAmount(int itemId) {
        return switch (itemId) {
            case 1859 -> 1000;
            case 1788 -> 2000;
            case 1789 -> 5000;
            case 1790 -> 10000;
            case 1791 -> 20000;
            case 1792 -> 50000;
            case 1793 -> 100000;
            case 1794 -> 200000;
            case 1795 -> 500000;
            default -> 0;
        };
    }

}
