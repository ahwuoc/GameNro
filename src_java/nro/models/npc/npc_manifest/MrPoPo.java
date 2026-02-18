package nro.models.npc.npc_manifest;

import clan.Clan;
import clan.ClanMember;
import consts.ConstNpc;
import models.DestronGas.DestronGas;
import models.DestronGas.DestronGasService;
import nro.models.npc.Npc;
import static nro.models.npc.NpcFactory.PLAYERID_OBJECT;
import nro.player.Player;
import nro.services.NpcService;
import services.func.Input;
import services.func.TopService;
import utils.TimeUtil;

public class MrPoPo extends Npc {

    public MrPoPo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player) || this.mapId != 0) {
            return;
        }

        String message = """
                Thượng Đế vừa phát hiện ra 1 loại khí đang âm thầm
                hủy diệt mọi mầm sống trên Trái Đất,
                nó được gọi là Destron Gas.
                Ta sẽ đưa các cậu đến nơi ấy, các cậu đã sẵn sàng chưa?
                """;

        if (player.clan != null) {
            createOtherMenu(player, ConstNpc.BASE_MENU, message,
                    "Thông tin\nChi tiết", "Top 100\nBang hội",
                    "Thành tích\nBang", "OK", "Từ chối");
        } else {
            createOtherMenu(player, ConstNpc.BASE_MENU, message,
                    "Thông tin\nChi tiết", "Top 100\nBang hội",
                    "OK", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player) || this.mapId != 0) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            if (player.clan != null) {
                // Có bang hội
                switch (select) {
                    case 0 -> NpcService.gI().createTutorial(player, tempId, this.avartar,
                            ConstNpc.HUONG_DAN_KHI_GAS_HUY_DIET);
                    case 1 -> TopService.gI().showTopClanKhiGas(player);
                    case 2 -> TopService.gI().showMyTopClanKhiGas(player);
                    case 3 -> handleJoinOrCreateDestronGas(player);
                    default -> { }
                }
            } else {
                // Không có bang hội
                switch (select) {
                    case 0 -> NpcService.gI().createTutorial(player, tempId, this.avartar,
                            ConstNpc.HUONG_DAN_KHI_GAS_HUY_DIET);
                    case 1 -> TopService.gI().showTopClanKhiGas(player);
                    case 2 -> NpcService.gI().createTutorial(player, tempId, this.avartar,
                            "Cần tham gia bang hội để sử dụng chức năng này!");
                    default -> {
                    }
                }
            }
        } else if (player.iDMark.getIndexMenu() == 2) {
            if (select == 0) {
                
                if (player.clan == null) {
                    return;
                }

                if (player.clanMember.getNumDateFromJoinTimeToToday() < 1) {
                    NpcService.gI().createTutorial(player, tempId, this.avartar,
                            "Gia nhập bang hội trên 1 ngày mới được tham gia");
                    return;
                }
                
                if (player.clan.KhiGasHuyDiet == null) {
                    DestronGasService.gI().openKhiGasHuyDiet(
                            player,
                            Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id)))
                    );
                } else {
                    DestronGasService.gI().openKhiGasHuyDiet(player, (byte) 0);
                }
            }
        }
    }

    private void handleJoinOrCreateDestronGas(Player player) {
        Clan clan = player.clan;
        if (clan == null) return;

        ClanMember cm = clan.getClanMember((int) player.id);
        if (cm == null) return;

        // Nếu bang hội đang tham gia
        if (clan.KhiGasHuyDiet != null) {
            createOtherMenu(player, 2,
                    "Bang hội của cậu đang tham gia Destron Gas cấp độ "
                            + clan.KhiGasHuyDiet.level
                            + "\nCậu có muốn đi cùng họ không ? ("
                            + TimeUtil.convertTimeNow(clan.KhiGasHuyDiet.getLastTimeOpen())
                            + " trước)",
                    "Đồng ý", "Từ chối");
            return;
        }

        // Chỉ bang chủ mới được mở
        if (!clan.isLeader(player)) {
            NpcService.gI().createTutorial(player, tempId, this.avartar,
                    "Chức năng chỉ dành cho bang chủ");
            return;
        }

        // Bang phải đủ thành viên
        if (clan.members.size() < DestronGas.N_PLAYER_CLAN) {
            NpcService.gI().createTutorial(player, tempId, this.avartar,
                    "Bang hội phải có ít nhất "
                            + DestronGas.N_PLAYER_CLAN
                            + " thành viên mới có thể tham gia");
            return;
        }

        // Mở form chọn cấp độ
        Input.gI().createFormChooseLevelKGHD(player);
    }
}
