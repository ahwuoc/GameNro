/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.message;

import zalo.services.CommandServices;
import zalo.services.DatabaseService;
import zalo.interfaces.Command;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import java.util.Map;

public class MessageHandlers {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private final CommandServices commandHandler;
    private final String prefix;
    private final Map<String, Command> commands;
    private final Apis api;
    private final String loginId;
    
    public MessageHandlers(CommandServices commandHandler, String prefix, 
                         Map<String, Command> commands, Apis api, String loginId) {
        this.commandHandler = commandHandler;
        this.prefix = prefix;
        this.commands = commands;
        this.api = api;
        this.loginId = loginId;
    }
    
    public void onMessage(Object rawMessage) {
        try {
            if (rawMessage instanceof zalo.models.Message) {
                zalo.models.Message message = (zalo.models.Message) rawMessage;
                
                if (message.isSelf() && !api.getContext().getOptions().isSelfListen()) {
                    return;
                }
                
                Map<String, Object> data = message.getData();
                if (data == null) {
                    return;
                }
                
                Object contentObj = data.get("content");
                String content = "";
                if (contentObj instanceof String) {
                    content = (String) contentObj;
                } else if (contentObj != null) {
                    if (contentObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> contentMap = (Map<String, Object>) contentObj;
                        Object textObj = contentMap.getOrDefault("text", 
                            contentMap.getOrDefault("msg", 
                            contentMap.getOrDefault("body", 
                            contentMap.getOrDefault("message", ""))));
                        if (textObj instanceof String) {
                            content = (String) textObj;
                        } else {
                            content = String.valueOf(textObj);
                        }
                    } else {
                        content = String.valueOf(contentObj);
                    }
                }
                
                if (content.isEmpty()) {
                    Object textObj = data.getOrDefault("text", 
                        data.getOrDefault("msg", 
                        data.getOrDefault("message", 
                        data.getOrDefault("body", ""))));
                    if (textObj instanceof String) {
                        content = (String) textObj;
                    } else {
                        content = String.valueOf(textObj);
                    }
                }
                
                content = content.trim();
                
                String threadId = message.getThreadId();
                ThreadType threadType = message.getType();
                boolean isGroup = threadType == ThreadType.GROUP;
                
                MessageContext ctx = new MessageContext();
                ctx.setData(data);
                ctx.setThreadId(threadId);
                ctx.setThreadType(threadType);
                ctx.setGroup(isGroup);
                ctx.setRaw(rawMessage);
                ctx.setLoginId(loginId);
                if (!content.isEmpty() && prefix != null && !prefix.isEmpty() && content.startsWith(prefix)) {
                    commandHandler.run(prefix, content, ctx, commands, api);
                }
                
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> messageMap = new java.util.HashMap<>();
                    messageMap.put("threadId", threadId);
                    messageMap.put("threadType", threadType);
                    messageMap.put("data", data);
                    DatabaseService.gI().updateUserFromMessage(messageMap, api);
                    if (isGroup) {
                        DatabaseService.gI().updateGroupFromMessage(messageMap, api);
                    }
                } catch (Exception e) {
                    System.err.println("[DATABASE] Error updating from message: " + e.getMessage());
                }
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> message = (Map<String, Object>) rawMessage;
                
                Object typeObj = message.get("type");
                int type = 0;
                if (typeObj instanceof Number) {
                    type = ((Number) typeObj).intValue();
                } else if (typeObj instanceof ThreadType) {
                    type = ((ThreadType) typeObj) == ThreadType.GROUP ? 1 : 0;
                }
                boolean isGroup = type == 1;
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.getOrDefault("data", new java.util.HashMap<>());
                
                Object contentObj = data.get("content");
                String content = "";
                if (contentObj instanceof String) {
                    content = (String) contentObj;
                } else if (contentObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentMap = (Map<String, Object>) contentObj;
                    content = String.valueOf(contentMap.getOrDefault("text", 
                        contentMap.getOrDefault("msg", 
                        contentMap.getOrDefault("body", ""))));
                }
                
                String threadId = String.valueOf(message.getOrDefault("threadId", ""));
                ThreadType threadType = isGroup ? ThreadType.GROUP : ThreadType.USER;
                
                MessageContext ctx = new MessageContext();
                ctx.setData(data);
                ctx.setThreadId(threadId);
                ctx.setThreadType(threadType);
                ctx.setGroup(isGroup);
                ctx.setRaw(rawMessage);
                ctx.setLoginId(loginId);
                
                if (!content.isEmpty() && prefix != null && !prefix.isEmpty() && content.startsWith(prefix)) {
                    commandHandler.run(prefix, content, ctx, commands, api);
                }
                
                try {
                    DatabaseService.gI().updateUserFromMessage(message, api);
                    if (isGroup) {
                        DatabaseService.gI().updateGroupFromMessage(message, api);
                    }
                } catch (Exception e) {
                    System.err.println("[DATABASE] Error updating from message: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[MESSAGE] Error handling message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

