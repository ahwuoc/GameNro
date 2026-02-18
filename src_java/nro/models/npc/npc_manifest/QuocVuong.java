package nro.models.npc.npc_manifest;

/**
 *
 * Box ZALO: https://zalo.me/g/ifjict764 SĐT ZALO: 0358176187 Chuyên chỉnh sửa
 * mua bán source NRO,...
 */
import consts.ConstNpc;
import consts.ConstTranhNgocNamek;
import item.Item;
import models.DragonNamecWar.TranhNgoc;
import nro.models.npc.Npc;
import nro.player.NPoint;
import nro.player.Player;
import nro.services.InventoryService;
import nro.services.OpenPowerService;
import nro.services.Service;
import services.func.TopService;
import shop.ShopService;
import utils.Util;

public class QuocVuong extends Npc {

    public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        switch (mapId) {
            case 13 -> {
                Item mcl = InventoryService.gI().findItemBagByTemp(player, ConstTranhNgocNamek.ITEM_TRANH_NGOC);
                int slMCL = (mcl == null) ? 0 : mcl.quantity;
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|0|Ngọc rồng Namếc đang bị 2 thế lực tranh giành\n|4|"
                        + "Hãy chọn cấp độ tham gia tùy theo sức mạnh bản thân",
                        "Tham gia",
                        "Đổi điểm\nThưởng\n[" + slMCL + "]",
                        "Bảng\nxếp hạng",
                        "Từ chối");
            }
            default ->
                createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                        "Bản thân", "Đệ tử", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        if (player.iDMark.isBaseMenu()) {
            switch (this.mapId) {
                case 13 ->
                    handleMenuTranhNgoc(player, select);
                default ->
                    handleMenuOpenPower(player, select);
            }
        }

        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.REGISTER_TRANH_NGOC ->
                handleRegisterTranhNgoc(player, select);
            case ConstNpc.LOG_OUT_TRANH_NGOC ->
                handleLogoutTranhNgoc(player, select);
            case ConstNpc.OPEN_POWER_MYSEFT ->
                handleOpenPowerMyself(player, select);
            case ConstNpc.OPEN_POWER_PET ->
                handleOpenPowerPet(player, select);
        }
    }

    // ================== HANDLER METHODS ==================
    private void handleMenuTranhNgoc(Player player, int select) {
        switch (select) {
            case 0 -> {
                if (TranhNgoc.gI().isTimeRegisterWar()) {
                    if (player.iDMark.getTranhNgoc() == -1) {
                        createOtherMenu(player, ConstNpc.REGISTER_TRANH_NGOC,
                                getInfoTranhNgoc(),
                                "Tham gia phe Xanh", "Tham gia phe Đỏ", "Đóng");
                    } else {
                        createOtherMenu(player, ConstNpc.LOG_OUT_TRANH_NGOC,
                                getInfoTranhNgoc(),
                                "Hủy\nĐăng Ký", "Đóng");
                    }
                } else {
                    Service.gI().sendPopUpMultiLine(player, 0, 4335,
                            "Sự kiện sẽ mở đăng ký vào lúc " + ConstTranhNgocNamek.HOUR_REGISTER + ":" + ConstTranhNgocNamek.MIN_REGISTER
                            + "\nSự kiện sẽ bắt đầu vào " + ConstTranhNgocNamek.HOUR_OPEN + ":" + ConstTranhNgocNamek.MIN_OPEN
                            + " và kết thúc vào " + ConstTranhNgocNamek.HOUR_CLOSE + ":" + ConstTranhNgocNamek.MIN_CLOSE);
                }
            }
            case 1 ->
                ShopService.gI().opendShop(player, "TRUONG_LAO", false);
            case 2 ->
                Service.gI().sendThongBaoOK(player, TopService.getTopQuocVuong());
        }
    }

    private void handleMenuOpenPower(Player player, int select) {
        switch (select) {
            case 0 -> { // Bản thân
                if (player.nPoint.limitPower < NPoint.MAX_LIMIT) {
                    createOtherMenu(player, ConstNpc.OPEN_POWER_MYSEFT,
                            "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của bản thân lên "
                            + Util.numberToMoney(player.nPoint.getPowerNextLimit()),
                            "Nâng\ngiới hạn\nsức mạnh",
                            "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " vàng",
                            "Đóng");
                } else {
                    createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Sức mạnh của con đã đạt tới giới hạn", "Đóng");
                }
            }
            case 1 -> { // Đệ tử
                if (player.pet != null) {
                    if (player.pet.nPoint.limitPower < NPoint.MAX_LIMIT) {
                        createOtherMenu(player, ConstNpc.OPEN_POWER_PET,
                                "Ta sẽ truyền năng lượng giúp con mở giới hạn sức mạnh của đệ tử lên "
                                + Util.numberToMoney(player.pet.nPoint.getPowerNextLimit()),
                                "Nâng ngay\n" + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) + " vàng",
                                "Đóng");
                    } else {
                        createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                "Sức mạnh của đệ con đã đạt tới giới hạn", "Đóng");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
            }
        }
    }

    private void handleRegisterTranhNgoc(Player player, int select) {
        if (!player.getSession().actived) {
            Service.gI().sendThongBao(player, "Vui lòng kích hoạt tài khoản để sử dụng chức năng này!");
            return;
        }

        switch (select) {
            case 0 -> {
                player.iDMark.setTranhNgoc((byte) 1);
                TranhNgoc.gI().addPlayersBlue(player);
                Service.gI().sendThongBao(player, "Đăng ký vào phe Xanh thành công");
            }
            case 1 -> {
                player.iDMark.setTranhNgoc((byte) 2);
                TranhNgoc.gI().addPlayersRed(player);
                Service.gI().sendThongBao(player, "Đăng ký vào phe Đỏ thành công");
            }
        }
    }

    private void handleLogoutTranhNgoc(Player player, int select) {
        if (select == 0) {
            player.iDMark.setTranhNgoc((byte) -1);
            TranhNgoc.gI().removePlayersBlue(player);
            TranhNgoc.gI().removePlayersRed(player);
            Service.gI().sendThongBao(player, "Hủy đăng ký thành công");
        }
    }

    private void handleOpenPowerMyself(Player player, int select) {
        switch (select) {
            case 0 ->
                OpenPowerService.gI().openPowerBasic(player);
            case 1 -> {
                if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                    if (OpenPowerService.gI().openPowerSpeed(player)) {
                        player.inventory.gold -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                        Service.gI().sendMoney(player);
                    }
                } else {
                    Service.gI().sendThongBao(player,
                            "Bạn không đủ vàng để mở, còn thiếu "
                            + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gold) + " vàng");
                }
            }
        }
    }

    private void handleOpenPowerPet(Player player, int select) {
        if (select == 0) {
            if (player.inventory.gold >= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER) {
                if (OpenPowerService.gI().openPowerSpeed(player.pet)) {
                    player.inventory.gold -= OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
                    Service.gI().sendMoney(player);
                }
            } else {
                Service.gI().sendThongBao(player,
                        "Bạn không đủ vàng để mở, còn thiếu "
                        + Util.numberToMoney(OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER - player.inventory.gold) + " vàng");
            }
        }
    }

    // ================== UTIL METHODS ==================
    private String getInfoTranhNgoc() {
        return "|0|Ngọc rồng Namếc đang bị 2 thế lực tranh giành\n"
                + "Hãy chọn cấp độ tham gia tùy theo sức mạnh bản thân\n|2|"
                + "Phe Xanh: " + TranhNgoc.gI().getPlayersBlue().size() + "\n|7|"
                + "Phe Đỏ: " + TranhNgoc.gI().getPlayersRed().size() + "\n|4|"
                + "Chú ý: Đăng kí xong phải online cho tới lúc tranh đấu, out game thì phải đăng kí lại!";
    }
}
