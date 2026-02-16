package bot;

import boss.AppearType;
import boss.Boss;
import boss.BossManager;
import bot.state.AttackState;
import bot.state.IdleState;
import bot.state.MoveState;
import java.util.List;
import player.Player;
import services.func.ChangeMapService;
import utils.Util;

/**
 * Bot săn boss - tìm và tấn công boss trong map
 */
public class BotFarmBoss extends Bot {

    private static final int ATTACK_RANGE = 20;
    private static final int SEARCH_RANGE = 800;

    private Boss bossTarget;
    private long lastTimeSearchBoss;

    public BotFarmBoss(int id, BotData data) {
        super(id, data);
    }

    @Override
    public void findTarget() {
        if (this.zone == null) {
            return;
        }

        // Tìm boss trong map
        Boss nearestBoss = null;
        int minDistance = Integer.MAX_VALUE;

        // Lấy danh sách boss target từ data (nếu có)
        int[] targetBossIds = data.getTargetBossIds();

        for (Player p : this.zone.getBosses()) {
            if (!(p instanceof Boss boss) || boss.isDie()) {
                continue;
            }

            // Nếu có danh sách target, chỉ nhắm vào boss trong danh sách
            if (targetBossIds != null && targetBossIds.length > 0) {
                boolean isTarget = false;
                for (int targetId : targetBossIds) {
                    if (boss.id == targetId) {
                        isTarget = true;
                        break;
                    }
                }
                if (!isTarget) {
                    continue;
                }
            }

            int distance = Util.getDistance(this.location.x, this.location.y,
                    boss.location.x, boss.location.y);
            if (distance < minDistance && distance <= SEARCH_RANGE) {
                minDistance = distance;
                nearestBoss = boss;
            }
        }

        if (nearestBoss != null) {
            this.bossTarget = nearestBoss;
            this.playerTarget = nearestBoss;

            if (minDistance <= ATTACK_RANGE) {
                changeState(AttackState.getInstance());
            } else {
                changeState(MoveState.getInstance());
            }
            return;
        }

        if (Util.canDoWithTime(lastTimeSearchBoss, 5000)) {
            try {
                List<Boss> bosses = BossManager.gI().getListBoss();
                List<Boss> candidates = new java.util.ArrayList<>();
                for (Boss boss : bosses) {
                    if (boss == null || boss.isDie() || boss.zone == null)
                        continue;

                    if (boss.getData() != null && boss.getData().length > 0) {
                        AppearType type = boss.getData()[0].getTypeAppear();
                        if (type != AppearType.DEFAULT_APPEAR) {
                            continue;
                        }
                    }

                    boolean isValid = true;
                    if (targetBossIds != null && targetBossIds.length > 0) {
                        boolean match = false;
                        for (int id : targetBossIds)
                            if (boss.id == id)
                                match = true;
                        if (!match)
                            isValid = false;
                    }

                    if (isValid) {
                        candidates.add(boss);
                    }
                }

                if (!candidates.isEmpty()) {
                    Boss target = candidates.get(Util.nextInt(0, candidates.size()));
                    if (target.zone.map.mapId != this.zone.map.mapId || target.zone.zoneId != this.zone.zoneId) {
                        ChangeMapService.gI().changeMap(this, target.zone, -1, -1);
                        lastTimeSearchBoss = System.currentTimeMillis();
                        return;
                    }
                }
                lastTimeSearchBoss = System.currentTimeMillis();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.bossTarget = null;
        this.playerTarget = null;
        changeState(IdleState.getInstance());
    }

    @Override
    public void moveToTarget() {
        if (bossTarget != null && !bossTarget.isDie()) {
            int distance = Util.getDistance(this.location.x, this.location.y,
                    bossTarget.location.x, bossTarget.location.y);

            if (distance <= ATTACK_RANGE) {
                changeState(AttackState.getInstance());
            } else {
                moveToPlayer(bossTarget);
            }
        } else {
            this.bossTarget = null;
            this.playerTarget = null;
            changeState(IdleState.getInstance());
        }
    }

    @Override
    public void attack() {
        if (bossTarget == null || bossTarget.isDie()) {
            this.bossTarget = null;
            this.playerTarget = null;
            changeState(IdleState.getInstance());
            return;
        }

        int distance = Util.getDistance(this.location.x, this.location.y,
                bossTarget.location.x, bossTarget.location.y);

        if (distance > ATTACK_RANGE) {
            changeState(MoveState.getInstance());
            return;
        }

        // Tấn công boss
        attackPlayer(bossTarget);
    }
}
