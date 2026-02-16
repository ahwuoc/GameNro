package bot.state;

import bot.Bot;
import bot.BotStatus;

/**
 * Trạng thái chết - xử lý cleanup sau khi bot chết
 */
public class DieState implements BotState {

    private static DieState instance;

    public static DieState getInstance() {
        if (instance == null) {
            instance = new DieState();
        }
        return instance;
    }

    @Override
    public void enter(Bot bot) {
        bot.changeStatus(BotStatus.DIE);
    }

    @Override
    public void update(Bot bot) {
        // Xử lý sau khi chết, chuyển sang trạng thái nghỉ
        bot.onDeath();
    }

    @Override
    public void exit(Bot bot) {
        // Cleanup nếu cần
    }

    @Override
    public String getStateName() {
        return "DIE";
    }
}
