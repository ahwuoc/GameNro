/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.interfaces;

import zalo.message.MessageContext;
import zalo.utils.Apis;
import java.util.List;
import java.util.Map;

public interface Command {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

   
    String getName();
    String getDescription();
    String getTag();
    int getCooldown();
    int getRole();
    default boolean isHidden() {
        return false;
    }
    
    default String getUsage() {
        return null;
    }
    
    void run(CommandContext context);
   
    class CommandContext {
        private MessageContext message;
        private Apis api;
        private List<String> args;
        private Map<String, Command> commands;
        
        public MessageContext getMessage() {
            return message;
        }
        
        public void setMessage(MessageContext message) {
            this.message = message;
        }
        
        public Apis getApi() {
            return api;
        }
        
        public void setApi(Apis api) {
            this.api = api;
        }
        
        public List<String> getArgs() {
            return args;
        }
        
        public void setArgs(List<String> args) {
            this.args = args;
        }
        
        public Map<String, Command> getCommands() {
            return commands;
        }
        
        public void setCommands(Map<String, Command> commands) {
            this.commands = commands;
        }
    }
}

