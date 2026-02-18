package services.func;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import map.EffectEventManager;
import map.EffectEventTemplate;
import map.Zone;
import nro.player.Player;
import network.Message;
import nro.server.Manager;
import nro.services.Service;

public class EffectMapService {

    private static EffectMapService i;

    private EffectMapService() {

    }

    public void sendEffEvent(Player pl) {
        int plmapid = pl.zone.map.mapId;
        for (EffectEventTemplate eff : EffectEventManager.gI().getList()) {
            if (Manager.ACTIVE_EVENTS.contains(eff.getEventId())) {
                if (plmapid == eff.getMapId()) {
                    EffectMapService.gI().sendEffectMapToPlayer(pl,
                            eff.getEffId(),
                            eff.getLayer(),
                            eff.getLoop(),
                            eff.getX(),
                            eff.getY(),
                            eff.getDelay());
                }
            }
        }
    }

    public static EffectMapService gI() {
        if (i == null) {
            i = new EffectMapService();
        }
        return i;
    }

    public void sendEffectMapToPlayer(Player player, int id, int layer, int loop, int x, int y, int delay) {
        Message msg = null;
        try {
            msg = new Message(113);
            msg.writer().writeByte(id);
            msg.writer().writeByte(layer);
            msg.writer().writeByte(id);
            msg.writer().writeShort(x);
            msg.writer().writeShort(y);
            msg.writer().writeShort(delay);
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void sendEffectMapToAllInMap(Zone zone, int id, int layer, int loop, int x, int y, int delay) {
        Message msg = null;
        try {
            msg = new Message(113);
            msg.writer().writeByte(loop);
            msg.writer().writeByte(layer);
            msg.writer().writeByte(id);
            msg.writer().writeShort(x);
            msg.writer().writeShort(y);
            msg.writer().writeShort(delay);
            Service.gI().sendMessAllPlayerInMap(zone, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void sendEffectMapToAllInMap(Player player, int id, int layer, int loop, int x, int y, int delay) {
        Message msg = null;
        try {
            msg = new Message(113);
            msg.writer().writeByte(loop);
            msg.writer().writeByte(layer);
            msg.writer().writeByte(id);
            msg.writer().writeShort(x);
            msg.writer().writeShort(y);
            msg.writer().writeShort(delay);
            Service.gI().sendMessAllPlayerInMap(player, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

}
