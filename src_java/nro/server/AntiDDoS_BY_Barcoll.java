package nro.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AntiDDoS_BY_Barcoll {

    // Config
    public static int PROXY_PORT = 14445;
    public static String REAL_HOST = "127.0.0.1";
    public static int REAL_PORT = 14446;

    // Limits - NÂNG CAO giới hạn cho server đông người
    private static final int MAX_ONLINE = 5000;           // Tăng lên 5000 người online
    private static final int MAX_CONN_PER_IP = 20;        // Tăng lên 20 kết nối/IP (cho multi-login)
    private static final int MAX_CONN_PER_MINUTE = 100;   // Tăng lên 100 kết nối/phút
    private static final int SOCKET_TIMEOUT = 180000;

    // Tracking
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    private static final Map<String, AtomicInteger> ipConnections = new ConcurrentHashMap<>();
    private static final Map<String, Deque<Long>> ipConnectionHistory = new ConcurrentHashMap<>();
    private static final Set<String> whitelistIps = ConcurrentHashMap.newKeySet();
    private static final Set<String> blacklistIps = ConcurrentHashMap.newKeySet();
    private static final Set<String> trustedIps = ConcurrentHashMap.newKeySet(); // IP được ưu tiên
    
    // Adaptive protection - Tự động điều chỉnh theo tải
    private static volatile boolean adaptiveMode = false;
    private static volatile int currentMaxOnline = MAX_ONLINE;
    private static volatile int currentMaxPerIp = MAX_CONN_PER_IP;
    
    // Thread pools
    private static final ExecutorService connectionPool = Executors.newVirtualThreadPerTaskExecutor();
    private static final ScheduledExecutorService maintenanceScheduler = Executors.newScheduledThreadPool(1);

    static {
        // Whitelist rộng hơn cho mạng nội bộ và VPS
        whitelistIps.addAll(Arrays.asList(
            "127.0.0.1", "localhost", "0:0:0:0:0:0:0:1", "::1",
            "192.168.", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
            "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31."
        ));
        
        // Trusted IPs - Các IP của game client phổ biến
        trustedIps.addAll(Arrays.asList(
            "14.", "27.", "42.", "58.", "59.", "60.", "61.", "111.", "112.",
            "113.", "114.", "115.", "116.", "117.", "118.", "119.", "120.",
            "121.", "122.", "123.", "124.", "125.", "126.", "183.", "202.",
            "203.", "210.", "211.", "212.", "213.", "214.", "215.", "216."
        ));
    }

    public static void runProxy() {
        main(new String[]{});
    }

    public static void main(String[] args) {
        startMaintenanceTasks();
        startAdaptiveProtection();
        
        try (ServerSocket serverSocket = new ServerSocket(PROXY_PORT)) {
            serverSocket.setSoTimeout(1000);
            log("🚀 Anti-DDoS Proxy started on port " + PROXY_PORT);
            log("🔗 Forwarding to " + REAL_HOST + ":" + REAL_PORT);
            log("🎯 Limits - Max Online: " + MAX_ONLINE + ", Per IP: " + MAX_CONN_PER_IP);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setSoTimeout(SOCKET_TIMEOUT);
                    
                    connectionPool.submit(() -> handleClientConnection(clientSocket));
                    
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    log("❌ Accept error: " + e.getMessage());
                    try { Thread.sleep(1000); } catch (InterruptedException ie) {}
                }
            }
            
        } catch (IOException e) {
            log("💥 Proxy server crashed: " + e.getMessage());
        }
    }

    private static void handleClientConnection(Socket clientSocket) {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();

        try {
            // Kiểm tra whitelist - KHÔNG BAO GIỜ chặn
            if (isWhitelisted(clientIp)) {
                handleWhitelistConnection(clientSocket, clientIp, clientPort);
                return;
            }

            // Kiểm tra trusted IPs - Ưu tiên cao
            if (isTrustedIp(clientIp)) {
                handleTrustedConnection(clientSocket, clientIp, clientPort);
                return;
            }

            // Kiểm tra blacklist - Chỉ chặn IP thực sự nguy hiểm
            if (blacklistIps.contains(clientIp)) {
                log("🚫 Blacklisted IP: " + clientIp);
                clientSocket.close();
                return;
            }

            // Kiểm tra giới hạn kết nối với độ ưu tiên
            if (!checkConnectionLimitsWithPriority(clientIp)) {
                log("⚠️ Connection limit exceeded for " + clientIp);
                clientSocket.close();
                return;
            }

            // Tăng số lượng kết nối
            ipConnections.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
            int currentOnline = onlineCount.incrementAndGet();

            log("🔗 New connection from " + clientIp + ":" + clientPort + " (Online: " + currentOnline + ")");
            
            // Xử lý kết nối bình thường
            forwardConnection(clientSocket, clientIp, clientPort);

        } catch (Exception e) {
            log("⚠️ Connection setup failed for " + clientIp + ": " + e.getMessage());
            cleanupConnection(clientIp, clientSocket, false);
        }
    }

    private static void handleWhitelistConnection(Socket clientSocket, String clientIp, int clientPort) {
        // Whitelist: KHÔNG KIỂM TRA GIỚI HẠN
        ipConnections.computeIfAbsent(clientIp, k -> new AtomicInteger(0)).incrementAndGet();
        int currentOnline = onlineCount.incrementAndGet();
        
        log("✅ Whitelist connection from " + clientIp + " (Online: " + currentOnline + ")");
        forwardConnection(clientSocket, clientIp, clientPort);
    }

    private static void handleTrustedConnection(Socket clientSocket, String clientIp, int clientPort) throws IOException {
        // Trusted IPs: Giới hạn cao hơn bình thường
        AtomicInteger connCount = ipConnections.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        
        if (connCount.get() >= currentMaxPerIp * 2) { // Trusted IP được gấp đôi giới hạn
            log("⚠️ Trusted IP limit exceeded: " + clientIp);
            clientSocket.close();
            return;
        }
        
        connCount.incrementAndGet();
        int currentOnline = onlineCount.incrementAndGet();
        
        log("⭐ Trusted connection from " + clientIp + " (Online: " + currentOnline + ")");
        forwardConnection(clientSocket, clientIp, clientPort);
    }

    private static boolean isWhitelisted(String ip) {
        return whitelistIps.stream().anyMatch(ip::startsWith);
    }

    private static boolean isTrustedIp(String ip) {
        return trustedIps.stream().anyMatch(ip::startsWith);
    }

    private static boolean checkConnectionLimitsWithPriority(String ip) {
        // Kiểm tra số kết nối đồng thời
        AtomicInteger currentConnections = ipConnections.get(ip);
        if (currentConnections != null && currentConnections.get() >= currentMaxPerIp) {
            return false;
        }

        // Kiểm tra số kết nối trong 1 phút (lỏng hơn cho IP mới)
        Deque<Long> history = ipConnectionHistory.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        long now = System.currentTimeMillis();
        history.addLast(now);

        // Dọn lịch sử cũ
        history.removeIf(time -> now - time > 60000);

        int maxConnPerMin = isNewIp(ip) ? MAX_CONN_PER_MINUTE * 2 : MAX_CONN_PER_MINUTE;
        return history.size() <= maxConnPerMin;
    }

    private static boolean isNewIp(String ip) {
        Deque<Long> history = ipConnectionHistory.get(ip);
        return history == null || history.size() < 5; // IP có ít hơn 5 kết nối được coi là mới
    }

    private static void forwardConnection(Socket clientSocket, String clientIp, int clientPort) {
        Socket backendSocket = null;
        boolean connectionSuccessful = false;
        
        try {
            backendSocket = new Socket(REAL_HOST, REAL_PORT);
            backendSocket.setSoTimeout(SOCKET_TIMEOUT);
            connectionSuccessful = true;
            
            try (InputStream clientInput = clientSocket.getInputStream();
                 OutputStream clientOutput = clientSocket.getOutputStream();
                 InputStream backendInput = backendSocket.getInputStream();
                 OutputStream backendOutput = backendSocket.getOutputStream()) {

                log("🔄 Forwarding " + clientIp + ":" + clientPort + " → " + REAL_HOST + ":" + REAL_PORT);

                // Sử dụng CompletableFuture cho hiệu năng cao
                CompletableFuture<Void> clientToBackend = CompletableFuture.runAsync(
                    () -> forwardData(clientInput, backendOutput, clientIp, "client→backend"), 
                    connectionPool
                );
                
                CompletableFuture<Void> backendToClient = CompletableFuture.runAsync(
                    () -> forwardData(backendInput, clientOutput, clientIp, "backend→client"), 
                    connectionPool
                );

                // Chờ cả hai chiều hoàn thành
                CompletableFuture.allOf(clientToBackend, backendToClient)
                    .orTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
                    .join();

                log("✅ Connection completed: " + clientIp);

            }catch (Exception e) {
                if (!isNormalDisconnect(e)) {
                    log("❌ Forwarding error for " + clientIp + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            if (!isNormalDisconnect(e)) {
                log("❌ Backend connection failed for " + clientIp + ": " + e.getMessage());
            }
        } finally {
            cleanupConnection(clientIp, clientSocket, connectionSuccessful);
            closeSocket(backendSocket);
        }
    }

    private static boolean isNormalDisconnect(Exception e) {
        String message = e.getMessage();
        return message != null && (
            message.contains("aborted") || 
            message.contains("reset") ||
            message.contains("closed") ||
            message.contains("timed out")
        );
    }

    private static void forwardData(InputStream input, OutputStream output, String ip, String direction) {
        byte[] buffer = new byte[8192];
        try {
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                output.flush();
            }
        } catch (IOException e) {
            if (!isNormalDisconnect(e)) {
                log("🔌 " + direction + " error for " + ip + ": " + e.getMessage());
            }
        }
    }

    private static void cleanupConnection(String ip, Socket socket, boolean connectionSuccessful) {
        try {
            // Giảm số lượng kết nối của IP
            AtomicInteger connCount = ipConnections.get(ip);
            if (connCount != null) {
                int remaining = connCount.decrementAndGet();
                if (remaining <= 0) {
                    ipConnections.remove(ip);
                }
            }

            if (connectionSuccessful) {
                int currentOnline = onlineCount.decrementAndGet();
                // Chỉ log khi online count thay đổi nhiều
                if (currentOnline % 50 == 0) {
                    log("🔌 Connection closed: " + ip + " (Online: " + currentOnline + ")");
                }
            }
            
            closeSocket(socket);

        } catch (Exception e) {
            // Không log lỗi cleanup
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
    }

    private static void startMaintenanceTasks() {
        // Dọn dẹp nhẹ nhàng mỗi 2 phút
        maintenanceScheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                
                // Dọn IP history cũ (giữ 2 phút thay vì 1 phút)
                ipConnectionHistory.entrySet().removeIf(entry -> {
                    entry.getValue().removeIf(time -> now - time > 120000);
                    return entry.getValue().isEmpty();
                });

                // Dọn IP không còn kết nối
                ipConnections.entrySet().removeIf(entry -> entry.getValue().get() <= 0);

                // Log trạng thái (ít verbose hơn)
                if (onlineCount.get() > 0) {
                    log("📊 Status - Online: " + onlineCount.get() + 
                        ", Unique IPs: " + ipConnections.size());
                }

            } catch (Exception e) {
                // Không log lỗi maintenance nhỏ
            }
        }, 2, 2, TimeUnit.MINUTES);
    }

    private static void startAdaptiveProtection() {
        // Tự động điều chỉnh giới hạn theo tải
        maintenanceScheduler.scheduleAtFixedRate(() -> {
            try {
                int currentOnline = onlineCount.get();
                
                if (currentOnline > MAX_ONLINE * 0.8) {
                    // Server đông: nới lỏng giới hạn
                    if (!adaptiveMode) {
                        adaptiveMode = true;
                        currentMaxOnline = (int)(MAX_ONLINE * 1.5);
                        currentMaxPerIp = (int)(MAX_CONN_PER_IP * 1.5);
                        log("🎯 Adaptive mode ON - Increased limits: " + 
                            currentMaxOnline + " online, " + currentMaxPerIp + " per IP");
                    }
                } else if (currentOnline < MAX_ONLINE * 0.3) {
                    // Server ít người: trở về bình thường
                    if (adaptiveMode) {
                        adaptiveMode = false;
                        currentMaxOnline = MAX_ONLINE;
                        currentMaxPerIp = MAX_CONN_PER_IP;
                        log("🎯 Adaptive mode OFF - Normal limits restored");
                    }
                }
                
            } catch (Exception e) {
                // Không log lỗi adaptive
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    // Utility methods
    public static void addToWhitelist(String ip) {
        whitelistIps.add(ip);
        log("➕ Added to whitelist: " + ip);
    }

    public static void addToTrusted(String ip) {
        trustedIps.add(ip);
        log("⭐ Added to trusted: " + ip);
    }

    public static void addToBlacklist(String ip) {
        // Chỉ blacklist khi thực sự cần thiết
        if (!whitelistIps.stream().anyMatch(ip::startsWith) && 
            !trustedIps.stream().anyMatch(ip::startsWith)) {
            blacklistIps.add(ip);
            log("🚫 Added to blacklist: " + ip);
        }
    }

    public static void removeFromBlacklist(String ip) {
        blacklistIps.remove(ip);
        log("✅ Removed from blacklist: " + ip);
    }

    public static int getOnlineCount() {
        return onlineCount.get();
    }

    public static int getUniqueIpCount() {
        return ipConnections.size();
    }

    public static String getStatus() {
        return String.format("Online: %d, Unique IPs: %d, Adaptive: %s", 
            onlineCount.get(), ipConnections.size(), adaptiveMode ? "ON" : "OFF");
    }

    public static void shutdown() {
        log("🛑 Shutting down Anti-DDoS Proxy...");
        connectionPool.shutdown();
        maintenanceScheduler.shutdown();
        try {
            if (!connectionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                connectionPool.shutdownNow();
            }
            if (!maintenanceScheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                maintenanceScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            connectionPool.shutdownNow();
            maintenanceScheduler.shutdownNow();
        }
        log("✅ Anti-DDoS Proxy stopped");
    }

    private static void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String logMessage = String.format("[%s] %s", timestamp, message);
        
        System.out.println(logMessage);
        
        try (PrintWriter out = new PrintWriter(new FileWriter("antiddos.log", true))) {
            out.println(logMessage);
        } catch (IOException e) {
            // Không log lỗi log file
        }
    }
}