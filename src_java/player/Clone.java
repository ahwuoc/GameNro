package player;
import mob.Mob;
import skill.Skill;


import player.Player;
import services.MapService;
import services.PlayerService;
import services.Service;
import services.SkillService;
import services.func.ChangeMapService;
import utils.SkillUtil;
import utils.Util;


public class Clone extends Player {

    public Player master;
    public long lastTimeRevival;

    public Clone(Player master) {
        this.master = master;
        this.isClone = true;    
        this.id = -master.id * 2 + Util.nextInt(1_000_000, 1_500_000);
        this.lastTimeRevival = System.currentTimeMillis() + (1000 * 60);
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
    public short getFlagBag() {
        return master.getFlagBag();
    }

    public void joinMapPlayer() {
        if (!isDie() && !master.isDie()) {
            this.location.x = master.location.x + Util.nextInt(-10, 10);
            this.location.y = master.location.y;
            MapService.gI().goToMap(this, master.zone);
            this.zone.load_Me_To_Another(this);
        }
    }

    @Override
    public void update() {
        super.update();
        try {
            if (master == null || master.zone == null || this.isDie() || Util.canDoWithTime(this.lastTimeRevival, 1000)) {
                this.dispose();
                return;
            }
            if (master != null && master.isDie() || effectSkill.isHaveEffectSkill()) {
                return;
            }
            if ((this.zone == null || this.zone != master.zone) && master != null && master.zone != null) {
                joinMapPlayer();
            }
            if (master != null) {
                followMaster(80);
            }
        } catch (Exception e) {
        }
    }

    public void attackWithMaster(Player plAtt, Mob mAtt) {
        if (SkillUtil.isUseSkillDam(this)) {
            if (plAtt != null) {
                PlayerService.gI().playerMove(this, plAtt.location.x + Util.nextInt(-60, 60), plAtt.location.y);
            } else if (mAtt != null) {
                PlayerService.gI().playerMove(this, mAtt.location.x + Util.nextInt(-60, 60), mAtt.location.y);
            }
        }
        SkillService.gI().useSkillAttack(this, plAtt, mAtt);
    }

    public void followMaster() {
        if (master == null || this.isDie() || effectSkill.isHaveEffectSkill()) {
            return;
        }
        followMaster(60);
    }

    private void followMaster(int dis) {
        int mX = master.location.x;
        int mY = master.location.y;
        int disX = this.location.x - mX;
        if (Math.sqrt(Math.pow(mX - this.location.x, 2) + Math.pow(mY - this.location.y, 2)) >= dis) {
            if (disX < 0) {
                this.location.x = mX - Util.nextInt(0, dis);
            } else {
                this.location.x = mX + Util.nextInt(0, dis);
            }
            this.location.y = mY;
            PlayerService.gI().playerMove(this, this.location.x, this.location.y);
        }
    }

    public static void callPoint(Player player) {
        if (player.clone != null) {
            player.clone.dispose();
            
            ChangeMapService.gI().exitMap(player.clone);
            return;
        }
        player.clone = new Clone(player);
        player.clone.name = "Nhân Bản";
        player.clone.gender = player.gender;
        player.clone.nPoint.limitPower = player.nPoint.limitPower;
        player.clone.nPoint.power = player.nPoint.power;
        player.clone.nPoint.hpg = player.nPoint.hpg;
        player.clone.nPoint.mpg = player.nPoint.mpg;
        player.clone.nPoint.dameg = player.nPoint.dameg;
        player.clone.nPoint.defg = player.nPoint.defg;
        player.clone.nPoint.critg = player.nPoint.critg;
        player.clone.nPoint.hp = player.nPoint.hp;
        player.clone.nPoint.mp = player.nPoint.mp;
        player.clone.nPoint.dame = player.nPoint.dame;
        player.clone.nPoint.def = player.nPoint.def;
        player.clone.nPoint.crit = player.nPoint.crit;
        player.clone.nPoint.stamina = player.nPoint.stamina;
        player.clone.nPoint.maxStamina = player.nPoint.maxStamina;
        player.clone.inventory = player.inventory;
        player.clone.effectSkill = player.effectSkill;
        player.clone.playerSkill.skills = player.playerSkill.skills;
        player.clone.playerSkill.skillSelect = player.playerSkill.skillSelect;
        player.clone.fusion = player.fusion;
       
        player.clone.nPoint.calPoint();
    }

    @Override
    public void dispose() {
        if (this.zone != null) {
            ChangeMapService.gI().exitMap(this);
        }
        this.zone = null;
        this.master.clone = null;
        this.master = null;
    }
}
