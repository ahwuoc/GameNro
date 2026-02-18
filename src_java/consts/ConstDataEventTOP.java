package consts;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;

public class ConstDataEventTOP {

    public static short MONTH_OPEN = 9;
    public static short DATE_OPEN = 21;
    public static short HOUR_OPEN = 10;
    public static short MIN_OPEN = 0;

    public static short MONTH_END = 9;
    public static short DATE_END = 25;
    public static short HOUR_END = 23;
    public static short MIN_END = 59;

    public static short MONTH_REWARD = 11;
    public static short DATE_REWARD = 24;
    public static short HOUR_REWARD = 20;
    public static short MIN_REWARD = 20;

    public static Timestamp timeEndEvent() {
        return Timestamp.valueOf(
            String.format("%04d-%02d-%02d %02d:%02d:00",
                    ConstDataEventSM.YEAR_EVENT, MONTH_END, DATE_END, HOUR_END, MIN_END)
        );
    }

    public static Timestamp timeReward() {
        return Timestamp.valueOf(
            String.format("%04d-%02d-%02d %02d:%02d:00",
                    ConstDataEventSM.YEAR_EVENT, MONTH_REWARD, DATE_REWARD, HOUR_REWARD, MIN_REWARD)
        );
    }

    public static String demTimeSuKien() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = timeEndEvent().toLocalDateTime();
        long daysRemaining = ChronoUnit.DAYS.between(now, end);
        if (daysRemaining > 0) {
            return "(" + daysRemaining + " ngày nữa)";
        } else {
            return "(Đã kết thúc)";
        }
    }

    public static String getTimeInfo() {
        return String.format("Bắt đầu: %02dh ngày %02d/%02d/%d\nKết thúc: %02dh%02d ngày %02d/%02d/%d\nTrao giải: %02dh%02d ngày %02d/%02d/%d",
                HOUR_OPEN, DATE_OPEN, MONTH_OPEN, ConstDataEventSM.YEAR_EVENT,
                HOUR_END, MIN_END, DATE_END, MONTH_END, ConstDataEventSM.YEAR_EVENT,
                HOUR_REWARD, MIN_REWARD, DATE_REWARD, MONTH_REWARD, ConstDataEventSM.YEAR_EVENT);
    }
}
