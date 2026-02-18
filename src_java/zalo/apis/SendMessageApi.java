/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.apis;

import zalo.utils.Json;
import zalo.utils.Maps;
import zalo.utils.Reponse;
import zalo.message.Message;
import zalo.utils.Crypto;
import zalo.utils.Url;
import zalo.services.HttpServices;
import zalo.utils.Apis;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import zalo.models.ThreadType;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SendMessageApi {
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
    
    public SendMessageApi(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
    }
    
    public CompletableFuture<SendMessageResponse> sendMessage(Object message, String threadId, ThreadType type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (message == null) {
                    throw new ZaloApiError("Missing message content");
                }
                if (threadId == null || threadId.isEmpty()) {
                    throw new ZaloApiError("Missing threadId");
                }
                
                MessageContent msgContent;
                if (message instanceof String) {
                    msgContent = new MessageContent();
                    msgContent.setMsg((String) message);
                } else if (message instanceof MessageContent) {
                    msgContent = (MessageContent) message;
                } else if (message instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageMap = (Map<String, Object>) message;
                    msgContent = mapToMessageContent(messageMap);
                } else {
                    throw new ZaloApiError("Invalid message format");
                }
                
                String msg = msgContent.getMsg() != null ? msgContent.getMsg() : "";
                Object[] attachments = msgContent.getAttachments();
                
                if (msg.isEmpty() && (attachments == null || attachments.length == 0)) {
                    throw new ZaloApiError("Missing message content");
                }
                
                SendMessageResponse response = new SendMessageResponse();
                
                if (attachments != null && attachments.length > 0) {
                    
                }
                
                if (!msg.isEmpty()) {
                    Map<String, Object> result = handleMessage(msgContent, threadId, type);
                    response.setMessage(result);
                }
                
                return response;
            } catch (Exception e) {
                throw new ZaloApiError("Failed to send message: " + e.getMessage(), e);
            }
        });
    }
    
    private Map<String, Object> handleMessage(MessageContent msgContent, String threadId, ThreadType type) throws Exception {
        String msg = msgContent.getMsg() != null ? msgContent.getMsg() : "";
        if (msg.isEmpty()) {
            throw new ZaloApiError("Missing message content");
        }
        
        boolean isGroupMessage = type == ThreadType.GROUP;
        MentionResult mentionResult = handleMentions(type, msg, msgContent.getMentions());
        msg = mentionResult.msgFinal;
        
        if (msgContent.getQuote() != null) {
            Map<String, Object> quote = extractQuote(msgContent.getQuote());
            if (quote != null) {
                String msgType = String.valueOf(quote.get("msgType"));
                if ("webchat".equals(msgType) && !(quote.get("content") instanceof String)) {
                    throw new ZaloApiError("This kind of `webchat` quote type is not available");
                }
                if ("group.poll".equals(msgType)) {
                    throw new ZaloApiError("The `group.poll` quote type is not available");
                }
            }
        }
        
        boolean isMentionsValid = mentionResult.mentionsFinal.size() > 0 && isGroupMessage;
        
        Map<String, Object> params = new HashMap<>();
        if (msgContent.getQuote() != null) {
            Map<String, Object> quote = extractQuote(msgContent.getQuote());
            if (isGroupMessage) {
                params.put("grid", threadId);
            } else {
                params.put("toid", threadId);
            }
            params.put("message", msg);
            params.put("clientId", System.currentTimeMillis());
            if (isMentionsValid) {
                params.put("mentionInfo", Json.stringify(mentionResult.mentionsFinal));
            }
            params.put("qmsgOwner", quote.get("uidFrom"));
            params.put("qmsgId", quote.get("msgId"));
            params.put("qmsgCliId", quote.get("cliMsgId"));
            params.put("qmsgType", Message.getClientMessageType(String.valueOf(quote.get("msgType"))));
            params.put("qmsgTs", quote.get("ts"));
            Object quoteContent = quote.get("content");
            if (quoteContent instanceof String) {
                params.put("qmsg", quoteContent);
            } else {
                params.put("qmsg", prepareQMSG(quote));
            }
            if (!isGroupMessage) {
                params.put("imei", ctx.getImei());
            }
            if (isGroupMessage) {
                params.put("visibility", 0);
                params.put("qmsgAttach", Json.stringify(prepareQMSGAttach(quote)));
            }
            if (quote.containsKey("ttl")) {
                params.put("qmsgTTL", quote.get("ttl"));
            }
            if (msgContent.getTtl() != null) {
                params.put("ttl", msgContent.getTtl());
            } else {
                params.put("ttl", 0);
            }
        } else {
            params.put("message", msg);
            params.put("clientId", System.currentTimeMillis());
            if (isMentionsValid) {
                params.put("mentionInfo", Json.stringify(mentionResult.mentionsFinal));
            }
            if (!isGroupMessage) {
                params.put("imei", ctx.getImei());
            }
            if (msgContent.getTtl() != null) {
                params.put("ttl", msgContent.getTtl());
            } else {
                params.put("ttl", 0);
            }
            if (isGroupMessage) {
                params.put("visibility", 0);
            }
            if (isGroupMessage) {
                params.put("grid", threadId);
            } else {
                params.put("toid", threadId);
            }
        }
        
        handleUrgency(params, msgContent.getUrgency());
        Maps.removeUndefinedKeys(params);
        
        String encryptedParams = Crypto.encodeAES(ctx.getSecretKey(), Json.stringify(params));
        if (encryptedParams == null) {
            throw new ZaloApiError("Failed to encrypt message");
        }
        
        String baseUrl = getServiceUrl(type);
        String endpointPath = getEndpointPath(type, params.containsKey("mentionInfo"), msgContent.getQuote() != null);
        String url = Url.makeURL(ctx, baseUrl + endpointPath, Collections.singletonMap("nretry", "0"));
        
        HttpServices.RequestOptions options = new HttpServices.RequestOptions();
        options.setMethod("POST");
        options.setBody("params=" + java.net.URLEncoder.encode(encryptedParams, "UTF-8"));
        
        HttpServices.HttpResponse response = HttpServices.request(ctx, url, options);
        Object responseData = Reponse.resolveResponse(ctx, response, null, true);
        
        if (responseData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) responseData;
            return result;
        } else if (responseData instanceof List) {
            Map<String, Object> result = new HashMap<>();
            result.put("data", responseData);
            return result;
        } else {
            Map<String, Object> result = new HashMap<>();
            result.put("data", responseData);
            return result;
        }
    }
    
    private String getServiceUrl(ThreadType type) throws Exception {
        Map<String, Object> serviceMap = api.getZpwServiceMap();
        if (type == ThreadType.GROUP) {
            Object groupObj = serviceMap.get("group");
            if (groupObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> groupServices = (List<String>) groupObj;
                return groupServices.get(0) + "/api/group";
            } else if (groupObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> groupMap = (Map<String, Object>) groupObj;
                Object group0Obj = groupMap.get("0");
                if (group0Obj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> groupServices = (List<String>) group0Obj;
                    return groupServices.get(0) + "/api/group";
                }
            }
            throw new ZaloApiError("Invalid group services format");
        } else {
            Object chatObj = serviceMap.get("chat");
            if (chatObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> chatServices = (List<String>) chatObj;
                return chatServices.get(0) + "/api/message";
            } else if (chatObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> chatMap = (Map<String, Object>) chatObj;
                Object chat0Obj = chatMap.get("0");
                if (chat0Obj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<String> chatServices = (List<String>) chat0Obj;
                    return chatServices.get(0) + "/api/message";
                }
            }
            throw new ZaloApiError("Invalid chat services format");
        }
    }
    
    private String getEndpointPath(ThreadType type, boolean hasMentions, boolean hasQuote) {
        if (hasQuote) {
            return "/quote";
        }
        if (type == ThreadType.GROUP) {
            return hasMentions ? "/mention" : "/sendmsg";
        } else {
            return "/sms";
        }
    }
    
    private MentionResult handleMentions(ThreadType type, String msg, Object[] mentions) {
        List<Map<String, Object>> mentionsFinal = new ArrayList<>();
        int totalMentionLen = 0;
        
        if (mentions != null && mentions.length > 0 && type == ThreadType.GROUP) {
            for (Object mentionObj : mentions) {
                if (mentionObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mention = (Map<String, Object>) mentionObj;
                    Object posObj = mention.get("pos");
                    Object uidObj = mention.get("uid");
                    Object lenObj = mention.get("len");
                    
                    if (posObj instanceof Number && uidObj != null && lenObj instanceof Number) {
                        int pos = ((Number) posObj).intValue();
                        String uid = String.valueOf(uidObj);
                        int len = ((Number) lenObj).intValue();
                        
                        if (pos >= 0 && !uid.isEmpty() && len > 0) {
                            totalMentionLen += len;
                            Map<String, Object> mentionFinal = new HashMap<>();
                            mentionFinal.put("pos", pos);
                            mentionFinal.put("uid", uid);
                            mentionFinal.put("len", len);
                            mentionFinal.put("type", "-1".equals(uid) ? 1 : 0);
                            mentionsFinal.add(mentionFinal);
                        }
                    }
                }
            }
        }
        
        if (totalMentionLen > msg.length()) {
            throw new ZaloApiError("Invalid mentions: total mention characters exceed message length");
        }
        
        MentionResult result = new MentionResult();
        result.mentionsFinal = mentionsFinal;
        result.msgFinal = msg;
        return result;
    }
    
    private void handleUrgency(Map<String, Object> params, Integer urgency) {
        if (urgency != null && (urgency == 1 || urgency == 2)) {
            Map<String, Object> metaData = new HashMap<>();
            metaData.put("urgency", urgency);
            params.put("metaData", metaData);
        }
    }
    
    private Map<String, Object> extractQuote(Object quote) {
        if (quote instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> quoteMap = (Map<String, Object>) quote;
            return quoteMap;
        }
        return null;
    }
    
    private String prepareQMSG(Map<String, Object> quote) {
        String msgType = String.valueOf(quote.get("msgType"));
        if ("chat.todo".equals(msgType)) {
            Object content = quote.get("content");
            if (content instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> contentMap = (Map<String, Object>) content;
                Object paramsObj = contentMap.get("params");
                if (paramsObj instanceof String) {
                    try {
                        Map<String, Object> params = Json.parseAsMap((String) paramsObj);
                        Object item = params.get("item");
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemMap = (Map<String, Object>) item;
                            Object itemContent = itemMap.get("content");
                            if (itemContent instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> itemContentMap = (Map<String, Object>) itemContent;
                                Object itemContentContent = itemContentMap.get("content");
                                return itemContentContent != null ? String.valueOf(itemContentContent) : "";
                            }
                        }
                    } catch (Exception e) {
                        return "";
                    }
                }
            }
        }
        return "";
    }
    
    private Map<String, Object> prepareQMSGAttach(Map<String, Object> quote) {
        Object content = quote.get("content");
        if (content instanceof String) {
            Object propertyExt = quote.get("propertyExt");
            if (propertyExt instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> propertyExtMap = (Map<String, Object>) propertyExt;
                return new HashMap<>(propertyExtMap);
            }
            return new HashMap<>();
        }
        
        String msgType = String.valueOf(quote.get("msgType"));
        if ("chat.todo".equals(msgType)) {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> properties = new HashMap<>();
            properties.put("color", 0);
            properties.put("size", 0);
            properties.put("type", 0);
            properties.put("subType", 0);
            properties.put("ext", "{\"shouldParseLinkOrContact\":0}");
            result.put("properties", properties);
            return result;
        }
        
        if (content instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> contentMap = (Map<String, Object>) content;
            Map<String, Object> result = new HashMap<>(contentMap);
            if (contentMap.containsKey("thumb")) {
                result.put("thumbUrl", contentMap.get("thumb"));
            }
            if (contentMap.containsKey("href")) {
                result.put("oriUrl", contentMap.get("href"));
                result.put("normalUrl", contentMap.get("href"));
            }
            return result;
        }
        
        return new HashMap<>();
    }
    
    public static Map<String, Object> createQuoteFromData(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Map<String, Object> quote = new HashMap<>();
        Object msgId = data.get("msgId");
        if (msgId != null) quote.put("msgId", msgId);
        Object cliMsgId = data.get("cliMsgId");
        if (cliMsgId != null) quote.put("cliMsgId", cliMsgId);
        Object uidFrom = data.get("uidFrom");
        if (uidFrom != null) quote.put("uidFrom", uidFrom);
        Object ts = data.get("ts");
        quote.put("ts", ts != null ? ts : System.currentTimeMillis());
        Object msgType = data.get("msgType");
        quote.put("msgType", msgType != null ? msgType : "webchat");
        Object content = data.get("content");
        quote.put("content", content != null ? content : "");
        return quote;
    }
    
    private MessageContent mapToMessageContent(Map<String, Object> map) {
        MessageContent content = new MessageContent();
        if (map.containsKey("msg")) {
            content.setMsg(String.valueOf(map.get("msg")));
        }
        if (map.containsKey("attachments")) {
            Object attachments = map.get("attachments");
            if (attachments instanceof Object[]) {
                content.setAttachments((Object[]) attachments);
            } else if (attachments instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) attachments;
                content.setAttachments(list.toArray());
            }
        }
        if (map.containsKey("mentions")) {
            Object mentions = map.get("mentions");
            if (mentions instanceof Object[]) {
                content.setMentions((Object[]) mentions);
            } else if (mentions instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) mentions;
                content.setMentions(list.toArray());
            }
        }
        if (map.containsKey("quote")) {
            content.setQuote(map.get("quote"));
        }
        if (map.containsKey("ttl")) {
            Object ttl = map.get("ttl");
            if (ttl instanceof Number) {
                content.setTtl(((Number) ttl).intValue());
            }
        }
        if (map.containsKey("urgency")) {
            Object urgency = map.get("urgency");
            if (urgency instanceof Number) {
                content.setUrgency(((Number) urgency).intValue());
            }
        }
        return content;
    }
    
    private static class MentionResult {
        List<Map<String, Object>> mentionsFinal;
        String msgFinal;
    }
    
    public static class MessageContent {
        private String msg;
        private Object[] attachments;
        private Object[] mentions;
        private Object quote;
        private Integer ttl;
        private Integer urgency;
        
        public String getMsg() {
            return msg;
        }
        
        public void setMsg(String msg) {
            this.msg = msg;
        }
        
        public Object[] getAttachments() {
            return attachments;
        }
        
        public void setAttachments(Object[] attachments) {
            this.attachments = attachments;
        }
        
        public Object[] getMentions() {
            return mentions;
        }
        
        public void setMentions(Object[] mentions) {
            this.mentions = mentions;
        }
        
        public Object getQuote() {
            return quote;
        }
        
        public void setQuote(Object quote) {
            this.quote = quote;
        }
        
        public Integer getTtl() {
            return ttl;
        }
        
        public void setTtl(Integer ttl) {
            this.ttl = ttl;
        }
        
        public Integer getUrgency() {
            return urgency;
        }
        
        public void setUrgency(Integer urgency) {
            this.urgency = urgency;
        }
    }
    
    public static class SendMessageResponse {
        private Object message;
        private Object[] attachment;
        
        public Object getMessage() {
            return message;
        }
        
        public void setMessage(Object message) {
            this.message = message;
        }
        
        public Object[] getAttachment() {
            return attachment;
        }
        
        public void setAttachment(Object[] attachment) {
            this.attachment = attachment;
        }
    }
}
