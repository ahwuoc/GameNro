/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services.top;

import item.Item;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import jdbc.DBConnecter;
import nro.player.Player;
import nro.services.ItemService;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/**
 *
 * @author Administrator
 */
public class TopTrung {

    @Getter
    private List<Player> list = new ArrayList<>();
    private static final TopTrung INSTANCE = new TopTrung();

    public static TopTrung getInstance() {
        return INSTANCE;
    }

    public void load() {
        list.clear();
        try (Connection con = DBConnecter.getConnectionServer(); java.sql.PreparedStatement ps = con.prepareStatement(
                "SELECT *, "
                + "CAST(JSON_EXTRACT(point_pokemon, '$[0]') AS UNSIGNED) AS trungNe "
                + "FROM player "
                + "ORDER BY trungNe DESC "
                + "LIMIT 100"); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Player player = processPlayerResultSet(rs);
                list.add(player);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Player processPlayerResultSet(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
//        player.point_pokemon = rs.getInt("trungNe");
        extractDataPoint(rs.getString("data_point"), player);
        extractItemsBody(rs.getString("items_body"), player);
        return player;
    }

    private void extractDataPoint(String dataPoint, Player player) {
        JSONValue jv = new JSONValue();
        JSONArray dataArray = (JSONArray) jv.parse(dataPoint);
        player.nPoint.power = Long.parseLong(dataArray.get(11).toString());
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
