package Top;

import item.Item;
import item.Item.ItemOption;
import lombok.Getter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jdbc.DBConnecter;
import nro.player.Player;
import nro.services.ItemService;

/**
 * @author outcast c-cute hột me 😳
 */
public class MyClanTopKhiGas {

    @Getter
    private List<Player> list = new ArrayList<>();

    private static final MyClanTopKhiGas INSTANCE = new MyClanTopKhiGas();

    public static MyClanTopKhiGas getInstance() {
        return INSTANCE;
    }

    public void load(int idLeader) {
        list.clear();

        try ( Connection con = DBConnecter.getConnectionServer();  PreparedStatement ps = con.prepareStatement("SELECT *, "
                + "SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang2, ',', 1), '[', -1) AS so1, "
                + "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang2, ',', 2), ',', -1) AS SIGNED) AS so2, "
                + "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang2, ',', 3), ',', -1) AS SIGNED) AS so3, "
                + "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang2, ',', 4), ',', -1) AS SIGNED) AS so4 "
                + "FROM player WHERE CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(SUBSTRING_INDEX(thanhTichBang2, ',', 2), ',', -1), ']', 1) AS SIGNED) > 0 "
                + "AND id = ? ORDER BY so2 DESC, so1 ASC LIMIT 100")) {

            // Đặt giá trị thực tế của idLeader vào câu truy vấn
            ps.setInt(1, idLeader);

            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Player player = extractPlayerFromResultSet(rs);
                    list.add(player);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Player extractPlayerFromResultSet(ResultSet rs) throws SQLException {
        Player player = new Player();

        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
        player.firstTimeLogin = rs.getTimestamp("firstTimeLogin");
        player.nameClan = rs.getString("so1");
        player.levelKhiGasDone = rs.getInt("so2");
        player.timeKhiGasDone = rs.getLong("so3");

        player.lastTimeUpdateTopKhiGas = (System.currentTimeMillis() - rs.getLong("so4")) / 1000;

        extractDataPoint(rs.getString("data_point"), player);
        extractItemsBody(rs.getString("items_body"), player);

        return player;
    }

    private void extractDataPoint(String dataPoint, Player player) {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = (JSONArray) jv.parse(dataPoint);
        player.nPoint.power = Long.parseLong(dataArray.get(1).toString());
        dataArray.clear();
    }

    private void extractItemsBody(String itemsBody, Player player) {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = (JSONArray) jv.parse(itemsBody);

        for (Object itemDataObject : dataArray) {
            Item item = createItemFromDataObject(itemDataObject.toString());
            player.inventory.itemsBody.add(item);
        }

        dataArray.clear();
    }

    private Item createItemFromDataObject(String itemData) {
        JSONValue jv = new JSONValue();
        JSONArray dataObject = (JSONArray) jv.parse(itemData);
        short tempId = Short.parseShort(String.valueOf(dataObject.get(0)));
        Item item;
        if (tempId != -1) {
            item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get(1))));
            JSONArray options = (JSONArray) jv.parse(String.valueOf(dataObject.get(2)).replaceAll("\"", ""));

            for (Object option : options) {
                JSONArray opt = (JSONArray) jv.parse(String.valueOf(option));
                item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                        Integer.parseInt(String.valueOf(opt.get(1)))));
            }
            item.createTime = Long.parseLong(String.valueOf(dataObject.get(3)));
            if (ItemService.gI().isOutOfDateTime(item)) {
                item = ItemService.gI().createItemNull();
            }
        } else {
            item = ItemService.gI().createItemNull();
        }

        return item;
    }
}
