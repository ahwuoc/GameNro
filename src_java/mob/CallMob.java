package mob;

import consts.cn;
import map.Zone;
import player.Player;
import utils.SkillUtil;
import services.Service;
import utils.Util;
import network.Message;

public final class CallMob extends Mob {

    private Player player;
    private final long lastTimeSpawn;
    private final int timeSurvive;

    public CallMob(Player player, int lv) {
        super();
        this.player = player;
        this.id = (int) player.id;
        this.tempId = SkillUtil.getTempMobMe2(lv);
        this.point.maxHp = 1_000_000;
        this.point.dame = player.nPoint.dame / 2;
        this.point.hp = this.point.maxHp;
        this.zone = player.zone;
        this.lastTimeSpawn = System.currentTimeMillis();
        this.timeSurvive = SkillUtil.getTimeSurviveMobMe(lv);

        spawn();
    }

    @Override
    public void update() {
        if (Util.canDoWithTime(lastTimeSpawn, timeSurvive)) {
            this.mobMeDie();
            this.dispose();
        }
    }

    public void attack(Player pl, Mob mob, boolean miss) {
        Message msg;
        try {
            if (pl != null) {
                long dame = !miss ? this.point.dame : 0;
                if ((pl.nPoint.hp > dame && pl.nPoint.hp > pl.nPoint.hpMax * 0.05)) {
                    long dameHit = pl.injured(this.player, dame, true, true);
                    msg = new Message(-95);
                    msg.writer().writeByte(2);
                    msg.writer().writeInt(this.id);
                    msg.writer().writeInt((int) pl.id);
                    msg.writeLongByDucPro(Util.toIntOrLong(dameHit), cn.readInt);
                    msg.writeLongByDucPro(Util.toIntOrLong(pl.nPoint.hp), cn.readInt);
                    Service.gI().sendMessAllPlayerInMap(this.player, msg);
                    msg.cleanup();
                }
            }

            if (mob != null) {
                if (mob.point.gethp() > this.point.dame) {
                    long tnsm = mob.getTiemNangForPlayer(this.player, this.point.dame);
                    msg = new Message(-95);
                    msg.writer().writeByte(3);
                    msg.writer().writeInt(this.id);
                    msg.writer().writeInt((int) mob.id);
                    mob.point.sethp((mob.point.gethp() - this.point.dame));
                    msg.writeLongByDucPro(Util.toIntOrLong(mob.point.gethp()), cn.readInt);
                    msg.writeLongByDucPro(Util.toIntOrLong(this.point.dame), cn.readInt);
                    Service.gI().sendMessAllPlayerInMap(this.player, msg);
                    msg.cleanup();
                    Service.gI().addSMTN(player, (byte) 2, tnsm, true);
                }
            }
        } catch (Exception e) {
        }
    }

    // tạo mobme
    public void spawn() {
        Message msg;
        try {
            msg = new Message(-95);
            msg.writer().writeByte(0);// type
            msg.writer().writeInt((int) player.id);
            msg.writer().writeShort(this.tempId);
            msg.writeLongByDucPro(Util.toIntOrLong(this.point.hp), cn.readInt);// hp mob
            Service.gI().sendMessAllPlayerInMap(this.player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void goToMap(Zone zone) {
        if (zone != null) {
            this.removeMobInMap();
            this.zone = zone;
        }
    }

    // xóa mobme khỏi map
    private void removeMobInMap() {
        Message msg;
        try {
            msg = new Message(-95);
            msg.writer().writeByte(7);// type
            msg.writer().writeInt((int) player.id);
            Service.gI().sendMessAllPlayerInMap(this.player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void mobMeDie() {
        Message msg;
        try {
            msg = new Message(-95);
            msg.writer().writeByte(6);// type
            msg.writer().writeInt((int) player.id);
            Service.gI().sendMessAllPlayerInMap(this.player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    @Override
    public synchronized void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
        Message msg;
        try {
            if (damage > point.maxHp / 20) {
                damage = point.maxHp / 20;
            }
            point.hp -= damage;
            msg = new Message(-95);
            msg.writer().writeByte(5);// type
            msg.writer().writeInt((int) plAtt.id);
            msg.writer().writeByte(plAtt.playerSkill.skillSelect.template.id); // id skill
            msg.writer().writeInt(id); // mob id
            msg.writeLongByDucPro(Util.toIntOrLong(damage), cn.readInt);
            msg.writeLongByDucPro(Util.toIntOrLong(point.hp), cn.readInt);
            Service.gI().sendMessAllPlayerInMap(this.player, msg);
            msg.cleanup();
            if (isDie()) {
                mobMeDie();
                dispose();
            }
        } catch (Exception e) {
        }
    }

    public void dispose() {
        player.mobMe = null;
        this.player = null;
    }
}
