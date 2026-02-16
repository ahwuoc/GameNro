package bot;

import bot.state.IdleState;
import mob.Mob;
import player.Pet;
import utils.Util;

/**
 * Bot farm bằng đệ tử - AFK và để đệ tử đánh trước
 */
public class BotFarmDeTu extends Bot {

    // Maps for De Tu farming
    public static final int[] FARM_MAPS = { 27, 28, 30, 31, 32, 33, 34, 36, 37, 38 };

    private Pet deTu; // Đệ tử của bot
    private boolean afkMode = true;
    private long lastTimeChangeMap;
    private int timeChangeMap = Util.nextInt(60000, 300000);
    private long lastTimeEatPea;
    private int timeEatPea = Util.nextInt(20000, 60000);

    public BotFarmDeTu(int id, BotData data) {
        super(id, data);
    }

    public void setDeTu(Pet deTu) {
        this.deTu = deTu;
        if (deTu != null) {
            deTu.master = this;
        }
    }

    public Pet getDeTu() {
        return deTu;
    }

    @Override
    public void findTarget() {
        if (this.zone == null || deTu == null) {
            return;
        }

        Mob nearestMob = null;
        int minDistance = Integer.MAX_VALUE;

        for (Mob mob : this.zone.mobs) {
            if (mob == null || mob.isDie()) {
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
            commandDeTuAttack(nearestMob);
            if (Util.getDistance(this, nearestMob) > 150) {
                services.PlayerService.gI().playerMove(this, nearestMob.location.x + Util.nextInt(-40, 40),
                        nearestMob.location.y);
            }
        } else {
            this.mobTarget = null;
        }
    }

    /**
     * Ra lệnh cho đệ tử tấn công mob
     */
    protected void commandDeTuAttack(Mob mob) {
        if (deTu == null || deTu.isDie()) {
            return;
        }
        // Force active mode if in Follow
        if (deTu.status == Pet.FOLLOW) {
            byte nextStatus = (byte) (Util.nextInt(0, 2) == 0 ? Pet.PROTECT : Pet.ATTACK);
            deTu.changeStatus(nextStatus);
            String text = (nextStatus == Pet.PROTECT) ? "bao ve" : "tan cong";
            services.Service.gI().chat(deTu, text);
        }
    }

    @Override
    public void afk() {
        if (!afkMode) {
            return;
        }

        if (deTu == null || deTu.isDie()) {
            afkMode = false;
            changeState(IdleState.getInstance());
            return;
        }

        findTarget();
    }

    @Override
    public void idle() {
        if (afkMode && deTu != null && !deTu.isDie()) {
            changeStatus(BotStatus.AFK);
            return;
        }
        super.findTarget();
    }

    @Override
    public void attack() {
        if (afkMode && deTu != null && !deTu.isDie()) {
            return;
        }
        super.attack();
    }

    /**
     * Bật/tắt chế độ AFK
     */
    public void setAfkMode(boolean afkMode) {
        this.afkMode = afkMode;
        if (afkMode) {
            changeStatus(BotStatus.AFK);
        } else {
            changeStatus(BotStatus.IDLE);
            changeState(IdleState.getInstance());
        }
    }

    public boolean isAfkMode() {
        return afkMode;
    }

    @Override
    public void update() {
        super.update();
        if (this.deTu == null && this.pet != null) {
            this.setDeTu(this.pet);
        }
        if (this.deTu != null && this.deTu.zone == null && this.zone != null) {
            this.deTu.joinMapMaster();
        }
        if (this.deTu != null && !this.deTu.isDie() && this.afkMode) {
            if (this.deTu.status == Pet.FOLLOW) {
                commandDeTuAttack(null);
            }
        }
        if (Util.canDoWithTime(lastTimeChangeMap, timeChangeMap)) {
            changeMap();
            lastTimeChangeMap = System.currentTimeMillis();
            timeChangeMap = Util.nextInt(60000, 300000);
        }
        if (Util.canDoWithTime(lastTimeEatPea, timeEatPea)) {
            eatPea();
            lastTimeEatPea = System.currentTimeMillis();
            timeEatPea = Util.nextInt(20000, 60000);
        }
    }

    private void eatPea() {
        if (!isDie()) {
            this.nPoint.setFullHpMp();
            services.Service.gI().sendInfoPlayerEatPea(this);
        }
        if (this.deTu != null && !this.deTu.isDie()) {
            this.deTu.nPoint.setFullHpMp();
            services.Service.gI().sendInfoPlayerEatPea(this.deTu);
        }
    }

    private void changeMap() {
        int mapId = FARM_MAPS[Util.nextInt(0, FARM_MAPS.length - 1)];
        services.func.ChangeMapService.gI().changeMap(this, mapId, -1, 100 + Util.nextInt(-50, 50), 300);
    }

    @Override
    public void die(player.Player plKill) {
        if (deTu != null) {
        }
        super.die(plKill);
    }
}
