package Bot;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import nro.services.ChatGlobalService;
import utils.Logger;
import utils.Util;

public class BotChatTG implements Runnable {

    private final Bot bot;
    private volatile boolean running = true;
    private long lastChatTime = 0;

    public BotChatTG(Bot bot) {
        this.bot = bot;
        this.lastChatTime = System.currentTimeMillis();
        startChatThread();
    }

    private void startChatThread() {
        Thread.ofVirtual().start(this);
    }

    @Override
    public void run() {
        // ... (Giữ nguyên logic tự động chat ngẫu nhiên của bài trước) ...
        // Logic cũ: Bot tự nói bâng quơ mỗi 45-90s
        try { TimeUnit.SECONDS.sleep(new Random().nextInt(20)); } catch (InterruptedException e) {}
        
        while (running && bot != null && !bot.isDie()) {
            try {
                // Tự động chat bâng quơ (Logic cũ)
                String msg = BotAIGenerator.generate(""); // Truyền rỗng để nó tự nghĩ
                if (bot.zone != null) {
                    ChatGlobalService.gI().autoChatGlobal(null, "[" + bot.name + "] " + msg);
                }
                long sleepTime = 60000 + new Random().nextInt(60000); // 1-2 phút chat 1 lần
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            } catch (Exception e) {
                try { TimeUnit.SECONDS.sleep(10); } catch (InterruptedException ex) {}
            }
        }
    }

    public void stop() {
        running = false;
    }

    // ========================================================================
    // 🟢 TÍNH NĂNG MỚI: BOT TỰ TRẢ LỜI NGƯỜI CHƠI (AUTO REPLY)
    // ========================================================================

    /**
     * Hàm này được gọi từ ChatGlobalService khi có người chơi chat
     * @param playerName Tên người chơi vừa chat
     * @param text Nội dung người chơi chat
     */
    // Trong file BotChatTG.java

    // ... (Code cũ giữ nguyên) ...

    /**
     * BOT NGHE VÀ TRẢ LỜI (Được gọi từ ChatGlobalService)
     */
    public static void onPlayerChat(String playerName, String text) {
        // 1. Tỉ lệ trả lời: 40% (Tăng lên chút cho xôm)
        if (!utils.Util.isTrue(40, 100)) return;

        // 2. Bỏ qua spam
        if (text.length() < 2 || text.startsWith("!")) return;

        // 3. Chạy luồng ảo để gọi Gemini
        Thread.ofVirtual().start(() -> {
            try {
                // Giả vờ suy nghĩ/gõ phím (2-4 giây)
                TimeUnit.SECONDS.sleep(utils.Util.nextInt(2, 4));

                Bot bot = getRandomActiveBot(); // Hàm lấy bot ngẫu nhiên đã viết ở bài trước
                if (bot == null) return;

                // --- GỌI GEMINI ---
                // Truyền đúng câu chat của người chơi vào để Gemini phân tích
                String aiReply = BotAIGenerator.generate(text);

                // Gửi ra kênh thế giới: @TenNguoiChoi NoiDung
                String finalMsg = "@" + playerName + " " + aiReply;
                
                nro.services.ChatGlobalService.gI().autoChatGlobal(null, "[" + bot.name + "] " + finalMsg);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Lấy ngẫu nhiên 1 con bot đang online để trả lời
     */
    private static Bot getRandomActiveBot() {
        try {
            List<Bot> bots = BotManager.gI().bot;
            if (bots.isEmpty()) return null;
            
            // Lọc ra bot đang có BotChatTG hoạt động (để đảm bảo nó đang 'sống')
            Bot b = bots.get(new Random().nextInt(bots.size()));
            if (b.chatBot != null && b.zone != null) {
                return b;
            }
        } catch (Exception e) {}
        return null;
    }
}