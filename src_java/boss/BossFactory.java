package boss;

import java.lang.reflect.Constructor;
import utils.Logger;

/**
 * Factory class for creating boss instances.
 * Uses SpecialBossRegistry to determine if a boss needs a special class
 * or can use the generic Boss class.
 * 
 * Requirements: 2.1, 3.2, 3.4
 */
public class BossFactory {
    
    /**
     * Create a boss instance using the appropriate class.
     * If the boss ID is registered in SpecialBossRegistry, uses that class.
     * Otherwise, creates a generic Boss instance.
     * 
     * @param bossId The boss ID
     * @param data The boss data array for each level
     * @return The created boss instance
     * @throws Exception If boss creation fails
     */
    public static Boss createBoss(int bossId, BossData... data) throws Exception {
        return createBoss(bossId, false, false, data);
    }
    
    /**
     * Create a boss instance with notify and zone spawn settings.
     * 
     * @param bossId The boss ID
     * @param isNotifyDisabled Whether to disable spawn notifications
     * @param isZone01SpawnDisabled Whether to disable spawning in zone 01
     * @param data The boss data array for each level
     * @return The created boss instance
     * @throws Exception If boss creation fails
     */
    public static Boss createBoss(int bossId, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        Class<? extends Boss> specialClass = SpecialBossRegistry.getClass(bossId);
        
        if (specialClass != null) {
            // Special boss - use registered class
            return createSpecialBoss(specialClass, bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
        } else {
            // Generic boss - use base Boss class
            return new Boss(bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
        }
    }
    
    /**
     * Create a boss instance with BossType.
     * 
     * @param bossType The boss type (determines which manager handles the boss)
     * @param bossId The boss ID
     * @param data The boss data array for each level
     * @return The created boss instance
     * @throws Exception If boss creation fails
     */
    public static Boss createBoss(BossType bossType, int bossId, BossData... data) throws Exception {
        return createBoss(bossType, bossId, false, false, data);
    }
    
    /**
     * Create a boss instance with BossType and settings.
     * 
     * @param bossType The boss type
     * @param bossId The boss ID
     * @param isNotifyDisabled Whether to disable spawn notifications
     * @param isZone01SpawnDisabled Whether to disable spawning in zone 01
     * @param data The boss data array for each level
     * @return The created boss instance
     * @throws Exception If boss creation fails
     */
    public static Boss createBoss(BossType bossType, int bossId, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        Class<? extends Boss> specialClass = SpecialBossRegistry.getClass(bossId);
        
        if (specialClass != null) {
            // Special boss - use registered class with BossType
            return createSpecialBossWithType(specialClass, bossType, bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
        } else {
            // Generic boss - use base Boss class with BossType
            return new Boss(bossType, bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
        }
    }
    
    /**
     * Create a special boss using reflection.
     * Tries multiple constructor signatures to find a matching one.
     */
    private static Boss createSpecialBoss(Class<? extends Boss> bossClass, int bossId, 
            boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        
        // Try constructor with all parameters: (int, boolean, boolean, BossData...)
        try {
            Constructor<? extends Boss> constructor = bossClass.getConstructor(
                int.class, boolean.class, boolean.class, BossData[].class
            );
            return constructor.newInstance(bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
        } catch (NoSuchMethodException e) {
            // Try next signature
        }
        
        // Try constructor with (int, BossData...)
        try {
            Constructor<? extends Boss> constructor = bossClass.getConstructor(
                int.class, BossData[].class
            );
            Boss boss = constructor.newInstance(bossId, data);
            boss.isNotifyDisabled = isNotifyDisabled;
            boss.isZone01SpawnDisabled = isZone01SpawnDisabled;
            return boss;
        } catch (NoSuchMethodException e) {
            // Try next signature
        }
        
        // Try no-arg constructor (most special bosses use this)
        try {
            Constructor<? extends Boss> constructor = bossClass.getConstructor();
            Boss boss = constructor.newInstance();
            boss.isNotifyDisabled = isNotifyDisabled;
            boss.isZone01SpawnDisabled = isZone01SpawnDisabled;
            return boss;
        } catch (NoSuchMethodException e) {
            // No matching constructor found
        }
        
        throw new Exception("No suitable constructor found for special boss class: " + bossClass.getName());
    }
    
    /**
     * Create a special boss with BossType using reflection.
     */
    private static Boss createSpecialBossWithType(Class<? extends Boss> bossClass, BossType bossType, 
            int bossId, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        
        // Try constructor with BossType: (BossType, int, boolean, boolean, BossData...)
        try {
            Constructor<? extends Boss> constructor = bossClass.getConstructor(
                BossType.class, int.class, boolean.class, boolean.class, BossData[].class
            );
            return constructor.newInstance(bossType, bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
        } catch (NoSuchMethodException e) {
            // Try next signature
        }
        
        // Try constructor with (BossType, int, BossData...)
        try {
            Constructor<? extends Boss> constructor = bossClass.getConstructor(
                BossType.class, int.class, BossData[].class
            );
            Boss boss = constructor.newInstance(bossType, bossId, data);
            boss.isNotifyDisabled = isNotifyDisabled;
            boss.isZone01SpawnDisabled = isZone01SpawnDisabled;
            return boss;
        } catch (NoSuchMethodException e) {
            // Try next signature
        }
        
        // Fall back to non-BossType constructor
        return createSpecialBoss(bossClass, bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
    }
    
    /**
     * Create a boss from database data.
     * Loads BossData from BossDataLoader and creates the appropriate boss instance.
     * 
     * ALL boss data MUST come from SQL database. No fallback to BossesData.java.
     * Uses special_class from DTO to determine if generic Boss or special class.
     * 
     * Requirements: 1.1, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4
     * 
     * @param bossId The boss ID
     * @return The created boss instance, or null if data not found in SQL
     */
    public static Boss createBossFromDatabase(int bossId) {
        try {
            // 1. Load data from SQL - REQUIRED, no fallback
            BossData[] data = BossDataLoader.gI().getBossData(bossId);
            if (data == null || data.length == 0) {
                Logger.error("BossFactory: No SQL data for boss ID " + bossId + ". Boss will NOT be created.\n");
                return null;
            }
            
            // 2. Get DTO for special_class and settings
            jdbc.daos.dto.BossDataDTO dto = BossDataLoader.gI().getBossDTO(bossId);
            if (dto == null) {
                Logger.error("BossFactory: No DTO for boss ID " + bossId + ". Boss will NOT be created.\n");
                return null;
            }
            
            boolean isNotifyDisabled = dto.isNotifyDisabled();
            boolean isZone01SpawnDisabled = dto.isZone01SpawnDisabled();
            String specialClass = dto.getSpecialClass();
            
            // 3. Create boss based on special_class
            if (specialClass == null || specialClass.trim().isEmpty()) {
                // Generic boss - use base Boss class
                return new Boss(bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
            } else {
                // Special boss - instantiate special class with data from SQL
                return createSpecialBossFromSQL(specialClass, bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
            }
        } catch (Exception e) {
            // Logger.error("BossFactory: Failed to create boss " + bossId + ": " + e.getMessage() + "\n", e);
            return null;
        }
    }
    
    /**
     * Create a special boss from SQL data using reflection.
     * Tries constructor with (int, BossData[]) first, then no-arg constructor.
     * 
     * Requirements: 1.1, 2.4
     * 
     * @param className Full class name (e.g., "boss.boss_manifest.Black.BlackGoku")
     * @param bossId The boss ID
     * @param isNotifyDisabled Whether to disable spawn notifications
     * @param isZone01SpawnDisabled Whether to disable spawning in zone 01
     * @param data The boss data array from SQL
     * @return The created boss instance, or null if creation fails
     */
    private static Boss createSpecialBossFromSQL(String className, int bossId,
            boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData[] data) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Boss> bossClass = (Class<? extends Boss>) Class.forName(className);
            
            // Try constructor with (int, BossData[]) - preferred for SQL data
            try {
                Constructor<? extends Boss> ctor = bossClass.getConstructor(int.class, BossData[].class);
                Boss boss = ctor.newInstance(bossId, data);
                boss.isNotifyDisabled = isNotifyDisabled;
                boss.isZone01SpawnDisabled = isZone01SpawnDisabled;
                return boss;
            } catch (NoSuchMethodException e) {
                // Try next signature
            }
            
            // Try constructor with (int, boolean, boolean, BossData[])
            try {
                Constructor<? extends Boss> ctor = bossClass.getConstructor(
                    int.class, boolean.class, boolean.class, BossData[].class);
                return ctor.newInstance(bossId, isNotifyDisabled, isZone01SpawnDisabled, data);
            } catch (NoSuchMethodException e) {
                // Try next signature
            }
            
            // Try no-arg constructor (legacy - will use hardcoded data)
            try {
                Constructor<? extends Boss> ctor = bossClass.getConstructor();
                Boss boss = ctor.newInstance();
                boss.isNotifyDisabled = isNotifyDisabled;
                boss.isZone01SpawnDisabled = isZone01SpawnDisabled;
                return boss;
            } catch (NoSuchMethodException e) {
                // No matching constructor
            }
            
            Logger.error("BossFactory: No suitable constructor for " + className + "\n");
            return null;
            
        } catch (ClassNotFoundException e) {
            Logger.error("BossFactory: Class not found: " + className + "\n");
            return null;
        } catch (Exception e) {
            // Logger.error("BossFactory: Error creating " + className + ": " + e.getMessage() + "\n", e);
            return null;
        }
    }
    
    /**
     * Create multiple instances of a boss from database.
     * 
     * @param bossId The boss ID
     * @param count Number of instances to create
     * @return Array of created boss instances
     */
    public static Boss[] createBossesFromDatabase(int bossId, int count) {
        Boss[] bosses = new Boss[count];
        for (int i = 0; i < count; i++) {
            bosses[i] = createBossFromDatabase(bossId);
        }
        return bosses;
    }
}
