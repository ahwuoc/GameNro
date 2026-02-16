package npc.npc_manifest;

import consts.ConstItem;
import consts.ConstNpc;
import item.Item;
import npc.Npc;
import player.Player;
import services.InventoryService;
import services.NpcService;
import services.Service;
import services.func.TopService;
import npc.specialnpc.NoiBanhChung;

/**
 * NPC Nấu Bánh Chưng Event
 * 
 * @author Antigravity
 */
public class NauBanh extends Npc {

    public NauBanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        System.out.println("Open base menu");
        if (canOpenNpc(player)) {
            if (player.noibanhchung == null) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Ngươi muốn nấu bánh chưng nào?",
                        "Hướng dẫn", "Nấu Bánh", "Xếp Top");
            } else {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Cái nồi đang sôi sùng sục, con muốn làm gì nào?",
                        "Hướng dẫn", "Nhận Bánh", "Xếp Top");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            int menuId = player.iDMark.getIndexMenu();
            if (player.iDMark.isBaseMenu()) {
                switch (select) {
                    case 0: // Hướng dẫn
                        NpcService.gI().createTutorial(player, this.tempId, this.avartar,
                                "Để nấu Bánh, con cần chuẩn bị các nguyên liệu sau:\n"
                                        + "- Bánh Chưng: 20 cái mỗi loại (Thịt heo, Nếp, Đậu xanh, Lá dong). Nhận 2 điểm Event.\n"
                                        + "- Bánh Tét: 10 cái mỗi loại (Thịt heo, Nếp, Đậu xanh). Không cần Lá dong và không có điểm Event.\n\n"
                                        + "Sau khi có đủ nguyên liệu, chọn 'Nấu Bánh'. Nấu mất 30 phút.\n"
                                        + "Bánh có thể dùng để đổi lấy quà hấp dẫn và Linh thú!");
                        break;
                    case 1: // Nấu Bánh hoặc Nhận Bánh
                        if (player.noibanhchung == null) {
                            this.createOtherMenu(player, 500, "Con muốn nấu loại bánh nào?\n"
                                    + "Bánh Chưng: 20 nguyên liệu mỗi loại (4 loại, Nhận 2 điểm Event)\n"
                                    + "Bánh Tét: 10 nguyên liệu mỗi loại (3 loại, Không nhận điểm Event)",
                                    "Bánh Chưng", "Bánh Tét", "Từ chối");
                        } else {
                            player.noibanhchung.finishCooking();
                        }
                        break;
                    case 2: // Xếp Top
                        TopService.showListTop(player, 9);
                        break;
                }
            } else if (menuId == 500) { // Menu Chọn Loại Bánh
                switch (select) {
                    case 0: // Chọn Bánh Chưng
                        this.createOtherMenu(player, 502, "Con muốn nấu bao nhiêu Bánh Chưng?",
                                "Nấu x1", "Nấu x10", "Nấu x100", "Từ chối");
                        break;
                    case 1: // Chọn Bánh Tét
                        this.createOtherMenu(player, 503, "Con muốn nấu bao nhiêu Bánh Tét?",
                                "Nấu x1", "Nấu x10", "Nấu x100", "Từ chối");
                        break;
                }
            } else if (menuId == 502) { // Menu Số Lượng Bánh Chưng
                switch (select) {
                    case 0: // x1
                        nauBanh(player, ConstItem.BANH_CHUNG, 1);
                        break;
                    case 1: // x10
                        nauBanh(player, ConstItem.BANH_CHUNG, 10);
                        break;
                    case 2: // x100
                        nauBanh(player, ConstItem.BANH_CHUNG, 100);
                        break;
                }
            } else if (menuId == 503) { // Menu Số Lượng Bánh Tét
                switch (select) {
                    case 0: // x1
                        nauBanh(player, ConstItem.BANH_TET, 1);
                        break;
                    case 1: // x10
                        nauBanh(player, ConstItem.BANH_TET, 10);
                        break;
                    case 2: // x100
                        nauBanh(player, ConstItem.BANH_TET, 100);
                        break;
                }
            }
        }
    }

    private void nauBanh(Player player, int type, int totalQuantity) {
        if (player.noibanhchung != null) {
            Service.gI().sendThongBao(player, "Con đang nấu một nồi bánh rồi, không thể nấu thêm!");
            return;
        }

        Item thitHeo = InventoryService.gI().findItemBag(player, ConstItem.THIT_HEO);
        Item thungNep = InventoryService.gI().findItemBag(player, ConstItem.THUNG_NEP);
        Item thungDauXanh = InventoryService.gI().findItemBag(player, ConstItem.THUNG_DAU_XANH);
        Item laDong = InventoryService.gI().findItemBag(player, ConstItem.LA_DONG);

        int quantityPerBanh = (type == ConstItem.BANH_CHUNG) ? 20 : 10;
        int totalRequired = quantityPerBanh * totalQuantity;

        if (thitHeo == null || thitHeo.quantity < totalRequired) {
            Service.gI().sendThongBao(player,
                    "Con còn thiếu " + (totalRequired - (thitHeo == null ? 0 : thitHeo.quantity)) + " Thịt heo");
            return;
        }
        if (thungNep == null || thungNep.quantity < totalRequired) {
            Service.gI().sendThongBao(player,
                    "Con còn thiếu " + (totalRequired - (thungNep == null ? 0 : thungNep.quantity)) + " Thùng nếp");
            return;
        }
        if (thungDauXanh == null || thungDauXanh.quantity < totalRequired) {
            Service.gI().sendThongBao(player,
                    "Con còn thiếu " + (totalRequired - (thungDauXanh == null ? 0 : thungDauXanh.quantity))
                            + " Thùng đậu xanh");
            return;
        }

        if (type == ConstItem.BANH_CHUNG) {
            if (laDong == null || laDong.quantity < totalRequired) {
                Service.gI().sendThongBao(player,
                        "Con còn thiếu " + (totalRequired - (laDong == null ? 0 : laDong.quantity)) + " Lá dong");
                return;
            }
            InventoryService.gI().subQuantityItemsBag(player, laDong, totalRequired);
        }

        InventoryService.gI().subQuantityItemsBag(player, thitHeo, totalRequired);
        InventoryService.gI().subQuantityItemsBag(player, thungNep, totalRequired);
        InventoryService.gI().subQuantityItemsBag(player, thungDauXanh, totalRequired);

        // Khởi tạo nồi nấu bánh
        player.noibanhchung = new NoiBanhChung(player, System.currentTimeMillis(), NoiBanhChung.TIME_COOKING, type,
                totalQuantity);
        player.noibanhchung.sendNoiBanhChung();

        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player,
                "Con đã bắt đầu nấu " + totalQuantity + " cái bánh, hãy quay lại sau khi bánh chín nhé!");
    }

}
