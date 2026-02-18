package nro.services;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import java.io.IOException;
import models.Template.FlagBag;
import java.util.List;
import nro.player.Player;
import nro.server.Manager;
import network.Message;
import java.util.ArrayList;

public class FlagBagService {

    private final List<FlagBag> flagClan = new ArrayList<>();
    private static FlagBagService i;

    public static FlagBagService gI() {
        if (i == null) {
            i = new FlagBagService();
        }
        return i;
    }

    public void sendIconFlagChoose(Player player, int id) {
        FlagBag fb = getFlagBag(id);
        if (fb != null) {
            Message msg;
            try {
                msg = new Message(-62);
                msg.writer().writeByte(fb.id);
                msg.writer().writeByte(fb.iconEffect.length + 1);
                msg.writer().writeShort(fb.iconId);
                for (Short iconId : fb.iconEffect) {
                    msg.writer().writeShort(iconId);
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
            }
        }
    }

    public void sendIconEffectFlag(Player player, int id) {
        FlagBag fb = getFlagBag(id);
        if (fb != null) {
            Message msg;
            try {
                msg = new Message(-63);
                msg.writer().writeByte(fb.id);
                msg.writer().writeByte(fb.iconEffect.length);
                for (Short iconId : fb.iconEffect) {
                    msg.writer().writeShort(iconId);
                }
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
            }
        }
    }

//    public void sendListFlagClan(Player pl) {
//        List<FlagBag> list = getFlagsForChooseClan();
//        Message msg;
//        try {
//            msg = new Message(-46);
//            msg.writer().writeByte(1);
//            msg.writer().writeByte(list.size());
//            for (FlagBag fb : list) {
//                msg.writer().writeByte(fb.id);
//                msg.writer().writeUTF(fb.name);
//                msg.writer().writeInt(fb.gold);
//                msg.writer().writeInt(fb.gem);
//                switch (fb.id) {
//                    case 97 -> { // id flbag
//                        msg.writer().writeByte(1);//số lượng dòng chỉ số
//                        msg.writer().writeShort(101);//id option 
//                        msg.writer().writeShort(100);//pram
//                    }
//                    case 98 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(95);
//                        msg.writer().writeShort(10);
//                    }
//                    case 99 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(50);
//                        msg.writer().writeShort(100);
//                    }
//                    case 100 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(77);
//                        msg.writer().writeShort(100);
//                    }
//                    case 70 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(5);
//                        msg.writer().writeShort(100);
//                    }
//                    case 71 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(14);
//                        msg.writer().writeShort(20);
//                    }
//                    case 72 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(77);
//                        msg.writer().writeShort(100);
//                    }
//                    case 77 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(0);
//                        msg.writer().writeShort(1000);
//                    }
//                    case 78 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(6);
//                        msg.writer().writeShort(1000);
//                    }
//                    case 79 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(7);
//                        msg.writer().writeShort(1000);
//                    }
//                    case 80 -> {
//                        msg.writer().writeByte(1);
//                        msg.writer().writeShort(19);
//                        msg.writer().writeShort(120);
//                    }
//                    default -> {
//                        msg.writer().writeByte(0);
//                    }
//                }
//            }
//            pl.sendMessage(msg);
//            msg.cleanup();
//        } catch (IOException e) {
//        }
//    }
    
    public void sendListFlagClan(Player pl) {
        List<FlagBag> list = getFlagsForChooseClan();
        Message msg;
        try {
            msg = new Message(-46);
            msg.writer().writeByte(1); //type
            msg.writer().writeByte(list.size());
            for (FlagBag fb : list) {
                msg.writer().writeByte(fb.id);
                msg.writer().writeUTF(fb.name);
                msg.writer().writeInt(fb.gold);
                msg.writer().writeInt(fb.gem);
            }
            pl.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public FlagBag getFlagBag(int id) {
        for (FlagBag fb : Manager.FLAGS_BAGS) {
            if (fb.id == id) {
                return fb;
            }
        }
        return null;
    }

    public List<FlagBag> getFlagsForChooseClan() {
        if (flagClan.isEmpty()) {
            int[] flagsId = {0, 8, 7, 6, 5, 4, 3, 2, 1, 18, 17, 16, 15, 14, 13,
                12, 11, 10, 9, 27, 26, 25, 24, 23, 36, 32, 33, 34, 35, 19, 22, 21, 20, 29,77,78,79
            };
            for (int i = 0; i < flagsId.length; i++) {
                flagClan.add(getFlagBag(flagsId[i]));
            }
        }
        return flagClan;
    }
}
