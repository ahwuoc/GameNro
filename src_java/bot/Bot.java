package bot;

import bot.ibot.IBot;
import bot.ibot.IBotOutfit;
import bot.state.BotState;
import bot.state.IdleState;
import bot.state.RestState;
import consts.ConstPlayer;
import map.Zone;
import mob.Mob;
import npc.Npc;
import player.Player;
import services.MapService;
import services.PlayerService;
import services.Service;
import services.SkillService;
import services.func.ChangeMapService;
import skill.Skill;
import utils.Logger;
import utils.SkillUtil;
import utils.Util;

/**
 * Base class cho tất cả Bot - extends Player, implement IBot và IBotOutfit
 */
public class Bot extends Player implements IBot, IBotOutfit {

    protected BotData data;
    protected BotStatus botStatus;
    protected BotState currentState;
    protected Zone lastZone;
    public short petFollowId = -1;
    public short titleId = -1;

    // Timing
    protected long lastTimeRest;
    protected int secondsRest;
    protected long lastTimeAttack;
    protected long lastTimeMove;

    // Targets
    protected Player playerTarget;
    protected Mob mobTarget;
    protected Npc npcTarget;
    protected int targetX;
    protected int targetY;

    private boolean isFirstRespawn = true;

    public Bot(int id, BotData data) {
        this.id = id;
        this.isBoss = false;
        this.isBot = true;
        this.data = data;
        this.secondsRest = data.getSecondsRest();
        this.botStatus = BotStatus.REST;
        this.currentState = RestState.getInstance();
        BotManager.gI().addBot(this);
    }

    @Override
    public void initBase() {
        this.name = data.getName();
        this.gender = data.getGender();

        // Set damage
        this.nPoint.dameg = data.getDame();
        this.nPoint.dame = data.getDame();

        // Set HP
        long[] hpArr = data.getHp();
        this.nPoint.hpg = hpArr[Util.nextInt(0, hpArr.length - 1)];
        this.nPoint.hp = nPoint.hpg;
        this.nPoint.hpMax = nPoint.hpg;

        // Set MP
        long mp = data.getMp() > 0 ? data.getMp() : nPoint.hpg / 2;
        this.nPoint.mpg = mp;
        this.nPoint.mp = mp;
        this.nPoint.mpMax = mp;

        // Set Defense
        int def = data.getDef() > 0 ? data.getDef() : (int) (data.getDame() / 10);
        this.nPoint.defg = def;
        this.nPoint.def = def;

        // Set Critical
        int crit = data.getCrit() > 0 ? data.getCrit() : 10;
        this.nPoint.critg = crit;
        this.nPoint.crit = crit;

        // Set Power
        long power = data.getPower() > 0 ? data.getPower() : (nPoint.hpg + nPoint.dame * 100);
        this.nPoint.power = power;

        this.nPoint.calPoint();
        this.initSkill();
    }

    protected void initSkill() {
        for (Skill skill : this.playerSkill.skills) {
            skill.dispose();
        }
        this.playerSkill.skills.clear();
        this.playerSkill.skillSelect = null;
        int[][] skillTemps = data.getSkillTemp();
        if (skillTemps != null) {
            for (int[] skillTemp : skillTemps) {
                Skill skill = SkillUtil.createSkill(skillTemp[0], skillTemp[1]);
                if (skillTemp.length == 3) {
                    skill.coolDown = skillTemp[2];
                }
                this.playerSkill.skills.add(skill);
            }
        }

    }

    // ==================== State Management ====================

    public void changeState(BotState newState) {
        if (currentState != null) {
            currentState.exit(this);
        }
        currentState = newState;
        if (currentState != null) {
            currentState.enter(this);
        }
    }

    @Override
    public void changeStatus(BotStatus status) {
        this.botStatus = status;
    }

    // ==================== Update Loop ====================

