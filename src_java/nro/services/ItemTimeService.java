package nro.services;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import consts.ConstPlayer;
import item.Item;

import static item.ItemTime.*;

import nro.player.Fusion;
import nro.player.Player;
import network.Message;

import java.io.IOException;

import models.TreasureUnderSea.TreasureUnderSea;
import models.SnakeWay.SnakeWay;
import models.DestronGas.DestronGas;
import models.RedRibbonHQ.RedRibbonHQ;
import utils.Logger;

public class ItemTimeService {

    private static ItemTimeService i;

    public static ItemTimeService gI() {
        if (i == null) {
            i = new ItemTimeService();
        }
        return i;
    }

    //gửi cho client
    public void sendAllItemTime(Player player) {
        ItemTimeService.gI().sendTextBanDoKhoBau(player);
        ItemTimeService.gI().sendTextDoanhTrai(player);
        ItemTimeService.gI().sendTextConDuongRanDoc(player);
        ItemTimeService.gI().sendTextKhiGasHuyDiet(player);
        ItemTimeService.gI().sendTextTimePickDoanhTrai(player);
        if (player.itemTime.isBoXuong) {
            int id = switch (player.gender) {
                case 0 ->
                    5101; // td
                case 1 ->
                    5105; // nm
                case 2 ->
                    5103; // xd
                default ->
                    5101;
            };
            sendItemTime(player, id, (int) ((TIME_ITEM_X2_DT - (System.currentTimeMillis() - player.itemTime.lastTimeBoXuong)) / 1000));
        } else if (player.itemTime.isBiMa) {
            sendItemTime(player, 7057, (int) ((TIME_ITEM_X2_DT - (System.currentTimeMillis() - player.itemTime.lastTimeBiMa)) / 1000));
        } else if (player.itemTime.isMaTroi) {
            sendItemTime(player, 6091, (int) ((TIME_ITEM_X2_DT - (System.currentTimeMillis() - player.itemTime.lastTimeMaTroi)) / 1000));
        } else if (player.itemTime.isDoiNhi) {
            sendItemTime(player, 6094, (int) ((TIME_ITEM_X2_DT - (System.currentTimeMillis() - player.itemTime.lastTimeDoiNhi)) / 1000));
        } else if (player.itemTime.iscarot) {
            sendItemTime(player, 4075, (int) ((TIME_CAROT - (System.currentTimeMillis() - player.itemTime.lastTimecarot)) / 1000));
        } 
        if (player.effectSkill.isBienHinh) {
            switch (player.gender) {
                case 0://td
                    sendItemTime(player, 20417 + (player.playerSkill.getSkillbyId(27).point - player.numUseSkill - 1), (int) ((player.effectSkill.lastTimeBienHinh + player.effectSkill.timeBienHinh) - System.currentTimeMillis()) / 1000);
                    break;
                case 1://nm
                    sendItemTime(player, 20422 + (player.playerSkill.getSkillbyId(27).point - player.numUseSkill - 1), (int) ((player.effectSkill.lastTimeBienHinh + player.effectSkill.timeBienHinh) - System.currentTimeMillis()) / 1000);
                    break;
                case 2://xd
                    sendItemTime(player, 20412 + (player.playerSkill.getSkillbyId(27).point - player.numUseSkill - 1), (int) ((player.effectSkill.lastTimeBienHinh + player.effectSkill.timeBienHinh) - System.currentTimeMillis()) / 1000);
                    break;
            }
        }
        if (player.fusion.typeFusion == ConstPlayer.LUONG_LONG_NHAT_THE) {
            sendItemTime(player, player.gender == ConstPlayer.NAMEC ? 3901 : 3790,
                    (int) ((Fusion.TIME_FUSION - (System.currentTimeMillis() - player.fusion.lastTimeFusion)) / 1000));
        }
        if (player.itemTime.isBanhTrungThu1Trung) {
            sendItemTime(player, 4042, (int) ((TIME_30P - (System.currentTimeMillis() - player.itemTime.lastTimeBanhTrungThu1Trung)) / 1000));
        }
        if (player.itemTime.ispho1) {
            sendItemTime(player, 16323, (int) ((TIME_EAT_MEAL3 - (System.currentTimeMillis() - player.itemTime.lastTimepho1)) / 1000));
        }
        if (player.itemTime.ispho2) {
            sendItemTime(player, 16324, (int) ((TIME_EAT_MEAL3 - (System.currentTimeMillis() - player.itemTime.lastTimepho2)) / 1000));
        }
        if (player.itemTime.ispho3) {
            sendItemTime(player, 16282, (int) ((TIME_EAT_MEAL3 - (System.currentTimeMillis() - player.itemTime.lastTimepho3)) / 1000));
        }
        if (player.itemTime.isBanhTrungThu2Trung) {
            sendItemTime(player, 4043, (int) ((TIME_30P - (System.currentTimeMillis() - player.itemTime.lastTimeBanhTrungThu2Trung)) / 1000));
        }
        if (player.itemTime.isBanhTrungThuDb) {
            sendItemTime(player, 4125, (int) ((TIME_30P - (System.currentTimeMillis() - player.itemTime.lastTimeBanhTrungThuDb)) / 1000));
        }
        if (player.itemTime.isBanhTrungHop) {
            sendItemTime(player, 4126, (int) ((TIME_30P - (System.currentTimeMillis() - player.itemTime.lastTimeBanhTrungThuHop )) / 1000));
        }
        if (player.itemTime.isUseLoX2) {
            sendItemTime(player, 21881, (int) ((TIME_30P- (System.currentTimeMillis() - player.itemTime.lastTimeLoX2)) / 1000));
        }
        if (player.itemTime.isUseLoX5) {
            sendItemTime(player, 21878, (int) ((TIME_30P- (System.currentTimeMillis() - player.itemTime.lastTimeLoX5)) / 1000));
        }
        if (player.itemTime.isUseLoX7) {
            sendItemTime(player, 21879, (int) ((TIME_30P- (System.currentTimeMillis() - player.itemTime.lastTimeLoX7)) / 1000));
        }
        if (player.itemTime.isUseLoX10) {
            sendItemTime(player, 21880, (int) ((TIME_30P- (System.currentTimeMillis() - player.itemTime.lastTimeLoX10)) / 1000));
        }
        if (player.itemTime.isUseLoX15) {
            sendItemTime(player, 21876, (int) ((TIME_30P- (System.currentTimeMillis() - player.itemTime.lastTimeLoX15)) / 1000));
        }
      
        if (player.itemTime.isUseBuax2DeTu) {
            sendItemTime(player, 13540, (int) ((TIME_30P- (System.currentTimeMillis() - player.itemTime.lastTimeBuax2DeTu)) / 1000));
        }
        if (player.itemTime.isUseKhauTrang) {
            sendItemTime(player, 7149, (int) ((TIME_30P - (System.currentTimeMillis() - player.itemTime.lastTimeKhauTrang)) / 1000));
        }
        if (player.itemTime.isUsevevang) {
            sendItemTime(player, 15686,
                    (int) ((player.itemTime.totalTimeVeVang
                    - (System.currentTimeMillis() - player.itemTime.lastTimevevang)) / 1000)
            );
        }

        if (player.itemTime.isUseBoHuyet) {
            sendItemTime(player, 2755, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet)) / 1000));
        }
        if (player.itemTime.isUseBoKhi) {
            sendItemTime(player, 2756, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi)) / 1000));
        }
        if (player.itemTime.isUseGiapXen) {
            sendItemTime(player, 2757, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen)) / 1000));
        }
        if (player.itemTime.isUseCuongNo) {
            sendItemTime(player, 2754, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo)) / 1000));
        }

        if (player.itemTime.isUseAnDanh) {
            sendItemTime(player, 2760, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh)) / 1000));
        }
        if (player.itemTime.isUseBoHuyet2) {
            sendItemTime(player, 10714, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoHuyet2)) / 1000));
        }
        if (player.itemTime.isUseBoKhi2) {
            sendItemTime(player, 10715, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeBoKhi2)) / 1000));
        }
        if (player.itemTime.isUseGiapXen2) {
            sendItemTime(player, 10712, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeGiapXen2)) / 1000));
        }
        if (player.itemTime.isUseCuongNo2) {
            sendItemTime(player, 10716, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeCuongNo2)) / 1000));
        }

        if (player.itemTime.isUseAnDanh2) {
            sendItemTime(player, 10717, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeAnDanh2)) / 1000));
        }
        if (player.itemTime.isUseCMS) {
            sendItemTime(player, 5829, (int) ((TIME_CMS - (System.currentTimeMillis() - player.itemTime.lastTimeUseCMS)) / 1000));
        }
        if (player.itemTime.isUseNCD) {
            sendItemTime(player, 11173, (int) ((TIME_NCD - (System.currentTimeMillis() - player.itemTime.lastTimeUseNCD)) / 1000));
        }
        if (player.itemTime.isUseGTPT) {
            sendItemTime(player, 3778, (int) ((TIME_ITEM - (System.currentTimeMillis() - player.itemTime.lastTimeUseGTPT)) / 1000));
        }
        if (player.itemTime.isUseDK) {
            sendItemTime(player, 5072, (int) ((TIME_DK - (System.currentTimeMillis() - player.itemTime.lastTimeUseDK)) / 1000));
        }
        if (player.itemTime.isOpenPower) {
            sendItemTime(player, 3783, (int) ((TIME_OPEN_POWER - (System.currentTimeMillis() - player.itemTime.lastTimeOpenPower)) / 1000));
        }
        if (player.itemTime.isUseMayDo) {
            sendItemTime(player, 2758, (int) ((TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo)) / 1000));
        }
        if (player.itemTime.isUseMayDo2) {
            sendItemTime(player, 2758, (int) ((TIME_MAY_DO - (System.currentTimeMillis() - player.itemTime.lastTimeUseMayDo2)) / 1000));
        }
        if (player.itemTime.isEatMeal) {
            sendItemTime(player, player.itemTime.iconMeal, (int) ((TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal)) / 1000));
        }
        if (player.itemTime.isEatMeal2) {
            sendItemTime(player, player.itemTime.iconMeal2, (int) ((TIME_EAT_MEAL - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal2)) / 1000));
        }
        if (player.itemTime.isCoBonLa) {
            sendItemTime(player, 13618, (int) ((TIME_CO_BON_LA - (System.currentTimeMillis() - player.itemTime.lastTimeCoBonLa)) / 1000));
        }
        if (player.itemTime.isEatMeal3) {
            sendItemTime(player, player.itemTime.iconMeal3, (int) ((TIME_EAT_MEAL3 - (System.currentTimeMillis() - player.itemTime.lastTimeEatMeal3)) / 1000));
        }
        if (player.itemTime.isUseTDLT) {
            sendItemTime(player, 4387, player.itemTime.timeTDLT / 1000);
        }
        if (player.itemTime.isUseRX) {
            sendItemTime(player, 6581, player.itemTime.timeRX / 1000);
        }
        if (player.itemTime.isUseRB) {
            sendItemTime(player, 8581, player.itemTime.timeRB / 1000);
        }
        if (player.itemTime.isUseKilis) {
            long remainingTime = (player.itemTime.lastTimeUseKilis + player.itemTime.timeLengthKilis) - System.currentTimeMillis();
            if (remainingTime > 0) {
                sendItemTime(player, 15359, (int) (remainingTime / 1000));
            }
        }
        if (player.itemTime.ischuotmap) {
            sendItemTime(player, 12831, (int) ((TIME_CHUOT_MAP - (System.currentTimeMillis() - player.itemTime.lastTimechuotmap)) / 1000));
        }
    }

    public void turnOnTDLT(Player player, Item item) {
        int min = 0;
        int minleft = 0;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 1) {
                min = io.param;
                minleft = (min * 60 - 32767) / 60;
                io.param = minleft > 0 ? minleft : 0;
                break;
            }
        }

        player.itemTime.isUseTDLT = true;

        int timeTDLTInMinutes = min - minleft;
        int timeTDLTInSeconds = timeTDLTInMinutes * 60;
        int maxTimeInSeconds = 32767;
        int timeTDLTInSecondsLimited = Math.min(timeTDLTInSeconds, maxTimeInSeconds);

        player.itemTime.timeTDLT = timeTDLTInSecondsLimited * 1000;

        player.itemTime.lastTimeUseTDLT = System.currentTimeMillis();
        sendCanAutoPlay(player);
        sendItemTime(player, 4387, timeTDLTInSecondsLimited);
        InventoryService.gI().sendItemBag(player);
    }

    public void turnOffTDLT(Player player, Item item) {
        player.itemTime.isUseTDLT = false;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 1) {
                io.param += (short) ((player.itemTime.timeTDLT - (System.currentTimeMillis() - player.itemTime.lastTimeUseTDLT)) / 60 / 1000);
                break;
            }
        }
        sendCanAutoPlay(player);
        removeItemTime(player, 4387);
        InventoryService.gI().sendItemBag(player);
    }
    
    public void removeTextTimeSuperNew(Player player) {
        switch (player.gender) {
            case 0:
                sendItemTime(player, 30006 + (player.playerSkill.getSkillbyId(27).point - player.numUseSkill - 1), 5);
                break;
            case 1:
                sendItemTime(player, 30012 + (player.playerSkill.getSkillbyId(28).point - player.numUseSkill - 1), 5);
                break;
            case 2:
                sendItemTime(player, 30000 + (player.playerSkill.getSkillbyId(29).point - player.numUseSkill - 1), 5);
                break;
        }
    }

    public void sendCanAutoPlay(Player player) {
        Message msg;
        try {
            msg = new Message(-116);
            msg.writer().writeByte(player.itemTime.isUseTDLT ? 1 : 0);
            player.sendMessage(msg);
        } catch (IOException e) {
            Logger.logException(ItemTimeService.class, e);
        }
    }
    
    public void sendCanAutoPlayvv(Player player) {
        Message msg;
        try {
            msg = new Message(-116);
            msg.writer().writeByte(player.itemTime.isUsevevang ? 1 : 0);
            player.sendMessage(msg);
        } catch (IOException e) {
            Logger.logException(ItemTimeService.class, e);
        }
    }

    public void sendTextDoanhTrai(Player player) {
        if (player.clan != null && !player.clan.haveGoneDoanhTrai
                && player.clan.lastTimeOpenDoanhTrai != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenDoanhTrai) / 1000);
            int secondsLeft = (RedRibbonHQ.TIME_DOANH_TRAI / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, DOANH_TRAI, "Trại độc nhãn:", secondsLeft);
        }
    }

    public void sendTextTimePickDoanhTrai(Player player) {
        if (player.clan != null && player.clan.doanhTrai != null && player.clan.doanhTrai.isTimePicking) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.doanhTrai.lastTimePick) / 1000);
            int secondsLeft = (RedRibbonHQ.TIME_PICK_DOANH_TRAI / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, DOANH_TRAI, "Trại độc nhãn:", secondsLeft);
        }
    }

    public void sendTextBanDoKhoBau(Player player) {
        if (player.clan != null
                && player.clan.lastTimeOpenBanDoKhoBau != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenBanDoKhoBau) / 1000);
            int secondsLeft = (TreasureUnderSea.TIME_BAN_DO_KHO_BAU / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, BAN_DO_KHO_BAU, "Hang kho báu:", secondsLeft);
        }
    }

    public void sendTextXinbato(Player player) {
        sendTextTime(player, BAN_DO_KHO_BAU, "Tìm nước cho Xinbatô ở đảo Kame hoặc đảo Guru", 30);
    }

    public void sendTextConDuongRanDoc(Player player) {
        if (player.clan != null
                && player.clan.lastTimeOpenConDuongRanDoc != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenConDuongRanDoc) / 1000);
            int secondsLeft = (SnakeWay.TIME_CON_DUONG_RAN_DOC / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, CON_DUONG_RAN_DOC, "Con đường rắn độc:", secondsLeft);
        }
    }

    public void sendTextKhiGasHuyDiet(Player player) {
        if (player.clan != null
                && player.clan.lastTimeOpenKhiGasHuyDiet != 0) {
            int secondPassed = (int) ((System.currentTimeMillis() - player.clan.lastTimeOpenKhiGasHuyDiet) / 1000);
            int secondsLeft = (DestronGas.TIME_KHI_GAS_HUY_DIET / 1000) - secondPassed;
            if (secondsLeft < 0 || secondsLeft > 1800) {
                return;
            }
            sendTextTime(player, KHI_GAS_HUY_DIET, "Khí gas hủy diệt:", secondsLeft);
        }
    }

    public void sendTextTimeKeoBuaBao(Player player, int time) {
        sendTextTime(player, TIME_KEO_BUA_BAO, "Thời gian : ", time);
    }

    public void removeTextDoanhTrai(Player player) {
        removeTextTime(player, DOANH_TRAI);
    }

    public void removeTextBanDoKhoBau(Player player) {
        removeTextTime(player, BAN_DO_KHO_BAU);
    }

    public void removeTextConDuongRanDoc(Player player) {
        removeTextTime(player, CON_DUONG_RAN_DOC);
    }

    public void removeTextKhiGasHuyDiet(Player player) {
        removeTextTime(player, KHI_GAS_HUY_DIET);
    }

    public void removeTextTime(Player player, byte id) {
        sendTextTime(player, id, null, 0);
    }

    public void sendTextTime(Player player, byte id, String text, int seconds) {
        Message msg;
        try {
            msg = new Message(65);
            msg.writer().writeByte(id);
            msg.writer().writeUTF(text == null ? "" : text);
            msg.writer().writeShort(seconds);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendItemTime(Player player, int itemId, int time) {
        Message msg;
        try {
            msg = new Message(-106);
            msg.writer().writeShort(itemId);
            msg.writer().writeShort(time);
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void removeItemTime(Player player, int itemTime) {
        sendItemTime(player, itemTime, 0);
    }
    
    public void removeTextmiNuong(Player player) {
//        removeTextTime(player, MI_NUONG);
    }
    
    public void sendItemTimeBienHinh(Player player, int level) {//sữa icon biến hình ở đây
        int iconLvFirst = player.gender == 0 ? 20427 : player.gender == 1 ? 20437 : 20432;
        int timeIcon = player.effectSkill.timeBienHinh / 1000;
        if (level == 1) {
            sendItemTime(player, iconLvFirst, timeIcon);
        } else {
            removeItemTime(player, iconLvFirst + level - 2);
            sendItemTime(player, iconLvFirst + level - 1, timeIcon);

        }
    }

}
