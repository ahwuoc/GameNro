/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;

public class Ping implements Command {
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
        return "ping";
    }
    
    @Override
    public String getDescription() {
        return "Kiểm tra độ trễ";
    }
    
    @Override
    public String getTag() {
        return "admin";
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
        long startTime = System.currentTimeMillis();
        try {
            MessageContext message = context.getMessage();
            Apis api = context.getApi();
            
            String threadId = message.getThreadId();
            ThreadType threadType = message.getThreadType();
            
            long responseTime = System.currentTimeMillis() - startTime;
            String msg = "Pong!\nToc do phan hoi: " + responseTime + "ms";
            
            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            msgContent.setMsg(msg);
            msgContent.setQuote(SendMessageApi.createQuoteFromData(message.getData()));
            
            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        } catch (Exception e) {
            System.err.println("[PING] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

