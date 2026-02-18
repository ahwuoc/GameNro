package nro.server;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class SystemMetrics {
    public static String getMemoryInfo() {
        Runtime rt = Runtime.getRuntime();
        return String.format("RAM[Used/Total]: %d/%dMB",
                (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024),
                rt.totalMemory() / (1024 * 1024));
    }

    public static String getCpuInfo() {
        OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return String.format("CPU[Proc/Sys]: %.1f%%/%.1f%% | Cores: %d",
                os.getProcessCpuLoad() * 100,
                os.getCpuLoad() * 100,
                os.getAvailableProcessors());
    }

    public static String getSystemMetrics() {
        return getMemoryInfo() + " | " + getCpuInfo();
    }
}