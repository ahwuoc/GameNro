package bot;

import lombok.Data;

/**
 * Dữ liệu cấu hình cho Bot - tương tự BossData
 */
@Data
public class BotData {

    private String name;
    private byte gender;
    private short[] outfit; // head, body, leg, flagBag, aura, effFront
    private long dame;
    private long[] hp;
    public long mp;
    public int def;
    public int crit;
    public long power;
    private int[] mapJoin;
    private int[][] skillTemp;
    private int secondsRest;
    private BotType botType;

    // Cho BotNPC
    private int targetNpcId;
    private int targetMapId;

    // Cho BotFarmBoss
    private int[] targetBossIds;

    // Explicit outfit parts for easier modification
    private short head;
    private short body;
    private short leg;
    private short flagBag;
    private byte aura;
    private byte effFront;

    public BotData() {
        this.botType = BotType.BASE;
        this.secondsRest = 60;
    }

    public BotData(String name, byte gender, short[] outfit, long dame, long[] hp,
            int[] mapJoin, int[][] skillTemp, int secondsRest, BotType botType) {
        this.name = name;
        this.gender = gender;
        this.outfit = outfit;
        this.dame = dame;
        this.hp = hp;
        this.mapJoin = mapJoin;
        this.skillTemp = skillTemp;
        this.secondsRest = secondsRest;
        this.botType = botType;

        // Unpack outfit
        if (outfit != null) {
            if (outfit.length > 0)
                this.head = outfit[0];
            if (outfit.length > 1)
                this.body = outfit[1];
            if (outfit.length > 2)
                this.leg = outfit[2];
            if (outfit.length > 3)
                this.flagBag = outfit[3];
            if (outfit.length > 4)
                this.aura = (byte) outfit[4];
            if (outfit.length > 5)
                this.effFront = (byte) outfit[5];
        }
    }

    public short getHead() {
        return head;
    }

    public void setHead(short head) {
        this.head = head;
    }

    public short getBody() {
        return body;
    }

    public void setBody(short body) {
        this.body = body;
    }

    public short getLeg() {
        return leg;
    }

    public void setLeg(short leg) {
        this.leg = leg;
    }

    public short getFlagBag() {
        return flagBag;
    }

    public void setFlagBag(short flagBag) {
        this.flagBag = flagBag;
    }

    public byte getAura() {
        return aura;
    }

    public void setAura(byte aura) {
        this.aura = aura;
    }

    public byte getEffFront() {
        return effFront;
    }

    public void setEffFront(byte effFront) {
        this.effFront = effFront;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public short[] getOutfit() {
        return outfit;
    }

    public void setOutfit(short[] outfit) {
        this.outfit = outfit;
        if (outfit != null) {
            if (outfit.length > 0)
                this.head = outfit[0];
            if (outfit.length > 1)
                this.body = outfit[1];
            if (outfit.length > 2)
                this.leg = outfit[2];
            if (outfit.length > 3)
                this.flagBag = outfit[3];
            if (outfit.length > 4)
                this.aura = (byte) outfit[4];
            if (outfit.length > 5)
                this.effFront = (byte) outfit[5];
        }
    }

    public long getDame() {
        return dame;
    }

    public void setDame(long dame) {
        this.dame = dame;
    }

    public long[] getHp() {
        return hp;
    }

    public void setHp(long[] hp) {
        this.hp = hp;
    }

    public int[] getMapJoin() {
        return mapJoin;
    }

    public void setMapJoin(int[] mapJoin) {
        this.mapJoin = mapJoin;
    }

    public int[][] getSkillTemp() {
        return skillTemp;
    }

    public void setSkillTemp(int[][] skillTemp) {
        this.skillTemp = skillTemp;
    }

    public int getSecondsRest() {
        return secondsRest;
    }

    public void setSecondsRest(int secondsRest) {
        this.secondsRest = secondsRest;
    }

    public BotType getBotType() {
        return botType;
    }

    public void setBotType(BotType botType) {
        this.botType = botType;
    }

    public int getTargetNpcId() {
        return targetNpcId;
    }

    public void setTargetNpcId(int targetNpcId) {
        this.targetNpcId = targetNpcId;
    }

    public int getTargetMapId() {
        return targetMapId;
    }

    public void setTargetMapId(int targetMapId) {
        this.targetMapId = targetMapId;
    }

    public int[] getTargetBossIds() {
        return targetBossIds;
    }

    public void setTargetBossIds(int[] targetBossIds) {
        this.targetBossIds = targetBossIds;
    }

    /**
     * Constructor cho BotNPC
     */
    public BotData(String name, byte gender, short[] outfit, long dame, long[] hp,
            int[] mapJoin, int[][] skillTemp, int secondsRest,
            int targetNpcId, int targetMapId) {
        this(name, gender, outfit, dame, hp, mapJoin, skillTemp, secondsRest, BotType.NPC);
        this.targetNpcId = targetNpcId;
        this.targetMapId = targetMapId;
    }

    /**
     * Constructor cho BotFarmBoss
     */
    public BotData(String name, byte gender, short[] outfit, long dame, long[] hp,
            int[] mapJoin, int[][] skillTemp, int secondsRest, int[] targetBossIds) {
        this(name, gender, outfit, dame, hp, mapJoin, skillTemp, secondsRest, BotType.FARM_BOSS);
        this.targetBossIds = targetBossIds;
    }
}
