package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import item.Item;
import item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import utils.Util;

public class QuayNuocMia extends Npc {

    private static int eventCount = 0;

    public QuayNuocMia(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player pl) {
        if (!canOpenNpc(pl)) {
            return;
        }

        createOtherMenu(pl, ConstNpc.BASE_MENU,
                "QUẦY NƯỚC MÍA NGỌC RỒNG\n"
                + "Sự kiện hè đang diễn ra sôi động.\n\n"
                + "Chiến binh " + pl.name + ", hãy cùng tham gia!\n\n"
                + "- Giải nhiệt: Dùng nguyên liệu để nhận quà.\n"
                + "- Trả hàng: Góp ly nước mía, tích điểm sự kiện và buff toàn server.",
                "Giải nhiệt", "Trả hàng");
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            switch (select) {
                case 0 ->
                    giaiNhiet(player);
                case 1 ->
                    openMenuTraHang(player);
            }
        } else if (player.iDMark.getIndexMenu() == ConstNpc.MENU_OPEN_SUKIEN_BINH) {
            traHangNuocMia(player, select);
        }
    }

    /**
     * Giải nhiệt - đổi nguyên liệu lấy phần thưởng
     */
    private void giaiNhiet(Player player) {
        Item nuocDa = InventoryService.gI().findItem(player.inventory.itemsBag, 1613); // Nước đá
        Item khucMia = InventoryService.gI().findItem(player.inventory.itemsBag, 1612); // Khúc mía

        if (nuocDa == null || khucMia == null
                || nuocDa.quantity < 30
                || khucMia.quantity < 30
                || player.inventory.gold < 100_000_000) {
            Service.gI().sendThongBao(player,
                    "Cần 30 Nước đá + 30 Khúc mía + 100tr vàng để giải nhiệt.");
            return;
        }

        Item reward;
        if (Util.isTrue(15, 100)) {
            reward = ItemService.gI().createNewItem((short) 1616); // phần thưởng hiếm
        } else if (Util.isTrue(30, 100)) {
            reward = ItemService.gI().createNewItem((short) 1615); // phần thưởng khá
        } else {
            reward = ItemService.gI().createNewItem((short) 1614); // phần thưởng cơ bản
        }
        reward.itemOptions.add(new ItemOption(30, 0));

        // Trừ nguyên liệu
        InventoryService.gI().subQuantityItemsBag(player, nuocDa, 30);
        InventoryService.gI().subQuantityItemsBag(player, khucMia, 30);
        player.inventory.gold -= 100_000_000;

        // Nhận quà
        InventoryService.gI().addItemBag(player, reward);
        InventoryService.gI().sendItemBag(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận: " + reward.template.name);
    }

    /**
     * Mở menu trả hàng
     */
    private void openMenuTraHang(Player player) {
        String text = "TRẢ HÀNG - GÓP LY NƯỚC MÍA\n\n"
                + "- Điểm sự kiện của bạn: " + player.event.getEventPoint() + "\n"
                + "- Tổng số ly toàn server: " + (eventCount % 999) + "/999\n\n"
                + (eventCount % 999 == 0 && eventCount > 0
                        ? "Toàn server đang được buff 20% sức đánh trong 60 phút!"
                        : "Khi đủ 999 ly, toàn server sẽ được buff 20% sức đánh trong 60 phút.");

        createOtherMenu(player, ConstNpc.MENU_OPEN_SUKIEN_BINH, text,
                "Trả 1 ly", "Trả 10 ly", "Trả 99 ly");
    }

    /**
     * Xử lý trả hàng
     */
    private void traHangNuocMia(Player player, int select) {
        int soLuong = switch (select) {
            case 0 ->
                1;
            case 1 ->
                10;
            case 2 ->
                99;
            default ->
                0;
        };

        if (soLuong <= 0) {
            Service.gI().sendThongBao(player, "Số lượng không hợp lệ.");
            return;
        }

        Item lyNuocMia = InventoryService.gI().finditemLyNuocMia(player, soLuong);
        if (lyNuocMia == null || lyNuocMia.quantity < soLuong) {
            Service.gI().sendThongBao(player, "Bạn cần có " + soLuong + " ly nước mía.");
            return;
        }

        // Trừ ly nước mía
        InventoryService.gI().subQuantityItemsBag(player, lyNuocMia, soLuong);
        InventoryService.gI().sendItemBag(player);

        // Cộng điểm sự kiện
        player.event.setEventPoint(player.event.getEventPoint() + soLuong);
        eventCount += soLuong;

        Service.gI().sendThongBao(player,
                "Bạn đã trả " + soLuong + " ly nước mía.\nĐiểm sự kiện hiện tại: "
                + player.event.getEventPoint());

        // Kiểm tra buff toàn server
        if (eventCount % 999 == 0) {
            Service.gI().sendThongBaoAllPlayer(
                    "Toàn server được buff 20% sức đánh trong 60 phút!");
            // TODO: Thêm logic áp dụng buff toàn server
        }
    }
}
