package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import models.BlackBallWar.BlackBallWar;
import models.MajinBuu.MajinBuuService;

public class TimeUtil {

    public static final byte SECOND = 1;
    public static final byte MINUTE = 2;
    public static final byte HOUR = 3;
    public static final byte DAY = 4;
    public static final byte WEEK = 5;
    public static final byte MONTH = 6;
    public static final byte YEAR = 7;

    public static long diffDate(Date d1, Date d2, byte type) {
        long timeDiff = Math.abs(d1.getTime() - d2.getTime());
        switch (type) {
            case SECOND:
                return (timeDiff / 1000);
            case MINUTE:
                return (timeDiff / (60 * 1000) % 60);
            case HOUR:
                return (timeDiff / (60 * 60 * 1000) % 24);
            case DAY:
                return (timeDiff / (24 * 60 * 60 * 1000));
            case WEEK:
                return (timeDiff / (7 * 24 * 60 * 60 * 1000));
            case MONTH:
                return (timeDiff / (30 * 24 * 60 * 60 * 1000));
            case YEAR:
                return (timeDiff / (365 * 24 * 60 * 60 * 1000));
            default:
                return 0;
        }
    }

    public static boolean isTimeNowInRangex(String d1, String d2, String format) throws Exception {
        SimpleDateFormat fm = new SimpleDateFormat(format);
        try {
            long time1 = fm.parse(d1).getTime();
            long time2 = fm.parse(d2).getTime();
            long now = fm.parse(fm.format(new Date())).getTime();
            return now > time1 && now < time2;
        } catch (Exception e) {
            throw new Exception("Thời gian không hợp lệ");
        }
    }

    public static int getCurrDay() {
        LocalDateTime now = LocalDateTime.now();
        return now.getDayOfWeek().getValue();
    }

    public static int getCurrHour() {
        LocalDateTime now = LocalDateTime.now();
        return now.getHour();
    }

    public static int getCurrMin() {
        LocalDateTime now = LocalDateTime.now();
        return now.getMinute();
    }

    public static String convertTime(int totalSeconds) {
        long days = TimeUnit.SECONDS.toDays(totalSeconds);
        long hours = TimeUnit.SECONDS.toHours(totalSeconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % 60;
        long seconds = totalSeconds % 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append(" ngày ");
        }
        if (hours > 0) {
            result.append(hours).append(" giờ ");
        }
        if (minutes > 0) {
            result.append(minutes).append(" phút ");
        }
        if (seconds > 0) {
            result.append(seconds).append(" giây");
        }
        return result.toString().trim();
    }

