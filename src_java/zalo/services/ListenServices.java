/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.services;

import zalo.utils.Apis;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import zalo.models.GroupMessage;
import zalo.models.UserMessage;
import zalo.consts.Constants;
import zalo.services.DecoderServices;
import zalo.utils.Json;
import zalo.utils.Url;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.List;

public class ListenServices {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private Context ctx;
    private Apis api;
    private WebSocketClient ws;
    private Map<String, List<Consumer<Object>>> eventHandlers = new ConcurrentHashMap<>();
    private String wsURL;
    private List<String> wsUrls;
    private String cipherKey;
    private int id = 0;
    private Timer pingTimer;
    private Timer connectionMonitorTimer;
    private long lastMessageTime = 0;
    private int rotateCount = 0;
    private Map<String, RetryInfo> retryCount = new HashMap<>();
    private boolean shouldRetryOnClose = false;
    
    private static class RetryInfo {
        int count = 0;
        int max;
        List<Integer> times;
    }
    
    public ListenServices(Context ctx, Apis api) {
        this.ctx = ctx;
        this.api = api;
        
        if (ctx.getCookie() == null) {
            throw new ZaloApiError("Cookie is not available");
        }
        if (ctx.getUserAgent() == null) {
            throw new ZaloApiError("User agent is not available");
        }
        
        Object wsUrlsObj = ctx.getLoginInfo().get("zpw_ws");
        if (wsUrlsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> urls = (List<Object>) wsUrlsObj;
            this.wsUrls = new ArrayList<>();
            for (Object url : urls) {
                if (url instanceof String) {
                    this.wsUrls.add((String) url);
                }
            }
        } else if (wsUrlsObj instanceof String) {
            this.wsUrls = Collections.singletonList((String) wsUrlsObj);
        } else {
            throw new ZaloApiError("Invalid WebSocket URLs format");
        }
        
        if (this.wsUrls.isEmpty()) {
            throw new ZaloApiError("No WebSocket URLs available");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) ctx.getSettings().get("features");
        if (features != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> socket = (Map<String, Object>) features.get("socket");
            if (socket != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> retries = (Map<String, Object>) socket.get("retries");
                if (retries != null) {
                    for (Map.Entry<String, Object> entry : retries.entrySet()) {
                        RetryInfo retryInfo = new RetryInfo();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> retryData = (Map<String, Object>) entry.getValue();
                        if (retryData != null) {
                            Object timesObj = retryData.get("times");
                            if (timesObj instanceof Number) {
                                retryInfo.times = Collections.singletonList(((Number) timesObj).intValue());
                            } else if (timesObj instanceof List) {
                                retryInfo.times = new ArrayList<>();
                                for (Object t : (List<?>) timesObj) {
                                    if (t instanceof Number) {
                                        retryInfo.times.add(((Number) t).intValue());
                                    }
                                }
                            }
                            Object maxObj = retryData.get("max");
                            if (maxObj instanceof Number) {
                                retryInfo.max = ((Number) maxObj).intValue();
                            }
                        }
                        retryCount.put(entry.getKey(), retryInfo);
                    }
                }
            }
        }
        
        Map<String, String> params = new HashMap<>();
        params.put("t", String.valueOf(System.currentTimeMillis()));
        this.wsURL = Url.makeURL(ctx, wsUrls.get(0), params);
    }
    
    public void on(String event, Consumer<Object> handler) {
        eventHandlers.computeIfAbsent(event, k -> new ArrayList<>()).add(handler);
    }
    
