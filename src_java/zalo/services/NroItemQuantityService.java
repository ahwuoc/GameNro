package zalo.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.io.FileInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class NroItemQuantityService {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static NroItemQuantityService instance;
    private String nroServerUrl;
    
    private NroItemQuantityService() {
        loadConfig();
    }
    
    public static NroItemQuantityService gI() {
        if (instance == null) {
            instance = new NroItemQuantityService();
        }
        return instance;
    }
    
    private void loadConfig() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("settings/zalo.properties"));
            String nroHost = props.getProperty("nro.host", "localhost");
            int nroPort = Integer.parseInt(props.getProperty("nro.port", "8889"));
            nroServerUrl = "http://" + nroHost + ":" + nroPort + "/nro/checkquantity";
        } catch (Exception e) {
            nroServerUrl = "http://localhost:8889/nro/checkquantity";
        }
    }
    
    public List<Map<String, Object>> checkItemQuantity(List<Map<String, Integer>> items) {
        try {
            StringBuilder urlBuilder = new StringBuilder(nroServerUrl + "?");
            for (int i = 0; i < items.size(); i++) {
                Map<String, Integer> item = items.get(i);
                if (i > 0) {
                    urlBuilder.append("&");
                }
                urlBuilder.append("item=").append(item.get("id")).append(":").append(item.get("quantity"));
            }
            
            String urlStr = urlBuilder.toString();
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return new ArrayList<>();
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            JSONParser parser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
            JSONArray dataArray = (JSONArray) jsonResponse.get("data");
            
            List<Map<String, Object>> results = new ArrayList<>();
            if (dataArray != null) {
                for (Object obj : dataArray) {
                    JSONObject playerObj = (JSONObject) obj;
                    Map<String, Object> result = new HashMap<>();
                    result.put("player", playerObj.get("player"));
                    
                    JSONArray itemsArray = (JSONArray) playerObj.get("items");
                    List<Map<String, Object>> itemsList = new ArrayList<>();
                    if (itemsArray != null) {
                        for (Object itemObj : itemsArray) {
                            JSONObject item = (JSONObject) itemObj;
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("id", ((Long) item.get("id")).intValue());
                            itemMap.put("name", item.get("name"));
                            itemMap.put("quantity", ((Long) item.get("quantity")).intValue());
                            itemsList.add(itemMap);
                        }
                    }
                    result.put("items", itemsList);
                    results.add(result);
                }
            }
            
            return results;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

