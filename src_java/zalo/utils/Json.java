/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class Json {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    public static Object parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new RuntimeException("Invalid JSON: empty string");
        }
        
        json = json.trim();
        
        if (json.startsWith("[") && json.endsWith("]")) {
            return parseArray(json);
        }
        
        if (json.startsWith("{") && json.endsWith("}")) {
            return parseObject(json);
        }
        
        throw new RuntimeException("Invalid JSON: " + (json.length() > 100 ? json.substring(0, 100) + "..." : json));
    }
    
    public static Map<String, Object> parseAsMap(String json) {
        Object parsed = parse(json);
        if (parsed instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) parsed;
            return result;
        }
        throw new RuntimeException("Expected JSON object, got: " + (parsed != null ? parsed.getClass() : "null"));
    }
    
    public static Map<String, Object> parseObject(String json) {
        Map<String, Object> result = new HashMap<>();
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new RuntimeException("Invalid JSON object: " + json);
        }
        
        json = json.substring(1, json.length() - 1).trim();
        
        String[] pairs = splitJsonPairs(json);
        for (String pair : pairs) {
            String[] keyValue = pair.split(":", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim();
                result.put(key, parseValue(value));
            }
        }
        
        return result;
    }
    
    public static List<Object> parseArray(String json) {
        List<Object> result = new ArrayList<>();
        
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new RuntimeException("Invalid JSON array: " + json);
        }
        
        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) {
            return result;
        }
        
        String[] elements = splitJsonPairs(json);
        for (String element : elements) {
            element = element.trim();
            if (element.isEmpty()) continue;
            
            if (element.startsWith("{")) {
                Map<String, Object> obj = parseObject(element);
                result.add(obj);
            } else if (element.startsWith("[")) {
                result.add(parseArray(element));
            } else {
                result.add(parseValue(element));
            }
        }
        
        return result;
    }
    
    public static String stringify(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;
            return stringifyMap(map);
        }
        
        if (obj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) obj;
            return stringifyList(list);
        }
        
        return stringifyValue(obj);
    }
    
    public static String stringifyMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escape(entry.getKey())).append("\":");
            sb.append(stringifyValue(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
    
    public static String stringifyList(List<Object> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                sb.append(",");
            }
            sb.append(stringifyValue(item));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String[] splitJsonPairs(String json) {
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean inString = false;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{' || c == '[') {
                    depth++;
                } else if (c == '}' || c == ']') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    pairs.add(json.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        if (start < json.length()) {
            pairs.add(json.substring(start).trim());
        }
        
        return pairs.toArray(new String[0]);
    }
    
    private static Object parseValue(String value) {
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            String str = value.substring(1, value.length() - 1);
            return decodeUnicode(str);
        } else if (value.equals("true")) {
            return true;
        } else if (value.equals("false")) {
            return false;
        } else if (value.equals("null")) {
            return null;
        } else if (value.startsWith("{") && value.endsWith("}")) {
            return parseObject(value);
        } else if (value.startsWith("[") && value.endsWith("]")) {
            return parseArray(value);
        } else {
            try {
                if (value.contains(".")) {
                    return Double.parseDouble(value);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException e) {
                return value;
            }
        }
    }
    
    private static String decodeUnicode(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            char c = str.charAt(i);
            if (c == '\\' && i + 1 < str.length()) {
                char next = str.charAt(i + 1);
                if (next == 'u' && i + 5 < str.length()) {
                    try {
                        String hex = str.substring(i + 2, i + 6);
                        int codePoint = Integer.parseInt(hex, 16);
                        result.append((char) codePoint);
                        i += 6;
                        continue;
                    } catch (NumberFormatException e) {
                        result.append(c);
                        i++;
                    }
                } else if (next == '\\') {
                    result.append('\\');
                    i += 2;
                    continue;
                } else if (next == '"') {
                    result.append('"');
                    i += 2;
                    continue;
                } else if (next == 'n') {
                    result.append('\n');
                    i += 2;
                    continue;
                } else if (next == 'r') {
                    result.append('\r');
                    i += 2;
                    continue;
                } else if (next == 't') {
                    result.append('\t');
                    i += 2;
                    continue;
                } else {
                    result.append(c);
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }
    
    private static String stringifyValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escape((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            return stringifyMap(map);
        } else if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            return stringifyList(list);
        } else {
            return "\"" + escape(value.toString()) + "\"";
        }
    }
    
    private static String escape(String str) {
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

