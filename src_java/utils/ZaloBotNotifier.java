package utils;

import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
// địt mẹ con chó bùi xuân nghĩa 
public class ZaloBotNotifier {
    
    private static final String BOT_URL = "http://localhost:8888/nro/boss";
    private static long lastConnectionErrorTime = 0;
    private static final long CONNECTION_ERROR_LOG_INTERVAL = 60000;  // NHẦM 5P
    private static boolean connectionErrorLogged = false;
    
    public static void notifyBossSpawn(String bossName, String mapName) {
        notifyBossSpawn(bossName, mapName, null);
    }// con chó bùi xuân nghĩa bịa chuyện số 1 việt nam
     // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */
    public static void notifyBossSpawn(String bossName, String mapName, String zoneInfo) {
        try {
            String json = "{\"bossName\":\"" + escapeJson(bossName) + "\",\"mapName\":\"" + escapeJson(mapName) + "\"";
            if (zoneInfo != null && !zoneInfo.isEmpty()) {
                json += ",\"zoneInfo\":\"" + escapeJson(zoneInfo) + "\"";
            }
            json += "}";
            
            URL url = new URL(BOT_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                connectionErrorLogged = false;
             } else {
                if (!connectionErrorLogged || (System.currentTimeMillis() - lastConnectionErrorTime) > CONNECTION_ERROR_LOG_INTERVAL) {
                     lastConnectionErrorTime = System.currentTimeMillis();
                    connectionErrorLogged = true;
                }
            }
            
            conn.disconnect();
        } catch (ConnectException | SocketTimeoutException e) {
            long currentTime = System.currentTimeMillis();
            if (!connectionErrorLogged || (currentTime - lastConnectionErrorTime) > CONNECTION_ERROR_LOG_INTERVAL) {
                 lastConnectionErrorTime = currentTime;
                connectionErrorLogged = true;
            }
        } catch (Exception e) {
            long currentTime = System.currentTimeMillis();
            if (!connectionErrorLogged || (currentTime - lastConnectionErrorTime) > CONNECTION_ERROR_LOG_INTERVAL) {
                 lastConnectionErrorTime = currentTime;
                connectionErrorLogged = true;
            }
        }
    }
    
    private static String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

