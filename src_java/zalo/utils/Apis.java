/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

import zalo.services.ListenServices;
import zalo.apis.SendMessageApi;
import zalo.apis.GetUserInfoApi;
import zalo.apis.AddReactionApi;
import zalo.apis.DeleteMessageApi;
import zalo.apis.GetGroupInfoApi;
import zalo.apis.UpdateGroupSettingsApi;
import java.util.Map;

public class Apis {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private Context ctx;
    private Map<String, Object> zpwServiceMap;
    private Object wsUrls;
    private ListenServices listener;
    
    public final SendMessageApi sendMessage;
    public final GetUserInfoApi getUserInfo;
    public final AddReactionApi addReaction;
    public final DeleteMessageApi deleteMessage;
    public final GetGroupInfoApi getGroupInfo;
    public final UpdateGroupSettingsApi updateGroupSettings;
  
    public Apis(Context ctx, Map<String, Object> zpwServiceMap, Object wsUrls) {
        this.ctx = ctx;
        this.zpwServiceMap = zpwServiceMap;
        this.wsUrls = wsUrls;
        this.listener = null; 
        this.sendMessage = new SendMessageApi(ctx, this);
        this.getUserInfo = new GetUserInfoApi(ctx, this);
        this.addReaction = new AddReactionApi(ctx, this);
        this.deleteMessage = new DeleteMessageApi(ctx, this);
        this.getGroupInfo = new GetGroupInfoApi(ctx, this);
        this.updateGroupSettings = new UpdateGroupSettingsApi(ctx, this);
        this.listener = new ListenServices(ctx, this);
    }
    
    public Context getContext() {
        return ctx;
    }
    
    public Map<String, Object> getZpwServiceMap() {
        return zpwServiceMap;
    }
    
    public Object getWsUrls() {
        return wsUrls;
    }
    
    public ListenServices getListener() {
        return listener;
    }
}

