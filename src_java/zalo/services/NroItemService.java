package zalo.services;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NroItemService {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static NroItemService instance;
    private String nroServerUrl;
    
    private NroItemService() {
        String nroHost = getProperty("nro.host", "localhost");
        int nroPort = Integer.parseInt(getProperty("nro.port", "8889"));
        this.nroServerUrl = "http://" + nroHost + ":" + nroPort + "/nro/item";
    }
    
    private String getProperty(String key, String defaultValue) {
        try {
            java.io.File propsFile = new java.io.File("settings/zalo.properties");
            if (propsFile.exists()) {
                java.util.Properties props = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(propsFile)) {
                    props.load(fis);
                    return props.getProperty(key, defaultValue);
                }
            }
        } catch (Exception e) {
        }
        return defaultValue;
    }
    
    public static NroItemService gI() {
        if (instance == null) {
            instance = new NroItemService();
        }
        return instance;
    }
    
    public boolean giveItem(String username, int itemId, int quantity) {
        return giveItem(username, itemId, quantity, null);
    }
    
    public boolean giveItem(String username, int itemId, int quantity, java.util.List<java.util.Map<String, Integer>> options) {
        try {
            StringBuilder json = new StringBuilder();
            json.append("{\"username\":\"").append(escapeJson(username))
                .append("\",\"itemId\":").append(itemId)
                .append(",\"quantity\":").append(quantity);
            
            if (options != null && !options.isEmpty()) {
                json.append(",\"options\":[");
                for (int i = 0; i < options.size(); i++) {
                    java.util.Map<String, Integer> option = options.get(i);
                    if (i > 0) {
                        json.append(",");
                    }
                    json.append("{\"id\":").append(option.get("id"))
                        .append(",\"param\":").append(option.get("param")).append("}");
                }
                json.append("]");
            }
            
            json.append("}");
            
            URL url = new URL(nroServerUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String escapeJson(String str) {
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

