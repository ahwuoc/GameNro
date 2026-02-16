package Top.weekly;

import player.Player;
import services.Service;
import network.CommandMessage;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import network.Message;



/**
 * Service layer for Weekly Top Race System
 * Handles UI display and user interactions
 */
public class WeeklyTopService {
    private static WeeklyTopService instance;
    private WeeklyTopManager manager;

    private WeeklyTopService() {
        this.manager = WeeklyTopManager.getInstance();
    }

    /**
     * Get singleton instance
     */
    public static WeeklyTopService gI() {
        if (instance == null) {
            synchronized (WeeklyTopService.class) {
                if (instance == null) {
                    instance = new WeeklyTopService();
                }
            }
        }
        return instance;
    }

    /**
     * Display weekly top menu to player
     * 
     * @param player Player to show menu to
     */
    public void showWeeklyTopMenu(Player player) {
        if (player == null) {
            return;
        }

        TopTypeConfig currentType = manager.getCurrentTopType();
        if (currentType == null) {
            Service.gI().sendThongBao(player, "Hệ thống đua top đang bảo trì!");
            return;
        }

        String remainingTime = getRemainingTimeDisplay();
        String info = "=== ĐUA TOP TUẦN ===\n";
        info += "Loại top: " + currentType.name + "\n";
        info += "Thời gian còn lại: " + remainingTime + "\n";
        info += "Chọn tùy chọn bên dưới";

        Service.gI().sendThongBao(player, info);
    }

    /**
     * Display top 10 rankings to player (text format)
     * 
     * @param player Player to show rankings to
     */
    public void showRankings(Player player) {
        if (player == null) {
            return;
        }

        TopTypeConfig currentType = manager.getCurrentTopType();
        if (currentType == null) {
            Service.gI().sendThongBao(player, "Hệ thống đua top đang bảo trì!");
            return;
        }

        List<WeeklyTopEntry> rankings = manager.getTopRankings(10);

        StringBuilder sb = new StringBuilder();
        sb.append("=== TOP 10 ").append(currentType.name).append(" ===\n");

        if (rankings.isEmpty()) {
            sb.append("Chưa có ai tham gia!");
        } else {
            for (WeeklyTopEntry entry : rankings) {
                sb.append(entry.rank).append(". ").append(entry.playerName)
                        .append(" - ").append(entry.points).append("\n");
            }
        }

        Service.gI().sendThongBao(player, sb.toString());
    }

    /**
     * Display top 10 rankings to player (Message format with avatar)
     * 
     * @param player Player to show rankings to
     */
    public void showRankingsWithAvatar(Player player) {
        if (player == null) {
            return;
        }

        TopTypeConfig currentType = manager.getCurrentTopType();
        if (currentType == null) {
            Service.gI().sendThongBao(player, "Hệ thống đua top đang bảo trì!");
            return;
        }

        List<WeeklyTopEntry> rankings = manager.getTopRankings(10);
        System.out.println("[WeeklyTop] Current top type: " + currentType.name + ", Rankings count: " + rankings.size());
        
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0);
        
            msg.writer().writeUTF(currentType.name);
            msg.writer().writeByte(rankings.size());
            System.out.println("[WeeklyTop] Sending " + rankings.size() + " rankings to player " + player.name);

            for (WeeklyTopEntry entry : rankings) {
                msg.writer().writeInt(entry.rank);
                msg.writer().writeInt(entry.rank);
                msg.writer().writeShort(entry.head);

                if (player.getSession().version >= 214) {
                    msg.writer().writeShort(-1);
                }
                msg.writer().writeShort((short) 0); // body
                msg.writer().writeShort((short) 0); // leg
                msg.writer().writeUTF(entry.playerName);
                msg.writer().writeUTF(String.valueOf(entry.points));
                msg.writer().writeUTF("...");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (IOException e) {
            System.err.println("Error showing rankings with avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Display player's current rank and score
     * 
     * @param player Player to show rank for
     */
    public void showPlayerRank(Player player) {
        if (player == null) {
            return;
        }

        TopTypeConfig currentType = manager.getCurrentTopType();
        if (currentType == null) {
            Service.gI().sendThongBao(player, "Hệ thống đua top đang bảo trì!");
            return;
        }

        int rank = manager.getPlayerRank((int) player.id);
        long score = manager.getPlayerScore((int) player.id);

        String message;
        if (rank < 0) {
            message = "Bạn chưa có điểm trong " + currentType.name;
        } else {
            message = "Xếp hạng: " + rank + "\n";
            message += "Điểm: " + score;
        }

        Service.gI().sendThongBao(player, message);
    }

    /**
     * Process reward claim for player
     * 
     * @param player Player claiming reward
     */
    public void processClaimReward(Player player) {
        if (player == null) {
            return;
        }

        // Check if it's Sunday using Vietnam timezone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek != Calendar.SUNDAY) {
            Service.gI().sendThongBao(player, "Chỉ có thể nhận thưởng vào Chủ nhật!");
            return;
        }

        // Check if player can claim
        if (!manager.canClaimReward((int) player.id)) {
            int rank = manager.getPlayerRank((int) player.id);
            if (rank < 0 || rank > 10) {
                Service.gI().sendThongBao(player, "Bạn không nằm trong top 10!");
            } else {
                Service.gI().sendThongBao(player, "Bạn đã nhận thưởng tuần này rồi!");
            }
            return;
        }

        // Get player's rank and reward
        int rank = manager.getPlayerRank((int) player.id);
        WeeklyTopReward reward = manager.getRewardForRank(rank);

        if (reward == null) {
            Service.gI().sendThongBao(player, "Không tìm thấy phần thưởng cho xếp hạng của bạn!");
            return;
        }

        // Give items to player
        boolean success = giveRewardItems(player, reward);

        if (success) {
            // Record claim
            manager.recordClaim((int) player.id, rank);
            Service.gI().sendThongBao(player, "Nhận thưởng thành công!\n" + reward.description);
        } else {
            Service.gI().sendThongBao(player, "Lỗi khi nhận thưởng!");
        }
    }

    /**
     * Give reward items to player
     * 
     * @param player Player to give items to
     * @param reward Reward tier containing items
     * @return true if all items were given successfully
     */
    private boolean giveRewardItems(Player player, WeeklyTopReward reward) {
        try {
            for (RewardItem item : reward.items) {
                System.out.println("Giving item " + item.tempId + " x" + item.quantity + " to player " + player.id);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error giving reward items: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get formatted remaining time display
     * 
     * @return Formatted time string (e.g., "2 ngày 5 giờ")
     */
    public String getRemainingTimeDisplay() {
        long remainingMs = manager.getRemainingTimeInWeek();

        long days = remainingMs / (1000 * 60 * 60 * 24);
        long hours = (remainingMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (remainingMs % (1000 * 60 * 60)) / (1000 * 60);

        if (days > 0) {
            return days + " ngày " + hours + " giờ";
        } else if (hours > 0) {
            return hours + " giờ " + minutes + " phút";
        } else {
            return minutes + " phút";
        }
    }

    /**
     * Get weekly top info string
     * 
     * @return Formatted info string
     */
    public String getWeeklyTopInfo() {
        TopTypeConfig currentType = manager.getCurrentTopType();
        if (currentType == null) {
            return "Hệ thống đua top đang bảo trì!";
        }

        String info = "=== THÔNG TIN ĐUA TOP ===\n";
        info += "Loại top: " + currentType.name + "\n";
        info += "Thời gian còn lại: " + getRemainingTimeDisplay();
        return info;
    }
}
