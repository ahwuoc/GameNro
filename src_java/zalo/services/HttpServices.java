/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.services;

import zalo.consts.Constants;
import zalo.utils.Context;
import zalo.utils.ZaloApiError;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpServices {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static final String ORIGIN = Constants.ZALO_CHAT_ORIGIN;
    private static final String REFERER = Constants.ZALO_CHAT_REFERER;
    private static final String ID_REFERER = Constants.ZALO_ID_REFERER;
    
    public static HttpResponse request(Context ctx, String url, RequestOptions options) throws Exception {
        return request(ctx, url, options, false);
    }
    
    public static HttpResponse request(Context ctx, String url, RequestOptions options, boolean raw) throws Exception {
        if (ctx.getCookie() == null) {
            ctx.setCookie(new HashMap<>());
        }
        
        java.net.URI uri = new java.net.URI(url);
        URL urlObj = uri.toURL();
        String origin = urlObj.getProtocol() + "://" + urlObj.getHost();
        
        Map<String, String> defaultHeaders = getDefaultHeaders(ctx, origin);
        
        if (!raw && options != null && options.getHeaders() != null) {
            defaultHeaders.putAll(options.getHeaders());
        }
        
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setRequestMethod(options != null && options.getMethod() != null ? options.getMethod() : "GET");
        
        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }
        
        if (options != null && options.getBody() != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] bodyBytes = options.getBody().getBytes(StandardCharsets.UTF_8);
                os.write(bodyBytes);
            }
        }
        
        int responseCode = conn.getResponseCode();
        boolean isError = responseCode >= 400;
        
        String responseBody = "";
        try {
            java.io.InputStream inputStream = isError ? conn.getErrorStream() : conn.getInputStream();
            if (inputStream != null) {
                String contentEncoding = conn.getHeaderField("Content-Encoding");
                
                if (contentEncoding != null) {
                    contentEncoding = contentEncoding.toLowerCase();
                    if (contentEncoding.contains("gzip")) {
                        inputStream = new java.util.zip.GZIPInputStream(inputStream);
                    } else if (contentEncoding.contains("deflate")) {
                        inputStream = new java.util.zip.InflaterInputStream(inputStream);
                    }
                }
                
                byte[] bytes = inputStream.readAllBytes();
                
                responseBody = new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            responseBody = "";
        }
        
        List<String> cookies = conn.getHeaderFields().get("Set-Cookie");
        if (cookies != null && !raw) {
        }
        
        if (responseCode >= 300 && responseCode < 400) {
            String location = conn.getHeaderField("Location");
            if (location != null) {
                RequestOptions redirectOptions = new RequestOptions();
                redirectOptions.setMethod("GET");
                if (!raw && options != null) {
                    redirectOptions.setHeaders(new HashMap<>(defaultHeaders));
                    redirectOptions.getHeaders().put("Referer", ID_REFERER);
                }
                return request(ctx, location, redirectOptions, raw);
            }
        }
        
        HttpResponse response = new HttpResponse();
        response.setStatusCode(responseCode);
        response.setBody(responseBody);
        response.setHeaders(conn.getHeaderFields());
        response.setOk(responseCode >= 200 && responseCode < 300);
        
        return response;
    }
    
    private static Map<String, String> getDefaultHeaders(Context ctx, String origin) {
        if (ctx.getCookie() == null) {
            throw new ZaloApiError("Cookie is not available");
        }
        if (ctx.getUserAgent() == null) {
            throw new ZaloApiError("User agent is not available");
        }
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Origin", ORIGIN);
        headers.put("Referer", REFERER);
        headers.put("User-Agent", ctx.getUserAgent());
        
        String cookieString = getCookieString(ctx, origin);
        if (cookieString != null && !cookieString.isEmpty()) {
            headers.put("Cookie", cookieString);
        }
        
        return headers;
    }
    
    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length && i < bytes.length; i++) {
            sb.append(String.format("%02x ", bytes[i]));
        }
        return sb.toString();
    }
    
    private static String getCookieString(Context ctx, String origin) {
        Object cookieObj = ctx.getCookie();
        if (cookieObj == null) {
            return "";
        }
        
        if (cookieObj instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> cookieList = (java.util.List<Object>) cookieObj;
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
        
        if (cookieObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cookieMap = (Map<String, Object>) cookieObj;
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, Object> entry : cookieMap.entrySet()) {
                if (!first) {
                    sb.append("; ");
                }
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
            return sb.toString();
        }
        
        if (cookieObj instanceof String) {
            return (String) cookieObj;
        }
        
        return "";
    }
    
    public static class RequestOptions {
        private String method = "GET";
        private Map<String, String> headers;
        private String body;
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public Map<String, String> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
    }
    
    public static class HttpResponse {
        private int statusCode;
        private String body;
        private Map<String, List<String>> headers;
        private boolean ok;
        
        public int getStatusCode() {
            return statusCode;
        }
        
        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }
        
        public String getBody() {
            return body;
        }
        
        public void setBody(String body) {
            this.body = body;
        }
        
        public Map<String, List<String>> getHeaders() {
            return headers;
        }
        
        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }
        
        public boolean isOk() {
            return ok;
        }
        
        public void setOk(boolean ok) {
            this.ok = ok;
        }
    }
}

