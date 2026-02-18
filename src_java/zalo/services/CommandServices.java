/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.services;

import zalo.message.MessageContext;
import zalo.interfaces.Command;
import zalo.utils.Apis;
import zalo.models.Reactions;
import zalo.apis.AddReactionApi;
import zalo.services.DatabaseService;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandServices {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private final Map<String, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    public boolean run(String prefix, String content, MessageContext ctx, Map<String, Command> commands, Apis api) {
        if (!content.startsWith(prefix)) {
            return false;
        }
        
        String raw = content.substring(prefix.length()).trim();
        if (raw.isEmpty()) {
            return false;
        }
        
        String[] parts = raw.split("\\s+");
        String name = parts[0].toLowerCase();
        List<String> args = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i]);
        }
        
        Command cmd = commands.get(name);
        if (cmd == null || cmd.isHidden()) {
             try {
                addReaction(api, ctx, Reactions.SUN);
             } catch (Exception e) {
             }
            return false;
        }
        
        int cooldownTime = cmd.getCooldown();
        if (cooldownTime > 0) {
            boolean skipCooldown = false;
           
            if (!skipCooldown) {
                if (!cooldowns.containsKey(name)) {
                    cooldowns.put(name, new ConcurrentHashMap<>());
                }
                
                long now = System.currentTimeMillis();
                Map<String, Long> userCooldowns = cooldowns.get(name);
                String senderUid = String.valueOf(ctx.getData().getOrDefault("uidFrom", "unknown"));
                String scopeId = String.valueOf(ctx.getThreadId());
                String key = scopeId + ":" + senderUid;
                Long lastUsed = userCooldowns.get(key);
                
                if (lastUsed != null) {
                    long endAt = lastUsed + cooldownTime * 1000L;
                    long remaining = endAt - now;
                    
                    if (remaining > 0) {
                        long secondsLeft = remaining / 1000;
                        try {
                            addReaction(api, ctx, Reactions.DISLIKE);
                             } catch (Exception e) {
                            }
                        return true;
                    }
                }
                
                userCooldowns.put(key, now);
            }
        }
        
        int requiredRole = cmd.getRole();
        if (requiredRole > 0) {
            String senderUid = String.valueOf(ctx.getData().getOrDefault("uidFrom", "unknown"));
            int userRole = DatabaseService.gI().getUserRole(senderUid);
            
            if (userRole < requiredRole) {
                try {
                    addReaction(api, ctx, Reactions.DISLIKE);
                } catch (Exception e) {
                }
                return true;
            }
        }
        
       
        try {
            Command.CommandContext cmdCtx = new Command.CommandContext();
            cmdCtx.setMessage(ctx);
            cmdCtx.setApi(api);
            cmdCtx.setArgs(args);
            cmdCtx.setCommands(commands);
            
            cmd.run(cmdCtx);
            
            try {
                addReaction(api, ctx, Reactions.OK);
            } catch (Exception e) {
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("[COMMAND] Error executing " + name + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void addReaction(Apis api, MessageContext ctx, Reactions reaction) {
        try {
            Map<String, Object> data = ctx.getData();
            String msgId = String.valueOf(data.getOrDefault("msgId", 0));
            String cliMsgId = String.valueOf(data.getOrDefault("cliMsgId", 0));
            AddReactionApi.ReactionDestination dest = AddReactionApi.ReactionDestination.create(
                ctx.getThreadType(),
                ctx.getThreadId(),
                msgId,
                cliMsgId
            );
            
            Map<String, Object> result = api.addReaction.addReaction(reaction, dest).get();
            System.out.println("[REACTION] Success: " + result);
        } catch (Exception e) {
            System.err.println("[REACTION] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

