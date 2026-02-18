package nro.models.npc.npc_manifest;

/**
 *
 * Box ZALO: https://zalo.me/g/ifjict764 SĐT Zalo: 0358176187 Chuyên chỉnh sửa -
 * mua bán source NRO
 *
 */
import boss.BossID;
import consts.ConstNpc;
import models.SnakeWay.SnakeWayService;
import models.Training.TrainingService;
import nro.models.npc.Npc;
import static nro.models.npc.NpcFactory.PLAYERID_OBJECT;
import nro.player.Player;
import nro.services.NpcService;
import nro.services.Service;
import services.func.ChangeMapService;
import services.func.Input;
import services.func.TopService;
import utils.TimeUtil;

public class ThanVuTru extends Npc {

    public ThanVuTru(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (this.mapId == 48) {
            switch (player.levelLuyenTap) {
                case 4 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Thượng đế đưa ngươi đến đây, chắc muốn ta dạy võ chứ gì\n"
                            + "Bắt được con khỉ Bubbles rồi hãy tính",
                            player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động"
                                    : "Đăng ký\ntập\ntự động",
                            "Tập luyện\nvới\nBubbles",
                            "Thách đấu\nBubbles",
                            "Di chuyển");

                case 5 ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ta là Thần Vũ Trụ Phương Bắc, cai quản khu vực Bắc Vũ Trụ.\n"
                            + "Nếu thắng được ta, ngươi sẽ được đến Lãnh Địa Kaio, nơi ở của Thần Linh.",
                            player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động"
                                    : "Đăng ký\ntập\ntự động",
                            "Tập luyện\nvới\nThần Vũ Trụ",
                            "Thách đấu\nThần Vũ Trụ",
                            "Di chuyển");

                default ->
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Con mạnh nhất phía Bắc Vũ Trụ này rồi đấy.\n"
                            + "Nhưng ngoài vũ trụ bao la kia vẫn có những kẻ mạnh hơn nhiều.\n"
                            + "Con cần phải tập luyện để mạnh hơn nữa!",
                            player.dangKyTapTuDong ? "Hủy đăng\nký tập\ntự động"
                                    : "Đăng ký\ntập\ntự động",
                            "Tập luyện\nvới\nBubbles",
                            "Tập luyện\nvới\nThần Vũ Trụ",
                            "Di chuyển");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (this.mapId == 48) {
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    // Đăng ký tập tự động
                    case 0 -> {
                        if (player.dangKyTapTuDong) {
                            player.dangKyTapTuDong = false;
                            NpcService.gI().createTutorial(player, tempId, avartar,
                                    "Con đã hủy thành công đăng ký tập tự động.\n"
                                    + "Từ giờ con muốn tập Offline hãy tự đến đây trước.");
                            return;
                        }
                        this.createOtherMenu(player, 2001,
                                "Đăng ký để mỗi khi Offline quá 30 phút, con sẽ được tự động luyện tập "
                                + "với tốc độ 1280 sức mạnh mỗi phút.",
                                "Hướng\ndẫn\nthêm",
                                "Đồng ý\n1 ngọc\nmỗi lần",
                                "Không\nđồng ý");
                    }

                    // Tập luyện
                    case 1 -> {
                        switch (player.levelLuyenTap) {
                            case 5 ->
                                this.createOtherMenu(player, 2002,
                                        "Con có chắc muốn tập luyện?\n"
                                        + "Tập luyện với ta sẽ tăng 640 sức mạnh mỗi phút.",
                                        "Đồng ý\nluyện tập",
                                        "Không\nđồng ý");
                            default ->
                                this.createOtherMenu(player, 2002,
                                        "Con có chắc muốn tập luyện?\n"
                                        + "Tập luyện với Khỉ Bubbles sẽ tăng 320 sức mạnh mỗi phút.",
                                        "Đồng ý\nluyện tập",
                                        "Không\nđồng ý");
                        }
                    }

                    // Thách đấu
                    case 2 -> {
                        switch (player.levelLuyenTap) {
                            case 4 ->
                                this.createOtherMenu(player, 2003,
                                        "Con có chắc muốn thách đấu?\n"
                                        + "Nếu thắng Khỉ Bubbles, con sẽ được tập với ta, tăng 640 sức mạnh mỗi phút.",
                                        "Đồng ý\ngiao đấu",
                                        "Không\nđồng ý");
                            case 5 ->
                                this.createOtherMenu(player, 2003,
                                        "Con có chắc muốn thách đấu?\n"
                                        + "Nếu thắng được ta, con sẽ được học võ với người mạnh hơn, tăng đến 1280 sức mạnh mỗi phút.",
                                        "Đồng ý\ngiao đấu",
                                        "Không\nđồng ý");
                            default ->
                                this.createOtherMenu(player, 2003,
                                        "Con có chắc muốn tập luyện?\n"
                                        + "Tập luyện với ta sẽ tăng 640 sức mạnh mỗi phút.",
                                        "Đồng ý\nluyện tập",
                                        "Không\nđồng ý");
                        }
                    }

                    // Di chuyển
                    case 3 ->
                        this.createOtherMenu(player, ConstNpc.MENU_DI_CHUYEN,
                                "Ta sẽ đưa con đi",
                                "Về\nthần điện",
                                "Thánh địa\nKaio",
                                "Con\nđường\nrắn độc",
                                "Từ chối");
                }

            } else if (player.iDMark.getIndexMenu() == 2001) {
                // Menu đăng ký tập tự động
                switch (select) {
                    case 0 ->
                        NpcService.gI().createTutorial(player, tempId, avartar, ConstNpc.TAP_TU_DONG);
                    case 1 -> {
                        player.mapIdDangTapTuDong = mapId;
                        player.dangKyTapTuDong = true;
                        NpcService.gI().createTutorial(player, tempId, avartar,
                                "Từ giờ, quá 30 phút Offline con sẽ được tự động luyện tập.");
                    }
                }

            } else if (player.iDMark.getIndexMenu() == 2002) {
                // Gọi Boss để tập luyện
                switch (player.levelLuyenTap) {
                    case 5 ->
                        TrainingService.gI().callBoss(player, BossID.THAN_VU_TRU, false);
                    default ->
                        TrainingService.gI().callBoss(player, BossID.KHI_BUBBLES, false);
                }

            } else if (player.iDMark.getIndexMenu() == 2003) {
                // Gọi Boss để thách đấu
                switch (player.levelLuyenTap) {
                    case 4 ->
                        TrainingService.gI().callBoss(player, BossID.KHI_BUBBLES, true);
                    case 5 ->
                        TrainingService.gI().callBoss(player, BossID.THAN_VU_TRU, true);
                    default ->
                        TrainingService.gI().callBoss(player, BossID.THAN_VU_TRU, false);
                }

            } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_DI_CHUYEN) {
                // Menu di chuyển
                switch (select) {
                    case 0 ->
                        ChangeMapService.gI().changeMapBySpaceShip(player, 45, -1, 354);
                    case 1 ->
                        ChangeMapService.gI().changeMap(player, 50, -1, 318, 336);
                    case 2 -> {
                        if (player.clan != null) {
                            if (player.clan.ConDuongRanDoc != null) {
                                if (true) {
                                    Service.gI().sendThongBao(player, "Chức năng tạm đóng");
                                    return;
                                }
                                this.createOtherMenu(player, 2,
                                        "Bang hội con đang ở con đường rắn độc cấp độ "
                                        + player.clan.ConDuongRanDoc.level
                                        + "\nCon có muốn đi cùng họ không? ("
                                        + TimeUtil.convertTimeNow(player.clan.ConDuongRanDoc.getLastTimeOpen())
                                        + " trước)",
                                        "Top\nBang hội",
                                        "Thành tích\nBang",
                                        "Đồng ý",
                                        "Từ chối");
                            } else {
                                this.createOtherMenu(player, 2,
                                        "Hãy mau trở về bằng con đường rắn độc.\n"
                                        + "Bọn Xayda đã đến Trái Đất!",
                                        "Top\nBang hội",
                                        "Thành tích\nBang",
                                        "Chọn\ncấp độ",
                                        "Từ chối");
                            }
                        } else {
                            NpcService.gI().createTutorial(player, tempId, this.avartar,
                                    "Hãy vào bang hội trước.");
                        }
                    }
                }
            } else if (player.iDMark.getIndexMenu() == 2) {
                // Menu CDRD
                switch (select) {
                    case 0 ->
                        TopService.gI().showTopClanCDRD(player);
                    case 1 ->
                        TopService.gI().showMyTopClanCDRD(player);
                    case 2 -> {
                        if (player.clan == null) {
                            return;
                        }
                        
                        if (player.clanMember.getNumDateFromJoinTimeToToday() < 2) {
                            NpcService.gI().createTutorial(player, tempId, this.avartar,
                                    "Gia nhập bang hội trên 2 ngày mới được tham gia");
                            return;
                        }

                        if (player.clan.ConDuongRanDoc == null) {
                            Input.gI().createFormChooseLevelCDRD(player);
                        } else {
                            SnakeWayService.gI().openConDuongRanDoc(player, (byte) 0);
                        }
                    }
                }

            } else if (player.iDMark.getIndexMenu() == 3) {
                // Đi vào CDRD
                if (select == 0) {
                    if (player.clan.ConDuongRanDoc != null) {
                        SnakeWayService.gI().openConDuongRanDoc(player, (byte) 0);
                    } else {
                        SnakeWayService.gI().openConDuongRanDoc(player,
                                Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
                    }
                }
            }
        }
    }
}
