package nro.https;

import com.sun.net.httpserver.HttpServer;
import nro.server.Manager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class SimpleHttpServerThread extends Thread {

    private HttpServer server;

    @Override
    public void run() {
        int port = Manager.apiPort;
        while (true) {
            try {
                // Tạo server
                server = HttpServer.create(new InetSocketAddress(port), 0);
                
                // Đăng ký Handler (Sử dụng HttpDashboardHandler mới)
                server.createContext("/", new HttpDashboardHandler());
                
                // Cấu hình Thread Pool
                server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Manager.workerGroup))); // Tăng thread lên chút
                server.start();
                
                System.out.println(">> [WEB ADMIN] Started at: http://localhost:" + port + "/admin");
                break; 

            } catch (IOException e) {
                System.out.println("⚠️ Port " + port + " busy, trying " + (port + 1));
                port++;
                if (port - Manager.apiPort > 10) {
                    System.err.println("❌ Cannot bind web admin port!");
                    break;
                }
            }
        }
        
        // Giữ thread sống
        try {
            while (!Thread.currentThread().isInterrupted()) Thread.sleep(10000);
        } catch (InterruptedException e) {
            shutdown();
        }
    }

    public void shutdown() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 [WEB ADMIN] Stopped");
        }
    }
}