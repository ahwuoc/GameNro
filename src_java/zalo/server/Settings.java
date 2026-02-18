/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.server;

import zalo.login.Credentials;
import zalo.login.ZaloOptions;
import zalo.utils.Json;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Settings {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static final String SETTINGS_DIR = "settings";
    private static final String PROPERTIES_FILE = SETTINGS_DIR + "/zalo.properties";
    private static final String COOKIE_FILE = SETTINGS_DIR + "/cookie.json";
    private static final String PROPERTIES_RESOURCE = "zalo.properties";
    
    private static Properties props = null;
    private static Object cookie = null;
    private static Credentials cachedCredentials = null;
    private static ZaloOptions cachedZaloOptions = null;
    
    static {
        loadProperties();
    }
    
    private static void loadProperties() {
        props = new Properties();
        try {
            File propsFile = new File(PROPERTIES_FILE);
            if (!propsFile.exists()) {
                InputStream is = Settings.class.getClassLoader().getResourceAsStream(PROPERTIES_RESOURCE);
                if (is != null) {
                    props.load(is);
                    is.close();
                } else {
                    System.err.println("[SETTINGS] Warning: " + PROPERTIES_FILE + " not found, using defaults");
                }
            } else {
                try (FileInputStream fis = new FileInputStream(propsFile)) {
                    props.load(fis);
                }
            }
        } catch (IOException e) {
            System.err.println("[SETTINGS] Error loading properties: " + e.getMessage());
        }
    }
    
    private static Object loadCookieFromFile() {
        if (cookie != null) {
            return cookie;
        }
        
        try {
            File cookieFile = new File(COOKIE_FILE);
            if (!cookieFile.exists()) {
                System.err.println("[SETTINGS] Warning: " + COOKIE_FILE + " not found");
                return null;
            }
            
            String cookieJson = new String(Files.readAllBytes(Paths.get(cookieFile.getAbsolutePath())));
            if (cookieJson == null || cookieJson.trim().isEmpty() || cookieJson.trim().equals("[]")) {
                System.err.println("[SETTINGS] Warning: cookie.json is empty");
                return null;
            }
            
            cookie = Json.parse(cookieJson);
            return cookie;
        } catch (Exception e) {
            System.err.println("[SETTINGS] Error loading cookie from file: " + e.getMessage());
            return null;
        }
    }
    
    public static String getImei() {
        return props.getProperty("imei", "");
    }
    
    public static Object getCookie() {
        return loadCookieFromFile();
    }
    
    public static String getUserAgent() {
        return props.getProperty("userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0");
    }
    
    public static String getLanguage() {
        return props.getProperty("language", "vi");
    }
    
    public static String getPrefix() {
        return props.getProperty("prefix", ".");
    }
    
    public static String getLoginId() {
        return props.getProperty("loginId", "default");
    }
    
    public static boolean isSelfListen() {
        return Boolean.parseBoolean(props.getProperty("selfListen", "true"));
    }
    
    public static boolean isLogging() {
        return Boolean.parseBoolean(props.getProperty("logging", "false"));
    }
    
    public static int getApiType() {
        return Integer.parseInt(props.getProperty("apiType", "30"));
    }
    
    public static int getApiVersion() {
        return Integer.parseInt(props.getProperty("apiVersion", "670"));
    }
    
    public static int getNroHttpPort() {
        return Integer.parseInt(props.getProperty("nroHttpPort", "8888"));
    }
    
    public static String getDatabaseUrlA() {
        String host = props.getProperty("db.host", "localhost");
        String port = props.getProperty("db.port", "3306");
        String dbName = props.getProperty("db.name.a", "a");
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
    }
    
    public static String getDatabaseUrlB() {
        String host = props.getProperty("db.host", "localhost");
        String port = props.getProperty("db.port", "3306");
        String dbName = props.getProperty("db.name.b", "b");
        return "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh";
    }
    
    public static String getDatabaseNameA() {
        return props.getProperty("db.name.a", "a");
    }
    
    public static String getDatabaseNameB() {
        return props.getProperty("db.name.b", "b");
    }
    
    public static String getDatabaseUsername() {
        return props.getProperty("db.username", "root");
    }
    
    public static String getDatabasePassword() {
        return props.getProperty("db.password", "");
    }
    
    public static void setCookie(Object cookie) {
        Settings.cookie = cookie;
    }
    
    public static void setCookie(List<Map<String, Object>> cookieList) {
        Settings.cookie = cookieList;
    }
    
    public static void setCookie(Map<String, Object> cookieMap) {
        Settings.cookie = cookieMap;
    }
    
    public static Credentials getCredentials() {
        if (cachedCredentials == null) {
            cachedCredentials = new Credentials();
            cachedCredentials.setImei(getImei());
            cachedCredentials.setCookie(getCookie());
            cachedCredentials.setUserAgent(getUserAgent());
            cachedCredentials.setLanguage(getLanguage());
        }
        return cachedCredentials;
    }
    
    public static ZaloOptions getZaloOptions() {
        if (cachedZaloOptions == null) {
            cachedZaloOptions = new ZaloOptions();
            cachedZaloOptions.setSelfListen(isSelfListen());
            cachedZaloOptions.setLogging(isLogging());
            cachedZaloOptions.setApiType(getApiType());
            cachedZaloOptions.setApiVersion(getApiVersion());
        }
        return cachedZaloOptions;
    }
    
    public static void clearCache() {
        cachedCredentials = null;
        cachedZaloOptions = null;
        cookie = null;
    }
}

