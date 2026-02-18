/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services.top;

import item.Item;
import item.Item.ItemOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import jdbc.DBConnecter;
import nro.player.Player;
import nro.server.Client;
import nro.services.InventoryService;
import nro.services.ItemService;
import nro.services.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import services.top.TopManager.TopTemplate;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;

/**
 *
 * @author Ts
 */
public class TOPAUTO {

    public TopTemplate template;
    public String info;
    public boolean isDone;
    public boolean isAuto;
    public boolean isSend;
    public int limit;
    public Timestamp date;
    public HashMap<Integer, ArrayList<Item>> items = new HashMap<>();
    public LinkedHashMap<Integer, Integer> users = new LinkedHashMap<>();

    public TOPAUTO(ResultSet rs, TopManager.TopTemplate template) throws Exception {
        date = rs.getTimestamp("date");
        isDone = rs.getBoolean("isDone");
        isAuto = rs.getBoolean("isAuto");
        limit = rs.getInt("limit");
        JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("users"));
        if (dataArray != null) {
            for (int i = 0; i < dataArray.size(); i++) {
                JSONObject dataObject = (JSONObject) dataArray.get(i);
                users.put(Integer.valueOf(dataObject.get("id").toString()), Integer.valueOf(dataObject.get("top").toString()));
                dataObject.clear();
            }
            dataArray.clear();
        }
        dataArray = (JSONArray) JSONValue.parse(rs.getString("items"));
        if (dataArray != null) {
            for (int top = 0; top < dataArray.size(); top++) {
                JSONObject dataObject = (JSONObject) dataArray.get(top);
                int topId = Integer.parseInt(dataObject.get("top").toString());
                items.put(topId, new ArrayList<>());
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataObject.get("items").toString());
                if (dataItem != null) {
                    for (int item = 0; item < dataItem.size(); item++) {
                        dataObject = (JSONObject) dataItem.get(item);
                        int itemId = Integer.parseInt(dataObject.get("id").toString());
                        int quantity = Integer.parseInt(dataObject.get("quantity").toString());
                        Item it = ItemService.gI().createNewItem((short) itemId, quantity);
                        JSONArray dataOption = (JSONArray) JSONValue.parse(dataObject.get("options").toString());
                        if (dataOption != null) {
                            for (int option = 0; option < dataOption.size(); option++) {
                                dataObject = (JSONObject) dataOption.get(option);
                                int optionId = Integer.parseInt(dataObject.get("id").toString());
                                int param = Integer.parseInt(dataObject.get("param").toString());
                                it.itemOptions.add(new ItemOption(optionId, param));
                            }
                            dataOption.clear();
                        }
                        dataObject.clear();
                        items.get(topId).add(it);
                    }
                    dataItem.clear();
                }
                dataObject.clear();
            }
            dataArray.clear();
        }
        this.template = template;
        getInfo();
    }

    private void getInfo() {
        //print info item gift
        StringBuilder text = new StringBuilder();
        text.append("|7|[ TOP ").append(template.name.toUpperCase()).append(" ]\n");
        text.append("|1|Trạng thái: ").append((isDone ? "Đã hoàn tất trao thưởng" : !isAuto ? "Chưa mở đua top" : !isToDate() ? "Chưa đến ngày trao thưởng" : items.isEmpty() ? "Chưa có danh sách phần thưởng" : "Đang tiến hành"));
        text.append("\n|2|Ngày kết thúc: ").append(TimeUtil.formatTime(date, "dd/MM/yyyy HH:mm:ss")).append("\n");
        if (!users.isEmpty()) {
            text.append("\n--\n|7|[ DANH SÁCH NGƯỜI CHƠI ĐÃ NHẬN THƯỞNG ]\n|-1|");
            for (Integer top : users.keySet()) {
                text.append(getName(top).toUpperCase()).append(" - TOP: ").append(users.get(top)).append("\n");
            }
        }
        if (!isDone && !items.isEmpty()) {
            text.append("\n--\n|7|[ DANH SÁCH PHẦN THƯỞNG ]\n");
            for (Integer top : items.keySet()) {
                if (items.get(top).isEmpty()) {
                    text.append("|7|Chưa có danh sách phần thưởng cho TOP này");
                    break;
                }
                text.append("|-1|TOP ").append(top).append("\n|3|");
                for (Item item : items.get(top)) {
                    text.append("x").append(Util.format(item.quantity)).append(" ").append(item.template.name).append("\n");
                }
            }
        }
        info = text.toString();
    }

    public boolean isToDate() {
        return System.currentTimeMillis() > date.getTime();
    }

    public boolean isNonReceive(int id) {
        return users.get(id) == null;
    }

    public boolean isCanGift() {
        return !isDone && isAuto && !items.isEmpty() && !template.tops.isEmpty();
    }

    public boolean isSendFullTop() {
        return users.size() >= limit || users.size() == template.tops.size() || users.isEmpty() && template.tops.isEmpty();
    }

    public void addPlayerReceive(int top, int id) {
        users.put(id, top);
    }

    public synchronized void addItemAutoGift(Connection con, int topId, Player plReceive) {
        Player plOnline = Client.gI().getPlayer(plReceive.id);
        if (plOnline != null) {
            for (Item item : items.get(topId)) {
                Item it = ItemService.gI().copyItem(item);
                if (it.template.id == 1467 || it.template.id == 1227) {
                    it.template = ItemService.gI().getTemplate(it.template.id + plOnline.gender);
                }
                it.message = "|2|Được gửi từ Hệ Thống\n|7|Quà Top " + template.name + " [TOP " + topId + "]" + "\n|3|Thời gian: " + TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
                InventoryService.gI().addItemGift(plOnline, it);
            }
            Service.gI().sendThongBaoOK(plOnline, "Bạn nhận được quà " + template.name + " kiểm tra tại hòm thư!");
            addPlayerReceive(topId, (int) plOnline.id);
            Logger.log(Logger.RED, "Auto Trao Online Quà " + Logger.BLUE + template.name + Logger.RED + " Vào Hòm Thư " + Logger.BLUE + plOnline.name + "[" + plReceive.id + "]" + " TOP:" + topId + Logger.RED + " Thành Công!");
        } else {
            try {
                PreparedStatement ps = con.prepareStatement("select items_box_lucky_round from player where id = ? limit 1");
                ps.setInt(1, (int) plReceive.id);
                ResultSet rs = ps.executeQuery();
                if (rs.first()) {
                    JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString(1));
                    if (dataArray != null) {
                        JSONArray arrItems = new JSONArray();
                        JSONArray options = new JSONArray();
                        JSONArray opt = new JSONArray();
                        for (Item item : items.get(topId)) {
                            arrItems.add((item.template.id == 1467 || item.template.id == 1227 ? item.template.id + plReceive.gender : item.template.id));
                            arrItems.add(item.quantity);
                            for (Item.ItemOption io : item.itemOptions) {
                                opt.add(io.optionTemplate.id);
                                opt.add(io.param);
                                options.add(opt.toJSONString());
                                opt.clear();
                            }
                            arrItems.add(options.toJSONString());
                            options.clear();
                            arrItems.add(item.createTime);
                            arrItems.add("|2|Được gửi từ Hệ Thống\n|7|Quà Top " + template.name + " [TOP " + topId + "]" + "\n|3|Thời gian: " + TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss"));
                            dataArray.add(arrItems.toJSONString());
                            arrItems.clear();
                        }
                        ps = con.prepareStatement("update player set items_box_lucky_round = ? where id = ?");
                        ps.setString(1, dataArray.toJSONString());
                        ps.setInt(2, (int) plReceive.id);
                        ps.executeUpdate();
                        dataArray.clear();
                        ps.close();
                        rs.close();
                        addPlayerReceive(topId, (int) plReceive.id);
                        Logger.log(Logger.RED, "Auto Trao Offline Quà " + Logger.BLUE + template.name + Logger.RED + " Vào Hòm Thư " + Logger.BLUE + plReceive.name + "[" + plReceive.id + "]" + " TOP:" + topId + Logger.RED + " Thành Công!");
                    }
                }
            } catch (SQLException e) {
            }
        }
    }

    public void update() {
        if (isSendFullTop()) {
            isDone = !isDone;
            isAuto = !isAuto;
        }
        JSONArray dataArray = new JSONArray();
        for (Integer i : users.keySet()) {
            LinkedHashMap data = new LinkedHashMap();
            data.put("top", this.users.get(i));
            data.put("id", i);
            data.put("log", TimeUtil.getTimeNow("HH:mm:ss dd/MM/yyyy"));
            dataArray.add(data);
        }
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement("UPDATE top_template SET users = '" + dataArray.toJSONString() + "', isDone = '" + isDone + "', isAuto = '" + isAuto + "' WHERE id = '" + template.id + "'")) {
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.logException(getClass(), e, "Lỗi Save AutoTop: " + template.name + " ID: " + template.id);
        } finally {
            isSend = !isSend;
            dataArray.clear();
            getInfo();
        }
    }

    public static synchronized String getName(Object value) {
        try (Connection con = DBConnecter.getConnectionServer()) {
            try {
                int id = Integer.parseInt(value.toString());
                try (PreparedStatement ps = con.prepareStatement("select name from player where id = '" + id + "' or account_id = '" + id + "' limit 1"); ResultSet rs = ps.executeQuery()) {
                    if (rs.first()) {
                        return rs.getString(1);
                    }
                }
            } catch (NumberFormatException e) {
                try (PreparedStatement ps = con.prepareStatement("select name from player where name = '" + value + "' limit 1"); ResultSet rs = ps.executeQuery()) {
                    if (rs.first()) {
                        return rs.getString(1);
                    }
                } catch (Exception ex) {
                }
            }
        } catch (Exception e) {
            Logger.logException(TOPAUTO.class, e);
        }
        return value.toString();
    }

    public void dispose() {
        date = null;
        template = null;
        if (items != null) {
            items.clear();
            items = null;
        }
        if (users != null) {
            users.clear();
            users = null;
        }
    }
}
