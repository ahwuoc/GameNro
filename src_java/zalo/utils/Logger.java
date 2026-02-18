/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.utils;

public class Logger {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    private Context ctx;
    
    public Logger(Context ctx) {
        this.ctx = ctx;
    }
    
    public void verbose(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[35m VERBOSE\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }
    
    public void info(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[34mINFO\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }
    
    public void warn(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[33mWARN\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }
    
    public void error(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[31mERROR\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }
    
    public void error(String message, Throwable error) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[31mERROR\u001B[0m " + message + ": ");
            error.printStackTrace();
        }
    }
    
    public void success(String... args) {
        if (ctx.getOptions() != null && ctx.getOptions().isLogging()) {
            System.out.print("\u001B[32mSUCCESS\u001B[0m ");
            for (String arg : args) {
                System.out.print(arg + " ");
            }
            System.out.println();
        }
    }
}

