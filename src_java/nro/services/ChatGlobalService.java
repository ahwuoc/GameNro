package nro.services;

/*
 * Box ZALO: https://zalo.me/g/ifjict764
 * SĐT Zalo: 0358176187
 * Chuyên chỉnh sửa, mua bán source NRO...
 */

import nro.player.Player;
import network.Message;
import nro.server.Maintenance;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import utils.Functions;

public class ChatGlobalService implements Runnable {

    private static ChatGlobalService instance;

    // Cấu hình
    private static final int MAX_ACTIVE_CHAT = 100; // Số lượng chat đang hiển thị
    private static final int MAX_WAITING_CHAT = 100; // Hàng chờ tối đa
    private static final int CHAT_DELAY = 1000; // Thời gian trôi tin nhắn (ms)
    private static final int COST_GEM = 5; // Phí ngọc
    private static final long POWER_REQ = 2_000_000_000L; // Sức mạnh yêu cầu
    private static final int TIME_WAIT_NEXT_CHAT = 30000; // Thời gian chờ giữa 2 lần chat

    // Danh sách an toàn luồng (Thread-safe)
    private final List<ChatGlobal> listChatting;
    private final List<ChatGlobal> waitingChat;

    // Dữ liệu NPC giả (Static để tiết kiệm bộ nhớ)
    private static final Object[][] FAKE_NPCS = {
        {"Quy Lão Kame", 33, 34, 35},
        {"Lý Tiểu Nương", 487, 488, 489},
        {"Bò Mộng", 80, 81, 82},
        {"Thần Mèo Karin", 89, 90, 91},
        {"Thượng Đế", 86, 87, 88},
        {"Thần Vũ Trụ", 98, 99, 100},
        {"Bunma", 267, 268, 269},
        {"Ca Lích", 270, 271, 272},
        {"Bunma Tương Lai", 42, 43, 44},
        {"Santa", 300, 301, 302}
    };

    private ChatGlobalService() {
        this.listChatting = Collections.synchronizedList(new LinkedList<>());
        this.waitingChat = Collections.synchronizedList(new LinkedList<>());
        this.start();
    }

    public static ChatGlobalService gI() {
        if (instance == null) {
            synchronized (ChatGlobalService.class) {
                if (instance == null) {
                    instance = new ChatGlobalService();
                }
            }
        }
        return instance;
    }

    private void start() {
        Thread.ofVirtual().name("Chat Global Worker").start(this);
    }

    /**
     * Chat VIP (Không tốn phí, không check time)
     */
    public void chatVip(Player player, String text) {
        addChatToQueue(new ChatGlobal(player, sanitizeText(text)));
    }

    /**
     * Chat thường (Có check delay time của player)
     */
    public void chat1(Player player, String text) {
        player.iDMark.setLastTimeChatGlobal(System.currentTimeMillis());
        addChatToQueue(new ChatGlobal(player, sanitizeText(text)));
    }

    /**
     * Chat thế giới chuẩn (Có check điều kiện, trừ tiền)
     */
    public void chat(Player player, String text) {
        // 1. Kiểm tra hàng chờ
        if (waitingChat.size() >= MAX_WAITING_CHAT) {
            Service.gI().sendThongBao(player, "Kênh thế giới hiện đang quá tải, vui lòng thử lại sau.");
            return;
        }

        // 2. Kiểm tra Spam (Nội dung trùng lặp đang chạy)
        synchronized (listChatting) {
            for (ChatGlobal chat : listChatting) {
                if (chat.text.equals(text)) {
                    Service.gI().sendThongBao(player, "Tin nhắn tương tự đang hiển thị.");
                    return;
                }
            }
        }

        // 3. Kiểm tra điều kiện ngọc
        if (player.inventory.gem < COST_GEM) {
            Service.gI().sendThongBao(player, "Bạn không đủ ngọc để chat thế giới.");
            return;
        }

        // 4. Kiểm tra điều kiện thời gian & Sức mạnh/Admin
        boolean isAdmin = player.isAdmin();
        boolean canChatTime = Util.canDoWithTime(player.iDMark.getLastTimeChatGlobal(), TIME_WAIT_NEXT_CHAT);

        if (isAdmin || canChatTime) {
            if (isAdmin || player.nPoint.power >= POWER_REQ) {
                // Trừ tiền
                player.inventory.subGemAndRuby(COST_GEM);
                Service.gI().sendMoney(player);
                
                // Cập nhật thời gian và thêm vào hàng chờ
                player.iDMark.setLastTimeChatGlobal(System.currentTimeMillis());
                addChatToQueue(new ChatGlobal(player, sanitizeText(text)));
            } else {
                Service.gI().sendThongBao(player, "Sức mạnh phải ít nhất 2 tỷ mới có thể chat thế giới.");
            }
        } else {
            Service.gI().sendThongBao(player, "Vui lòng đợi " 
                    + TimeUtil.getTimeLeft(player.iDMark.getLastTimeChatGlobal(), TIME_WAIT_NEXT_CHAT / 1000) 
                    + " nữa.");
        }
    }

