/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.apis;

import zalo.utils.Json;
import zalo.utils.Crypto;
import zalo.utils.Encryptos;
import zalo.utils.Url;
import zalo.services.HttpServices;
import zalo.consts.Constants;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import java.util.HashMap;
import java.util.Map;

public class LoginApi {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static final String LOGIN_INFO_URL = Constants.ZALO_CHAT_BASE + Constants.LOGIN_GET_LOGIN_INFO;
    private static final String SERVER_INFO_URL = Constants.ZALO_CHAT_BASE + Constants.LOGIN_GET_SERVER_INFO;
    
    public Map<String, Object> login(Context ctx, boolean encryptParams) {
        try {
            EncryptParamResult encryptedParams = getEncryptParam(ctx, encryptParams, "getlogininfo");
            
             Map<String, String> urlParams = new HashMap<>(encryptedParams.getParams());
            urlParams.put("nretry", "0");
            String url = Url.makeURL(ctx, LOGIN_INFO_URL, urlParams);
            
             HttpServices.RequestOptions options = new HttpServices.RequestOptions();
            options.setMethod("POST");
            String formData = buildFormData(encryptedParams.getParams());
            options.setBody(formData);
            
            HttpServices.HttpResponse response = HttpServices.request(ctx, url, options);
            
            if (!response.isOk()) {
                throw new ZaloApiError("Failed to fetch login info: " + response.getStatusCode());
            }
            
            String body = response.getBody();
            
            if (body == null || body.trim().isEmpty()) {
                throw new ZaloApiError("Empty response body from login API (status: " + response.getStatusCode() + ")");
            }
            
            try {
                Object parsed = Json.parse(body);
                if (!(parsed instanceof Map)) {
                    throw new ZaloApiError("Expected JSON object in response, got: " + parsed.getClass());
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) parsed;
            
             if (data.containsKey("error_code")) {
                Object errorCodeObj = data.get("error_code");
                int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                if (errorCode != 0) {
                    String errorMessage = data.containsKey("error_message") ? 
                        String.valueOf(data.get("error_message")) : "Unknown error";
                    String errorMessageLocalize = data.containsKey("error_message_localize") ?
                        String.valueOf(data.get("error_message_localize")) : null;
                    throw new ZaloApiError("Login API error: " + errorMessage + 
                        (errorMessageLocalize != null ? " (" + errorMessageLocalize + ")" : "") + 
                        " (code: " + errorCode + ")");
                }
            }
            
             if (encryptedParams.getEnk() != null && data.containsKey("data")) {
                Object dataObj = data.get("data");
                if (dataObj instanceof String) {
                    String encryptedData = (String) dataObj;
                    String decryptedData = Crypto.decryptResp(encryptedParams.getEnk(), encryptedData);
                    if (decryptedData != null) {
                        Object decryptedParsed = Json.parse(decryptedData);
                        if (decryptedParsed instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> decrypted = (Map<String, Object>) decryptedParsed;
                            if (decrypted != null && !decrypted.isEmpty()) {
                                return decrypted;
                            }
                        }
                    }
                }
            }
            
             if (data.containsKey("data")) {
                Object dataObj = data.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    if (dataMap != null && !dataMap.isEmpty()) {
                        return dataMap;
                    }
                }
            }
            
             return data;
            } catch (RuntimeException e) {
                 throw new ZaloApiError("Failed to parse login response: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new ZaloApiError("Login failed: " + e.getMessage(), e);
        }
    }
    
    public Map<String, Object> getServerInfo(Context ctx, boolean encryptParams) {
        try {
            EncryptParamResult encryptedParams = getEncryptParam(ctx, encryptParams, "getserverinfo");
            
            if (encryptedParams.getParams().get("signkey") == null) {
                throw new ZaloApiError("Missing signkey");
            }
            
             Map<String, String> params = new HashMap<>();
            params.put("imei", ctx.getImei());
            params.put("type", String.valueOf(ctx.getAPI_TYPE()));
            params.put("client_version", String.valueOf(ctx.getAPI_VERSION()));
            params.put("computer_name", "Web");
            params.put("signkey", encryptedParams.getParams().get("signkey"));
            
            String url = Url.makeURL(ctx, SERVER_INFO_URL, params, false);
            
             HttpServices.RequestOptions options = new HttpServices.RequestOptions();
            options.setMethod("GET");
            
            HttpServices.HttpResponse response = HttpServices.request(ctx, url, options);
            
            if (!response.isOk()) {
                throw new ZaloApiError("Failed to fetch server info: " + response.getStatusCode());
            }
            
            String body = response.getBody();
            if (body == null || body.trim().isEmpty()) {
                throw new ZaloApiError("Empty response body from server info API");
            }
            
            Object parsed = Json.parse(body);
            if (!(parsed instanceof Map)) {
                throw new ZaloApiError("Expected JSON object in server info response");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) parsed;
            
             if (data.containsKey("error_code")) {
                Object errorCodeObj = data.get("error_code");
                int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                if (errorCode != 0) {
                    String errorMessage = data.containsKey("error_message") ? 
                        String.valueOf(data.get("error_message")) : "Unknown error";
                    throw new ZaloApiError("Server info API error: " + errorMessage + " (code: " + errorCode + ")");
                }
            }
            
             if (data.containsKey("data")) {
                Object dataObj = data.get("data");
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    return dataMap;
                }
            }
            
             return data;
        } catch (Exception e) {
            throw new ZaloApiError("Failed to fetch server info: " + e.getMessage(), e);
        }
    }
    
