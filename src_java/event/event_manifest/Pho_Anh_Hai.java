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

import boss.BossID;
import consts.ConstNpc;
import event.Event;
import jdbc.daos.EventDAO;
import map.Map;
import nro.models.npc.npc_manifest.HaiHoaHong;
import nro.services.MapService;
import utils.Util;

/**
 *
 * @author hoquo
 */
public class Pho_Anh_Hai extends Event {

    @Override
    public void init() {
        super.init();
        EventDAO.loadInternationalWomensDayEvent();
    }

    @Override
    public void npc() {
        createNpc(0, 87, 1091, 432);
        createNpc(7, 87, 1072, 432);
        createNpc(14, 87, 1091, 408);
    }
}
