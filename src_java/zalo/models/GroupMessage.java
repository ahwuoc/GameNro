/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.models;

import java.util.Map;

public class GroupMessage extends Message {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    public GroupMessage(String uid, Map<String, Object> data) {
        this.type = ThreadType.GROUP;
        this.data = data;
        this.threadId = (String) data.get("idTo");
        this.isSelf = "0".equals(data.get("uidFrom"));
        
        String uidFrom = (String) data.get("uidFrom");
        if ("0".equals(uidFrom)) {
            data.put("uidFrom", uid);
        }
        
        Object quoteObj = data.get("quote");
        if (quoteObj instanceof Map) {
            Map<String, Object> quote = (Map<String, Object>) quoteObj;
            Object ownerId = quote.get("ownerId");
            if (ownerId != null) {
                quote.put("ownerId", String.valueOf(ownerId));
            }
        }
    }
}

