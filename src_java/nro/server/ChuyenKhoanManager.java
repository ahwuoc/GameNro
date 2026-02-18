package nro.server;

import com.google.gson.Gson;
import consts.Cmd;
import nro.server.Client;
import nro.services.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import jdbc.DBConnecter;
import jdbc.daos.PlayerDAO;
import models.JsonResponse;
import models.Transaction;
import models.TransactionHistory;
import network.Message;
import nro.player.Player;
import utils.TimeUtil;
import utils.Util;

public class ChuyenKhoanManager {

    public static void InsertTransaction(long playerId, long amount, String description) {
        String sql = """
        INSERT INTO transaction_banking (player_id, amount, description, status, is_recieve, last_time_check, created_date)
        VALUES (?, ?, ?, 0, 0, NULL, NOW());
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, playerId);
            ps.setLong(2, amount);
            ps.setString(3, description);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void ShowTransaction(Player player) {
        List<Transaction> list = GetTransaction((int) player.id);
        Message msg = new Message(Cmd.TOP);
        try {
            msg.writer().writeByte(0);
            msg.writer().writeUTF("Lịch sử giao dịch");
            msg.writer().writeByte(list.size());
            for (int i = 0; i < list.size(); i++) {
                Transaction top = list.get(i);
                msg.writer().writeInt(i + 1);
                msg.writer().writeInt(Math.toIntExact(top.id));
                msg.writer().writeShort(player.getHead());
                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort(player.getBody());
                msg.writer().writeShort(player.getLeg());
                msg.writer().writeUTF(player.name);

                msg.writer().writeUTF(Util.formatCurrency(top.amount));

                msg.writer().writeUTF("Nội dung: " + top.description + "\n"
                        + "Trạng thái giao dịch: " + (top.status ? "Đã thanh toán" : "Chờ thanh toán") + "\n"
                        + "Trạng thái nhận quà " + (top.isReceive ? "Đã nhận quà" : "Chờ nhận quà") + "\n"
                        + "Ngày giao dịch " + (Util.formatLocalDateTime(top.createdDate)) + "\n");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Transaction> GetTransactionAuto() {
        List<Transaction> result = new ArrayList<>();

        String sql = """
        SELECT * FROM transaction_banking
        WHERE status = FALSE
        AND created_date >= NOW() - INTERVAL 60 MINUTE
        ORDER BY created_date DESC;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.id = rs.getLong("id");
                transaction.playerId = rs.getLong("player_id");
                transaction.amount = rs.getLong("amount");
                transaction.description = rs.getString("description");
                transaction.status = rs.getBoolean("status");
                transaction.isReceive = rs.getBoolean("is_recieve");

                Timestamp lastCheck = rs.getTimestamp("last_time_check");
                if (lastCheck != null) {
                    transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                }

                transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();

                result.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static List<Transaction> GetTransactionDoneAuto() {
        List<Transaction> result = new ArrayList<>();

        String sql = """
        SELECT * FROM transaction_banking
        WHERE status = TRUE
        AND is_recieve = FALSE
        AND created_date >= NOW() - INTERVAL 60 MINUTE
        ORDER BY created_date DESC;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.id = rs.getLong("id");
                transaction.playerId = rs.getLong("player_id");
                transaction.amount = rs.getLong("amount");
                transaction.description = rs.getString("description");
                transaction.status = rs.getBoolean("status");
                transaction.isReceive = rs.getBoolean("is_recieve");

                Timestamp lastCheck = rs.getTimestamp("last_time_check");
                if (lastCheck != null) {
                    transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                }

                transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();

                result.add(transaction);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static List<Transaction> GetTransaction(int player_id) {
        List<Transaction> result = new ArrayList<>();

        String sql = """
        SELECT * FROM transaction_banking
        WHERE player_id = ?
        ORDER BY created_date DESC;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, player_id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction transaction = new Transaction();

                    transaction.id = rs.getLong("id");
                    transaction.playerId = rs.getLong("player_id");
                    transaction.amount = rs.getLong("amount");
                    transaction.description = rs.getString("description");
                    transaction.status = rs.getBoolean("status");
                    transaction.isReceive = rs.getBoolean("is_recieve");

                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        transaction.lastTimeCheck = lastCheck.toLocalDateTime();
                    }

                    transaction.createdDate = rs.getTimestamp("created_date").toLocalDateTime();

                    result.add(transaction);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Transaction GetTransactionById(long player_id, int id) {
        Transaction result = null;

        String sql = """
        SELECT * FROM transaction_banking
        WHERE id = ? AND player_id = ?
        ORDER BY created_date DESC
        LIMIT 1;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.setLong(2, player_id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = new Transaction();

                    result.id = rs.getLong("id");
                    result.playerId = rs.getLong("player_id");
                    result.amount = rs.getLong("amount");
                    result.description = rs.getString("description");
                    result.status = rs.getBoolean("status");
                    result.isReceive = rs.getBoolean("is_recieve");

                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        result.lastTimeCheck = lastCheck.toLocalDateTime();
                    }

                    result.createdDate = rs.getTimestamp("created_date").toLocalDateTime();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static Transaction GetTransactionLast(long player_id) {
        Transaction result = null;

        String sql = """
        SELECT * FROM transaction_banking
        WHERE player_id = ?
        ORDER BY created_date DESC
        LIMIT 1;
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, player_id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    result = new Transaction();

                    result.id = rs.getLong("id");
                    result.playerId = rs.getLong("player_id");
                    result.amount = rs.getLong("amount");
                    result.description = rs.getString("description");
                    result.status = rs.getBoolean("status");
                    result.isReceive = rs.getBoolean("is_recieve");

                    Timestamp lastCheck = rs.getTimestamp("last_time_check");
                    if (lastCheck != null) {
                        result.lastTimeCheck = lastCheck.toLocalDateTime();
                    }

                    result.createdDate = rs.getTimestamp("created_date").toLocalDateTime();
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static LocalDateTime GetLastimeCreateTransaction(Player player) {
        LocalDateTime result = null;

        String sql = """
        SELECT created_date 
        FROM transaction_banking 
        WHERE player_id = ? 
        ORDER BY created_date DESC 
        LIMIT 1
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, player.id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp time = rs.getTimestamp("created_date");
                    if (time != null) {
                        result = time.toLocalDateTime();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static LocalDateTime GetLastimeCheckTransaction(Player player) {
        LocalDateTime result = null;

        String sql = """
        SELECT MAX(last_time_check) AS last_time_check 
        FROM transaction_banking 
        WHERE player_id = ?
    """;

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, player.id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp time = rs.getTimestamp("last_time_check");
                    if (time != null) {
                        result = time.toLocalDateTime();
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public static void UpdateLastTimeCheck(long playerId, int transactionId) {
        String sql = "UPDATE transaction_banking SET last_time_check = NOW() WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UpdateGift(long playerId, long transactionId) {
        String sql = "UPDATE transaction_banking SET status = 1, is_recieve = 1 WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UpdateDoneNap(long playerId, long transactionId) {
        String sql = "UPDATE transaction_banking SET status = 1 WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void UpdateDoneRecieve(long playerId, long transactionId) {
        String sql = "UPDATE transaction_banking SET is_recieve = 1 WHERE id = ? AND player_id = ?";

        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, transactionId);
            ps.setLong(2, playerId);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void HandleTransactionAuto() {
        List<Transaction> transactions = GetTransactionAuto();

        if (!transactions.isEmpty()) {
            String history = GetTransactionOnline("https://api.sieuthicode.net/historyapimb/Conchongungoc1@/0000331855198/5c03ffbe2f5d585a0593bf5b9aa31d17");
            JsonResponse response = parseApiResponse(history);

            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                for (TransactionHistory transactionHistory : response.getData()) {
                    for (Transaction transaction : transactions) {
                        if (Double.parseDouble(transactionHistory.getCreditAmount()) == transaction.amount && Util.containsSubstring(transactionHistory.getDescription(), transaction.description)) {
                            UpdateDoneNap(transaction.playerId, transaction.id);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static void HandleTransaction(Player player, int transactionId) {
        boolean canCheck = false;
        LocalDateTime lastTimeCheck = GetLastimeCheckTransaction(player);
        Transaction transaction = null;
        long timeDifference = 0;

        if (lastTimeCheck == null) {
            canCheck = true;
            return;
        } else {
            LocalDateTime now = LocalDateTime.now();
            timeDifference = TimeUtil.calculateTimeDifferenceInSeconds(lastTimeCheck, now);

            if (timeDifference > 10) {
                canCheck = true;
            }

            transaction = GetTransactionById(player.id, transactionId);

            if (transaction != null && (transaction.status || transaction.isReceive)) {
                Service.gI().sendThongBao(player, "Bạn thanh toán giao dịch này và đã nhận được ruby!");
                return;
            }
        }

        if (transaction == null) {
            return;
        }

        if (canCheck) {
            transaction = GetTransactionById(player.id, transactionId);
            String history = GetTransactionOnline("https://api.sieuthicode.net/historyapimb/Conchongungoc1@/0000331855198/5c03ffbe2f5d585a0593bf5b9aa31d17");

            JsonResponse response = parseApiResponse(history);

            UpdateLastTimeCheck(player.id, transactionId);

            if (response != null && response.getData() != null && response.getData().size() > 0) {
                for (TransactionHistory transactionHistory : response.getData()) {
                    if (Double.parseDouble(transactionHistory.getCreditAmount()) == transaction.amount && Util.containsSubstring(transactionHistory.getDescription(), transaction.description)) {
                        double coin = transaction.amount;

                        // Tính toán bonus
                        int bonus = calculateBonus((int) coin);

                        // Cập nhật số tiền nạp và bonus cho người chơi
                        int totalAmount = (int) (coin * 10 + bonus); // Cộng số tiền nạp + bonus

                        // Cập nhật tài khoản người chơi
                        PlayerDAO.addCash(player, totalAmount); // Cộng số tiền vào tài khoản người chơi
                        PlayerDAO.addDaNap(player, totalAmount); // Cộng tổng tiền nạp

                        // Cập nhật các giá trị session cho người chơi
                        player.getSession().cash += totalAmount; // Cộng vào vndBar
                        player.getSession().danap += totalAmount; // Cộng vào tongnap

                        // In ra thông tin giao dịch
                        System.out.println("ADD COIN: " + totalAmount + " + Bonus: " + bonus);

                        // Thông báo cho người chơi về số tiền nhận được (bao gồm cả bonus)
                        Service.gI().sendThongBao(player, "Bạn nhận được tiền là: " + (coin * 10) + " và thưởng: " + bonus);

                        // Cập nhật các phần thưởng khác nếu có
                        UpdateGift(player.id, (long) transactionId);
                        return;
                    }
                }
            }
            Service.gI().sendThongBao(player, "Tài khoản của admin chưa nhận được tiền hoặc bạn chuyển khoản sai nội dung!");
        } else {
            Service.gI().sendThongBao(player, "Bạn cần đợi " + (10 - timeDifference) + " giây nữa để được check giao dịch");
        }
    }

    public static void HandleTransactionAddMoneyAuto() {
        List<Transaction> transactions = GetTransactionDoneAuto();

        if (!transactions.isEmpty()) {
            for (Transaction transaction : transactions) {
                Player player = Client.gI().getPlayer(transaction.getPlayerId());
                if (player != null) {
                    // Tính toán bonus
                    int bonus = calculateBonus((int) transaction.amount);

                    // Cộng số tiền vào tài khoản chính (vnd, tongnap, tongnaptuan)
                    int totalAmount = (int) (transaction.amount * 10 + bonus);

                    // Cập nhật tài khoản người chơi
                    PlayerDAO.addCash(player, totalAmount);
                    PlayerDAO.addDaNap(player, totalAmount);

                    // Cập nhật các giá trị session cho người chơi
                    player.getSession().cash += totalAmount;
                    player.getSession().danap += totalAmount;

                    // In ra số tiền thưởng đã thêm
                    System.out.println("ADD COIN: " + transaction.amount * 10 + " + Thưởng: " + bonus);
                    Service.gI().sendThongBao(player, "Bạn nhận được tiền là: " + (transaction.amount * 10) + " và thưởng: " + bonus);

                    // Cập nhật giao dịch đã hoàn thành
                    UpdateDoneRecieve(player.id, transaction.id);
                }
            }
        }
    }

// Hàm tính toán phần thưởng dựa trên số tiền nạp
    private static int calculateBonus(int amount) {
        int bonus = 0;
        // Xử lý bonus theo tỷ lệ phần trăm cho các mức nạp tiền
        if (amount >= 20000 && amount < 50000) {
            bonus = (int) (amount * 0.02); // Nạp 20k đến dưới 50k, nhận thêm 2%
        } else if (amount >= 50000 && amount < 100000) {
            bonus = (int) (amount * 0.05); // Nạp 50k đến dưới 100k, nhận thêm 5%
        } else if (amount >= 100000 && amount < 200000) {
            bonus = (int) (amount * 0.07); // Nạp 100k đến dưới 200k, nhận thêm 7%
        } else if (amount >= 200000 && amount < 500000) {
            bonus = (int) (amount * 0.10); // Nạp 200k đến dưới 500k, nhận thêm 10%
        } else if (amount >= 500000 && amount < 1000000) {
            bonus = (int) (amount * 0.15); // Nạp 500k đến dưới 1 triệu, nhận thêm 15%
        } else if (amount >= 1000000 && amount < 2000000) {
            bonus = (int) (amount * 0.20); // Nạp 1 triệu đến dưới 2 triệu, nhận thêm 20%
        } else if (amount >= 2000000 && amount < 5000000) {
            bonus = (int) (amount * 0.25); // Nạp 2 triệu đến dưới 5 triệu, nhận thêm 25%
        } else if (amount >= 5000000 && amount < 10000000) {
            bonus = (int) (amount * 0.30); // Nạp 5 triệu đến dưới 10 triệu, nhận thêm 30%
        } else if (amount >= 10000000) {
            bonus = (int) (amount * 0.35); // Nạp từ 10 triệu trở lên, nhận thêm 35%
        }
        return bonus;
    }

    private static String GetTransactionOnline(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                in.close();
                return response.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JsonResponse parseApiResponse(String apiResponse) {
        Gson gson = new Gson();
        return gson.fromJson(apiResponse, JsonResponse.class);
    }
}
