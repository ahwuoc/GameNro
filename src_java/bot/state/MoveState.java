package bot.state;

import bot.Bot;
import bot.BotStatus;

/**
 * Trạng thái di chuyển - bot di chuyển tới mục tiêu
 */
public class MoveState implements BotState {

    private static MoveState instance;

    public static MoveState getInstance() {
        if (instance == null) {
            instance = new MoveState();
        }
        return instance;
    }

    @Override
    public void enter(Bot bot) {
        bot.changeStatus(BotStatus.MOVING);
    }

    @Override
    public void update(Bot bot) {
        // Di chuyển tới mục tiêu
        bot.moveToTarget();
    }

    @Override
    public void exit(Bot bot) {
        // Cleanup nếu cần
    }

    @Override
    public String getStateName() {
        return "MOVING";
    }
}
