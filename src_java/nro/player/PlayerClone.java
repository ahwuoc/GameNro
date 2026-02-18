package nro.player;

import java.util.ArrayList;
import mob.Mob;
import network.Message;
import static nro.player.Pet.PROTECT;
import nro.services.InventoryService;
import nro.services.MapService;
import nro.services.PlayerService;
import nro.services.Service;
import nro.services.SkillService;
import services.func.ChangeMapService;
import skill.PlayerSkill;
import skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class PlayerClone extends Player {

    public Player master;
    private long lastSpawnTime = 0;
    private long lastTimeDie = 0;
    private byte status;
    private long lastTimeUnfusion;
    private long lastTimeAskPea;

    public PlayerClone(Player master) {
        super();
        this.master = master;
        this.isClone = true;
        this.id = master.id - 5;
        this.gender = master.gender;
        this.name = master.name;
        this.nPoint.hpg = master.nPoint.hpg;
        this.nPoint.mpg = master.nPoint.mpg;
        this.nPoint.dameg = master.nPoint.dameg;
        this.nPoint.defg = master.nPoint.defg;
        this.nPoint.critg = master.nPoint.critg;
        this.nPoint.power = master.nPoint.power;
        this.nPoint.tiemNang = master.nPoint.tiemNang;
        this.nPoint.stamina = master.nPoint.stamina;
        this.nPoint.maxStamina = master.nPoint.maxStamina;

        if (master.inventory != null) {
            this.inventory = new Inventory();
            this.inventory.itemsBody = InventoryService.gI().copyItemsBody(master);
            if (this.inventory.itemsBody == null) {
                this.inventory.itemsBody = new ArrayList<>();
            }
        } else {
            this.inventory = new Inventory();
            this.inventory.itemsBody = new ArrayList<>();
        }

        this.playerSkill = new PlayerSkill(this);
        this.cloneSkill();
        this.nPoint.calPoint();
        this.nPoint.setFullHpMp();
        this.lastSpawnTime = System.currentTimeMillis();
    }

    private void cloneSkill() {
        for (Skill skill : master.playerSkill.skills) {
            Skill cloneSkill = new Skill(skill);
            this.playerSkill.skills.add(cloneSkill);
        }
    }

    @Override
    public void update() {
        super.update();
        if (isDie() && canRespawn()) {
            Service.gI().hsChar(this, nPoint.hpMax, nPoint.mpMax);
        }
        if (master != null && (this.zone == null || this.zone != master.zone)) {
            joinMapMaster();
        }
        if (Util.canDoWithTime(lastSpawnTime, 80000) || (lastTimeDie != 0 && Util.canDoWithTime(lastTimeDie, 3_000))) {
            dispose();
        }
    }

    @Override
    public void setDie(Player plAtt) {
        super.setDie(plAtt);
        lastTimeDie = System.currentTimeMillis();
    }

    private boolean canRespawn() {
        return lastTimeDie == 0;
    }

    public void attackWithMaster(Player plAtt, Mob mAtt) {
        if (plAtt != null) {
            if (SkillUtil.isUseSkillDam(this)) {
                PlayerService.gI().playerMove(this, plAtt.location.x + Util.nextInt(-60, 60), plAtt.location.y);
            }
            SkillService.gI().useSkillAttack(this, plAtt, null);
        } else if (mAtt != null) {
            if (SkillUtil.isUseSkillDam(this)) {
                PlayerService.gI().playerMove(this, mAtt.location.x + Util.nextInt(-60, 60), mAtt.location.y);
            }
            SkillService.gI().useSkillAttack(this, null, mAtt);
        }
    }

    public void joinMapMaster() {
        this.location.x = master.location.x + Util.nextInt(-10, 10);
        this.location.y = master.location.y;
        MapService.gI().goToMap(this, master.zone);
        this.zone.load_Me_To_Another(this);
    }

    public void followMaster() {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= 40) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, 40);
            } else {
                this.location.x = mX + Util.nextInt(0, 40);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    public void unFusion() {
        master.fusion.typeFusion = 0;
        this.status = PROTECT;
        Service.gI().point(master);
        joinMapMaster();
        fusionEffect(master.fusion.typeFusion);
        Service.gI().Send_Caitrang(master);
        Service.gI().point(master);
        this.lastTimeUnfusion = System.currentTimeMillis();
    }

    private void fusionEffect(int type) {
        Message msg;
        try {
            msg = new Message(125);
            msg.writer().writeByte(type);
            msg.writer().writeInt((int) master.id);
            Service.gI().sendMessAllPlayerInMap(master, msg);
            msg.cleanup();
        } catch (Exception e) {

        }
    }

    @Override
    public short getHead() {
        return master.getHead();
    }

    @Override
    public short getBody() {
        return master.getBody();
    }

    @Override
    public short getLeg() {
        return master.getLeg();
    }

    @Override
    public byte getEffFront() {
        return master.getEffFront();
    }

    @Override
    public byte getAura() {
        return master.getAura();
    }

    @Override
    public short getFlagBag() {
        return master.getFlagBag();
    }

    @Override
    public short getMount() {
        return master.getMount();
    }

    @Override
    public void dispose() {
        MapService.gI().exitMap(this);
        if (this.master != null) {
            this.master.clone = null;
        }
        this.master = null;
        super.dispose();
    }
}
