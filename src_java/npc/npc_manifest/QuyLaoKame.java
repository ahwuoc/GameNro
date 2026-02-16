package npc.npc_manifest;

import clan.Clan;
import consts.ConstNpc;
import item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdbc.DBConnecter;
import models.TreasureUnderSea.TreasureUnderSea;
import models.TreasureUnderSea.TreasureUnderSeaService;
import npc.Npc;
import static npc.NpcFactory.PLAYERID_OBJECT;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import player.Archivement;
import player.Achievement_TieuTien;
import player.Player;
import services.InventoryService;
import services.ItemService;
import services.NpcService;
import services.RewardService;
import services.Service;
import services.TaskService;
import services.func.ChangeMapService;
import services.func.Input;
import shop.ShopService;
import utils.Util;
import network.Message;

public class QuyLaoKame extends Npc {

    public QuyLaoKame(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            ArrayList<String> menu = new ArrayList<>();
            menu.add("Nói\nchuyện");
            menu.add("Quà Mốc Nạp");
            menu.add("Quà Mốc Tiêu Tiền");
            menu.add("Hộp Thư");
            menu.add("Hồi Skill");
            String[] menus = menu.toArray(String[]::new);
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU, "Con muốn hỏi gì nào?", menus);
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.canReward) {
                RewardService.gI().rewardLancon(player);
                return;
            }
            switch (player.iDMark.getIndexMenu()) {
                case ConstNpc.BASE_MENU -> {
                    switch (select) {
                        case 0 -> {
                            ArrayList<String> menu = new ArrayList<>();
                            menu.add("Nhiệm vụ");
                            menu.add("Học\nKỹ năng");
                            Clan clan = player.clan;
                            if (clan != null) {
                                menu.add("Về khu\nvực bang");
                                if (clan.isLeader(player)) {
                                    menu.add("Giải tán\nBang hội");
                                }
                            }
                            menu.add("Kho báu\ndưới biển");
                            String[] menus = menu.toArray(String[]::new);

                            this.createOtherMenu(player, 0,
                                    "Chào con, ta rất vui khi gặp con\nCon muốn làm gì nào ?", menus);
                        }
                        case 1 -> {
                            this.createOtherMenu(player, 1115, "Quà Mốc Nạp reset mỗi t2 :3", "Xem quà mốc nạp",
                                    "Nhận quà mốc nạp", "Đóng");
                        }
                        case 2 -> {
                            this.createOtherMenu(player, 1116, "Quà Mốc Tiêu Tiền - Tiêu vàng để nhận thưởng!",
                                    "Xem quà mốc", "Nhận quà mốc", "Top Tieu tien", "Đóng");
                        }
                        case 3 -> {
                            this.createOtherMenu(player, ConstNpc.MAIL_BOX,
                                    "|0|Tình yêu như một dây đàn\n"
                                            + "Tình vừa được thì đàn đứt dây\n"
                                            + "Đứt dây này anh thay dây khác\n"
                                            + "Mất em rồi anh biết thay ai?",
                                    "Hòm Thư\n(" + (player.inventory.itemsMailBox.size()
                                            - InventoryService.gI()
                                                    .getCountEmptyListItem(player.inventory.itemsMailBox))
                                            + " món)",
                                    "Xóa Hết\nHòm Thư", "Đóng");
                            break;
                        }
                        case 4 -> Service.gI().releaseCooldownSkill(player);
                    }
                }
                case ConstNpc.MAIL_BOX -> {
                    switch (select) {
                        case 0:
                            ShopService.gI().opendShop(player, "ITEMS_MAIL_BOX", true);
                            break;
                        case 1:
                            NpcService.gI().createMenuConMeo(player,
                                    ConstNpc.CONFIRM_REMOVE_ALL_ITEM_MAIL_BOX, this.avartar,
                                    "|3|Bạn chắc muốn xóa hết vật phẩm trong hòm thư?\n"
                                            + "|7|Sau khi xóa sẽ không thể khôi phục!",
                                    "Đồng ý", "Hủy bỏ");
                            break;
                        case 2:
                            break;
                    }
                }

                case 203 -> {

                }
                case 1115 -> {
                    switch (select) {
                        case 0:

                            JSONArray dataArray;
                            JSONObject dataObject;
                            PreparedStatement ps = null;
                            ResultSet rs = null;
                            StringBuilder sb = new StringBuilder();
                            sb.append("|0|꧁__Nạp tích lũy để nhận quà theo mốc reset t2 mỗi tuần_꧂\n");
                            try (Connection con2 = DBConnecter.getConnectionServer()) {
                                ps = con2.prepareStatement(
                                        "SELECT id, required_amount, detail FROM moc_nap ORDER BY id");
                                rs = ps.executeQuery();

                                while (rs.next()) {
                                    dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                                    int requiredAmount = rs.getInt("required_amount");
                                    sb.append("◥_____________________◤\n|7|");
                                    sb.append("✎▶Mốc Nạp ").append(String.format("%,d", requiredAmount))
                                            .append(" VND◀\n|0|");
                                    sb.append("◢¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯◣\n|0|");

                                    for (int i = 0; i < dataArray.size(); i++) {
                                        dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                                        int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                                        int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                                        JSONArray optionsArray = (JSONArray) dataObject.get("options");

                                        sb.append("▷ x").append(quantity).append(" ")
                                                .append(ItemService.gI().getTemplate(tempid).name).append("\n|4|");

                                        if (optionsArray != null) {
                                            for (int j = 0; j < optionsArray.size(); j++) {
                                                JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                                int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                                                int param = Integer.parseInt(String.valueOf(optionObject.get("param")));

                                                String optionTemplateName = ItemService.gI()
                                                        .getItemOptionTemplate(optionId).name;
                                                String formattedOption = optionTemplateName.replace("#",
                                                        String.valueOf(param));

                                                sb.append(formattedOption).append("\n");
                                            }
                                        }
                                        sb.append("\n|0|");
                                    }
                                }
                            } catch (SQLException ex) {
                                Logger.getLogger(QuyLaoKame.class.getName()).log(Level.SEVERE, null, ex);
                            }

                            Service.gI().sendThongBaoFromAdmin(player, sb.toString());

                            break;
                        case 1:
                            if (player.getSession().actived) {
                                Archivement.gI().getAchievement(player);
                            } else {
                                Service.gI().sendThongBao(player,
                                        "Mở thành viên tại King Kong đi rồi qua đây nhận nhe baby!");
                            }
                            break;
                        case 2:
                            break;
                    }
                }
                case 1116 -> {
                    switch (select) {
                        case 0:
                            // Xem quà mốc tiêu tiền
                            JSONArray dataArray2;
                            JSONObject dataObject2;
                            PreparedStatement ps2 = null;
                            ResultSet rs2 = null;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("|0|꧁__Mốc tiêu tiền – Nhận quà liền tay__꧂\n");
                            sb2.append("|7|Đã tiêu: ").append(String.format("%,d", player.tieutien))
                                    .append(" vàng\n\n");
                            try (Connection con2 = DBConnecter.getConnectionServer()) {
                                ps2 = con2.prepareStatement(
                                        "SELECT id, info, required_amount, detail FROM moc_tieutien ORDER BY required_amount");
                                rs2 = ps2.executeQuery();

                                while (rs2.next()) {
                                    dataArray2 = (JSONArray) JSONValue.parse(rs2.getString("detail"));
                                    int requiredAmount = rs2.getInt("required_amount");
                                    String info = rs2.getString("info");
                                    boolean received = player.achievement.isRecieveTieuTienMilestone(requiredAmount);
                                    boolean reached = player.tieutien >= requiredAmount;

                                    sb2.append("◥_____________________◤\n");
                                    sb2.append(received ? "|2|✓ " : (reached ? "|7|● " : "|0|○ "));
                                    sb2.append("Mốc ").append(String.format("%,d", requiredAmount)).append(" vàng");
                                    sb2.append(received ? " (Đã nhận)" : (reached ? " (Có thể nhận)" : ""))
                                            .append("\n|0|");
                                    sb2.append("◢¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯¯◣\n|0|");

                                    if (dataArray2 != null) {
                                        for (int i = 0; i < dataArray2.size(); i++) {
                                            dataObject2 = (JSONObject) JSONValue
                                                    .parse(String.valueOf(dataArray2.get(i)));
                                            int tempid = Integer.parseInt(String.valueOf(dataObject2.get("temp_id")));
                                            int quantity = Integer
                                                    .parseInt(String.valueOf(dataObject2.get("quantity")));
                                            JSONArray optionsArray = (JSONArray) dataObject2.get("options");

                                            sb2.append("▷ x").append(quantity).append(" ")
                                                    .append(ItemService.gI().getTemplate(tempid).name).append("\n|4|");

                                            if (optionsArray != null) {
                                                for (int j = 0; j < optionsArray.size(); j++) {
                                                    JSONObject optionObject = (JSONObject) optionsArray.get(j);
                                                    int optionId = Integer
                                                            .parseInt(String.valueOf(optionObject.get("id")));
                                                    int param = Integer
                                                            .parseInt(String.valueOf(optionObject.get("param")));

                                                    String optionTemplateName = ItemService.gI()
                                                            .getItemOptionTemplate(optionId).name;
                                                    String formattedOption = optionTemplateName.replace("#",
                                                            String.valueOf(param));

                                                    sb2.append(formattedOption).append("\n");
                                                }
                                            }
                                            sb2.append("\n|0|");
                                        }
                                    }
                                }
                            } catch (SQLException ex) {
                                Logger.getLogger(QuyLaoKame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            Service.gI().sendThongBaoFromAdmin(player, sb2.toString());
                            break;
                        case 1:
                            Achievement_TieuTien.openTieuTienMilestoneUI(player);
                            break;
                        case 2:
                            Message msgTop = null;
                            try {
                                msgTop = new Message(-96);
                                msgTop.writer().writeByte(0);
                                msgTop.writer().writeUTF("Top Tiêu Tiền");

                                try (Connection con = DBConnecter.getConnectionServer();
                                        PreparedStatement ps = con.prepareStatement(
                                                "SELECT name, head, gender, tieutien FROM player WHERE tieutien > 0 ORDER BY tieutien DESC LIMIT 10");
                                        ResultSet rs = ps.executeQuery()) {

                                    java.util.List<Object[]> topList = new java.util.ArrayList<>();
                                    while (rs.next()) {
                                        topList.add(new Object[] {
                                                rs.getString("name"),
                                                rs.getInt("head"),
                                                rs.getInt("gender"),
                                                rs.getLong("tieutien")
                                        });
                                    }

                                    msgTop.writer().writeByte(topList.size());
                                    for (int i = 0; i < topList.size(); i++) {
                                        Object[] data = topList.get(i);
                                        msgTop.writer().writeInt(i + 1);
                                        msgTop.writer().writeInt(i + 1);
                                        msgTop.writer().writeShort((int) data[1]); // head
                                        if (player.getSession().version >= 214) {
                                            msgTop.writer().writeShort(-1);
                                        }
                                        msgTop.writer().writeShort((int) data[2]); // body (gender)
                                        msgTop.writer().writeShort((int) data[2]); // leg (gender)
                                        msgTop.writer().writeUTF((String) data[0]); // name
                                        msgTop.writer().writeUTF("Tiêu tiền: " + Util.numberToMoney((long) data[3]));
                                        msgTop.writer().writeUTF("...");
                                    }
                                }
                                player.sendMessage(msgTop);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                if (msgTop != null) {
                                    msgTop.cleanup();
                                }
                            }
                            break;
                    }
                }
                case 0 -> {
                    switch (select) {
                        case 0 ->
                            NpcService.gI().createTutorial(player, tempId, avartar,
                                    player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name);
                        case 1 ->
                            Service.gI().sendThongBao(player, "Bạn đã học hết các kỹ năng");
                        case 2 -> {
                            Clan clan = player.clan;
                            if (clan != null && select == 2) {
                                if (player.nPoint.power <= 100_000_000_000L) {
                                    Service.gI().sendThongBao(player, "Yêu cầu sức mạnh đạt 100 tỉ");
                                    return;
                                }
                                ChangeMapService.gI().changeMapNonSpaceship(player, 156, Util.nextInt(392, 400), 192);
                            } else {
                                if (player.clan != null && player.clan.BanDoKhoBau != null) {
                                    this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                            "Bang hội con đang ở hang kho báu cấp "
                                                    + player.clan.BanDoKhoBau.level + "\ncon có muốn đi cùng họ không?",
                                            "Top\nBang hội", "Thành tích\nBang", "Đồng ý", "Từ chối");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                            "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\nỞ đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                            "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
                                }
                            }
                        }
                        case 3 -> {
                            boolean clanCheck = true;
                            Clan clan = player.clan;
                            if (clan != null) {
                                clanCheck = false;
                                if (clan.isLeader(player)) {
                                    createOtherMenu(player, 3, "Con có chắc muốn giải tán bang hội không?", "Đồng ý",
                                            "Từ chối");
                                } else {
                                    clanCheck = true;
                                }
                            }
                            if (clanCheck) {
                                if (player.clan != null && player.clan.BanDoKhoBau != null) {
                                    this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                            "Bang hội con đang ở hang kho báu cấp "
                                                    + player.clan.BanDoKhoBau.level + "\ncon có muốn đi cùng họ không?",
                                            "Top\nBang hội", "Thành tích\nBang", "Đồng ý", "Từ chối");
                                } else {
                                    this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                            "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\nỞ đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                            "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
                                }
                            }
                        }
                        case 4 -> {
                            if (player.clan != null && player.clan.BanDoKhoBau != null) {
                                this.createOtherMenu(player, ConstNpc.MENU_OPENED_DBKB,
                                        "Bang hội con đang ở hang kho báu cấp "
                                                + player.clan.BanDoKhoBau.level + "\ncon có muốn đi cùng họ không?",
                                        "Top\nBang hội", "Thành tích\nBang", "Đồng ý", "Từ chối");
                            } else {
                                this.createOtherMenu(player, ConstNpc.MENU_OPEN_DBKB,
                                        "Đây là bản đồ kho báu hải tặc tí hon\nCác con cứ yên tâm lên đường\nỞ đây có ta lo\nNhớ chọn cấp độ vừa sức mình nhé",
                                        "Top\nBang hội", "Thành tích\nBang", "Chọn\ncấp độ", "Từ chối");
                            }
                        }
                    }
                }
                case 3 -> {
                    Clan clan = player.clan;
                    if (clan != null) {
                        if (clan.isLeader(player)) {
                            if (select == 0) {
                                Input.gI().createFormGiaiTanBangHoi(player);
                            }
                        }
                    }
                }
                case ConstNpc.MENU_OPENED_DBKB -> {
                    switch (select) {
                        case 2 -> {
                            if (player.clan == null) {
                                Service.gI().sendThongBao(player, "Hãy vào bang hội trước");
                                return;
                            }
                            if (player.isAdmin() || player.nPoint.power >= TreasureUnderSea.POWER_CAN_GO_TO_DBKB) {
                                ChangeMapService.gI().goToDBKB(player);
                            } else {
                                this.npcChat(player, "Yêu cầu sức mạnh lớn hơn "
                                        + Util.numberToMoney(TreasureUnderSea.POWER_CAN_GO_TO_DBKB));
                            }
                        }

                    }
                }
                case ConstNpc.MENU_OPEN_DBKB -> {
                    switch (select) {
                        case 2 -> {
                            if (player.clan == null) {
                                Service.gI().sendThongBao(player, "Hãy vào bang hội trước");
                                return;
                            }
                            if (player.isAdmin() || player.nPoint.power >= TreasureUnderSea.POWER_CAN_GO_TO_DBKB) {
                                Input.gI().createFormChooseLevelBDKB(player);
                            } else {
                                this.npcChat(player, "Yêu cầu sức mạnh lớn hơn "
                                        + Util.numberToMoney(TreasureUnderSea.POWER_CAN_GO_TO_DBKB));
                            }
                        }

                    }
                }
                case ConstNpc.MENU_ACCEPT_GO_TO_BDKB -> {
                    switch (select) {
                        case 0 ->
                            TreasureUnderSeaService.gI().openBanDoKhoBau(player,
                                    Byte.parseByte(String.valueOf(PLAYERID_OBJECT.get(player.id))));
                    }
                }

            }
        }
    }

}
