package consts;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class ConstTranhNgocNamek {

    // ===== Thời gian, tham số chung =====
    public static final int TIME_SECOND = 600;           // Thời gian trận tính bằng giây
    public static final long TIME = 600000;              // Thời gian trận (ms) = 10 phút
    public static final long LAST_TIME_DROP_BALL = 60000;// Thời gian rơi ngọc (ms) = 1 phút
    public static final int MAP_ID = 164;                // ID bản đồ Tranh Ngọc
    public static final String RED = "Đỏ";               // Đội đỏ
    public static final String BLUE = "Xanh";            // Đội xanh
    public static final byte MAX_POINT = 7;              // Điểm tối đa để thắng
    public static final int MAX_LIFE = 10;               // Mạng tối đa
    public static final byte LOSE = 0;                   // Thua
    public static final byte WIN = 1;                    // Thắng
    public static final byte DRAW = 2;                   // Hòa
    public static final short ITEM_TRANH_NGOC = 1705;    // ID item Tranh Ngọc

    // ===== Giờ phút quy định =====
    public static final byte HOUR_REGISTER = 12;         // Giờ bắt đầu đăng ký
    public static final byte MIN_REGISTER = 40;          // Phút bắt đầu đăng ký
    public static final byte HOUR_OPEN = 12;             // Giờ mở sự kiện
    public static final byte MIN_OPEN = 50;               // Phút mở sự kiện
    public static final byte HOUR_CLOSE = 15;            // Giờ kết thúc sự kiện
    public static final byte MIN_CLOSE = 20;             // Phút kết thúc sự kiện
    
    public static final long TIME_REGISTER = toMillis(HOUR_REGISTER, MIN_REGISTER); 
    public static final long TIME_OPEN = toMillis(HOUR_OPEN, MIN_OPEN);             
    public static final long TIME_CLOSE = toMillis(HOUR_CLOSE, MIN_CLOSE);          

    // ===== Hàm tiện ích chuyển giờ:phút -> millis hôm nay =====
    private static long toMillis(int hour, int min) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime time = now.withHour(hour).withMinute(min).withSecond(0).withNano(0);
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
