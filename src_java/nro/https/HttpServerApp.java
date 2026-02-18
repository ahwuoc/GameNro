package nro.https;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerApp {
    private static HttpServer server;

    // ----------------- KHỞI ĐỘNG SERVER -----------------
    public static void start(int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new SimpleHttpHandler());
            server.setExecutor(Executors.newFixedThreadPool(8)); // xử lý song song
            server.start();
            System.out.println("✅ HTTP server running on port " + port);
        } catch (Exception e) {
            System.err.println("❌ Failed to start HTTP server: " + e.getMessage());
        }
    }

    // ----------------- DỪNG SERVER -----------------
    public static void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("🛑 HTTP server stopped");
        }
    }

    // ----------------- MAIN TEST -----------------
    public static void main(String[] args) {
        HttpServerApp.start(8899); // chạy server tại cổng 8888
    }
}
