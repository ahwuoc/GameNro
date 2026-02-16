package boss;

import DucPro.Functions;

import player.Player;
import network.Message;
import services.MapService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import map.Zone;
import server.Maintenance;
import utils.Logger;

public class BossManager implements Runnable {

    private static BossManager instance;
    public static byte ratioReward = 10;

    private final Set<Integer> creatingBossIds = new HashSet<>();

    private final ExecutorService bossExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
                Thread t = new Thread(r, "BossWorker");
                t.setDaemon(true);
                return t;
            });
    private static final long BOSS_UPDATE_TIMEOUT_MS = 500;

    public static BossManager gI() {
        if (instance == null) {
            instance = new BossManager();
        }
        return instance;
    }

    public BossManager() {
        this.ListBoss = new ArrayList<>();
    }

    protected final List<Boss> ListBoss;

    public void addBoss(Boss boss) {
        this.ListBoss.add(boss);
    }

    public void removeBoss(Boss boss) {
        this.ListBoss.remove(boss);
    }

    public List<Boss> getListBoss() {
        return this.ListBoss;
    }

    public void loadBoss() {
        SpecialBossRegistry.init();
        BossDataLoader.gI().loadAll();
        loadBossesFromDatabase();
    }

    private void loadBossesFromDatabase() {
        List<Integer> bossIds = BossDataLoader.gI().getAllBossIds();

        int loadedCount = 0;
        int skippedCount = 0;

        for (int bossId : bossIds) {
            try {
                // Check if boss should auto-spawn
                jdbc.daos.dto.BossDataDTO dto = BossDataLoader.gI().getBossDTO(bossId);
                if (dto != null && !dto.isAutoSpawn()) {
                    skippedCount++;
                    continue;
                }

                Boss boss = BossFactory.createBossFromDatabase(bossId);
                if (boss != null) {
                    loadedCount++;
                }
            } catch (Exception e) {
                Logger.error("BossManager: Failed to load boss " + bossId + ": " + e.getMessage() + "\n");
            }
        }

        Logger.log("BossManager: Loaded " + loadedCount + " bosses, skipped " + skippedCount + " (autoSpawn=false)\n");
    }

    public void reloadBossData() {
        Logger.log("BossManager: Reloading boss data from database...\n");
        BossDataLoader.gI().reload();
        Logger.log("BossManager: Boss data reloaded successfully\n");
    }

    public void createBoss(int bossID, int total) {
        for (int i = 0; i < total; i++) {
            createBoss(bossID);
        }
    }

    public Boss createBoss(int bossID) {
        if (creatingBossIds.contains(bossID)) {
            return null;
        }

        creatingBossIds.add(bossID);
        try {
            if (BossDataLoader.gI().hasBossData(bossID)) {
                return BossFactory.createBossFromDatabase(bossID);
            }
            return null;
        } finally {
            creatingBossIds.remove(bossID);
        }
    }

    public Boss getBoss(int id) {
        try {
            Boss boss = this.ListBoss.get(id);
            if (boss != null) {
                return boss;
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void showListBoss(Player player) {
        player.iDMark.setMenuType(3);
        Message msg;
        try {
            long bossCount = ListBoss.stream()
                    .filter(boss -> !isTaskBoss((int) boss.id)
                            && !MapService.gI().isMapBossFinal(boss.data[0].getMapJoin()[0])
                            && !MapService.gI().isMapHuyDiet(boss.data[0].getMapJoin()[0])
                            && !MapService.gI().isMapYardart(boss.data[0].getMapJoin()[0])
                            && !MapService.gI().isMapMaBu(boss.data[0].getMapJoin()[0])
                            && !MapService.gI().isMapBlackBallWar(boss.data[0].getMapJoin()[0]))
                    .count();

            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Boss Thường (" + bossCount + " con)");
            msg.writer().writeByte((int) bossCount);
            for (int i = 0; i < ListBoss.size(); i++) {
                Boss boss = this.ListBoss.get(i);
                if (isTaskBoss((int) boss.id)
                        || MapService.gI().isMapBossFinal(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapYardart(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapHuyDiet(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapMaBu(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapBlackBallWar(boss.data[0].getMapJoin()[0])) {
                    continue;
                }
                msg.writer().writeInt(i);
                msg.writer().writeInt(i);
                msg.writer().writeShort(boss.data[0].getOutfit()[0]);
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(boss.data[0].getOutfit()[1]);
                msg.writer().writeShort(boss.data[0].getOutfit()[2]);
                msg.writer().writeUTF(boss.data[0].getName());
                if (boss.zone != null) {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(
                            boss.zone.map.mapName + "(" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId + "");
                } else {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(getRespawnTimeText(boss));
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void showListBossNomar(Player player) {
        Message msg;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Boss");
            msg.writer()
                    .writeByte((int) ListBoss.stream()
                            .filter(boss -> !MapService.gI().isMapBossFinal(boss.data[0].getMapJoin()[0])
                                    && !MapService.gI().isMapHuyDiet(boss.data[0].getMapJoin()[0])
                                    && !MapService.gI().isMapYardart(boss.data[0].getMapJoin()[0])
                                    && !MapService.gI().isMapMaBu(boss.data[0].getMapJoin()[0])
                                    && !MapService.gI().isMapBlackBallWar(boss.data[0].getMapJoin()[0]))
                            .count());
            for (int i = 0; i < ListBoss.size(); i++) {
                Boss boss = this.ListBoss.get(i);
                if (MapService.gI().isMapBossFinal(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapYardart(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapHuyDiet(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapMaBu(boss.data[0].getMapJoin()[0])
                        || MapService.gI().isMapBlackBallWar(boss.data[0].getMapJoin()[0])) {
                    continue;
                }
                msg.writer().writeInt(i);
                msg.writer().writeInt(i);
                msg.writer().writeShort(boss.data[0].getOutfit()[0]);
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(boss.data[0].getOutfit()[1]);
                msg.writer().writeShort(boss.data[0].getOutfit()[2]);
                msg.writer().writeUTF(boss.data[0].getName());
                if (boss.zone != null) {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(
                            boss.zone.map.mapName + "(" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId + "");
                } else {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(getRespawnTimeText(boss));
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    /**
     * Tính thời gian boss hồi sinh
     */
    private String getRespawnTimeText(Boss boss) {
        if (boss.zone != null) {
            return boss.zone.map.mapName + "(" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId;
        }

        // Check appearType của level tiếp theo
        int nextLevel = boss.currentLevel + 1;
        if (nextLevel >= boss.data.length) {
            nextLevel = 0;
        }
        AppearType appearType = boss.data[nextLevel].getTypeAppear();

        // Nếu không phải DEFAULT_APPEAR, boss phải chờ boss khác gọi
        if (appearType == AppearType.APPEAR_WITH_ANOTHER) {
            return "Chờ boss chính xuất hiện...";
        } else if (appearType == AppearType.CALL_BY_ANOTHER) {
            return "Chờ boss khác triệu hồi...";
        } else if (appearType == AppearType.ANOTHER_LEVEL) {
            return "Chờ boss chính lên level...";
        }

        long elapsed = System.currentTimeMillis() - boss.getLastTimeRest();
        long remaining = (boss.getSecondsRest() * 1000L) - elapsed;
        if (remaining <= 0) {
            return "Đang hồi sinh...";
        }
        long seconds = remaining / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return String.format("Hồi sinh sau: %dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("Hồi sinh sau: %dm %ds", minutes, seconds % 60);
        } else {
            return String.format("Hồi sinh sau: %ds", seconds);
        }
    }

    private static final int[] TASK_BOSS_IDS = {
            BossID.KUKU, // Task 21.0
            BossID.MAP_DAU_DINH, // Task 21.1
            BossID.RAMBO, // Task 21.2
            BossID.SO_4, // Task 22.0
            BossID.SO_3, // Task 22.1
            BossID.SO_2,
            BossID.SO_1, // Task 22.2
            BossID.TIEU_DOI_TRUONG, // Task 22.3
            BossID.FIDE,
            BossID.FIDE,

            // Task 23.0-2
            BossID.ANDROID_19, // Task 25.1
            BossID.DR_KORE, // Task 25.2
            BossID.ANDROID_15, // Task 26.1
            BossID.ANDROID_14, // Task 26.2
            BossID.ANDROID_13, // Task 26.3
            BossID.PIC, // Task 27.2
            BossID.POC, // Task 27.1
            BossID.KING_KONG, // Task 27.3
            BossID.XEN_BO_HUNG, // Task 28.1-3
            BossID.XEN_CON_1, // Task 29.3
            BossID.XEN_CON_2,
            BossID.XEN_CON_3,
            BossID.XEN_CON_4,
            BossID.XEN_CON_5,
            BossID.XEN_CON_6,
            BossID.XEN_CON_7,
            BossID.SIEU_BO_HUNG, // Task 29.4
            BossID.DRABURA, // Task 30.1, 30.5
            BossID.DRABURA_2,
            BossID.DRABURA_3,
            BossID.BUI_BUI, // Task 30.2, 30.3
            BossID.BUI_BUI_2,
            BossID.YA_CON, // Task 30.4
            BossID.MABU_12H, // Task 30.6
            BossID.BLACK_GOKU, // Task 31.0
            BossID.TRUNG_UY_TRANG // Task 19.1
    };

    /**
     * Check if boss is a task boss
     */
    private boolean isTaskBoss(int bossId) {
        for (int id : TASK_BOSS_IDS) {
            if (id == bossId)
                return true;
        }
        return false;
    }

    /**
     * Show list of task bosses only
     */
    public void showListBossTask(Player player) {
        Message msg;
        try {
            // Filter task bosses
            java.util.List<Boss> taskBosses = ListBoss.stream()
                    .filter(boss -> isTaskBoss((int) boss.id))
                    .collect(java.util.stream.Collectors.toList());

            msg = new Message(-96);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Boss Nhiệm Vụ (" + taskBosses.size() + " con)");
            msg.writer().writeByte(taskBosses.size());

            for (int i = 0; i < taskBosses.size(); i++) {
                Boss boss = taskBosses.get(i);
                int index = ListBoss.indexOf(boss);

                msg.writer().writeInt(index);
                msg.writer().writeInt(index);
                msg.writer().writeShort(boss.data[0].getOutfit()[0]);
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(boss.data[0].getOutfit()[1]);
                msg.writer().writeShort(boss.data[0].getOutfit()[2]);
                msg.writer().writeUTF(boss.data[0].getName());
                if (boss.zone != null) {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(
                            boss.zone.map.mapName + "(" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId);
                } else {
                    msg.writer().writeUTF(boss.bossStatus.toString());
                    msg.writer().writeUTF(getRespawnTimeText(boss));
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.error("showListBossTask error: " + e.getMessage() + "\n");
        }
    }

    public Boss getBossById(int bossId) {
        return this.ListBoss.stream().filter(boss -> boss.id == bossId && !boss.isDie()).findFirst().orElse(null);
    }

    public boolean checkBosses(Zone zone, int BossID) {
        return this.ListBoss.stream()
                .filter(boss -> boss.id == BossID && boss.zone != null && boss.zone.equals(zone) && !boss.isDie())
                .findFirst().orElse(null) != null;
    }

    public Player findBossClone(Player player) {
        return player.zone.getBosses().stream().filter(boss -> boss.id < -100_000_000 && !boss.isDie()).findFirst()
                .orElse(null);
    }

    public Boss getBossById(int bossId, int mapId, int zoneId) {
        return this.ListBoss.stream().filter(boss -> boss.id == bossId && boss.zone != null
                && boss.zone.map.mapId == mapId && boss.zone.zoneId == zoneId && !boss.isDie()).findFirst()
                .orElse(null);
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();

                List<Boss> runningBosses = new ArrayList<>();
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (int i = this.ListBoss.size() - 1; i >= 0; i--) {
                    final Boss boss = this.ListBoss.get(i);
                    runningBosses.add(boss);

                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            long bossStart = System.currentTimeMillis();
                            boss.update();
                            long bossTime = System.currentTimeMillis() - bossStart;
                            if (bossTime > 100) {
                                Logger.warning(
                                        "[BossManager] SLOW: " + boss.id + "(" + boss.name + ") " + bossTime + "ms\n");
                            }
                        } catch (Exception e) {
                            Logger.error("[BossManager] ERROR: " + boss.id + "(" + boss.name + "): " + e.getMessage()
                                    + "\n");
                        }
                    }, bossExecutor);

                    futures.add(future);
                }
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(BOSS_UPDATE_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    StringBuilder stuck = new StringBuilder();
                    for (int i = 0; i < futures.size(); i++) {
                        if (!futures.get(i).isDone()) {
                            Boss b = runningBosses.get(i);
                            stuck.append(b.id).append("(").append(b.name).append(") ");
                        }
                    }
                    Logger.warning("[BossManager] TIMEOUT! Stuck: " + stuck + "\n");
                }
                long elapsed = System.currentTimeMillis() - st;
                if (elapsed < 150) {
                    Functions.sleep(150 - elapsed);
                }

            } catch (Exception e) {
                Logger.error("[BossManager] Critical: " + e.getMessage() + "\n");
            }
        }
        bossExecutor.shutdown();
    }

}
