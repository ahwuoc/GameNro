package bot.state;

import bot.Bot;
import bot.BotStatus;
import utils.Util;

/**
 * Trạng thái nghỉ ngơi - chờ respawn
 */
public class RestState implements BotState {

    private static RestState instance;

    public static RestState getInstance() {
        if (instance == null) {
            instance = new RestState();
        }
        return instance;
    }

    @Override
    public void enter(Bot bot) {
        bot.changeStatus(BotStatus.REST);
        bot.setLastTimeRest(System.currentTimeMillis());
    }

    @Override
    public void update(Bot bot) {
        // Kiểm tra thời gian nghỉ đã đủ chưa
        if (Util.canDoWithTime(bot.getLastTimeRest(), bot.getSecondsRest() * 1000L)) {
            bot.changeStatus(BotStatus.RESPAWN);
        }
    }

    @Override
    public void exit(Bot bot) {
        // Cleanup nếu cần
    }

    @Override
    public String getStateName() {
        return "REST";
    }
}
