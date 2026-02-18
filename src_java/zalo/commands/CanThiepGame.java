package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import zalo.services.NroAccountService;
import java.util.*;

public class CanThiepGame implements Command {
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
        return "cthiep";
    }
    
    @Override
    public String getDescription() {
        return "Can thiệp game: .cthiep vnd user sotien | mtv user | ban user | unban user | admin user | unadmin user";
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
            
            if (args == null || args.size() < 2) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Sử dụng:\n.cthiep vnd <user|all> <sotien>\n.cthiep mtv <user|all>\n.cthiep ban <user>\n.cthiep unban <user>\n.cthiep admin <user>\n.cthiep unadmin <user>");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            String action = args.get(0).toLowerCase().trim();
            String targetUser = args.get(1).trim();
            
            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            
            if ("vnd".equals(action)) {
                if (args.size() < 3) {
                    msgContent.setMsg("Sử dụng: .cthiep vnd <user|all> <sotien>");
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                    return;
                }
                
                String amountStr = args.get(2).trim();
                int amount;
                try {
                    amount = Integer.parseInt(amountStr);
                    if (amount <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    msgContent.setMsg("Số tiền phải là số nguyên dương.");
                    msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                    api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                    return;
                }
                
                boolean success = NroAccountService.gI().updateAccountVnd(targetUser, amount);
                if (success) {
                    String target = "all".equalsIgnoreCase(targetUser) ? "tất cả account" : targetUser;
                    msgContent.setMsg("Đã buff " + amount + " VND cho " + target);
                } else {
                    msgContent.setMsg("Không thể buff VND cho " + targetUser);
                }
                
            } else if ("mtv".equals(action)) {
                boolean success = NroAccountService.gI().addMemberToVip(targetUser);
                if (success) {
                    String target = "all".equalsIgnoreCase(targetUser) ? "tất cả account" : targetUser;
                    msgContent.setMsg("Đã thêm mở thành viên cho " + target);
                } else {
                    msgContent.setMsg("Không thể thêm mở thành viên cho " + targetUser);
                }
                
            } else if ("ban".equals(action)) {
                boolean success = NroAccountService.gI().setAccountBanned(targetUser, true);
                if (success) {
                    msgContent.setMsg("Đã ban " + targetUser);
                } else {
                    msgContent.setMsg("Không thể ban " + targetUser);
                }
                
            } else if ("unban".equals(action)) {
                boolean success = NroAccountService.gI().setAccountBanned(targetUser, false);
                if (success) {
                    msgContent.setMsg("Đã unban " + targetUser);
                } else {
                    msgContent.setMsg("Không thể unban " + targetUser);
                }
                
            } else if ("admin".equals(action)) {
                boolean success = NroAccountService.gI().setAccountAdmin(targetUser, true);
                if (success) {
                    msgContent.setMsg("Đã set admin cho " + targetUser);
                } else {
                    msgContent.setMsg("Không thể set admin cho " + targetUser);
                }
                
            } else if ("unadmin".equals(action)) {
                boolean success = NroAccountService.gI().setAccountAdmin(targetUser, false);
                if (success) {
                    msgContent.setMsg("Đã unset admin cho " + targetUser);
                } else {
                    msgContent.setMsg("Không thể unset admin cho " + targetUser);
                }
                
            } else {
                msgContent.setMsg("Lệnh không hợp lệ. Sử dụng:\n.cthiep vnd <user|all> <sotien>\n.cthiep mtv <user|all>\n.cthiep ban <user>\n.cthiep unban <user>\n.cthiep admin <user>\n.cthiep unadmin <user>");
            }
            
            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
        } catch (Exception e) {
        }
    }
}

