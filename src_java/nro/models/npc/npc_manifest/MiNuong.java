///*
// * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
// * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
// */
//package nro.models.npc.npc_manifest;
//
//import boss.boss_manifest.replica.MapMiNuong;
//import clan.ClanMember;
//import consts.ConstNpc;
//import nro.models.npc.Npc;
//import nro.player.Player;
//import utils.TimeUtil;
//
//
///**
// *
// * @author admin
// */
//public class MiNuong extends Npc {
//
//    public MiNuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
//        super(mapId, status, cx, cy, tempId, avartar);
//    }
//
//    @Override
//    public void openBaseMenu(Player player) {
//        if (canOpenNpc(player)) {
//            if (this.mapId == 27) {
//                if (player.clan == null) {
//                    this.createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_MI_NUONG,
//                            "Chỉ tiếp các Bang Hội", "Đóng");
//                } else {
//                    ClanMember clanMember = player.clan.getClanMember((int) player.id);
//                    int days = (int) (((System.currentTimeMillis() / 1000) - clanMember.joinTime) / 60 / 60 / 24);
////                                        if (days < 1) {
////                                            NpcService.gI().createTutorial(player, avartar,
////                                                    "Chỉ những thành viên gia nhập bang hội tối thiểu 1 ngày mới có thể tham gia");
////                                            return;
////                                        }
//                    if (!player.clan.haveGoneMiNuong && player.clan.miNuong != null) {
//                        createOtherMenu(player, ConstNpc.MENU_OPENED_MI_NUONG,
//                                "Bang hội của con đang tham gia phó bản Mị Nương\n" + "Thời gian còn lại là "
//                                + TimeUtil.getMinLeft(player.clan.miNuong.getLastTimeOpen(), MapMiNuong.TIME_MI_NUONG / 1000)
//                                + " phút.\nCon có muốn tham gia không?",
//                                "Đi thôi", "Không");
//                    } else {
//                        if (!player.isAdmin() && player.clanMember.getNumDateFromJoinTimeToToday() < 1) {
//                            createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_MI_NUONG,
//                                    "Bang hội chỉ cho phép những người ở trong bang trên 1 ngày. Hẹn ngươi quay lại vào lúc khác",
//                                    "Ok");
//                        } else if (player.clan.haveGoneMiNuong) {
//                            createOtherMenu(player, ConstNpc.MENU_KHONG_CHO_VAO_MI_NUONG,
//                                    "Bang hội của ngươi đã đi Mị nương lúc " + Util.formatTime(player.clan.miNuong_lastTimeOpen)
//                                    + " hôm nay. Người mở " + "(" + player.clan.miNuong_playerOpen + ")."
//                                    + " Hẹn ngươi quay lại vào ngày mai", "Ok");
//
//                        } else {
//                            createOtherMenu(player, ConstNpc.MENU_OPEN_MI_NUONG,
//                                    "Hôm nay bang hội của ngươi chưa vào Mị nương lần nào.\nNgươi có muốn vào không?",
//                                    "Ok", "Không");
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    public void confirmMenu(Player player, int select) {
//        if (canOpenNpc(player)) {
//            if (this.mapId == 27) {
//                switch (player.iDMark.getIndexMenu()) {
//                    case ConstNpc.MENU_OPENED_MI_NUONG:
//                        if (select == 0) {
//                            MiNuongService.gI().joinMiNuong(player);
//                        }
//                        break;
//                    case ConstNpc.MENU_OPEN_MI_NUONG:
//                        switch (select) {
//                            case 0:
//                                Object level = PLAYERID_OBJECT.get(player.id);
//                                MiNuongService.gI().openMiNuong(player);
//                                break;
//                        }
//                        break;
//                }
//            }
//        }
//    }
//}
