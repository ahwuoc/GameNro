# Phân Tích Chuyên Sâu Kiến Trúc Mạng SplitSession (Update v2)

Tài liệu này cung cấp cái nhìn **cực kỳ chi tiết** về kiến trúc mạng mới, dành cho những ai muốn hiểu sâu về cơ chế hoạt động, lý do kỹ thuật và các mẫu thiết kế (design patterns) được áp dụng.

## 1. Bối Cảnh Kỹ Thuật & Vấn Đề Deadlock

### 1.1. Kiến Trúc Cũ: "Khóa Vạn Năng" (The God Lock)
Trước đây, chúng ta sử dụng `tokio::sync::RwLock<AsyncSession>` bọc toàn bộ object session.

```rust
// Mô hình cũ
let session: Arc<RwLock<AsyncSession>>;
```

**Cơ chế của RwLock:**
*   **Read Lock (`read()`):** Cho phép nhiều lường đọc cùng lúc.
*   **Write Lock (`write()`):** Độc quyền. Khi ai đó giữ write lock, **không ai khác** được đọc hay ghi.

**Kịch Bản Thảm Họa (The Deadlock Scenario):**
Hệ thống game server có đặc thù là giao tiếp hai chiều liên tục (Full Duplex).

1.  **Read Task (Luồng Đọc):** Luôn phải giữ một tham chiếu đến `socket` để đọc dữ liệu.
    *   Trong code cũ: `let mut session = session.write().await;` -> Giữ khóa GHI để đọc socket (vì `TcpStream.read` cần `&mut`).
    *   Sau đó: `socket.read().await`.
    *   **Vấn đề:** Trong suốt thời gian chờ gói tin từ client (có thể là mãi mãi nếu client AFK), khóa GHI vẫn bị giữ!

2.  **Logic Task (Luồng Game):** Quái đánh người chơi, cần gửi tin nhắn thông báo.
    *   Cần gọi: `session.write().await` để gửi tin.
    *   **Kết quả:** Bị chặn đứng (Blocked) vì Read Task đang giữ khóa GHI.

-> **Hệ quả:** Server "treo" cứng ngắc. Read Task chờ client, Logic Task chờ Read Task.

## 2. Giải Pháp: "Chia Để Trị" (SplitSession Architecture)

Chúng ta áp dụng nguyên lý **Segregation of Concerns** (Phân tách mối quan tâm). Thay vì một khóa to, chúng ta chia nhỏ `AsyncSession` thành các phần tử nguyên tử (atomic parts).

### 2.1. Cấu Trúc Dữ Liệu Mới

```rust
pub struct AsyncSession {
    // 1. Kênh Đọc (Read Channel)
    // Mutex đảm bảo chỉ 1 luồng được đọc tại 1 thời điểm (thường là Read Loop)
    pub reader: Mutex<SessionReader>, 
    
    // 2. Kênh Ghi (Write Channel)
    // Mutex đảm bảo các tin nhắn không bị ghi đè lên nhau (byte interleaving)
    pub writer: Mutex<SessionWriter>, 
    
    // 3. Trạng Thái (State)
    // RwLock cho phép nhiều luồng đọc thông tin (map, hp...) cùng lúc
    // Nhưng chỉ 1 luồng được sửa đổi (tránh race condition)
    pub state: RwLock<SessionState>,
    
    // 4. Hàng Đợi Đồng Bộ (Synchronous Queue) << CẢI TIẾN QUAN TRỌNG
    // Sử dụng std::sync::RwLock thay vì tokio::sync::RwLock để truy cập NHANH & ĐỒNG BỘ
    pub message_tx: std::sync::RwLock<Option<mpsc::Sender<Message>>>,
}
```

## 3. Luồng Dữ Liệu (Data Flow) & Tối Ưu Hóa Queue

Chúng ta hãy đi theo hành trình của một tin nhắn để thấy sự khác biệt.

### 3.1. Luồng Gửi (Sending Flow) - Non-Blocking & Sync

**Trường hợp: Người chơi đánh quái -> Cần gửi tin hiển thị sát thương.**

1.  **Caller (Controller/Service):**
    *   Gọi `session.send_message(msg)`.
    *   Hành động: Clone message và đẩy vào channel `message_tx`.
    *   **Cải tiến:** Hàm `queue_message` bây giờ là **Synchronous** (không cần `await`).
    *   *Lợi ích:* Có thể gọi trực tiếp bên trong các closure đồng bộ (như `modify_player`).

