/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.login;

import zalo.utils.Apis;
import zalo.utils.Context;
import zalo.utils.ContextSession;
import zalo.utils.ZaloApiError;
import zalo.utils.Cookie;
import zalo.utils.Logger;
import zalo.apis.LoginApi;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Zalo {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private ZaloOptions options;
    
    public Zalo() {
        this.options = new ZaloOptions();
    }
    
    public Zalo(ZaloOptions options) {
        this.options = options != null ? options : new ZaloOptions();
    }
    
    public CompletableFuture<Apis> login(Credentials credentials) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (options == null) {
                    options = new ZaloOptions();
                }
                
                Context ctx = Context.createContext(
                    options.getApiType() != null ? options.getApiType() : 30,
                    options.getApiVersion() != null ? options.getApiVersion() : 670
                );
                ctx.setOptions(options);
                return loginCookie(ctx, credentials);
            } catch (Exception e) {
                throw new ZaloApiError("Login failed: " + e.getMessage(), e);
            }
        });
    }
    
    private Apis loginCookie(Context ctx, Credentials credentials) throws ZaloApiError {
        validateParams(credentials);
        
        ctx.setImei(credentials.getImei());
        ctx.setCookie(Cookie.parseCookies(credentials.getCookie()));
        ctx.setUserAgent(credentials.getUserAgent());
        ctx.setLanguage(credentials.getLanguage() != null ? credentials.getLanguage() : "vi");
         
        LoginApi loginApi = new LoginApi();
        Map<String, Object> loginInfo = null;
        try {
            loginInfo = loginApi.login(ctx, true);
        } catch (ZaloApiError e) {
            if (e.getMessage().contains("Invalid encryption protocol") || 
                e.getMessage().contains("18060")) {
                loginInfo = loginApi.login(ctx, false);
            } else {
                throw e;
            }
        }
        
        if (loginInfo == null || loginInfo.isEmpty()) {
            throw new ZaloApiError("Login failed: Empty login info");
        }
        
        Map<String, Object> serverInfo = loginApi.getServerInfo(ctx, false);
        
        if (serverInfo == null || serverInfo.isEmpty()) {
            throw new ZaloApiError("Login failed: Empty server info");
        }
        
        String secretKey = (String) loginInfo.get("zpw_enk");
        @SuppressWarnings("unchecked")
        Map<String, Object> zpwServiceMap = (Map<String, Object>) loginInfo.get("zpw_service_map_v3");
        Object wsUrls = loginInfo.get("zpw_ws");
        
        if (zpwServiceMap == null || secretKey == null) {
            throw new ZaloApiError("Login failed: Missing required login info (zpw_service_map_v3 or zpw_enk)");
        }
        
        ctx.setSecretKey(secretKey);
        ctx.setUid((String) loginInfo.get("uid"));
        Map<String, Object> serverData = serverInfo;
        if (serverInfo.containsKey("data") && serverInfo.get("data") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataObj = (Map<String, Object>) serverInfo.get("data");
            serverData = dataObj;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> settings = (Map<String, Object>) serverData.get("setttings");
        if (settings == null) {
            settings = (Map<String, Object>) serverData.get("settings");
        }
        ctx.setSettings(settings);
        ctx.setExtraVer(serverData.get("extra_ver"));
        ctx.setLoginInfo(loginInfo);
        
        if (!ContextSession.isContextSession(ctx)) {
            throw new ZaloApiError("Khởi tạo ngữ cảnh thất bại.");
        }
        
        Apis api = new Apis(ctx, zpwServiceMap, wsUrls);
        
        Logger logger = new Logger(ctx);
        if (options.isLogging()) {
            logger.info("Login successful. UID: " + ctx.getUid());
        }
        
        return api;
    }
    
    private void validateParams(Credentials credentials) {
        if (credentials == null) {
            throw new ZaloApiError("Credentials are required");
        }
        if (credentials.getImei() == null || credentials.getImei().isEmpty()) {
            throw new ZaloApiError("IMEI is required");
        }
        if (credentials.getCookie() == null) {
            throw new ZaloApiError("Cookie is required");
        }
        if (credentials.getUserAgent() == null || credentials.getUserAgent().isEmpty()) {
            throw new ZaloApiError("User agent is required");
        }
    }
}
