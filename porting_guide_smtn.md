# Hướng dẫn Port Logic `addSMTN` sang Rust

Tài liệu này trình bày cấu trúc và cách triển khai logic cộng Tiềm năng/Sức mạnh từ Java sang hệ thống Actor của Rust.

## 1. Phân tích Logic Java
- **Pet**: Khi Pet nhận TNSM, nó cộng cho bản thân và truyền 50% cho Sư phụ (Master). Sư phụ sau đó gọi đệ quy `addSMTN`.
- **Power Limit**: Kiểm tra giới hạn sức mạnh trước khi cộng.
- **Packet**: Gửi packet `-12` (`sendTNSM`) cho client.
- **Clan**: Cộng TNSM cho bang hội nếu `isOri` là true.

## 2. Cấu trúc đề xuất trong Rust

Do Rust sử dụng mô hình **Actor**, chúng ta không thể gọi trực tiếp `master.addSMTN` nếu Master đang ở trong một Thread khác. Chúng ta sẽ gửi tin nhắn (Message) cho Actor của Master.

### Bước 1: Thêm Packet `-12` vào `ServiceHandles`
Trong [services.rs](file:///home/ahwuocdz/GameNro/src/services/services.rs):

```rust
pub fn send_tnsm(player: &Player, type_smtn: i8, param: i64) -> Result<()> {
    let mut msg = Message::new(-12);
    msg.write_byte(type_smtn)?;
    msg.write_long(param)?;
    player.send_to_client(msg)?;
    Ok(())
}
```

### Bước 2: Triển khai Service logic
Tạo file mới [src/services/player_smtn_service.rs](file:///home/ahwuocdz/GameNro/src/services/player_smtn_service.rs):

```rust
use crate::player::Player;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::ServiceHandles;

pub struct PlayerSmtnService;

impl PlayerSmtnService {
    pub fn add_smtn(player: &mut Player, type_smtn: i8, mut param: i64, is_ori: bool) {
        if player.is_pet {
            // 1. Cộng cho Pet
            player.n_point.power_add(param);
            player.n_point.tiem_nang_add(param);

            // 2. Truyền cho Master (50%)
            let master_id = player.id - 1000000; // Theo logic pet/service.rs
            if let Some(master_handle) = PLAYER_MANAGER.get(master_id) {
                let master_param = (param as f64 * 0.5) as i64;
                
                // Gửi tin nhắn cho Master Actor để tránh Deadlock
                let _ = master_handle.send_forget(crate::player::player_actor::PlayerMessage::AddSMTN {
                    type_smtn,
                    param: master_param,
                    is_ori: true,
                });
            }
        } else {
            // Sư phụ hoặc người chơi thường
            if player.n_point.power > player.n_point.get_power_limit() as i64 {
                return;
            }

            match type_smtn {
                1 => player.n_point.tiem_nang_add(param),
                2 => {
                    player.n_point.power_add(param);
                    player.n_point.tiem_nang_add(param);
                }
                _ => player.n_point.power_add(param),
            };

            // Gửi packet cho client
            let _ = ServiceHandles::send_tnsm(player, type_smtn, param);

            // Logic bang hội
            if is_ori && player.clan_id != -1 {
                // Triển khai thêm logic clan add SM ở đây
            }
        }
    }
}
```

### Bước 3: Xử lý Message trong Actor
Trong `src/player/player_actor/actor.rs`, cần thêm xử lý cho `PlayerMessage::AddSMTN`:

```rust
// Trong match msg { ... }
PlayerMessage::AddSMTN { type_smtn, param, is_ori } => {
    PlayerSmtnService::add_smtn(&mut self.player, type_smtn, param, is_ori);
}
```

## 3. Lưu ý Quan Trọng
1. **Tránh Deadlock**: Tuyệt đối không dùng `PLAYER_MANAGER.get_mut` cho Master khi đang ở trong context của Pet. Luôn dùng `send_forget` để gửi message.
2. **Atomic Ops**: Rust handles concurrency thông qua Actor, nên biến `n_point` được bảo vệ bởi Actor lock.
3. **Power Limit**: Phương thức `get_power_limit()` đã có sẵn trong `NPoint`.

---
> [!TIP]
> Bạn có muốn tôi tiến hành thực hiện tạo các file này và tích hợp vào hệ thống không?
