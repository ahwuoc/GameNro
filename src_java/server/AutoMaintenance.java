package server;

import DucPro.Functions;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import utils.Logger;

public class AutoMaintenance extends Thread {

    public static boolean AutoMaintenance = true;

    public static final List<MaintenanceSchedule> schedules = new ArrayList<>() {
        {
            add(new MaintenanceSchedule(6, 0)); // 6:00
        }
    };

    private static AutoMaintenance instance;
    public static boolean isRunning;
    private static int lastExecutedDay = -1;
    private static int lastExecutedSlot = -1;

    public static AutoMaintenance gI() {
        if (instance == null) {
            instance = new AutoMaintenance();
        }
        return instance;
    }

    @Override
    public void run() {
        StringBuilder sb = new StringBuilder("AutoMaintenance thread started - Scheduled at ");
        for (int i = 0; i < schedules.size(); i++) {
            sb.append(schedules.get(i));
            if (i < schedules.size() - 1)
                sb.append(", ");
        }
        Logger.log(Logger.PURPLE, sb.toString() + "\n");

        while (!Maintenance.isRunning) {
            try {
                if (AutoMaintenance) {
                    LocalTime currentTime = LocalTime.now();
                    int currentDay = java.time.LocalDate.now().getDayOfYear();
                    int currentHour = currentTime.getHour();
                    int currentMin = currentTime.getMinute();

                    for (int i = 0; i < schedules.size(); i++) {
                        MaintenanceSchedule schedule = schedules.get(i);
                        if (currentHour == schedule.hour && currentMin == schedule.minute
                                && !(lastExecutedDay == currentDay && lastExecutedSlot == i)) {
                            Logger.log(Logger.PURPLE,
                                    "Auto maintenance (slot " + (i + 1) + ") triggered at " + currentTime + "\n");
                            Maintenance.gI().start(60);
                            lastExecutedDay = currentDay;
                            lastExecutedSlot = i;
                            isRunning = true;
                            break;
                        }
                    }
                }
                Functions.sleep(1000);
            } catch (Exception e) {
                Logger.logException(this.getClass(), e, "Error in AutoMaintenance");
            }
        }
    }

    // Class lưu thông tin khung giờ bảo trì
    public static class MaintenanceSchedule {
        public int hour;
        public int minute;

        public MaintenanceSchedule(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }

        @Override
        public String toString() {
            return String.format("%02d:%02d", hour, minute);
        }
    }

}
