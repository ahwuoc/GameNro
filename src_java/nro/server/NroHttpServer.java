package nro.server;

import author.TextServer;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import consts.ConstSQL;
import item.Item;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import jdbc.DBConnecter;
import nro.player.Player;
import nro.services.InventoryService;
import static nro.services.InventoryService.checkListsEqual;
import nro.services.ItemService;
import nro.services.Service;

public class NroHttpServer {
    
    private static NroHttpServer instance;
    private HttpServer server;
    private static final int DEFAULT_PORT = 8889;  
    
    private NroHttpServer() {
    }
    
    public static NroHttpServer gI() {
        if (instance == null) {
            instance = new NroHttpServer();
        }
        return instance;
    }
    
    public void start(int port) {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/nro/baotri", new MaintenanceHandler());
            server.createContext("/nro/item", new ItemHandler());
            server.createContext("/nro/bxh", new BxhHandler());
            server.createContext("/nro/trade", new TradeHandler());
            server.createContext("/nro/bag", new BagHandler());
            server.createContext("/nro/checkitem", new CheckItemHandler());
            server.createContext("/nro/checkquantity", new CheckItemQuantityHandler());
            server.setExecutor(null);
            server.start();
            utils.Logger.log(utils.Logger.BLACK, "[NRO HTTP] Server started on port " + port + "\n");
        } catch (IOException e) {
            utils.Logger.log(utils.Logger.RED, "[NRO HTTP] Failed to start server: " + e.getMessage() + "\n");
        }
    }
    
    public void start() {
        start(DEFAULT_PORT);
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            utils.Logger.log(utils.Logger.BLACK, "[NRO HTTP] Server stopped\n");
        }
    }
    
    private static class MaintenanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if ("GET".equals(method)) {
                String response = "NRO Maintenance Service is running!\n\n" +
                                "Endpoint: POST /nro/baotri\n" +
                                "Content-Type: application/json\n\n" +
                                "Example request body:\n" +
                                "{\n" +
                                "  \"minutes\": 120\n" +
                                "}\n";
                sendResponse(exchange, 200, response);
                return;
            }
            
            if (!"POST".equals(method)) {
                sendResponse(exchange, 405, "Method not allowed. Use POST or GET");
                return;
            }
            
            try {
                Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8.name());
                StringBuilder requestBody = new StringBuilder();
                while (scanner.hasNextLine()) {
                    requestBody.append(scanner.nextLine());
                }
                scanner.close();
                
                String body = requestBody.toString();
                if (body == null || body.trim().isEmpty()) {
                    sendResponse(exchange, 400, "Empty request body");
                    return;
                }
                
                int minutes = parseMinutes(body);
                if (minutes <= 0) {
                    sendResponse(exchange, 400, "Invalid minutes value. Must be positive integer.");
                    return;
                }
                
                if (Maintenance.isRunning) {
                    sendResponse(exchange, 400, "Maintenance is already running");
                    return;
                }
                
                Maintenance.gI().start(minutes);
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP] Maintenance started: " + minutes + " minutes\n");
                
                sendResponse(exchange, 200, "OK");
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP] Error handling request: " + e.getMessage() + "\n");
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error");
            }
        }
        
        private int parseMinutes(String json) {
            try {
                int startIndex = json.indexOf("\"minutes\":");
                if (startIndex == -1) {
                    startIndex = json.indexOf("\"minutes\" :");
                }
                if (startIndex == -1) {
                    startIndex = json.indexOf("minutes:");
                }
                if (startIndex == -1) {
                    return -1;
                }
                
                int valueStart = json.indexOf(":", startIndex) + 1;
                while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
                    valueStart++;
                }
                
                int valueEnd = valueStart;
                while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
                    valueEnd++;
                }
                
                if (valueEnd > valueStart) {
                    return Integer.parseInt(json.substring(valueStart, valueEnd));
                }
            } catch (Exception e) {
            }
            return -1;
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    private static class ItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if ("GET".equals(method)) {
                String response = "NRO Item Service is running!\n\n" +
                                "Endpoint: POST /nro/item\n" +
                                "Content-Type: application/json\n\n" +
                                "Example request body:\n" +
                                "{\n" +
                                "  \"username\": \"player123\",\n" +
                                "  \"itemId\": 457,\n" +
                                "  \"quantity\": 10\n" +
                                "}\n";
                sendResponse(exchange, 200, response);
                return;
            }
            
            if (!"POST".equals(method)) {
                sendResponse(exchange, 405, "Method not allowed. Use POST or GET");
                return;
            }
            
            try {
                Scanner scanner = new Scanner(exchange.getRequestBody(), StandardCharsets.UTF_8.name());
                StringBuilder requestBody = new StringBuilder();
                while (scanner.hasNextLine()) {
                    requestBody.append(scanner.nextLine());
                }
                scanner.close();
                
                String body = requestBody.toString();
                if (body == null || body.trim().isEmpty()) {
                    sendResponse(exchange, 400, "Empty request body");
                    return;
                }
                
                String username = parseString(body, "username");
                int itemId = parseInt(body, "itemId");
                int quantity = parseInt(body, "quantity");
                List<Item.ItemOption> customOptions = parseOptions(body);
                
                if (username == null || username.isEmpty()) {
                    sendResponse(exchange, 400, "Missing required field: username");
                    return;
                }
                
                if (itemId <= 0) {
                    sendResponse(exchange, 400, "Invalid itemId value. Must be positive integer.");
                    return;
                }
                
                if (quantity <= 0) {
                    sendResponse(exchange, 400, "Invalid quantity value. Must be positive integer.");
                    return;
                }
                
                Player player = Client.gI().getPlayer(username);
                if (player == null) {
                    sendResponse(exchange, 404, "Player not found or offline");
                    return;
                }
                
                Item item = ItemService.gI().createNewItem((short) itemId);
                List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
                if (!ops.isEmpty()) {
                    item.itemOptions = ops;
                }
                item.quantity = quantity;
                
                if (customOptions != null && !customOptions.isEmpty()) {
                    for (Item.ItemOption option : customOptions) {
                        item.itemOptions.add(option);
                    }
                }
                
                int bagSize = player.inventory.itemsBag.size();
                int validItemCount = 0;
                for (Item it : player.inventory.itemsBag) {
                    if (it.isNotNullItem()) {
                        validItemCount++;
                    }
                }
                
                if (validItemCount >= bagSize) {
                    boolean canStack = false;
                    if (item.template.isUpToUp) {
                        for (Item it : player.inventory.itemsBag) {
                            if (it.isNotNullItem() && it.template.id == item.template.id) {
                                boolean canMerge = false;
                                try {
                                    canMerge = checkListsEqual(it.itemOptions, item.itemOptions) || item.template.id == 2074;
                                    if (!canMerge) {
                                        try {
                                            canMerge = item.isDaNangCap() || item.isManhThienSu();
                                        } catch (Exception e) {
                                        }
                                    }
                                } catch (Exception e) {
                                }
                                
                                if (canMerge && it.quantity < 99999) {
                                    canStack = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (!canStack) {
                        sendResponse(exchange, 400, "Hành trang đã đầy");
                        return;
                    }
                }
                
                boolean success = InventoryService.gI().addItemBag(player, item);
                if (success) {
                    InventoryService.gI().sendItemBag(player);
                    Service.gI().sendThongBao(player, "Nhận " + item.template.name + " x" + quantity);
                } else {
                    sendResponse(exchange, 400, "Không thể thêm item vào hành trang");
                    return;
                }
                
                sendResponse(exchange, 200, "OK");
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal server error");
            }
        }
        
        private String parseString(String json, String key) {
            try {
                String searchKey = "\"" + key + "\":";
                int startIndex = json.indexOf(searchKey);
                if (startIndex == -1) {
                    searchKey = "\"" + key + "\" :";
                    startIndex = json.indexOf(searchKey);
                }
                if (startIndex == -1) {
                    return null;
                }
                
                int valueStart = json.indexOf(":", startIndex) + 1;
                while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
                    valueStart++;
                }
                
                if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
                    return null;
                }
                
                valueStart++;
                int valueEnd = valueStart;
                while (valueEnd < json.length() && json.charAt(valueEnd) != '"') {
                    if (json.charAt(valueEnd) == '\\' && valueEnd + 1 < json.length()) {
                        valueEnd += 2;
                    } else {
                        valueEnd++;
                    }
                }
                
                if (valueEnd > valueStart) {
                    String value = json.substring(valueStart, valueEnd);
                    return value.replace("\\\"", "\"").replace("\\\\", "\\");
                }
            } catch (Exception e) {
            }
            return null;
        }
        
        private int parseInt(String json, String key) {
            try {
                String searchKey = "\"" + key + "\":";
                int startIndex = json.indexOf(searchKey);
                if (startIndex == -1) {
                    searchKey = "\"" + key + "\" :";
                    startIndex = json.indexOf(searchKey);
                }
                if (startIndex == -1) {
                    return -1;
                }
                
                int valueStart = json.indexOf(":", startIndex) + 1;
                while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
                    valueStart++;
                }
                
                int valueEnd = valueStart;
                while (valueEnd < json.length() && Character.isDigit(json.charAt(valueEnd))) {
                    valueEnd++;
                }
                
                if (valueEnd > valueStart) {
                    return Integer.parseInt(json.substring(valueStart, valueEnd));
                }
            } catch (Exception e) {
            }
            return -1;
        }
        
        private List<Item.ItemOption> parseOptions(String json) {
            List<Item.ItemOption> options = new ArrayList<>();
            try {
                int optionsStart = json.indexOf("\"options\":[");
                if (optionsStart == -1) {
                    return options;
                }
                
                int arrayStart = json.indexOf("[", optionsStart);
                if (arrayStart == -1) {
                    return options;
                }
                
                int arrayEnd = json.indexOf("]", arrayStart);
                if (arrayEnd == -1) {
                    return options;
                }
                
                String optionsArray = json.substring(arrayStart + 1, arrayEnd);
                String[] optionStrings = optionsArray.split("\\},\\{");
                
                for (String optionStr : optionStrings) {
                    optionStr = optionStr.replace("{", "").replace("}", "").trim();
                    if (optionStr.isEmpty()) {
                        continue;
                    }
                    
                    int optionId = parseInt(optionStr, "id");
                    int optionParam = parseInt(optionStr, "param");
                    
                    if (optionId > 0 && optionParam > 0) {
                        options.add(new Item.ItemOption(optionId, optionParam));
                    }
                }
            } catch (Exception e) {
            }
            return options;
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    private static class BxhHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if (!"GET".equals(method)) {
                sendResponseStatic(exchange, 405, "Method not allowed. Use GET");
                return;
            }
            
            try {
                String query = exchange.getRequestURI().getQuery();
                
                if (query == null || query.isEmpty()) {
                    String response = "NRO BXH Service is running!\n\n" +
                                    "Endpoint: GET /nro/bxh?method=<method>&limit=<limit>\n" +
                                    "Methods: sm, nv, whis, nap\n" +
                                    "Example: GET /nro/bxh?method=sm&limit=10\n";
                    sendResponseStatic(exchange, 200, response);
                    return;
                }
                
                utils.Logger.log(utils.Logger.BLACK, "[NRO HTTP BXH] Received request, query: " + query + "\n");
                
                String methodParam = null;
                int limit = 10;
                
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        if ("method".equals(keyValue[0])) {
                            methodParam = keyValue[1].toLowerCase();
                        } else if ("limit".equals(keyValue[0])) {
                            try {
                                limit = Integer.parseInt(keyValue[1]);
                                if (limit < 1 || limit > 100) {
                                    limit = 10;
                                }
                            } catch (NumberFormatException e) {
                                limit = 10;
                            }
                        }
                    }
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] Request received - Method: " + methodParam + ", Limit: " + limit + "\n");
                
                if (methodParam == null || methodParam.isEmpty()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BXH] Error: Missing method parameter\n");
                    sendResponseStatic(exchange, 400, "Missing required parameter: method");
                    return;
                }
                
                String sqlQuery = getSqlQuery(methodParam);
                if (sqlQuery == null) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BXH] Error: Invalid method: " + methodParam + "\n");
                    sendResponseStatic(exchange, 400, "Invalid method. Use: sm, nv, whis, nap");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] Querying database...\n");
                List<BxhData> results = queryBxhFromDatabase(sqlQuery, methodParam, limit);
                
                if (results == null || results.isEmpty()) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] No data found, returning empty array\n");
                    sendResponseStatic(exchange, 200, "{\"method\":\"" + methodParam + "\",\"data\":[]}");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] Found " + results.size() + " results, building JSON response\n");
                
                JSONArray jsonArray = buildJsonResponse(results, methodParam);
                JSONObject response = new JSONObject();
                response.put("method", methodParam);
                response.put("data", jsonArray);
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] Response sent successfully\n");
                sendJsonResponseStatic(exchange, 200, response.toJSONString());
            } catch (Exception e) {
                sendResponseStatic(exchange, 500, "Internal server error");
            }
        }
        
        private static void sendResponseStatic(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private static void sendJsonResponseStatic(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        /**
         * Data class to hold BXH result from database
         */
        private static class BxhData {
            String name;
            byte gender;
            short head;
            short body;
            short leg;
            long power;
            byte nv;
            byte subnv;
            long lasttime;
            int level;
            int time;
            int cash;
        }
        
        /**
         * Get SQL query string based on method parameter
         * @param method Method name: sm, nv, whis, nap
         * @return SQL query string or null if invalid method
         */
        private String getSqlQuery(String method) {
            switch (method) {
                case "sm":
                    return ConstSQL.TOP_SM;
                case "nv":
                    return ConstSQL.TOP_NV;
                case "whis":
                    return ConstSQL.TOP_WHIS;
                case "nap":
                    return ConstSQL.TOP_NAP;
                default:
                    return null;
            }
        }
        
        /**
         * Query BXH data from database
         * @param query SQL query string
         * @param method Method name for logging
         * @param limit Maximum number of results
         * @return List of BxhData or empty list if error
         */
        private List<BxhData> queryBxhFromDatabase(String query, String method, int limit) {
            List<BxhData> results = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = DBConnecter.getConnectionServer();
                if (conn == null || conn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BXH] Error: Database connection is null or closed\n");
                    return results;
                }
                
                String limitedQuery = query.replace("LIMIT 20", "LIMIT " + limit)
                                           .replace("LIMIT 10", "LIMIT " + limit);
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] Executing query with limit: " + limit + "\n");
                ps = conn.prepareStatement(limitedQuery);
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    BxhData data = new BxhData();
                    data.name = rs.getString("name");
                    data.gender = rs.getByte("gender");
                    
                    short head = utils.Util.getHead(data.gender);
                    short body = (short) (data.gender == 1 ? 59 : 57);
                    short leg = (short) (data.gender == 1 ? 60 : 58);
                    
                    JSONArray dataArray = (JSONArray) JSONValue.parse(rs.getString("items_body"));
                    if (dataArray != null && dataArray.size() > 0) {
                        short[] headArr = new short[]{head};
                        short[] bodyArr = new short[]{body};
                        short[] legArr = new short[]{leg};
                        parseItemsBody(dataArray, headArr, bodyArr, legArr);
                        head = headArr[0];
                        body = bodyArr[0];
                        leg = legArr[0];
                    }
                    
                    data.head = head;
                    data.body = body;
                    data.leg = leg;
                    
                    switch (method) {
                        case "sm":
                            data.power = rs.getLong("sm");
                            break;
                        case "nv":
                            data.nv = rs.getByte("nv");
                            data.subnv = rs.getByte("subnv");
                            data.lasttime = rs.getLong("lasttime");
                            break;
                        case "whis":
                            data.level = rs.getInt("top");
                            data.time = rs.getInt("time");
                            data.lasttime = rs.getLong("lasttime");
                            break;
                        case "nap":
                            data.cash = rs.getInt("cash");
                            break;
                    }
                    
                    results.add(data);
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BXH] Query executed successfully, got " + results.size() + " results\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP BXH] SQL Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP BXH] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BXH] Error closing resources: " + e.getMessage() + "\n");
                }
            }
            
            return results;
        }
        
        /**
         * Parse items_body JSON to extract head, body, leg appearance
         * @param dataArray JSON array from items_body field
         * @param head Array with single element for head value (will be modified)
         * @param body Array with single element for body value (will be modified)
         * @param leg Array with single element for leg value (will be modified)
         */
        private void parseItemsBody(JSONArray dataArray, short[] head, short[] body, short[] leg) {
            if (dataArray.size() > 0) {
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(0).toString());
                if (dataItem != null && dataItem.size() > 0 && dataItem.get(0) != null) {
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId != -1) {
                        Item item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                        body[0] = (short) item.template.part;
                    }
                }
            }
            
            if (dataArray.size() > 1) {
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(1).toString());
                if (dataItem != null && dataItem.size() > 0 && dataItem.get(0) != null) {
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId != -1) {
                        Item item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                        leg[0] = (short) item.template.part;
                    }
                }
            }
            
            if (dataArray.size() > 5) {
                JSONArray dataItem = (JSONArray) JSONValue.parse(dataArray.get(5).toString());
                if (dataItem != null && dataItem.size() > 0 && dataItem.get(0) != null) {
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId != -1) {
                        Item item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataItem.get(1))));
                        if (item.template.head != -1) {
                            head[0] = (short) item.template.head;
                        }
                        if (item.template.body != -1) {
                            body[0] = (short) item.template.body;
                        }
                        if (item.template.leg != -1) {
                            leg[0] = (short) item.template.leg;
                        }
                    }
                }
            }
        }
        
        /**
         * Build JSON response from BXH data
         * @param results List of BxhData
         * @param method Method name
         * @return JSONArray containing formatted data
         */
        private JSONArray buildJsonResponse(List<BxhData> results, String method) {
            JSONArray jsonArray = new JSONArray();
            
            for (int i = 0; i < results.size(); i++) {
                BxhData data = results.get(i);
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("rank", i + 1);
                jsonObj.put("name", data.name);
                jsonObj.put("gender", data.gender);
                
                switch (method) {
                    case "sm":
                        jsonObj.put("power", data.power);
                        break;
                    case "nv":
                        jsonObj.put("nv", data.nv);
                        jsonObj.put("subnv", data.subnv);
                        jsonObj.put("lasttime", data.lasttime);
                        break;
                    case "whis":
                        jsonObj.put("level", data.level);
                        jsonObj.put("time", data.time);
                        jsonObj.put("lasttime", data.lasttime);
                        break;
                    case "nap":
                        jsonObj.put("cash", data.cash);
                        break;
                }
                
                jsonArray.add(jsonObj);
            }
            
            return jsonArray;
        }
    }
    
    /**
     * Handler for trade/transaction queries
     */
    private static class TradeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponseStatic(exchange, 405, "Method not allowed");
                    return;
                }
                
                String query = exchange.getRequestURI().getQuery();
                if (query == null || query.isEmpty()) {
                    sendResponseStatic(exchange, 400, "Missing required parameter: username");
                    return;
                }
                
                String username = null;
                int limit = 10;
                
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    if (keyValue.length == 2) {
                        if ("username".equals(keyValue[0])) {
                            username = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        } else if ("limit".equals(keyValue[0])) {
                            try {
                                limit = Integer.parseInt(keyValue[1]);
                                if (limit < 1 || limit > 50) {
                                    limit = 10;
                                }
                            } catch (NumberFormatException e) {
                                limit = 10;
                            }
                        }
                    }
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] Request received - Username: " + username + ", Limit: " + limit + "\n");
                
                if (username == null || username.isEmpty()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP TRADE] Error: Missing username parameter\n");
                    sendResponseStatic(exchange, 400, "Missing required parameter: username");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] Querying database...\n");
                List<TradeData> results = queryTransactionsFromDatabase(username, limit);
                
                if (results == null || results.isEmpty()) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] No transactions found for user: " + username + "\n");
                    sendResponseStatic(exchange, 200, "{\"data\":[]}");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] Found " + results.size() + " transactions, building JSON response\n");
                
                JSONArray jsonArray = buildTradeJsonResponse(results);
                JSONObject response = new JSONObject();
                response.put("data", jsonArray);
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] Response sent successfully\n");
                sendJsonResponseStatic(exchange, 200, response.toJSONString());
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP TRADE] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
                sendResponseStatic(exchange, 500, "Internal server error");
            }
        }
        
        private static void sendResponseStatic(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private static void sendJsonResponseStatic(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        /**
         * Data class to hold transaction result from database
         */
        private static class TradeData {
            String player1;
            String player2;
            String items1;
            String items2;
            String time;
        }
        
        /**
         * Query transactions from database by username
         * @param username Username to search for
         * @param limit Maximum number of results
         * @return List of TradeData or empty list if error
         */
        private List<TradeData> queryTransactionsFromDatabase(String username, int limit) {
            List<TradeData> results = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = DBConnecter.getConnectionServer();
                if (conn == null || conn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP TRADE] Error: Database connection is null or closed\n");
                    return results;
                }
                
                String sql = "SELECT player_1, player_2, item_player_1, item_player_2, time_tran " +
                             "FROM history_transaction " +
                             "WHERE player_1 LIKE ? OR player_2 LIKE ? " +
                             "ORDER BY time_tran DESC LIMIT ?";
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] Executing query with limit: " + limit + "\n");
                ps = conn.prepareStatement(sql);
                String searchPattern = "%" + username + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
                ps.setInt(3, limit);
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    TradeData data = new TradeData();
                    data.player1 = rs.getString("player_1");
                    data.player2 = rs.getString("player_2");
                    data.items1 = rs.getString("item_player_1");
                    data.items2 = rs.getString("item_player_2");
                    
                    java.sql.Timestamp timestamp = rs.getTimestamp("time_tran");
                    if (timestamp != null) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                        data.time = sdf.format(timestamp);
                    } else {
                        data.time = "N/A";
                    }
                    
                    results.add(data);
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP TRADE] Query executed successfully, got " + results.size() + " results\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP TRADE] SQL Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP TRADE] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP TRADE] Error closing resources: " + e.getMessage() + "\n");
                }
            }
            
            return results;
        }
        
        /**
         * Build JSON response from transaction data
         * @param results List of TradeData
         * @return JSONArray containing formatted data
         */
        private JSONArray buildTradeJsonResponse(List<TradeData> results) {
            JSONArray jsonArray = new JSONArray();
            
            for (TradeData data : results) {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("player1", data.player1);
                jsonObj.put("player2", data.player2);
                jsonObj.put("items1", data.items1 != null ? data.items1 : "");
                jsonObj.put("items2", data.items2 != null ? data.items2 : "");
                jsonObj.put("time", data.time);
                jsonArray.add(jsonObj);
            }
            
            return jsonArray;
        }
    }
    
    /**
     * Handler for player bag/inventory queries
     */
    private static class BagHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponseStatic(exchange, 405, "Method not allowed");
                    return;
                }
                
                String query = exchange.getRequestURI().getQuery();
                if (query == null || query.isEmpty()) {
                    sendResponseStatic(exchange, 400, "Missing required parameter: username");
                    return;
                }
                
                String username = null;
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=", 2);
                    if (keyValue.length == 2 && "username".equals(keyValue[0])) {
                        username = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                        break;
                    }
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Request received - Username: " + username + "\n");
                
                if (username == null || username.isEmpty()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error: Missing username parameter\n");
                    sendResponseStatic(exchange, 400, "Missing required parameter: username");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Querying database...\n");
                List<BagItemData> items = queryBagFromDatabase(username);
                
                if (items == null) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Player not found: " + username + "\n");
                    sendResponseStatic(exchange, 404, "Player not found");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Found " + items.size() + " items, building JSON response\n");
                
                JSONArray jsonArray = buildBagJsonResponse(items);
                JSONObject response = new JSONObject();
                response.put("items", jsonArray);
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Response sent successfully\n");
                sendJsonResponseStatic(exchange, 200, response.toJSONString());
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
                sendResponseStatic(exchange, 500, "Internal server error");
            }
        }
        
        /**
         * Data class to hold bag item result
         */
        private static class BagItemData {
            int itemId;
            String itemName;
            int quantity;
        }
        
        /**
         * Query bag items from database by username
         * @param username Username to search for
         * @return List of BagItemData or null if player not found
         */
        private List<BagItemData> queryBagFromDatabase(String username) {
            List<BagItemData> results = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = DBConnecter.getConnectionServer();
                if (conn == null || conn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error: Database connection is null or closed\n");
                    return null;
                }
                
                String sql = "SELECT items_bag FROM player WHERE name = ?";
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Executing query for player: " + username + "\n");
                ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                rs = ps.executeQuery();
                
                if (!rs.next()) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Player not found in database\n");
                    return null;
                }
                
                String itemsBagJson = rs.getString("items_bag");
                if (itemsBagJson == null || itemsBagJson.isEmpty() || itemsBagJson.equals("[]")) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Player bag is empty\n");
                    return results;
                }
                
                JSONArray itemsArray = (JSONArray) JSONValue.parse(itemsBagJson);
                if (itemsArray == null || itemsArray.isEmpty()) {
                    return results;
                }
                
                Map<Integer, String> itemNameMap = getItemNameMap(conn);
                
                for (int i = 0; i < itemsArray.size(); i++) {
                    Object obj = itemsArray.get(i);
                    if (obj == null) {
                        continue;
                    }
                    
                    JSONArray dataItem = (JSONArray) JSONValue.parse(obj.toString());
                    if (dataItem == null || dataItem.isEmpty()) {
                        continue;
                    }
                    
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId == -1) {
                        continue;
                    }
                    
                    int quantity = 1;
                    if (dataItem.size() > 1) {
                        quantity = Integer.parseInt(String.valueOf(dataItem.get(1)));
                    }
                    
                    BagItemData data = new BagItemData();
                    data.itemId = tempId;
                    data.quantity = quantity;
                    data.itemName = itemNameMap.getOrDefault((int) tempId, "Unknown Item (ID: " + tempId + ")");
                    
                    results.add(data);
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Query executed successfully, got " + results.size() + " items\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] SQL Error: " + e.getMessage() + "\n");
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error closing resources: " + e.getMessage() + "\n");
                }
            }
            
            return results;
        }
        
        /**
         * Get item name map from item_template table
         * @param conn Database connection (should be from getConnection_Data for item_template)
         * @return Map of item ID to item name
         */
        private Map<Integer, String> getItemNameMap(Connection conn) {
            Map<Integer, String> nameMap = new HashMap<>();
            Connection dataConn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                dataConn = DBConnecter.getConnectionServer();
                if (dataConn == null || dataConn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error: Data database connection is null or closed\n");
                    return nameMap;
                }
                
                String sql = "SELECT id, name FROM item_template";
                ps = dataConn.prepareStatement(sql);
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    nameMap.put(id, name);
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP BAG] Loaded " + nameMap.size() + " item names from template\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP BAG] Error loading item template: " + e.getMessage() + "\n");
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (dataConn != null) dataConn.close();
                } catch (SQLException e) {
                }
            }
            
            return nameMap;
        }
        
        /**
         * Build JSON response from bag items
         * @param items List of BagItemData
         * @return JSONArray containing formatted data
         */
        private JSONArray buildBagJsonResponse(List<BagItemData> items) {
            JSONArray jsonArray = new JSONArray();
            
            for (BagItemData item : items) {
                JSONObject jsonObj = new JSONObject();
                jsonObj.put("id", item.itemId);
                jsonObj.put("name", item.itemName);
                jsonObj.put("quantity", item.quantity);
                jsonArray.add(jsonObj);
            }
            
            return jsonArray;
        }
        
        private static void sendResponseStatic(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private static void sendJsonResponseStatic(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    /**
     * Handler for checking item options across all players
     */
    private static class CheckItemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponseStatic(exchange, 405, "Method not allowed");
                    return;
                }
                
                String query = exchange.getRequestURI().getQuery();
                if (query == null || query.isEmpty()) {
                    sendResponseStatic(exchange, 400, "Missing required parameter: option");
                    return;
                }
                
                List<OptionCheck> optionsToCheck = new ArrayList<>();
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("option=")) {
                        String optionStr = param.substring(7);
                        String[] parts = optionStr.split(":");
                        if (parts.length == 2) {
                            try {
                                int optionId = Integer.parseInt(parts[0].trim());
                                int requiredParam = Integer.parseInt(parts[1].trim());
                                OptionCheck check = new OptionCheck();
                                check.optionId = optionId;
                                check.requiredParam = requiredParam;
                                optionsToCheck.add(check);
                            } catch (NumberFormatException e) {
                                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Invalid option format: " + optionStr + "\n");
                            }
                        }
                    }
                }
                
                if (optionsToCheck.isEmpty()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] No valid options to check\n");
                    sendResponseStatic(exchange, 400, "No valid options provided");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Request received - Checking " + optionsToCheck.size() + " option(s)\n");
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Querying database for all players...\n");
                List<PlayerItemData> results = queryAllPlayersItems(optionsToCheck);
                
                if (results == null || results.isEmpty()) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] No players found with matching options\n");
                    sendResponseStatic(exchange, 200, "{\"data\":[]}");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Found " + results.size() + " players, building JSON response\n");
                
                JSONArray jsonArray = buildCheckItemJsonResponse(results);
                JSONObject response = new JSONObject();
                response.put("data", jsonArray);
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Response sent successfully\n");
                sendJsonResponseStatic(exchange, 200, response.toJSONString());
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
                sendResponseStatic(exchange, 500, "Internal server error");
            }
        }
        
        /**
         * Data class for option check criteria
         */
        private static class OptionCheck {
            int optionId;
            int requiredParam;
        }
        
        /**
         * Data class to hold player item data with matching options
         */
        private static class PlayerItemData {
            String playerName;
            List<ItemWithOptions> items = new ArrayList<>();
        }
        
        /**
         * Data class to hold item with matching options
         */
        private static class ItemWithOptions {
            int itemId;
            String itemName;
            List<OptionData> options = new ArrayList<>();
        }
        
        /**
         * Data class to hold option data
         */
        private static class OptionData {
            int optionId;
            int param;
        }
        
        /**
         * Query all players' items and check for matching options
         * @param optionsToCheck List of options to check
         * @return List of PlayerItemData with matching items
         */
        private List<PlayerItemData> queryAllPlayersItems(List<OptionCheck> optionsToCheck) {
            List<PlayerItemData> results = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = DBConnecter.getConnectionServer();
                if (conn == null || conn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error: Database connection is null or closed\n");
                    return results;
                }
                
                String sql = "SELECT name, items_bag, items_body FROM player";
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Executing query for all players\n");
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                
                Map<Integer, String> itemNameMap = getItemNameMap(conn);
                
                int playerCount = 0;
                while (rs.next()) {
                    playerCount++;
                    String playerName = rs.getString("name");
                    
                    PlayerItemData playerData = new PlayerItemData();
                    playerData.playerName = playerName;
                    
                    List<ItemWithOptions> matchingItems = new ArrayList<>();
                    
                    String itemsBagJson = rs.getString("items_bag");
                    if (itemsBagJson != null && !itemsBagJson.isEmpty() && !itemsBagJson.equals("[]")) {
                        List<ItemWithOptions> bagItems = parseItems(itemsBagJson, itemNameMap, optionsToCheck);
                        matchingItems.addAll(bagItems);
                    }
                    
                    String itemsBodyJson = rs.getString("items_body");
                    if (itemsBodyJson != null && !itemsBodyJson.isEmpty() && !itemsBodyJson.equals("[]")) {
                        List<ItemWithOptions> bodyItems = parseItems(itemsBodyJson, itemNameMap, optionsToCheck);
                        matchingItems.addAll(bodyItems);
                    }
                    
                    if (!matchingItems.isEmpty()) {
                        playerData.items = matchingItems;
                        results.add(playerData);
                    }
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Checked " + playerCount + " players, found " + results.size() + " with matching options\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] SQL Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error closing resources: " + e.getMessage() + "\n");
                }
            }
            
            return results;
        }
        
        /**
         * Parse items JSON and check for matching options
         * @param itemsJson JSON string of items
         * @param itemNameMap Map of item ID to name
         * @param optionsToCheck List of options to check
         * @return List of items with matching options
         */
        private List<ItemWithOptions> parseItems(String itemsJson, Map<Integer, String> itemNameMap, List<OptionCheck> optionsToCheck) {
            List<ItemWithOptions> matchingItems = new ArrayList<>();
            
            try {
                JSONArray itemsArray = (JSONArray) JSONValue.parse(itemsJson);
                if (itemsArray == null || itemsArray.isEmpty()) {
                    return matchingItems;
                }
                
                for (int i = 0; i < itemsArray.size(); i++) {
                    Object obj = itemsArray.get(i);
                    if (obj == null) {
                        continue;
                    }
                    
                    JSONArray dataItem = (JSONArray) JSONValue.parse(obj.toString());
                    if (dataItem == null || dataItem.isEmpty()) {
                        continue;
                    }
                    
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId == -1) {
                        continue;
                    }
                    
                    JSONArray optionsArray = null;
                    if (dataItem.size() > 2) {
                        String optionsStr = String.valueOf(dataItem.get(2)).replaceAll("\"", "");
                        optionsArray = (JSONArray) JSONValue.parse(optionsStr);
                    }
                    
                    if (optionsArray == null || optionsArray.isEmpty()) {
                        continue;
                    }
                    
                    List<OptionData> itemOptions = new ArrayList<>();
                    boolean hasMatchingOption = false;
                    
                    for (int j = 0; j < optionsArray.size(); j++) {
                        JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(optionsArray.get(j)));
                        if (opt == null || opt.size() < 2) {
                            continue;
                        }
                        
                        int optId = Integer.parseInt(String.valueOf(opt.get(0)));
                        int optParam = Integer.parseInt(String.valueOf(opt.get(1)));
                        
                        OptionData optData = new OptionData();
                        optData.optionId = optId;
                        optData.param = optParam;
                        itemOptions.add(optData);
                        
                        for (OptionCheck check : optionsToCheck) {
                            if (optId == check.optionId && optParam >= check.requiredParam) {
                                hasMatchingOption = true;
                                break;
                            }
                        }
                    }
                    
                    if (hasMatchingOption) {
                        ItemWithOptions item = new ItemWithOptions();
                        item.itemId = tempId;
                        item.itemName = itemNameMap.getOrDefault((int) tempId, "Unknown Item (ID: " + tempId + ")");
                        item.options = itemOptions;
                        matchingItems.add(item);
                    }
                }
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error parsing items: " + e.getMessage() + "\n");
            }
            
            return matchingItems;
        }
        
        /**
         * Get item name map from item_template table
         * @param conn Database connection
         * @return Map of item ID to item name
         */
        private Map<Integer, String> getItemNameMap(Connection conn) {
            Map<Integer, String> nameMap = new HashMap<>();
            Connection dataConn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                dataConn = DBConnecter.getConnectionServer();
                if (dataConn == null || dataConn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error: Data database connection is null or closed\n");
                    return nameMap;
                }
                
                String sql = "SELECT id, name FROM item_template";
                ps = dataConn.prepareStatement(sql);
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    nameMap.put(id, name);
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKITEM] Loaded " + nameMap.size() + " item names from template\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKITEM] Error loading item template: " + e.getMessage() + "\n");
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (dataConn != null) dataConn.close();
                } catch (SQLException e) {
                }
            }
            
            return nameMap;
        }
        
        /**
         * Build JSON response from player item data
         * @param results List of PlayerItemData
         * @return JSONArray containing formatted data
         */
        private JSONArray buildCheckItemJsonResponse(List<PlayerItemData> results) {
            JSONArray jsonArray = new JSONArray();
            
            for (PlayerItemData playerData : results) {
                JSONObject playerObj = new JSONObject();
                playerObj.put("player", playerData.playerName);
                
                JSONArray itemsArray = new JSONArray();
                for (ItemWithOptions item : playerData.items) {
                    JSONObject itemObj = new JSONObject();
                    itemObj.put("id", item.itemId);
                    itemObj.put("name", item.itemName);
                    
                    JSONArray optionsArray = new JSONArray();
                    for (OptionData opt : item.options) {
                        JSONObject optObj = new JSONObject();
                        optObj.put("id", opt.optionId);
                        optObj.put("param", opt.param);
                        optionsArray.add(optObj);
                    }
                    itemObj.put("options", optionsArray);
                    itemsArray.add(itemObj);
                }
                playerObj.put("items", itemsArray);
                jsonArray.add(playerObj);
            }
            
            return jsonArray;
        }
        
        private static void sendResponseStatic(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private static void sendJsonResponseStatic(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    /**
     * Handler for checking item quantity across all players
     */
    private static class CheckItemQuantityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    sendResponseStatic(exchange, 405, "Method not allowed");
                    return;
                }
                
                String query = exchange.getRequestURI().getQuery();
                if (query == null || query.isEmpty()) {
                    sendResponseStatic(exchange, 400, "Missing required parameter: item");
                    return;
                }
                
                List<ItemQuantityCheck> itemsToCheck = new ArrayList<>();
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("item=")) {
                        String itemStr = param.substring(5);
                        String[] parts = itemStr.split(":");
                        if (parts.length == 2) {
                            try {
                                int itemId = Integer.parseInt(parts[0].trim());
                                int requiredQuantity = Integer.parseInt(parts[1].trim());
                                ItemQuantityCheck check = new ItemQuantityCheck();
                                check.itemId = itemId;
                                check.requiredQuantity = requiredQuantity;
                                itemsToCheck.add(check);
                            } catch (NumberFormatException e) {
                                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Invalid item format: " + itemStr + "\n");
                            }
                        }
                    }
                }
                
                if (itemsToCheck.isEmpty()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] No valid items to check\n");
                    sendResponseStatic(exchange, 400, "No valid items provided");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Request received - Checking " + itemsToCheck.size() + " item(s)\n");
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Querying database for all players...\n");
                List<PlayerItemQuantityData> results = queryAllPlayersItemQuantity(itemsToCheck);
                
                if (results == null || results.isEmpty()) {
                    utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] No players found with matching items\n");
                    sendResponseStatic(exchange, 200, "{\"data\":[]}");
                    return;
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Found " + results.size() + " players, building JSON response\n");
                
                JSONArray jsonArray = buildCheckQuantityJsonResponse(results);
                JSONObject response = new JSONObject();
                response.put("data", jsonArray);
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Response sent successfully\n");
                sendJsonResponseStatic(exchange, 200, response.toJSONString());
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
                sendResponseStatic(exchange, 500, "Internal server error");
            }
        }
        
        /**
         * Data class for item quantity check criteria
         */
        private static class ItemQuantityCheck {
            int itemId;
            int requiredQuantity;
        }
        
        /**
         * Data class to hold player item quantity data
         */
        private static class PlayerItemQuantityData {
            String playerName;
            List<ItemQuantityData> items = new ArrayList<>();
        }
        
        /**
         * Data class to hold item quantity data
         */
        private static class ItemQuantityData {
            int itemId;
            String itemName;
            int quantity;
        }
        
        /**
         * Query all players' items and check for matching quantities
         * @param itemsToCheck List of items to check
         * @return List of PlayerItemQuantityData with matching items
         */
        private List<PlayerItemQuantityData> queryAllPlayersItemQuantity(List<ItemQuantityCheck> itemsToCheck) {
            List<PlayerItemQuantityData> results = new ArrayList<>();
            Connection conn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                conn = DBConnecter.getConnectionServer();
                if (conn == null || conn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error: Database connection is null or closed\n");
                    return results;
                }
                
                String sql = "SELECT name, items_bag, items_body FROM player";
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Executing query for all players\n");
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                
                Map<Integer, String> itemNameMap = getItemNameMap(conn);
                
                Map<Integer, Integer> itemQuantityMap = new HashMap<>();
                for (ItemQuantityCheck check : itemsToCheck) {
                    itemQuantityMap.put(check.itemId, check.requiredQuantity);
                }
                
                int playerCount = 0;
                while (rs.next()) {
                    playerCount++;
                    String playerName = rs.getString("name");
                    
                    PlayerItemQuantityData playerData = new PlayerItemQuantityData();
                    playerData.playerName = playerName;
                    
                    List<ItemQuantityData> matchingItems = new ArrayList<>();
                    
                    String itemsBagJson = rs.getString("items_bag");
                    if (itemsBagJson != null && !itemsBagJson.isEmpty() && !itemsBagJson.equals("[]")) {
                        List<ItemQuantityData> bagItems = parseItemsQuantity(itemsBagJson, itemNameMap, itemQuantityMap);
                        matchingItems.addAll(bagItems);
                    }
                    
                    String itemsBodyJson = rs.getString("items_body");
                    if (itemsBodyJson != null && !itemsBodyJson.isEmpty() && !itemsBodyJson.equals("[]")) {
                        List<ItemQuantityData> bodyItems = parseItemsQuantity(itemsBodyJson, itemNameMap, itemQuantityMap);
                        matchingItems.addAll(bodyItems);
                    }
                    
                    if (!matchingItems.isEmpty()) {
                        Map<Integer, ItemQuantityData> itemMap = new HashMap<>();
                        for (ItemQuantityData item : matchingItems) {
                            if (itemMap.containsKey(item.itemId)) {
                                ItemQuantityData existing = itemMap.get(item.itemId);
                                existing.quantity += item.quantity;
                            } else {
                                itemMap.put(item.itemId, item);
                            }
                        }
                        
                        for (ItemQuantityCheck check : itemsToCheck) {
                            if (itemMap.containsKey(check.itemId)) {
                                ItemQuantityData item = itemMap.get(check.itemId);
                                if (item.quantity >= check.requiredQuantity) {
                                    playerData.items.add(item);
                                }
                            }
                        }
                        
                        if (!playerData.items.isEmpty()) {
                            results.add(playerData);
                        }
                    }
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Checked " + playerCount + " players, found " + results.size() + " with matching items\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] SQL Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error: " + e.getMessage() + "\n");
                e.printStackTrace();
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (conn != null) conn.close();
                } catch (SQLException e) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error closing resources: " + e.getMessage() + "\n");
                }
            }
            
            return results;
        }
        
        /**
         * Parse items JSON and check for matching quantities
         * @param itemsJson JSON string of items
         * @param itemNameMap Map of item ID to name
         * @param itemQuantityMap Map of item ID to required quantity
         * @return List of items with matching quantities
         */
        private List<ItemQuantityData> parseItemsQuantity(String itemsJson, Map<Integer, String> itemNameMap, Map<Integer, Integer> itemQuantityMap) {
            List<ItemQuantityData> matchingItems = new ArrayList<>();
            
            try {
                JSONArray itemsArray = (JSONArray) JSONValue.parse(itemsJson);
                if (itemsArray == null || itemsArray.isEmpty()) {
                    return matchingItems;
                }
                
                for (int i = 0; i < itemsArray.size(); i++) {
                    Object obj = itemsArray.get(i);
                    if (obj == null) {
                        continue;
                    }
                    
                    JSONArray dataItem = (JSONArray) JSONValue.parse(obj.toString());
                    if (dataItem == null || dataItem.isEmpty()) {
                        continue;
                    }
                    
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId == -1) {
                        continue;
                    }
                    
                    int quantity = 1;
                    if (dataItem.size() > 1) {
                        try {
                            quantity = Integer.parseInt(String.valueOf(dataItem.get(1)));
                        } catch (NumberFormatException e) {
                            quantity = 1;
                        }
                    }
                    
                    if (itemQuantityMap.containsKey((int) tempId)) {
                        ItemQuantityData item = new ItemQuantityData();
                        item.itemId = tempId;
                        item.itemName = itemNameMap.getOrDefault((int) tempId, "Unknown Item (ID: " + tempId + ")");
                        item.quantity = quantity;
                        matchingItems.add(item);
                    }
                }
            } catch (Exception e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error parsing items: " + e.getMessage() + "\n");
            }
            
            return matchingItems;
        }
        
        /**
         * Get item name map from item_template table
         * @param conn Database connection
         * @return Map of item ID to item name
         */
        private Map<Integer, String> getItemNameMap(Connection conn) {
            Map<Integer, String> nameMap = new HashMap<>();
            Connection dataConn = null;
            PreparedStatement ps = null;
            ResultSet rs = null;
            
            try {
                dataConn = DBConnecter.getConnectionServer();
                if (dataConn == null || dataConn.isClosed()) {
                    utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error: Data database connection is null or closed\n");
                    return nameMap;
                }
                
                String sql = "SELECT id, name FROM item_template";
                ps = dataConn.prepareStatement(sql);
                rs = ps.executeQuery();
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    nameMap.put(id, name);
                }
                
                utils.Logger.log(utils.Logger.YELLOW, "[NRO HTTP CHECKQUANTITY] Loaded " + nameMap.size() + " item names from template\n");
                
            } catch (SQLException e) {
                utils.Logger.log(utils.Logger.RED, "[NRO HTTP CHECKQUANTITY] Error loading item template: " + e.getMessage() + "\n");
            } finally {
                try {
                    if (rs != null) rs.close();
                    if (ps != null) ps.close();
                    if (dataConn != null) dataConn.close();
                } catch (SQLException e) {
                }
            }
            
            return nameMap;
        }
        
        /**
         * Build JSON response from player item quantity data
         * @param results List of PlayerItemQuantityData
         * @return JSONArray containing formatted data
         */
        private JSONArray buildCheckQuantityJsonResponse(List<PlayerItemQuantityData> results) {
            JSONArray jsonArray = new JSONArray();
            
            for (PlayerItemQuantityData playerData : results) {
                JSONObject playerObj = new JSONObject();
                playerObj.put("player", playerData.playerName);
                
                JSONArray itemsArray = new JSONArray();
                for (ItemQuantityData item : playerData.items) {
                    JSONObject itemObj = new JSONObject();
                    itemObj.put("id", item.itemId);
                    itemObj.put("name", item.itemName);
                    itemObj.put("quantity", item.quantity);
                    itemsArray.add(itemObj);
                }
                playerObj.put("items", itemsArray);
                jsonArray.add(playerObj);
            }
            
            return jsonArray;
        }
        
        private static void sendResponseStatic(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private static void sendJsonResponseStatic(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] responseBytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }


