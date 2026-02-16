package bot.state;

import bot.Bot;
import bot.BotStatus;

/**
 * Trạng thái tấn công - bot đang tấn công mục tiêu
 */
public class AttackState implements BotState {

    private static AttackState instance;

    public static AttackState getInstance() {
        if (instance == null) {
            instance = new AttackState();
        }
        return instance;
    }

    @Override
    public void enter(Bot bot) {
        bot.changeStatus(BotStatus.ATTACKING);
    }

    @Override
    public void update(Bot bot) {
        // Tấn công mục tiêu
        bot.attack();
    }

    @Override
    public void exit(Bot bot) {
        // Cleanup nếu cần
    }

    @Override
    public String getStateName() {
        return "ATTACKING";
    }
}
