package bot;

import bot.state.AttackState;
import bot.state.IdleState;
import bot.state.MoveState;
import mob.Mob;
import utils.Util;

public class BotFarmMob extends Bot {

    private static final int ATTACK_RANGE = 20;

    public BotFarmMob(int id, BotData data) {
        super(id, data);
        this.timeWander = Util.nextInt(1000, 6000000); // Initial random wander time
        this.lastTimeWander = System.currentTimeMillis();
    }

    private long lastTimeWander;
    private int timeWander;
    private boolean isWandering;
    private map.WayPoint targetWayPoint;
    private boolean isChangingMap;
    private long lastTimeChangeMap;

    @Override
    public void findTarget() {
        if (this.zone == null) {
            return;
        }

        if (isChangingMap) {
            if (targetWayPoint != null && this.zone.map.mapId == targetWayPoint.goMap) {
                isChangingMap = false;
                targetWayPoint = null;
                isWandering = false;
                lastTimeWander = System.currentTimeMillis();
                timeWander = Util.nextInt(15000, 600000);
                // System.out.println("[BOT] Bot " + this.id + " successfully arrived at map " +
                // this.zone.map.mapId);
            } else if (System.currentTimeMillis() - lastTimeChangeMap > 10000) {
                isChangingMap = false;
                isWandering = false;
                targetWayPoint = null;
                // System.out.println("[BOT] Bot " + this.id + " map change TIMEOUT.
                // Resetting.");
            } else {
                return;
            }
        }

        if (isWandering) {
            if (Util.getDistance(this.location.x, this.location.y, targetX, targetY) <= 50) {
                if (targetWayPoint != null) {
                    // System.out.println("[BOT] Bot " + this.id + " reached waypoint " +
                    // targetWayPoint.name
                    // + ", triggering map change to " + targetWayPoint.goMap);
                    isChangingMap = true;
                    lastTimeChangeMap = System.currentTimeMillis();
                    services.func.ChangeMapService.gI().changeMap(this, targetWayPoint.goMap, -1, targetWayPoint.goX,
                            targetWayPoint.goY);
                    return;
                }
                isWandering = false;
                return;
            }
            // Continue moving to target
            moveTo(targetX, targetY);
            return;
        }

        if (Util.canDoWithTime(lastTimeWander, timeWander)) {
            if (this.zone.map.wayPoints != null && !this.zone.map.wayPoints.isEmpty()) {
                java.util.List<map.WayPoint> validWayPoints = new java.util.ArrayList<>();
                int[] allowedMaps = null;
                if (this.gender >= 0 && this.gender < BotManager.map.length) {
                    allowedMaps = BotManager.map[this.gender];
                }
                for (map.WayPoint wp : this.zone.map.wayPoints) {
                    if (allowedMaps != null) {
                        boolean isAllowed = false;
                        for (int mapId : allowedMaps) {
                            if (wp.goMap == mapId) {
                                isAllowed = true;
                                break;
                            }
                        }
                        if (isAllowed) {
                            validWayPoints.add(wp);
                        }
                    }
                }

                if (!validWayPoints.isEmpty()) {
                    this.targetWayPoint = validWayPoints.get(Util.nextInt(0, validWayPoints.size() - 1));
                    this.targetX = targetWayPoint.minX;
                    this.targetY = targetWayPoint.minY;
                    this.isWandering = true;
                    moveTo(targetX, targetY);
                    return;
                }
            }
            lastTimeWander = System.currentTimeMillis();
        }

        Mob nearestMob = null;
        int minDistance = Integer.MAX_VALUE;
        int deadCount = 0;

        for (Mob mob : this.zone.mobs) {
            if (mob == null) {
                continue;
            }
            if (mob.isDie()) {
                deadCount++;
                continue;
            }
            int distance = Util.getDistance(this.location.x, this.location.y,
                    mob.location.x, mob.location.y);
            if (distance < minDistance) {
                minDistance = distance;
                nearestMob = mob;
            }
        }

        if (nearestMob != null) {
            this.mobTarget = nearestMob;
            if (minDistance <= ATTACK_RANGE) {
                changeState(AttackState.getInstance());
            } else {
                changeState(MoveState.getInstance());
            }
        } else {
            this.mobTarget = null;
            changeState(IdleState.getInstance());
        }
    }

    @Override
    public void moveToTarget() {
        if (mobTarget != null && !mobTarget.isDie()) {
            int distance = Util.getDistance(this.location.x, this.location.y,
                    mobTarget.location.x, mobTarget.location.y);

            if (distance <= ATTACK_RANGE) {
                changeState(AttackState.getInstance());
            } else {
                moveToMob(mobTarget);
            }
        } else {
            this.mobTarget = null;
            changeState(IdleState.getInstance());
        }
    }

    @Override
    public void attack() {
        if (mobTarget == null || mobTarget.isDie()) {
            this.mobTarget = null;
            findTarget(); // Find new target immediately
            return;
        }

        int distance = Util.getDistance(this.location.x, this.location.y,
                mobTarget.location.x, mobTarget.location.y);

        if (distance > ATTACK_RANGE) {
            changeState(MoveState.getInstance());
            return;
        }

        super.attack();
    }
}
