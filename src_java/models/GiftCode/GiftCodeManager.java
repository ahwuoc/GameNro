package models.GiftCode;

import nro.player.Player;
import nro.services.NpcService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import nro.services.Service;
import item.Item.ItemOption;
import jdbc.DBConnecter;

public class GiftCodeManager {

    public final ArrayList<GiftCode> listGiftCode = new ArrayList<>();
    private static GiftCodeManager instance;

    public static GiftCodeManager gI() {
        if (instance == null) instance = new GiftCodeManager();
        return instance;
    }

    /**
     * Load giftcode từ database
     */
    public void loadGiftCodeFromDB() {
        listGiftCode.clear();
        try (Connection con2 = DBConnecter.getConnectionServer()) {
            if (con2 == null) {
                System.out.println("Lỗi: Không kết nối được DB server!");
                return;
            }

            PreparedStatement ps = con2.prepareStatement("SELECT * FROM giftcode");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GiftCode gc = new GiftCode();
                gc.id = rs.getInt("id");
                gc.code = rs.getString("code");
                gc.countLeft = rs.getInt("count_left");
                if (gc.countLeft == -1) gc.countLeft = 999999999; // số lượng vô hạn
                gc.datecreate = rs.getTimestamp("datecreate");
                gc.dateexpired = rs.getTimestamp("expired");
                gc.type = rs.getInt("type");

                // Parse JSON detail
                String detailStr = rs.getString("detail");
                JSONArray jar = (JSONArray) JSONValue.parse(detailStr);
                gc.detail = new HashMap<>();
                gc.option = new HashMap<>();

                if (jar != null) {
                    for (int i = 0; i < jar.size(); i++) {
                        JSONObject jsonObj = (JSONObject) jar.get(i);
                        int tempId = Integer.parseInt(jsonObj.get("temp_id").toString());
                        int quantity = Integer.parseInt(jsonObj.get("quantity").toString());
                        gc.detail.put(tempId, quantity);

                        JSONArray optArr = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();
                        if (optArr != null) {
                            for (int j = 0; j < optArr.size(); j++) {
                                JSONObject opt = (JSONObject) optArr.get(j);
                                int param = Integer.parseInt(opt.get("param").toString());
                                int id = Integer.parseInt(opt.get("id").toString());
                                optionList.add(new ItemOption(id, param));
                            }
                        }
                        gc.option.put(tempId, optionList);
                    }
                }

                listGiftCode.add(gc);
                System.out.println("Load giftcode: " + gc.code + ", Số lượng: " + gc.countLeft);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public GiftCode checkUseGiftCode(Player player, String code) {
        for (GiftCode giftCode : listGiftCode) {
            if (!giftCode.code.equals(code)) continue;

            if (giftCode.countLeft <= 0) {
                Service.gI().sendThongBao(player, "Giftcode đã hết lượt nhập");
                return null;
            }
            if (giftCode.isUsedGiftCode(player)) {
                Service.gI().sendThongBao(player, "Bạn đã sử dụng GiftCode này rồi");
                return null;
            }
            if (giftCode.type == 1 && !player.getSession().actived) {
                Service.gI().sendThongBao(player, "Bạn cần mở thành viên để sử dụng mã này.");
                return null;
            }

            giftCode.countLeft--;
            player.giftCode.add(code);
            updateGiftCode(giftCode);
            return giftCode;
        }
        return null;
    }

    public void updateGiftCode(GiftCode giftcode) {
        try {
            DBConnecter.executeUpdate(
                "UPDATE giftcode SET count_left = ? WHERE id = ?",
                giftcode.countLeft, giftcode.id
            );
        } catch (Exception ignored) {}
    }

    public void checkInfomationGiftCode(Player player) {
        if (listGiftCode.isEmpty()) {
            NpcService.gI().createTutorial(player, 5073, "Hiện tại không có Giftcode nào.");
            return;
        }

        List<String> lines = new ArrayList<>();
        for (GiftCode gc : listGiftCode) {
            lines.add("Code: " + gc.code +
                      ", Số lượng còn lại: " + gc.countLeft +
                      ", Ngày tạo: " + gc.datecreate +
                      ", Ngày hết hạn: " + gc.dateexpired);
        }

        String result = String.join("\n", lines);
        NpcService.gI().createTutorial(player, 5073, result);
    }
}
