package utils;

import java.util.Random;
import java.util.regex.Pattern;
import nro.player.Player;

public class Functions {

    // Regex kiểm tra từ nhạy cảm / spam
    private static final String REGEX = "\\b(dkm|đkm|đbrr|địt|đĩ|đỹ|cm|cmm|lồn|buồi|cc|ôm cl|mẹ mày|cặc|đụ|fuck|damn|clmm|dcmm|cl|tml|đ\\*t|c\\*c|dit|d\\*t|c\\.a\\.c|l\\.o\\.n|c\\.ặ\\.c|l\\.ồ\\.n|b\\.u\\.ồ\\.i|bu\\*i|đặc cầu|đồn lầu|bú cu|buscu|đm|cc|đb|db|lol|nhu lon|nhu cac|vc|vl|vãi|đéo|đờ mờ|đờ cờ mờ|clgt|dell|mẹ|cứt|shit|idiot|khốn|xiên chết|cụ|giao phối|thiểu năng|ngáo|chó|dog|đcmm|vcl|vkl|đ!t|d!t|đỵt|dyt|ngu|óc|\\.com|\\.net|\\.online|\\.vn|\\.pw|\\.pro|\\.org|\\.info|\\.ml|\\.ga|\\.gq|\\.cf|\\.fun|\\.xyz|\\.io|\\.club)\\b";
    private static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);

    // Kiểm tra spam
    public static boolean isSpam(Player player, String text) {
        return PATTERN.matcher(text.toLowerCase()).find() && !player.name.equals("BARCOLL");
    }

    // Chuyển long -> int an toàn
    public static int maxInt(long n) {
        return (int) Math.min(n, Integer.MAX_VALUE);
    }

    // Tạo chuỗi ký tự ngẫu nhiên (số + chữ in hoa)
    public static String generateRandomCharacters(int quantity) {
        StringBuilder sb = new StringBuilder(quantity);
        Random random = new Random();

        for (int i = 0; i < quantity; i++) {
            char c = random.nextBoolean() 
                    ? (char) ('0' + random.nextInt(10))  // số
                    : (char) ('A' + random.nextInt(26)); // chữ in hoa
            sb.append(c);
        }

        return sb.toString();
    }

    // Sleep an toàn
    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }
}
