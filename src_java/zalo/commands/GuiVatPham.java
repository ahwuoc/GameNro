package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import zalo.services.NroItemService;
import java.util.*;

public class GuiVatPham implements Command {
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
        return "bitem";
    }
    
    @Override
    public String getDescription() {
        return "Gửi vật phẩm cho player NRO: .bitem username id_item +soluong";
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
            
            if (args == null || args.size() < 3) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Sử dụng: .bitem <username> <id_item> +<soluong> [option=id:param ...]\nVí dụ: .bitem player123 457 +10\nVí dụ có option: .bitem tester 1112 option=50:200 option=77:123");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            String username = args.get(0).trim();
            String itemIdStr = args.get(1).trim();
            String quantityStr = args.get(2).trim();
            
            if (quantityStr.startsWith("+")) {
                quantityStr = quantityStr.substring(1);
            }
            
            int itemId;
            int quantity;
            
            try {
                itemId = Integer.parseInt(itemIdStr);
                quantity = Integer.parseInt(quantityStr);
                
                if (itemId <= 0 || quantity <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("ID item và số lượng phải là số nguyên dương.\nVí dụ: .bitem player123 457 +10");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            List<Map<String, Integer>> options = new ArrayList<>();
            for (int i = 3; i < args.size(); i++) {
                String arg = args.get(i).trim();
                if (arg.startsWith("option=")) {
                    String optionStr = arg.substring(7);
                    String[] parts = optionStr.split(":");
                    if (parts.length == 2) {
                        try {
                            int optionId = Integer.parseInt(parts[0].trim());
                            int optionParam = Integer.parseInt(parts[1].trim());
                            Map<String, Integer> option = new HashMap<>();
                            option.put("id", optionId);
                            option.put("param", optionParam);
                            options.add(option);
                        } catch (NumberFormatException e) {
                            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                            msgContent.setMsg("Option không hợp lệ: " + arg + "\nFormat: option=id:param\nVí dụ: option=50:200");
                            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                            return;
                        }
                    } else {
                        SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                        msgContent.setMsg("Option không hợp lệ: " + arg + "\nFormat: option=id:param\nVí dụ: option=50:200");
                        msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                        api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                        return;
                    }
                }
            }
            
            boolean success = NroItemService.gI().giveItem(username, itemId, quantity, options);
            
            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            if (success) {
                msgContent.setMsg("Đã gửi " + quantity + " vật phẩm ID " + itemId + " cho " + username);
            } else {
                msgContent.setMsg("Không thể gửi vật phẩm. Player có thể balo full đồ hoặc offline hoặc không kết nối được game.");
            }
            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        } catch (Exception e) {
        }
    }
}

