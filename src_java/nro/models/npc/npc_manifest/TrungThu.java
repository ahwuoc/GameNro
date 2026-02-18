package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import event.EventManager;
import item.Item;
import item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import shop.ShopService;
import utils.Util;

public class TrungThu extends Npc {

    public TrungThu(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (!canOpenNpc(player)) {
            return;
        }

        player.iDMark.setIndexMenu(ConstNpc.BASE_MENU);
        if (EventManager.TRUNG_THU) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Chúc các bạn chơi game vui vẻ!",
                    "Cửa\nhàng", "Đổi\nThỏ cưng", "Đổi 99\nCarot\nlấy quà", "Đổi 99\nĐuôi khỉ\nlấy quà");
        } else {
            Service.gI().sendThongBao(player, "Sự kiện Trung Thu hiện chưa diễn ra.");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }
        if (this.mapId != 0 && this.mapId != 5 && this.mapId != 7 && this.mapId != 14) {
            return;
        }

        // Menu chính
        if (player.iDMark.isBaseMenu()) {
            if (EventManager.TRUNG_THU) {
                switch (select) {
                    case 0 ->
                        ShopService.gI().opendShop(player, "CUA_HANG_TT", false);
                    case 1 ->
                        createOtherMenu(player, 1,
                                "• Đổi 20 Bánh Trung Thu Gà Quay → Thỏ Xám\n"
                                + "• Đổi 10 Bánh Trung Thu Thập Cẩm → Thỏ Trắng\n"
                                + "• Đổi 10 Bánh Trung Thu Hạt Sen → Mèo Đuôi Vàng\n\n"
                                + "Hạn sử dụng: ngẫu nhiên.",
                                "Đổi\nThỏ Xám", "Đổi\nThỏ Trắng", "Đổi\nMèo Đuôi Vàng", "Đóng");
                    case 2 ->
                        ShopService.gI().opendShop(player, "CUA_HANG_CAROT", false);
                    case 3 ->
                        ShopService.gI().opendShop(player, "CUA_HANG_DUOI_KHI", false);
                }
            }
            return;
        }

        // Menu chọn đổi quà
        if (player.iDMark.getIndexMenu() == 1) {
            switch (select) {
                case 0 ->
                    createConfirmMenu(player, 890, 892, 20, "Gà Quay", "Thỏ Xám");
                case 1 ->
                     createConfirmMenu(player, 891, 893, 10, "Thập Cẩm", "Thỏ Trắng");
                case 2 ->
                    createConfirmMenu(player, 1313, 1188, 10, "Hạt Sen", "Mèo Đuôi Vàng");
            }
            return;
        }

        // Xác nhận đồng ý đổi
        if (player.iDMark.getIndexMenu() == 2) {
            switch (select) {
                case 0 -> { // Đồng ý
                    handleExchange(player);
                }
                case 1 ->
                    Service.gI().sendThongBao(player, "Bạn đã hủy đổi quà.");
            }
        }
    }

    // ------------------ HÀM HỖ TRỢ --------------------
    private void createConfirmMenu(Player player, int requiredItemId, int rewardItemId,
            int requiredQuantity, String requiredName, String rewardName) {
        player.iDMark.setIndexMenu(2);
        player.iDMark.setMenuData(new int[]{requiredItemId, rewardItemId, requiredQuantity});
        String msg = "Bạn có chắc muốn đổi " + requiredQuantity + " Bánh Trung Thu " + requiredName
                + " để nhận \"" + rewardName + "\" không?";
        createOtherMenu(player, 2, msg, "Đồng ý", "Từ chối");
    }

    private void handleExchange(Player player) {
        int[] data = player.iDMark.getMenuData();
        if (data == null || data.length < 3) {
            return;
        }

        int requiredItemId = data[0];
        int rewardItemId = data[1];
        int requiredQuantity = data[2];

        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBao(player, "Hành trang của bạn đã đầy.");
            return;
        }

        Item requiredItem = InventoryService.gI().findItemBag(player, requiredItemId);
        if (requiredItem == null || requiredItem.quantity < requiredQuantity) {
            Service.gI().sendThongBao(player, "Không đủ vật phẩm để đổi.");
            return;
        }

        // Trừ vật phẩm
        InventoryService.gI().subQuantityItemsBag(player, requiredItem, requiredQuantity);

        // Tạo vật phẩm mới
        Item reward = ItemService.gI().createNewItem((short) rewardItemId);
        reward.itemOptions.add(new ItemOption((short) 50, Util.nextInt(7, 15)));
        reward.itemOptions.add(new ItemOption((short) 77, Util.nextInt(7, 10)));
        reward.itemOptions.add(new ItemOption((short) 103, Util.nextInt(7, 10)));
        reward.itemOptions.add(new ItemOption((short) 14, Util.nextInt(3, 10)));
        reward.itemOptions.add(new ItemOption((short) 5, Util.nextInt(7, 20)));
        reward.itemOptions.add(new ItemOption((short) 30, 0));

        // Thêm vào túi
        InventoryService.gI().addItemBag(player, reward);
        InventoryService.gI().sendItemBag(player);

        Service.gI().sendThongBao(player, "Bạn đã nhận được " + reward.template.name + "!");
    }
}
