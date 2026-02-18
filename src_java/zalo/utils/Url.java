/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Url {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    public static String makeURL(Context ctx, String baseURL, Map<String, String> params) {
        return makeURL(ctx, baseURL, params, true);
    }
    
    public static String makeURL(Context ctx, String baseURL, Map<String, String> params, boolean apiVersion) {
        StringBuilder url = new StringBuilder(baseURL);
        
        if (params == null || params.isEmpty()) {
            if (apiVersion) {
                url.append(url.toString().contains("?") ? "&" : "?");
                url.append("zpw_ver=").append(ctx.getAPI_VERSION());
                url.append("&zpw_type=").append(ctx.getAPI_TYPE());
            }
            return url.toString();
        }
        
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.append(first ? (baseURL.contains("?") ? "&" : "?") : "&");
            try {
                url.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                   .append("=")
                   .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                url.append(entry.getKey()).append("=").append(entry.getValue());
            }
            first = false;
        }
        
        if (apiVersion) {
            if (!params.containsKey("zpw_ver")) {
                url.append("&zpw_ver=").append(ctx.getAPI_VERSION());
            }
            if (!params.containsKey("zpw_type")) {
                url.append("&zpw_type=").append(ctx.getAPI_TYPE());
            }
        }
        
        return url.toString();
    }
}

