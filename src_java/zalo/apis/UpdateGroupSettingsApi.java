/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.apis;

import zalo.utils.Json;
import zalo.utils.Crypto;
import zalo.utils.Url;
import zalo.services.HttpServices;
import zalo.utils.Apis;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class UpdateGroupSettingsApi {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private Context ctx;
    private Apis api;
    
    public UpdateGroupSettingsApi(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
    }
    
    public CompletableFuture<String> updateGroupSettings(Map<String, Object> options, String groupId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (groupId == null || groupId.isEmpty()) {
                    throw new ZaloApiError("Missing group id");
                }
                
                Map<String, Object> serviceMap = api.getZpwServiceMap();
                String baseUrl;
                Object groupObj = serviceMap.get("group");
                if (groupObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> groupServices = (List<String>) groupObj;
                    baseUrl = groupServices.get(0) + "/api/group/setting/update";
                } else if (groupObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> groupMap = (Map<String, Object>) groupObj;
                    Object group0Obj = groupMap.get("0");
                    if (group0Obj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> groupServices = (List<String>) group0Obj;
                        baseUrl = groupServices.get(0) + "/api/group/setting/update";
                    } else {
                        throw new ZaloApiError("Invalid group services format");
                    }
                } else {
                    throw new ZaloApiError("Invalid group services format");
                }
                
                Map<String, Object> params = new HashMap<>();
                params.put("blockName", options.containsKey("blockName") && Boolean.TRUE.equals(options.get("blockName")) ? 1 : 0);
                params.put("signAdminMsg", options.containsKey("signAdminMsg") && Boolean.TRUE.equals(options.get("signAdminMsg")) ? 1 : 0);
                params.put("setTopicOnly", options.containsKey("setTopicOnly") && Boolean.TRUE.equals(options.get("setTopicOnly")) ? 1 : 0);
                params.put("enableMsgHistory", options.containsKey("enableMsgHistory") && Boolean.TRUE.equals(options.get("enableMsgHistory")) ? 1 : 0);
                params.put("joinAppr", options.containsKey("joinAppr") && Boolean.TRUE.equals(options.get("joinAppr")) ? 1 : 0);
                params.put("lockCreatePost", options.containsKey("lockCreatePost") && Boolean.TRUE.equals(options.get("lockCreatePost")) ? 1 : 0);
                params.put("lockCreatePoll", options.containsKey("lockCreatePoll") && Boolean.TRUE.equals(options.get("lockCreatePoll")) ? 1 : 0);
                params.put("lockSendMsg", options.containsKey("lockSendMsg") && Boolean.TRUE.equals(options.get("lockSendMsg")) ? 1 : 0);
                params.put("lockViewMember", options.containsKey("lockViewMember") && Boolean.TRUE.equals(options.get("lockViewMember")) ? 1 : 0);
                params.put("bannFeature", 0);
                params.put("dirtyMedia", 0);
                params.put("banDuration", 0);
                params.put("blocked_members", new ArrayList<>());
                params.put("grid", groupId);
                params.put("imei", ctx.getImei());
                
                String encryptedParams = Crypto.encodeAES(ctx.getSecretKey(), Json.stringify(params));
                if (encryptedParams == null) {
                    throw new ZaloApiError("Failed to encrypt params");
                }
                
                Map<String, String> urlParams = new HashMap<>();
                urlParams.put("params", encryptedParams);
                String url = Url.makeURL(ctx, baseUrl, urlParams);
                
                HttpServices.RequestOptions requestOptions = new HttpServices.RequestOptions();
                requestOptions.setMethod("GET");
                
                HttpServices.HttpResponse response = HttpServices.request(ctx, url, requestOptions);
                
                if (!response.isOk()) {
                    throw new ZaloApiError("Request failed with status code " + response.getStatusCode());
                }
                
                String responseBody = response.getBody();
                if (responseBody == null || responseBody.isEmpty()) {
                    return "";
                }
                
                Object parsed = Json.parse(responseBody);
                if (!(parsed instanceof Map)) {
                    return "";
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonData = (Map<String, Object>) parsed;
                
                if (jsonData.containsKey("error_code")) {
                    Object errorCodeObj = jsonData.get("error_code");
                    int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                    if (errorCode != 0) {
                        String errorMessage = jsonData.containsKey("error_message") ? 
                            String.valueOf(jsonData.get("error_message")) : "Unknown error";
                        throw new ZaloApiError(errorMessage, errorCode);
                    }
                }
                
                return "";
            } catch (Exception e) {
                throw new ZaloApiError("Failed to update group settings: " + e.getMessage(), e);
            }
        });
    }
}

