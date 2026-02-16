package minigame.RubyGemGame;

import DucPro.Functions;
import jdbc.daos.PlayerDAO;
import player.Player;
import server.Client;
import server.Maintenance;
import services.Service;
import services.ItemTimeService;
import item.ItemTime;
import utils.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import jdbc.daos.HistoryTaiXiuDAO;
import java.util.ArrayList;

public class RubyGemGame implements Runnable {

    private static RubyGemGame instance;

    public static RubyGemGame gI() {
        if (instance == null) {
            instance = new RubyGemGame();
        }
        return instance;
    }

    public static final int TAI = 0;
    public static final int XIU = 1;

    public static final int RUBY_SIDE = TAI;
    public static final int GEM_SIDE = XIU;

    public static final int GAME_DURATION = 60 * 1;

    public int timeLeft = GAME_DURATION; // 30 seconds
    public long totalTaiBet = 0;
    public long totalXiuBet = 0;
    public List<BetData> bets = Collections.synchronizedList(new ArrayList<>());
    public List<Integer> history = new ArrayList<>();

    public void run() {
        while (!Maintenance.isRunning) {
            try {
                if (timeLeft > 0) {
                    timeLeft--;
                } else {
                    calculateResult();
                    timeLeft = GAME_DURATION;
                }
                Functions.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        refundAll();
    }

    private void refundAll() {
        synchronized (bets) {
            for (BetData bet : bets) {
                PlayerDAO.addcash(bet.accountId, bet.amount);
                Player player = Client.gI().getPlayerByID((int) bet.playerId);
                if (player != null) {
                    Service.gI().sendThongBao(player,
                            "Bảo trì server! Hoàn trả cược: " + Util.numberToMoney(bet.amount) + " Coin.");
                    if (player.getSession() != null) {
                        player.getSession().cash += bet.amount;
                    }
                }
                HistoryTaiXiuDAO.insert(bet.playerId, bet.playerName, bet.amount, bet.side, -1, "REFUND");
            }
            bets.clear();
        }
    }

    private synchronized void calculateResult() {
        int result;
        if (Util.isTrue(30, 100)) {
            if (totalTaiBet > totalXiuBet) {
                result = XIU;
            } else if (totalXiuBet > totalTaiBet) {
                result = TAI;
            } else {
                result = Util.nextInt(0, 1) == 0 ? TAI : XIU;
            }
        } else {
            result = Util.nextInt(0, 1) == 0 ? TAI : XIU;
        }

        int d1, d2, d3, sum;
        do {
            d1 = Util.nextInt(1, 6);
            d2 = Util.nextInt(1, 6);
            d3 = Util.nextInt(1, 6);
            sum = d1 + d2 + d3;
        } while ((result == TAI && sum <= 9) || (result == XIU && sum > 9));

        history.add(0, result);
        if (history.size() > 20) {
            history.remove(history.size() - 1);
        }

        String resultStr = (result == TAI) ? "Tài" : "Xỉu";
        String announcement = "|7|Kết quả: " + d1 + " + " + d2 + " + " + d3 + " = " + sum + " (" + resultStr + ")\n";

        synchronized (bets) {
            for (BetData bet : bets) {
                if (bet.side == result) {
                    long prize = (long) bet.amount * 2;
                    long fee = prize * 5 / 100;
                    long realPrize = prize - fee;
                    PlayerDAO.addcash(bet.accountId, (int) realPrize);
                    Player player = Client.gI().getPlayerByID((int) bet.playerId);
                    if (player != null) {
                        Service.gI().sendThongBao(player,
                                "Chúc mừng! Bạn thắng cược " + Util.numberToMoney(realPrize) + " Coin.");
                        if (player.getSession() != null) {
                            player.getSession().cash += realPrize;
                        }
                    }
                    HistoryTaiXiuDAO.insert(bet.playerId, bet.playerName, bet.amount, bet.side, result, "WIN");
                } else {
                    Player player = Client.gI().getPlayerByID((int) bet.playerId);
                    if (player != null) {
                        Service.gI().sendThongBao(player, "Rất tiếc! Bạn đã thua cược.");
                    }
                    HistoryTaiXiuDAO.insert(bet.playerId, bet.playerName, bet.amount, bet.side, result, "LOSE");
                }
            }
            bets.clear();
        }
        totalTaiBet = 0;
        totalXiuBet = 0;

        Service.gI().sendThongBaoAllPlayer(announcement);
    }

    public void addBet(Player player, int side, int amount) {
        if (Maintenance.isRunning) {
            Service.gI().sendThongBao(player, "Hệ thống đang bảo trì, không thể cược.");
            return;
        }
        if (player.getSession().cash < amount) {
            Service.gI().sendThongBao(player, "Bạn không đủ Coin để cược.");
            return;
        }
        if (PlayerDAO.subcashNoTieuTien(player, amount)) {
            synchronized (bets) {
                if (Maintenance.isRunning) {
                    PlayerDAO.addcash(player.getSession().userId, amount);
                    Service.gI().sendThongBao(player, "Hệ thống đang bảo trì, hoàn tiền cược.");
                    return;
                }
                bets.add(new BetData(player.getSession().userId, player.id, player.name, side, amount));
                if (side == TAI) {
                    totalTaiBet += amount;
                } else {
                    totalXiuBet += amount;
                }
            }
            Service.gI().sendThongBao(player,
                    "Bạn đã đặt cược " + Util.numberToMoney(amount) + " Coin vào phe " + (side == TAI ? "Tài" : "Xỉu"));
            ItemTimeService.gI().sendTextTime(player, ItemTime.TEXT_TAI_XIU, "Tai Xiu:", timeLeft);
        } else {
            Service.gI().sendThongBao(player, "Giao dịch thất bại. Số dư không đủ.");
        }
    }

    public static class BetData {
        public int accountId;
        public long playerId;
        public String playerName;
        public int side;
        public int amount;

        public BetData(int accountId, long playerId, String playerName, int side, int amount) {
            this.accountId = accountId;
            this.playerId = playerId;
            this.playerName = playerName;
            this.side = side;
            this.amount = amount;
        }
    }
}
