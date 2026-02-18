/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.apis;

import zalo.utils.Json;
import zalo.utils.Reponse;
import zalo.utils.Crypto;
import zalo.utils.Url;
import zalo.services.HttpServices;
import zalo.utils.Apis;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import zalo.models.ThreadType;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DeleteMessageApi {
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
    
    public DeleteMessageApi(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
    }
    
    public CompletableFuture<Map<String, Object>> deleteMessage(DeleteMessageDestination destination, boolean onlyMe) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (destination == null) {
                    throw new ZaloApiError("Missing destination");
                }
                
                ThreadType type = ThreadType.fromValue(destination.getType());
                boolean isGroup = type == ThreadType.GROUP;
                boolean isSelf = ctx.getUid().equals(destination.getData().get("uidFrom"));
                
                if (isSelf && !onlyMe) {
                    throw new ZaloApiError("To delete your message for everyone, use undo api instead");
                }
                if (!isGroup && !onlyMe) {
                    throw new ZaloApiError("Can't delete message for everyone in a private chat");
                }
                
                Map<String, Object> serviceMap = api.getZpwServiceMap();
                String baseUrl;
                if (isGroup) {
                    @SuppressWarnings("unchecked")
                    List<String> groupServices = (List<String>) ((Map<String, Object>) serviceMap.get("group")).get("0");
                    baseUrl = groupServices.get(0) + "/api/group/deletemsg";
                } else {
                    @SuppressWarnings("unchecked")
                    List<String> chatServices = (List<String>) ((Map<String, Object>) serviceMap.get("chat")).get("0");
                    baseUrl = chatServices.get(0) + "/api/message/delete";
                }
                String url = Url.makeURL(ctx, baseUrl, new HashMap<>());
                
                Map<String, Object> params = new HashMap<>();
                if (isGroup) {
                    params.put("grid", destination.getThreadId());
                } else {
                    params.put("toid", destination.getThreadId());
                    params.put("imei", ctx.getImei());
                }
                params.put("cliMsgId", System.currentTimeMillis());
                
                List<Map<String, Object>> msgs = new ArrayList<>();
                Map<String, Object> msg = new HashMap<>();
                msg.put("cliMsgId", destination.getData().get("cliMsgId"));
                msg.put("globalMsgId", destination.getData().get("msgId"));
                msg.put("ownerId", destination.getData().get("uidFrom"));
                msg.put("destId", destination.getThreadId());
                msgs.add(msg);
                params.put("msgs", msgs);
                params.put("onlyMe", onlyMe ? 1 : 0);
                
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
                
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) 
                    Reponse.resolveResponse(ctx, response, null, true);
                
                return result;
            } catch (Exception e) {
                throw new ZaloApiError("Failed to delete message: " + e.getMessage(), e);
            }
        });
    }
    
    public static class DeleteMessageDestination {
        private int type;
        private String threadId;
        private Map<String, Object> data;
        
        public int getType() {
            return type;
        }
        
        public void setType(int type) {
            this.type = type;
        }
        
        public String getThreadId() {
            return threadId;
        }
        
        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }
        
        public Map<String, Object> getData() {
            return data;
        }
        
        public void setData(Map<String, Object> data) {
            this.data = data;
        }
    }
}

