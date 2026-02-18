package nro.models.npc.npc_manifest;

/**
 * Box ZALO:https://zalo.me/g/ifjict764 sdt zalo: 0358176187 Chuyên chỉnh sữa
 * mua bán source nro,...
 */
import java.sql.PreparedStatement;
import consts.ConstNpc;
import item.Item;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdbc.DBConnecter;
import java.sql.Connection;
import jdbc.NDVResultSet;
import nro.models.npc.Npc;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import services.func.Input;
import shop.ShopService;
import utils.Util;

public class Santa extends Npc {

    public Santa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    private void thucHienDiemDanh(Player player) {
        try {
            String checkQuery = "SELECT COUNT(*) FROM history_items_diemdanh WHERE account_id = ? AND DATE(bought_date) = CURDATE()";
            NDVResultSet resultSet = DBConnecter.executeQuery(checkQuery, player.getSession().userId);

            if (resultSet.next() && resultSet.getInt(1) > 0) {
                Service.gI().sendThongBao(player, "Hôm nay bạn đã điểm danh rồi!");
                return;
            }
            player.getSession().diemdanh++;

            String insertQuery = "INSERT INTO history_items_diemdanh (account_id, item_temp_id, bought_date) VALUES (?, 0, NOW())";
            DBConnecter.executeUpdate(insertQuery, player.getSession().userId);

            Service.gI().sendThongBao(player, "Điểm danh thành công! Tổng điểm danh: " + player.getSession().diemdanh);

        } catch (Exception e) {
            e.printStackTrace();
            Service.gI().sendThongBao(player, "Lỗi điểm danh, vui lòng báo Admin!");
        }
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            int soLuong = 0;
            if (pGG != null) {
                soLuong = pGG.quantity;
            }
            List<String> menu = new ArrayList<>(Arrays.asList(
                    "Cửa hàng",
                    "Mở rộng\nHành trang\nRương đồ",
                    "Nhập mã\nquà tặng",
                    "Cửa hàng\nHạn sử dụng",
                    "Tiệm\nHớt tóc",
                    "Danh\nhiệu",
//                    "Điểm Danh",
                    "Cửa hàng\nHành trang"
            ));

            if (soLuong >= 1) {
                menu.add(1, "Giảm giá\n80%");
            }

            String[] menus = menu.toArray(new String[0]);

            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào, ta có một số vật phẩm đặc biệt cậu có muốn xem không?", menus);
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            int soLuong = 0;
            if (pGG != null) {
                soLuong = pGG.quantity;
            }
            if (this.mapId == 5 || this.mapId == 13 || this.mapId == 20) {
                if (player.iDMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {// Cửa hàng
                            ShopService.gI().opendShop(player, "SANTA", false);
                        }
                        case 1 -> {
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_GIAM_GIA_1", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                            }
                        }
                        case 2 -> {// Nhập mã quà tặng
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_MO_RONG_HANH_TRANG", false);
                            } else {
                                Input.gI().createFormGiftCode(player);
                            }
                        }
                        case 3 -> { // Cửa hàng hạn sử dụng
                            if (soLuong >= 1) {
                                Input.gI().createFormGiftCode(player);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_HAN_SU_DUNG", false);
                            }
                        }
                        case 4 -> { // Cửa hàng hạn sử dụng
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_HAN_SU_DUNG", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                            }
                        }
                        case 5 -> // Danh hiệu
                        {
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_HEAD", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                            }
                        }
                        case 6 -> // Điểm Danh
                        {
                            if (soLuong >= 1) {
                                ShopService.gI().opendShop(player, "SANTA_DANH_HIEU", false);
                            } else {
                                ShopService.gI().opendShop(player, "SANTA_PHUKIEN", false);
                            }
                        }
                        case 7 -> // shop phụ kiện
                            ShopService.gI().opendShop(player, "SANTA_PHUKIEN", false);
                            
//                        case 7 -> // Điểm Danh
//                        {
//                            if (soLuong >= 1) {
//                                this.createOtherMenu(player, 111,
//                                        "Santa đây!\n"
//                                        + "Chỉ cần điểm danh mỗi ngày là có quà miễn phí.\n"
//                                        + "Chúc bạn may mắn và nhận được phần quà đặc biệt!",
//                                        "Nhận quà",
//                                        "Điểm danh",
//                                        "Đóng");
//                            } else {
//                                ShopService.gI().opendShop(player, "SANTA_PHUKIEN", false);
//                            }
//                        }
//                        case 8 -> // shop phụ kiện
//                            ShopService.gI().opendShop(player, "SANTA_PHUKIEN", false);

                    }
                }
            }
            if (player.iDMark.getIndexMenu() == 111) {
                switch (select) {
                    case 0:
                        ShopService.gI().opendShop(player, "DIEM_DANH", false);
                        break;
                    case 1:
                        thucHienDiemDanh(player);
                        break;
                }
                return;
            }
        }
    }
}
