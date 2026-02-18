/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class Crypto {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    public static String decryptResp(String key, String data) {
        try {
            String decrypted = decodeRespAES(key, data);
            return decrypted;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String decodeRespAES(String key, String data) throws Exception {
        data = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8.name());
        byte[] keyBytes = Base64.getDecoder().decode(key);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        
        byte[] iv = new byte[16];
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        byte[] encryptedBytes = Base64.getDecoder().decode(data);
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        byte[] decrypted = cipher.doFinal(encryptedBytes);
        
        return new String(decrypted, StandardCharsets.UTF_8);
    }
    
    public static String encodeAES(String secretKey, String data) {
        return encodeAES(secretKey, data, 0);
    }
    
    private static String encodeAES(String secretKey, String data, int retry) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            
            byte[] iv = new byte[16]; 
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            if (retry < 3) {
                return encodeAES(secretKey, data, retry + 1);
            }
            return null;
        }
    }
    
    public static String decodeAES(String secretKey, String data) {
        return decodeAES(secretKey, data, 0);
    }
    
    private static String decodeAES(String secretKey, String data, int retry) {
        try {
            String decodedData = data;
            if (data.contains("%")) {
                try {
                    decodedData = java.net.URLDecoder.decode(data, StandardCharsets.UTF_8.name());
                } catch (Exception e) {
                    decodedData = data;
                }
            }
            decodedData = decodedData.trim();
            StringBuilder sb = new StringBuilder();
            for (char c : decodedData.toCharArray()) {
                if (c != ' ' && c != '\n' && c != '\r' && c != '\t') {
                    sb.append(c);
                }
            }
            decodedData = sb.toString();
            
            int remainder = decodedData.length() % 4;
            if (remainder > 0) {
                decodedData += "=".repeat(4 - remainder);
            }
            
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            
            byte[] iv = new byte[16]; 
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            byte[] encryptedBytes = null;
            Exception lastException = null;
            
            try {
                encryptedBytes = Base64.getDecoder().decode(decodedData);
            } catch (IllegalArgumentException e) {
                lastException = e;
                try {
                    encryptedBytes = Base64.getMimeDecoder().decode(decodedData);
                } catch (Exception e2) {
                    lastException = e2;
                    try {
                        encryptedBytes = Base64.getUrlDecoder().decode(decodedData);
                    } catch (Exception e3) {
                        lastException = e3;
                        try {
                            String fixed = decodedData;
                            while (fixed.length() % 4 != 0) {
                                fixed += "=";
                            }
                            encryptedBytes = Base64.getDecoder().decode(fixed);
                        } catch (Exception e4) {
                            throw new IllegalArgumentException("Failed to decode base64 after all attempts: " + e.getMessage(), lastException);
                        }
                    }
                }
            }
            
            if (encryptedBytes == null) {
                throw new IllegalArgumentException("Failed to decode base64");
            }
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(encryptedBytes);
            
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (retry == 0) {
                System.err.println("[Crypto] decodeAES error (attempt " + (retry + 1) + "): " + e.getClass().getSimpleName() + " - " + e.getMessage());
                System.err.println("[Crypto] Data length: " + (data != null ? data.length() : 0));
                System.err.println("[Crypto] Data preview: " + (data != null && data.length() > 50 ? data.substring(0, 50) + "..." : data));
            }
            if (retry < 3) {
                return decodeAES(secretKey, data, retry + 1);
            }
            return null;
        }
    }
    
    public static String getSignKey(String type, Map<String, String> params) {
        java.util.List<String> keys = new java.util.ArrayList<>(params.keySet());
        java.util.Collections.sort(keys);
        
        StringBuilder sb = new StringBuilder("zsecure" + type);
        for (String key : keys) {
            sb.append(params.get(key));
        }
        
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }
}

