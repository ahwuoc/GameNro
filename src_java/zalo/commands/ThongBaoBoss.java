package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import zalo.services.NroNotifyService;
import java.util.*;

public class ThongBaoBoss implements Command {
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
        return "boss";
    }
    
    @Override
    public String getDescription() {
        return "Quản lý thông báo boss NRO: .boss on/off hoặc .boss status";
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
                msgContent.setMsg("Sử dụng:\n.boss on - Bật thông báo boss\n.boss off - Tắt thông báo boss\n.boss status - Xem trạng thái");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            String action = args.get(0).toLowerCase().trim();
            
            if ("on".equals(action) || "true".equals(action) || "1".equals(action) || "enable".equals(action)) {
                boolean wasRegistered = NroNotifyService.gI().isRegistered(threadId);
                NroNotifyService.gI().registerGroup(threadId);
                
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                if (wasRegistered) {
                    msgContent.setMsg(" Nhóm này đã bật thông báo boss NRO rồi.");
                } else {
                    msgContent.setMsg(" Đã bật thông báo boss NRO cho nhóm này.\nBot sẽ thông báo khi có boss spawn trong game.");
                }
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                
            } else if ("off".equals(action) || "false".equals(action) || "0".equals(action) || "disable".equals(action)) {
                boolean wasRegistered = NroNotifyService.gI().isRegistered(threadId);
                NroNotifyService.gI().unregisterGroup(threadId);
                
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                if (!wasRegistered) {
                    msgContent.setMsg(" Nhóm này chưa bật thông báo boss NRO.");
                } else {
                    msgContent.setMsg(" Đã tắt thông báo boss NRO cho nhóm này.");
                }
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                
            } else if ("status".equals(action) || "info".equals(action) || "check".equals(action)) {
                boolean isRegistered = NroNotifyService.gI().isRegistered(threadId);
                int totalRegistered = NroNotifyService.gI().getRegisteredCount();
                
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                StringBuilder sb = new StringBuilder();
                sb.append("📊 Trạng thái thông báo boss NRO:\n\n");
                sb.append("Nhóm này: ").append(isRegistered ? " Đã bật" : " Đã tắt").append("\n");
                sb.append("Tổng số nhóm đã đăng ký: ").append(totalRegistered);
                msgContent.setMsg(sb.toString());
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                
            } else {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Lệnh không hợp lệ.\nSử dụng:\n.boss on - Bật thông báo\n.boss off - Tắt thông báo\n.boss status - Xem trạng thái");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
            }
        } catch (Exception e) {
        }
    }
}

