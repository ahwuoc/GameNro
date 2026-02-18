package nro.recharge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jdbc.DBConnecter;
import nro.player.Player;
import nro.server.Client;
import nro.services.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RechargeHttp {

    private static final int PORT = 8080;
    private static final String WEBHOOK_KEY = "BARCOLLxENZEEFXNRO"; // Thay đúng key của bạn
    
    // Config Sự kiện nạp (1.0 = x1, 2.0 = x2...)
    private static final double HE_SO_SU_KIEN = 1.0; 

    // Regex tìm cú pháp: Chấp nhận "NAP123", "NAP 123", "NAP_123", "nro 123"...
    // Group 1 sẽ là ID tài khoản
    private static final Pattern PATTERN_ID = Pattern.compile("(?:NAP|NRO)[\\s_\\-]*(\\d+)", Pattern.CASE_INSENSITIVE);

    public static void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/sepay", new SepayHandler());
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool()); // Xử lý đa luồng
            server.start();
            System.out.println(">> [Recharge] Webhook started on PORT: " + PORT);
        } catch (Exception e) {
            System.err.println(">> [Recharge] Error starting webhook: " + e.getMessage());
        }
    }

    static class SepayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) {
            try {
                String method = ex.getRequestMethod();
                
                // 1. Chỉ chấp nhận POST
                if (!"POST".equalsIgnoreCase(method)) {
                    sendResponse(ex, 405, "Method Not Allowed");
                    return;
                }

                // 2. Check API Key bảo mật
                String authHeader = ex.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.equals("Apikey " + WEBHOOK_KEY)) {
                    System.out.println(">> [Recharge] Hack attempt detected! Wrong Key.");
                    sendResponse(ex, 401, "Unauthorized");
                    return;
                }

                // 3. Đọc dữ liệu Body
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                System.out.println(">> [Recharge] Received Payload: " + body);

                // 4. Parse JSON
                JsonObject json;
                try {
                    // Cú pháp cũ (Gson < 2.8.6), nếu lỗi thì dùng JsonParser.parseString(body)
                    json = new JsonParser().parse(body).getAsJsonObject();
                } catch (Exception e) {
                    System.err.println(">> [Recharge] JSON Parse Error: " + e.getMessage());
                    sendResponse(ex, 400, "Invalid JSON");
                    return;
                }

                // 5. Lấy số tiền (Hỗ trợ nhiều format của SePay)
                int amount = 0;
                if (isValidMember(json, "transferAmount")) amount = json.get("transferAmount").getAsInt();
                else if (isValidMember(json, "amount_in")) amount = json.get("amount_in").getAsInt();
                else if (isValidMember(json, "amount")) amount = json.get("amount").getAsInt();

                // 6. Lấy nội dung chuyển khoản
                String description = "";
                if (isValidMember(json, "transaction_content")) description = json.get("transaction_content").getAsString();
                else if (isValidMember(json, "content")) description = json.get("content").getAsString();
                else if (isValidMember(json, "description")) description = json.get("description").getAsString();

                // 7. Lấy mã giao dịch
                String transId = "";
                if (isValidMember(json, "transaction_id")) transId = json.get("transaction_id").getAsString();
                else if (isValidMember(json, "referenceCode")) transId = json.get("referenceCode").getAsString();
                else if (isValidMember(json, "id")) transId = json.get("id").getAsString();

                // 8. Tách ID tài khoản từ nội dung
                Matcher m = PATTERN_ID.matcher(description);
                int accountId = -1;
                if (m.find()) {
                    accountId = Integer.parseInt(m.group(1));
                }

                System.out.println(">> [Process] AccID: " + accountId + " | Amount: " + amount + " | TransID: " + transId);

                // 9. Xử lý cộng tiền
                if (accountId > 0 && amount > 0) {
                    boolean success = processTopup(accountId, amount);
                    
                    // Lưu log lại dù thành công hay thất bại
                    logTransaction(transId, accountId, amount, description, success);
                    
                    if (success) {
                        sendResponse(ex, 200, "{\"success\":true,\"message\":\"Topup success\"}");
                    } else {
                        // Trả về 200 để SePay không gửi lại nữa (vì lỗi do logic server, ko phải lỗi mạng)
                        sendResponse(ex, 200, "{\"success\":false,\"message\":\"Account not found\"}"); 
                    }
                } else {
                    System.out.println(">> [Recharge] Invalid data (No ID or Amount 0)");
                    logTransaction(transId, -1, amount, description + " (Invalid Data)", false);
                    sendResponse(ex, 200, "Invalid Data Parsed");
                }

            } catch (Exception e) {
                e.printStackTrace();
                try { sendResponse(ex, 500, "Server Error"); } catch (Exception ignored) {}
            }
        }
    }

    private static boolean processTopup(int accountId, int amount) {
        int realAmount = (int) (amount * HE_SO_SU_KIEN);
        
        Connection con = null;
        PreparedStatement ps = null;
        boolean result = false;

        try {
            con = DBConnecter.getConnectionServer();
            if (con == null) return false;

            // Kiểm tra xem account có tồn tại không
            ps = con.prepareStatement("SELECT id FROM account WHERE id = ? LIMIT 1");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return false; // Không tìm thấy acc
            }
            rs.close();
            ps.close();

            // Thực hiện cộng tiền
            // CASH: Tiền dùng để mua đồ (đã nhân khuyến mãi)
            // danap: Tổng nạp tích lũy (giữ nguyên gốc để tính mốc nạp)
            String sql = "UPDATE account SET cash = cash + ?, danap = danap + ? WHERE id = ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, realAmount);
            ps.setInt(2, amount);
            ps.setInt(3, accountId);
            
            if (ps.executeUpdate() > 0) {
                result = true;
                System.out.println(">> [DB] Update Success: Acc " + accountId + " + " + realAmount + " CASH");
                
                // Notify Player Online
                notifyPlayer(accountId, amount, realAmount);
            }
        } catch (Exception e) {
            System.err.println(">> [DB Error] " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
        return result;
    }

    private static void notifyPlayer(int accountId, int amountGoc, int amountNhan) {
        try {
            // Lấy người chơi đang online qua account_id
            // Lưu ý: Hàm này tùy source, check kỹ bên Client hoặc Manager
            Player pl = Client.gI().getPlayerByUser(accountId); 
            
            if (pl != null) {
                // Cập nhật số dư trong bộ nhớ RAM (nếu source có lưu)
                if (pl.getSession() != null) {
                    pl.getSession().cash += amountNhan;
                    // pl.getSession().danap += amountGoc; // Nếu có biến này
                }
                
                // Gửi thông báo
                Service.gI().sendThongBao(pl, 
                    "Nạp thành công " + formatMoney(amountGoc) + "\n" +
                    "Bạn nhận được: " + formatMoney(amountNhan) + " CASH (x" + HE_SO_SU_KIEN + ")");
                
                // Cập nhật tiền hiển thị ở UI
                Service.gI().sendMoney(pl); 
            }
        } catch (Exception e) {
            System.err.println(">> [Notify Error] " + e.getMessage());
        }
    }

    private static void logTransaction(String transId, int accId, int amount, String desc, boolean status) {
        // Yêu cầu Database phải có bảng `recharge_log`
        // CREATE TABLE `recharge_log` ( `id` int(11) NOT NULL AUTO_INCREMENT, `trans_id` varchar(100), `account_id` int(11), `amount` int(11), `description` text, `status` int(1), `created_at` timestamp DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`id`) );
        String sql = "INSERT INTO recharge_log (trans_id, account_id, amount, description, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnecter.getConnectionServer();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, transId);
            ps.setInt(2, accId);
            ps.setInt(3, amount);
            ps.setString(4, desc);
            ps.setInt(5, status ? 1 : 0);
            ps.executeUpdate();
        } catch (Exception e) {
            // Chỉ log ra console nếu không insert được log, không làm ảnh hưởng luồng chính
            System.err.println(">> [Log DB Error] Cannot save log: " + e.getMessage());
        }
    }

    private static void sendResponse(HttpExchange ex, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static boolean isValidMember(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull();
    }
    
    private static String formatMoney(int money) {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(money);
    }
}