package boss;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry for special boss classes that have custom behavior.
 * Special bosses override methods like injured(), reward(), attack() etc.
 * Generic bosses use the base Boss class with data from database.
 * 
 * Requirements: 3.1, 3.2
 */
public class SpecialBossRegistry {
    
    private static final Map<Integer, Class<? extends Boss>> registry = new HashMap<>();
    private static final Map<Integer, String> classNameRegistry = new HashMap<>();
    private static boolean initialized = false;
    
    /**
     * Register a special boss class for a boss ID.
     * 
     * @param bossId The boss ID
     * @param bossClass The class to instantiate for this boss
     */
    public static void register(int bossId, Class<? extends Boss> bossClass) {
        registry.put(bossId, bossClass);
        classNameRegistry.put(bossId, bossClass.getName());
    }
    
    /**
     * Register a special boss by class name (for database-driven registration).
     * 
     * @param bossId The boss ID
     * @param className Full class name (e.g., "boss.boss_manifest.Black.BlackGoku")
     */
    public static void registerByClassName(int bossId, String className) {
        if (className == null || className.isEmpty()) {
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Boss> bossClass = (Class<? extends Boss>) Class.forName(className);
            registry.put(bossId, bossClass);
            classNameRegistry.put(bossId, className);
        } catch (ClassNotFoundException e) {
            utils.Logger.error("SpecialBossRegistry: Class not found: " + className + "\n");
        }
    }
    
    /**
     * Get the registered class for a boss ID.
     * 
     * @param bossId The boss ID
     * @return The registered class, or null if not registered
     */
    public static Class<? extends Boss> getClass(int bossId) {
        return registry.get(bossId);
    }
    
    /**
     * Get the registered class name for a boss ID.
     * 
     * @param bossId The boss ID
     * @return The class name, or null if not registered
     */
    public static String getClassName(int bossId) {
        return classNameRegistry.get(bossId);
    }
    
    /**
     * Check if a boss ID has a special class registered.
     * 
     * @param bossId The boss ID
     * @return true if a special class is registered
     */
    public static boolean hasSpecialClass(int bossId) {
        return registry.containsKey(bossId);
    }
    
    /**
     * Get all registered boss IDs.
     * 
     * @return Set of registered boss IDs
     */
    public static Set<Integer> getRegisteredBossIds() {
        return registry.keySet();
    }
    
    /**
     * Get the number of registered special bosses.
     * 
     * @return Number of registered bosses
     */
    public static int getRegistrySize() {
        return registry.size();
    }
    
    /**
     * Clear all registrations (useful for testing or reload).
     */
    public static void clear() {
        registry.clear();
        classNameRegistry.clear();
        initialized = false;
    }
    
