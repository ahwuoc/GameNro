package models.TreasureUnderSea;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import utils.Functions;
import boss.Boss;
import boss.boss_manifest.TreasureUnderSea.TrungUyXanhLo;
import clan.Clan;
import map.TrapMap;
import map.Zone;
import mob.Mob;
import nro.player.Player;
import nro.services.ItemTimeService;
import nro.services.MapService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import nro.server.Maintenance;
import nro.services.ClanService;
import nro.services.ItemMapService;
import utils.TimeUtil;

@Data
public class TreasureUnderSea implements Runnable {

    public static final long POWER_CAN_GO_TO_DBKB = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_BAN_DO_KHO_BAU = 1800000;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;
    private boolean kickoutbdkb;
    private long timeKickOutBDKB;
    private Boss boss;
    private long lastTimeSendNotify;
    private boolean allCharactersDead;

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public TreasureUnderSea(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isOpened) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                long elapsedTime = System.currentTimeMillis() - startTime;
                long sleepTime = 150 - elapsedTime;
                if (sleepTime > 0) {
                    Functions.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (isOpened) {
            if (Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU) || (kickoutbdkb && Util.canDoWithTime(timeKickOutBDKB, 60000))) {
                finish();
                dispose();
            }

            allCharactersDead = true;
            for (Zone zone : zones) {

                if (zone.map.mapId == 135) {
                    for (Player pl : zone.getNotBosses()) {
                        sendThanhTichKhoBau(pl);
                        if (pl != null) {
                            TrapMap trap = zone.isInTrap(pl);
                            if (trap != null) {
                                trap.doPlayer(pl);
                            }
                        }
                    }
                }

                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }

                if (allCharactersDead) {
                    for (Player cBoss : zone.getBosses()) {
                        if (!cBoss.isDie()) {
                            allCharactersDead = false;
                            break;
                        }
                    }
                }
            }

            if (!kickoutbdkb && (allCharactersDead || Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU - 60000))) {
                kickoutbdkb = true;
                timeKickOutBDKB = System.currentTimeMillis();
            }

            if (kickoutbdkb && Util.canDoWithTime(lastTimeSendNotify, 10000)) {
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Cái hang này sắp sập rồi, chúng ta phải rời khỏi đây ngay " + TimeUtil.getTimeLeft(timeKickOutBDKB, 60) + " nữa");
                    }
                    lastTimeSendNotify = System.currentTimeMillis();
                }
            }

        }
    }

    public void sendThanhTichKhoBau(Player pl) {
        if (pl == null || pl.clan == null || pl.clan.BanDoKhoBau == null) {
            return;
        }

        long timeDoneKhoBau;
        timeDoneKhoBau = System.currentTimeMillis() - pl.clan.lastTimeOpenBanDoKhoBau;
        int levelDoneKG;
        levelDoneKG = pl.clan.BanDoKhoBau.level;
        if (levelDoneKG > pl.clan.levelDoneBanDoKhoBau) {
            pl.clan.levelDoneBanDoKhoBau = levelDoneKG;
            pl.clan.thoiGianHoanThanhBDKB = timeDoneKhoBau;
        } else if (levelDoneKG == pl.clan.levelDoneBanDoKhoBau) {
            if (timeDoneKhoBau < pl.clan.thoiGianHoanThanhBDKB) {
                pl.clan.thoiGianHoanThanhBDKB = timeDoneKhoBau;
            }
        }
        pl.clan.updatethanhTichBDKB(pl.clan.id);
        pl.clan.updatethanhTichBDKBForLeader();
        pl.clan.updateThongTinLeader3(pl.clan.id);
    }

    public void openBanDoKhoBau(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenBanDoKhoBau = this.lastTimeOpen;
            this.clan.playerOpenBanDoKhoBau = plOpen;
            this.clan.BanDoKhoBau = this;
            this.kickoutbdkb = false;
            this.isOpened = true;
            this.allCharactersDead = false;
            this.init();
            ChangeMapService.gI().goToDBKB(plOpen);
            sendTextBanDoKhoBau();
        } catch (Exception e) {
            plOpen.clan.lastTimeOpenBanDoKhoBau = 0;
            this.dispose();
        }
    }

    private void init() {
        for (Zone zone : this.zones) {

            // Gán dame cho trap
            for (TrapMap trap : zone.trapMaps) {
                trap.dame = this.level * 100_000;
            }

            // Xử lý mob
            for (int i = 0; i < zone.mobs.size(); i++) {
                Mob mob = zone.mobs.get(i);
                boolean isSpecial
                        = (zone.map.mapId == 135 && (i == 5 || i == 10))
                        || (zone.map.mapId == 136 && i == 5)
                        || (zone.map.mapId == 137 && i == 5);

                mob.lvMob = isSpecial ? 1 : 0;

                long dame = (long) level * 31 * 50 * mob.tempId;
                long hp = (long) level * 3107 * 50 * mob.tempId;

                if (isSpecial) {
                    dame *= 10;
                    hp *= 10;
                }

                mob.point.dame = Util.maxIntValue(dame);
                mob.point.maxHp = Util.maxIntValue(hp);

                mob.hoiSinh();
                mob.hoiSinhMobPhoBan();
            }

            // Boss riêng cho map 137
            if (zone.map.mapId == 137) {
                try {
                    long bossDamage = Util.maxIntValue(200_000L * level);
                    long bossMaxHp = Util.maxIntValue(200_000_000L * level);
                    boss = new TrungUyXanhLo(zone, level, bossDamage, bossMaxHp);
                } catch (Exception ignored) {
                }
            }
        }
        Thread.ofVirtual()
                .name("Bản Đồ Kho Báu: " + this.clan.name)
                .start(this);
    }

    //kết thúc bản đồ kho báu
    public void finish() {
        for (Zone zone : zones) {
            for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
                if (i < zone.getPlayers().size()) {
                    Player pl = zone.getPlayers().get(i);
                    kickOutOfBDKB(pl);
                }
            }

        }
    }

    private void kickOutOfBDKB(Player player) {
        if (MapService.gI().isMapBanDoKhoBau(player.zone.map.mapId)) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1038);
            sendThanhTichKhoBau(player);
        }
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void sendTextBanDoKhoBau() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextBanDoKhoBau(pl);
        }
    }

    private void removeTextBanDoKhoBau() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextBanDoKhoBau(pl);
        }
    }

    public void dispose() {
        if (boss != null) {
            this.boss.leaveMap();
        }
        for (Zone zone : zones) {
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        this.removeTextBanDoKhoBau();
        this.allCharactersDead = false;
        this.boss = null;
        this.isOpened = false;
        this.clan.BanDoKhoBau = null;
        this.clan = null;
        this.kickoutbdkb = false;
    }
}
