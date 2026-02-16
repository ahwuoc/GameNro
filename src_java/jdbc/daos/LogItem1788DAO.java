package jdbc.daos;

import jdbc.DBConnecter;
import player.Player;
import java.sql.Timestamp;

public class LogItem1788DAO {

    private static boolean tableExists = false;

    public static void ensureTableExists() {
        if (tableExists) return;
        try {
            DBConnecter.executeUpdate("CREATE TABLE IF NOT EXISTS log_item_1788 (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "player_id BIGINT," +
                    "player_name VARCHAR(100)," +
                    "source VARCHAR(200)," +
                    "quantity INT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");");
            tableExists = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void log(Player pl, String source, int quantity) {
        if (pl == null) return;
        new Thread(() -> {
            try {
                ensureTableExists();
                DBConnecter.executeUpdate("INSERT INTO log_item_1788 (player_id, player_name, source, quantity, created_at) VALUES (?, ?, ?, ?, ?)",
                        pl.id, pl.name, source, quantity, new Timestamp(System.currentTimeMillis()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }
}
