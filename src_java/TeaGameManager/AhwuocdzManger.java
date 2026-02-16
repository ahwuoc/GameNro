package TeaGameManager;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jdbc.daos.PlayerDAO;
import player.Player;

import server.Client;
import utils.Logger;

public class AhwuocdzManger {

    private static AhwuocdzManger instance = null;

    public static synchronized AhwuocdzManger getInstance() {
        if (instance == null) {
            instance = new AhwuocdzManger();
        }
        return instance;
    }

    private ScheduledExecutorService scheduler;

    public void startAutoSave() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                handleAutoSave();
            } catch (Exception e) {
                System.out.println("[AutoSaveManager] start autosave error: " + e.getLocalizedMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void handleAutoSave() {
        long st = System.currentTimeMillis();
        List<Player> players = Client.gI().getPlayers();

        if (players.isEmpty()) {
            return;
        }

        int saved = PlayerDAO.updatePlayersBatch(players);

        Logger.success("Auto-saved " + saved + "/" + players.size() + " players in " + (System.currentTimeMillis() - st)
                + "ms\n");
    }

}
