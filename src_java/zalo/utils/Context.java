/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import zalo.login.ZaloOptions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Context {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private int API_TYPE;
    private int API_VERSION;
    private Map<String, Object> uploadCallbacks = new ConcurrentHashMap<>();
    private ZaloOptions options;
    private String secretKey;
    private String imei;
    private Object cookie; 
    private String userAgent;
    private String language;
    private String uid;
    private Map<String, Object> settings;
    private Object extraVer; 
    private Map<String, Object> loginInfo;
    
    private static final long FIVE_MINUTES = 5 * 60 * 1000;
    
    public static Context createContext(int apiType, int apiVersion) {
        Context ctx = new Context();
        ctx.API_TYPE = apiType;
        ctx.API_VERSION = apiVersion;
        ctx.options = new ZaloOptions();
        ctx.options.setSelfListen(false);
        ctx.options.setCheckUpdate(true);
        ctx.options.setLogging(true);
        return ctx;
    }
    
    public int getAPI_TYPE() {
        return API_TYPE;
    }
    
    public void setAPI_TYPE(int API_TYPE) {
        this.API_TYPE = API_TYPE;
    }
    
    public int getAPI_VERSION() {
        return API_VERSION;
    }
    
    public void setAPI_VERSION(int API_VERSION) {
        this.API_VERSION = API_VERSION;
    }
    
    public Map<String, Object> getUploadCallbacks() {
        return uploadCallbacks;
    }
    
    public void setUploadCallbacks(Map<String, Object> uploadCallbacks) {
        this.uploadCallbacks = uploadCallbacks;
    }
    
    public ZaloOptions getOptions() {
        return options;
    }
    
    public void setOptions(ZaloOptions options) {
        this.options = options;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getImei() {
        return imei;
    }
    
    public void setImei(String imei) {
        this.imei = imei;
    }
    
    public Object getCookie() {
        return cookie;
    }
    
    public void setCookie(Object cookie) {
        this.cookie = cookie;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getUid() {
        return uid;
    }
    
    public void setUid(String uid) {
        this.uid = uid;
    }
    
    public Map<String, Object> getSettings() {
        return settings;
    }
    
    public void setSettings(Map<String, Object> settings) {
        this.settings = settings;
    }
    
    public Object getExtraVer() {
        return extraVer;
    }
    
    public void setExtraVer(Object extraVer) {
        this.extraVer = extraVer;
    }
    
    public Map<String, Object> getLoginInfo() {
        return loginInfo;
    }
    
    public void setLoginInfo(Map<String, Object> loginInfo) {
        this.loginInfo = loginInfo;
    }
    
    public static final int MAX_MESSAGES_PER_SEND = 50;
}

