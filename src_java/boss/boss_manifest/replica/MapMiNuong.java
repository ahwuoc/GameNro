///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package boss.boss_manifest.replica;
//
//import clan.Clan;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//import lombok.Data;
//import map.Zone;
//import mob.Mob;
//import nro.models.npc.npc_manifest.MiNuong;
//import nro.player.Player;
//import nro.services.ItemTimeService;
//import services.func.ChangeMapService;
//
///**
// *
// * @author Louis Vegeta - Nguyễn Tuấn Bình
// */
//@Data
//public class MapMiNuong {
//
//    public static final long POWER_CAN_GO_TO_MI_NUONG = 2000000000L;
//
//    public static final int N_PLAYER_CLAN = 2;
//    public static final int AVAILABLE = 50; // số lượng bdkb tối đa trong server
//    public static final int TIME_MI_NUONG = 1800000;
//    private int id;
//    public List<Zone> zones;
//    private Clan clan;
//
//    private long lastTimeOpen;
//
//    List<Integer> listMap = Arrays.asList(210, 211, 212);
//    private int currentIndexMap = -1;
//
//    public boolean timePickReward;
//
//    public final List<MiNuong> bosses;
//
//    public MapMiNuong(int id) {
//        this.id = id;
//        this.zones = new ArrayList<>();
//        this.bosses = new ArrayList<>();
//    }
//
//    public Zone getMapById(int mapId) {
//        for (Zone zone : this.zones) {
//            if (zone.map.mapId == mapId) {
//                return zone;
//            }
//        }
//        return null;
//    }
//
//    public void openMiNuong(Player player) {
//        this.lastTimeOpen = System.currentTimeMillis();
//        this.clan = player.clan;
//        player.clan.miNuong = this;
//        player.clan.miNuong_playerOpen = player.name;
//        player.clan.miNuong_lastTimeOpen = this.lastTimeOpen;
//        player.miNuong_isJoinBdkb = true;
//        player.miNuong_countPerDay++;
//        player.clan.haveGoneMiNuong = true;
//        player.miNuong_lastTimeJoin = System.currentTimeMillis();
//        ChangeMapService.gI().goToMiNuong(player);
//        for (Player pl : player.clan.membersInGame) {
//            if (pl == null || pl.zone == null) {
//                continue;
//            }
//            sendTextmiNuong();
//        }
//    }
//
//    public void init() {
//        //Hồi sinh quái và thả boss
//        for (Zone zone : this.zones) {
//            if (zone.map.mapId == this.listMap.get(this.currentIndexMap)) {
//                for (Mob mob : zone.mobs) {
//                    mob.point.dame = (int) (80000);
//                    mob.point.maxHp = (int) (500000000);
//                    mob.hoiSinh();
//                }
//            }
//        }
//        for (MiNuong boss : bosses) {
//            boss.leaveMap();
//        }
//        this.bosses.clear();
//        initBoss();
//    }
//
//    private void initBoss() {
//        this.bosses.add(new MiNuong(this));
//    }
//
//    public void sendTextmiNuong() {
//        for (Player pl : this.clan.membersInGame) {
//            ItemTimeService.gI().sendTextMiNuong(pl);
//        }
//    }
//
//}
