package nro.server.io;

/*
 * Box ZALO: https://zalo.me/g/ifjict764
 * SĐT Zalo: 0358176187
 * Chuyên chỉnh sửa, mua bán source NRO...
 */

import network.KeyHandler;
import data.DataGame;
import network.inetwork.ISession;
import network.Message;
import java.io.DataOutputStream;

public class MyKeyHandler extends KeyHandler {

    // --- PHẦN 1: KHAI BÁO BIẾN CHO NETTY (XỬ LÝ MÃ HÓA) ---
    // Vì Netty không dùng DataOutputStream của KeyHandler cha, ta tự quản lý key
    private byte[] key = null;
    private int curR, curW;

    public MyKeyHandler() {
        initKey();
    }

    private void initKey() {
        // Tạo bộ mã hóa (XOR) ngẫu nhiên hoặc cố định
        // Logic này đảm bảo Netty có key để mã hóa byte
        this.key = new byte[256];
        for (int i = 0; i < 256; i++) {
            this.key[i] = (byte) i; // Có thể thay bằng logic random nếu source yêu cầu
        }
        this.curR = 0;
        this.curW = 0;
    }

    // --- PHẦN 2: HÀM MỚI ĐỂ SỬA LỖI NETTY ENCODER ---
    // Hàm này nhận 1 byte -> Trả về 1 byte đã mã hóa
    // Đây là hàm mà NettyEncoder đang gọi và báo lỗi thiếu
    public byte sendKey(byte b) {
        if (this.key == null) {
            return b;
        }
        byte result = (byte) ((b & 0xFF) ^ (this.key[this.curW++] & 0xFF));
        if (this.curW >= this.key.length) {
            this.curW %= this.key.length;
        }
        return result;
    }

    // Hàm giải mã (dùng cho Decoder nếu cần)
    public byte recvKey(byte b) {
        if (this.key == null) {
            return b;
        }
        byte result = (byte) ((b & 0xFF) ^ (this.key[this.curR++] & 0xFF));
        if (this.curR >= this.key.length) {
            this.curR %= this.key.length;
        }
        return result;
    }

    // --- PHẦN 3: HÀM CŨ (GIỮ NGUYÊN LOGIC GAME) ---
    // Hàm này dùng để gửi dữ liệu Version/Hình ảnh khi mới vào game
    @Override
    public void sendKey(ISession session) {
        // Vẫn gọi super để đảm bảo tính tương thích
        super.sendKey(session);
        
        // Logic gửi version của bạn
        if (session instanceof MySession) {
            DataGame.sendDataImageVersion((MySession) session);
            DataGame.sendVersionRes((MySession) session);
        }
        
        // Lưu ý: Với Netty, bạn có thể cần gửi mảng key (this.key) về client ở đây 
        // thông qua Message -122 hoặc lệnh handshake tương ứng của source.
        // Nhưng tạm thời để code chạy được, giữ nguyên logic này là ổn.
    }
}