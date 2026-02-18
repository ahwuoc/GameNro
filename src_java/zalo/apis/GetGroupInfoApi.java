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

public class GetGroupInfoApi {
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
    
    public GetGroupInfoApi(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
    }
    
    public CompletableFuture<Map<String, Object>> getGroupInfo(Object groupId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> groupIdList = new ArrayList<>();
                if (groupId instanceof String) {
                    groupIdList.add((String) groupId);
                } else if (groupId instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) groupId;
                    for (Object id : list) {
                        groupIdList.add(String.valueOf(id));
                    }
                } else if (groupId instanceof Object[]) {
                    for (Object id : (Object[]) groupId) {
                        groupIdList.add(String.valueOf(id));
                    }
                } else {
                    groupIdList.add(String.valueOf(groupId));
                }
                
                Map<String, Object> serviceMap = api.getZpwServiceMap();
                String baseUrl;
                Object groupObj = serviceMap.get("group");
                if (groupObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> groupServices = (List<String>) groupObj;
                    baseUrl = groupServices.get(0) + "/api/group/getmg-v2";
                } else if (groupObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> groupMap = (Map<String, Object>) groupObj;
                    Object group0Obj = groupMap.get("0");
                    if (group0Obj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> groupServices = (List<String>) group0Obj;
                        baseUrl = groupServices.get(0) + "/api/group/getmg-v2";
                    } else {
                        throw new ZaloApiError("Invalid group services format");
                    }
                } else {
                    throw new ZaloApiError("Invalid group services format");
                }
                String url = Url.makeURL(ctx, baseUrl, new HashMap<>());
                Map<String, Integer> gridVerMap = new HashMap<>();
                for (String id : groupIdList) {
                    gridVerMap.put(id, 0);
                }
                
                Map<String, Object> params = new HashMap<>();
                Map<String, Object> gridVerMapObj = new HashMap<>();
                for (Map.Entry<String, Integer> entry : gridVerMap.entrySet()) {
                    gridVerMapObj.put(entry.getKey(), entry.getValue());
                }
                params.put("gridVerMap", Json.stringify(gridVerMapObj));
                
                String encryptedParams = Crypto.encodeAES(ctx.getSecretKey(),
                    Json.stringify(params)
                );
                
                if (encryptedParams == null) {
                    throw new ZaloApiError("Failed to encrypt message");
                }
                
                HttpServices.RequestOptions options = new HttpServices.RequestOptions();
                options.setMethod("POST");
                options.setBody("params=" + java.net.URLEncoder.encode(encryptedParams, "UTF-8"));
                
                HttpServices.HttpResponse response = HttpServices.request(ctx, url, options);
                
                if (!response.isOk()) {
                    throw new ZaloApiError("Request failed with status code " + response.getStatusCode());
                }
                
                String responseBody = response.getBody();
                if (responseBody == null || responseBody.isEmpty()) {
                    throw new ZaloApiError("Empty response body");
                }
                
                // Parse response body thành JSON (theo JS: await response.json())
                Object parsed = Json.parse(responseBody);
                if (!(parsed instanceof Map)) {
                    throw new ZaloApiError("Expected JSON object in response");
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonData = (Map<String, Object>) parsed;
                
                // Kiểm tra error_code (theo JS: if (jsonData.error_code != 0))
                if (jsonData.containsKey("error_code")) {
                    Object errorCodeObj = jsonData.get("error_code");
                    int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                    if (errorCode != 0) {
                        String errorMessage = jsonData.containsKey("error_message") ? 
                            String.valueOf(jsonData.get("error_message")) : "Unknown error";
                        throw new ZaloApiError(errorMessage, errorCode);
                    }
                }
                
                // Decrypt data field (theo JS: JSON.parse(decodeAES(ctx.secretKey, jsonData.data)))
                if (!jsonData.containsKey("data") || ctx.getSecretKey() == null) {
                    throw new ZaloApiError("Missing data field or secret key");
                }
                
                Object dataObj = jsonData.get("data");
                if (!(dataObj instanceof String)) {
                    throw new ZaloApiError("Data field is not a string");
                }
                
                String encryptedData = (String) dataObj;
                 String decrypted = Crypto.decodeAES(ctx.getSecretKey(), encryptedData);
                
                if (decrypted == null || decrypted.isEmpty()) {
                    throw new ZaloApiError("Failed to decrypt data field");
                }
                
                 Object decryptedParsed = Json.parse(decrypted);
                
                if (!(decryptedParsed instanceof Map)) {
                    throw new ZaloApiError("Decrypted data is not a JSON object");
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> decodedData = (Map<String, Object>) decryptedParsed;
                
                // Kiểm tra error_code trong decoded data (theo JS: if (decodedData.error_code != 0))
                if (decodedData.containsKey("error_code")) {
                    Object errorCodeObj = decodedData.get("error_code");
                    int errorCode = errorCodeObj instanceof Number ? ((Number) errorCodeObj).intValue() : 0;
                    if (errorCode != 0) {
                        String errorMessage = decodedData.containsKey("error_message") ? 
                            String.valueOf(decodedData.get("error_message")) : "Unknown error";
                        throw new ZaloApiError(errorMessage, errorCode);
                    }
                }
                
                // Return decodedData.data (theo JS: return result.data)
                if (decodedData.containsKey("data")) {
                    Object dataValue = decodedData.get("data");
                    if (dataValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) dataValue;
                        return result;
                    } else {
                        Map<String, Object> result = new HashMap<>();
                        result.put("data", dataValue);
                        return result;
                    }
                } else {
                    // Nếu không có data field, trả về toàn bộ decodedData
                    return decodedData;
                }
            } catch (Exception e) {
                throw new ZaloApiError("Failed to get group info: " + e.getMessage(), e);
            }
        });
    }
}

