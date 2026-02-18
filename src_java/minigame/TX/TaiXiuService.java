/*
 * ENZEEFX_NROxBarColl - TaiXiuService
 */
package minigame.TX;

import consts.ConstMiniGame;
import consts.ConstNpc;
import consts.ConstTask;
import nro.player.Player;
import nro.services.Service;
import nro.services.TaskService;
import services.func.Input;
import nro.models.npc.Npc;

public class TaiXiuService {

    private static TaiXiuService instance;

    public static TaiXiuService gI() {
        if (instance == null) {
            instance = new TaiXiuService();
        }
        return instance;
    }

    // 🔽 Điểm vào duy nhất từ NPC
    public void confirmMenu(Npc npc, Player player, int select, int indexMenu) {
        switch (indexMenu) {
            case ConstMiniGame.MENU_TAI_XIU ->
                handleMainMenu(npc, player, select);
            case ConstMiniGame.MINIGAME_TAIXIU_TUYCHON ->
                handleTuyChon(npc, player, select);
            case ConstMiniGame.MINIGAME_TAIXIU_TAI ->
                handleTai(npc, player, select);
            case ConstMiniGame.MINIGAME_TAIXIU_XIU ->
                handleXiu(npc, player, select);
        }
    }

    // ========== MENU CHÍNH ==========
    private void handleMainMenu(Npc npc, Player player, int select) {
        switch (select) {
            case 0 ->
                npc.createOtherMenu(
                        player,
                        ConstNpc.IGNORE_MENU,
                        ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_HD),
                        "Ok"
                );
            case 1 -> {
                if (TaiXiu.baotri) {
                    npc.createOtherMenu(
                            player,
                            ConstMiniGame.MINIGAME_BAOTRI,
                            ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU),
                            "Cập nhập", "Đóng"
                    );
                    return;
                }
                if (player.goldTai == 0 && player.goldXiu == 0) {
                    npc.createOtherMenu(
                            player,
                            ConstMiniGame.MINIGAME_TAIXIU_TUYCHON,
                            ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_EMPTY),
                            "Cập nhập", "Đặt Tài", "Đặt Xỉu", "Đóng"
                    );
                } else if (player.goldTai > 0) {
                    if (player.isAdmin()) {
                        npc.createOtherMenu(
                                player,
                                ConstMiniGame.MINIGAME_TAIXIU_TAI,
                                ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_TAI),
                                "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập",
                                "Thêm Tài", "Thêm Xỉu", "Đóng"
                        );
                    } else {
                        // Non-admin: include Thêm Xỉu so indices match handleTai non-admin
                        npc.createOtherMenu(
                                player,
                                ConstMiniGame.MINIGAME_TAIXIU_TAI,
                                ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_TAI),
                                "Cập nhập", "Thêm Tài", "Thêm Xỉu", "Đóng"
                        );
                    }
                } else if (player.goldXiu > 0) {
                    if (player.isAdmin()) {
                        npc.createOtherMenu(
                                player,
                                ConstMiniGame.MINIGAME_TAIXIU_XIU,
                                ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_XIU),
                                "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập",
                                "Thêm Xỉu", "Đóng"
                        );
                    } else {
                        npc.createOtherMenu(
                                player,
                                ConstMiniGame.MINIGAME_TAIXIU_XIU,
                                ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_XIU),
                                "Cập nhập", "Thêm Xỉu", "Đóng"
                        );
                    }
                }
            }
        }
    }

    // ========== MENU TUỲ CHỌN ==========
    private void handleTuyChon(Npc npc, Player player, int select) {
        if (player.goldTai == 0 && player.goldXiu == 0 && !TaiXiu.baotri) {
            switch (select) {
                case 0 ->
                    npc.createOtherMenu(
                            player,
                            ConstMiniGame.MINIGAME_TAIXIU_TUYCHON,
                            ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_EMPTY),
                            "Cập nhập", "Đặt Tài", "Đặt Xỉu", "Đóng"
                    );
                case 1 ->
                    openTaiMenu(npc, player);
                case 2 ->
                    openXiuMenu(npc, player);
            }
        }
    }

    // ========== MENU TÀI ==========
    private void handleTai(Npc npc, Player player, int select) {
        if (player.goldTai >= 0 && !TaiXiu.baotri) {
            if (player.isAdmin()) {
                switch (select) {
                    // admin: 0..2 = Auto Tài/Auto Xỉu/Tam Hoa -> force + reload menu
                    case 0, 1, 2 -> {
                        forceResult(player, select);
                        openTaiMenu(npc, player); // Load lại menu
                    }
                    // admin: 3 = Cập nhập (reload)
                    case 3 -> openTaiMenu(npc, player);
                    // admin: 4 = Thêm Tài (open input form -> menu should close)
                    case 4 -> placeTai(player);
                    // admin: 5 = Thêm Xỉu (open input form -> menu should close)
                    case 5 -> placeXiu(player);
                    default -> {
                        // Nếu bấm "Đóng" (index 6) hoặc out of range -> không làm gì (menu đóng)
                    }
                }
            } else {
                // non-admin menu buttons: 0=Cập nhập,1=Thêm Tài,2=Thêm Xỉu,3=Đóng
                switch (select) {
                    case 0 -> openTaiMenu(npc, player); // Cập nhập (reload)
                    case 1 -> placeTai(player); // Thêm Tài -> open input (menu closes)
                    case 2 -> placeXiu(player); // Thêm Xỉu -> open input (menu closes)
                    default -> {
                        // 3 = Đóng -> không làm gì
                    }
                }
            }
        }
    }

    // ========== MENU XỈU ==========
    private void handleXiu(Npc npc, Player player, int select) {
        if (player.goldXiu >= 0 && !TaiXiu.baotri) {
            if (player.isAdmin()) {
                switch (select) {
                    // admin: 0..2 = Auto Tài/Auto Xỉu/Tam Hoa -> force + reload menu
                    case 0, 1, 2 -> {
                        forceResult(player, select);
                        openXiuMenu(npc, player); // Load lại menu
                    }
                    // admin: 3 = Cập nhập (reload)
                    case 3 -> openXiuMenu(npc, player);
                    // admin: 4 = Thêm Xỉu -> open input (menu closes)
                    case 4 -> placeXiu(player);
                    default -> {
                        // 5 = Đóng or others -> nothing
                    }
                }
            } else {
                // non-admin menu buttons: 0=Cập nhập,1=Thêm Xỉu,2=Đóng
                switch (select) {
                    case 0 -> openXiuMenu(npc, player); // Cập nhập (reload)
                    case 1 -> placeXiu(player); // Thêm Xỉu -> open input (menu closes)
                    default -> {
                        // 2 = Đóng -> nothing
                    }
                }
            }
        }
    }

    // ========== HỖ TRỢ ==========
    private void openTaiMenu(Npc npc, Player player) {
        if (player.isAdmin()) {
            // admin: 0..6
            npc.createOtherMenu(
                    player,
                    ConstMiniGame.MINIGAME_TAIXIU_TAI,
                    ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_TAI),
                    "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập",
                    "Thêm Tài", "Thêm Xỉu", "Đóng"
            );
        } else {
            // non-admin: 0..3 (Cập nhập, Thêm Tài, Thêm Xỉu, Đóng)
            npc.createOtherMenu(
                    player,
                    ConstMiniGame.MINIGAME_TAIXIU_TAI,
                    ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_TAI),
                    "Cập nhập", "Thêm Tài", "Thêm Xỉu", "Đóng"
            );
        }
    }

    private void openXiuMenu(Npc npc, Player player) {
        if (player.isAdmin()) {
            // admin: 0..5
            npc.createOtherMenu(
                    player,
                    ConstMiniGame.MINIGAME_TAIXIU_XIU,
                    ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_XIU),
                    "Auto Tài", "Auto Xỉu", "Tam Hoa", "Cập nhập",
                    "Thêm Xỉu", "Đóng"
            );
        } else {
            // non-admin: 0..2
            npc.createOtherMenu(
                    player,
                    ConstMiniGame.MINIGAME_TAIXIU_XIU,
                    ConstMiniGame.TextNpc(player, ConstMiniGame.TAI_XIU_XIU),
                    "Cập nhập", "Thêm Xỉu", "Đóng"
            );
        }
    }

    private void forceResult(Player player, int select) {
        TaiXiu.gI().chinhCau((byte) select);
        Service.gI().sendThongBao(player,
                "|7|Bạn đã can thiệp kết quả 100% ra: "
                        + (select == 0 ? "TÀI" : select == 1 ? "XỈU" : "TAM HOA"));
    }

    private void placeTai(Player player) {
        if (TaskService.gI().getIdTask(player) < ConstTask.TASK_20_0) {
            Service.gI().sendThongBao(player,
                    "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
            return;
        }
        Input.gI().taixiu_Tai(player);
    }

    private void placeXiu(Player player) {
        if (TaskService.gI().getIdTask(player) < ConstTask.TASK_20_0) {
            Service.gI().sendThongBao(player,
                    "Bạn phải làm tới nhiệm vụ TĐST mới có thể tham gia");
            return;
        }
        Input.gI().taixiu_Xiu(player);
    }
}
