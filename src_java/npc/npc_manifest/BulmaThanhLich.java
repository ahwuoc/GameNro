package npc.npc_manifest;

import consts.ConstNpc;
import minigame.RubyGemGame.RubyGemGame;
import npc.Npc;
import player.Player;

public class BulmaThanhLich extends Npc {

    public BulmaThanhLich(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (canOpenNpc(pl)) {
            RubyGemGame game = RubyGemGame.gI();
            String npcSay = "|7|Chào mừng đến trò chơi Tài Xỉu (Cược Coin)\n" +
                    "|0|Số dư của bạn: " + utils.Util.numberToMoney(pl.getSession().cash) + " Coin\n" +
                    "|1|Thời gian còn lại: " + String.format("%02d:%02d", game.timeLeft / 60, game.timeLeft % 60) + "\n"
                    +
                    "|2|Bên Tài: " + utils.Util.numberToMoney(game.totalTaiBet) + " Coin\n" +
                    "|4|Bên Xỉu: " + utils.Util.numberToMoney(game.totalXiuBet) + " Coin\n";

            npcSay += "|0|Bạn muốn làm gì?";
            this.createOtherMenu(pl, ConstNpc.BASE_MENU,
                    npcSay,
                    "Lịch sử", "Cược Tài", "Cược Xỉu", "Người cược", "Top Cược", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            switch (select) {
                case 0 -> { // Lịch sử
                    RubyGemGame game = RubyGemGame.gI();
                    String historyStr = "|7|Lịch sử 20 trận gần nhất:\n";
                    if (game.history.isEmpty()) {
                        historyStr += "|0|Chưa có dữ liệu.";
                    } else {
                        for (int i = 0; i < game.history.size(); i++) {
                            historyStr += (i + 1) + ". "
                                    + (game.history.get(i) == RubyGemGame.TAI ? "{{861-1}}" : "{{77-1}}")
                                    + "\n";
                        }
                    }
                    services.Service.gI().sendThongBaoFromAdmin(player, historyStr);
                }
                case 1 -> { // Cược Tài
                    services.func.Input.gI().createFormBetRuby(player, RubyGemGame.TAI);
                }
                case 2 -> { // Cược Xỉu
                    services.func.Input.gI().createFormBetRuby(player, RubyGemGame.XIU);
                }
                case 3 -> { // Người cược
                    RubyGemGame game = RubyGemGame.gI();
                    StringBuilder listStr = new StringBuilder("|7|Danh sách người cược:\n");
                    for (RubyGemGame.BetData bet : game.bets) {
                        if (bet.side == RubyGemGame.TAI) {
                            listStr.append("|2|").append(bet.playerName).append(": ")
                                    .append(utils.Util.numberToMoney(bet.amount)).append("\n");
                        } else {
                            listStr.append("|4|").append(bet.playerName).append(": ")
                                    .append(utils.Util.numberToMoney(bet.amount)).append("\n");
                        }
                    }
                    if (game.bets.isEmpty()) {
                        listStr.append("|0|Chưa có người chơi nào cược.");
                    }
                    services.Service.gI().sendThongBaoFromAdmin(player, listStr.toString());
                }
                case 4 -> { // Top Cược
                    services.Service.gI().sendThongBaoFromAdmin(player, jdbc.daos.HistoryTaiXiuDAO.getTop());
                }
            }
        }
    }
}
