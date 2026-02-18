package map;

import java.sql.Connection;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdbc.DBConnecter;
import org.json.JSONException;

/**
 * @build by barcoll
 */
public class EffectEventManager {

    private static final EffectEventManager i = new EffectEventManager();

    public static EffectEventManager gI() {
        return i;
    }

    @Getter
    private final List<EffectEventTemplate> list = new ArrayList<>();

    public void load() {
        try {
            PreparedStatement ps = DBConnecter.getConnectionServer().prepareStatement("SELECT * FROM `map_template`");
            ResultSet rs = ps.executeQuery();
            try {
                while (rs.next()) {
                    int mapID = rs.getInt("id");
                    JSONArray jar = new JSONArray(rs.getString("eff_event"));
                    for (int j = 0; j < jar.length(); j++) {
                        JSONObject jobj = jar.getJSONObject(j);
                        int evID = jobj.getInt("event_id");
                        int effID = jobj.getInt("eff_id");
                        int layer = jobj.getInt("layer");
                        int x = jobj.getInt("x");
                        int y = jobj.getInt("y");
                        int loop = jobj.getInt("loop");
                        int delay = jobj.getInt("delay");

                        EffectEventTemplate ee = EffectEventTemplate.builder()
                                .mapId(mapID)
                                .eventId(evID)
                                .effId(effID)
                                .layer(layer)
                                .x(x)
                                .y(y)
                                .loop(loop)
                                .delay(delay)
                                .build();
                        add(ee);
                    }
                }
            } catch (JSONException ex) {
                Logger.getLogger(EffectEventManager.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                rs.close();
                ps.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void add(EffectEventTemplate ee) {
        list.add(ee);
    }
}
