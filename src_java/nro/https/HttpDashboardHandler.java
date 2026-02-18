package nro.https;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.management.OperatingSystemMXBean;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

// Import từ Source game
import nro.server.Client;
import nro.server.Maintenance;
import nro.server.Manager;
import nro.server.AutoSaveManager;
import nro.services.Service;
import boss.BossManager;
import boss.BossID;
import boss.Boss;
import boss.BossStatus;
import models.GiftCode.GiftCodeManager;
import models.kygui.ConsignShopManager;
import network.SessionManager;
import jdbc.DBConnecter;
import jdbc.daos.PlayerDAO;
import nro.player.Player;

public class HttpDashboardHandler implements HttpHandler {

    private final Instant serverStartTime = Instant.now();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String method = exchange.getRequestMethod();
        String response = "";
        int statusCode = 200;

        try {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            // --- ROUTING ---
            if (path.equals("/admin") || path.equals("/")) {
                response = getAdminPageHTML();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } 
            // --- API: STATS ---
            else if (path.equals("/api/stats")) {
                response = getSystemStats();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            } 
            // --- API: BOSS LIST ---
            else if (path.equals("/api/boss-ids")) {
                response = getBossIdList();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            }
            // --- API: PLAYERS ---
            else if (path.equals("/api/players")) {
                String searchName = "";
                if (query != null && query.contains("search=")) {
                    try {
                        searchName = query.split("search=")[1].split("&")[0];
                        searchName = java.net.URLDecoder.decode(searchName, StandardCharsets.UTF_8);
                    } catch (Exception e) {}
                }
                response = getPlayerListDB(searchName);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            }
            // --- API: PLAYER DETAIL ---
            else if (path.equals("/api/player-detail")) {
                String idStr = "0";
                if(query != null && query.contains("id=")) idStr = query.split("id=")[1].split("&")[0];
                response = getPlayerDetailDB(Integer.parseInt(idStr));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            }
            // --- API: ACTIONS (POST) ---
            else if (path.equals("/api/action") && method.equals("POST")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                response = handleAction(body);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            } 
            else {
                statusCode = 404;
                response = "404 Not Found";
            }

        } catch (Exception e) {
            statusCode = 500;
            response = "{\"error\": \"" + e.getMessage() + "\"}";
            e.printStackTrace();
        }

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ================== LOGIC API ==================

    private String getPlayerListDB(String keyword) {
        StringBuilder json = new StringBuilder("[");
        String sql = "SELECT p.id, p.name, p.power, p.data_inventory, a.cash, a.danap, a.ban " +
                     "FROM player p LEFT JOIN account a ON p.account_id = a.id ";
        
        if (keyword != null && !keyword.isEmpty()) {
            sql += "WHERE p.name LIKE ? ";
        }
        sql += "ORDER BY p.id DESC LIMIT 20";

        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            if (keyword != null && !keyword.isEmpty()) {
                ps.setString(1, "%" + keyword + "%");
            }

            ResultSet rs = ps.executeQuery();
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                
                long gold = 0, gem = 0;
                try {
                    JsonArray inv = new JsonParser().parse(rs.getString("data_inventory")).getAsJsonArray();
                    gold = inv.get(0).getAsLong();
                    gem = inv.get(1).getAsLong();
                } catch (Exception e) {}

                json.append(String.format(
                    "{\"id\":%d, \"name\":\"%s\", \"power\":%d, \"gold\":%d, \"gem\":%d, \"cash\":%d, \"ban\":%d}",
                    rs.getInt("id"), rs.getString("name"), rs.getLong("power"),
                    gold, gem, rs.getLong("cash"), rs.getInt("ban")
                ));
                first = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
        json.append("]");
        return json.toString();
    }

    private String getPlayerDetailDB(int playerId) {
        String sql = "SELECT p.*, a.cash, a.danap, a.active, a.ban " +
                     "FROM player p LEFT JOIN account a ON p.account_id = a.id WHERE p.id = ?";
        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String inventory = rs.getString("data_inventory");
                String task = rs.getString("data_task");
                String point = rs.getString("data_point");
                
                return String.format(
                    "{\"id\":%d, \"account_id\":%d, \"name\":\"%s\", \"power\":%d, \"head\":%d, " +
                    "\"cash\":%d, \"danap\":%d, \"active\":%d, \"ban\":%d, " +
                    "\"inventory\":%s, \"task\":%s, \"point\":%s}",
                    rs.getInt("id"), rs.getInt("account_id"), rs.getString("name"), rs.getLong("power"), rs.getInt("head"),
                    rs.getLong("cash"), rs.getLong("danap"), rs.getInt("active"), rs.getInt("ban"),
                    (inventory == null ? "[]" : inventory),
                    (task == null ? "[]" : task),
                    (point == null ? "[]" : point)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Lỗi DB: " + e.getMessage() + "\"}";
        }
        return "{}";
    }

