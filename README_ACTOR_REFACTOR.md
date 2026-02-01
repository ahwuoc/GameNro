# Phân Tích Chuyên Sâu: Refactor Player sang Actor Model

## 1. Thực Trạng Sử Dụng Player
Sau khi rà soát toàn bộ source code, tôi nhận thấy struct `Player` được sử dụng cực kỳ dày đặc:
- **Combat (`skill_service.rs`)**: Các hàm như `deal_damage_to_player` nhận 2 `&mut Player`. Đây là điểm cực kỳ nguy hiểm vì nếu lock sai thứ tự sẽ gây deadlock.
- **Inventory (`inventory_service.rs`)**: Hầu hết các hàm nhận `&mut Player` để thay đổi túi đồ.
- **Broadcast (`services.rs`)**: Các hàm như `send_to_all_in_zone` duyệt qua danh sách và lock từng Player để gửi tin nhắn.
- **Map (`change_map_service.rs`)**: Xử lý việc thoát map này và vào map kia, thay đổi tọa độ.

## 2. Vì sao cần Actor thay vì RwLock?
Hiện tại, server dùng `DashMap<u64, Player>` (một loại lock). Khi Player A tương tác với Player B (ví dụ: đánh nhau hoặc dùng skill hỗ trợ như Huýt Sáo):
- **Luồng hiện tại**: Lock A -> Tìm B -> Lock B -> Thay đổi -> Nhả lock.
- **Rủi ro**: Nếu B cũng làm ngược lại cùng lúc -> **Deadlock (Server treo cứng)**.
- **Vấn đề hiệu năng**: Mỗi khi có packet gửi lên, bạn đang `take_player` (Clone toàn bộ Player ra ngoài) và `set_player` (Clone ngược lại). Việc này gây tốn CPU và RAM khi server đông người.

## 3. Giải Pháp Actor "Lai" (Hybrid Actor)
Để tránh việc phải sửa hàng nghìn dòng code logic, tôi đề xuất mô hình Actor Lai:

### Cơ chế:
1.  **Chủ sở hữu**: Mỗi Player khi login sẽ được cấp một **Actor Task** (Tokio Task). Task này là người duy nhất sở hữu struct `Player`.
2.  **Thông điệp (Messages)**: Thay vì bên ngoài gọi `PLAYER_MANAGER.get_mut`, họ sẽ gửi tin nhắn vào Mailbox của Player đó.
3.  **Giữ nguyên Logic**: Các hàm service hiện tại như `skill_service::execute_skill(&mut player)` sẽ **KHÔNG thay đổi**. Chúng chỉ đơn giản là được gọi từ bên trong vòng lặp của Actor.

### Ví dụ mô phỏng thay đổi:

#### Trước (Nguy hiểm):
```rust
// Trong controller.rs
if let Some(mut player) = session.take_player().await {
    skill_service::execute_skill(&mut player, ...);
    session.set_player(player, session.clone()).await;
}
```

#### Sau (An toàn):
```rust
// Trong controller.rs
// Chỉ gửi lệnh, không cần lock hay clone player
player_handle.send(PlayerMessage::UseSkill(msg)).await;

// Trong player/actor.rs (Hệ điều hành của player)
loop {
    let msg = receiver.recv().await;
    match msg {
        PlayerMessage::UseSkill(m) => {
            // Logic cũ vẫn chạy bình thường, nhưng chỉ 1 thread duy nhất được chạm vào
            skill_service::execute_skill(&mut self.player, m); 
        }
    }
}
```

## 4. Kế Hoạch Triển Khai "An Toàn"
Tôi sẽ không sửa tất cả cùng lúc mà đi theo từng bước:
1.  **Bước 1**: Tạo hạ tầng `PlayerActor` và `PlayerHandle`.
2.  **Bước 2**: Thay đổi `PLAYER_MANAGER` để thay vì trả về `Player`, nó trả về `PlayerHandle`.
3.  **Bước 3**: Refactor từng phần một:
    - Ưu tiên 1: Chuyển Update Loop (`player_service::update`) vào Actor.
    - Ưu tiên 2: Chuyển các lệnh từ Controller sang gửi Message.
    - Ưu tiên 3: Thay đổi các hàm gây deadlock như Huýt Sáo (Hồi HP cho cả zone).

## 5. Kết Luận
Refactor sang Actor không có nghĩa là viết lại toàn bộ Game Logic. Nó chỉ là thay đổi **cách chúng ta truy cập và bảo vệ dữ liệu**. 
- **Lợi ích lớn nhất**: Server không bao giờ treo (No deadlocks), hiệu năng ổn định hơn (No deep cloning).
- **Chi phí**: Cần sửa lại các Entry Point (nơi bắt đầu tương tác với Player).

Bạn thấy hướng tiếp cận "Hybrid" này thế nào? Nó sẽ giữ lại được 90% logic code cũ của bạn.
