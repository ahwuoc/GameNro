package zalo.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledLogger {
    
    private static ScheduledLogger instance;
    private ScheduledExecutorService scheduler;
    private static final String LOG_MESSAGE = 
        "==========================\n" +
        "ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CẦM ĐẦU BOX PROGAYMERS NHÉ ĐỊT CON MẸ MÀY\n" +
        "@Author ENZEEFXNROxBARCOLL\n" +
        "==========================";
    // LƯU Ý NHÉ THẰNG BỊ GWEN NHẮC LÀ THẰNG NGU ÓC CỨT.
    private ScheduledLogger() {
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    public static ScheduledLogger gI() {
        if (instance == null) {
            instance = new ScheduledLogger();
        }
        return instance;
    }
    
    public void start() {
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("\n" + LOG_MESSAGE + "\n");
        }, 0, 30, TimeUnit.MINUTES);
        System.out.println("[SCHEDULED LOGGER] ok - gửi log 30p");
    }
    
    public void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
            System.out.println("[SCHEDULED LOGGER] địt mẹ con chó nghĩa ngu lồn");
        }
    }
}