    /**
     * Check if registry has been initialized.
     * 
     * @return true if init() has been called
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Initialize the registry with all known special bosses.
     * This should be called during server startup.
     * 
     * Requirements: 3.1
     */
    public static void init() {
        if (initialized) {
            return;
        }
        
        // ======================== NAPPA ========================
        register(BossID.KUKU, boss.boss_manifest.Nappa.Kuku.class);
        register(BossID.MAP_DAU_DINH, boss.boss_manifest.Nappa.MapDauDinh.class);
        register(BossID.RAMBO, boss.boss_manifest.Nappa.Rambo.class);
        
        // ======================== ANDROID ========================
        register(BossID.ANDROID_19, boss.boss_manifest.Android.Android19.class);
        register(BossID.DR_KORE, boss.boss_manifest.Android.DrKore.class);
        register(BossID.ANDROID_13, boss.boss_manifest.Android.Android13.class);
        register(BossID.ANDROID_14, boss.boss_manifest.Android.Android14.class);
        register(BossID.ANDROID_15, boss.boss_manifest.Android.Android15.class);
        register(BossID.PIC, boss.boss_manifest.Android.Pic.class);
        register(BossID.POC, boss.boss_manifest.Android.Poc.class);
        register(BossID.KING_KONG, boss.boss_manifest.Android.KingKong.class);
        
        // ======================== CELL ========================
        register(BossID.XEN_BO_HUNG, boss.boss_manifest.Cell.XenBoHung.class);
        register(BossID.SIEU_BO_HUNG, boss.boss_manifest.Cell.SieuBoHung.class);
        register(BossID.XEN_CON_1, boss.boss_manifest.Cell.XENCON1.class);
        register(BossID.XEN_CON_2, boss.boss_manifest.Cell.XENCON2.class);
        register(BossID.XEN_CON_3, boss.boss_manifest.Cell.XENCON3.class);
        register(BossID.XEN_CON_4, boss.boss_manifest.Cell.XENCON4.class);
        register(BossID.XEN_CON_5, boss.boss_manifest.Cell.XENCON5.class);
        register(BossID.XEN_CON_6, boss.boss_manifest.Cell.XENCON6.class);
        register(BossID.XEN_CON_7, boss.boss_manifest.Cell.XENCON7.class);
        
        // ======================== FRIEZA ========================
        register(BossID.FIDE, boss.boss_manifest.Frieza.Fide.class);
        
        // ======================== COOLER ========================
        register(BossID.COOLER, boss.boss_manifest.Cooler.Cooler.class);
        
        // ======================== BLACK GOKU ========================
        register(BossID.BLACK_GOKU, boss.boss_manifest.Black.BlackGoku.class);
        
        // ======================== GOLDEN FRIEZA ========================
        register(BossID.GOLDEN_FRIEZA, boss.boss_manifest.GoldenFrieza.GoldenFrieza.class);
        register(BossID.DEATH_BEAM_1, boss.boss_manifest.GoldenFrieza.DeathBeam1.class);
        register(BossID.DEATH_BEAM_2, boss.boss_manifest.GoldenFrieza.DeathBeam2.class);
        register(BossID.DEATH_BEAM_3, boss.boss_manifest.GoldenFrieza.DeathBeam3.class);
        register(BossID.DEATH_BEAM_4, boss.boss_manifest.GoldenFrieza.DeathBeam4.class);
        register(BossID.DEATH_BEAM_5, boss.boss_manifest.GoldenFrieza.DeathBeam5.class);
        
        // ======================== GINYU FORCE ========================
        register(BossID.SO_4, boss.boss_manifest.GinyuForce.SO4.class);
        register(BossID.SO_3, boss.boss_manifest.GinyuForce.SO3.class);
        register(BossID.SO_2, boss.boss_manifest.GinyuForce.SO2.class);
        register(BossID.SO_1, boss.boss_manifest.GinyuForce.SO1.class);
        register(BossID.TIEU_DOI_TRUONG, boss.boss_manifest.GinyuForce.TDT.class);
        
        // ======================== NAMEK GINYU FORCE ========================
        register(BossID.SO_4_NM, boss.boss_manifest.NamekGinyuForce.SO4_NM.class);
        register(BossID.SO_3_NM, boss.boss_manifest.NamekGinyuForce.SO3_NM.class);
        register(BossID.SO_2_NM, boss.boss_manifest.NamekGinyuForce.SO2_NM.class);
        register(BossID.SO_1_NM, boss.boss_manifest.NamekGinyuForce.SO1_NM.class);
        register(BossID.TIEU_DOI_TRUONG_NM, boss.boss_manifest.NamekGinyuForce.TDT_NM.class);
        
        // ======================== EARTH ========================
        register(BossID.BUJIN, boss.boss_manifest.Earth.BUJIN.class);
        register(BossID.KOGU, boss.boss_manifest.Earth.KOGU.class);
        register(BossID.ZANGYA, boss.boss_manifest.Earth.ZANGYA.class);
        register(BossID.BIDO, boss.boss_manifest.Earth.BIDO.class);
        register(BossID.BOJACK, boss.boss_manifest.Earth.BOJACK.class);
        register(BossID.SUPER_BOJACK, boss.boss_manifest.Earth.SUPER_BOJACK.class);
        
        // ======================== MAJIN BUU 12H ========================
        register(BossID.DRABURA, boss.boss_manifest.MajinBuu12H.Drabura.class);
        register(BossID.BUI_BUI, boss.boss_manifest.MajinBuu12H.BuiBui.class);
        register(BossID.BUI_BUI_2, boss.boss_manifest.MajinBuu12H.BuiBui2.class);
        register(BossID.YA_CON, boss.boss_manifest.MajinBuu12H.Yacon.class);
        register(BossID.MABU_12H, boss.boss_manifest.MajinBuu12H.Mabu.class);
        register(BossID.DRABURA_2, boss.boss_manifest.MajinBuu12H.Drabura2.class);
        register(BossID.DRABURA_3, boss.boss_manifest.MajinBuu12H.Drabura3.class);
        register(BossID.GOKU, boss.boss_manifest.MajinBuu12H.Goku.class);
        register(BossID.CADIC, boss.boss_manifest.MajinBuu12H.Cadic.class);
        
        // ======================== MAJIN BUU 14H ========================
        register(BossID.MABU, boss.boss_manifest.MajinBuu14H.Mabu2H.class);
        register(BossID.SUPERBU, boss.boss_manifest.MajinBuu14H.SuperBu.class);
        
        // ======================== BROLY ========================
        register(BossID.BROLY, boss.boss_manifest.Broly.Broly.class);
        register(BossID.SUPERBUVIP, boss.boss_manifest.Broly.SuperBuVip.class);
        register(BossID.BROLYSUPERVIP, boss.boss_manifest.Broly.BrolySuperVip.class);
        
        // ======================== TAO PAI PAI ========================
        register(BossID.TAU_PAY_PAY_DONG_NAM_KARIN, boss.boss_manifest.TaoPaiPai.TaoPaiPai.class);
        
        // ======================== TRUNG THU EVENT ========================
        register(BossID.KHIDOT, boss.boss_manifest.TrungThuEvent.KhiDot.class);
        register(BossID.NGUYETTHAN, boss.boss_manifest.TrungThuEvent.NguyetThan.class);
        register(BossID.NHATTHAN, boss.boss_manifest.TrungThuEvent.NhatThan.class);
        
        // ======================== HALLOWEEN EVENT ========================
        register(BossID.MATROI, boss.boss_manifest.HalloweenEvent.MaTroi.class);
        register(BossID.DOI, boss.boss_manifest.HalloweenEvent.Doi.class);
        register(BossID.BIMA, boss.boss_manifest.HalloweenEvent.BiMa.class);
        
        // ======================== CHRISTMAS EVENT ========================
        register(BossID.ONG_GIA_NOEL, boss.boss_manifest.ChristmasEvent.OngGiaNoel.class);
        
        // ======================== HUNG VUONG EVENT ========================
        register(BossID.SON_TINH, boss.boss_manifest.HungVuongEvent.SonTinh.class);
        register(BossID.THUY_TINH, boss.boss_manifest.HungVuongEvent.ThuyTinh.class);
        
        // ======================== TET EVENT ========================
        register(BossID.LAN_CON, boss.boss_manifest.LunarNewYearEvent.LanCon.class);
        
        // ======================== YARDART ========================
        register(BossID.TAP_SU_0, boss.boss_manifest.Yardart.TAPSU0.class);
        register(BossID.TAP_SU_1, boss.boss_manifest.Yardart.TAPSU1.class);
        register(BossID.TAP_SU_2, boss.boss_manifest.Yardart.TAPSU2.class);
        register(BossID.TAP_SU_3, boss.boss_manifest.Yardart.TAPSU3.class);
        register(BossID.TAP_SU_4, boss.boss_manifest.Yardart.TAPSU4.class);
        register(BossID.TAN_BINH_0, boss.boss_manifest.Yardart.TANBINH0.class);
        register(BossID.TAN_BINH_1, boss.boss_manifest.Yardart.TANBINH1.class);
        register(BossID.TAN_BINH_2, boss.boss_manifest.Yardart.TANBINH2.class);
        register(BossID.TAN_BINH_3, boss.boss_manifest.Yardart.TANBINH3.class);
        register(BossID.TAN_BINH_4, boss.boss_manifest.Yardart.TANBINH4.class);
        register(BossID.TAN_BINH_5, boss.boss_manifest.Yardart.TANBINH5.class);
        register(BossID.CHIEN_BINH_0, boss.boss_manifest.Yardart.CHIENBINH0.class);
        register(BossID.CHIEN_BINH_1, boss.boss_manifest.Yardart.CHIENBINH1.class);
        register(BossID.CHIEN_BINH_2, boss.boss_manifest.Yardart.CHIENBINH2.class);
        register(BossID.CHIEN_BINH_3, boss.boss_manifest.Yardart.CHIENBINH3.class);
        register(BossID.CHIEN_BINH_4, boss.boss_manifest.Yardart.CHIENBINH4.class);
        register(BossID.CHIEN_BINH_5, boss.boss_manifest.Yardart.CHIENBINH5.class);
        register(BossID.DOI_TRUONG_5, boss.boss_manifest.Yardart.DOITRUONG5.class);
        
        // ======================== BAC CON SOI ========================
        register(BossID.BACONSOi, boss.boss_manifest.BacConSoi.BaConSoi.class);
        
        // ======================== BOSS PHU ========================
        register(BossID.AN_TROM, boss.boss_manifest.BossPhu.AnTrom.class);
        register(BossID.AN_TROM_TV, boss.boss_manifest.BossPhu.AnTromTV.class);
        register(BossID.O_DO1, boss.boss_manifest.BossPhu.O_DO1.class);
        register(BossID.SOI_HEC_QUEN, boss.boss_manifest.BossPhu.SOI_HEC_QUEN.class);
        register(BossID.XINBATO1, boss.boss_manifest.BossPhu.XINBATO1.class);
        
        // ======================== CUMBER ========================
        register(BossID.CUMBER, boss.boss_manifest.Cumber.Cumber.class);
        
        // ======================== BOSS THE GIOI ========================
        // REMOVED: boss_world_1 and boss_world_2 now use generic Boss class
        // Data is loaded from SQL with damageCap, eventPoints, and rewards configuration
        // Requirements: 4.1, 4.2
        
        // ======================== ZAMASU ========================
        register(BossID.ZAMASU, boss.boss_manifest.zamasu.Zamasu.class);
        register(BossID.GOKUBLACK, boss.boss_manifest.zamasu.GokuBlack.class);
        
        // ======================== THIEN SU ========================
        register(BossID.BILL, boss.boss_manifest.ThienSu.Bill.class);
        register(BossID.WHISS, boss.boss_manifest.ThienSu.WHISS.class);
        
        initialized = true;
        utils.Logger.log("SpecialBossRegistry: Initialized with " + registry.size() + " special bosses\n");
    }
}
