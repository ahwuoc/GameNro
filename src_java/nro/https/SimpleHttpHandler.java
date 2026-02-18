package nro.https;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.management.OperatingSystemMXBean;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import nro.server.Client;
import nro.server.Maintenance;
import boss.BossManager;
import boss.BossStatus;
import nro.server.ServerManager;

public class SimpleHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        String response = "";
        int statusCode = 200;

        try {
            // --- ROUTING ---
            if (path.equals("/admin") || path.equals("/")) {
                // Trả về giao diện Web HTML
                response = getAdminPageHTML();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            } 
            
            // --- API JSON ENDPOINTS ---
            else if (path.equals("/api/stats")) {
                // API lấy thông tin Dashboard (CPU, RAM, Online)
                response = getSystemStats();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            } 
            else if (path.equals("/api/players")) {
                // API lấy danh sách người chơi
                response = getPlayerList();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
            }
            else if (path.equals("/api/action") && method.equals("POST")) {
                // API thực hiện hành động (Bảo trì, Boss, Kick...)
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

    // --- LOGIC XỬ LÝ API ---

    private String getSystemStats() {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        double cpu = osBean.getProcessCpuLoad() * 100;
        long ramUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
        long ramTotal = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        int online = (Client.gI() != null) ? Client.gI().getPlayers().size() : 0;
        int threads = Thread.activeCount();

        return String.format("{\"cpu\": %.2f, \"ramUsed\": %d, \"ramTotal\": %d, \"online\": %d, \"threads\": %d}", 
                cpu, ramUsed, ramTotal, online, threads);
    }

    private String getPlayerList() {
        if (Client.gI() == null) return "[]";
        // Convert danh sách player sang JSON
        String players = Client.gI().getPlayers().stream()
            .map(p -> String.format("{\"id\":%d, \"name\":\"%s\", \"power\":%d}", p.id, p.name, p.nPoint.power))
            .collect(Collectors.joining(","));
        return "[" + players + "]";
    }

    private String handleAction(String body) {
        // body format: key=value (simple parsing)
        if (body.contains("action=maintenance")) {
            Maintenance.gI().start(60); // Bảo trì 60s
            return "{\"msg\": \"Đã gọi bảo trì 60s\"}";
        }
        if (body.contains("action=reset_boss")) {
            if (BossManager.gI() != null) {
                BossManager.gI().resetAllBosses();
                return "{\"msg\": \"Đã Reset toàn bộ Boss\"}";
            }
        }
        // Thêm các action khác ở đây (Kick, Chat Server...)
        return "{\"msg\": \"Action done\"}";
    }

    // --- GIAO DIỆN HTML (FRONTEND) ---
    private String getAdminPageHTML() {
        return """
        <!DOCTYPE html>
        <html lang="vi">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>NRO Server Manager</title>
            <style>
                :root { --bg: #1e1e2d; --sidebar: #2b2b40; --text: #e0e0e0; --accent: #007bff; --card: #323248; }
                body { margin: 0; font-family: 'Segoe UI', sans-serif; background: var(--bg); color: var(--text); display: flex; height: 100vh; }
                
                /* Sidebar */
                .sidebar { width: 250px; background: var(--sidebar); display: flex; flex-direction: column; padding: 20px; border-right: 1px solid #444; }
                .brand { font-size: 24px; font-weight: bold; color: var(--accent); margin-bottom: 40px; text-align: center; }
                .menu-item { padding: 12px 15px; cursor: pointer; border-radius: 8px; margin-bottom: 5px; transition: 0.3s; color: #a6a6cc; }
                .menu-item:hover, .menu-item.active { background: var(--accent); color: white; }
                
                /* Content */
                .content { flex: 1; padding: 30px; overflow-y: auto; }
                .panel { display: none; animation: fadeIn 0.3s; }
                .panel.active { display: block; }
                
                /* Cards */
                .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
                .card { background: var(--card); padding: 20px; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                .card h3 { margin: 0 0 10px 0; font-size: 14px; color: #888; }
                .card .val { font-size: 28px; font-weight: bold; }
                .card.cpu { border-bottom: 4px solid #ff4757; }
                .card.ram { border-bottom: 4px solid #2ed573; }
                .card.online { border-bottom: 4px solid #1e90ff; }

                /* Tables & Controls */
                table { width: 100%; border-collapse: collapse; margin-top: 20px; background: var(--card); border-radius: 8px; overflow: hidden; }
                th, td { padding: 12px; text-align: left; border-bottom: 1px solid #444; }
                th { background: #222; color: #fff; }
                
                .btn { padding: 10px 20px; border: none; border-radius: 5px; cursor: pointer; font-weight: bold; color: white; transition: 0.2s; }
                .btn-danger { background: #ff4757; } .btn-danger:hover { background: #e84118; }
                .btn-success { background: #2ed573; } .btn-success:hover { background: #26af61; }
                .control-group { display: flex; gap: 10px; margin-top: 20px; }

                @keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
            </style>
        </head>
        <body>
            <div class="sidebar">
                <div class="brand">NRO ADMIN</div>
                <div class="menu-item active" onclick="showPanel('dashboard')">📊 Dashboard</div>
                <div class="menu-item" onclick="showPanel('players')">👥 Quản lý Player</div>
                <div class="menu-item" onclick="showPanel('boss')">👹 Cấu hình Boss</div>
                <div class="menu-item" onclick="showPanel('actions')">⚡ Chức năng nhanh</div>
            </div>

            <div class="content">
                <div id="dashboard" class="panel active">
                    <h2>Tổng quan hệ thống</h2>
                    <div class="stats-grid">
                        <div class="card cpu"><h3>CPU Usage</h3><div class="val" id="cpuVal">0%</div></div>
                        <div class="card ram"><h3>RAM Usage</h3><div class="val" id="ramVal">0 MB</div></div>
                        <div class="card online"><h3>Online Players</h3><div class="val" id="onlineVal">0</div></div>
                        <div class="card"><h3>Active Threads</h3><div class="val" id="threadVal">0</div></div>
                    </div>
                </div>

                <div id="players" class="panel">
                    <h2>Danh sách người chơi</h2>
                    <button class="btn btn-success" onclick="loadPlayers()">🔄 Làm mới danh sách</button>
                    <table>
                        <thead><tr><th>ID</th><th>Tên nhân vật</th><th>Sức mạnh</th><th>Hành động</th></tr></thead>
                        <tbody id="playerTable"></tbody>
                    </table>
                </div>

                <div id="boss" class="panel">
                    <h2>Quản lý Boss</h2>
                    <div class="control-group">
                        <button class="btn btn-danger" onclick="doAction('reset_boss')">Reset Toàn bộ Boss</button>
                        <button class="btn btn-success">Triệu hồi Boss (Coming Soon)</button>
                    </div>
                </div>
                
                <div id="actions" class="panel">
                    <h2>Chức năng nhanh</h2>
                    <div class="control-group">
                        <button class="btn btn-danger" onclick="doAction('maintenance')">📢 Bảo trì (60s)</button>
                        <button class="btn btn-success">💾 Lưu dữ liệu</button>
                    </div>
                </div>
            </div>

            <script>
                // Navigation Logic
                function showPanel(id) {
                    document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
                    document.getElementById(id).classList.add('active');
                    document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
                    event.target.classList.add('active');
                }

                // API Calls
                async function fetchStats() {
                    try {
                        let res = await fetch('/api/stats');
                        let data = await res.json();
                        document.getElementById('cpuVal').innerText = data.cpu.toFixed(1) + '%';
                        document.getElementById('ramVal').innerText = data.ramUsed + ' / ' + data.ramTotal + ' MB';
                        document.getElementById('onlineVal').innerText = data.online;
                        document.getElementById('threadVal').innerText = data.threads;
                    } catch(e) { console.error(e); }
                }

                async function loadPlayers() {
                    let res = await fetch('/api/players');
                    let data = await res.json();
                    let html = '';
                    data.forEach(p => {
                        html += `<tr><td>${p.id}</td><td>${p.name}</td><td>${p.power}</td><td><button class='btn btn-danger' style='padding:5px'>Kick</button></td></tr>`;
                    });
                    document.getElementById('playerTable').innerHTML = html;
                }

                async function doAction(act) {
                    if(!confirm('Bạn có chắc chắn muốn thực hiện?')) return;
                    await fetch('/api/action', {
                        method: 'POST',
                        body: 'action=' + act
                    });
                    alert('Đã gửi lệnh!');
                }

                // Auto Refresh Dashboard
                setInterval(fetchStats, 2000);
                fetchStats();
            </script>
        </body>
        </html>
        """;
    }
}