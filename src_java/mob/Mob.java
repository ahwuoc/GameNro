package mob;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */
import nro.services.InventoryService;
import nro.services.Service;
import nro.services.TaskService;
import nro.services.ItemMapService;
import consts.ConstMap;
import consts.ConstMob;
import consts.ConstTask;
import consts.ConstTaskBadges;
import consts.ConstNpcConfig;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;
import map.ItemMap;

import java.util.List;

import map.Zone;
import nro.player.Location;
import nro.player.Pet;
import nro.player.Player;
import network.Message;

import java.io.IOException;

import nro.server.Maintenance;
import nro.server.Manager;
import utils.Util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import models.Achievement.AchievementService;
import models.Training.TrainingService;
import models.TreasureUnderSea.TreasureUnderSea;
import nro.player.PlayerClone;
import nro.server.ServerNotify;
import nro.services.ChatGlobalService;
import nro.services.ItemService;
import nro.services.MapService;
import skill.Skill;
import task.Badges.BadgesTaskService;
import utils.TimeUtil;

public class Mob {

    public int id;
    public Zone zone;
    public int tempId;
    public String name;
    public byte level;

    public List<Player> temporaryEnemies = new ArrayList<>();

    public MobPoint point;
    public MobEffectSkill effectSkill;
    public Location location;

    public byte pDame;
    public int pTiemNang;
    private long maxTiemNang;

    public long lastTimeDie;
    public int lvMob = 0;
    public int status = 5;
    public int type = 1;

    private long lastTimeAttackPlayer;
    private long timeAttack = 2000;
    public long lastTimePhucHoi = System.currentTimeMillis();
    public long lastTimeSendEffect = System.currentTimeMillis();

    public Mob(Mob mob) {
        this.point = new MobPoint(this);
        this.effectSkill = new MobEffectSkill(this);
        this.location = new Location();
        this.id = mob.id;
        this.tempId = mob.tempId;
        this.level = mob.level;
        this.point.setHpFull(mob.point.getHpFull());
        this.point.sethp((this.point.getHpFull()));
        this.location.x = mob.location.x;
        this.location.y = mob.location.y;
        this.pDame = mob.pDame;
        this.pTiemNang = mob.pTiemNang;
        this.type = mob.type;
        this.setTiemNang();
    }

    public Mob() {
        this.point = new MobPoint(this);
        this.effectSkill = new MobEffectSkill(this);
        this.location = new Location();
    }
    
    private boolean canDrop(int baseChance, int outOf) {
        double globalRate = 100.0 / 100.0;
        // tính lại baseChance rồi gọi Util
        int scaledChance = (int) Math.round(baseChance * globalRate);
        return Util.isTrue(scaledChance, outOf);
    }

    public void setTiemNang() {
        this.maxTiemNang = (int) (long) this.point.getHpFull() * (long) (this.pTiemNang + Util.nextInt(-2, 2)) / 100L;
    }

    public boolean isDie() {
        return this.point.gethp() <= 0;
    }

    public void setDie() {
        this.lastTimePhucHoi = System.currentTimeMillis();
        this.lastTimeDie = System.currentTimeMillis();
    }

    public void addTemporaryEnemies(Player pl) {
        if (pl != null && !temporaryEnemies.contains(pl)) {
            temporaryEnemies.add(pl);
        }
    }

