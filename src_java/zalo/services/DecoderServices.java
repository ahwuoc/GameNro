/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.services;

import zalo.utils.ZaloApiError;
import zalo.utils.Json;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class DecoderServices {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    public static Map<String, Object> decodeEventData(Map<String, Object> parsed, String cipherKey) throws Exception {
        Object dataObj = parsed.get("data");
        Object encryptObj = parsed.get("encrypt");
        
        if (!(dataObj instanceof String)) {
            throw new ZaloApiError("Invalid data, expected string but got " + 
                (dataObj != null ? dataObj.getClass().getSimpleName() : "null"));
        }
        
        if (!(encryptObj instanceof Number)) {
            throw new ZaloApiError("Invalid encrypt type, expected number but got " + 
                (encryptObj != null ? encryptObj.getClass().getSimpleName() : "null"));
        }
        
        int encryptType = ((Number) encryptObj).intValue();
        if (encryptType < 0 || encryptType > 3) {
            throw new ZaloApiError("Invalid encrypt type, expected 0-3 but got " + encryptType);
        }
        
        String rawData = (String) dataObj;
        
        // If not encrypted, parse directly
        if (encryptType == 0) {
            Object parsedData = Json.parse(rawData);
            if (parsedData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) parsedData;
                return result;
            }
            return new java.util.HashMap<>();
        }
        
        byte[] decodedBuffer;
        try {
            String dataToDecode;
            if (encryptType == 1) {
                dataToDecode = rawData;
            } else {
                dataToDecode = rawData;
                try {
                    dataToDecode = rawData.replace("+", "%2B");
                    dataToDecode = java.net.URLDecoder.decode(dataToDecode, "UTF-8");
                    if (dataToDecode.contains("%")) {
                        dataToDecode = dataToDecode.replace("+", "%2B");
                        dataToDecode = java.net.URLDecoder.decode(dataToDecode, "UTF-8");
                    }
                } catch (Exception e) {
                    try {
                        dataToDecode = java.net.URLDecoder.decode(rawData, "UTF-8");
                        if (dataToDecode.contains("%")) {
                            dataToDecode = java.net.URLDecoder.decode(dataToDecode, "UTF-8");
                        }
                    } catch (Exception e2) {
                        dataToDecode = rawData;
                    }
                }
            }
            dataToDecode = dataToDecode.replaceAll("\\s+", "");
            
            try {
                decodedBuffer = Base64.getDecoder().decode(dataToDecode);
            } catch (IllegalArgumentException e) {
               
                String withPadding = dataToDecode;
                int remainder = withPadding.length() % 4;
                if (remainder > 0) {
                    withPadding += "=".repeat(4 - remainder);
                }
                try {
                    decodedBuffer = Base64.getDecoder().decode(withPadding);
                } catch (IllegalArgumentException e2) {
                    
                    try {
                        decodedBuffer = Base64.getUrlDecoder().decode(dataToDecode);
                    } catch (IllegalArgumentException e3) {
                      
                        try {
                            withPadding = dataToDecode;
                            remainder = withPadding.length() % 4;
                            if (remainder > 0) {
                                withPadding += "=".repeat(4 - remainder);
                            }
                            decodedBuffer = Base64.getUrlDecoder().decode(withPadding);
                        } catch (IllegalArgumentException e4) {
                            throw new ZaloApiError("Base64 decode failed: " + e4.getMessage() + 
                                " (original length: " + rawData.length() + 
                                ", after URL decode: " + dataToDecode.length() + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ZaloApiError("Base64 decode failed: " + e.getMessage());
        }
        
        byte[] decryptedBuffer = decodedBuffer;
        
        if (encryptType != 1) {
            if (cipherKey == null || cipherKey.isEmpty()) {
                throw new ZaloApiError("Missing cipher key for decryption (encryptType=" + encryptType + ")");
            }
            if (decodedBuffer.length < 48) {
                throw new ZaloApiError("Invalid data length: " + decodedBuffer.length);
            }
            
            byte[] iv = new byte[16];
            byte[] additionalData = new byte[16];
            byte[] dataSource = new byte[decodedBuffer.length - 32];
            System.arraycopy(decodedBuffer, 0, iv, 0, 16);
            System.arraycopy(decodedBuffer, 16, additionalData, 0, 16);
            System.arraycopy(decodedBuffer, 32, dataSource, 0, dataSource.length);
            
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(cipherKey);
            } catch (IllegalArgumentException e) {
                try {
                    keyBytes = Base64.getUrlDecoder().decode(cipherKey);
                } catch (IllegalArgumentException e2) {
                    throw new ZaloApiError("Failed to decode cipher key: " + e2.getMessage());
                }
            }
            
            try {
                decryptedBuffer = decryptAESGCM(keyBytes, iv, additionalData, dataSource);
            } catch (javax.crypto.AEADBadTagException e) {
              
            
                throw new ZaloApiError("Decryption failed: Tag mismatch");
            } catch (Exception e) {
                System.out.println("[DECRYPT] Failed: " + e.getMessage());
                throw new ZaloApiError("Decryption failed: " + e.getMessage());
            }
        }
        
        byte[] decompressedBuffer;
        if (encryptType == 3) {
            decompressedBuffer = decryptedBuffer;
        } else {
            try {
                decompressedBuffer = decompress(decryptedBuffer);
               } catch (Exception e) {
               
                if (decryptedBuffer.length > 0) {
                    StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < Math.min(20, decryptedBuffer.length); i++) {
                        hex.append(String.format("%02x ", decryptedBuffer[i]));
                    }
                    String testStr = new String(decryptedBuffer, 0, Math.min(100, decryptedBuffer.length), StandardCharsets.UTF_8);
                  }
                  decompressedBuffer = decryptedBuffer;
            }
        }
        
        String decodedData = new String(decompressedBuffer, StandardCharsets.UTF_8);
        if (decodedData == null || decodedData.isEmpty()) {
            return null;
        }
        
        try {
            Object parsedData = Json.parse(decodedData);
            if (parsedData instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) parsedData;
                return result;
            }
            return new java.util.HashMap<>();
        } catch (Exception e) {
            throw new ZaloApiError("Invalid JSON: " + e.getMessage());
        }
    }
    
    private static byte[] decryptAESGCM(byte[] key, byte[] iv, byte[] additionalData, byte[] encryptedData) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        javax.crypto.spec.GCMParameterSpec spec = new javax.crypto.spec.GCMParameterSpec(128, iv);
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(key, "AES");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, spec);
        cipher.updateAAD(additionalData);
        return cipher.doFinal(encryptedData);
    }
    
    private static byte[] decompress(byte[] data) throws Exception {
        if (data.length >= 2 && (data[0] & 0xFF) == 0x1f && (data[1] & 0xFF) == 0x8b) {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
            java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = gzip.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            gzip.close();
            return outputStream.toByteArray();
        } else {
            java.util.zip.Inflater inflater = new java.util.zip.Inflater(true);
            inflater.setInput(data);
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            
            try {
                while (!inflater.finished()) {
                    int count = inflater.inflate(buffer);
                    if (count > 0) {
                        outputStream.write(buffer, 0, count);
                    } else if (inflater.needsInput()) {
                        break;
                    } else if (inflater.needsDictionary()) {
                        throw new ZaloApiError("Inflater needs dictionary");
                    }
                }
            } catch (java.util.zip.DataFormatException e) {
                throw new ZaloApiError("Decompression failed: " + e.getMessage());
            } finally {
                inflater.end();
            }
            
            return outputStream.toByteArray();
        }
    }
    
    public static int[] getHeader(byte[] headerBytes) {
        if (headerBytes.length < 4) {
            return new int[]{0, 0, 0};
        }
        
        int version = headerBytes[0] & 0xFF;
        int cmd = ((headerBytes[2] & 0xFF) << 8) | (headerBytes[1] & 0xFF);
        int subCmd = headerBytes[3] & 0xFF;
        
        return new int[]{version, cmd, subCmd};
    }
}

