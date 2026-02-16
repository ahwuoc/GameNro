package npc.specialnpc;

import player.Player;
import network.Message;
import services.InventoryService;
import services.ItemService;
import services.Service;
import utils.Logger;
import utils.Util;
import consts.ConstItem;
import item.Item;
import jdbc.daos.PlayerDAO;

public class NoiBanhChung {

    public static final long TIME_COOKING = 1000L;

    private Player player;
    public long lastTimeCreate;
    public long timeDone;
    public int type;
    public int quantity;

    private final short id = 94;

    public NoiBanhChung(Player player, long lastTimeCreate, long timeDone, int type, int quantity) {
        this.player = player;
        this.lastTimeCreate = lastTimeCreate;
        this.timeDone = timeDone;
        this.type = type;
        this.quantity = quantity;
    }

    public void sendNoiBanhChung() {
        Message msg;
        try {
            msg = new Message(-122);
            msg.writer().writeShort(this.id);
            msg.writer().writeByte(1);
            msg.writer().writeShort(7082);
            msg.writer().writeByte(0);
            msg.writer().writeInt(this.getSecondDone());
            this.player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(NoiBanhChung.class, e);
        }
    }

    public int getSecondDone() {
        int seconds = (int) ((lastTimeCreate + timeDone - System.currentTimeMillis()) / 1000);
        return seconds > 0 ? seconds : 0;
    }

    public boolean isDone() {
        return getSecondDone() <= 0;
    }

    public void finishCooking() {
        if (!isDone()) {
            Service.gI().sendThongBao(player, "Bánh chưa chín, vui lòng đợi thêm " + getSecondDone() + " giây");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 2) {
            Service.gI().sendThongBao(player, "Hành trang của con không đủ chỗ trống (Cần ít nhất 2 ô)");
            return;
        }

        destroyNoi();

        int points = (type == ConstItem.BANH_CHUNG ? 2 : 0) * quantity;
        player.pointtet += points;
        if (points > 0) {
            PlayerDAO.addPointTet(player, points);
        }

        for (int i = 0; i < quantity; i++) {
            if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                Service.gI().sendThongBao(player, "Hành trang đã đầy, con nhận được " + i + " phần quà");
                break;
            }
            giveReward();
        }

        InventoryService.gI().sendItemBag(player);
        if (points > 0) {
            Service.gI().sendThongBao(player,
                    "Chúc mừng con đã nấu thành công và nhận được " + points + " điểm sự kiện!");
        } else {
            Service.gI().sendThongBao(player, "Chúc mừng con đã nấu thành công!");
        }
        player.noibanhchung = null;
    }

    private void giveReward() {
        int rd = Util.nextInt(1, 100);
        Item gift = null;
        if (type == ConstItem.BANH_CHUNG) {
            if (rd <= 40) {
                gift = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, Util.nextInt(5, 10));
            } else if (rd <= 70) {
                gift = ItemService.gI().createNewItem((short) ConstItem.NGOC, Util.nextInt(100, 300));
            } else if (rd <= 95) {
                gift = ItemService.gI().createNewItem((short) ConstItem.DA_NGU_SAC, Util.nextInt(1, 2));
            } else {
                gift = ItemService.gI()
                        .createNewItem((short) (Util.nextInt(ConstItem.NGOC_RONG_1_SAO, ConstItem.NGOC_RONG_3_SAO)));
            }
        } else {
            if (rd <= 40) {
                gift = ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, Util.nextInt(1, 5));
            } else if (rd <= 70) {
                gift = ItemService.gI().createNewItem((short) ConstItem.NGOC, Util.nextInt(20, 100));
            } else if (rd <= 95) {
                gift = ItemService.gI().createNewItem((short) ConstItem.DA_NGU_SAC, 1);
            } else {
                gift = ItemService.gI()
                        .createNewItem((short) (Util.nextInt(ConstItem.NGOC_RONG_4_SAO, ConstItem.NGOC_RONG_7_SAO)));
            }
        }

        Item pet = ItemService.gI().createNewItem((short) 1621, 1);
        pet.addOptionParam(50, Util.nextInt(1, 30));
        pet.addOptionParam(77, Util.nextInt(1, 30));
        pet.addOptionParam(103, Util.nextInt(1, 30));
        pet.addOptionParam(5, Util.nextInt(1, 30));
        pet.addOptionParam(189, Util.nextInt(1, 30));
        if (Util.isTrue(1, 2)) {
            pet.addOptionParam(93, Util.nextInt(1, 5));
        }

        if (gift != null) {
            InventoryService.gI().addItemBag(player, gift);
        }
        InventoryService.gI().addItemBag(player, pet);
    }

    public void destroyNoi() {
        try {
            Message msg = new Message(-117);
            msg.writer().writeByte(101);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void subTimeDone(int d, int h, int m, int s) {
        this.timeDone -= ((d * 24 * 60 * 60 * 1000) + (h * 60 * 60 * 1000) + (m * 60 * 1000) + (s * 1000));
        this.sendNoiBanhChung();
    }

    public void dispose() {
        this.player = null;
    }
}
