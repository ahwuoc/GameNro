package bot;

import bot.state.IdleState;
import map.Zone;
import npc.Npc;
import services.MapService;
import services.Service;
import services.func.ChangeMapService;
import utils.Util;

public class BotNPC extends Bot {

    private static final int NPC_INTERACT_RANGE = 50;
    private static final int MAP_TARGET = 5;

    private boolean reachedTarget = false;
    private long lastTimeInteract;
    private int timeInteract;

    private long lastTimeChangeZone;
    private int timeChangeZone;

    private long lastTimeGoHome;
    private int timeGoHome;

    private boolean isAtHome;
    private boolean isInteracting;

    public BotNPC(int id, BotData data) {
        super(id, data);
        this.timeChangeZone = Util.nextInt(30000, 600000); // 30-60s change zone
        this.lastTimeChangeZone = System.currentTimeMillis();

        this.timeGoHome = Util.nextInt(60000, 1200000); // 1-2m go home
        this.lastTimeGoHome = System.currentTimeMillis() + Util.nextInt(60000, 1200000); // Offset start
    }

    @Override
    public Zone getMapJoin() {
        return MapService.gI().getMapWithRandZone(MAP_TARGET);
    }

    @Override
    public void findTarget() {
        if (this.zone == null)
            return;
        if (this.zone.map.mapId != MAP_TARGET && !isAtHome) {
            isAtHome = true;
            lastTimeGoHome = System.currentTimeMillis();
            timeGoHome = Util.nextInt(5000, 15000);
            return;
        }

        if (isAtHome) {
            if (Util.canDoWithTime(lastTimeGoHome, timeGoHome)) {
                returnToTargetMap();
            }
            return;
        }

        // 2. Check if we should go home
        if (Util.canDoWithTime(lastTimeGoHome, 120000 + Util.nextInt(60000))) { // Every ~2-3 mins
            goHome();
            return;
        }

        // 3. Check if we should change zone (only if in MAP_TARGET)
        if (this.zone.map.mapId == MAP_TARGET && Util.canDoWithTime(lastTimeChangeZone, timeChangeZone)) {
            changeZone();
            return;
        }

        // 4. Handle Interaction (Waiting at NPC)
        if (isInteracting) {
            if (Util.canDoWithTime(lastTimeInteract, timeInteract)) {
                isInteracting = false;
                reachedTarget = false;
                this.npcTarget = null; // Done interacting, find new target
            } else {
                return; // Still interacting
            }
        }

        // 5. Find new NPC target if needed
        if (this.npcTarget == null) {
            if (this.zone.map.npcs != null && !this.zone.map.npcs.isEmpty()) {
                Npc randomNpc = this.zone.map.npcs.get(Util.nextInt(this.zone.map.npcs.size()));
                if (randomNpc != null) {
                    this.npcTarget = randomNpc;

                }
            }
        }

        // 6. Move to NPC
        if (this.npcTarget != null) {
            int distance = Util.getDistance(this.location.x, this.location.y, npcTarget.cx, npcTarget.cy);
            if (distance <= NPC_INTERACT_RANGE) {
                if (!reachedTarget) {
                    reachedTarget = true;
                    onReachNPC(npcTarget);
                }
            } else {
                moveToNpc(npcTarget);
            }
        }
    }

    private void goHome() {

        int homeMapId = 21 + this.gender;
        ChangeMapService.gI().changeMapInYard(this, homeMapId, -1, -1);

        isAtHome = true;
        isInteracting = false;
        reachedTarget = false;
        npcTarget = null;

        lastTimeGoHome = System.currentTimeMillis();
        timeGoHome = Util.nextInt(10000, 30000); // Stay home for 10-30s

        // System.out.println("[BotNPC] " + this.id + " went HOME (Map " + homeMapId +
        // ")");
    }

    private void returnToTargetMap() {
        ChangeMapService.gI().changeMapInYard(this, MAP_TARGET, -1, -1);

        isAtHome = false;
        lastTimeGoHome = System.currentTimeMillis(); // Reset timer for next home trip

        // System.out.println("[BotNPC] " + this.id + " returned to TARGET (Map " +
        // MAP_TARGET + ")");
    }

    private void changeZone() {
        ChangeMapService.gI().changeMap(this, this.zone.map.mapId, -1, -1, 5);
        this.lastTimeChangeZone = System.currentTimeMillis();
        this.timeChangeZone = Util.nextInt(30000, 60000);

        this.npcTarget = null;
        this.reachedTarget = false;
        this.isInteracting = false;
    }

    protected void onReachNPC(Npc npc) {
        if (npc.tempId == consts.ConstNpc.SANTA) {
            models.Template.ItemTemplate item = BotManager.gI().getRandomOutfitTemplate(this.data.getGender());
            if (item != null) {
                this.data.setHead((short) item.head);
                this.data.setBody((short) item.body);
                this.data.setLeg((short) item.leg);
                Service.gI().Send_Caitrang(this);
            }
        }

        isInteracting = true;
        lastTimeInteract = System.currentTimeMillis();
        timeInteract = Util.nextInt(3000, 8000);
        changeState(IdleState.getInstance());
    }

    @Override
    public void idle() {
        if (!reachedTarget && !isAtHome) {
            findTarget();
        }
    }
}
