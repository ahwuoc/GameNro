package zalo.services;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import zalo.utils.Json;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class NroHttpServer {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static NroHttpServer instance;
    private HttpServer server;
    
    private NroHttpServer() {
    }
    
    public static NroHttpServer gI() {
        if (instance == null) {
            instance = new NroHttpServer();
        }
        return instance;
    }
    
    public void start(int port) {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/nro/boss", new BossNotifyHandler());
            server.createContext("/nro/baotri", new BaoTriHandler());
            server.setExecutor(null);
            server.start();
        } catch (IOException e) {
        }
    }
    
    public void start() {
        start(8888);
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
    
    private static class BossNotifyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if ("GET".equals(method)) {
                String response = "NRO Boss Notify Service is running!\n\n" +
                                "Endpoint: POST /nro/boss\n" +
                                "Content-Type: application/json\n\n" +
                                "Example request body:\n" +
                                "{\n" +
                                "  \"bossName\": \"Broly\",\n" +
                                "  \"mapName\": \"Namek\",\n" +
                                "  \"zoneInfo\": \"khu 2\"\n" +
                                "}\n\n" +
                                "Registered groups: " + NroNotifyService.gI().getRegisteredCount();
                sendResponse(exchange, 200, response);
                return;
            }
            
            if (!"POST".equals(method)) {
                sendResponse(exchange, 405, "Method not allowed. Use POST or GET");
                return;
            }
            
            try {
                byte[] requestBytes = exchange.getRequestBody().readAllBytes();
                String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
                
                if (requestBody == null || requestBody.trim().isEmpty()) {
                    sendResponse(exchange, 400, "Empty request body");
                    return;
                }
                
                Object parsed = Json.parse(requestBody);
                
                if (!(parsed instanceof Map)) {
                    sendResponse(exchange, 400, "Invalid JSON");
                    return;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) parsed;
                
                String bossName = data.containsKey("bossName") ? String.valueOf(data.get("bossName")) : "Unknown";
                String mapName = data.containsKey("mapName") ? String.valueOf(data.get("mapName")) : "Unknown";
                String zoneInfo = data.containsKey("zoneInfo") ? String.valueOf(data.get("zoneInfo")) : null;
                
                if ("Unknown".equals(bossName) || "Unknown".equals(mapName)) {
                     sendResponse(exchange, 400, "Missing required fields: bossName, mapName");
                    return;
                }
                
                if (zoneInfo != null && !zoneInfo.isEmpty()) {
                    NroNotifyService.gI().notifyBossSpawn(bossName, mapName, zoneInfo);
                } else {
                    NroNotifyService.gI().notifyBossSpawn(bossName, mapName);
                }
                
                sendResponse(exchange, 200, "OK");
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal server error");
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    private static class BaoTriHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if ("GET".equals(method)) {
                String response = "NRO Maintenance Service\n\n" +
                                "Endpoint: POST /nro/baotri\n" +
                                "Content-Type: application/json\n\n" +
                                "Example request body:\n" +
                                "{\n" +
                                "  \"minutes\": 120\n" +
                                "}\n";
                sendResponse(exchange, 200, response);
                return;
            }
            
            if (!"POST".equals(method)) {
                sendResponse(exchange, 405, "Method not allowed. Use POST or GET");
                return;
            }
            
            try {
                byte[] requestBytes = exchange.getRequestBody().readAllBytes();
                String requestBody = new String(requestBytes, StandardCharsets.UTF_8);
                
                if (requestBody == null || requestBody.trim().isEmpty()) {
                    sendResponse(exchange, 400, "Empty request body");
                    return;
                }
                
                Object parsed = Json.parse(requestBody);
                
                if (!(parsed instanceof Map)) {
                    sendResponse(exchange, 400, "Invalid JSON");
                    return;
                }
                
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) parsed;
                
                Object minutesObj = data.get("minutes");
                if (minutesObj == null) {
                    sendResponse(exchange, 400, "Missing required field: minutes");
                    return;
                }
                
                int minutes;
                try {
                    if (minutesObj instanceof Number) {
                        minutes = ((Number) minutesObj).intValue();
                    } else {
                        minutes = Integer.parseInt(String.valueOf(minutesObj));
                    }
                } catch (NumberFormatException e) {
                    sendResponse(exchange, 400, "Invalid minutes value. Must be integer.");
                    return;
                }
                
                if (minutes <= 0) {
                    sendResponse(exchange, 400, "Minutes must be positive integer.");
                    return;
                }
                
                boolean success = NroMaintenanceService.gI().startMaintenance(minutes);
                
                if (success) {
                    sendResponse(exchange, 200, "OK");
                } else {
                    sendResponse(exchange, 500, "Failed to start maintenance");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal server error");
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}

