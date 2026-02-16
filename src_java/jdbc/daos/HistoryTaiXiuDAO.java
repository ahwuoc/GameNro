package jdbc.daos;

import jdbc.DBConnecter;
import java.sql.Timestamp;

import jdbc.NDVResultSet;

public class HistoryTaiXiuDAO {
    public static void insert(long playerId, String name, int amount, int side, int result, String status) {
        try {
            DBConnecter.executeUpdate(
                    "INSERT INTO history_taixiu (player_id, name, amount, side, result, status, time) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    playerId, name, amount, side, result, status, new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getTop() {
        StringBuilder sb = new StringBuilder("|7|Bang Xep Hang Tai Xiu\n");
        try {
            NDVResultSet rs = DBConnecter.executeQuery(
                    "SELECT name, SUM(amount) as total FROM history_taixiu WHERE status != 'REFUND' GROUP BY name ORDER BY total DESC LIMIT 10");
            int i = 1;
            while (rs.next()) {
                sb.append(i).append(". ").append(rs.getString("name")).append(": ")
                        .append(utils.Util.numberToMoney(((Number) rs.getObject("total")).longValue()))
                        .append(" Coin\n");
                i++;
            }
            rs.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