    private void emit(String event, Object data) {
        List<Consumer<Object>> handlers = eventHandlers.get(event);
        if (handlers != null && !handlers.isEmpty()) {
            for (Consumer<Object> handler : handlers) {
                try {
                    handler.accept(data);
                } catch (Exception e) {
                    System.err.println("[LISTENER] Error in event handler for " + event + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void start() {
        start(false);
    }
    
    public void start(boolean retryOnClose) {
        this.shouldRetryOnClose = retryOnClose;
        
        if (ws != null && ws.isOpen()) {
            System.out.println("[LISTENER] WebSocket already connected, skipping start");
            return;
        }
        
        if (ws != null) {
            try {
                ws.close();
            } catch (Exception e) {
            }
            reset();
        }
        
        try {
            URI uri = new URI(wsURL);
            
            String cookieString = getCookieString();
            
            final boolean shouldRetry = this.shouldRetryOnClose;
            
            ws = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    lastMessageTime = System.currentTimeMillis();
                    System.out.println("[LISTENER] WebSocket connected");
                    emit("connected", null);
                    startConnectionMonitor();
                }
                
                @Override
                public void onMessage(ByteBuffer buffer) {
                    try {
                        lastMessageTime = System.currentTimeMillis();
                        handleMessage(buffer);
                    } catch (Exception e) {
                        System.err.println("[LISTENER] Error handling message: " + e.getMessage());
                        e.printStackTrace();
                        emit("error", e);
                    }
                }
                
                @Override
                public void onMessage(String message) {
                    lastMessageTime = System.currentTimeMillis();
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[LISTENER] WebSocket closed: code=" + code + ", reason=" + reason + ", remote=" + remote);
                    stopConnectionMonitor();
                    reset();
                    emit("disconnected", code);
                    
                    boolean needRetry = shouldRetry;
                    if (code == 1006) {
                        System.out.println("[LISTENER] Connection lost (1006), will reconnect...");
                        needRetry = true;
                    }
                    
                    if (needRetry && canRetry(code)) {
                        int retryTime = getRetryTime(code);
                        boolean shouldRotate = shouldRotate(code);
                        if (shouldRotate) {
                            rotateEndpoint();
                        }
                        System.out.println("[LISTENER] Will reconnect in " + retryTime + "ms");
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    start(true);
                                } catch (Exception e) {
                                    System.err.println("[LISTENER] Reconnect failed: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                        }, retryTime);
                    } else {
                        emit("closed", code);
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    System.err.println("[LISTENER] WebSocket error: " + ex.getMessage());
                    ex.printStackTrace();
                    emit("error", ex);
                }
            };
            
            Map<String, String> headers = new HashMap<>();
            headers.put("accept-encoding", "gzip, deflate, br, zstd");
            headers.put("accept-language", "en-US,en;q=0.9");
            headers.put("cache-control", "no-cache");
            headers.put("connection", "Upgrade");
            headers.put("origin", Constants.ZALO_CHAT_ORIGIN);
            headers.put("pragma", "no-cache");
            headers.put("sec-websocket-extensions", "permessage-deflate; client_max_window_bits");
            headers.put("sec-websocket-version", "13");
            headers.put("upgrade", "websocket");
            headers.put("user-agent", ctx.getUserAgent());
            headers.put("cookie", cookieString);
            
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                ws.addHeader(entry.getKey(), entry.getValue());
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) ctx.getSettings().get("features");
            long connectionLostTimeout = 120000;
            if (features != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> socket = (Map<String, Object>) features.get("socket");
                if (socket != null && socket.containsKey("connection_lost_timeout")) {
                    Object timeoutObj = socket.get("connection_lost_timeout");
                    if (timeoutObj instanceof Number) {
                        connectionLostTimeout = ((Number) timeoutObj).longValue();
                    }
                }
            }
            ws.setConnectionLostTimeout((int) (connectionLostTimeout / 1000));
            ws.setTcpNoDelay(true);
            
            ws.connect();
            
        } catch (Exception e) {
            throw new ZaloApiError("Failed to start WebSocket: " + e.getMessage(), e);
        }
    }
    
    private void handleMessage(ByteBuffer buffer) throws Exception {
        if (buffer.remaining() < 4) {
            return;
        }
        
        byte[] headerBytes = new byte[4];
        buffer.get(headerBytes);
        
        int[] header = DecoderServices.getHeader(headerBytes);
        int version = header[0];
        int cmd = header[1];
        int subCmd = header[2];
        
        byte[] dataBytes = new byte[buffer.remaining()];
        buffer.get(dataBytes);
        
        String decodedData = new String(dataBytes, StandardCharsets.UTF_8);
        if (decodedData.isEmpty()) {
            return;
        }
        
        Object parsed = Json.parse(decodedData);
        if (!(parsed instanceof Map)) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parsedMap = (Map<String, Object>) parsed;
        
        if (version == 1 && cmd == 501 && subCmd == 0) {
        } else if (version == 1 && cmd == 521 && subCmd == 0) {
        } else if (version == 1 && cmd == 1 && subCmd == 1) {
        } else {
            return;
        }
        
        if (version == 1 && cmd == 1 && subCmd == 1 && parsedMap.containsKey("key")) {
            String newCipherKey = (String) parsedMap.get("key");
            this.cipherKey = newCipherKey;
            emit("cipher_key", cipherKey);
            
            if (pingTimer != null) {
                pingTimer.cancel();
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> features = (Map<String, Object>) ctx.getSettings().get("features");
            long pingInterval = 30000;
            if (features != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> socket = (Map<String, Object>) features.get("socket");
                if (socket != null && socket.containsKey("ping_interval")) {
                    Object pingIntervalObj = socket.get("ping_interval");
                    if (pingIntervalObj instanceof Number) {
                        pingInterval = ((Number) pingIntervalObj).longValue();
                    }
                }
            }
            
            pingTimer = new Timer();
            pingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    sendPing();
                }
            }, pingInterval, pingInterval);
        }
        
        if (version == 1 && cmd == 501 && subCmd == 0) {
            
            if (parsedMap.containsKey("error_code")) {
                Object errorCodeObj = parsedMap.get("error_code");
                if (errorCodeObj instanceof Number) {
                    int errorCode = ((Number) errorCodeObj).intValue();
                    if (errorCode != 0) {
                        return; 
                    }
                }
            }
            
            Map<String, Object> parsedData;
            if (parsedMap.containsKey("data") && parsedMap.containsKey("encrypt")) {
                try {
                    if (cipherKey == null || cipherKey.isEmpty()) {
                        System.out.println("[LISTENER] Warning: cipherKey not available, skipping message");
                        return;
                    }
                    Object encryptObj = parsedMap.get("encrypt");
                    if (encryptObj instanceof Number) {
                        int encryptType = ((Number) encryptObj).intValue();
                        if (encryptType == 0) {
                            Object dataObj = parsedMap.get("data");
                            if (dataObj instanceof String) {
                                Object parsedJson = Json.parse((String) dataObj);
                                if (parsedJson instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> parsedJsonMap = (Map<String, Object>) parsedJson;
                                    parsedData = parsedJsonMap;
                                } else {
                                    parsedData = parsedMap;
                                }
                            } else {
                                parsedData = parsedMap;
                            }
                        } else {
                            if (cipherKey == null || cipherKey.isEmpty()) {
                                return;
                            }
                            Map<String, Object> decoded = DecoderServices.decodeEventData(parsedMap, cipherKey);
                            
                            if (decoded != null && decoded.containsKey("data")) {
                                Object dataObj = decoded.get("data");
                                if (dataObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                                    parsedData = dataMap;
                                } else {
                                    parsedData = decoded;
                                }
                            } else {
                                parsedData = decoded;
                            }
                        }
                    } else {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }
            } else {
                parsedData = parsedMap;
            }
            
            if (parsedData != null) {
                Object msgsObj = parsedData.get("msgs");
                if (msgsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> msgs = (List<Map<String, Object>>) msgsObj;
                    for (Map<String, Object> msg : msgs) {
                        Object contentObj = msg.get("content");
                        if (contentObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> content = (Map<String, Object>) contentObj;
                            if (content.containsKey("deleteMsg")) {
                                emit("undo", msg);
                            } else {
                                UserMessage messageObject = new UserMessage(ctx.getUid(), msg);
                                if (messageObject.isSelf() && !ctx.getOptions().isSelfListen()) {
                                    continue;
                                }
                                emit("message", messageObject);
                                emit("userMessage", messageObject);
                            }
                        } else {
                            UserMessage messageObject = new UserMessage(ctx.getUid(), msg);
                            if (messageObject.isSelf() && !ctx.getOptions().isSelfListen()) {
                                continue;
                            }
                            emit("message", messageObject);
                            emit("userMessage", messageObject);
                        }
                    }
                }
            }
        }
        
        if (version == 1 && cmd == 521 && subCmd == 0) {
            if (parsedMap.containsKey("error_code")) {
                Object errorCodeObj = parsedMap.get("error_code");
                if (errorCodeObj instanceof Number) {
                    int errorCode = ((Number) errorCodeObj).intValue();
                    if (errorCode != 0) {
                        return; 
                    }
                }
            }
            
            Map<String, Object> parsedData;
            if (parsedMap.containsKey("data") && parsedMap.containsKey("encrypt")) {
                try {
                    if (cipherKey == null || cipherKey.isEmpty()) {
                        return;
                    }
                    Object encryptObj = parsedMap.get("encrypt");
                    if (encryptObj instanceof Number) {
                        int encryptType = ((Number) encryptObj).intValue();
                        if (encryptType == 0) {
                            Object dataObj = parsedMap.get("data");
                            if (dataObj instanceof String) {
                                Object parsedJson = Json.parse((String) dataObj);
                                if (parsedJson instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> parsedJsonMap = (Map<String, Object>) parsedJson;
                                    parsedData = parsedJsonMap;
                                } else {
                                    parsedData = parsedMap;
                                }
                            } else {
                                parsedData = parsedMap;
                            }
                        } else {
                            if (cipherKey == null || cipherKey.isEmpty()) {
                                return;
                            }
                            Map<String, Object> decoded = DecoderServices.decodeEventData(parsedMap, cipherKey);
                            if (decoded != null && decoded.containsKey("data")) {
                                Object dataObj = decoded.get("data");
                                if (dataObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                                    parsedData = dataMap;
                                } else {
                                    parsedData = decoded;
                                }
                            } else {
                                parsedData = decoded;
                            }
                        }
                    } else {
                        return;
                    }
                } catch (Exception e) {
                    return;
                }
            } else {
                parsedData = parsedMap;
            }
            
            if (parsedData != null) {
                Object groupMsgsObj = parsedData.get("groupMsgs");
                if (groupMsgsObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> groupMsgs = (List<Map<String, Object>>) groupMsgsObj;
                    for (Map<String, Object> msg : groupMsgs) {
                        Object contentObj = msg.get("content");
                        if (contentObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> content = (Map<String, Object>) contentObj;
                            if (content.containsKey("deleteMsg")) {
                                emit("undo", msg);
                            } else {
                                GroupMessage messageObject = new GroupMessage(ctx.getUid(), msg);
                                if (messageObject.isSelf() && !ctx.getOptions().isSelfListen()) {
                                    continue;
                                }
                                emit("message", messageObject);
                                emit("groupMessage", messageObject);
                            }
                        } else {
                            GroupMessage messageObject = new GroupMessage(ctx.getUid(), msg);
                            if (messageObject.isSelf() && !ctx.getOptions().isSelfListen()) {
                                continue;
                            }
                            emit("message", messageObject);
                            emit("groupMessage", messageObject);
                        }
                    }
                }
            }
        }
        
        if (version == 1 && cmd == 3000 && subCmd == 0) {
            if (ws != null && ws.isOpen()) {
                ws.close(3000);
            }
        }
    }
    
    private void sendPing() {
        if (ws == null || !ws.isOpen()) {
            return;
        }
        
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventId", System.currentTimeMillis());
            
            sendWs(1, 2, 1, payload, false);
        } catch (Exception e) {
            System.err.println("[LISTENER] Error sending ping: " + e.getMessage());
            if (ws != null && ws.isOpen()) {
                try {
                    ws.close(1000);
                } catch (Exception ex) {
                }
            }
        }
    }
    
    private void sendWs(int version, int cmd, int subCmd, Map<String, Object> data, boolean requireId) {
        if (ws == null || !ws.isOpen()) {
            return;
        }
        
        if (requireId) {
            data.put("req_id", "req_" + (id++));
        }
        
        try {
            String jsonData = Json.stringify(data);
            byte[] encodedData = jsonData.getBytes(StandardCharsets.UTF_8);
            
            ByteBuffer buffer = ByteBuffer.allocate(4 + encodedData.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put((byte) version);
            buffer.putShort((short) cmd);
            buffer.put((byte) subCmd);
            buffer.put(encodedData);
            
            ws.send(buffer.array());
        } catch (Exception e) {
            System.err.println("[LISTENER] Error sending WebSocket message: " + e.getMessage());
        }
    }
    
    private String getCookieString() {
        Object cookieObj = ctx.getCookie();
        if (cookieObj == null) {
            return "";
        }
        
        if (cookieObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> cookieList = (List<Object>) cookieObj;
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Object cookieItem : cookieList) {
                if (cookieItem instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cookie = (Map<String, Object>) cookieItem;
                    String key = String.valueOf(cookie.get("key"));
                    String value = String.valueOf(cookie.get("value"));
                    if (key != null && value != null && !key.equals("null") && !value.equals("null")) {
                        if (!first) {
                            sb.append("; ");
                        }
                        sb.append(key).append("=").append(value);
                        first = false;
                    }
                }
            }
            return sb.toString();
        }
        
        if (cookieObj instanceof String) {
            return (String) cookieObj;
        }
        
        return "";
    }
    
    private boolean canRetry(int code) {
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) ctx.getSettings().get("features");
        if (features != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> socket = (Map<String, Object>) features.get("socket");
            if (socket != null) {
                @SuppressWarnings("unchecked")
                List<Integer> closeAndRetryCodes = (List<Integer>) socket.get("close_and_retry_codes");
                if (closeAndRetryCodes != null && closeAndRetryCodes.contains(code)) {
                    RetryInfo retryInfo = retryCount.get(String.valueOf(code));
                    if (retryInfo != null && retryInfo.count < retryInfo.max) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private int getRetryTime(int code) {
        RetryInfo retryInfo = retryCount.get(String.valueOf(code));
        if (retryInfo != null) {
            retryInfo.count++;
            int index = retryInfo.count - 1;
            if (index < retryInfo.times.size()) {
                return retryInfo.times.get(index);
            } else if (!retryInfo.times.isEmpty()) {
                return retryInfo.times.get(retryInfo.times.size() - 1);
            }
        }
        return 5000; 
    }
    
    private boolean shouldRotate(int code) {
        @SuppressWarnings("unchecked")
        Map<String, Object> features = (Map<String, Object>) ctx.getSettings().get("features");
        if (features != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> socket = (Map<String, Object>) features.get("socket");
            if (socket != null) {
                @SuppressWarnings("unchecked")
                List<Integer> rotateErrorCodes = (List<Integer>) socket.get("rotate_error_codes");
                if (rotateErrorCodes != null && rotateErrorCodes.contains(code)) {
                    if (rotateCount < wsUrls.size() - 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void rotateEndpoint() {
        rotateCount++;
        if (rotateCount < wsUrls.size()) {
            Map<String, String> params = new HashMap<>();
            params.put("t", String.valueOf(System.currentTimeMillis()));
            this.wsURL = Url.makeURL(ctx, wsUrls.get(rotateCount), params);
            System.out.println("[LISTENER] Rotating endpoint to " + wsURL);
        }
    }
    
    public void stop() {
        stopConnectionMonitor();
        if (ws != null) {
            try {
                ws.close(1000);
            } catch (Exception e) {
                System.err.println("[LISTENER] Error closing WebSocket: " + e.getMessage());
            }
            reset();
        }
    }
    
    public void close() {
        stop();
    }
    
    private void reset() {
        ws = null;
        cipherKey = null;
        if (pingTimer != null) {
            pingTimer.cancel();
            pingTimer = null;
        }
        stopConnectionMonitor();
    }
    
    private void startConnectionMonitor() {
        stopConnectionMonitor();
        connectionMonitorTimer = new Timer(true);
        connectionMonitorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (ws == null || !ws.isOpen()) {
                        return;
                    }
                    
                    long timeSinceLastMessage = System.currentTimeMillis() - lastMessageTime;
                    if (timeSinceLastMessage > 120000) {
                        System.out.println("[LISTENER] No message received for " + (timeSinceLastMessage / 1000) + " seconds, reconnecting...");
                        ws.close(1000);
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                try {
                                    start(true);
                                } catch (Exception e) {
                                    System.err.println("[LISTENER] Auto-reconnect failed: " + e.getMessage());
                                }
                            }
                        }, 5000);
                    }
                } catch (Exception e) {
                    System.err.println("[LISTENER] Connection monitor error: " + e.getMessage());
                }
            }
        }, 60000, 60000);
    }
    
    private void stopConnectionMonitor() {
        if (connectionMonitorTimer != null) {
            connectionMonitorTimer.cancel();
            connectionMonitorTimer = null;
        }
    }
}
