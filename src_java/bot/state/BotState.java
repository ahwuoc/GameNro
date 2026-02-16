package bot.state;

import bot.Bot;

/**
 * Interface cho State Pattern - định nghĩa các trạng thái của bot
 */
public interface BotState {

    /**
     * Được gọi khi bot chuyển sang trạng thái này
     */
    void enter(Bot bot);

    /**
     * Được gọi mỗi frame update của bot
     */
    void update(Bot bot);

    /**
     * Được gọi khi bot rời khỏi trạng thái này
     */
    void exit(Bot bot);

    /**
     * Lấy tên trạng thái
     */
    String getStateName();
}
