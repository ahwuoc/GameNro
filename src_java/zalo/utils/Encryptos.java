/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Encryptos {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private String zcid;
    private String encVer = "v2";
    private String zcidExt;
    private String encryptKey;
    
    public Encryptos(int type, String imei, long firstLaunchTime) {
        createZcid(type, imei, firstLaunchTime);
        this.zcidExt = randomString();
        createEncryptKey();
    }
    
    public String getEncryptKey() {
        if (encryptKey == null) {
            throw new ZaloApiError("getEncryptKey: didn't create encryptKey yet");
        }
        return encryptKey;
    }
    
    private void createZcid(int type, String imei, long firstLaunchTime) {
        if (type == 0 || imei == null || firstLaunchTime == 0) {
            throw new ZaloApiError("createZcid: missing params");
        }
        String msg = type + "," + imei + "," + firstLaunchTime;
        this.zcid = encodeAES("3FC4F0D2AB50057BCE0D90D9187A22B1", msg, "hex", true);
    }
    
    private void createEncryptKey() {
        createEncryptKey(0);
    }
    
    private boolean createEncryptKey(int retry) {
        if (zcid == null || zcidExt == null) {
            throw new ZaloApiError("createEncryptKey: zcid or zcid_ext is null");
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(zcidExt.getBytes(StandardCharsets.UTF_8));
            String hashUpper = bytesToHex(hash).toUpperCase();
            
            ProcessStrResult even = processStr(hashUpper);
            ProcessStrResult zcidResult = processStr(zcid);
            
            if (even.even == null || zcidResult.even == null || zcidResult.odd == null) {
                if (retry < 3) {
                    return createEncryptKey(retry + 1);
                }
                return false;
            }
            
            List<Character> evenList = even.even;
            List<Character> oddList = zcidResult.odd;
            List<Character> oddReversed = new ArrayList<>(oddList);
            java.util.Collections.reverse(oddReversed);
            
            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < Math.min(8, evenList.size()); i++) {
                keyBuilder.append(evenList.get(i));
            }
            for (int i = 0; i < Math.min(12, zcidResult.even.size()); i++) {
                keyBuilder.append(zcidResult.even.get(i));
            }
            for (int i = 0; i < Math.min(12, oddReversed.size()); i++) {
                keyBuilder.append(oddReversed.get(i));
            }
            
            this.encryptKey = keyBuilder.toString();
            return true;
        } catch (Exception e) {
            if (retry < 3) {
                return createEncryptKey(retry + 1);
            }
            return false;
        }
    }
    
    public EncryptParams getParams() {
        if (zcid == null) {
            return null;
        }
        EncryptParams params = new EncryptParams();
        params.setZcid(zcid);
        params.setZcidExt(zcidExt);
        params.setEncVer(encVer);
        return params;
    }
    
    private ProcessStrResult processStr(String str) {
        if (str == null) {
            return new ProcessStrResult(null, null);
        }
        List<Character> even = new ArrayList<>();
        List<Character> odd = new ArrayList<>();
        char[] chars = str.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (i % 2 == 0) {
                even.add(chars[i]);
            } else {
                odd.add(chars[i]);
            }
        }
        return new ProcessStrResult(even, odd);
    }
    
    private String randomString() {
        return randomString(6, 12);
    }
    
    private String randomString(int min, int max) {
        Random random = new Random();
        int length = random.nextInt(max - min + 1) + min;
        if (length > 12) {
            StringBuilder sb = new StringBuilder();
            while (length > 0) {
                int chunk = Math.min(12, length);
                String hex = Long.toHexString(random.nextLong());
                sb.append(hex.substring(0, Math.min(hex.length(), chunk)));
                length -= chunk;
            }
            return sb.toString();
        }
        return Long.toHexString(random.nextLong()).substring(0, length);
    }
    
    public static String encodeAES(String key, String message, String type, boolean uppercase) {
        return encodeAES(key, message, type, uppercase, 0);
    }
    
    private static String encodeAES(String key, String message, String type, boolean uppercase, int retry) {
        if (message == null) {
            return null;
        }
        
        try {
            byte[] keyBytes = hexToBytes(key);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(new byte[16]); // Zero IV
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));
            
            String result;
            if ("hex".equals(type)) {
                result = bytesToHex(encrypted);
            } else {
                result = java.util.Base64.getEncoder().encodeToString(encrypted);
            }
            
            return uppercase ? result.toUpperCase() : result;
        } catch (Exception e) {
            if (retry < 3) {
                return encodeAES(key, message, type, uppercase, retry + 1);
            }
            return null;
        }
    }
    
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private static class ProcessStrResult {
        List<Character> even;
        List<Character> odd;
        
        ProcessStrResult(List<Character> even, List<Character> odd) {
            this.even = even;
            this.odd = odd;
        }
    }
    
    public static class EncryptParams {
        private String zcid;
        private String zcidExt;
        private String encVer;
        
        public String getZcid() {
            return zcid;
        }
        
        public void setZcid(String zcid) {
            this.zcid = zcid;
        }
        
        public String getZcidExt() {
            return zcidExt;
        }
        
        public void setZcidExt(String zcidExt) {
            this.zcidExt = zcidExt;
        }
        
        public String getEncVer() {
            return encVer;
        }
        
        public void setEncVer(String encVer) {
            this.encVer = encVer;
        }
    }
}

