package bot.ibot;

import bot.BotStatus;
import map.Zone;
import mob.Mob;
import npc.Npc;
import player.Player;

/**
 * Interface chính định nghĩa các hành vi cơ bản của Bot
 */
public interface IBot {

    // Lifecycle methods
    void update();

    void initBase();

    void changeStatus(BotStatus status);

    // Combat methods
    Player getPlayerTarget();

    Mob getMobTarget();

    void attack();

    void attackMob(Mob mob);

    void attackPlayer(Player player);

    // Movement methods
    void moveToPlayer(Player player);

    void moveToMob(Mob mob);

    void moveToNpc(Npc npc);

    void moveTo(int x, int y);

    // Map management
    void rest();

    void respawn();

    void joinMap();

    Zone getMapJoin();

    void leaveMap();

    void autoLeaveMap();

    // State methods
    void active();

    void idle();

    void die(Player plKill);

    void afk();
}
