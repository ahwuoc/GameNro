package services.top;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import lombok.Builder;
import lombok.Data;
import nro.player.Player;
import nro.services.ItemService;
import nro.services.TaskService;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import task.TaskMain;

@Data
@Builder
public class TOP {

    private Player player;
    protected String info1;
    protected String info2;
    private long lasttime;
    private int id_player;
    private int level;
     private long time;

    public synchronized TOP findPlayer(Connection con, int id) throws Exception {
        try (PreparedStatement ps = con.prepareStatement("select * from player where id = '" + id + "' limit 1"); ResultSet rs = ps.executeQuery()) {
            if (rs.first()) {
                player = new Player();
                getInfo(rs);
                getBody(rs);
            }
        }
        return this;
    }

    private synchronized void getInfo(ResultSet rs) throws Exception {
        player.id = rs.getInt("id");
        player.name = rs.getString("name");
        player.head = rs.getShort("head");
        player.gender = rs.getByte("gender");
    }

    public synchronized TOP getPoint(ResultSet rs) throws Exception {
        player.nPoint.power = rs.getLong("sm");
        return this;
    }

    public synchronized TOP getTask(ResultSet rs) throws Exception {
        TaskMain taskMain = TaskService.gI().getTaskMainById(player, rs.getByte("nv"));
        taskMain.index = rs.getByte("nvp");
        taskMain.subTasks.get(taskMain.index).count = rs.getShort("count");
        taskMain.lastTime = rs.getLong("time");
        player.playerTask.taskMain = taskMain;
        return this;
    }

    private synchronized void getBody(ResultSet rs) throws Exception {
        JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("items_body"));
        if (dataArray != null && !dataArray.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                player.inventory.itemsBody.add(ItemService.gI().createItemNull());
            }
            JSONArray dataCaiTrang = (JSONArray) JSONValue.parse(dataArray.get(5).toString());
            short tempId = Short.parseShort(String.valueOf(dataCaiTrang.get(0)));
            if (tempId != -1) {
                player.inventory.itemsBody.set(5, ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataCaiTrang.get(1)))));
            }
        }
    }

    public synchronized TOP setInfo1(String info) {
        info1 = info;
        return this;
    }

    public synchronized TOP setInfo2(String info) {
        info2 = info;
        return this;
    }

    public void dispose() {
        if (player != null) {
            player.dispose();
            player = null;
        }
        info1 = null;
        info2 = null;
    }
}
