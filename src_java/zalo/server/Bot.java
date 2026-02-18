/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.server;

import zalo.message.MessageHandlers;
import zalo.utils.CommandLoader;
import zalo.services.CommandServices;
import zalo.interfaces.Command;
import zalo.login.Credentials;
import zalo.login.Zalo;
import zalo.login.ZaloOptions;
import zalo.utils.Apis;
import zalo.services.ListenServices;
import zalo.services.NroNotifyService;
import zalo.services.NroHttpServer;
import zalo.services.ScheduledLogger;
import zalo.services.DatabaseService;
import java.util.Map;

public class Bot {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private Apis api;
    private MessageHandlers messageHandler;
    private CommandServices commandHandler;
    private Map<String, Command> commands;
    private ListenServices listener;
    private String loginId;
    
    public void start() {
        try {
            System.out.println("========================================");
            System.out.println("ZALO BOT - ENZEEFXNROxBARCOLL...");
            System.out.println("========================================");
            
            Credentials creds = Settings.getCredentials();
            ZaloOptions options = Settings.getZaloOptions();
            System.out.println("Logging in...");
            Zalo zalo = new Zalo(options);
            try {
                api = zalo.login(creds).get();
            } catch (Exception e) {
                System.err.println("ERROR: Login failed: " + e.getMessage());
                return;
            }
            
            if (api == null) {
                System.err.println("ERROR: Login failed!");
                return;
            }
            
            System.out.println("Login successful!");
            System.out.println("UID: " + api.getContext().getUid());
            
            loginId = Settings.getLoginId();
            
            try {
                DatabaseService.gI().initialize(loginId);
            } catch (Exception e) {
                System.err.println("[DATABASE] Warning: Database initialization failed: " + e.getMessage());
            }
            
            System.out.println("\nLoading commands...");
            commands = CommandLoader.loadCommands();
            
             if (commands.isEmpty()) {
             } else {
             }
            
            commandHandler = new CommandServices();
            messageHandler = new MessageHandlers(
                commandHandler,
                Settings.getPrefix(),
                commands,
                api,
                loginId
            );
            System.out.println("\nSetting up message listener...");
            listener = new ListenServices(api.getContext(), api);
            
            listener.on("message", (event) -> {
                try {
                    messageHandler.onMessage(event);
                } catch (Exception e) {
                    System.err.println("[LISTENER] Error handling message event: " + e.getMessage());
                }
            });
            System.out.println("Starting listener...");
            listener.start();
            
            NroNotifyService.gI().setApi(api);
            NroNotifyService.gI().loadFromDatabase();
            
            int nroPort = Settings.getNroHttpPort();
            NroHttpServer.gI().start(nroPort);
            System.out.println("NRO HTTP Server started on port " + nroPort);
            
            ScheduledLogger.gI().start();
            
            System.out.println("\n========================================");
            System.out.println("BOT IS RUNNING!");
            System.out.println("Prefix: " + Settings.getPrefix());
            System.out.println("Commands: " + commands.size());
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to start bot: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void stop() {
        try {
            ScheduledLogger.gI().stop();
            NroHttpServer.gI().stop();
            DatabaseService.gI().close();
            if (listener != null) {
                listener.close();
            }
            System.out.println("Bot stopped");
        } catch (Exception e) {
            System.err.println("Error stopping bot: " + e.getMessage());
        }
    }
    
    public Apis getApi() {
        return api;
    }
    
    public Map<String, Command> getCommands() {
        return commands;
    }
}

