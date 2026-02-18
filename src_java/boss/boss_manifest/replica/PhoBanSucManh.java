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
//import nro.player.Player;
//import nro.services.ItemTimeService;
//import services.func.ChangeMapService;
//
//
///**
// *
// * @author Louis Vegeta - Nguyễn Tuấn Bình
// */
//@Data
//public class PhoBanSucManh {
//
//    public static final long POWER_CAN_GO_TO_MAPX2 = 2000000000L;
//
//    public static final int N_PLAYER_CLAN = 0;
//    public static final int AVAILABLE = 100; // số lượng bdkb tối đa trong server
//    //public static int TIME_MAPX2 = 3000000;
//
//    private int TIME_MAPX2;
//    private int id;
//    public List<Zone> zones;
//    private Clan clan;
//
//    private long lastTimeOpen;
//    List<Integer> listMap = Arrays.asList(222);
//    private int currentIndexMap = -1;
//
//    public boolean timePickReward;
//    public byte capDo;
//
//    public PhoBanSucManh(int id) {
//        this.id = id;
//        this.zones = new ArrayList<>();
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
//    public void openPhoBanSucManh(Player player, byte capDo) {
//        this.lastTimeOpen = System.currentTimeMillis();
//        this.clan = player.clan;
//        this.TIME_MAPX2 = capDo * 3600000;
//        player.clan.pBanSucManh = this;
//        player.clan.pBanSucManh_playerOpen = player.name;
//        player.clan.pBanSucManh_lastTimeOpen = this.lastTimeOpen;
//        player.pBanSucManh_isJoinpBanSucManh = true;
//        player.pBanSucManh_countPerDay++;
//        player.clan.haveGonepBanSucManh = true;
//        player.pBanSucManh_lastTimeJoin = System.currentTimeMillis();
//        ChangeMapService.gI().goTopBanSucManh(player);
//        for (Player pl : player.clan.membersInGame) {
//            if (pl == null || pl.zone == null) {
//                continue;
//            }
//            sendTextPhoBan();
//        }
//    }
//
//    public void init() {
//    }
//
//    public void sendTextPhoBan() {
//        for (Player pl : this.clan.membersInGame) {
//            ItemTimeService.gI().sendTextMapx2(pl);
//        }
//    }
//
//    // Phương thức getter cho TIME_MAPX2
//    public int getTIME_MAPX2() {
//        return this.TIME_MAPX2;
//    }
//
//}
