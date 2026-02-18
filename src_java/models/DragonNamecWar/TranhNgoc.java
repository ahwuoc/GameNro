package models.DragonNamecWar;


import consts.ConstTranhNgocNamek;
import java.util.ArrayList;
import java.util.List;
import nro.player.Player;
import services.func.ChangeMapService;
import utils.TimeUtil;
import utils.Util;

public class TranhNgoc {

    private static TranhNgoc i;
    
    private static long TIME_REGISTER;
    private static long TIME_OPEN;
    private static long TIME_CLOSE;

    private final List<Player> playersRed = new ArrayList<>();
    private final List<Player> playersBlue = new ArrayList<>();

    private int day = -1;

    public static TranhNgoc gI() {
        if (i == null) {
            i = new TranhNgoc();
        }
        i.setTime();
        return i;
    }

    public List<Player> getPlayersBlue() {
        return this.playersBlue;
    }

    public List<Player> getPlayersRed() {
        return this.playersRed;
    }

    public void addPlayersBlue(Player player) {
        synchronized (playersBlue) {
            if (!this.playersBlue.contains(player)) {
                this.playersBlue.add(player);
            }
        }
    }

    public void addPlayersRed(Player player) {
        synchronized (playersRed) {
            if (!this.playersRed.contains(player)) {
                this.playersRed.add(player);
            }
        }
    }

    public void removePlayersBlue(Player player) {
        synchronized (playersBlue) {
            if (this.playersBlue.contains(player)) {
                this.playersBlue.remove(player);
            }
        }
    }

    public void removePlayersRed(Player player) {
        synchronized (playersRed) {
            if (this.playersRed.contains(player)) {
                this.playersRed.remove(player);
            }
        }
    }

    public void setTime() {
        if (i.day == -1 || i.day != TimeUtil.getCurrDay()) {
            i.day = TimeUtil.getCurrDay();
            try {
                TIME_OPEN = TimeUtil.getTime(TimeUtil.getTimeNow("dd/MM/yyyy") + " " + ConstTranhNgocNamek.HOUR_OPEN + ":" + ConstTranhNgocNamek.MIN_OPEN + ":" + 0, "dd/MM/yyyy HH:mm:ss");
                TIME_CLOSE = TimeUtil.getTime(TimeUtil.getTimeNow("dd/MM/yyyy") + " " + ConstTranhNgocNamek.HOUR_CLOSE + ":" + ConstTranhNgocNamek.MIN_CLOSE + ":" + 0, "dd/MM/yyyy HH:mm:ss");
                TIME_REGISTER = TimeUtil.getTime(TimeUtil.getTimeNow("dd/MM/yyyy") + " " + ConstTranhNgocNamek.HOUR_REGISTER + ":" + ConstTranhNgocNamek.MIN_REGISTER + ":" + 0, "dd/MM/yyyy HH:mm:ss");
            } catch (Exception e) {   e.printStackTrace();
            }
        }
    }

    public void update(Player player) {
        try {
            long currentTime = System.currentTimeMillis();
            if (Util.canDoWithTime(player.lastTimeUpdateBallWar, 1000)) {
                player.lastTimeUpdateBallWar = currentTime;
                if (player.zone != null && player.zone.map.mapId == ConstTranhNgocNamek.MAP_ID) {
                    try {
                        if (!isTimeStartWar() || (!player.zone.getPlayersRed().contains(player) && !player.zone.getPlayersBlue().contains(player))) {
                            kickOutOfMap(player);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    if (isTimeStartWar() && (playersBlue.contains(player) || playersRed.contains(player))) {
                        ChangeMapService.gI().changeMapInYard(player, ConstTranhNgocNamek.MAP_ID, -1, -1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void kickOutOfMap(Player player) {
        player.iDMark.setTranhNgoc((byte) -1);
        ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
        player.isHoldNamecBallTranhDoat = false;
        player.tempIdNamecBallHoldTranhDoat = -1;
    }

    public boolean isTimeRegisterWar() {
        long now = System.currentTimeMillis();
        return now > TIME_REGISTER && now < TIME_OPEN;
    }

    public boolean isTimeStartWar() {
        long now = System.currentTimeMillis();
        return now > TIME_OPEN && now < TIME_CLOSE;
    }
}