    private String getSystemStats() {
        double cpu = 0;
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            cpu = osBean.getProcessCpuLoad() * 100;
        } catch (Exception e) { cpu = 0; } // Fallback cho Android/Linux nếu thiếu thư viện

        long ramUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long ramTotal = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        int online = (Client.gI() != null) ? Client.gI().getPlayers().size() : 0;
        int threads = Thread.activeCount();
        int sessions = (SessionManager.gI() != null) ? SessionManager.gI().getSessions().size() : 0;
        int giftCodes = (GiftCodeManager.gI() != null) ? GiftCodeManager.gI().listGiftCode.size() : 0;
        int consignItems = (ConsignShopManager.gI() != null) ? ConsignShopManager.gI().listItem.size() : 0;
        
        Duration d = Duration.between(serverStartTime, Instant.now());
        String uptime = String.format("%dd %02dh %02dm", d.toDays(), d.toHoursPart(), d.toMinutesPart());

        String bossStatStr = "N/A";
        if (BossManager.gI() != null) {
            try {
                int[] stats = BossManager.gI().getBossStatusCounts();
                bossStatStr = String.format("%d Sống / %d Chờ", stats[0], stats[1]);
            } catch(Exception e){}
        }

        return String.format(
            "{\"cpu\": %.1f, \"ramUsed\": %d, \"ramTotal\": %d, \"online\": %d, \"threads\": %d, \"sessions\": %d, \"giftcodes\": %d, \"consign\": %d, \"uptime\": \"%s\", \"bossStats\": \"%s\", \"expRate\": %d}", 
            cpu, ramUsed, ramTotal, online, threads, sessions, giftCodes, consignItems, uptime, bossStatStr, Manager.RATE_EXP_SERVER
        );
    }

    private String getBossIdList() {
        List<String> list = new ArrayList<>();
        try {
            for (Field field : BossID.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.getType() == int.class) {
                    String name = field.getName().replace("_", " ").toLowerCase();
                    name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    int id = field.getInt(null);
                    list.add(String.format("{\"id\":%d, \"name\":\"%s\"}", id, name));
                }
            }
        } catch (Exception e) {}
        return "[" + String.join(",", list) + "]";
    }

    private String handleAction(String body) {
        String action = "";
        try {
            Map<String, String> params = new HashMap<>();
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    params.put(kv[0], java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
            action = params.getOrDefault("action", "");
            String param = params.getOrDefault("param", "");

            switch (action) {
                case "update_player":
                    return updatePlayer(params);
                case "buff_item":
                    return buffItem(params);
                case "kick":
                    if(Client.gI() != null) Client.gI().kickSession(Client.gI().getPlayer(Integer.parseInt(param)).getSession());
                    return "{\"msg\": \"Đã gửi lệnh Kick (ID: " + param + ")\"}";

                case "maintenance":
                    int seconds = 60;
                    try { seconds = Integer.parseInt(param); } catch(Exception e){}
                    Maintenance.gI().start(seconds);
                    return "{\"msg\": \"Đã kích hoạt bảo trì (" + seconds + "s)\"}";

                case "restart":
                    new Thread(() -> {
                        try {
                            if (Client.gI() != null) {
                                System.out.println("Web Admin: Restart triggered...");
                                Client.gI().close(); 
                                Client.gI().getPlayers().forEach(p -> {
                                    try { PlayerDAO.updatePlayer(p); } catch (Exception ex) {}
                                });
                            }
                            // Windows Command. Nếu chạy Linux/Android cần đổi lệnh này
                            String cmd = "cmd /c start cmd /c \"title AUTO RESTART & mode 60,12 & color 0e & echo SYSTEM RESTARTING... & timeout /t 10 & start run.bat\"";
                            try { Runtime.getRuntime().exec(cmd); } catch(Exception ex) { System.exit(0); }
                            
                            Thread.sleep(1000);
                            System.exit(0);
                        } catch (Exception e) { e.printStackTrace(); }
                    }).start();
                    return "{\"msg\": \"Server đang khởi động lại...\"}";

                case "save_data":
                    if (Client.gI() != null) {
                         Client.gI().getPlayers().forEach(p -> {
                            try { PlayerDAO.updatePlayer(p); } catch (Exception e) {}
                        });
                    }
                    return "{\"msg\": \"Đã lưu dữ liệu người chơi!\"}";

                case "reset_boss":
                    if (BossManager.gI() != null) BossManager.gI().resetAllBosses();
                    return "{\"msg\": \"Đã Reset toàn bộ Boss!\"}";
                
                case "respawn_boss":
                    if (BossManager.gI() != null) BossManager.gI().respawnAllRestingBosses();
                    return "{\"msg\": \"Đã hồi sinh các Boss đang chờ!\"}";

                case "summon_boss":
                    int bossId = Integer.parseInt(param);
                    Boss b = BossManager.gI().createBoss(bossId);
                    b.changeStatus(BossStatus.RESPAWN);
                    return "{\"msg\": \"Đã triệu hồi Boss ID: " + bossId + "\"}";

                case "set_exp":
                    Manager.RATE_EXP_SERVER = Integer.parseInt(param);
                    Service.gI().sendThongBaoAllPlayer("Server thay đổi Exp Rate: x" + param);
                    return "{\"msg\": \"Đã cập nhật EXP Rate: x" + param + "\"}";

                case "toggle_autosave":
                    if (param.equals("off")) AutoSaveManager.getInstance().stopAutoSave();
                    else AutoSaveManager.getInstance().startAutoSave();
                    return "{\"msg\": \"Đã đổi trạng thái AutoSave: " + param + "\"}";
                
                case "reload_config":
                    if(param.equals("giftcode")) GiftCodeManager.gI().listGiftCode.clear();
                    return "{\"msg\": \"Reload " + param + " done\"}";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Lỗi Server: " + e.getMessage() + "\"}";
        }
        return "{\"msg\": \"Action done\"}";
    }

    private String updatePlayer(Map<String, String> p) {
        int pid = Integer.parseInt(p.get("id"));
        int accId = Integer.parseInt(p.get("account_id"));
        
        try (Connection conn = DBConnecter.getConnectionServer()) {
            String sqlGet = "SELECT data_inventory, data_task FROM player WHERE id=?";
            PreparedStatement psGet = conn.prepareStatement(sqlGet);
            psGet.setInt(1, pid);
            ResultSet rs = psGet.executeQuery();
            JsonArray invArr = new JsonArray();
            JsonArray taskArr = new JsonArray();

            if (rs.next()) {
                try { invArr = new JsonParser().parse(rs.getString("data_inventory")).getAsJsonArray(); } catch(Exception e){}
                try { taskArr = new JsonParser().parse(rs.getString("data_task")).getAsJsonArray(); } catch(Exception e){}
            }
            
            long gold = Long.parseLong(p.get("gold"));
            long gem = Long.parseLong(p.get("gem"));
            if(invArr.size() < 2) { invArr.add(0); invArr.add(0); }
            invArr.set(0, new JsonPrimitive(gold));
            invArr.set(1, new JsonPrimitive(gem));

            int taskId = Integer.parseInt(p.get("task_id"));
            int taskIndex = Integer.parseInt(p.get("task_index"));
            int taskCount = Integer.parseInt(p.get("task_count"));
            if(taskArr.size() < 3) { taskArr.add(0); taskArr.add(0); taskArr.add(0); }
            taskArr.set(0, new JsonPrimitive(taskId));
            taskArr.set(1, new JsonPrimitive(taskIndex));
            taskArr.set(2, new JsonPrimitive(taskCount));

            String sqlPlayer = "UPDATE player SET power=?, data_inventory=?, data_task=? WHERE id=?";
            PreparedStatement psP = conn.prepareStatement(sqlPlayer);
            psP.setLong(1, Long.parseLong(p.get("power")));
            psP.setString(2, invArr.toString());
            psP.setString(3, taskArr.toString());
            psP.setInt(4, pid);
            psP.executeUpdate();

            String sqlAcc = "UPDATE account SET cash=?, danap=?, active=?, ban=? WHERE id=?";
            PreparedStatement psA = conn.prepareStatement(sqlAcc);
            psA.setLong(1, Long.parseLong(p.get("cash")));
            psA.setLong(2, Long.parseLong(p.get("danap")));
            psA.setInt(3, Integer.parseInt(p.get("active")));
            psA.setInt(4, Integer.parseInt(p.get("ban")));
            psA.setInt(5, accId);
            psA.executeUpdate();
            
            if(Client.gI() != null) {
                Player pl = Client.gI().getPlayer(pid);
                if(pl != null) Client.gI().kickSession(pl.getSession());
            }

            return "{\"msg\": \"Cập nhật thành công! Player đã được kick để reload.\"}";
        } catch (Exception e) {
            return "{\"error\": \"Lỗi SQL: " + e.getMessage() + "\"}";
        }
    }

    private String buffItem(Map<String, String> p) {
        int pid = Integer.parseInt(p.get("id"));
        int itemId = Integer.parseInt(p.get("item_id"));
        int quantity = Integer.parseInt(p.get("quantity"));
        String options = "[]"; 

        try (Connection conn = DBConnecter.getConnectionServer()) {
            String sql = "SELECT items_bag FROM player WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                JsonArray bag = new JsonParser().parse(rs.getString("items_bag")).getAsJsonArray();
                boolean added = false;
                for (int i = 0; i < bag.size(); i++) {
                    JsonArray item = bag.get(i).getAsJsonArray();
                    if (item.get(0).getAsInt() == -1) { 
                        item.set(0, new JsonPrimitive(itemId));
                        item.set(1, new JsonPrimitive(quantity));
                        item.set(2, new JsonParser().parse(options));
                        item.set(3, new JsonPrimitive(System.currentTimeMillis()));
                        bag.set(i, item);
                        added = true;
                        break;
                    }
                }

                if (added) {
                    String updateSql = "UPDATE player SET items_bag=? WHERE id=?";
                    PreparedStatement psUp = conn.prepareStatement(updateSql);
                    psUp.setString(1, bag.toString());
                    psUp.setInt(2, pid);
                    psUp.executeUpdate();
                    return "{\"msg\": \"Đã thêm item " + itemId + " x" + quantity + " vào hành trang.\"}";
                } else {
                    return "{\"error\": \"Hành trang đầy!\"}";
                }
            }
        } catch (Exception e) {
            return "{\"error\": \"Lỗi Buff: " + e.getMessage() + "\"}";
        }
        return "{\"error\": \"Không tìm thấy player\"}";
    }

    // ================== FRONTEND HTML ==================
    private String getAdminPageHTML() {
        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>NRO Server Manager</title>
            <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
            <style>
                :root { --bg: #f4f6f9; --dark: #343a40; --blue: #007bff; --green: #28a745; --red: #dc3545; }
                body { font-family: 'Segoe UI', sans-serif; margin: 0; background: var(--bg); display: flex; height: 100vh; overflow: hidden; }
                
                /* Sidebar */
                .sidebar { width: 250px; background: var(--dark); color: #fff; padding: 20px; display: flex; flex-direction: column; flex-shrink: 0; overflow-y: auto; }
                .brand { font-size: 24px; font-weight: bold; margin-bottom: 30px; text-align: center; color: var(--blue); }
                .menu-item { padding: 12px; cursor: pointer; border-radius: 5px; margin-bottom: 5px; transition: 0.2s; white-space: nowrap; }
                .menu-item:hover, .menu-item.active { background: var(--blue); }
                .menu-item i { margin-right: 10px; width: 20px; text-align: center; }

                /* Content */
                .content { flex: 1; padding: 20px; overflow-y: auto; position: relative; }
                .panel { display: none; animation: fadeIn 0.3s; }
                .panel.active { display: block; }
                @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
                
                /* Cards */
                .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 15px; margin-bottom: 20px; }
                .card { background: #fff; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
                .card h3 { margin: 0 0 5px 0; color: #666; font-size: 13px; text-transform: uppercase; }
                .card .val { font-size: 20px; font-weight: bold; color: var(--dark); word-break: break-all; }

                /* Tables */
                .table-container { background: #fff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); overflow-x: auto; -webkit-overflow-scrolling: touch; }
                table { width: 100%; border-collapse: collapse; min-width: 600px; } /* Min width to force scroll */
                th, td { padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; white-space: nowrap; }
                th { background: #f8f9fa; font-weight: 600; color: #666; position: sticky; top: 0; }
                tr:hover { background: #f1f1f1; }
                
                /* Controls */
                .search-box { margin-bottom: 20px; display: flex; gap: 10px; flex-wrap: wrap; }
                input, select { padding: 10px; border: 1px solid #ddd; border-radius: 4px; outline: none; }
                .btn { padding: 10px 15px; border: none; border-radius: 4px; color: #fff; cursor: pointer; font-weight: 500; white-space: nowrap; }
                .btn:hover { opacity: 0.9; }
                .btn-primary { background: var(--blue); }
                .btn-success { background: var(--green); }
                .btn-danger { background: var(--red); }
                .btn-warning { background: #ffc107; color: #212529; }
                .btn-sm { padding: 5px 10px; font-size: 12px; }

                /* Modal */
                .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); justify-content: center; align-items: flex-start; z-index: 1000; padding-top: 5vh; overflow-y: auto; }
                .modal.show { display: flex; }
                .modal-content { background: #fff; width: 95%; max-width: 600px; margin-bottom: 50px; border-radius: 8px; padding: 20px; animation: slideDown 0.3s; box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
                @keyframes slideDown { from {transform: translateY(-20px); opacity: 0;} to {transform: translateY(0); opacity: 1;} }
                .form-group { margin-bottom: 15px; }
                .form-group label { display: block; margin-bottom: 5px; font-weight: 500; color: #555; font-size: 14px; }
                .form-group input, .form-group select { width: 100%; box-sizing: border-box; height: 40px; }
                .modal-header { display: flex; justify-content: space-between; margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
                .close { font-size: 28px; cursor: pointer; color: #aaa; line-height: 20px; }
                .grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
                
                .tag { padding: 3px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; }
                .tag-green { background: #d4edda; color: #155724; }
                .tag-red { background: #f8d7da; color: #721c24; }

                /* === RESPONSIVE MOBILE === */
                @media (max-width: 768px) {
                    body { flex-direction: column; height: auto; min-height: 100vh; }
                    .sidebar { width: 100%; height: auto; flex-direction: row; padding: 10px; overflow-x: auto; box-sizing: border-box; align-items: center; }
                    .brand { font-size: 18px; margin: 0 15px 0 0; white-space: nowrap; }
                    .menu-item { margin: 0 5px; padding: 8px 12px; font-size: 14px; }
                    .menu-item i { margin-right: 5px; }
                    
                    .content { padding: 10px; height: auto; overflow: visible; }
                    .grid-2 { grid-template-columns: 1fr; } /* Stack inputs */
                    .search-box { flex-direction: column; }
                    .search-box input, .search-box select, .search-box button { width: 100%; }
                    
                    .modal { align-items: center; padding-top: 0; }
                    .modal-content { max-height: 90vh; overflow-y: auto; width: 90%; }
                }
            </style>
        </head>
        <body>
            <div class="sidebar">
                <div class="brand"><i class="fas fa-dragon"></i> ADMIN</div>
                <div class="menu-item active" onclick="switchTab('dashboard')"><i class="fas fa-chart-line"></i> Dash</div>
                <div class="menu-item" onclick="switchTab('players')"><i class="fas fa-users"></i> Player</div>
                <div class="menu-item" onclick="switchTab('config')"><i class="fas fa-cogs"></i> Boss</div>
                <div class="menu-item" onclick="switchTab('actions')"><i class="fas fa-bolt"></i> Tools</div>
            </div>

            <div class="content">
                <div id="dashboard" class="panel active">
                    <h2>Tổng quan hệ thống</h2>
                    <div class="stats-grid">
                        <div class="card"><h3>CPU</h3><div class="val" id="cpuVal">0%</div></div>
                        <div class="card"><h3>RAM</h3><div class="val" id="ramVal">0 MB</div></div>
                        <div class="card"><h3>Online</h3><div class="val" id="onlineVal">0</div></div>
                        <div class="card"><h3>Threads</h3><div class="val" id="threadVal">0</div></div>
                    </div>
                    <div class="stats-grid">
                        <div class="card"><h3>Giftcodes</h3><div class="val" id="gcVal">0</div></div>
                        <div class="card"><h3>Ký Gửi</h3><div class="val" id="consignVal">0</div></div>
                        <div class="card"><h3>Uptime</h3><div class="val" id="uptimeVal">--</div></div>
                        <div class="card"><h3>EXP Rate</h3><div class="val" id="expDisplay">x1</div></div>
                    </div>
                    <div class="card" style="margin-top:10px">
                         <h3>Boss Status</h3>
                         <div class="val" style="font-size:16px" id="bossStatsVal">Loading...</div>
                    </div>
                </div>

                <div id="players" class="panel">
                    <h2>Quản lý Player</h2>
                    <div class="search-box">
                        <input type="text" id="searchInp" placeholder="Tìm tên nhân vật (Enter)..." style="flex:1" onkeypress="if(event.key==='Enter') loadPlayers()">
                        <button class="btn btn-primary" onclick="loadPlayers()"><i class="fas fa-search"></i> Tìm</button>
                    </div>
                    <div class="table-container">
                        <table>
                            <thead><tr><th>ID</th><th>Tên</th><th>Sức mạnh</th><th>Vàng</th><th>Ngọc</th><th>VND</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                            <tbody id="playerTable"><tr><td colspan="8" style="text-align:center">Nhập tên để tìm kiếm...</td></tr></tbody>
                        </table>
                    </div>
                </div>
                
                <div id="config" class="panel">
                    <h2>Cấu hình & Boss</h2>
                    <div class="card" style="margin-bottom:20px">
                        <h3>Triệu hồi Boss</h3>
                        <div class="search-box" style="margin-bottom:0">
                            <select id="bossSelect" style="flex:1"><option>Đang tải...</option></select>
                            <button class="btn btn-success" onclick="summonBoss()">Triệu hồi ngay</button>
                        </div>
                    </div>
                    <div class="card">
                        <h3>Cài đặt Server</h3>
                        <div class="search-box" style="margin-bottom:10px">
                            <input type="number" id="expInput" value="1" placeholder="Exp Rate">
                            <button class="btn btn-primary" onclick="setExp()">Set EXP</button>
                            <button class="btn btn-danger" onclick="toggleAutoSave()" id="btnAutoSave">AutoSave: ON</button>
                        </div>
                    </div>
                </div>
                
                <div id="actions" class="panel">
                    <h2>Chức năng Server</h2>
                    <div class="stats-grid">
                        <div class="card">
                            <h3>Hệ thống</h3>
                            <button class="btn btn-danger" style="width:100%; margin-bottom:10px" onclick="doRestart()">⚡ KHỞI ĐỘNG LẠI SERVER</button>
                            <input type="number" id="maintTime" value="60" style="width:100%; margin-bottom:10px; box-sizing:border-box" placeholder="Số giây">
                            <button class="btn btn-warning" style="width:100%" onclick="doMaintenance()">Bảo trì (Đếm ngược)</button>
                        </div>
                        <div class="card">
                            <h3>Quản lý Boss</h3>
                            <button class="btn btn-primary" style="width:100%; margin-bottom:10px" onclick="doAction('reset_boss')">Reset Toàn bộ Boss</button>
                            <button class="btn btn-success" style="width:100%" onclick="doAction('respawn_boss')">Hồi sinh Boss Chờ</button>
                        </div>
                        <div class="card">
                            <h3>Dữ liệu</h3>
                            <button class="btn btn-primary" style="width:100%; margin-bottom:10px" onclick="doAction('save_data')">Lưu Dữ liệu Player</button>
                            <button class="btn btn-primary" style="background:#6610f2; width:100%" onclick="doAction('reload_config&param=giftcode')">Reload Giftcode</button>
                        </div>
                    </div>
                </div>
            </div>

            <div id="editModal" class="modal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>Sửa: <span id="editName" style="color:var(--blue)"></span></h3>
                        <span class="close" onclick="closeModal()">&times;</span>
                    </div>
                    <input type="hidden" id="editId"><input type="hidden" id="editAccId">
                    
                    <div class="grid-2">
                        <div class="form-group"><label>Sức mạnh</label><input type="text" id="editPower"></div>
                        <div class="form-group"><label>Vàng (Hành trang)</label><input type="text" id="editGold"></div>
                        <div class="form-group"><label>Ngọc xanh</label><input type="text" id="editGem"></div>
                        <div class="form-group"><label>VND (Account)</label><input type="text" id="editCash"></div>
                        <div class="form-group"><label>Đã nạp</label><input type="text" id="editDanap"></div>
                        <div class="form-group"><label>Trạng thái</label>
                            <select id="editActive"><option value="0">Chưa kích hoạt</option><option value="1">Đã kích hoạt</option></select>
                        </div>
                        <div class="form-group"><label>Ban (Khóa)</label>
                            <select id="editBan"><option value="0">Không</option><option value="1">Đang khóa</option></select>
                        </div>
                    </div>
                    
                    <h4 style="border-bottom:1px solid #eee; padding-bottom:5px; margin-top:0">Nhiệm vụ</h4>
                    <div class="grid-2">
                        <div class="form-group"><label>ID Nhiệm vụ</label><input type="number" id="editTaskId"></div>
                        <div class="form-group"><label>Index (Bước)</label><input type="number" id="editTaskIndex"></div>
                        <div class="form-group"><label>Count (Số lượng)</label><input type="number" id="editTaskCount"></div>
                    </div>

                    <h4 style="border-bottom:1px solid #eee; padding-bottom:5px; margin-top:0">Buff Item</h4>
                    <div class="grid-2">
                        <div class="form-group"><label>ID Item</label><input type="number" id="buffItemId"></div>
                        <div class="form-group"><label>Số lượng</label><input type="number" id="buffItemQty" value="1"></div>
                        <div class="form-group" style="grid-column: span 2"><button class="btn btn-success" onclick="buffItem()" style="width:100%">Thêm Item</button></div>
                    </div>

                    <div style="margin-top:20px; text-align:right">
                        <button class="btn btn-danger" onclick="closeModal()">Hủy</button>
                        <button class="btn btn-primary" onclick="savePlayer()">Lưu thay đổi</button>
                    </div>
                </div>
            </div>

            <script>
                // --- TABS & UI ---
                function switchTab(id) {
                    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
                    document.getElementById(id).classList.add('active');
                    document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
                    event.currentTarget.classList.add('active');
                    if(id === 'config') loadBossList();
                }
                function closeModal() { document.getElementById('editModal').classList.remove('show'); }

                // --- API HELPERS ---
                async function fetchApi(url, method='GET', body=null) {
                    try {
                        let opts = { method: method };
                        if(body) opts.body = body;
                        let res = await fetch(url, opts);
                        return await res.json();
                    } catch(e) { console.error(e); return {error: 'Lỗi kết nối'}; }
                }

                // --- STATS LOOP ---
                setInterval(async () => {
                    if(!document.getElementById('dashboard').classList.contains('active')) return;
                    let d = await fetchApi('/api/stats');
                    if(d.cpu !== undefined) {
                        document.getElementById('cpuVal').innerText = d.cpu.toFixed(1) + '%';
                        document.getElementById('ramVal').innerText = d.ramUsed + ' / ' + d.ramTotal + ' MB';
                        document.getElementById('onlineVal').innerText = d.online;
                        document.getElementById('threadVal').innerText = d.threads;
                        document.getElementById('gcVal').innerText = d.giftcodes;
                        document.getElementById('consignVal').innerText = d.consign;
                        document.getElementById('uptimeVal').innerText = d.uptime;
                        document.getElementById('expDisplay').innerText = 'x' + d.expRate;
                        document.getElementById('bossStatsVal').innerText = d.bossStats;
                    }
                }, 2000);

                // --- PLAYER LOGIC ---
                async function loadPlayers() {
                    let key = document.getElementById('searchInp').value;
                    let data = await fetchApi('/api/players?search=' + encodeURIComponent(key));
                    let html = '';
                    if(data.length === 0) html = '<tr><td colspan="8" style="text-align:center">Không tìm thấy dữ liệu</td></tr>';
                    else {
                        data.forEach(p => {
                            let banHtml = p.ban == 1 ? '<span class="tag tag-red">Blocked</span>' : '<span class="tag tag-green">Active</span>';
                            html += `<tr>
                                <td>${p.id}</td>
                                <td><b>${p.name}</b></td>
                                <td>${new Intl.NumberFormat().format(p.power)}</td>
                                <td>${new Intl.NumberFormat().format(p.gold)}</td>
                                <td>${new Intl.NumberFormat().format(p.gem)}</td>
                                <td>${new Intl.NumberFormat().format(p.cash)}</td>
                                <td>${banHtml}</td>
                                <td>
                                    <button class="btn btn-primary btn-sm" onclick="editPlayer(${p.id})"><i class="fas fa-edit"></i></button>
                                </td>
                            </tr>`;
                        });
                    }
                    document.getElementById('playerTable').innerHTML = html;
                }

                async function editPlayer(id) {
                    let d = await fetchApi('/api/player-detail?id=' + id);
                    if(d.error) return alert(d.error);

                    document.getElementById('editName').innerText = d.name;
                    document.getElementById('editId').value = d.id;
                    document.getElementById('editAccId').value = d.account_id;
                    document.getElementById('editPower').value = d.power;
                    
                    let inv = d.inventory; 
                    document.getElementById('editGold').value = inv.length > 0 ? inv[0] : 0;
                    document.getElementById('editGem').value = inv.length > 1 ? inv[1] : 0;
                    
                    document.getElementById('editCash').value = d.cash;
                    document.getElementById('editDanap').value = d.danap;
                    document.getElementById('editActive').value = d.active;
                    document.getElementById('editBan').value = d.ban;

                    let task = d.task; 
                    document.getElementById('editTaskId').value = task.length > 0 ? task[0] : 0;
                    document.getElementById('editTaskIndex').value = task.length > 1 ? task[1] : 0;
                    document.getElementById('editTaskCount').value = task.length > 2 ? task[2] : 0;

                    document.getElementById('editModal').classList.add('show');
                }

                async function savePlayer() {
                    if(!confirm('Lưu thay đổi? Player sẽ bị kick để cập nhật.')) return;
                    let body = 'action=update_player'
                        + '&id=' + document.getElementById('editId').value
                        + '&account_id=' + document.getElementById('editAccId').value
                        + '&power=' + document.getElementById('editPower').value
                        + '&gold=' + document.getElementById('editGold').value
                        + '&gem=' + document.getElementById('editGem').value
                        + '&cash=' + document.getElementById('editCash').value
                        + '&danap=' + document.getElementById('editDanap').value
                        + '&active=' + document.getElementById('editActive').value
                        + '&ban=' + document.getElementById('editBan').value
                        + '&task_id=' + document.getElementById('editTaskId').value
                        + '&task_index=' + document.getElementById('editTaskIndex').value
                        + '&task_count=' + document.getElementById('editTaskCount').value;

                    let d = await fetchApi('/api/action', 'POST', body);
                    alert(d.msg || d.error);
                    if(d.msg) { closeModal(); loadPlayers(); }
                }

                async function buffItem() {
                    let id = document.getElementById('editId').value;
                    let itemId = document.getElementById('buffItemId').value;
                    let qty = document.getElementById('buffItemQty').value;
                    if(!itemId) return alert('Nhập ID Item');
                    
                    let body = `action=buff_item&id=${id}&item_id=${itemId}&quantity=${qty}`;
                    let d = await fetchApi('/api/action', 'POST', body);
                    alert(d.msg || d.error);
                }

                // --- CONFIG & BOSS LOGIC ---
                let bossListLoaded = false;
                async function loadBossList() {
                    if(bossListLoaded) return;
                    let data = await fetchApi('/api/boss-ids');
                    let html = data.map(b => `<option value="${b.id}">${b.name} (ID: ${b.id})</option>`).join('');
                    document.getElementById('bossSelect').innerHTML = html;
                    bossListLoaded = true;
                }

                function doAction(act) {
                    if(confirm('Thực hiện hành động này?')) postAction(act.split('&')[0], act.split('param=')[1] || '');
                }
                function summonBoss() { postAction('summon_boss', document.getElementById('bossSelect').value); }
                function setExp() { postAction('set_exp', document.getElementById('expInput').value); }
                function doMaintenance() { 
                    let sec = document.getElementById('maintTime').value;
                    if(confirm('Bảo trì sau ' + sec + 's?')) postAction('maintenance', sec); 
                }
                
                function doRestart() {
                    if(confirm('CẢNH BÁO: Server sẽ lưu dữ liệu và tắt ngay lập tức. Bạn có chắc chắn?')) {
                        postAction('restart');
                    }
                }
                
                let autoSave = true;
                function toggleAutoSave() {
                    autoSave = !autoSave;
                    document.getElementById('btnAutoSave').innerText = 'AutoSave: ' + (autoSave ? 'ON' : 'OFF');
                    postAction('toggle_autosave', autoSave ? 'on' : 'off');
                }

                async function postAction(action, param='') {
                    let d = await fetchApi('/api/action', 'POST', 'action=' + action + '&param=' + param);
                    alert(d.msg || d.error);
                }
                
                // Init
                loadPlayers();
            </script>
        </body>
        </html>
        """;
    }
}