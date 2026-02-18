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
import zalo.models.Reactions;
import zalo.models.ThreadType;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AddReactionApi {
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
    
    public AddReactionApi(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
    }
    
    public CompletableFuture<Map<String, Object>> addReaction(Object icon, ReactionDestination destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Integer rType = null;
                Integer source = null;
                String rIcon = null;
                
                if (icon instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> iconMap = (Map<String, Object>) icon;
                    rType = ((Number) iconMap.get("rType")).intValue();
                    source = ((Number) iconMap.get("source")).intValue();
                    Object iconObj = iconMap.get("icon");
                    rIcon = iconObj != null ? String.valueOf(iconObj) : null;
                } else if (icon instanceof Reactions) {
                    Reactions reaction = (Reactions) icon;
                    ReactionInfo info = getReactionInfo(reaction);
                    rType = info.rType;
                    source = info.source;
                    rIcon = reaction.getValue();
                } else {
                    throw new ZaloApiError("Invalid reaction");
                }
                
                if (rType == null || source == null || rIcon == null) {
                    throw new ZaloApiError("Invalid reaction");
                }
                
                Map<String, Object> serviceMap = api.getZpwServiceMap();
                Object reactionObj = serviceMap.get("reaction");
                List<String> reactionServices;
                
                if (reactionObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> reactionList = (List<String>) reactionObj;
                    reactionServices = reactionList;
                } else if (reactionObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reactionService = (Map<String, Object>) reactionObj;
                    Object reaction0Obj = reactionService.get("0");
                    if (reaction0Obj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> reactionList = (List<String>) reaction0Obj;
                        reactionServices = reactionList;
                    } else {
                        throw new ZaloApiError("Invalid reaction service format");
                    }
                } else {
                    throw new ZaloApiError("Reaction service not found");
                }
                
                String baseUrl = reactionServices.get(0) + "/api";
                String endpoint = destination.getType() == ThreadType.GROUP.getValue() ? "group" : "message";
                String url = Url.makeURL(ctx, baseUrl + "/" + endpoint + "/reaction", null, true);
                
                Map<String, Object> message = new HashMap<>();
                List<Map<String, Object>> rMsg = new ArrayList<>();
                Map<String, Object> msgItem = new HashMap<>();
                msgItem.put("gMsgID", Long.parseLong(String.valueOf(destination.getData().get("msgId"))));
                msgItem.put("cMsgID", Long.parseLong(String.valueOf(destination.getData().get("cliMsgId"))));
                msgItem.put("msgType", 1);
                rMsg.add(msgItem);
                message.put("rMsg", rMsg);
                message.put("rIcon", rIcon);
                message.put("rType", rType);
                message.put("source", source);
                
                Map<String, Object> params = new HashMap<>();
                List<Map<String, Object>> reactListArray = new ArrayList<>();
                Map<String, Object> reactItem = new HashMap<>();
                reactItem.put("message", Json.stringify(message));
                reactItem.put("clientId", System.currentTimeMillis());
                reactListArray.add(reactItem);
                params.put("react_list", reactListArray);
                
                if (destination.getType() == ThreadType.USER.getValue()) {
                    params.put("toid", destination.getThreadId());
                } else {
                    params.put("grid", destination.getThreadId());
                    params.put("imei", ctx.getImei());
                }
                
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
                
                Object responseData = Reponse.resolveResponse(ctx, response, null, true);
                
                 
                Map<String, Object> result = new HashMap<>();
                
                if (responseData instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = (Map<String, Object>) responseData;
                    
                    if (responseMap.containsKey("data")) {
                        Object dataObj = responseMap.get("data");
                        if (dataObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> data = (Map<String, Object>) dataObj;
                            if (data.containsKey("msgIds")) {
                                Object msgIdsObj = data.get("msgIds");
                                if (msgIdsObj instanceof String) {
                                    data.put("msgIds", Json.parse((String) msgIdsObj));
                                }
                                return data;
                            }
                            return data;
                        } else if (dataObj instanceof List) {
                            result.put("data", dataObj);
                            return result;
                        }
                    }
                    
                    if (responseMap.containsKey("msgIds")) {
                        Object msgIdsObj = responseMap.get("msgIds");
                        if (msgIdsObj instanceof String) {
                            responseMap.put("msgIds", Json.parse((String) msgIdsObj));
                        }
                    }
                    
                    return responseMap;
                } else if (responseData instanceof List) {
                    result.put("data", responseData);
                    return result;
                } else {
                    result.put("result", responseData);
                    return result;
                }
            } catch (Exception e) {
                throw new ZaloApiError("Failed to add reaction: " + e.getMessage(), e);
            }
        });
    }
    
    private ReactionInfo getReactionInfo(Reactions reaction) {
        ReactionInfo info = new ReactionInfo();
        info.source = 6;
        
        switch (reaction) {
            case HAHA: info.rType = 0; break;
            case LIKE: info.rType = 3; break;
            case HEART: info.rType = 5; break;
            case WOW: info.rType = 32; break;
            case CRY: info.rType = 2; break;
            case ANGRY: info.rType = 20; break;
            case KISS: info.rType = 8; break;
            case TEARS_OF_JOY: info.rType = 7; break;
            case SHIT: info.rType = 66; break;
            case ROSE: info.rType = 120; break;
            case BROKEN_HEART: info.rType = 65; break;
            case DISLIKE: info.rType = 4; break;
            case LOVE: info.rType = 29; break;
            case CONFUSED: info.rType = 51; break;
            case WINK: info.rType = 45; break;
            case FADE: info.rType = 121; break;
            case SUN: info.rType = 67; break;
            case BIRTHDAY: info.rType = 126; break;
            case BOMB: info.rType = 127; break;
            case OK: info.rType = 68; break;
            case PEACE: info.rType = 69; break;
            case THANKS: info.rType = 70; break;
            case PUNCH: info.rType = 71; break;
            case SHARE: info.rType = 72; break;
            case PRAY: info.rType = 73; break;
            case NO: info.rType = 131; break;
            case BAD: info.rType = 132; break;
            case LOVE_YOU: info.rType = 133; break;
            case SAD: info.rType = 1; break;
            case VERY_SAD: info.rType = 16; break;
            case COOL: info.rType = 21; break;
            case NERD: info.rType = 22; break;
            case BIG_SMILE: info.rType = 23; break;
            case SUNGLASSES: info.rType = 26; break;
            case NEUTRAL: info.rType = 30; break;
            case SAD_FACE: info.rType = 35; break;
            case BYE: info.rType = 36; break;
            case SLEEPY: info.rType = 38; break;
            case WIPE: info.rType = 39; break;
            case DIG: info.rType = 42; break;
            case ANGUISH: info.rType = 44; break;
            case HANDCLAP: info.rType = 46; break;
            case ANGRY_FACE: info.rType = 47; break;
            case F_CHAIR: info.rType = 48; break;
            case L_CHAIR: info.rType = 49; break;
            case R_CHAIR: info.rType = 50; break;
            case SILENT: info.rType = 52; break;
            case SURPRISE: info.rType = 53; break;
            case EMBARRASSED: info.rType = 54; break;
            case AFRAID: info.rType = 60; break;
            case SAD2: info.rType = 61; break;
            case BIG_LAUGH: info.rType = 62; break;
            case RICH: info.rType = 63; break;
            case BEER: info.rType = 99; break;
            default: info.rType = -1; break;
        }
        
        return info;
    }
    
    private static class ReactionInfo {
        Integer rType;
        Integer source;
    }
    
    public static class ReactionDestination {
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
        
        public static ReactionDestination create(ThreadType threadType, String threadId, 
                                                 String msgId, String cliMsgId) {
            ReactionDestination dest = new ReactionDestination();
            dest.setType(threadType.getValue());
            dest.setThreadId(threadId);
            Map<String, Object> data = new HashMap<>();
            data.put("msgId", msgId);
            data.put("cliMsgId", cliMsgId);
            dest.setData(data);
            return dest;
        }
    }
}

