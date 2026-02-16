package bot.state;

import bot.Bot;
import bot.BotStatus;

/**
 * Trạng thái nghỉ ngơi - bot đứng yên, tìm mục tiêu
 */
public class IdleState implements BotState {

    private static IdleState instance;

    public static IdleState getInstance() {
        if (instance == null) {
            instance = new IdleState();
        }
        return instance;
    }

    @Override
    public void enter(Bot bot) {
        bot.changeStatus(BotStatus.IDLE);
    }

    @Override
    public void update(Bot bot) {
        // Tìm mục tiêu dựa trên loại bot
        bot.findTarget();
    }

    @Override
    public void exit(Bot bot) {
        // Cleanup nếu cần
    }

    @Override
    public String getStateName() {
        return "IDLE";
    }
}
