package matches.pvp;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import utils.Functions;
import matches.PVP;
import matches.TYPE_LOSE_PVP;
import matches.TYPE_PVP;
import nro.player.Enemy;
import nro.player.Player;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class TraThu extends PVP {

    public TraThu(Player p1, Player p2) {
        super(TYPE_PVP.TRA_THU, p1, p2);
    }

    @Override
    public void start() {
        if (!p1.zone.equals(p2.zone)) {
            p1.changeMapVIP = true;
            ChangeMapService.gI().changeMap(
                    p1,
                    p2.zone,
                    p2.location.x + Util.nextInt(-5, 5),
                    p2.location.y
            );
        }

        Service.gI().sendThongBao(p2, "Có người đang đến tìm bạn để trả thù");
        Service.gI().chat(p1, "Mày tới số rồi con ạ!");

        Thread.ofVirtual().name("PVP-Start").start(() -> {
            try {
                Thread.sleep(3000);
                super.start();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void finish() {

    }

    @Override
    public void update() {

    }

    @Override
    public void reward(Player plWin) {

    }

    @Override
    public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
        if (typeLose == TYPE_LOSE_PVP.RUNS_AWAY) {
            Service.gI().sendThongBao(p1.equals(plLose) ? p1 : p2, "Bạn bị xử thua vì đã bỏ chạy");
        }
        if (typeLose == TYPE_LOSE_PVP.DEAD) {
            if (p2.equals(plLose)) {
                for (Enemy pl : p1.enemies) {
                    if (pl.id == p2.id) {
                        p1.enemies.remove(pl);
                        break;
                    }
                }
            }
        }
    }

}