    private String buildFormData(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            try {
                sb.append(java.net.URLEncoder.encode(entry.getKey(), "UTF-8"))
                  .append("=")
                  .append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (Exception e) {
                sb.append(entry.getKey()).append("=").append(entry.getValue());
            }
            first = false;
        }
        return sb.toString();
    }
    
    private EncryptParamResult getEncryptParam(Context ctx, boolean encryptParams, String type) {
        Map<String, String> params = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("computer_name", "Web");
        data.put("imei", ctx.getImei());
        data.put("language", ctx.getLanguage());
        data.put("ts", System.currentTimeMillis());
        
        EncryptDataResult encryptedData = encryptParam(ctx, data, encryptParams);
        
        if (encryptedData == null) {
            params.putAll(convertToStringMap(data));
        } else {
            params.putAll(encryptedData.getEncryptedParams());
            params.put("params", encryptedData.getEncryptedData());
        }
        
        params.put("type", String.valueOf(ctx.getAPI_TYPE()));
        params.put("client_version", String.valueOf(ctx.getAPI_VERSION()));
        
        if ("getserverinfo".equals(type)) {
            Map<String, String> signKeyParams = new HashMap<>();
            signKeyParams.put("imei", ctx.getImei());
            signKeyParams.put("type", String.valueOf(ctx.getAPI_TYPE()));
            signKeyParams.put("client_version", String.valueOf(ctx.getAPI_VERSION()));
            signKeyParams.put("computer_name", "Web");
            params.put("signkey", getSignKey(type, signKeyParams));
        } else {
            params.put("signkey", getSignKey(type, params));
        }
        
        EncryptParamResult result = new EncryptParamResult();
        result.setParams(params);
        result.setEnk(encryptedData != null ? encryptedData.getEnk() : null);
        return result;
    }
    
    private EncryptDataResult encryptParam(Context ctx, Map<String, Object> data, boolean encryptParams) {
        if (!encryptParams) {
            return null;
        }
        
        try {
            Encryptos encryptor = new Encryptos(
                ctx.getAPI_TYPE(),
                (String) data.get("imei"),
                System.currentTimeMillis()
            );
            
            String stringifiedData = Json.stringify(data);
            String encryptedKey = encryptor.getEncryptKey();
            String encodedData = Encryptos.encodeAES(encryptedKey, stringifiedData, "base64", false);
            Encryptos.EncryptParams params = encryptor.getParams();
            
            if (params == null) {
                return null;
            }
            
            EncryptDataResult result = new EncryptDataResult();
            result.setEncryptedData(encodedData);
            result.setEncryptedParams(convertToStringMap(params));
            result.setEnk(encryptedKey);
            return result;
        } catch (Exception e) {
            throw new ZaloApiError("Failed to encrypt params: " + e.getMessage(), e);
        }
    }
    
    private String getSignKey(String type, Map<String, String> params) {
        return Crypto.getSignKey(type, params);
    }
    
    private Map<String, String> convertToStringMap(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return result;
    }
    
    private Map<String, String> convertToStringMap(Encryptos.EncryptParams params) {
        Map<String, String> result = new HashMap<>();
        result.put("zcid", params.getZcid());
        result.put("zcid_ext", params.getZcidExt());
        result.put("enc_ver", params.getEncVer());
        return result;
    }
    
    
    private static class EncryptParamResult {
        private Map<String, String> params;
        private String enk;
        
        public Map<String, String> getParams() {
            return params;
        }
        
        public void setParams(Map<String, String> params) {
            this.params = params;
        }
        
        public String getEnk() {
            return enk;
        }
        
        public void setEnk(String enk) {
            this.enk = enk;
        }
    }
    
    private static class EncryptDataResult {
        private String encryptedData;
        private Map<String, String> encryptedParams;
        private String enk;
        
        public String getEncryptedData() {
            return encryptedData;
        }
        
        public void setEncryptedData(String encryptedData) {
            this.encryptedData = encryptedData;
        }
        
        public Map<String, String> getEncryptedParams() {
            return encryptedParams;
        }
        
        public void setEncryptedParams(Map<String, String> encryptedParams) {
            this.encryptedParams = encryptedParams;
        }
        
        public String getEnk() {
            return enk;
        }
        
        public void setEnk(String enk) {
            this.enk = enk;
        }
    }
}

