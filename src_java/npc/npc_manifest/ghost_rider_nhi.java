package npc.npc_manifest;

import consts.ConstNpc;
import npc.Npc;
import player.Player;
import Top.weekly.WeeklyTopService;

public class ghost_rider_nhi extends Npc {

    public ghost_rider_nhi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            switch (mapId) {
                case 5 -> {
                    String currentTopInfo = WeeklyTopService.gI().getWeeklyTopInfo();
                    createOtherMenu(player, ConstNpc.BASE_MENU, currentTopInfo,
                            "Top", "Top Tuần", "Đóng");
                }

                default ->
                    super.openBaseMenu(player);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.iDMark.isBaseMenu()) {
                switch (mapId) {
                    case 5 -> {
                        switch (select) {
                            case 0 -> {
                                // Top option - not implemented yet
                            }
                            case 1 -> {
                                // Top Tuần option - show weekly top menu
                                createOtherMenu(player, 100, "Đua Top Tuần",
                                        "Xem Xếp Hạng", "Nhận Thưởng", "Hạng Của Tôi", "Quay lại");
                            }
                        }
                    }
                }
            } else if (player.iDMark.getIndexMenu() == 100) {
                // Handle weekly top sub-menu
                switch (select) {
                    case 0 -> {
                        // Xem Xếp Hạng
                        WeeklyTopService.gI().showRankingsWithAvatar(player);
                    }
                    case 1 -> {
                        // Nhận Thưởng
                        WeeklyTopService.gI().processClaimReward(player);
                    }
                    case 2 -> {
                        // Hạng Của Tôi
                        WeeklyTopService.gI().showPlayerRank(player);
                    }
                    case 3 -> {
                        // Quay lại
                        openBaseMenu(player);
                    }
                }
            }
        }
    }
}
