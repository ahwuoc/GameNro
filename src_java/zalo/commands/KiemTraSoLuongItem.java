package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import zalo.services.NroItemQuantityService;
import java.util.*;

public class KiemTraSoLuongItem implements Command {
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
        return "csoluong";
    }
    
    @Override
    public String getDescription() {
        return "Kiểm tra số lượng item của tất cả players: .csoluong itemid:soluong\nVí dụ: .csoluong 1112:100";
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
            
            if (args == null || args.isEmpty()) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Sử dụng: .csoluong itemid:soluong\nVí dụ: .csoluong 1112:100\nKiểm tra tất cả players có item ID 1112 với số lượng >= 100");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            List<Map<String, Integer>> itemsToCheck = new ArrayList<>();
            for (String arg : args) {
                if (arg.contains(":")) {
                    String[] parts = arg.split(":");
                    if (parts.length == 2) {
                        try {
                            int itemId = Integer.parseInt(parts[0].trim());
                            int requiredQuantity = Integer.parseInt(parts[1].trim());
                            Map<String, Integer> item = new HashMap<>();
                            item.put("id", itemId);
                            item.put("quantity", requiredQuantity);
                            itemsToCheck.add(item);
                        } catch (NumberFormatException e) {
                            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                            msgContent.setMsg("Item không hợp lệ: " + arg + "\nFormat: itemid:quantity\nVí dụ: 1112:100");
                            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                            return;
                        }
                    } else {
                        SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                        msgContent.setMsg("Item không hợp lệ: " + arg + "\nFormat: itemid:quantity\nVí dụ: 1112:100");
                        msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                        api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                        return;
                    }
                } else {
                    SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                    msgContent.setMsg("Item không hợp lệ: " + arg + "\nFormat: itemid:quantity\nVí dụ: 1112:100");
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                    return;
                }
            }
            
            if (itemsToCheck.isEmpty()) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Vui lòng cung cấp ít nhất 1 item để kiểm tra.\nVí dụ: .csoluong 1112:100");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            List<Map<String, Object>> results = NroItemQuantityService.gI().checkItemQuantity(itemsToCheck);
            
            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            if (results == null || results.isEmpty()) {
                msgContent.setMsg("Không tìm thấy player nào có item với số lượng vượt quá yêu cầu.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("🔍 Kết quả kiểm tra số lượng item:\n\n");
                sb.append("Tìm thấy ").append(results.size()).append(" player(s):\n\n");
                
                for (int i = 0; i < results.size() && i < 50; i++) {
                    Map<String, Object> result = results.get(i);
                    String playerName = (String) result.get("player");
                    Object itemsObj = result.get("items");
                    
                    sb.append("👤 ").append(playerName).append("\n");
                    
                    if (itemsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                        for (Map<String, Object> item : items) {
                            String itemName = (String) item.get("name");
                            Integer itemId = (Integer) item.get("id");
                            Integer quantity = (Integer) item.get("quantity");
                            
                            sb.append("  • ").append(itemName).append(" (ID: ").append(itemId).append(") x").append(quantity).append("\n");
                        }
                    }
                    sb.append("\n");
                }
                
                if (results.size() > 50) {
                    sb.append("... và ").append(results.size() - 50).append(" player khác");
                }
                
                msgContent.setMsg(sb.toString());
            }
            
            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        } catch (Exception e) {
        }
    }
}

