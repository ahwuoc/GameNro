package models.ShenronEvent_NOEL;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import models.ShenronEvent_NOEL.*;
import consts.ConstNpc;
import item.Item;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.NpcService;
import nro.services.Service;
import utils.Util;

public class ShenronEventServicenoel {

    private static ShenronEventServicenoel instance;

    public static final short NGOC_RONG_1_SAO = 925;
    public static final short NGOC_RONG_2_SAO = 926;
    public static final short NGOC_RONG_3_SAO = 927;
    public static final short NGOC_RONG_4_SAO = 928;
    public static final short NGOC_RONG_5_SAO = 929;
    public static final short NGOC_RONG_6_SAO = 930;
    public static final short NGOC_RONG_7_SAO = 931;

    public static ShenronEventServicenoel gI() {
        if (instance == null) {
            instance = new ShenronEventServicenoel();
        }
        return instance;
    }

    public void openMenuSummonShenron(Player pl, int type) {
        pl.iDMark.setShenronType(type);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.SUMMON_SHENRON_EVENT_NOEL, -1, "Bạn có muốn gọi Rồng Băng không ?",
                "Đồng ý", "Từ chối");
    }

    public void summonShenron(Player player) {
        if (player.zone.map.mapId != 0 && player.zone.map.mapId != 7 && player.zone.map.mapId != 14) {
            if (checkShenronBall(player)) {
                if (player.isShenronAppear || player.shenronEventnoel != null) {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                    return;
                }

                if (Util.canDoWithTime(player.lastTimeShenronAppeared, ShenronEventnoel.timeResummonShenron)) {
                    for (int i = NGOC_RONG_1_SAO; i <= NGOC_RONG_7_SAO; i++) {
                        try {
                            InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, i), 1);
                        } catch (Exception ex) {
                        }
                    }
                    InventoryService.gI().sendItemBag(player);
                    ShenronEventnoel shenron = new ShenronEventnoel();
                    shenron.setPlayer(player);
                    ShenronEventManagernoel.gI().add(shenron);
                    player.shenronEventnoel = shenron;
                    shenron.setZone(player.zone);
                    shenron.activeShenronoel(true, ShenronEventnoel.DRAGON_EVENT_NOEL);
                    shenron.sendWhishesShenron();
                } else {
                    int timeLeft = (int) ((ShenronEventnoel.timeResummonShenron - (System.currentTimeMillis() - player.lastTimeShenronAppeared)) / 1000);
                    Service.gI().sendThongBao(player, "Vui lòng đợi " + (timeLeft < 7200 ? timeLeft + " giây" : timeLeft / 60 + " phút") + " nữa");
                }
            }
        } else {
            Service.gI().sendThongBao(player, "Không thể gọi rồng ở đây");
        }
    }

    private boolean checkShenronBall(Player pl) {
        for (int i = NGOC_RONG_1_SAO; i <= NGOC_RONG_7_SAO; i++) {
            if (!InventoryService.gI().isExistItemBag(pl, i)) {
                Item it = ItemService.gI().createNewItem((short) i);
                Service.gI().sendThongBao(pl, "Bạn còn thiếu 1 viên " + it.template.name);
                return false;
            }
        }
        return true;
    }
}
