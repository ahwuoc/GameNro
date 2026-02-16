package boss;

import jdbc.daos.BossDataDAO;
import jdbc.daos.dto.BossDataDTO;
import jdbc.daos.dto.BossLevelDTO;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class BossDataLoader {
    
    private static BossDataLoader instance;
   
    private final Map<Integer, BossData[]> bossDataCache = new ConcurrentHashMap<>();
    
    
    private final Map<Integer, BossDataDTO> bossDTOCache = new ConcurrentHashMap<>();
    
    private final BossDataDAO dao;
    
    private BossDataLoader() {
        this.dao = BossDataDAO.gI();
    }
    
  
    public static BossDataLoader gI() {
        if (instance == null) {
            synchronized (BossDataLoader.class) {
                if (instance == null) {
                    instance = new BossDataLoader();
                }
            }
        }
        return instance;
    }
    
    
    public void loadAll() {
        try {
            List<BossDataDTO> allData = dao.loadAllBossData();
            
            for (BossDataDTO dto : allData) {
                BossData[] levels = convertToLegacyFormat(dto);
                bossDataCache.put(dto.getBossId(), levels);
                bossDTOCache.put(dto.getBossId(), dto);
            }
            
            Logger.log("BossDataLoader: Loaded " + bossDataCache.size() + " boss configurations into cache\n");
            
        } catch (Exception e) {
            Logger.error("BossDataLoader: Failed to load boss data from database: " + e.getMessage() + "\n");
        }
    }
    
   
    public BossData[] getBossData(int bossId) {
        return bossDataCache.get(bossId);
    }
   
    public BossDataDTO getBossDTO(int bossId) {
        return bossDTOCache.get(bossId);
    }
    
    public boolean hasBossData(int bossId) {
        return bossDataCache.containsKey(bossId);
    }
    
    
    public List<Integer> getAllBossIds() {
        return new ArrayList<>(bossDataCache.keySet());
    }
    
   
    public int getCacheSize() {
        return bossDataCache.size();
    }
    
    public void reload() {
        int beforeCount = bossDataCache.size();
        Logger.log("BossDataLoader: Reloading boss data from database...\n");
        bossDataCache.clear();
        bossDTOCache.clear();
        loadAll();
        int afterCount = bossDataCache.size();
        Logger.log("BossDataLoader: Reload complete. " + beforeCount + " -> " + afterCount + " boss configurations\n");
    }
    
    
    public DataSourceStats getStats() {
        DataSourceStats stats = new DataSourceStats();
        stats.totalBossCount = bossDataCache.size();
        
        for (BossDataDTO dto : bossDTOCache.values()) {
            if (dto.getSpecialClass() != null && !dto.getSpecialClass().isEmpty()) {
                stats.specialBossCount++;
            } else {
                stats.genericBossCount++;
            }
        }
        
        return stats;
    }
    
    public static class DataSourceStats {
        public int totalBossCount;
        public int specialBossCount;
        public int genericBossCount;
        
        @Override
        public String toString() {
            return "DataSourceStats{total=" + totalBossCount + 
                   ", special=" + specialBossCount + 
                   ", generic=" + genericBossCount + "}";
        }
    }

   
    public BossData[] convertToLegacyFormat(BossDataDTO dto) {
        if (dto == null || dto.getLevels() == null || dto.getLevels().isEmpty()) {
            return new BossData[0];
        }
        
        List<BossLevelDTO> levels = dto.getLevels();
        BossData[] result = new BossData[levels.size()];
        
        for (int i = 0; i < levels.size(); i++) {
            BossLevelDTO level = levels.get(i);
            result[i] = convertLevelToLegacy(dto, level);
        }
        
        return result;
    }
    
    /**
     * Convert a single BossLevelDTO to BossData.
     * 
     * @param dto Parent DTO for gender info
     * @param level The level DTO to convert
     * @return BossData instance
     */
    private BossData convertLevelToLegacy(BossDataDTO dto, BossLevelDTO level) {
        // Convert skills from int[][] to match expected format
        int[][] skills = level.getSkills();
        if (skills == null) {
            skills = new int[0][];
        }
        
        // Convert text arrays, ensuring non-null
        String[] textS = level.getTextS() != null ? level.getTextS() : new String[0];
        String[] textM = level.getTextM() != null ? level.getTextM() : new String[0];
        String[] textE = level.getTextE() != null ? level.getTextE() : new String[0];
        
        // Convert hp from long[] - ensure non-null
        long[] hp = level.getHp() != null ? level.getHp() : new long[]{1000};
        
        // Convert outfit from short[] - ensure non-null
        short[] outfit = level.getOutfit() != null ? level.getOutfit() : new short[]{-1, -1, -1, -1, -1, -1};
        
        // Convert mapJoin from int[] - ensure non-null
        int[] mapJoin = level.getMapJoin() != null ? level.getMapJoin() : new int[]{0};
        
        // Get appear type, default to DEFAULT_APPEAR
        AppearType appearType = level.getAppearType() != null ? level.getAppearType() : AppearType.DEFAULT_APPEAR;
        
        // Get seconds rest
        int secondsRest = level.getSecondsRest();
        
        // Get bosses appear together
        int[] bossesAppearTogether = level.getBossesAppearTogether();
        
        // Create BossData using appropriate constructor based on available data
        BossData bossData;
        
        if (bossesAppearTogether != null && bossesAppearTogether.length > 0) {
            // Has bosses that appear together
            bossData = new BossData(
                level.getName(),
                dto.getGender(),
                outfit,
                level.getDame(),
                hp,
                mapJoin,
                skills,
                textS,
                textM,
                textE,
                secondsRest,
                bossesAppearTogether
            );
        } else if (appearType != AppearType.DEFAULT_APPEAR) {
            if (secondsRest > 0) {
                bossData = new BossData(
                    level.getName(),
                    dto.getGender(),
                    outfit,
                    level.getDame(),
                    hp,
                    mapJoin,
                    skills,
                    textS,
                    textM,
                    textE,
                    secondsRest,
                    appearType
                );
            } else {
                bossData = new BossData(
                    level.getName(),
                    dto.getGender(),
                    outfit,
                    level.getDame(),
                    hp,
                    mapJoin,
                    skills,
                    textS,
                    textM,
                    textE,
                    appearType
                );
            }
        } else {
            bossData = new BossData(
                level.getName(),
                dto.getGender(),
                outfit,
                level.getDame(),
                hp,
                mapJoin,
                skills,
                textS,
                textM,
                textE,
                secondsRest
            );
        }
        
        return bossData;
    }
}