    /**
     * Hàm dùng cho Bot hoặc Hệ thống chat
     */
    public void autoChatGlobal(Player player, String message) {
        if (waitingChat.size() >= MAX_WAITING_CHAT) {
            if (player != null) {
                Service.gI().sendThongBao(player, "Kênh thế giới quá tải.");
            }
            return;
        }

        String safeText = sanitizeText(message);
        if (player == null) {
            // Chat hệ thống (NPC giả)
            addChatToQueue(new ChatGlobal(safeText));
        } else {
            // Bot chat (như player)
            addChatToQueue(new ChatGlobal(player, safeText));
        }
    }

    private void addChatToQueue(ChatGlobal chatGlobal) {
        synchronized (waitingChat) {
            waitingChat.add(chatGlobal);
        }
    }

    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                // 1. Xử lý danh sách đang hiển thị (Xóa tin nhắn cũ)
                synchronized (listChatting) {
                    if (!listChatting.isEmpty()) {
                        ChatGlobal chat = listChatting.get(0);
                        if (Util.canDoWithTime(chat.timeSendToPlayer, CHAT_DELAY)) {
                            listChatting.remove(0); // Tự động dispose bởi GC
                        }
                    }
                }

                // 2. Đẩy tin nhắn từ hàng chờ sang danh sách hiển thị
                synchronized (waitingChat) {
                    if (!waitingChat.isEmpty() && listChatting.size() < MAX_ACTIVE_CHAT) {
                        ChatGlobal chat = waitingChat.remove(0);
                        chat.timeSendToPlayer = System.currentTimeMillis();
                        
                        listChatting.add(chat);
                        sendChatPacket(chat);
                    }
                }
                
                synchronized (waitingChat) {
                    if (!waitingChat.isEmpty() && listChatting.size() < MAX_ACTIVE_CHAT) {
                        ChatGlobal chat = waitingChat.remove(0);
                        chat.timeSendToPlayer = System.currentTimeMillis();
                        
                        listChatting.add(chat);
                        sendChatPacket(chat);
                        if (chat.playerId > 0) {
                             Bot.BotChatTG.onPlayerChat(chat.playerName, chat.text);
                        }
                    }
                }

                // Sleep hợp lý
                long sleepTime = 1000;
                // Nếu hàng chờ đông, đẩy nhanh tốc độ một chút (giảm delay)
                if (waitingChat.size() > 10) {
                    sleepTime = 500;
                }
                Functions.sleep(sleepTime);

            } catch (Exception e) {
                Logger.logException(ChatGlobalService.class, e, "Error in ChatGlobal run loop");
            }
        }
    }

    private void sendChatPacket(ChatGlobal chat) {
        Message msg = null;
        try {
            msg = new Message(92);
            msg.writer().writeUTF(chat.playerName);
            msg.writer().writeUTF("|5|" + chat.text);
            msg.writer().writeInt(chat.playerId);
            msg.writer().writeShort(chat.head);
            msg.writer().writeShort(-1); // Body 2?
            msg.writer().writeShort(chat.body);
            msg.writer().writeShort(chat.bag);
            msg.writer().writeShort(chat.leg);
            msg.writer().writeByte(0);
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
            Logger.logException(ChatGlobalService.class, e, "Error sending chat packet");
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    /**
     * Inner Class đại diện cho một tin nhắn
     */
    private static class ChatGlobal {
        public String playerName;
        public int playerId;
        public short head;
        public short body;
        public short leg;
        public short bag;
        public String text;
        public long timeSendToPlayer;

        // Constructor cho tin nhắn hệ thống (NPC ngẫu nhiên)
        public ChatGlobal(String text) {
            Random random = new Random();
            int index = random.nextInt(FAKE_NPCS.length);
            
            this.playerName = (String) FAKE_NPCS[index][0];
            this.head = (short) (int) FAKE_NPCS[index][1];
            this.body = (short) (int) FAKE_NPCS[index][2];
            this.leg = (short) (int) FAKE_NPCS[index][3];
            this.playerId = -1;
            this.bag = -1;
            this.text = text;
        }

        // Constructor cho tin nhắn người chơi
        public ChatGlobal(Player player, String text) {
            if (!player.isAdmin()) {
                this.playerName = player.name;
            } else if ("ENZEEFX_NRO".equals(player.name)) {
                this.playerName = player.name + " - Founder";
            } else {
                this.playerName = player.name + " - Quản Trị Viên";
            }
            
            this.playerId = (int) player.id;
            this.head = player.getHead();
            this.body = player.getBody();
            this.leg = player.getLeg();
            this.bag = player.getFlagBag();
            this.text = text;
        }
    }
}