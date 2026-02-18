package Bot;

import java.util.Random;

/**
 * Bộ não Bot Offline (Chạy bằng cơm)
 * Tự động bắt từ khóa và trả lời, không cần API, không cần mạng.
 */
public class BotAIGenerator {

    private static final Random rand = new Random();

    // 1. DATA: Câu chat ngẫu nhiên (Dùng khi bot tự nói hoặc không hiểu người chơi nói gì)
    private static final String[] RANDOM_CHATS = {
        "Anh em đi Doanh Trại không?",
        "Server hôm nay đông vui thế nhỉ",
        "Ai có đậu thần cho xin ít với",
        "Up đệ tử chua quá ae ơi",
        "Mua vàng số lượng lớn đây, ai bán pm",
        "Boss Xên Bọ Hung ra chưa nhỉ?",
        "Có ai solo kèo nhẹ nhàng không?",
        "Chán quá, kiếm gì làm đi ae",
        "Admin đẹp trai fix lỗi dùm cái coi",
        "Mọi người ơi, cho hỏi xíu...",
        "Kênh thế giới hôm nay xôm tụ thế",
        "Farm nãy giờ không rớt đồ sao, đen vãi"
    };

    /**
     * Hàm chính: Sinh câu trả lời dựa trên từ khóa
     */
    public static String generate(String playerMessage) {
        // Case 1: Nếu người chơi không nói gì (Bot tự chat bâng quơ)
        if (playerMessage == null || playerMessage.trim().isEmpty()) {
            return getRandom(RANDOM_CHATS);
        }

        // Chuyển về chữ thường để so sánh cho dễ
        String msg = playerMessage.toLowerCase();

        // Case 2: Bắt từ khóa để trả lời (Logic Offline)
        
        // --- CHÀO HỎI ---
        if (msg.contains("chào") || msg.contains("hello") || msg.contains("hi ") || msg.equals("hi") || msg.contains("lô")) {
            return getRandom(new String[]{
                "Lô người anh em", 
                "Chào cậu nhé", 
                "Hi, đi farm không?", 
                "Chào ae, chúc ngày mới vui vẻ"
            });
        }

        // --- KÊU LAG ---
        if (msg.contains("lag") || msg.contains("lác") || msg.contains("delay") || msg.contains("giật")) {
            return getRandom(new String[]{
                "Do mạng ông cùi ấy chứ tui mượt mà", 
                "Tui chơi bình thường mà ta?", 
                "Kêu admin fix đi, than đây làm gì", 
                "Đổi mạng 4G đi bạn ơi",
                "Sv đông nên lag tí thôi"
            });
        }

        // --- GỌI ADMIN ---
        if (msg.contains("admin") || msg.contains("ad ")) {
            return getRandom(new String[]{
                "Admin đang ngủ rồi đừng gọi", 
                "Gọi admin làm gì thế?", 
                "Admin đẹp trai đang bận fix bug rồi",
                "Admin đi vắng rồi, có gì nhắn tui chuyển lời cho :v"
            });
        }

        // --- SĂN BOSS ---
        if (msg.contains("boss") || msg.contains("săn") || msg.contains("super")) {
            return getRandom(new String[]{
                "Boss chết hết rồi, ra trễ thế", 
                "Đi săn boss không? Xin slot đấm ké", 
                "Boss khu mấy thế ae?", 
                "Yếu đừng ra gió bạn ơi, boss đấm phát chết đấy"
            });
        }

        // --- MUA BÁN ---
        if (msg.contains("mua") || msg.contains("bán") || msg.contains("trade") || msg.contains("gd")) {
            return getRandom(new String[]{
                "Ra khu mua bán mà rao, đây kênh thế giới cha", 
                "Giá sao inbox vùng kín nha", 
                "Ở đây không bán chịu", 
                "Cẩn thận scam nha, giao dịch trung gian cho chắc"
            });
        }

        // --- XIN XỎ ---
        if (msg.contains("xin") || msg.contains("cho") || msg.contains("ít")) {
            return getRandom(new String[]{
                "Làm thì mới có ăn nha bạn", 
                "Về xin mẹ ấy", 
                "Tui nghèo rớt mồng tơi đây này", 
                "Đang farm, không có đậu đâu mà xin"
            });
        }

        // --- CHỬI BỚI (TOXIC) ---
        if (msg.contains("ngu") || msg.contains("cút") || msg.contains("chó") || msg.contains("óc") || msg.contains("đm")) {
            return getRandom(new String[]{
                "Ăn nói cẩn thận nha bạn êi", 
                "Toxic là ra đảo chơi với khỉ đấy", 
                "Sợ quá sợ quá, anh hùng bàn phím", 
                "Bớt nóng đi người anh em, game vui mà"
            });
        }

        // --- UP ĐỆ TỬ ---
        if (msg.contains("đệ") || msg.contains("up")) {
            return getRandom(new String[]{
                "Up đệ chua lắm, bỏ đi làm người", 
                "Treo map nào thế? Qua ks phát", 
                "Mua bùa chưa? Không bùa up tới tết", 
                "Đệ tui ngu lắm, toàn đánh sư phụ"
            });
        }
        
        // --- DOANH TRẠI ---
        if (msg.contains("dt") || msg.contains("doanh trại")) {
             return getRandom(new String[]{
                "Xin slot DT với", 
                "Bang nào đi DT cho ké 1 vé", 
                "Đi DT nhớ mang đậu nha"
            });
        }

        // Case 3: Không khớp từ khóa nào -> Trả lời ngẫu nhiên cho đỡ trống trải
        return getRandom(RANDOM_CHATS);
    }

    private static String getRandom(String[] list) {
        return list[rand.nextInt(list.length)];
    }
}