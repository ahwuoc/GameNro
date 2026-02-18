package zalo.login;

/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
public class ZaloOptions {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private Boolean selfListen;
    private Boolean checkUpdate;
    private Boolean logging;
    private Integer apiType;
    private Integer apiVersion;
    
    public boolean isSelfListen() {
        return selfListen != null ? selfListen : true;
    }
    
    public void setSelfListen(Boolean selfListen) {
        this.selfListen = selfListen;
    }
    
    public boolean isCheckUpdate() {
        return checkUpdate != null ? checkUpdate : false;
    }
    
    public void setCheckUpdate(Boolean checkUpdate) {
        this.checkUpdate = checkUpdate;
    }
    
    public boolean isLogging() {
        return logging != null ? logging : false;
    }
    
    public void setLogging(Boolean logging) {
        this.logging = logging;
    }
    
    public Integer getApiType() {
        return apiType;
    }
    
    public void setApiType(Integer apiType) {
        this.apiType = apiType;
    }
    
    public Integer getApiVersion() {
        return apiVersion;
    }
    
    public void setApiVersion(Integer apiVersion) {
        this.apiVersion = apiVersion;
    }
}