    public static String getTimeLeft(long lastTime, int secondTarget) {
        int secondPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);
        int secondsLeft = secondTarget - secondPassed;
        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        return secondsLeft > 60 ? (secondsLeft / 60) + " phút" : secondsLeft + " giây";
    }

    public static String getTimeLeft(long lastTime) {
        int secondPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);
        return secondPassed > 86400 ? (secondPassed / 86400) + "n trước"
                : secondPassed > 3600 ? (secondPassed / 3600) + "g trước"
                        : secondPassed > 60 ? (secondPassed / 60) + "p trước" : secondPassed + "gi trước";
    }

    public static int getMinLeft(long lastTime, int secondTarget) {
        int secondPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);
        int secondsLeft = secondTarget - secondPassed;
        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        int minLeft = 0;
        if (secondsLeft > 0 && secondsLeft <= 60) {
            minLeft = 1;
        } else if (secondsLeft > 60) {
            minLeft = secondsLeft / 60;
        }
        return minLeft;
    }

    public static int getSecondLeft(long lastTime, int secondTarget) {
        int secondPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);
        int secondsLeft = secondTarget - secondPassed;
        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        return secondsLeft;
    }

    public static String getDateLeft(long lastTime, int secondTarget) {
        int secondPassed = (int) ((System.currentTimeMillis() - lastTime) / 1000);
        int secondsLeft = secondTarget - secondPassed;
        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        return convertTime(secondsLeft);
    }

    public static String convertTimeNow(long lastTime) {
        int secondsLeft = (int) ((System.currentTimeMillis() - lastTime) / 1000);
        if (secondsLeft < 0) {
            secondsLeft = 0;
        }
        return convertTime(secondsLeft);
    }

    public static long getTime(String time, String format) throws Exception {
        SimpleDateFormat fm = new SimpleDateFormat(format);
        try {
            return fm.parse(time).getTime();
        } catch (ParseException ex) {
            throw new Exception("Thời gian không hợp lệ");
        }
    }

    public static String getTimeNow(String format) {
        SimpleDateFormat fm = new SimpleDateFormat(format);
        return fm.format(new Date());
    }

    public static String getTimeBeforeCurrent(int subTime, String format) {
        SimpleDateFormat fm = new SimpleDateFormat(format);
        Date date = new Date(System.currentTimeMillis() - subTime);
        return fm.format(date);
    }

    public static String formatTime(Date time, String format) {
        SimpleDateFormat fm = new SimpleDateFormat(format);
        return fm.format(time);
    }

    public static String formatTime(long time, String format) {
        SimpleDateFormat fm = new SimpleDateFormat(format);
        return fm.format(new Date(time));
    }

    public static boolean isMabuOpen() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.SATURDAY, Calendar.SUNDAY -> {
                return true;
            }
            case Calendar.FRIDAY -> {
                MajinBuuService.HOUR_OPEN_MAP_MABU = 18;
                return (hour >= 18 && hour < 19);
            }
            default -> {
                MajinBuuService.HOUR_OPEN_MAP_MABU = 12;
                return (hour >= 12 && hour < 13);
            }
        }
    }

    public static boolean isMabu14HOpen() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return (hour >= 14 && hour < 15);
    }

    public static boolean is21H() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return (hour >= 21 && hour < 22);
    }

    public static long getStartTimeBlackBallWar() {
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (BlackBallWar.TimeFrame frame : BlackBallWar.TIME_CONFIGS) {
            LocalTime startTime = LocalTime.of(frame.hourOpen, frame.minOpen, frame.secondOpen);
            if (currentTime.isBefore(startTime)) {
                LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")), startTime);
                return startDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();
            }
        }
        if (!BlackBallWar.TIME_CONFIGS.isEmpty()) {
            BlackBallWar.TimeFrame firstFrame = BlackBallWar.TIME_CONFIGS.get(0);
            LocalTime startTime = LocalTime.of(firstFrame.hourOpen, firstFrame.minOpen, firstFrame.secondOpen);
            LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1),
                    startTime);
            return startDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant().toEpochMilli();
        }
        return 0;
    }

    public static boolean isBlackBallWarOpen() {
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (BlackBallWar.TimeFrame frame : BlackBallWar.TIME_CONFIGS) {
            LocalTime startTime = LocalTime.of(frame.hourOpen, frame.minOpen, frame.secondOpen);
            LocalTime endTime = LocalTime.of(frame.hourClose, frame.minClose, frame.secondClose);
            if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlackBallWarCanPick() {
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (BlackBallWar.TimeFrame frame : BlackBallWar.TIME_CONFIGS) {
            LocalTime pickTime = LocalTime.of(frame.hourPick, frame.minPick, frame.secondPick);
            LocalTime endTime = LocalTime.of(frame.hourClose, frame.minClose, frame.secondClose);
            if (currentTime.isAfter(pickTime) && currentTime.isBefore(endTime)) {
                return true;
            }
        }
        return false;
    }

    public static long getSecondsUntilCanPick() {
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        for (BlackBallWar.TimeFrame frame : BlackBallWar.TIME_CONFIGS) {
            LocalTime startTime = LocalTime.of(frame.hourOpen, frame.minOpen, frame.secondOpen);
            LocalTime endTime = LocalTime.of(frame.hourClose, frame.minClose, frame.secondClose);

            // If current time is within an open frame
            if (currentTime.isAfter(startTime) && currentTime.isBefore(endTime)) {
                LocalTime pickTime = LocalTime.of(frame.hourPick, frame.minPick, frame.secondPick);
                if (currentTime.isBefore(pickTime)) {
                    return Duration.between(currentTime, pickTime).getSeconds();
                }
                return 0; // Already can pick
            }
        }
        return 0;
    }

    public static long currentTimeMillisPlus11() {
        ZoneId zoneIdPlus3 = ZoneId.of("UTC+11");
        ZonedDateTime currentTimeInPlus3Zone = ZonedDateTime.now(zoneIdPlus3);
        long currentTimeMillisPlus3 = currentTimeInPlus3Zone.toInstant().toEpochMilli();
        return currentTimeMillisPlus3;
    }

    public static boolean checkTime(long time) {
        return (time - System.currentTimeMillis()) / 1000 > 0;
    }

    public static String convertTimeMS(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + " m " + seconds + " s";
    }

}