    public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
        if (!this.isDie()) {
            if (damage >= this.point.hp) {
                damage = this.point.hp;
            }
            if (!dieWhenHpFull) {
                if (this.point.hp == this.point.maxHp && damage >= this.point.hp) {
                    damage = this.point.hp - 1;
                }
                if ((this.tempId == ConstMob.MOC_NHAN || this.tempId == ConstMob.BU_NHIN_MA_QUAI) && damage > this.point.maxHp / 10) {
                    damage = this.point.maxHp / 10;
                }
            }
            if (MapService.gI().isMapKhiGasHuyDiet(this.zone.map.mapId)) {
                boolean mob76Die = true;
                for (Mob mob : this.zone.mobs) {
                    if (!mob.isDie() && mob.tempId == ConstMob.CO_MAY_HUY_DIET) {
                        mob76Die = false;
                        break;
                    }
                }
                if (!mob76Die && plAtt != null && plAtt.playerSkill != null && plAtt.playerSkill.skillSelect != null) {
                    switch (plAtt.playerSkill.skillSelect.template.id) {
                        case Skill.LIEN_HOAN, Skill.ANTOMIC, Skill.MASENKO, Skill.KAMEJOKO ->
                            damage = 1;
                    }
                }
            }
            if (!dieWhenHpFull && !isBigBoss() && !MapService.gI().isMapPhoBan(this.zone.map.mapId) && this.lvMob > 0 && plAtt != null && plAtt.charms.tdOaiHung < System.currentTimeMillis()) {
                damage = (int) ((this.point.maxHp <= 20000000 ? this.point.maxHp * 10 : 2000000000) * (10.0 / 100));
                this.mobAttackPlayer(plAtt);
            }
            if (plAtt != null && plAtt.isBoss && this.tempId > 0 && Util.isTrue(1, 2) && Util.canDoWithTime(lastTimeAttackPlayer, 2500)) {
                this.mobAttackPlayer(plAtt);
                lastTimeAttackPlayer = System.currentTimeMillis();
            }

//            if (damage > 2_000_000_000) {
//                damage = 2_000_000_000;
//            }
            this.point.hp -= damage;
            addTemporaryEnemies(plAtt);
            if (this.isDie()) {
                this.status = 0;
                this.setDie();
                this.temporaryEnemies.clear();
                if (plAtt != null) {
                    this.sendMobDieAffterAttacked(plAtt, (int) damage);
                    TaskService.gI().checkDoneTaskKillMob(plAtt, this);
                    TaskService.gI().checkDoneSideTaskKillMob(plAtt, this);
                    TaskService.gI().checkDoneClanTaskKillMob(plAtt, this);
                    AchievementService.gI().checkDoneTaskKillMob(plAtt, this);
                    if (plAtt.itemTime != null && plAtt.itemTime.isUseTDLT) {
                        plAtt.playerTask.kolTask.addCount();
                    }
                }
                if (this.id == 13) {
                    this.zone.isbulon1Alive = false;
                }
                if (this.id == 14) {
                    this.zone.isbulon2Alive = false;
                }
            } else {
                this.sendMobStillAliveAffterAttacked(damage, plAtt != null ? (plAtt.nPoint != null && plAtt.nPoint.isCrit) : false);
            }
            if (plAtt != null) {
                if (plAtt.isPl() && plAtt.satellite != null && plAtt.satellite.isDefend) {
                    plAtt.satellite.isDefend = false;
                }
                Service.gI().addSMTN(plAtt, (byte) 2, getTiemNangForPlayer(plAtt, damage), true);
                TrainingService.gI().tangTnsmLuyenTap(plAtt, getTiemNangForPlayer(plAtt, damage));
                plAtt.total_damage_maydam += damage;
                Service.gI().updatePlayerTotalDamage(plAtt);
                long realDamage = this.point.hp / 100 > 0 ? this.point.hp / 100 : 1;
                damage = this.point.hp / 100 > 0 ? this.point.hp / 100 : 1;
            }
            if (MapService.gI().isMapCadic(this.zone.map.mapId) && plAtt != null) {
                boolean hasKilisBuff = plAtt.itemTime != null && plAtt.itemTime.isUseKilis;
                int successRate = hasKilisBuff ? 10 : 1;

                if (Util.isTrue(successRate, 333)) {
                    if (plAtt != null) {
                        Item item = InventoryService.gI().findItemBag(plAtt, 1822);
                        if (item != null) {
                            boolean updated = false;
                            Item.ItemOption kilisOption = item.getOptionById(249);
                            if (kilisOption != null) {
                                kilisOption.param += 1;
                                updated = true;
                            } else {
                                item.itemOptions.add(new Item.ItemOption(249, 1));
                                updated = true;
                            }

                            if (updated) {
                                InventoryService.gI().sendItemBag(plAtt);
                                Service.gI().sendThongBao(plAtt, "Bạn vừa nhận được 1 chỉ số Kilis!");
                            }
                        }
                    }
                }
            }
        }
    }

    public long getTiemNangForPlayer(Player pl, long dame) {
        int levelPlayer = Service.gI().getCurrLevel(pl);
        int n = levelPlayer - this.level;
        if (pl.zone != null && MapService.gI().isMapBanDoKhoBau(pl.zone.map.mapId)) {
            n = 0;
        }
        if (pl.nPoint != null && pl.nPoint.power < 40_000_000_000L) {
            n = 0;
        }
        long pDameHit = dame * 100 / point.getHpFull();
        long tiemNang = pDameHit * maxTiemNang / 100;
        if (tiemNang <= 0) {
            tiemNang = 1;
        }
        if (tiemNang >= 1000000000) {
            tiemNang = 999999999;
        }
        if (n >= 0) {
            for (int i = 0; i < n; i++) {
                long sub = tiemNang * 10 / 100;
                if (sub <= 0) {
                    sub = 1;
                }
                tiemNang -= sub;
            }
        } else {
            for (int i = 0; i < -n; i++) {
                long add = tiemNang * 10 / 100;
                if (add <= 0) {
                    add = 1;
                }
                tiemNang += add;
            }
        }
        if (tiemNang <= 0) {
            tiemNang = 1;
        }
        if (pl.nPoint != null) {
            tiemNang = (int) pl.nPoint.calSucManhTiemNang(tiemNang);
        } else {
            return 0;
        }
        if (pl.zone.map.mapId == 122 || pl.zone.map.mapId == 123 || pl.zone.map.mapId == 124) {
            tiemNang *= 2;
        }
        return tiemNang;
    }

    public void update() {
        if (zone.isGoldenFriezaAlive && TimeUtil.is21H()) {
            if (!isDie()) {
                startDie();
                return;
            }
        }
        if (!this.isDie() && this.tempId == ConstMob.CO_MAY_HUY_DIET && Util.canDoWithTime(lastTimeSendEffect, 1000)) {
            sendEffect(55);
            lastTimeSendEffect = System.currentTimeMillis();
        }

        if (this.isDie() && !Maintenance.isRunning && !isBigBoss()) {
            switch (zone.map.type) {
                case ConstMap.MAP_DOANH_TRAI:
                    if (this.tempId == ConstMob.BULON && this.zone.isTUTAlive && Util.canDoWithTime(lastTimeDie, 10000)) {
                        this.hoiSinh();
                        this.hoiSinhMobPhoBan();
                        if (this.id == 13) {
                            this.zone.isbulon1Alive = true;
                        }
                        if (this.id == 14) {
                            this.zone.isbulon2Alive = true;
                        }
                    }
                    break;
                case ConstMap.MAP_BAN_DO_KHO_BAU:
                    break;
                case ConstMap.MAP_CON_DUONG_RAN_DOC:
                    break;
                case ConstMap.MAP_KHI_GAS_HUY_DIET:
                    break;
                case ConstMap.MAP_TAY_KARIN:
                    break;
                default:
                    if (this.zone.isGoldenFriezaAlive && TimeUtil.is21H()) {
                        return;
                    }
                    if (Util.canDoWithTime(lastTimeDie, 5000)) {
                        this.hoiSinh();
                        this.sendMobHoiSinh();
                    }
                    if (Util.canDoWithTime(lastTimePhucHoi, 30000) && !isDie()) {
                        lastTimePhucHoi = System.currentTimeMillis();
                        long hpMax = this.point.maxHp;
                        if (this.point.hp < hpMax) {
                            hoi_hp(hpMax / 10);
                        } else {
                            this.sendMobHoiSinh();
                        }
                    }
            }
        }

        effectSkill.update();
        attack();
    }

    public boolean isBigBoss() {
        return (this.tempId == ConstMob.HIRUDEGARN
                || this.tempId == ConstMob.VUA_BACH_TUOC
                || this.tempId == ConstMob.ROBOT_BAO_VE
                || this.tempId == ConstMob.GAU_TUONG_CUOP
                || this.tempId == ConstMob.VOI_CHIN_NGA
                || this.tempId == ConstMob.GA_CHIN_CUA
                || this.tempId == ConstMob.NGUA_CHIN_LMAO
                || this.tempId == ConstMob.PIANO
                || this.tempId == ConstMob.KONG
                || this.tempId == ConstMob.GOZILLA
                || this.tempId == ConstMob.MAY_DO_SUC_MANH);
    }

    public void attack() {
        Player player = getPlayerCanAttack();
        if (!isDie() && !effectSkill.isHaveEffectSkill() 
                && tempId != ConstMob.MOC_NHAN 
                && tempId != ConstMob.MOC_NHAN_BOT 
                && tempId != ConstMob.MAY_DO_SUC_MANH 
                && tempId != ConstMob.BU_NHIN_MA_QUAI 
                && tempId != ConstMob.CO_MAY_HUY_DIET 
                && !this.isBigBoss() 
                && (this.lvMob < 1 || MapService.gI().isMapPhoBan(this.zone.map.mapId)) 
                && Util.canDoWithTime(lastTimeAttackPlayer, timeAttack)) {
            if (player != null) {
                this.mobAttackPlayer(player);
            }
            this.lastTimeAttackPlayer = System.currentTimeMillis();
        }
    }

    public Player getPlayerCanAttack() {
        Player plAttack = getFirstPlayerCanAttack();
        if (plAttack != null) {
            return plAttack;
        }
        int distance = 100;
        try {
            List<Player> players = this.zone.getNotBosses();
            for (Player pl : players) {
                if (!pl.isDie() && !pl.isBoss && !pl.isNewPet && !pl.nPoint.buffDefenseSatellite && (pl.satellite == null || !pl.satellite.isDefend) && (pl.effectSkin == null || !pl.effectSkin.isVoHinh) && (this.tempId > 18 || (this.tempId > 9 && this.type == 4)) || isBigBoss()) {
                    int dis = Util.getDistance(pl, this);
                    if (dis <= distance || isBigBoss()) {
                        plAttack = pl;
                        distance = dis;
                    }
                }
            }
            this.timeAttack = 2000;
        } catch (Exception e) {

        }
        return plAttack;
    }

    private Player getFirstPlayerCanAttack() {
        Player plAtt = null;
        try {
            List<Player> playersMap = zone.getHumanoids();
            int dis = 300;
            if (playersMap != null) {
                for (Player plAttt : playersMap) {
                    if (plAttt.isDie() || plAttt.isBoss || (plAttt.satellite != null && plAttt.satellite.isDefend) || (plAttt.effectSkin != null && plAttt.effectSkin.isVoHinh) || !this.temporaryEnemies.contains(plAttt)) {
                        continue;
                    }
                    int d = Util.getDistance(plAttt, this);
                    if (d <= dis) {
                        dis = d;
                        plAtt = plAttt;
                    }
                }
            }
            this.timeAttack = 1000;
        } catch (Exception e) {

        }
        return plAtt;
    }

    private void mobAttackPlayer(Player player) {
        long dameMob = Util.maxIntValue(this.point.getDameAttack());
        if (player.charms != null && player.charms.tdDaTrau > System.currentTimeMillis()) {
            dameMob /= 2;
        }
        if (player.isPet && ((Pet) player).master.charms != null && ((Pet) player).master.charms.tdDeTu > System.currentTimeMillis()) {
            dameMob /= 2;
        }
        if (this.lvMob > 0 && !MapService.gI().isMapPhoBan(this.zone.map.mapId)) {
            dameMob = (long) (player.nPoint.hpMax * (10.0 / 100));
        }
        if (player.satellite != null && player.satellite.isDefend) {
            dameMob -= dameMob / 5;
        }
        if (player.itemTime != null && player.itemTime.isUseCMS) {
            dameMob = (long) Math.round(dameMob * 0.1);
        }
        if (this.lvMob > 0 && player.charms.tdOaiHung > System.currentTimeMillis()) {
            dameMob = 0;
        }
        long dame = player.injured(null, Util.maxIntValue(dameMob), false, true);

        this.sendMobAttackMe(player, dame);
        this.sendMobAttackPlayer(player);
        this.phanSatThuong(player, dame);
    }

    private void sendMobAttackMe(Player player, long dame) {
        if (!player.isPet && !player.isClone && !player.isNewPet) {
            Message msg;
            try {
                msg = new Message(-11);
                msg.writer().writeByte(this.id);
                msg.writeLongByEmti(Util.maxIntValue(dame), ConstNpcConfig.readInt); //dame
                player.sendMessage(msg);
                msg.cleanup();
            } catch (Exception e) {
            }
        }
    }

    private void sendMobAttackPlayer(Player player) {
        Message msg;
        try {
            msg = new Message(-10);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt((int) player.id);
            msg.writeLongByEmti(Util.maxIntValue(player.nPoint.hp), ConstNpcConfig.readInt); //hp
            Service.gI().sendMessAnotherNotMeInMap(player, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void hoiSinh() {
        this.status = 5;
        this.point.hp = this.point.maxHp;
        this.setTiemNang();
    }

    public int lvMob() {
        for (Mob mobMap : this.zone.mobs) {
            if (mobMap.lvMob > 0) {
                return 0;
            }
        }
        this.lvMob = this.tempId > 18 && !isBigBoss() ? Util.isTrue(10, 100) ? 1 : 0 : 0;
        this.point.hp = this.lvMob > 0 ? this.point.maxHp <= 20000000 ? this.point.maxHp * 10 : 2000000000 : this.point.maxHp;
        return this.lvMob;
    }

    public void sendMobHoiSinh() {
        Message msg = null;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(lvMob());
            msg.writeLongByEmti(Util.maxIntValue(this.point.hp), ConstNpcConfig.readInt);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            this.sendMobMaxHp(this.point.hp);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void hoi_hp(long hp) {
        Message msg = null;
        try {
            this.point.sethp((this.point.gethp() + hp));
            long HP = hp > 0 ? 1 : Math.abs(hp);
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writeLongByEmti(Util.maxIntValue(this.point.gethp()), ConstNpcConfig.readInt);
            msg.writeLongByEmti(Util.maxIntValue(HP), ConstNpcConfig.readInt);
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(-1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    public void sendEffect(int Effect) {
        Message msg = null;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writeLongByEmti(Util.maxIntValue(this.point.gethp()), ConstNpcConfig.readInt);
            msg.writeLongByEmti(Util.maxIntValue(this.point.gethp()), ConstNpcConfig.readInt);
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(Effect);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    private void sendMobDieAffterAttacked(Player plKill, long dameHit) {
        Message msg;
        try {
            msg = new Message(-12);
            msg.writer().writeByte(this.id);
            msg.writeLongByEmti(Util.maxIntValue(dameHit), ConstNpcConfig.readInt);
            msg.writer().writeBoolean(plKill.nPoint.isCrit); // crit
            List<ItemMap> items = mobReward(plKill, this.dropItemTask(plKill), msg);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
            hutItem(plKill, items);
        } catch (Exception e) {
        }
    }

    private void hutItem(Player player, List<ItemMap> items) {
        if (!player.isPet && !player.isBot && !player.isNewPet && !player.isClone) {
            if (player.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    ItemMapService.gI().pickItem(player, item.itemMapId, true);
                }
            }
        } else {
            if (((Pet) player).master.charms.tdThuHut > System.currentTimeMillis()) {
                for (ItemMap item : items) {
                    ItemMapService.gI().pickItem(((Pet) player).master, item.itemMapId, true);
                }
            }
        }
    }

    private List<ItemMap> mobReward(Player player, ItemMap itemTask, Message msg) {
        List<ItemMap> itemReward = new ArrayList<>();
        try {
            if (player.isBot) {
                // Nếu là bot, không rơi vật phẩm, trả về danh sách rỗng
                msg.writer().writeByte(0);  // không có vật phẩm
                return itemReward;  // trả về danh sách rỗng
            }
            itemReward = this.getItemMobReward(player, this.location.x + Util.nextInt(-10, 10),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y));
            if (itemTask != null) {
                itemReward.add(itemTask);
            }
            msg.writer().writeByte(itemReward.size()); //sl item roi
            for (ItemMap itemMap : itemReward) {
                msg.writer().writeShort(itemMap.itemMapId);// itemmapid
                msg.writer().writeShort(itemMap.itemTemplate.id); // id item
                msg.writer().writeShort(itemMap.x); // xend item
                msg.writer().writeShort(itemMap.y); // yend item
                msg.writer().writeInt((int) itemMap.playerId); // id nhan vat
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemReward;
    }

    public List<ItemMap> getItemMobReward(Player player, int x, int yEnd) throws Exception {
        List<ItemMap> list = new ArrayList<>();
        long dropForPlayer = player.isClone ? ((PlayerClone) player).master.id : player.id;//thêm phân thân
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.ONG_THAN_VE_CHAI, 1);
        if (player.isBoss) {
            return list;
        }
        if (this.tempId == 0) {
            return list;
        }
        int mapid = player.zone.map.mapId;

        if (EventManager.CHRISTMAS) {
            Player pl = player;

            // Nếu là đệ → lấy master
            if (pl.isPet) {
                pl = ((Pet) pl).master;
            }

            // ===== RƠI TẤT VÔ GIÁNG SINH =====
            if (Util.isTrue(1, 50)) {
                if (pl.itemEvent != null && pl.itemEvent.canDropTatVoGiangSinh(100)) {
                    list.add(new ItemMap(
                            zone,
                            649, // tempId
                            1, // quantity
                            x,
                            yEnd,
                            pl.id // owner đúng
                    ));
                }
            }

            // ===== RƠI ITEM EVENT CÓ OPTION =====
            if (Util.isTrue(1, 50)) {
                if (pl.itemEvent != null && MapService.gI().isMapCold(zone.map.mapId)) {

                    ItemMap itemMap = new ItemMap(
                            zone,
                            2000, // tempId
                            1,
                            x,
                            yEnd,
                            pl.id // owner đúng
                    );

                    itemMap.options.add(new Item.ItemOption(87, 0));  // option sự kiện
                    itemMap.options.add(new Item.ItemOption(93, 35)); // hạn sử dụng 35 ngày
                    itemMap.options.add(new Item.ItemOption(30, 0));  // khóa / new

                    list.add(itemMap);
                }
            }
            
            // ===== RƠI ITEM EVENT CÓ OPTION =====
            if (Util.isTrue(1, 300)) {
                if (pl.itemEvent != null && MapService.gI().isMapCold(zone.map.mapId)) {

                    ItemMap itemMap = new ItemMap(
                            zone,
                            2003, // tempId
                            1,
                            x,
                            yEnd,
                            pl.id // owner đúng
                    );

                    itemMap.options.add(new Item.ItemOption(87, 0));  // option sự kiện
                    itemMap.options.add(new Item.ItemOption(93, 35)); // hạn sử dụng 35 ngày
                    itemMap.options.add(new Item.ItemOption(30, 0));  // khóa / new

                    list.add(itemMap);
                }
            }
        }
        if (mapid == 5 || mapid == 13) {
            Player pl = player;
            if (pl.isPet) {
                pl = ((Pet) pl).master;
            }
            if (pl.isPet) {
                pl = ((Pet) pl).master;
            }
            if (Util.isTrue(1, 500)) {
                if (pl.itemEvent != null && pl.itemEvent.canDropBinhNuoc(100)) {
                    list.add(new ItemMap(zone, 456, 1, x, yEnd, pl.id));
                }
            }
        }
        if (EventManager.INTERNATIONAL_WOMANS_DAY) {
            Player pl = player;
            if (pl.isPet) {
                pl = ((Pet) pl).master;
            }
            if (pl.isPet) {
                pl = ((Pet) pl).master;
            }
            if (Util.isTrue(1, 50)) {
                if (pl.itemEvent != null && pl.itemEvent.canDropHoaHong(100)) {
                    list.add(new ItemMap(zone, 610, 1, x, yEnd, player.id));
                }
            }
        }
        if (EventManager.TEACHERS_DAY) {
            Player pl = player;
            if (pl.isPet) {
                pl = ((Pet) pl).master;
            }
            if (Util.isTrue(1, 50)) {
                if (pl.itemEvent != null && pl.itemEvent.canDropHoaHong(100)) {
                    ItemMap itemMap = new ItemMap(zone, 610, 1, x, yEnd, player.id);
                    long e = TimeUtil.getTime("30-11-2025", "dd-MM-yyyy");
                    itemMap.options.add(new ItemOption(73, (int) (e / 1000)));
                    list.add(itemMap);
                }
            }
            if (Util.isTrue(1, 300)) {
                if (MapService.gI().isMapTeacherDay(mapid)) {
                    ItemMap itemMap = new ItemMap(zone, 1364, 1, x, yEnd, player.id);
                    long e = TimeUtil.getTime("30-11-2025", "dd-MM-yyyy");
                    itemMap.options.add(new ItemOption(73, (int) (e / 1000)));
                    list.add(itemMap);
                }
            }
            if (player.zone != null && MapService.gI().isMapPhoBan(player.zone.map.mapId)) {

                int levelPlayer = Service.gI().getCurrLevel(player);
                int dropRate;
                if (levelPlayer <= 10) {
                    dropRate = 5;
                } else if (levelPlayer <= 20) {
                    dropRate = 10;
                } else if (levelPlayer <= 30) {
                    dropRate = 15;
                } else if (levelPlayer <= 40) {
                    dropRate = 20;
                } else if (levelPlayer <= 50) {
                    dropRate = 25;
                } else if (levelPlayer <= 60) {
                    dropRate = 30;
                } else if (levelPlayer <= 70) {
                    dropRate = 35;
                } else if (levelPlayer <= 80) {
                    dropRate = 40;
                } else if (levelPlayer <= 90) {
                    dropRate = 45;
                } else if (levelPlayer <= 100) {
                    dropRate = 50;
                } else {
                    dropRate = 60;
                }
                if (Util.isTrue(dropRate, 100) || (player.actived() && Util.isTrue(dropRate + levelPlayer / 2, 100))) {
                    Set<Integer> usedXPositions = new HashSet<>();
                    while (usedXPositions.contains(x)) {
                        x += 10;
                    }
                    usedXPositions.add(x);

                    ItemMap itemMap = new ItemMap(zone, 1365, 1, x, yEnd, player.id);
                    long e = TimeUtil.getTime("30-11-2025", "dd-MM-yyyy");
                    itemMap.options.add(new ItemOption(73, (int) (e / 1000)));
                    list.add(itemMap);
                }
            }
        }
        if (Util.isTrue(15, 300)) {
            list.add(new ItemMap(zone, 18, 1, x, yEnd, player.id));
        }
        if (Util.isTrue(15, 300)) {
            list.add(new ItemMap(zone, 19, 1, x, yEnd, player.id));
        }
        if (Util.isTrue(15, 300)) {
            list.add(new ItemMap(zone, 20, 1, x, yEnd, player.id));
        }
        if (Util.isTrue(1, 400)) {
            ItemMap it = new ItemMap(zone, 1530, 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(30, 0));
            list.add(it);

        }
        // Set kich hoat
        if (player.itemTime.isCoBonLa) {
            if (MapService.gI().isMapUpSKH(mapid)) {
                if (Util.isTrue(1, 1000)) {
                    short itTemp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
                    ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
                    if (!ops.isEmpty()) {
                        it.options = ops;
                    }
                    int[] opsrand = ItemService.gI().randOptionItemKichHoat(player.gender);
                    it.options.add(new Item.ItemOption(opsrand[0], 0));
                    it.options.add(new Item.ItemOption(opsrand[1], 0));
                    it.options.add(new Item.ItemOption(30, 0));
                    list.add(it);
                    ChatGlobalService.gI().autoChatGlobal(player, "[ Hệ Thống ] " + player.name + " vừa nhặt được " + it.itemTemplate.name + " Sét Kích Hoạt");
                }
            }
        } else {
            if (MapService.gI().isMapUpSKH(mapid)) {
                if (Util.isTrue(1, 5000)) {
                    short itTemp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
                    ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
                    if (!ops.isEmpty()) {
                        it.options = ops;
                    }
                    int[] opsrand = ItemService.gI().randOptionItemKichHoat(player.gender);
                    it.options.add(new Item.ItemOption(opsrand[0], 0));
                    it.options.add(new Item.ItemOption(opsrand[1], 0));
                    list.add(it);
                    ChatGlobalService.gI().autoChatGlobal(player, "[ Hệ Thống ] " + player.name + " vừa nhặt được " + it.itemTemplate.name + " Sét Kích Hoạt");
                }
            }
        }

        if (((Util.isTrue(1, 50000)) || (Manager.TEST && Util.isTrue(5, 1000000)) || Util.isTrue(1, 10000000)) && MapService.gI().isMapUpSKH(mapid)) {
            short itTemp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
            ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
            List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
            if (!ops.isEmpty()) {
                it.options = ops;
            }

            int[] opsrand = ItemService.gI().randOptionItemKichHoatNew(player.gender);
            it.options.add(new Item.ItemOption(opsrand[0], 0));
            it.options.add(new Item.ItemOption(opsrand[1], 0));
            it.options.add(new Item.ItemOption(opsrand[2], 0));
            it.options.add(new Item.ItemOption(opsrand[3], 0));
            it.options.add(new Item.ItemOption(30, 0));
            list.add(it);
            ChatGlobalService.gI().autoChatGlobal(player, "[ Hệ Thống ] " + player.name + " vừa nhặt được " + it.itemTemplate.name + " Sét Kích Hoạt");
        }
        if (EventManager.HALLOWEEN) {
            if (MapService.gI().isMapEventHalloween(mapid)) {
                if (Util.isTrue(1, 50)) {
                    list.add(new ItemMap(zone, 707, 1, x, yEnd, player.id));
                } else if (Util.isTrue(1, 50)) {
                    list.add(new ItemMap(zone, 708, 1, x, yEnd, player.id));
                }
            }
        }

        if (player.itemTime.isUseMayDo && (Util.isTrue(15, 100) || (player.actived() && Util.isTrue(1, 10))) && this.tempId > 57 && this.tempId < 66) {
            list.add(new ItemMap(zone, 380, 1, x, yEnd, player.id));
        }
        if (player.itemTime.isUseMayDo2 && Util.isTrue(15, 100) && this.tempId > 80 && this.tempId < 81) {
            list.add(new ItemMap(zone, 1110, 1, x, yEnd, player.id));
        }

        if (player.isPl() && TaskService.gI().getIdTask(player) == ConstTask.TASK_8_1) {
            if (player.gender == 0 && this.tempId == 11 || player.gender == 1 && this.tempId == 12 || player.gender == 2 && this.tempId == 10) {
                list.add(new ItemMap(zone, 20, 1, x, yEnd, player.id));
            }
        }
        
        if (MapService.gI().isMapDoanhTrai(mapid)) {
            if (canDrop(1, 500)) {
                list.add(new ItemMap(zone, 1861, 1, x, yEnd, player.id));
            }
        }
        
        if (MapService.gI().AllMap(mapid) && EventManager.LUNAR_NEW_YEAR) {
            if (canDrop(1, 200)) {
                ItemMap it = new ItemMap(zone, 748, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(30, 0));
                it.options.add(new Item.ItemOption(87, 0));
                it.options.add(new Item.ItemOption(93, 30));
                it.options.add(new Item.ItemOption(174, 2026));
                list.add(it);
            }
        }

        if (MapService.gI().isMapPhoBan(mapid) && EventManager.LUNAR_NEW_YEAR) {
            Set<Integer> usedXPositions = new HashSet<>(); // Tập hợp để lưu các giá trị x đã dùng

            // Determine the player's level
            int levelPlayer = Service.gI().getCurrLevel(player);
            int dropRate = 10;  // Default to 10% for level 1-10

            // Adjust drop rate based on the player's level
            if (levelPlayer >= 11 && levelPlayer <= 20) {
                dropRate = 15;  // 15% for level 11-20
            } else if (levelPlayer >= 21 && levelPlayer <= 30) {
                dropRate = 20;  // 20% for level 21-30
            } else if (levelPlayer >= 31 && levelPlayer <= 40) {
                dropRate = 25;  // 25% for level 31-40
            } else if (levelPlayer >= 41 && levelPlayer <= 50) {
                dropRate = 30;  // 30% for level 41-50
            } else if (levelPlayer >= 51 && levelPlayer <= 60) {
                dropRate = 35;  // 35% for level 51-60
            } else if (levelPlayer >= 61 && levelPlayer <= 70) {
                dropRate = 40;  // 40% for level 61-70
            } else if (levelPlayer >= 71 && levelPlayer <= 80) {
                dropRate = 45;  // 45% for level 71-80
            } else if (levelPlayer >= 81 && levelPlayer <= 90) {
                dropRate = 50;  // 50% for level 81-90
            } else if (levelPlayer >= 91 && levelPlayer <= 100) {
                dropRate = 55;  // 55% for level 91-100
            } else if (levelPlayer >= 101 && levelPlayer <= 110) {
                dropRate = 60;  // 60% for level 101-110
            }

            // Adjust for specific zones
            if (player.zone != null && MapService.gI().isMapBanDoKhoBau(player.zone.map.mapId)) {
                dropRate = 0;  // Set drop rate to 0 if in special map zone
            }

            // Rơi vật phẩm ID 748
            if (Util.isTrue(10 + levelPlayer, 100) || (player.actived() && Util.isTrue(dropRate + levelPlayer / 2, 50))) {
                while (usedXPositions.contains(x)) {
                    x += 10; // Điều chỉnh tọa độ x (tăng thêm 10 mỗi lần kiểm tra)
                }
                usedXPositions.add(x); // Lưu giá trị x đã dùng
                list.add(new ItemMap(zone, 748, 1, x, yEnd, player.id));
            }

            // Rơi vật phẩm ID 749
            if (Util.isTrue(50 + levelPlayer / 2, 100) || (player.actived() && Util.isTrue(dropRate + levelPlayer / 2, 50))) {
                while (usedXPositions.contains(x)) {
                    x += 10;
                }
                usedXPositions.add(x);
                list.add(new ItemMap(zone, 749, 1, x, yEnd, player.id));
            }

            // Rơi vật phẩm ID 750
            if (Util.isTrue(50 + levelPlayer / 2, 100) || (player.actived() && Util.isTrue(dropRate + levelPlayer / 2, 50))) {
                while (usedXPositions.contains(x)) {
                    x += 10;
                }
                usedXPositions.add(x);
                list.add(new ItemMap(zone, 750, 1, x, yEnd, player.id));
            }

            // Rơi vật phẩm ID 751
            if (Util.isTrue(50 + levelPlayer / 2, 100) || (player.actived() && Util.isTrue(dropRate + levelPlayer / 2, 50))) {
                while (usedXPositions.contains(x)) {
                    x += 10;
                }
                usedXPositions.add(x);
                list.add(new ItemMap(zone, 751, 1, x, yEnd, player.id));
            }
        }
        if (MapService.gI().isMapNguHanhSon(mapid) && this.tempId != 22) {
            if (Util.isTrue(20, 100) || (player.actived() && Util.isTrue(50, 100))) {
                player.event.addEventPointNHS(1);
            }
        }
        //========================Map Bang Hội========================
        if (MapService.gI().isMapUpPorata(mapid)) {
            int dropRate1 = 10;
            int dropRate2 = 5;
            int dropRate3 = 1;

            if (player.itemTime.isCoBonLa) {
                dropRate1 += 5;
                dropRate2 += 3;
                dropRate3 += 1;
            }

            if (Util.isTrue(dropRate1, 100)) {
                ItemMap it = new ItemMap(zone, 933, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, 1));
                list.add(it);
            } else if (Util.isTrue(dropRate2, 100) && player.itemEvent.canDropManhVo(150)) {
                ItemMap it = new ItemMap(zone, 934, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, 1));
                list.add(it);
            } else if (Util.isTrue(dropRate3, 500)) {
                ItemMap it = new ItemMap(zone, 935, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, 1));
                list.add(it);
            }
        }
        
        if (MapService.gI().isMapUpPoratavevang(mapid)) {
            if (Util.isTrue(1, 200)) {
                ItemMap it = new ItemMap(zone, 934, Util.nextInt(1, 3), x, yEnd, player.id);
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);
            } else if (Util.isTrue(1, 200)) {
                ItemMap it = new ItemMap(zone, 935, Util.nextInt(1, 3), x, yEnd, player.id);
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);
            } else if (Util.isTrue(1, 10)) {
                ItemMap it = new ItemMap(zone, 933, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, Util.nextInt(1, 10)));
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);
            }  else if (Util.isTrue(1, 50)) {
                ItemMap it = new ItemMap(zone, 1855, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(31, Util.nextInt(1, 10)));
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);
            }
        }

         //======================== Vàng Ngọc ========================
        if (MapService.gI().isMap3Planets(mapid)) {
            if (Util.isTrue(1, 20)) {
                int vang = Util.nextInt(500, 3000);
                if (vang < 1000) {
                    list.add(new ItemMap(zone, 76, vang, x, yEnd, player.id));
                } else if (vang < 2000) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                }
            }
        }
        
        if (MapService.gI().isMapNappa(mapid)) {
            if (Util.isTrue(1, 100)) {
                int vang = Util.nextInt(2000, 6000);
                if (vang < 3000) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else if (vang < 5000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id));
                }
            }
        }
        if (MapService.gI().AllMap(mapid)) {
            if (player != null) {
                player.monsterKillCountAutoTrain++;
            }

            if (player.event.luotNhanNgocMienPhi == 1) {
                ItemMap item1 = new ItemMap(zone, 77, 1, x, yEnd, player.id);
                ItemMap item2 = new ItemMap(zone, 77, 1, x + 10, yEnd, player.id);
                list.add(item1);
                list.add(item2);
                player.event.luotNhanNgocMienPhi = 0;
            }
        }
        
        if (MapService.gI().isMapCold(mapid)) {
            if (Util.isTrue(30, 100)) {
                int vang = Util.nextInt(150000, 250000);
                if (vang < 10000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id)); // Rơi vàng cấp 189
                } else if (vang < 14000) {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id)); // Rơi vàng cấp 190
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id)); // Rơi vàng cấp 190
                }
            }
        }
        
        if (MapService.gI().isMapTuongLai(mapid)) {
            if (Util.isTrue(15, 100)) {
                int vang = Util.nextInt(80000, 150000);
                if (vang < 6000) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else if (vang < 10000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id));
                }
            }
        }
        
        if (MapService.gI().isMapPhoBan(mapid)) {
            if (Util.isTrue(1, 100)) {
                int vang = Util.nextInt(80000, 200000);
                if (player.itemTime.isCoBonLa) {
                    vang = (int) (vang * 1.15);
                }
                if (vang < 6000) {
                    list.add(new ItemMap(zone, 188, vang, x, yEnd, player.id));
                } else if (vang < 10000) {
                    list.add(new ItemMap(zone, 189, vang, x, yEnd, player.id));
                } else {
                    list.add(new ItemMap(zone, 190, vang, x, yEnd, player.id));
                }
            }
        }
        
        if (Util.isTrue(1, 1000000)) {
            int ngoc = Util.nextInt(1, 1);
            list.add(new ItemMap(zone, 77, ngoc, x, yEnd, player.id));
        }
        
        if (MapService.gI().isMapUpSKH(mapid)) {
            int baseTileDrop = 1;
            int tileDrop = baseTileDrop;
            if (player.itemTime.isCoBonLa) {
                tileDrop = (int) (baseTileDrop * 1.15);
            }

            if (Util.isTrue(tileDrop, 9999)) {
                int soLuong = 1;
                list.add(new ItemMap(zone, 1634, soLuong, x, yEnd, player.id));
            }
        }
        if (MapService.gI().isMapDoanhTrai(mapid)) {
            int baseTileDrop = 20;
            int tileDrop = baseTileDrop;
            if (player.itemTime.isCoBonLa) {
                tileDrop = (int) (baseTileDrop * 1.15);
            }

            if (Util.isTrue(tileDrop, 100)) {
                int soLuong = 1;
                list.add(new ItemMap(zone, 1778, soLuong, x, yEnd, player.id));
            }
        }
        if (MapService.gI().isMapRiengTu(mapid)) {
            int baseTileDrop = 1;
            int tileDrop = baseTileDrop;
            if (player.itemTime.isCoBonLa) {
                tileDrop = (int) (baseTileDrop * 1.15);
            }

            if (Util.isTrue(tileDrop, 19999)) {
                int soLuong = 1;
                list.add(new ItemMap(zone, 1634, soLuong, x, yEnd, player.id));
            }
        }
        
        if (MapService.gI().isMapRiengTu(mapid)) {
            int baseTileDrop = 2;
            double tileDrop = baseTileDrop;

            int totalOption236Param = 0;
            for (Item item : player.inventory.itemsBody) {
                if (item != null && item.itemOptions != null) {
                    for (Item.ItemOption op : item.itemOptions) {
                        if (op.optionTemplate.id == 236) {
                            totalOption236Param += op.param;
                        }
                    }
                }
            }

            if (totalOption236Param > 100) {
                totalOption236Param = 100;
            }

            double percentFromOption236 = Math.pow(totalOption236Param / 100.0, 1.5) * 20.0; // max 20%
            tileDrop *= (1 + percentFromOption236 / 100.0);

            if (player.itemTime.isCoBonLa) {
                tileDrop *= 1.5;
            }

            // Check rơi đồ kích hoạt
            if (Util.isTrue((int) tileDrop, 9999)) {
                short itTemp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
                ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
                if (!ops.isEmpty()) {
                    it.options = ops;
                }

                int[] opsrand = ItemService.gI().randOptionItemKichHoat(player.gender);
                for (int opId : opsrand) {
                    if (opId > 0) {
                        it.options.add(new Item.ItemOption(opId, 0));
                    }
                }
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);

                //  ChatGlobalService.gI().ThongBaoRoiDo(player, player.name + " vừa nhặt được " + it.itemTemplate.name + " sét kích hoạt tại " + this.zone.map.mapName + " khu " + this.zone.zoneId);
            }
        }
        if (MapService.gI().isMapUpSKH(mapid)) {
            int baseTileDrop = 2;
            double tileDrop = baseTileDrop;

            int totalOption236Param = 0;
            for (Item item : player.inventory.itemsBody) {
                if (item != null && item.itemOptions != null) {
                    for (Item.ItemOption op : item.itemOptions) {
                        if (op.optionTemplate.id == 236) {
                            totalOption236Param += op.param;
                        }
                    }
                }
            }

            if (totalOption236Param > 100) {
                totalOption236Param = 100;
            }

            double percentFromOption236 = Math.pow(totalOption236Param / 100.0, 1.5) * 20.0; // max 20%
            tileDrop *= (1 + percentFromOption236 / 100.0);

            if (player.itemTime.isCoBonLa) {
                tileDrop *= 1.5;
            }

            // Check rơi đồ kích hoạt
            if (Util.isTrue((int) tileDrop, 9999)) {
                short itTemp = (short) ItemService.gI().randTempItemKichHoat(player.gender);
                ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
                if (!ops.isEmpty()) {
                    it.options = ops;
                }

                int[] opsrand = ItemService.gI().randOptionItemKichHoat(player.gender);
                for (int opId : opsrand) {
                    if (opId > 0) {
                        it.options.add(new Item.ItemOption(opId, 0));
                    }
                }
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);

                //  ChatGlobalService.gI().ThongBaoRoiDo(player, player.name + " vừa nhặt được " + it.itemTemplate.name + " sét kích hoạt tại " + this.zone.map.mapName + " khu " + this.zone.zoneId);
            }
        }

        //========================Đồ Sao Khác Vải Thô========================
        if (((Util.isTrue(1, 8000))) && MapService.gI().isMapUpSKH(mapid)) {
            int baseDropRate = 1;
            if (player.itemTime.isCoBonLa) {
                baseDropRate = 1;
                int coBonLaBonus = 15;
                baseDropRate = (int) (baseDropRate * (1 + coBonLaBonus / 100.0));
            }

            if (Util.isTrue(baseDropRate, 19999)) {
                short itTemp = (short) ItemService.gI().randTempItemDoSao(player.gender);
                ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
                if (!ops.isEmpty()) {
                    it.options = ops;
                }

                int randOption = Util.nextInt(100);
                boolean hasOption = false;
                if (randOption < 50) {
                    int randAddOption = Util.nextInt(100);
                    if (randAddOption < 50) {
                        it.options.add(new Item.ItemOption(107, 1));
                        hasOption = true;
                    } else if (randAddOption < 90) {
                        it.options.add(new Item.ItemOption(107, 2));
                        hasOption = true;
                    } else {
                        it.options.add(new Item.ItemOption(107, 3));
                        hasOption = true;
                    }
                }

                if (hasOption) {
                    list.add(it);
                    //  ChatGlobalService.gI().ThongBaoRoiDo(player, "[Hệ Thống] " + player.name + " vừa nhặt được " + it.itemTemplate.name + " Sét Kích Hoạt");
                }
            }
        }

        // END
        //========================Đồ Sao 3 Map Đầu========================
        if (((Util.isTrue(50, 50000))) && MapService.gI().isMapUpSKH(mapid)) {
            int baseRate = 50;
            if (player.itemTime.isCoBonLa) {
                baseRate = (int) (baseRate * (1 + 0.15));
            }

            int powerReduction = (int) Math.min(player.nPoint.power / 100000, 5) * 20;
            int finalRate = Math.max(baseRate - powerReduction, 0);

            if (finalRate > 0 && Util.nextInt(100) < finalRate) {
                short itTemp = (short) ItemService.gI().randDoSao(player.gender);
                ItemMap it = new ItemMap(zone, itTemp, 1, x, yEnd, player.id);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop(itTemp);
                if (!ops.isEmpty()) {
                    it.options = ops;
                }

                int randOption = Util.nextInt(1, 100);
                boolean hasOption = false;
                if (randOption < 50) {
                    int randAddOption = Util.nextInt(100);
                    if (randAddOption < 60) {
                        it.options.add(new Item.ItemOption(107, 1));
                        hasOption = true;
                    } else if (randAddOption < 90) {
                        it.options.add(new Item.ItemOption(107, 2));
                        hasOption = true;
                    } else {
                        it.options.add(new Item.ItemOption(107, 3));
                        hasOption = true;
                    }
                }
                if (hasOption) {
                    list.add(it);
                    //   ChatGlobalService.gI().ThongBaoRoiDo(player, "[ Hệ Thống ] " + player.name + " vừa nhặt được " + it.itemTemplate.name + " tại " + this.zone.map.mapName + " khu " + this.zone.zoneId);
                }
            }
        }

        //========================Đồ Thần + Thức Ăn========================
        if (MapService.gI().isMapCold(mapid)) {
            if (player.isPet) {
                player = ((Pet) player).master;
            }
            int rate = 1;
            if (player.itemTime.isCoBonLa) {
                rate = (int) (rate * 0.85);
            }
            if (Util.isTrue(1, 50000)) {
                ItemMap it = ItemService.gI().randDoTL(this.zone, 1, x, yEnd, player.id);
                list.add(it);
                //   ChatGlobalService.gI().ThongBaoRoiDo(player, "[ Hệ Thống ] " + player.name + " vừa nhặt được " + it.itemTemplate.name + " tại " + this.zone.map.mapName + " khu " + this.zone.zoneId);
            }
        }
        int rate1 = 100;
        if (player.itemTime.isCoBonLa) {
            rate1 = (int) (rate1 * 1.15);
        }
        if (Util.isTrue(3, 333)) {
            if (player.setClothes.checkSetGod()) {
                ItemMap it = new ItemMap(zone, Util.nextInt(663, 667), 1, x, yEnd, player.id);
                list.add(it);
            }
        }

        if (MapService.gI().isMapCold(mapid)) {
            if (Util.isTrue(20, 100)) {
                int rand = Util.nextInt(0, 4);
                ItemMap it = new ItemMap(zone, 220 + rand, 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(71 - rand, 0));
                list.add(it);
            }
        }

        if (MapService.gI().isMapDoanhTrai(mapid) && (Util.isTrue(10, 100))) {
            ItemMap it = new ItemMap(zone, 225, 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(74, 0));
            list.add(it);
        }

        if (MapService.gI().isMap3Planets(mapid) && (Util.isTrue(10, 100))) {
            ItemMap it = new ItemMap(zone, 225, 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(74, 0));
            list.add(it);
        }

        if (MapService.gI().isMap3Planets(mapid) || MapService.gI().isMapNappa(mapid) || MapService.gI().isMapTuongLai(mapid) || MapService.gI().isMapCold(mapid)) {
            int dropRate = 10;
            if (player.itemTime.isCoBonLa) {
                dropRate = (int) (dropRate * 1.15);
            }

            if (Util.isTrue(dropRate, 70) || (player.isActive() && Util.isTrue(1, 100))) {
                int rand = Util.nextInt(0, 1);
                ItemMap it = new ItemMap(zone, 19 + rand, 1, x, yEnd, player.id);
                list.add(it);
            }
        }
        if (player.setClothes.checkSetDes() && MapService.gI().isMapNgucTu(mapid)) {
            if ((player.isActive() && Util.isTrue(2, 555)) || Util.isTrue(10, 100)) {
                list.add(new ItemMap(zone, Util.nextInt(1066, 1070), 1, x, yEnd, player.id));
            }
        }
        if ((Util.isTrue(10, 100) || (player.isActive() && player.setClothes.checkSetDes() && Util.isTrue(20, 100))) && MapService.gI().isMapNgucTu(mapid)) {
            list.add(new ItemMap(zone, 1229, 1, x, yEnd, player.id));
        }

        if ((Util.isTrue(10, 100) && MapService.gI().isMapNguHanhSon(mapid))) {
            list.add(new ItemMap(zone, Util.nextInt(541, 542), 1, x, yEnd, player.id));
        }

        if (this.zone.map.mapId >= 0) {
            int dropRate = 1;
            if (player.itemTime.isCoBonLa) {
                dropRate = (int) (dropRate * 1.15);
            }

            if (Util.isTrue(dropRate, 100)) { // nro
                list.add(new ItemMap(zone, Util.nextInt(17, 20), 1, x, this.location.y, player.id));
            }
        }

        if (this.zone.map.mapId >= 0) {
            int dropRate = 10;
            if (player.itemTime.isCoBonLa) {
                dropRate = (int) (dropRate * 1.15);
            }

            if (Util.isTrue(dropRate, 100)) { // spl
                list.add(new ItemMap(Util.spl(zone, Util.nextInt(441, 447), 1, x, this.location.y, player.id)));
            }
        }

        //Sao pha le
        if (Util.isTrue(1, 100) || (player.nPoint.isDoSPL && Util.isTrue(5, 100)) || (player.actived() && Util.isTrue(3, 100))) {
            int rand = Util.nextInt(0, 6);
            ItemMap it = new ItemMap(zone, 441 + rand, 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(95 + rand, (rand == 3 || rand == 4) ? 3 : 5));
            list.add(it);
        }

        //Da nang cap
        if (Util.isTrue(1, 400) || (Util.isTrue(1, 300) && MapService.gI().isMapTuongLai(mapid)) || (player.actived() && Util.isTrue(1, 200))) {
            int rand = Util.nextInt(0, 4);
            ItemMap it = new ItemMap(zone, 220 + rand, 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(71 - rand, 0));
            list.add(it);
        }

        if (MapService.gI().isMapCold(mapid)) {
            if (player.isPet || player.isClone) {
                player = ((Pet) player).master;
            }
            if (Util.isTrue(1, 10000)) {
                ItemMap it = ItemService.gI().randDoTL(this.zone, 1, x, yEnd, player.id);
                list.add(it);
                ServerNotify.gI().notify(player.name + " vừa nhặt được " + it.itemTemplate.name + " tại " + this.zone.map.mapName + " khu " + this.zone.zoneId);
            }
            if (Util.isTrue(1, 400) && InventoryService.gI().fullSetThan(player)) {
                ItemMap it = new ItemMap(zone, Util.nextInt(663, 667), 1, x, yEnd, player.id);
                it.options.add(new Item.ItemOption(30, 0));
                list.add(it);
            }
        }

        if (MapService.gI().isMapTuongLai(mapid) && ((Util.isTrue(1, 1000) || (player.actived() && Util.isTrue(1, 300)))) && InventoryService.gI().fullSetThan(player)) {
            ItemMap it = new ItemMap(zone, Util.nextInt(663, 667), 1, x, yEnd, player.id);
            it.options.add(new Item.ItemOption(30, 0));
            list.add(it);
        }

        if (Util.isTrue(1, 100000) || (player.actived() && Util.isTrue(1, 10000))) {
            list.add(new ItemMap(zone, 457, 1, x, yEnd, player.id));
        }
        // Manh thien su
        if ((Util.isTrue(1, 1000) || (player.actived() && Util.isTrue(1, 300))) && MapService.gI().isMapHanhTinhThucVat(mapid) && InventoryService.gI().findItemNTK(player)) {
            list.add(new ItemMap(zone, Util.nextInt(1066, 1070), 1, x, yEnd, player.id));
        }
        if (Util.isTrue(1, 200) || (player.actived() && Util.isTrue(1, 100))) {
            list.add(new ItemMap(zone, 861, 1, x, yEnd, player.id));
        }
        return list;
    }

    private ItemMap dropItemTask(Player player) {
        ItemMap itemMap = null;
        switch (tempId) {
            case ConstMob.KHUNG_LONG:
            case ConstMob.LON_LOI:
            case ConstMob.QUY_DAT:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_2_0) {
                    itemMap = new ItemMap(zone, 73, 1, location.x, location.y, player.id);
                }
                break;
            case ConstMob.THAN_LAN_ME:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_8_1) {
                    if (Util.isTrue(1, 3)) {
                        itemMap = new ItemMap(zone, 20, 1, location.x, location.y, player.id);
                    } else {
                        Service.gI().sendThongBao(player, "Con thằn lằn mẹ này không giữ ngọc, hãy tìm con thằn lằn mẹ khác");
                    }
                }
            case ConstMob.OC_MUON_HON:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_14_1) {
                    if (Util.isTrue(1, 3)) {
                        itemMap = new ItemMap(zone, 85, 1, location.x, location.y, player.id);
                    } else {
                        Service.gI().sendThongBao(player, "Con ốc mượn hồn này không giữ truyện tranh, hãy thử tìm con ốc mượn hồn khác");
                    }
                }
            case ConstMob.HEO_XAYDA_ME:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_14_1) {
                    if (Util.isTrue(1, 3)) {
                        itemMap = new ItemMap(zone, 85, 1, location.x, location.y, player.id);
                    } else {
                        Service.gI().sendThongBao(player, "Con heo xayda mẹ này không giữ truyện tranh, hãy thử tìm con heo xayda mẹ khác");
                    }
                }
            case ConstMob.OC_SEN:
                if (TaskService.gI().getIdTask(player) == ConstTask.TASK_14_1) {
                    if (Util.isTrue(1, 3)) {
                        itemMap = new ItemMap(zone, 85, 1, location.x, location.y, player.id);
                    } else {
                        Service.gI().sendThongBao(player, "Con ốc xên này không giữ truyện tranh, hãy thử tìm con ốc xên khác");
                    }
                }
        }
        if (itemMap != null) {
            return itemMap;
        }
        return null;
    }

    private void sendMobStillAliveAffterAttacked(long dameHit, boolean crit) {
        Message msg;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writeLongByEmti(Util.maxIntValue(this.point.gethp()), ConstNpcConfig.readInt);
            msg.writeLongByEmti(Util.maxIntValue(dameHit), ConstNpcConfig.readInt);
            msg.writer().writeBoolean(crit); // chí mạng
            msg.writer().writeInt(-1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void hoiSinhMobPhoBan() {
        this.point.hp = this.point.maxHp;
        this.setTiemNang();
        Message msg;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(this.lvMob); //level mob
            msg.writeLongByEmti(Util.maxIntValue(this.point.hp), ConstNpcConfig.readInt);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void hoiSinhMobTayKarin() {
        this.point.hp = this.point.maxHp;
        this.maxTiemNang = 1;
        Message msg;
        try {
            msg = new Message(-13);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(this.tempId);
            msg.writer().writeByte(this.lvMob); //level mob
            msg.writeLongByEmti(Util.maxIntValue(this.point.hp), ConstNpcConfig.readInt);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendSieuQuai(int type) {
        Message msg;
        try {
            msg = new Message(-75);
            msg.writer().writeByte(this.id);
            msg.writer().writeByte(type);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendDisable(boolean bool) {
        Message msg;
        try {
            msg = new Message(81);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendDoneMove(boolean bool) {
        Message msg;
        try {
            msg = new Message(82);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendFire(boolean bool) {
        Message msg;
        try {
            msg = new Message(85);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendIce(boolean bool) {
        Message msg;
        try {
            msg = new Message(86);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendWind(boolean bool) {
        Message msg;
        try {
            msg = new Message(87);
            msg.writer().writeByte(this.id);
            msg.writer().writeBoolean(bool);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    public void sendMobMaxHp(long maxHp) {
        Message msg;
        try {
            msg = new Message(87);
            msg.writer().writeByte(this.id);
            msg.writeLongByEmti(Util.maxIntValue(maxHp), ConstNpcConfig.readInt);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }

    private void phanSatThuong(Player plTarget, long dame) {
        if (plTarget.nPoint == null) {
            return;
        }
        int percentPST = plTarget.nPoint.tlPST;
        if (percentPST != 0) {
            long damePST = Util.maxIntValue(dame * percentPST / 100L);
            Message msg;
            try {
                msg = new Message(-9);
                msg.writer().writeByte(this.id);
                if (damePST >= this.point.hp) {
                    damePST = this.point.hp - 1;
                }
                long hpMob = Util.maxIntValue(this.point.hp);
                injured(null, damePST, true);
                damePST = hpMob - this.point.hp;
                msg.writeLongByEmti(Util.maxIntValue(this.point.hp), ConstNpcConfig.readInt);
                msg.writeLongByEmti(Util.maxIntValue(damePST), ConstNpcConfig.readInt);
                msg.writer().writeBoolean(false);
                msg.writer().writeByte(36);
                Service.gI().sendMessAllPlayerInMap(this.zone, msg);
                msg.cleanup();
            } catch (IOException e) {
            }
        }
    }

    public void startDie() {
        Message msg;
        try {
            setDie();
            this.point.hp = -1;
            this.status = 0;
            msg = new Message(-12);
            msg.writer().writeByte(this.id);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
            msg.cleanup();
        } catch (IOException e) {
        }
    }
}
