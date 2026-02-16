package jdbc.daos;

import boss.AppearType;
import jdbc.DBConnecter;
import jdbc.daos.dto.BossDataDTO;
import jdbc.daos.dto.BossLevelDTO;
import jdbc.daos.dto.BossRewardDTO;
import utils.Logger;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for boss data stored in SQL database.
 * 
 * Updated for FLATTENED schema where each level is a separate row.
 * Groups rows by boss ID to create BossDataDTO with levels list.
 */
public class BossDataDAO {

    private static BossDataDAO instance;
    private final Gson gson = new Gson();

    /**
     * SQL query to load all boss data from flattened table.
     * Orders by id and level_index to ensure correct level ordering.
     */
    private static final String SQL_LOAD_ALL = "SELECT id, level_index, boss_name, display_name, level_name, " +
            "gender, dame, hp, outfit, map_join, " +
            "appear_type, seconds_rest, skills, " +
            "text_start, text_mid, text_end, " +
            "rewards, bosses_appear_together, " +
            "is_notify_disabled, is_zone01_spawn_disabled, special_class, auto_spawn, " +
            "IFNULL(damage_divisor, 1) as damage_divisor " +
            "FROM boss_data ORDER BY id, level_index";

    /**
     * SQL query to load boss by ID.
     */
    private static final String SQL_LOAD_BY_ID = "SELECT id, level_index, boss_name, display_name, level_name, " +
            "gender, dame, hp, outfit, map_join, " +
            "appear_type, seconds_rest, skills, " +
            "text_start, text_mid, text_end, " +
            "rewards, bosses_appear_together, " +
            "is_notify_disabled, is_zone01_spawn_disabled, special_class, auto_spawn, " +
            "IFNULL(damage_divisor, 1) as damage_divisor " +
            "FROM boss_data WHERE id = ? ORDER BY level_index";

    /**
     * SQL query to load boss by name.
     */
    private static final String SQL_LOAD_BY_NAME = "SELECT id, level_index, boss_name, display_name, level_name, " +
            "gender, dame, hp, outfit, map_join, " +
            "appear_type, seconds_rest, skills, " +
            "text_start, text_mid, text_end, " +
            "rewards, bosses_appear_together, " +
            "is_notify_disabled, is_zone01_spawn_disabled, special_class, auto_spawn, " +
            "IFNULL(damage_divisor, 1) as damage_divisor " +
            "FROM boss_data WHERE boss_name = ? ORDER BY level_index";

    private BossDataDAO() {
    }

    public static BossDataDAO gI() {
        if (instance == null) {
            instance = new BossDataDAO();
        }
        return instance;
    }

    /**
     * Load all boss data from database.
     * Groups flattened rows by boss ID into BossDataDTO objects.
     */
    public List<BossDataDTO> loadAllBossData() {
        Map<Integer, BossDataDTO> bossMap = new LinkedHashMap<>();

        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(SQL_LOAD_ALL);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int bossId = rs.getInt("id");

                // Get or create BossDataDTO
                BossDataDTO dto = bossMap.get(bossId);
                if (dto == null) {
                    dto = createBossDTO(rs);
                    bossMap.put(bossId, dto);
                }

                // Add level to boss
                BossLevelDTO level = createLevelDTO(rs);
                dto.getLevels().add(level);
            }