    @Override
    public void update() {
        try {
            super.update();
        } catch (Exception e) {

        }

        if (this.botStatus != BotStatus.REST &&
                this.botStatus != BotStatus.RESPAWN &&
                this.botStatus != BotStatus.JOIN_MAP) {
            if (this.nPoint == null) {

                return;
            }
            this.nPoint.mp = this.nPoint.mpg;

            if (this.nPoint.hp <= 0 && this.botStatus != BotStatus.DIE && this.botStatus != BotStatus.LEAVE_MAP
                    && this.botStatus != BotStatus.REST && this.botStatus != BotStatus.RESPAWN) {
                this.die(null);
                return;
            }

            // Generic Pet Logic: Force Attack/Protect if in Follow mode
            if (this.pet != null && !this.pet.isDie()) {
                if (this.pet.status == player.Pet.FOLLOW) {
                    byte nextStatus = (byte) (utils.Util.nextInt(0, 2) == 0 ? player.Pet.PROTECT : player.Pet.ATTACK);
                    this.pet.changeStatus(nextStatus);
                    String text = (nextStatus == player.Pet.PROTECT) ? "bao ve" : "tan cong";
                    services.Service.gI().chat(this.pet, text);
                }
            }

            if (this.effectSkill != null && this.effectSkill.isHaveEffectSkill()) {
                return;
            }
        }

        // State machine update
        if (currentState != null) {
            currentState.update(this);
        }

        // Status-based update - MUST run

        switch (this.botStatus) {
            case REST -> rest();
            case RESPAWN -> {
                respawn();
                changeStatus(BotStatus.JOIN_MAP);
                this.currentState = null;
            }
            case JOIN_MAP -> joinMap();
            case IDLE -> idle();
            case MOVING -> moveToTarget();
            case ATTACKING -> attack();
            case DIE -> onDeath();
            case LEAVE_MAP -> leaveMap();
            case AFK -> afk();
        }
    }

    // ==================== Lifecycle Methods ====================

    @Override
    public void rest() {
        long timeElapsed = System.currentTimeMillis() - lastTimeRest;

        if (Util.canDoWithTime(lastTimeRest, secondsRest * 1000L)) {

            changeStatus(BotStatus.RESPAWN);
        }
    }

    @Override
    public void respawn() {
        initBase();
        changeToTypeNonPK();

        if (isFirstRespawn) {
            isFirstRespawn = false;
            this.zone = getMapJoin();
            // System.out.println("[BOT] " + this.name + " initial spawn at Map "
            // + (this.zone != null ? this.zone.map.mapId : "NULL"));
        } else {
            int homeMapId = 21 + this.gender;
            this.zone = MapService.gI().getMapWithRandZone(homeMapId);
            if (this.zone == null) {
                this.zone = getMapJoin();
            }
            // System.out.println("[BOT] " + this.name + " respawned at Home Map "
            // + (this.zone != null ? this.zone.map.mapId : "NULL"));
        }
    }

    @Override
    public Zone getMapJoin() {
        int[] mapJoinArr = data.getMapJoin();
        if (mapJoinArr == null || mapJoinArr.length == 0) {
            return null;
        }
        int mapId = mapJoinArr[Util.nextInt(0, mapJoinArr.length - 1)];
        return MapService.gI().getMapWithRandZone(mapId);
    }

    @Override
    public void joinMap() {
        if (this.zone == null) {
            Zone newZone = lastZone != null ? lastZone : getMapJoin();

            this.zone = newZone;
        }

        if (this.zone != null) {
            try {
                int x = this.zone.map.mapWidth > 100
                        ? Util.nextInt(100, this.zone.map.mapWidth - 100)
                        : Util.nextInt(100);
                int y = this.zone.map.yPhysicInTop(x, 100);
                ChangeMapService.gI().changeMap(this, this.zone, x, y);
                if (this.petFollowId != -1) {
                    services.Service.gI().sendPetFollow(this, this.petFollowId);
                }
                if (this.titleId != -1) {
                    services.Service.gI().sendTitle(this, this.titleId);
                }
                Service.gI().sendFlagBag(this);
                changeStatus(BotStatus.IDLE);
                changeState(IdleState.getInstance());

                if (this.pet != null) {
                    this.pet.changeStatus((byte) Util.nextInt(1, 2));
                }
            } catch (Exception e) {
                Logger.error("Bot.joinMap error: " + e.getMessage() + "\n");
                e.printStackTrace();
                changeStatus(BotStatus.REST);
            }
        } else {
            // System.out.println("[BOT-DEBUG] Zone is NULL for bot " + this.id + ".
            // mapJoin="
            // + java.util.Arrays.toString(data.getMapJoin()) + ". Retrying RESPAWN.");
            changeStatus(BotStatus.RESPAWN);
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        changeStatus(BotStatus.REST);
        changeState(RestState.getInstance());
    }

    @Override
    public void autoLeaveMap() {
    }

    // ==================== Combat Methods ====================

    @Override
    public void active() {
        if (this.isDie()) {
            return;
        }
        if (this.typePk == ConstPlayer.NON_PK) {
            changeToTypePK();
        }
        attack();
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
            this.lastTimeAttack = System.currentTimeMillis();

            if (mobTarget != null && !mobTarget.isDie()) {
                attackMob(mobTarget);
                return;
            }

            if (this.typePk == ConstPlayer.PK_ALL && playerTarget != null && !playerTarget.isDie()) {
                attackPlayer(playerTarget);
            }
        }
    }

