package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import java.util.*;

public class Group implements Command {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    @Override
    public String getName() {
        return "group";
    }
    
    @Override
    public String getDescription() {
        return "Group tools: info | lockchat | unlockchat";
    }
    
    @Override
    public String getTag() {
        return "group";
    }
    
    @Override
    public int getCooldown() {
        return 0;
    }
    
    @Override
    public int getRole() {
        return 2;
    }
    
    @Override
    public void run(Command.CommandContext context) {
        try {
            MessageContext message = context.getMessage();
            Apis api = context.getApi();
            List<String> args = context.getArgs();
            
            String threadId = message.getThreadId();
            ThreadType threadType = message.getThreadType();
            Map<String, Object> data = message.getData();
            
            if (!message.isGroup()) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Lệnh này chỉ dùng trong nhóm.");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            String sub = (args != null && !args.isEmpty()) ? args.get(0).toLowerCase().trim() : "info";
            
            if ("lockchat".equals(sub) || "unlockchat".equals(sub)) {
                try {
                    int lock = "lockchat".equals(sub) ? 1 : 0;
                    Map<String, Object> options = new HashMap<>();
                    options.put("lockSendMsg", lock == 1);
                    
                    api.updateGroupSettings.updateGroupSettings(options, threadId).get();
                    
                    String text = lock == 1 ? "Đã khóa gửi tin trong nhóm." : "Đã mở khóa gửi tin trong nhóm.";
                    SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                    msgContent.setMsg(text);
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                } catch (Exception e) {
                    SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                    msgContent.setMsg("Không thể cập nhật cài đặt nhóm: " + e.getMessage());
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                }
                return;
            }
            
            try {
                Map<String, Object> groupInfo = api.getGroupInfo.getGroupInfo(threadId).get();
                
                if (groupInfo == null || groupInfo.isEmpty()) {
                    SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                    msgContent.setMsg("Không thể lấy thông tin group.");
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                    return;
                }
                
                Map<String, Object> g = null;
                Object gridInfoMapObj = groupInfo.get("gridInfoMap");
                if (gridInfoMapObj == null) {
                    gridInfoMapObj = groupInfo.get("gridInfoMap");
                }
                
                if (gridInfoMapObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> gridInfoMap = (Map<String, Object>) gridInfoMapObj;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> groupData = (Map<String, Object>) gridInfoMap.get(threadId);
                    if (groupData != null) {
                        g = groupData;
                    } else if (!gridInfoMap.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> firstGroup = (Map<String, Object>) gridInfoMap.values().iterator().next();
                        g = firstGroup;
                    }
                }
                
                if (g == null || g.isEmpty()) {
                    SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                    msgContent.setMsg("Không tìm thấy thông tin group.");
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                    return;
                }
                
                String groupId = getStringValue(g, "groupId", "id", threadId);
                String name = getStringValue(g, "name", "groupName", "Unknown");
                String desc = getStringValue(g, "desc", "description", "Không có mô tả");
                String creatorId = getStringValue(g, "creatorId", "creator", "");
                
                Object totalMemberObj = g.get("totalMember");
                int totalMember = totalMemberObj instanceof Number ? ((Number) totalMemberObj).intValue() : 0;
                
                Object maxMemberObj = g.get("maxMember");
                int maxMember = maxMemberObj instanceof Number ? ((Number) maxMemberObj).intValue() : 1000;
                
                String createdTime = "Không rõ";
                Object createdTimeObj = g.get("createdTime");
                if (createdTimeObj instanceof Number) {
                    long timestamp = ((Number) createdTimeObj).longValue();
                    if (timestamp < 10000000000L) {
                        timestamp *= 1000;
                    }
                    Date date = new Date(timestamp);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                    createdTime = sdf.format(date);
                }
                
                Object settingObj = g.get("setting");
                Map<String, Object> settingMap = new HashMap<>();
                if (settingObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> settingData = (Map<String, Object>) settingObj;
                    settingMap = settingData;
                }
                
                List<String> lines = new ArrayList<>();
                lines.add("═══════════════════");
                lines.add(" THÔNG TIN NHÓM");
                lines.add("═══════════════════");
                lines.add(" Tên: " + name);
                lines.add("ID: " + groupId);
                lines.add("Mô tả: " + desc);
                lines.add("Người tạo: " + (creatorId.isEmpty() ? "Không rõ" : creatorId));
                lines.add("Thành viên: " + totalMember + "/" + maxMember);
                lines.add("Tạo lúc: " + createdTime);
                lines.add("");
                lines.add("CÀI ĐẶT:");
                
                List<String> settingsItems = new ArrayList<>();
                settingsItems.add("Chặn đổi tên: " + toFlag(settingMap.get("blockName")));
                settingsItems.add("Ký hiệu admin: " + toFlag(settingMap.get("signAdminMsg")));
                settingsItems.add("Chỉ admin thêm TV: " + toFlag(settingMap.get("addMemberOnly")));
                settingsItems.add("Chỉ admin đổi chủ đề: " + toFlag(settingMap.get("setTopicOnly")));
                settingsItems.add("Lưu lịch sử chat: " + toFlag(settingMap.get("enableMsgHistory")));
                settingsItems.add("Khoá tạo bài viết: " + toFlag(settingMap.get("lockCreatePost")));
                settingsItems.add("Khoá tạo bình chọn: " + toFlag(settingMap.get("lockCreatePoll")));
                settingsItems.add("Duyệt vào nhóm: " + toFlag(settingMap.get("joinAppr")));
                settingsItems.add("Khoá gửi tin: " + toFlag(settingMap.get("lockSendMsg")));
                settingsItems.add("Ẩn danh sách TV: " + toFlag(settingMap.get("lockViewMember")));
                
                lines.addAll(settingsItems);
                lines.add("═══════════════════");
                lines.add("Lệnh: .group lockchat | unlockchat");
                
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg(String.join("\n", lines));
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                
            } catch (Exception e) {
                System.err.println("[GROUP] Error: " + e.getMessage());
                e.printStackTrace();
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Đã xảy ra lỗi khi lấy thông tin nhóm: " + e.getMessage());
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
            }
        } catch (Exception e) {
            System.err.println("[GROUP] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
    
    private String toFlag(Object v) {
        if (v instanceof Number) {
            return ((Number) v).intValue() == 1 ? "✓" : "✗";
        }
        return Boolean.TRUE.equals(v) ? "✓" : "✗";
    }
}

