package nro.server;

/*
 *
 *
 *  Box ZALO:https://zalo.me/g/ifjict764
 *  sdt zalo: 0358176187
 * Chuyên chỉnh sữa mua bán source nro,...
 */

import java.io.IOException;
import java.time.LocalTime;
import java.util.logging.Level;
import nro.services.ClanService;
import nro.services.Service;
import utils.Functions;
import utils.Logger;

public class AutoMaintenance extends Thread {
    
    public static boolean isRunning = false;
    private static AutoMaintenance instance;
    public static boolean AutoMaintenance = Manager.AUTO_MAINTENANCE == 1; 
    public static final int hours = Manager.AUTO_MAINTENANCE_HOUR; 
    public static final int mins = Manager.AUTO_MAINTENANCE_MINUTE;

    public static AutoMaintenance gI() {
        if (instance == null) {
            instance = new AutoMaintenance();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && !isRunning) {
            try {
                if (AutoMaintenance) {
                    LocalTime currentTime = LocalTime.now();
                    if (currentTime.getHour() == hours && currentTime.getMinute() == mins) {
                        Logger.log(Logger.PURPLE, "Đang tiến hành quá trình bảo trì tự động\n");
                        Maintenance.gI().start(60);
                        isRunning = true;
                        AutoMaintenance = false;
                    }
                }
                Functions.sleep(1000);
            } catch (Exception e) {
            }
        }
    }
    
    public void execute() {
        try {
            isRunning = true;
            int seconds = 60;
            while (seconds > 0) {
                seconds--;
                Service.gI().sendThongBaoAllPlayer("NgocRongOnlineHayZo sẽ bảo trì định kì sau " + seconds
                        + " giây nữa, vui lòng thoát game để tránh mất vật phẩm.");
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                Client.gI().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                ClanService.gI().close();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            ServerManager.listenSocket.close();
            String executeCommand = Manager.executeCommand;
            if (executeCommand != null) {
                openCmd(executeCommand);
            }
        } 
//        catch (IOException ex) {
//            java.util.logging.Logger.getLogger(AutoMaintenance.class.getName()).log(Level.SEVERE, null, ex);
//            System.exit(1);
//        } 
        finally {
            System.exit(0);
        }
    }
    
    private void openCmd(String cmd) {
        try {
            Runtime rt = Runtime.getRuntime();
            rt.exec("cmd /c start cmd.exe /K \"dir && " + cmd);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AutoMaintenance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
