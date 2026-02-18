package nro.models.npc.npc_manifest;

import consts.ConstNpc;
import consts.ConstTask;
import consts.ConstTaskBadges;
import item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdbc.DBConnecter;
import jdbc.NDVResultSet;
import jdbc.daos.PlayerDAO;
import models.Transaction;
import nro.models.npc.Npc;
import nro.player.Archivement;
import nro.player.ArchivementSanBoss;
import nro.player.Player;
import nro.server.ChuyenKhoanManager;
import nro.server.ServerManager;
import nro.services.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.func.Input;
import shop.ShopService;
import task.Badges.BadgesTaskService;
import utils.TimeUtil;
import utils.Util;

public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    // Chi phí quy đổi
    private static final int COST_NAP_VANG = 1;
    private static final int COST_NAP_NGOC = 1;

    // Bảng nạp vàng (VNĐ -> Thỏi vàng)
    private static final int[][] NAP_VANG = {
        {20000, 20}, {50000, 55}, {100000, 125}, {500000, 1250},
        {1000000, 2500}, {2000000, 5000}, {5000000, 15000}
    };

    // Bảng nạp ngọc (VNĐ -> Hồng ngọc)
    private static final int[][] NAP_NGOC = {
        {20000, 11000}, {50000, 30000}, {100000, 70000}, {500000, 600000},
        {1000000, 1500000}, {2000000, 3500000}, {5000000, 10000000}
    };

    // ===================== MENU GỐC =====================
    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player) && !TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            createOtherMenu(player, ConstNpc.BASE_MENU,
                    "Xin chào!\nTại đây con có thể thực hiện:\n"
                    + "- Nạp tiền, vàng, ngọc\n"
                    + "- Mở thành viên / Hỗ trợ nhiệm vụ\n"
                    + "- Nhận quà miễn phí (Ngọc / Đệ tử / Điểm danh)\n"
                    + "- Đổi mật khẩu / quên mã bảo vệ\n"
                    + "- Nhập GiftCode\n",
                    "Đổi Mật Khẩu", "GiftCode", "Nạp tiền",
                    "Quà Miễn Phí", "Quên Mã Bảo Vệ?", "Khác");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        switch (player.iDMark.getIndexMenu()) {
            case ConstNpc.BASE_MENU ->
                handleBaseMenu(player, select);
            case 111 ->
                handleFreeItemMenu(player, select);
            case 112 ->
                handleNhanQuaMenu(player, select);
            case 991 ->
                handleFreeGiftMenu(player, select);
            case 992 ->
                handleOtherOptionsMenu(player, select);
            case 993 ->
                showMemberMenu(player,select);
            case ConstNpc.MAIL_BOX ->
                handleMailBox(player, select);
            case ConstNpc.NAP_TIEN ->
                handleNapTienMenu(player, select);
            case ConstNpc.MOC_QUA_TANG ->
                handleNhanQuaMenu(player, select);
            case ConstNpc.CHUYEN_KHOAN -> handleChuyenKhoan(player, select);
            case ConstNpc.CONTENT_CHUYEN_KHOAN -> handleChuyenKhoanqr(player, select);
            case ConstNpc.NAP_VANG ->
                handleNapVang(player, select);
            case ConstNpc.NAP_NGOC ->
                handleNapNgoc(player, select);
            case 12456 ->
                handleDeletePet(player, select);
            case 1115 ->
                handleMocNap(player, select);
            case 1116 ->
                handleMocBoss(player, select);
            case 782 ->
                handleMemberActivation(player, select);
        }
    }

    // ===================== MENU PHỤ =====================
    private void handleBaseMenu(Player player, int select) {
        switch (select) {
            case 0 ->
                Input.gI().createFormChangePassword(player);
            case 1 ->
                Input.gI().createFormGiftCode(player);
            case 2 ->
                showNapTienMenu(player);
            case 3 ->
                createOtherMenu(player, 991, "Chọn phần quà miễn phí:", "Nhận\nngọc Xanh", "Nhận\nđệ tử","Điểm Danh\nHàng Ngày","Nhận Quà\nThành Tích", "Đóng");
            case 4 ->
                Input.gI().createFormMBV(player);
            case 5 ->
                openOtherOptions(player);
        }
    }

    private void handleFreeGiftMenu(Player player, int select) {
        switch (select) {
            case 0 ->
                giveFreeGem(player);
            case 1 ->
                giveFreePet(player);
            case 2 ->
                this.createOtherMenu(player, 111,
                        "Xin chào hãy thực hiện!\n"
                        + "Điểm Danh để ghi nhận ngày tham gia.\n"
                        + "Khi đủ điều kiện, nhấn Nhận Quà để lấy vào hành trang.\n"
                        + "\nKhông tốn gì cả, chăm chỉ mỗi ngày nhé!",
                        "Nhận Quà",
                        "Điểm Danh",
                        "Đóng");
            case 3 ->
                showQuaTangMenu(player);
        }
    }

    private void handleFreeItemMenu(Player player, int select) {
        switch (select) {
            case 0:
                ShopService.gI().opendShop(player, "DIEM_DANH", false);
                break;
            case 1:
                thucHienDiemDanh(player);
                break;
        }
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

    private void openOtherOptions(Player player) {
        if (player.getSession() == null) {
            return;
        }

        // Nếu đã mở thành viên thì ẩn mục "Mở thành viên"
        if (player.getSession().actived) {
            createOtherMenu(player, 992, "Tùy chọn khác:",
                    "Xóa đệ", "Hỗ trợ nhiệm vụ", "Hòm thư", "Đóng");
        } else {
            createOtherMenu(player, 992, "Tùy chọn khác:",
                    "Xóa đệ", "Hỗ trợ nhiệm vụ", "Hòm thư", "Mở thành viên", "Đóng");
        }
    }

    private void handleOtherOptionsMenu(Player player, int select) {
        if (player.getSession() == null) {
            return;
        }

        if (player.getSession().actived) {
            // menu đã ẩn mục "Mở thành viên" -> select chỉ còn 0..3
            switch (select) {
                case 0 ->
                    createOtherMenu(player, 12456,
                            "|7|CẢNH BÁO XÓA ĐỆ TỬ\n"
                            + "|2|Chi phí xóa đệ là: 10,000 VNĐ (Trừ vào số dư)\n"
                            + "Con có chắc chắn muốn thực hiện không?",
                            "Đồng ý\n(10k VND)", "Suy nghĩ lại");
                case 1 ->
                    supportTask(player);
                case 2 -> {
                    int countItem = player.inventory.itemsMailBox.size() - InventoryService.gI().getCountEmptyListItem(player.inventory.itemsMailBox);
                    createOtherMenu(player, ConstNpc.MAIL_BOX,
                            "|7|HÒM THƯ CÁ NHÂN\n"
                            + "|2|Nơi nhận quà từ sự kiện, đền bù hoặc quà Admin.\n"
                            + "|1|Hiện có: " + countItem + " vật phẩm.",
                            "Xem\nHòm Thư", "Xóa Hết\nHòm Thư", "Đóng");
                }
            }
        } else {
            // menu có mục "Mở thành viên"
            switch (select) {
                case 0 ->
                    createOtherMenu(player, 12456, "|0|Bạn muốn xóa đệ với giá 10K VND?", "Đồng ý", "Không");
                case 1 ->
                    supportTask(player);
                case 2 ->
                    createOtherMenu(player, ConstNpc.MAIL_BOX,
                            "Chào con\nĐây là hòm thư nhận quà từ sự kiện, đua top hoặc Admin",
                            "Hòm Thư\n(" + (player.inventory.itemsMailBox.size()
                            - InventoryService.gI().getCountEmptyListItem(player.inventory.itemsMailBox)) + " món)",
                            "Xóa Hết\nHòm Thư", "Đóng");
                case 3 ->
                    createOtherMenu(player, 993, "Mở thành viên 10.000đ hoặc đạt 30 tỷ sức mạnh và hoàn thành xong nhiệm vụ tiêu diệt Rambo?",
                    "Mở bằng\n10K VNĐ", "Mở bằng\nSức mạnh", "Đóng");
            }
        }
    }

    // ===================== MỞ THÀNH VIÊN =====================
    private void showMemberMenu(Player player, int select) {
        if (player.getSession() == null) {
            return;
        }
        if (player.getSession().actived) {
            Service.gI().sendThongBao(player, "Bạn đã mở thành viên rồi!");
            return;
        }

        switch (select) {
            case 0 -> { // Mở bằng tiền
                if (player.getSession().cash < 10000) {
                    Service.gI().sendThongBao(player, "Bạn không đủ 10.000đ. Hãy nạp thêm nhé!");
                    return;
                }
                if (PlayerDAO.subVND2(player, 10000)) {
                    Item itemQua = ItemService.gI().createNewItem((short) 77, 20000); // 20k ngọc hồng khuyến mãi
                    InventoryService.gI().addItemBag(player, itemQua);
                    Service.gI().sendMoney(player);
                    Service.gI().sendThongBao(player, "Kích hoạt thành công! Nhận được 20.000 Hồng ngọc.");
                } else {
                    this.npcChat(player, "Có lỗi hệ thống, vui lòng báo Admin.");
                }
            }
            case 1 -> { // Mở bằng sức mạnh
                long power = player.nPoint != null ? player.nPoint.power : 0L;
                if (power < 30_000_000_000L) {
                    Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu: 30 Tỷ");
                    return;
                }
                if (TaskService.gI().getIdTask(player) < ConstTask.TASK_21_2) {
                    Service.gI().sendThongBao(player, "Yêu cầu hoàn thành nhiệm vụ: Tiêu diệt Rambo");
                    return;
                }
                if (PlayerDAO.subVND2(player, 0)) {
                    Service.gI().sendThongBao(player, "Kích hoạt thành viên thành công!");
                } else {
                    this.npcChat(player, "Lỗi hệ thống, vui lòng liên hệ Admin.");
                }
            }
        }
    }

    // ===================== NẠP TIỀN =====================
    private void showNapTienMenu(Player player) {
        String msg = "Số dư của con là: " + Util.mumberToLouis(player.getSession().cash) + " VND\n"
                + "Ta đang giữ giúp con " + Util.mumberToLouis(player.getSession().goldBar) + " thỏi vàng";
        createOtherMenu(player, ConstNpc.NAP_TIEN, msg,
                "Nạp VNĐ", "Nạp vàng", "Nạp Ngọc", "Nhận\nThỏi vàng", "Đóng");
    }
    
    // ===================== MỐC QUÀ TẶNG =====================
    private void showQuaTangMenu(Player player) {
        String msg = "Số dư của con là: " + Util.mumberToLouis(player.getSession().cash) + " VND\n"
                + "Ta đang giữ giúp con " + Util.mumberToLouis(player.getSession().goldBar) + " thỏi vàng";
        createOtherMenu(player, ConstNpc.MOC_QUA_TANG, msg,
                "Mốc\nĐã Nạp", "Mốc\nSăn Boss", "Đóng");
    }
    
    private void handleNhanQuaMenu(Player player, int select) {
        switch (select) {
            case 0 -> {
                this.createOtherMenu(player, 1115, "Nạp đạt mốc nhận quà he :3", "Xem quà mốc nạp", "Nhận quà mốc nạp", "Đóng");
            }
            case 1 -> {
                this.createOtherMenu(player, 1116, "Quà săn boss đạt mốc thì nhận quà he :3", "Xem\nquà mốc\nSăn Boss", "Nhận\nquà mốc\nSăn Boss", "Đóng");
            }
        }
    }

    private void handleNapTienMenu(Player player, int select) {
        switch (select) {
            case 0 -> createOtherMenu(player, ConstNpc.CHUYEN_KHOAN,
                "Nạp Coin ATM tự động " + ServerManager.NAME
                + "\n|1|Số dư: " + Util.mumberToLouis(player.getSession().cash) + " VNĐ\n"
                + "|8|Tỉ lệ Nạp X1 GIÁ TRỊ NẠP\n"
                + "|2|Chọn đúng mệnh giá. Sai sẽ không được cộng tiền\n"
                + "|7|Đợi 1-3 phút để hệ thống xử lý\n"
                + "|2|Nếu quá 5 phút chưa nhận liên hệ Admin",
                "Tạo giao dịch", "Xem lịch sử\ngiao dịch", "Hướng Dẫn\nBONUS");
            case 1 -> {
                List<String> menu = new ArrayList<>();
                for (int[] option : NAP_VANG) {
                    menu.add(Util.mumberToLouis(option[0]) + "\n" + Util.mumberToLouis(option[1] * COST_NAP_VANG) + " Thỏi vàng");
                }
                createOtherMenu(player, ConstNpc.NAP_VANG, "Ta sẽ giữ giúp con.\nCần thì quay lại gặp ta!", menu.toArray(new String[0]));
            }
            case 2 -> {
                List<String> menu = new ArrayList<>();
                for (int[] option : NAP_NGOC) {
                    menu.add(Util.mumberToLouis(option[0]) + "\n" + Util.mumberToLouis(option[1] * COST_NAP_NGOC) + " Hồng Ngọc");
                }
                createOtherMenu(player, ConstNpc.NAP_NGOC, "Ta sẽ giữ giúp con.\nCần thì quay lại gặp ta!", menu.toArray(new String[0]));
            }
            case 3 -> {
                receiveGoldBar(player);
            }
//            case 4 -> {
//               Input.gI().createFormNapThe2(player,null,null);
//            }
//            case 4 -> {
//                showQuaTangMenu(player);
//            }
        }
    }

    private void handleMocNap(Player player, int select) {
        switch (select) {
            case 0 -> {
                JSONArray dataArray;
                JSONObject dataObject;
                PreparedStatement ps = null;
                ResultSet rs = null;
                StringBuilder sb = new StringBuilder();
                sb.append("Phần thưởng mốc nạp (reset vào thứ 2 hàng tuần):\n");

                try (Connection con2 = DBConnecter.getConnectionServer()) {
                    ps = con2.prepareStatement("SELECT * FROM moc_nap");
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                        int mốcNạp = Archivement.GIADOLACHIADOI[rs.getInt("id") - 1];
                        String formattedMoney = NumberFormat.getInstance(new Locale("vi", "VN")).format(mốcNạp) + " VND";

                        sb.append("\n------------------------------\n");
                        sb.append("Mốc nạp ").append(formattedMoney).append(":\n");

                        for (int i = 0; i < dataArray.size(); i++) {
                            dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                            int tempId = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                            int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                            JSONArray optionsArray = (JSONArray) dataObject.get("options");

                            sb.append("- ").append(quantity).append(" x ")
                                    .append(ItemService.gI().getTemplate(tempId).name).append("\n");

                            if (optionsArray != null) {
                                for (int j = 0; j < optionsArray.size(); j++) {
                                    JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                    int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                    int param = Integer.parseInt(String.valueOf(optionObject.get("param")));

                                    String optionTemplateName = ItemService.gI().getItemOptionTemplate(optionId).name;
                                    String formattedOption = optionTemplateName.replace("#", String.valueOf(param));
                                    sb.append("  + ").append(formattedOption).append("\n");
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(QuyLaoKame.class.getName()).log(Level.SEVERE, null, ex);
                }

                Service.gI().sendThongBaoFromAdmin(player, sb.toString());
            }

            case 1 -> {
                if (player.getSession().actived) {
                    Archivement.gI().getAchievement(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần mở thành viên để nhận thưởng.");
                }
            }
        }
    }
    
    private void handleMocBoss(Player player, int select) {
        switch (select) {
            case 0 -> {
                JSONArray dataArray;
                JSONObject dataObject;
                PreparedStatement ps = null;
                ResultSet rs = null;
                StringBuilder sb = new StringBuilder();
                sb.append("Phần thưởng mốc Săn Boss (reset vào cuối tháng):\n");

                try (Connection con2 = DBConnecter.getConnectionServer()) {
                    ps = con2.prepareStatement("SELECT * FROM moc_san_boss");
                    rs = ps.executeQuery();

                    while (rs.next()) {
                        dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                        int mocsanboss = ArchivementSanBoss.DIEMSANBOSS[rs.getInt("id") - 1];
                        String formattedMoney = NumberFormat.getInstance(new Locale("vi", "VN")).format(mocsanboss) + " Điểm";

                        sb.append("\n------------------------------\n");
                        sb.append("MỐC SĂN BOSS ").append(formattedMoney).append(":\n");

                        for (int i = 0; i < dataArray.size(); i++) {
                            dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                            int tempId = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                            int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                            JSONArray optionsArray = (JSONArray) dataObject.get("options");

                            sb.append("- ").append(quantity).append(" x ")
                                    .append(ItemService.gI().getTemplate(tempId).name).append("\n");

                            if (optionsArray != null) {
                                for (int j = 0; j < optionsArray.size(); j++) {
                                    JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                    int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                    int param = Integer.parseInt(String.valueOf(optionObject.get("param")));

                                    String optionTemplateName = ItemService.gI().getItemOptionTemplate(optionId).name;
                                    String formattedOption = optionTemplateName.replace("#", String.valueOf(param));
                                    sb.append("  + ").append(formattedOption).append("\n");
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    Logger.getLogger(QuyLaoKame.class.getName()).log(Level.SEVERE, null, ex);
                }

                Service.gI().sendThongBaoFromAdmin(player, sb.toString());
            }

            case 1 -> {
                if (player.getSession().actived) {
                    ArchivementSanBoss.gI().getMocSanBoss(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần mở thành viên để nhận thưởng.");
                }
            }
        }
    }

    private void handleChuyenKhoan(Player player, int select) {
        switch (select) {
            case 0 -> {
                boolean canCreate = false;
                long timeDifference = 0;
                LocalDateTime lastTimeCreate = ChuyenKhoanManager.GetLastimeCreateTransaction(player);

                if (lastTimeCreate == null) {
                    canCreate = true;
                } else {
                    LocalDateTime now = LocalDateTime.now();
                    timeDifference = TimeUtil.calculateTimeDifferenceInSeconds(lastTimeCreate, now);

                    if (timeDifference > 10) {
                        canCreate = true;
                    }
                }

                if (player.isAdmin()) {
                    canCreate = true;
                }

                if (canCreate) {
                    Input.gI().createFormChuyenKhoan(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần đợi " + (10 - timeDifference) + " giây nữa để được tạo giao dịch mới");
                }
            }
            case 1 ->
                ChuyenKhoanManager.ShowTransaction(player);
            case 2 ->
                NpcService.gI().createTutorial(player, tempId, this.avartar, ConstNpc.HUONG_DAN_NAP);
        }
    }

    private void handleChuyenKhoanqr(Player player, int select) {
        switch (select) {
            case 0 -> {
                Transaction transaction = ChuyenKhoanManager.GetTransactionLast(player.id);
                Service.gI().LinkService(player, 10684,
                        "Ấn quét để được tự động chuyển sang trình duyệt webiste\n"
                        + "Nếu có thắc mắc gì liên hệ admin để được giải quyết\n"
                        + "Hãy đợi admin rep nhé<3", "https://img.vietqr.io/image/MB-0000331855198-compact2.png?amount=" + transaction.amount + "&addInfo=" + transaction.description, "Quét QR");
            }
            case 1 -> Service.gI().sendThongBao(player, "Chưa có giao dịch nào!");
        }
    }

    private void handleNapVang(Player player, int select) {
        if (player.getSession().cash < NAP_VANG[select][0]) {
            Service.gI().sendThongBao(player, "Không đủ số dư");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }

        Item item = ItemService.gI().createNewItem((short) 457, NAP_VANG[select][1] * COST_NAP_VANG);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        PlayerDAO.subcash(player, NAP_VANG[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, NAP_VANG[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.EM_XINH_EM_DEP, NAP_VANG[select][0]);
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.mumberToLouis(NAP_VANG[select][1]) + " Thỏi vàng");
    }

    private void handleNapNgoc(Player player, int select) {
        if (player.getSession().cash < NAP_NGOC[select][0]) {
            Service.gI().sendThongBao(player, "Không đủ số dư");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }

        Item item = ItemService.gI().createNewItem((short) 861, NAP_NGOC[select][1] * COST_NAP_NGOC);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        PlayerDAO.subcash(player, NAP_NGOC[select][0]);
        BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.BI_MOC_SACH_TUI, NAP_NGOC[select][0]);
        Service.gI().sendThongBao(player, "Bạn nhận được " + Util.mumberToLouis(NAP_NGOC[select][1]) + " Hồng Ngọc");
    }

    private void receiveGoldBar(Player player) {
        if (player.getSession().goldBar <= 0) {
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Cần ít nhất 1 ô trống trong hành trang");
            return;
        }

        Item item = ItemService.gI().createNewItem((short) 457, player.getSession().goldBar);
        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBag(player);

        Service.gI().sendThongBao(player, "Bạn đã nhận " + player.getSession().goldBar + " Thỏi vàng");
        PlayerDAO.subGoldBar(player, player.getSession().goldBar);
    }

    // ===================== QUÀ MIỄN PHÍ =====================
    private void giveFreeGem(Player player) {
        if (player.inventory.gem >= 5000) {
            Service.gI().sendThongBao(player, "Tiêu bớt ngọc đi rồi nhận tiếp");
            return;
        }
        player.inventory.gem += 5000;
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Bạn nhận được 5.000 Ngọc xanh");
    }

    private void giveFreePet(Player player) {
        if (!player.getSession().actived) {
            Service.gI().sendThongBao(player, "Vui lòng mở thành viên trước");
            return;
        }
        if (player.pet != null) {
            npcChat(player, "Bú ít thôi con, còn đệ rồi!");
            return;
        }
        PetService.gI().createNormalPet(player);
        Service.gI().sendThongBao(player, "Bạn nhận được đệ tử");
    }

    // ===================== HỖ TRỢ NHIỆM VỤ =====================
    private void supportTask(Player player) {
        int id = TaskService.gI().getIdTask(player);
        if ((id >= ConstTask.TASK_9_0 && id < ConstTask.TASK_11_0)
                || (id >= ConstTask.TASK_18_0 && id < ConstTask.TASK_20_0)) {

            player.playerTask.taskMain.id = (id >= ConstTask.TASK_18_0) ? 19 : 10;
            player.playerTask.taskMain.index = 0;
            TaskService.gI().sendNextTaskMain(player);

            Service.gI().sendThongBao(player, "Bạn đã được hỗ trợ nhiệm vụ thành công");
        } else {
            Service.gI().sendThongBao(player, "Chỉ hỗ trợ nhiệm vụ tàu Pảy Pảy và nhiệm vụ DHVT, Trung úy trắng");
        }
    }

    // ===================== HÒM THƯ =====================
    private void handleMailBox(Player player, int select) {
        switch (select) {
            case 0 ->
                ShopService.gI().opendShop(player, "ITEMS_MAIL_BOX", true);
            case 1 ->
                NpcService.gI().createMenuConMeo(player,
                        ConstNpc.CONFIRM_REMOVE_ALL_ITEM_MAIL_BOX, this.avartar,
                        "|3|Bạn có chắc muốn xóa hết vật phẩm trong hòm thư?\n|7|Sau khi xóa sẽ không thể khôi phục!",
                        "Đồng ý", "Hủy bỏ");
        }
    }

    private void handleMemberActivation(Player player, int select) {
        if (select != 0 || player.getSession() == null) {
            return;
        }

        if (player.getSession().actived) {
            Service.gI().sendThongBao(player, "Bạn đã mở thành viên rồi");
        } else if (player.getSession().cash < 20000) {
            NpcService.gI().createBigMessage(player, avartar,
                    "Bạn chưa nạp đủ 20K.\nBạn có muốn nạp để mở thành viên không?",
                    (byte) 1, "NẠP", "https://dragonball.online/nap-atm.php");
        } else if (PlayerDAO.updateActive(player, 1)) {
            Service.gI().sendThongBao(player, "Mở thành viên thành công!");
        } else {
            Service.gI().sendThongBao(player,
                    "Có lỗi khi kích hoạt.\n"
                    + "Nếu đã bị trừ tiền mà chưa được kích hoạt, vui lòng liên hệ Admin.\n"
                    + "Hãy chụp lại thông báo này.");
        }
    }

    // ===================== XÓA ĐỆ =====================
    private void handleDeletePet(Player player, int select) {
        if (select == 0) {
            if (player.getSession().cash < 10000) {
                Service.gI().sendThongBao(player, "Bạn không đủ 10.000 VND.");
                return;
            }
            if (player.pet != null) {
                PetService.gI().deletePet(player);
                PlayerDAO.subcash(player, 10000); // Thêm trừ tiền nếu logic game yêu cầu
                Service.gI().sendThongBao(player, "Đã xóa đệ tử!");
            } else {
                Service.gI().sendThongBao(player, "Bạn đâu có đệ tử?");
            }
        }
    }
}
