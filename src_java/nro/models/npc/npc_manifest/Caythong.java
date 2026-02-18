package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import nro.models.npc.Npc;
import network.io.Message;
import nro.models.npc.NpcFactory;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.RewardService;
import nro.services.Service;
import services.func.ChangeMapService;
import utils.Logger;
import utils.Util;

public class Caythong extends Npc {

    public Caythong(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        switch (this.mapId) {
            case 0:
            case 7:
            case 14:
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Đang có 1 lượt trang trí\n"
                        + "Trang trí 2.000 lượt: x2 EXP 12h\n"
                        + "Trang trí 5.000 lượt: x3 EXP 24h\n"
                        + "Trang trí 10.000 lượt: x3 EXP 72h",
                        "Trang trí\nQuả châu",
                        "Trang trí\nNgọc Rồng 1 Sao",
                        "Vùng đất băng giá\nSự kiện Noel",
                        "Đóng");
                break;

            case 195:
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "QUY ĐỔI NGƯỜI TUYẾT\n"
                        + "Người Tuyết\n"
                        + "→ Phần thưởng: Item Siêu cập, Ngọc Rồng Băng\n"
                        + "Người Tuyết Băng Giá\n"
                        + "→ Phần thưởng: Pet, Cải Trang Bông Băng Golden,..",
                        "Giao\nNgười Tuyết",
                        "Giao\nNgười Tuyết\nBăng Giá",
                        "Quay về",
                        "Đóng");
                break;

        }
    }

    // -------------------------------------------------------------------------
    // Xử lý menu chính
    // -------------------------------------------------------------------------
    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        // ================= MAP 0,7,14 =================
        if (this.mapId == 0 || this.mapId == 7 || this.mapId == 14) {

            // MENU GỐC
            if (player.iDMark.isBaseMenu()) {
                switch (select) {

                    // ----------------------------------------------------------
                    // TRANG TRÍ QUẢ CHÂU
                    // ----------------------------------------------------------
                    case 0:
                        handleTrangTriQuaChau(player);
                        return;

                    // ----------------------------------------------------------
                    // TRANG TRÍ NGỌC RỒNG
                    // ----------------------------------------------------------
                    case 1:
                        handleTrangTriNgocRong(player);
                        return;

                    // ----------------------------------------------------------
                    // ĐI ĐẾN MAP 195
                    // ----------------------------------------------------------
                    case 2:
                        ChangeMapService.gI().changeMapNonSpaceship(player, 195, 80, 408);
                        return;
                }
            }

            // ------------------------------------------------------------------
            // MENU ĐỔI TRANG TRÍ QUẢ CHÂU
            // ------------------------------------------------------------------
            if (player.iDMark.getIndexMenu() == ConstNpc.MENU_DOI_VPSK) {
                if (select == 0) {
                    handleDoiQuaChau(player);
                }
                return;
            }

            // ------------------------------------------------------------------
            // MENU ĐỔI NGỌC RỒNG
            // ------------------------------------------------------------------
            if (player.iDMark.getIndexMenu() == ConstNpc.MENU_NR) {
                if (select == 0) {
                    handleDoiNgocRong(player);
                }
                return;
            }
        }

        // ================= MAP 195 =================
        if (this.mapId == 195 && player.iDMark.isBaseMenu()) {

            // ========== GIAO NGƯỜI TUYẾT THƯỜNG ==========
            if (select == 0) {

                Item item = InventoryService.gI().findItemBag(player, 1448);
                if (item == null || item.quantity < 1) {
                    Service.gI().sendThongBao(player, "Bạn không có Người Tuyết!");
                    return;
                }

                if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                    Service.gI().sendThongBao(player, "Cần ít nhất 1 ô hành trang trống!");
                    return;
                }

                RewardService.gI().rewardNguoiTuyet(player);
                InventoryService.gI().subQuantityItemsBag(player, item, 1);
                InventoryService.gI().sendItemBag(player);

                Service.gI().sendThongBao(player, "Bạn đã giao thành công Người Tuyết!");
                return;
            }

            // ========== GIAO NGƯỜI TUYẾT BĂNG GIÁ ==========
            if (select == 1) {

                Item item = InventoryService.gI().findItemBag(player, 1449);
                if (item == null || item.quantity < 1) {
                    Service.gI().sendThongBao(player, "Bạn không có Người Tuyết Băng Giá!");
                    return;
                }

                if (InventoryService.gI().getCountEmptyBag(player) < 1) {
                    Service.gI().sendThongBao(player, "Cần ít nhất 1 ô hành trang trống!");
                    return;
                }

                RewardService.gI().rewardNguoiTuyetBangGia(player);
                InventoryService.gI().subQuantityItemsBag(player, item, 1);
                InventoryService.gI().sendItemBag(player);

                Service.gI().sendThongBao(player, "Bạn đã giao thành công Người Tuyết Băng Giá!");
                return;
            }

            // ========== QUAY VỀ ==========
            if (select == 2) {
                ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 250);
                return;
            }

            // ========== ĐÓNG ==========
            if (select == 3) {
                return;
            }
        }
    }

    // =========================================================================
    // XỬ LÝ MENU TRANG TRÍ QUẢ CHÂU
    // =========================================================================
    private void handleTrangTriQuaChau(Player player) {

        Item chuong = InventoryService.gI().findItemBagByTemp(player, 1215);
        Item quachau = InventoryService.gI().findItemBagByTemp(player, 1216);
        Item ngoisao = InventoryService.gI().findItemBagByTemp(player, 1217);
        Item kimtuyen = InventoryService.gI().findItemBagByTemp(player, 1218);
        Item moctreo = InventoryService.gI().findItemBagByTemp(player, 1219);

        boolean enough = chuong != null && chuong.quantity >= 30
                && quachau != null && quachau.quantity >= 30
                && ngoisao != null && ngoisao.quantity >= 30
                && kimtuyen != null && kimtuyen.quantity >= 2
                && moctreo != null && moctreo.quantity >= 1;

        if (enough) {
            createOtherMenu(player, ConstNpc.MENU_DOI_VPSK,
                    "|1|Trang trí Noel\n"
                    + "Chuông " + chuong.quantity + "/30\n"
                    + "Quả châu " + quachau.quantity + "/30\n"
                    + "Ngôi sao " + ngoisao.quantity + "/30\n"
                    + "Dây kim tuyến " + kimtuyen.quantity + "/2\n"
                    + "Móc treo Noel " + moctreo.quantity + "/1\n"
                    + "Giá ngọc: 100",
                    "Đồng ý", "Từ chối");
        } else {
            String msg = "|1|Trang trí Noel\n"
                    + line("Chuông", chuong, 30)
                    + line("Quả châu", quachau, 30)
                    + line("Ngôi sao", ngoisao, 30)
                    + line("Dây kim tuyến", kimtuyen, 2)
                    + line("Móc treo Noel", moctreo, 1);

            createOtherMenu(player, ConstNpc.MENU_DOI_VPSK_2, msg, "Từ chối");
        }
    }

    // =========================================================================
    // XỬ LÝ TRANG TRÍ NGỌC RỒNG
    // =========================================================================
    private void handleTrangTriNgocRong(Player player) {
        Item nr1 = InventoryService.gI().findItemBagByTemp(player, 14);
        Item nr2 = InventoryService.gI().findItemBagByTemp(player, 15);
        Item nr3 = InventoryService.gI().findItemBagByTemp(player, 16);

        boolean enough = nr1 != null && nr1.quantity >= 30
                && nr2 != null && nr2.quantity >= 30
                && nr3 != null && nr3.quantity >= 30
                && player.inventory.ruby >= 500;   // THÊM CHECK NGỌC 500

        // Nếu đủ → mở menu xác nhận
        if (enough) {
            createOtherMenu(player, ConstNpc.MENU_NR,
                    "|2|Trang trí Noel\n"
                    + "Ngọc Rồng 1 Sao " + nr1.quantity + "/30\n"
                    + "Ngọc Rồng 2 Sao " + nr2.quantity + "/30\n"
                    + "Ngọc Rồng 3 Sao " + nr3.quantity + "/30\n"
                    + "|1|Ngọc " + player.inventory.ruby + "/500",
                    "Đồng ý", "Từ chối");
            return;
        }

        // Nếu thiếu → hiển thị theo màu
        String msg = "|2|Trang trí Noel\n"
                + line("Ngọc Rồng 1 Sao", nr1, 30)
                + line("Ngọc Rồng 2 Sao", nr2, 30)
                + line("Ngọc Rồng 3 Sao", nr3, 30)
                + rubyLine(player.inventory.ruby, 500) // DÒNG NGỌC TỰ ĐỔI MÀU
                ;

        createOtherMenu(player, ConstNpc.MENU_DOI_VPSK_2, msg, "Từ chối");
    }

    private String rubyLine(int have, int need) {
        String color = have >= need ? "|1|" : "|7|";
        return color + "Ngọc " + have + "/" + need + "\n";
    }

    // =========================================================================
    // ĐỔI QUẢ CHÂU
    // =========================================================================
    private void handleDoiQuaChau(Player player) {

        Item c = InventoryService.gI().findItemBagByTemp(player, 1459);
        Item q = InventoryService.gI().findItemBagByTemp(player, 1460);
        Item s = InventoryService.gI().findItemBagByTemp(player, 1461);
        Item k = InventoryService.gI().findItemBagByTemp(player, 1462);
        Item m = InventoryService.gI().findItemBagByTemp(player, 1463);

        doEffect(player, c, q, s, k, m);

        InventoryService.gI().subQuantityItemsBag(player, c, 30);
        InventoryService.gI().subQuantityItemsBag(player, q, 30);
        InventoryService.gI().subQuantityItemsBag(player, s, 30);
        InventoryService.gI().subQuantityItemsBag(player, k, 2);
        InventoryService.gI().subQuantityItemsBag(player, m, 1);

        player.diemnoel++;
        Service.gI().sendThongBao(player, "Bạn nhận được 1 điểm trang trí");
        InventoryService.gI().sendItemBag(player);
    }

    // =========================================================================
    // ĐỔI NGỌC RỒNG
    // =========================================================================
    private void handleDoiNgocRong(Player player) {

        Item nr1 = InventoryService.gI().findItemBagByTemp(player, 14);
        Item nr2 = InventoryService.gI().findItemBagByTemp(player, 15);
        Item nr3 = InventoryService.gI().findItemBagByTemp(player, 16);

        Item reward = ItemService.gI().createNewItem((short) 1930);

        doEffect(player, nr1, nr2, nr3);

        InventoryService.gI().addItemList(player.inventory.itemsBag, reward);
        InventoryService.gI().subQuantityItemsBag(player, nr1, 1);
        InventoryService.gI().subQuantityItemsBag(player, nr2, 1);
        InventoryService.gI().subQuantityItemsBag(player, nr3, 1);

        player.diemnoel++;
        Service.gI().sendThongBao(player, "Bạn nhận được 1 điểm trang trí");
        InventoryService.gI().sendItemBag(player);
    }

    // =========================================================================
    // GỬI HIỆU ỨNG NO–DELAY
    // =========================================================================
    private void doEffect(Player p, Item... items) {
        try {
            Message msg = new Message(-81);
            msg.writer().writeByte(0);
            msg.writer().writeUTF("test");
            msg.writer().writeUTF("test");
            msg.writer().writeShort(tempId);
            p.sendMessage(null);
            msg.cleanup();

            msg = new Message(-81);
            msg.writer().writeByte(1);
            msg.writer().writeByte(2);
            for (Item it : items) {
                msg.writer().writeByte(InventoryService.gI().getIndexBag(p, it));
            }
            p.sendMessage(null);
            msg.cleanup();

            msg = new Message(-81);
            msg.writer().writeByte(7);
            msg.writer().writeShort(-1);
            msg.writer().writeShort(-1);
            msg.writer().writeShort(-1);
            p.sendMessage(null);
            msg.cleanup();

        } catch (Exception e) {
            Logger.logException(NpcFactory.class, e, "Lỗi hiệu ứng Noel");
        }
    }

    // =========================================================================
    // DÒNG HIỂN THỊ ITEM
    // =========================================================================
    private String line(String name, Item item, int need) {
        int have = item == null ? 0 : item.quantity;
        String color = have >= need ? "|1|" : "|7|";
        return color + name + " " + have + "/" + need + "\n";
    }
}
