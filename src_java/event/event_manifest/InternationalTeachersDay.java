/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
/**
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
package event.event_manifest;

import consts.ConstNpc;
import event.Event;
import jdbc.daos.EventDAO;
import map.Map;
import nro.models.npc.Npc;
import nro.models.npc.npc_manifest.HaiHoaHong;
import nro.services.MapService;
import utils.Util;

/**
 *
 * @author hoquo
 */
public class InternationalTeachersDay extends Event {

    @Override
    public void init() {
        super.init();
        EventDAO.loadInternationalWomensDayEvent();
    }

    @Override
    public void npc() {
        createNpc(5, 82, 231, 288);
//        createNpc(14, 109, 279, 408);
        createNpc(0, 105, 953, 432);
        createNpc(7, 105, 528, 432);
        createNpc(14, 105, 1066, 408);
        createNpc(5, 105, 927, 408);
        for (int i = 0; i < 5; i++) {
            createNpcHoaHongRandom();
        }

    }

    public void createNpcHoaHongRandom() {

    // Lấy map ngẫu nhiên
    Map mapHoahong = MapService.gI().getMapForHoaHong();
    if (mapHoahong == null) {
        mapHoahong = MapService.gI().getMapById(0); // fallback nếu null
    }

    // ❗ KIỂM TRA MAP ĐÃ CÓ HOA HỒNG CHƯA
    for (Npc npc : mapHoahong.npcs) {
        if (npc instanceof HaiHoaHong) {
            // Map đã có => KHÔNG tạo thêm
            return;
        }
    }

    // Random vị trí trong map
    int cx = Util.nextInt(100, mapHoahong.mapWidth - 100);
    int cy = mapHoahong.yPhysicInTop(cx, 0);

    // Tạo NPC
    int tempId = 105;
    int avatar = 422;

    HaiHoaHong hoaHong = new HaiHoaHong(mapHoahong.mapId, 0, cx, cy, tempId, avatar);
    mapHoahong.npcs.add(hoaHong);
}
}
