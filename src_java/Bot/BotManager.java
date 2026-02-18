/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Bot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import nro.server.ServerManager;

/**
 *
 * @author Administrator
 */
public class BotManager implements Runnable {
    public static final ScheduledExecutorService BOT_SERVICE =
      Executors.newScheduledThreadPool(
        Runtime.getRuntime().availableProcessors() * 2
      );

    public static BotManager i;
    
    public List<Bot> bot =  new ArrayList<>();
    
    
    public static BotManager gI(){
        if(i == null){
            i = new BotManager();
        }
            return i;
    }
       @Override
    public void run() {
        while (ServerManager.isRunning) {
            try {
                long st = System.currentTimeMillis();
                for (Bot bot : this.bot) {
                    bot.update();
                }
                Thread.sleep(150 - (System.currentTimeMillis() - st));
            } catch (Exception ignored) {
            }

        }
    }
}