/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.Reactions;
import zalo.models.ThreadType;
import zalo.apis.AddReactionApi;
import java.util.Map;

public class Reaction implements Command {
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
        return "reaction";
    }
    
    @Override
    public String getDescription() {
        return "Test reaction API";
    }
    
    @Override
    public String getTag() {
        return "test";
    }
    
    @Override
    public void run(CommandContext ctx) {
        MessageContext msgCtx = ctx.getMessage();
        Apis api = ctx.getApi();
        java.util.List<String> args = ctx.getArgs();
        
        String threadId = msgCtx.getThreadId();
        ThreadType threadType = msgCtx.getThreadType();
        
        if (args.isEmpty()) {
            try {
                api.sendMessage.sendMessage(
                    "Usage: .reaction <reaction_name>\nAvailable: LIKE, LOVE, HAHA, WOW, SAD, ANGRY, OK, SUN, COOL, DISLIKE",
                    threadId,
                    threadType
                ).get();
            } catch (Exception e) {
                System.err.println("[REACTION COMMAND] Failed to send usage: " + e.getMessage());
            }
            return;
        }
        
        String reactionName = args.get(0).toUpperCase();
        Reactions reaction;
        
        try {
            reaction = Reactions.valueOf(reactionName);
        } catch (IllegalArgumentException e) {
            try {
                api.sendMessage.sendMessage(
                    "Invalid reaction: " + reactionName + "\nAvailable: LIKE, LOVE, HAHA, WOW, SAD, ANGRY, OK, SUN, COOL, DISLIKE",
                    threadId,
                    threadType
                ).get();
            } catch (Exception ex) {
                System.err.println("[REACTION COMMAND] Failed to send error: " + ex.getMessage());
            }
            return;
        }
        
        try {
            Map<String, Object> data = msgCtx.getData();
            String msgId = String.valueOf(data.getOrDefault("msgId", 0));
            String cliMsgId = String.valueOf(data.getOrDefault("cliMsgId", 0));
            
             
            AddReactionApi.ReactionDestination dest = AddReactionApi.ReactionDestination.create(
                threadType,
                threadId,
                msgId,
                cliMsgId
            );
            
            Map<String, Object> result = api.addReaction.addReaction(reaction, dest).get();
             
            api.sendMessage.sendMessage(
                "Added reaction: " + reaction + " (" + reaction.getValue() + ")",
                threadId,
                threadType
            ).get();
        } catch (Exception e) {
            System.err.println("[REACTION COMMAND] Error: " + e.getMessage());
            e.printStackTrace();
            try {
                api.sendMessage.sendMessage(
                    "Failed to add reaction: " + e.getMessage(),
                    threadId,
                    threadType
                ).get();
            } catch (Exception ex) {
                System.err.println("[REACTION COMMAND] Failed to send error message: " + ex.getMessage());
            }
        }
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
    public boolean isHidden() {
        return false;
    }
}