    @Override
    public void attackMob(Mob mob) {
        if (mob == null || mob.isDie()) {
            // System.out.println("[BOT-DEBUG] Bot " + this.id + " attackMob: mob null or
            // dead");
            return;
        }

        if (this.playerSkill != null && this.playerSkill.skills != null && !this.playerSkill.skills.isEmpty()) {
            int disToMob = Util.getDistance(this, mob);

            if (disToMob <= 50) {

                this.playerSkill.skillSelect = this.playerSkill.skills.get(0);
                if (Util.getDistance(this, mob) > 50) {
                    PlayerService.gI().playerMove(this, mob.location.x + Util.nextInt(-20, 20), mob.location.y);
                }
                SkillService.gI().useSkill(this, null, mob, -1, null);
            } else {
                if (this.playerSkill.skills.size() > 1) {
                    this.playerSkill.skillSelect = this.playerSkill.skills.get(1);
                    SkillService.gI().useSkill(this, null, mob, -1, null);
                } else {
                    this.playerSkill.skillSelect = this.playerSkill.skills.get(0);
                    PlayerService.gI().playerMove(this, mob.location.x + Util.nextInt(-20, 20), mob.location.y);
                    SkillService.gI().useSkill(this, null, mob, -1, null);
                }
            }
        }
    }

    @Override
    public void attackPlayer(Player player) {
        if (player == null || player.isDie()) {
            return;
        }
        if (!this.playerSkill.skills.isEmpty()) {
            this.playerSkill.skillSelect = this.playerSkill.skills
                    .get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
            SkillService.gI().useSkill(this, player, null, -1, null);
        }
    }

    @Override
    public Player getPlayerTarget() {
        return this.playerTarget;
    }

    @Override
    public Mob getMobTarget() {
        return this.mobTarget;
    }

    // ==================== Movement Methods ====================

    @Override
    public void moveTo(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(40, 60);
        PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move),
                y + (Util.isTrue(3, 10) ? -50 : 0));
    }

    @Override
    public void moveToPlayer(Player player) {
        if (player != null && player.location != null) {
            moveTo(player.location.x, player.location.y);
        }
    }

    @Override
    public void moveToMob(Mob mob) {
        if (mob != null) {
            moveTo(mob.location.x, mob.location.y);
        }
    }

    @Override
    public void moveToNpc(Npc npc) {
        if (npc != null) {
            moveTo(npc.cx, npc.cy);
        }
    }

    /**
     * Di chuyển tới mục tiêu hiện tại - được gọi bởi MoveState
     */
    public void moveToTarget() {
        if (mobTarget != null && !mobTarget.isDie()) {
            moveToMob(mobTarget);
        } else if (playerTarget != null && !playerTarget.isDie()) {
            moveToPlayer(playerTarget);
        } else if (npcTarget != null) {
            moveToNpc(npcTarget);
        }
    }

    // ==================== State Callbacks ====================

    @Override
    public void idle() {
        findTarget();
    }

    public void findTarget() {
    }

    @Override
    public void afk() {
    }

    @Override
    public void die(Player plKill) {
        changeStatus(BotStatus.DIE);
    }

    public void onDeath() {
        ChangeMapService.gI().spaceShipArrive(this, (byte) 2, ChangeMapService.DEFAULT_SPACE_SHIP);
        leaveMap();
    }

    // ==================== PK Type Methods ====================

    public void changeToTypePK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_ALL);
    }

    public void changeToTypeNonPK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.NON_PK);
    }

    // ==================== Outfit Methods ====================

    @Override
    public short getHead() {
        return data.getHead();
    }

    @Override
    public short getBody() {
        return data.getBody();
    }

    @Override
    public short getLeg() {
        return data.getLeg();
    }

    @Override
    public short getFlagBag() {
        return data.getFlagBag();
    }

    @Override
    public byte getAura() {
        return data.getAura();
    }

    @Override
    public byte getEffFront() {
        return data.getEffFront();
    }

    // ==================== Getters/Setters ====================

    public long getLastTimeRest() {
        return lastTimeRest;
    }

    public void setLastTimeRest(long lastTimeRest) {
        this.lastTimeRest = lastTimeRest;
    }

    public int getSecondsRest() {
        return secondsRest;
    }

    public BotData getData() {
        return data;
    }

    public BotStatus getBotStatus() {
        return botStatus;
    }

    public BotState getCurrentState() {
        return currentState;
    }
}
