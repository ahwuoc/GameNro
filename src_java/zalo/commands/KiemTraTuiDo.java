package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import zalo.services.NroBagService;
import java.util.*;

public class KiemTraTuiDo implements Command {
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
        return "cbag";
    }
    
    @Override
    public String getDescription() {
        return "Kiểm tra balo của người chơi: .cbag <username>";
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
                msgContent.setMsg("Sử dụng: .cbag <username>\nVí dụ: .cbag player123");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            String username = args.get(0).trim();
            
            Map<String, Object> bagInfo = NroBagService.gI().getPlayerBag(username);
            
            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            if (bagInfo == null || bagInfo.isEmpty()) {
                msgContent.setMsg("Không tìm thấy player: " + username);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("🎒 Balo của ").append(username).append("\n\n");
                
                Object itemsObj = bagInfo.get("items");
                if (itemsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) itemsObj;
                    
                    if (items.isEmpty()) {
                        sb.append("Balo trống");
                    } else {
                        sb.append("Tổng số item: ").append(items.size()).append("\n\n");
                        
                        for (int i = 0; i < items.size(); i++) {
                            Map<String, Object> item = items.get(i);
                            String itemName = (String) item.get("name");
                            Integer quantity = (Integer) item.get("quantity");
                            Integer itemId = (Integer) item.get("id");
                            
                            sb.append("• ").append(itemName);
                            if (quantity != null && quantity > 1) {
                                sb.append(" x").append(quantity);
                            }
                            sb.append(" (ID: ").append(itemId).append(")\n");
                        }
                    }
                } else {
                    sb.append("Không có dữ liệu items");
                }
                
                msgContent.setMsg(sb.toString());
            }
            
            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        } catch (Exception e) {
        }
    }
}

