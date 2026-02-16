package bot;

import DucPro.Functions;
import server.Maintenance;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BotManager implements Runnable {

    private static BotManager instance;

    public static BotManager gI() {
        if (instance == null) {
            instance = new BotManager();
        }
        return instance;
    }

    public models.Template.ItemTemplate getRandomOutfitTemplate(int gender) {
        final String[] SHOP_NAMES = { "SANTA_HONG_NGOC", "SANTA_HONG_NGOC" };

        try {
            List<models.Template.ItemTemplate> allOutfits = new ArrayList<>();

            for (shop.Shop shop : server.Manager.SHOPS) {
                if (shop.tagName == null)
                    continue;

                boolean isAllowed = false;
                for (String name : SHOP_NAMES) {
                    if (shop.tagName.equals(name)) {
                        isAllowed = true;
                        break;
                    }
                }

                if (isAllowed) {
                    for (shop.TabShop tab : shop.tabShops) {
                        for (shop.ItemShop item : tab.itemShops) {
                            if (item.temp.type == 5 && (item.temp.gender == gender || item.temp.gender == 3)) {
                                allOutfits.add(item.temp);
                            }
                        }
                    }
                }
            }

            if (!allOutfits.isEmpty()) {
                return allOutfits.get(utils.Util.nextInt(0, allOutfits.size() - 1));
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return null; // Return null if nothing found
    }

    public static final int[][] map = {
            { 0, 1, 2, 3 }, { 7, 8, 9 }, { 14, 15, 16 }
    };

    private final List<Bot> bots;

    // Thread pool cho bot update
    private final ExecutorService botExecutor;
    private static final long BOT_UPDATE_TIMEOUT_MS = 500;

    public BotManager() {
        this.bots = new ArrayList<>();
        this.botExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread t = new Thread(r, "BotWorker");
                    t.setDaemon(true);
                    return t;
                });
    }

    // ==================== Bot Management ====================

    public void addBot(Bot bot) {
        synchronized (bots) {
            this.bots.add(bot);
        }
    }

    public void removeBot(Bot bot) {
        synchronized (bots) {
            this.bots.remove(bot);
        }
    }

    public Bot getBot(int id) {
        synchronized (bots) {
            return this.bots.stream()
                    .filter(bot -> bot.id == id)
                    .findFirst()
                    .orElse(null);
        }
    }

    public List<Bot> getAllBots() {
        synchronized (bots) {
            return new ArrayList<>(bots);
        }
    }

    public List<Bot> getBotsByType(BotType type) {
        synchronized (bots) {
            return this.bots.stream()
                    .filter(bot -> bot.getData().getBotType() == type)
                    .toList();
        }
    }

    // ==================== Factory Methods ====================

    /**
     * Tạo bot farm quái
     */
    public BotFarmMob createBotFarmMob(int id, BotData data) {
        data.setBotType(BotType.FARM_MOB);
        BotFarmMob bot = new BotFarmMob(id, data);
        services.PetService.gI().createNormalPet(bot, data.getGender());
        return bot;
    }

    /**
     * Tạo bot đánh boss
     */
    public BotFarmBoss createBotFarmBoss(int id, BotData data) {
        data.setBotType(BotType.FARM_BOSS);
        BotFarmBoss bot = new BotFarmBoss(id, data);
        services.PetService.gI().createNormalPet(bot, data.getGender());
        return bot;
    }

    /**
     * Tạo bot di chuyển tới NPC
     */
    public BotNPC createBotNPC(int id, BotData data) {
        data.setBotType(BotType.NPC);
        BotNPC bot = new BotNPC(id, data);
        services.PetService.gI().createNormalPet(bot, data.getGender());
        return bot;
    }

    /**
     * Tạo bot farm đệ tử
     */
    public BotFarmDeTu createBotFarmDeTu(int id, BotData data) {
        data.setBotType(BotType.FARM_DE_TU);
        BotFarmDeTu bot = new BotFarmDeTu(id, data);
        services.PetService.gI().createNormalPet(bot, data.getGender());
        return bot;
    }

    // ==================== Update Loop ====================

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();

                List<Bot> runningBots;
                synchronized (bots) {
                    runningBots = new ArrayList<>(bots);
                }

                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (Bot bot : runningBots) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            // long botStart = System.currentTimeMillis();
                            bot.update();
                            // long botTime = System.currentTimeMillis() - botStart;
                            // if (botTime > 100) {
                            // Logger.warning(
                            // "[BotManager] SLOW: " + bot.id + "(" + bot.name + ") " + botTime + "ms\n");
                            // }
                        } catch (Exception e) {
                            Logger.error(
                                    "[BotManager] ERROR: " + bot.id + "(" + bot.name + "): " + e.getMessage() + "\n");
                            e.printStackTrace();
                        }
                    }, botExecutor);

                    futures.add(future);
                }

                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(BOT_UPDATE_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    StringBuilder stuck = new StringBuilder();
                    for (int i = 0; i < futures.size(); i++) {
                        if (!futures.get(i).isDone()) {
                            Bot b = runningBots.get(i);
                            stuck.append(b.id).append("(").append(b.name).append(") ");
                        }
                    }
                }

                long elapsed = System.currentTimeMillis() - st;
                if (elapsed < 150) {
                    Functions.sleep(150 - elapsed);
                }

            } catch (Exception e) {
                Logger.error("[BotManager] Critical: " + e.getMessage() + "\n");
            }
        }

        botExecutor.shutdown();
    }

    public void shutdown() {
        botExecutor.shutdown();
        try {
            if (!botExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                botExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            botExecutor.shutdownNow();
        }
    }
}
