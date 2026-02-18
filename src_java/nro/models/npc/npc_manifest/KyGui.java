package nro.models.npc.npc_manifest;

/**
 *
 * @author
 */
import models.kygui.ConsignShopService;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.NpcService;

public class KyGui extends Npc {

    public KyGui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, 0,
                    "Xin chào!\nĐây là cửa hàng Ký Gửi, nơi bạn có thể mua bán những vật phẩm giá trị.\nBạn muốn làm gì?",
                    "Hướng dẫn", "Mua bán ký gửi", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player pl, int select) {
        if (canOpenNpc(pl)) {
            switch (select) {
                case 0 -> NpcService.gI().createTutorial(pl, tempId, avartar,
                        "Cửa hàng Ký Gửi hoạt động như sau:\b"
                        + "- Phí đăng ký bán: 5 ngọc.\b"
                        + "- Giá trị ký gửi: 10.000 - 200.000.000 vàng hoặc 2 - 2.000 ngọc.\b"
                        + "- Bạn đăng bán, tất cả người chơi đều có thể mua.\b"
                        + "Đơn giản - nhanh chóng - tiện lợi!");
                case 1 -> {
                    if (pl.getSession().actived) {
                        ConsignShopService.gI().openShopKyGui(pl);
                    } else {
                        this.npcChat(pl, "Bạn cần kích hoạt thành viên để sử dụng chức năng này.");
                    }
                }
            }
        }
    }
}