            Logger.log("Loaded " + bossMap.size() + " boss configurations from database\n");

        } catch (Exception e) {
            Logger.error("Failed to load boss data from database: " + e.getMessage() + "\n");
        }

        return new ArrayList<>(bossMap.values());
    }

    /**
     * Load boss data by boss ID.
     */
    public BossDataDTO loadBossById(int bossId) {
        BossDataDTO dto = null;

        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(SQL_LOAD_BY_ID)) {

            ps.setInt(1, bossId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (dto == null) {
                        dto = createBossDTO(rs);
                    }
                    BossLevelDTO level = createLevelDTO(rs);
                    dto.getLevels().add(level);
                }
            }

        } catch (Exception e) {
            Logger.error("Failed to load boss by ID " + bossId + ": " + e.getMessage() + "\n");
        }

        return dto;
    }

    /**
     * Load boss data by boss name.
     */
    public BossDataDTO loadBossByName(String bossName) {
        if (bossName == null || bossName.isEmpty()) {
            return null;
        }

        BossDataDTO dto = null;

        try (Connection con = DBConnecter.getConnectionServer();
                PreparedStatement ps = con.prepareStatement(SQL_LOAD_BY_NAME)) {

            ps.setString(1, bossName);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (dto == null) {
                        dto = createBossDTO(rs);
                    }
                    BossLevelDTO level = createLevelDTO(rs);
                    dto.getLevels().add(level);
                }
            }

        } catch (Exception e) {
            Logger.error("Failed to load boss by name " + bossName + ": " + e.getMessage() + "\n");
        }

        return dto;
    }

    private BossDataDTO createBossDTO(ResultSet rs) throws Exception {
        BossDataDTO dto = new BossDataDTO();
        dto.setBossId(rs.getInt("id"));
        dto.setBossName(rs.getString("boss_name"));
        dto.setDisplayName(rs.getString("display_name"));
        dto.setGender(rs.getByte("gender"));
        dto.setNotifyDisabled(rs.getBoolean("is_notify_disabled"));
        dto.setZone01SpawnDisabled(rs.getBoolean("is_zone01_spawn_disabled"));
        dto.setSpecialClass(rs.getString("special_class"));
        dto.setAutoSpawn(rs.getBoolean("auto_spawn"));
        dto.setLevels(new ArrayList<>());
        return dto;
    }

    private BossLevelDTO createLevelDTO(ResultSet rs) throws Exception {
        BossLevelDTO level = new BossLevelDTO();

        level.setLevel(rs.getInt("level_index"));
        level.setName(rs.getString("level_name"));
        level.setDame(rs.getLong("dame"));

        // Parse JSON fields
        level.setHp(parseJsonLongArray(rs.getString("hp")));
        level.setOutfit(parseJsonShortArray(rs.getString("outfit")));
        level.setMapJoin(parseJsonIntArray(rs.getString("map_join")));
        level.setSkills(parseJsonInt2DArray(rs.getString("skills")));
        level.setTextS(parseJsonStringArray(rs.getString("text_start")));
        level.setTextM(parseJsonStringArray(rs.getString("text_mid")));
        level.setTextE(parseJsonStringArray(rs.getString("text_end")));
        level.setBossesAppearTogether(parseJsonIntArray(rs.getString("bosses_appear_together")));

        // Parse rewards
        String rewardsJson = rs.getString("rewards");
        if (rewardsJson != null && !rewardsJson.isEmpty() && !rewardsJson.equals("[]")) {
            Type listType = new TypeToken<List<BossRewardDTO>>() {
            }.getType();
            level.setRewards(gson.fromJson(rewardsJson, listType));
        }

        // Parse appear type
        String appearTypeStr = rs.getString("appear_type");
        try {
            level.setAppearType(AppearType.valueOf(appearTypeStr));
        } catch (Exception e) {
            level.setAppearType(AppearType.DEFAULT_APPEAR);
        }

        level.setSecondsRest(rs.getInt("seconds_rest"));

        // Parse damage divisor - try to get from DB, default 1 if column doesn't exist
        try {
            level.setDamageDivisor(rs.getInt("damage_divisor"));
        } catch (Exception e) {
            level.setDamageDivisor(1); // Default - no reduction
        }

        return level;
    }

    // JSON parsing helpers
    private long[] parseJsonLongArray(String json) {
        if (json == null || json.isEmpty() || json.equals("[]"))
            return new long[0];
        try {
            Long[] arr = gson.fromJson(json, Long[].class);
            long[] result = new long[arr.length];
            for (int i = 0; i < arr.length; i++)
                result[i] = arr[i];
            return result;
        } catch (Exception e) {
            return new long[0];
        }
    }

    private short[] parseJsonShortArray(String json) {
        if (json == null || json.isEmpty() || json.equals("[]"))
            return new short[0];
        try {
            Integer[] arr = gson.fromJson(json, Integer[].class);
            short[] result = new short[arr.length];
            for (int i = 0; i < arr.length; i++)
                result[i] = arr[i].shortValue();
            return result;
        } catch (Exception e) {
            return new short[0];
        }
    }

    private int[] parseJsonIntArray(String json) {
        if (json == null || json.isEmpty() || json.equals("[]") || json.equals("null"))
            return null;
        try {
            Integer[] arr = gson.fromJson(json, Integer[].class);
            if (arr == null)
                return null;
            int[] result = new int[arr.length];
            for (int i = 0; i < arr.length; i++)
                result[i] = arr[i];
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private int[][] parseJsonInt2DArray(String json) {
        if (json == null || json.isEmpty() || json.equals("[]"))
            return new int[0][];
        try {
            return gson.fromJson(json, int[][].class);
        } catch (Exception e) {
            return new int[0][];
        }
    }

    private String[] parseJsonStringArray(String json) {
        if (json == null || json.isEmpty() || json.equals("[]"))
            return new String[0];
        try {
            return gson.fromJson(json, String[].class);
        } catch (Exception e) {
            return new String[0];
        }
    }
}