2.  **MPSC Channel (Hàng đợi):**
    *   Đóng vai trò bộ đệm (Buffer). Nếu mạng chậm, tin nhắn sẽ xếp hàng ở đây thay vì chặn đứng Server.

3.  **Write Task (Background Worker):**
    *   Một vòng lặp vô tận: `while let Some(msg) = rx.recv().await`.
    *   Khi có tin nhắn:
        1.  Lock `writer` (`Mutex`).
        2.  Mã hóa (Encrypt) tin nhắn.
        3.  Ghi xuống `TcpStream` (Syscall).
        4.  Unlock `writer`.

### 3.2. Luồng Đọc (Reading Flow)

1.  **Read Task:**
    *   Lock `reader` (`Mutex`).
    *   Gọi `read_message().await`.
    *   Nếu chưa có dữ liệu: Task này sẽ "ngủ" (Suspend). Tuy nhiên, nó chỉ giữ khóa `reader`.
    *   **Quan Trọng:** Việc nó ngủ và giữ khóa `reader` **KHÔNG ẢNH HƯỞNG** đến `writer` hay `state`. Các luồng khác vẫn hoạt động bình thường.

## 4. Quản Lý Trạng Thái An Toàn (Safe State Management)

Rust có quy tắc: "Reference XOR Mutable Reference" (Hoặc nhiều người đọc, hoặc một người ghi).

### Vấn đề: `get_player_mut()`
Hàm này trả về `&mut Player`. Nếu bạn giữ cái này và gọi `.await` (ví dụ: `db.save().await`), bạn đang giữ khóa độc quyền trong thời gian dài -> Lại gây tắc nghẽn (dù nhẹ hơn Deadlock nhưng vẫn làm giảm hiệu năng - Latency Spike).

### Giải pháp: "Mượn - Trả" (Take/Set Pattern)

Chúng ta sử dụng `Option.take()` để "move" (di chuyển) quyền sở hữu `Player` ra khỏi Session tạm thời.

**Quy trình chuẩn:**

1.  **Take:** `let player = session.take_player().await;`
    *   Trong Session, `state.player` trở thành `None`.
    *   Bạn đang cầm `Player` trên tay (Owned).

2.  **Modify:** Bạn thích làm gì thì làm: trừ máu, thêm item, đổi map...
3.  **Set:** `session.set_player(player).await;` (Trả về).

### 3.3. Sử Dụng Helper `modify_player` (Khuyên Dùng)

Để tránh quên `set_player`, mình đã thêm hàm tiện ích `modify_player`. Bây giờ nó hỗ trợ gửi tin nhắn trực tiếp bên trong closure nhờ tối ưu hóa `std::sync::RwLock`.

```rust
// An toàn & Gọn gàng hơn
session.modify_player(|player| {
    player.map_id = 5;
    player.inventory.add_item(...);
    
    // Gửi tin nhắn ngay lập tức (không cần gom vào Vector nữa!)
    session.queue_message(msg_hp_update); 
    session.queue_message(msg_bag_update);
    
    Ok(())
}).await?;
```

Nếu dùng cách này, bạn **không cần gọi** `take_player` và `set_player` thủ công nữa.

## 5. Tổng Kết

Kiến trúc SplitSession (v2) mang lại hiệu năng tối đa nhờ loại bỏ gần như toàn bộ các tác vụ async không cần thiết ở tầng gửi tin nhắn.

| Đặc Điểm | Cũ (Single Lock) | Mới (SplitSession v2) |
| :--- | :--- | :--- |
| **Độ Phức Tạp** | Thấp | Trung bình |
| **An Toàn Data** | Tốt | Rất tốt (Type System đảm bảo) |
| **Gửi Tin Nhắn** | Blocking (Chậm) | **Non-blocking & Sync (Siêu nhanh)** |
| **Rủi Ro Deadlock** | Rất cao | Gần như bằng 0 |
| **Code Logic** | Rắc rối (phải gom msg) | Gọn gàng (gửi thẳng trong closure) |

Hy vọng tài liệu này giúp bạn hiểu rõ "chân tơ kẽ tóc" của hệ thống mạng mới!
