package nro.models.npc.npc_manifest;

import consts.ConstDataEventNAP;
import consts.ConstDataEventSM;
import consts.ConstDataEventTOP;
import consts.ConstMenu;
import consts.ConstNpc;
import jdbc.DBConnecter;
import nro.models.npc.Npc;
import nro.player.ArchivementSucManh;
import nro.player.Player;
import nro.server.Manager;
import nro.services.ItemService;
import nro.services.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.func.TopService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DaiThienSu extends Npc {

    public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    @Override
    public void openBaseMenu(Player player) {
        // Lấy thời gian từ ConstDataEventTOP (đã load từ config)
        String start = String.format("%02dh%02d' ngày %02d/%02d/%d",
                ConstDataEventTOP.HOUR_OPEN,
                ConstDataEventTOP.MIN_OPEN,
                ConstDataEventTOP.DATE_OPEN,
                ConstDataEventTOP.MONTH_OPEN,
                ConstDataEventSM.YEAR_EVENT);

        String end = String.format("%02dh%02d' ngày %02d/%02d/%d",
                ConstDataEventTOP.HOUR_END,
                ConstDataEventTOP.MIN_END,
                ConstDataEventTOP.DATE_END,
                ConstDataEventTOP.MONTH_END,
                ConstDataEventSM.YEAR_EVENT);

        String reward = String.format("%02dh%02d' ngày %02d/%02d/%d",
                ConstDataEventTOP.HOUR_REWARD,
                ConstDataEventTOP.MIN_REWARD,
                ConstDataEventTOP.DATE_REWARD,
                ConstDataEventTOP.MONTH_REWARD,
                ConstDataEventSM.YEAR_EVENT);

        // Thông tin hiển thị
        String info = """
        Sự kiện đua TOP chào mừng Vũ Trụ 15
        Thời gian: %s - %s
        Nhận thưởng vào: %s
        Giải thưởng giá trị, xem chi tiết tại diễn đàn hoặc fanpage
        Kết thúc %s
        """.formatted(
                start,
                end,
                reward,
                ConstDataEventTOP.demTimeSuKien()
        );

        // Hiển thị menu
        createOtherMenu(
                player,
                ConstMenu.MENU_SHOW,
                info,
                "Top\nSức mạnh",
                "Top\nĐại gia",
                "Top\nNhiệm vụ",
                "Từ chối"
        );
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!canOpenNpc(player)) {
            return;
        }

        switch (player.iDMark.getIndexMenu()) {
            case ConstMenu.MENU_SHOW -> {
                switch (select) {
                    case 0 ->
                        showTopSMMenu(player);
                    case 1 ->
                        showTopNapMenu(player);
                    case 2 ->
                        TopService.showListTop(player, 0);
                }
            }
            case 1115 ->
                handleTopSMOptions(player, select);
            case 1116 ->
                handleTopNapOptions(player, select);
        }
    }

    // Menu Top Sức Mạnh
    private void showTopSMMenu(Player player) {
        if (!ConstDataEventSM.isRunningSK) {
            Service.gI().sendThongBao(player, "Sự kiện Top Sức Mạnh đã kết thúc.");
            return;
        }
        createOtherMenu(player, 1115, "Bảng Xếp Hạng Sức Mạnh",
                "Xem Top", "Phần Thưởng Top", "Phần Thưởng Mốc", "Nhận Thưởng", "Đóng");
    }

    // Menu Top Nạp
    private void showTopNapMenu(Player player) {
        if (!ConstDataEventNAP.isRunningSK) {
            Service.gI().sendThongBao(player, "Sự kiện Top Nạp Tiền đã kết thúc.");
            return;
        }
        createOtherMenu(player, 1116, "Bảng Xếp Hạng Nạp Tiền",
                "Xem Top", "Phần Thưởng", "Đóng");
    }

    // Xử lý Top Sức Mạnh
    private void handleTopSMOptions(Player player, int select) {
        switch (select) {
            case 0 ->
                TopService.gI().showListTopPower(player);
            case 1 ->
                showRewardList(player, "moc_suc_manh_top", true);
            case 2 ->
                showRewardListMocSM(player, "moc_suc_manh");
            case 3 -> {
                if (player.getSession().actived) {
                    ArchivementSucManh.gI().getAchievement(player);
                } else {
                    Service.gI().sendThongBao(player, "Bạn cần mở thành viên để nhận thưởng.");
                }
            }
        }
    }

    // Xử lý Top Nạp
    private void handleTopNapOptions(Player player, int select) {
        switch (select) {
            case 0 ->
                TopService.gI().showListTopVnd(player);
            case 1 ->
                showRewardList(player, "moc_nap_top", true);
        }
    }

    // Hiển thị danh sách phần thưởng Top hoặc Nạp
    private void showRewardList(Player player, String tableName, boolean isTopReward) {
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement("SELECT * FROM " + tableName); ResultSet rs = ps.executeQuery()) {

            StringBuilder sb = new StringBuilder("Danh sách phần thưởng:\n");

            while (rs.next()) {
                sb.append("\n------------------------------\n")
                        .append(isTopReward ? "Phần Thưởng TOP " : "Phần Thưởng Mốc ")
                        .append(rs.getInt("id")).append(":\n");

                appendItemList(sb, rs.getString("detail"));
            }
            Service.gI().sendThongBaoFromAdmin(player, sb.toString());

        } catch (SQLException ex) {
            logError(ex);
            Service.gI().sendThongBao(player, "Lỗi khi tải danh sách phần thưởng.");
        }
    }

    // Hiển thị danh sách phần thưởng mốc sức mạnh
    private void showRewardListMocSM(Player player, String tableName) {
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement("SELECT * FROM " + tableName); ResultSet rs = ps.executeQuery()) {

            StringBuilder sb = new StringBuilder("Danh sách phần thưởng:\n");

            while (rs.next()) {
                long power = rs.getLong("power");
                int id = rs.getInt("id");

                sb.append("\n------------------------------\n")
                        .append("Phần Thưởng Mốc ").append(id).append(":\n")
                        .append("Yêu cầu: ").append(String.format("%,d", power)).append(" Sức mạnh\n");

                appendItemList(sb, rs.getString("detail"));
            }
            Service.gI().sendThongBaoFromAdmin(player, sb.toString());

        } catch (SQLException ex) {
            logError(ex);
            Service.gI().sendThongBao(player, "Lỗi khi tải danh sách phần thưởng.");
        }
    }

    // Hàm chung để parse danh sách item từ JSON
    private void appendItemList(StringBuilder sb, String detailJson) {
        JSONArray dataArray = (JSONArray) JSONValue.parse(detailJson);
        int index = 1;

        for (Object obj : dataArray) {
            JSONObject data = (JSONObject) JSONValue.parse(obj.toString());
            int tempId = Integer.parseInt(String.valueOf(data.get("temp_id")));
            int quantity = Integer.parseInt(String.valueOf(data.get("quantity")));
            JSONArray options = (JSONArray) data.get("options");

            sb.append(String.format("%2d. x%d %s\n", index++, quantity,
                    ItemService.gI().getTemplate(tempId).name));

            if (options != null) {
                for (Object opt : options) {
                    JSONObject option = (JSONObject) opt;
                    int optionId = Integer.parseInt(String.valueOf(option.get("id")));
                    int param = Integer.parseInt(String.valueOf(option.get("param")));
                    String template = ItemService.gI().getItemOptionTemplate(optionId).name;
                    sb.append("     + ").append(template.replace("#", String.valueOf(param))).append("\n");
                }
            }
        }
    }

    // Log lỗi
    private void logError(Exception ex) {
        Logger.getLogger(DaiThienSu.class.getName()).log(Level.SEVERE, null, ex);
    }
}
