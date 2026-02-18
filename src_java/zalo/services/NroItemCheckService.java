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

public class NroItemCheckService {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    private static NroItemCheckService instance;
    private String nroServerUrl;
    
    private NroItemCheckService() {
        loadConfig();
    }
    
    public static NroItemCheckService gI() {
        if (instance == null) {
            instance = new NroItemCheckService();
        }
        return instance;
    }
    
    private void loadConfig() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("settings/zalo.properties"));
            String nroHost = props.getProperty("nro.host", "localhost");
            int nroPort = Integer.parseInt(props.getProperty("nro.port", "8889"));
            nroServerUrl = "http://" + nroHost + ":" + nroPort + "/nro/checkitem";
        } catch (Exception e) {
            nroServerUrl = "http://localhost:8889/nro/checkitem";
        }
    }
    
    public List<Map<String, Object>> checkItemOptions(List<Map<String, Integer>> options) {
        try {
            StringBuilder urlBuilder = new StringBuilder(nroServerUrl + "?");
            for (int i = 0; i < options.size(); i++) {
                Map<String, Integer> opt = options.get(i);
                if (i > 0) {
                    urlBuilder.append("&");
                }
                urlBuilder.append("option=").append(opt.get("id")).append(":").append(opt.get("param"));
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
                    List<Map<String, Object>> items = new ArrayList<>();
                    if (itemsArray != null) {
                        for (Object itemObj : itemsArray) {
                            JSONObject item = (JSONObject) itemObj;
                            Map<String, Object> itemMap = new HashMap<>();
                            itemMap.put("id", ((Long) item.get("id")).intValue());
                            itemMap.put("name", item.get("name"));
                            
                            JSONArray optsArray = (JSONArray) item.get("options");
                            List<Map<String, Object>> opts = new ArrayList<>();
                            if (optsArray != null) {
                                for (Object optObj : optsArray) {
                                    JSONObject opt = (JSONObject) optObj;
                                    Map<String, Object> optMap = new HashMap<>();
                                    optMap.put("id", ((Long) opt.get("id")).intValue());
                                    optMap.put("param", ((Long) opt.get("param")).intValue());
                                    opts.add(optMap);
                                }
                            }
                            itemMap.put("options", opts);
                            items.add(itemMap);
                        }
                    }
                    result.put("items", items);
                    results.add(result);
                }
            }
            
            return results;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}

