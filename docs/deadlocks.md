# Các Trường Họp Gây Deadlock và Cách Phòng Ngừa

Tài liệu này mô tả các tình huống gây treo hệ thống (deadlock) được phát hiện trong mã nguồn **Arc NRO** và hướng dẫn kiến trúc để phòng tránh.

## Cụ Thể Các Trường Hợp Deadlock

### 1. Khóa Lồng Nhau Trên Cùng Shard (Nested DashMap Locking)
**Mức độ: NGHIÊM TRỌNG**

`DashMap` và `DashSet` chia dữ liệu thành nhiều mảnh (shard) để xử lý song song. Tuy nhiên, khóa của chúng **không hỗ trợ tái nhập (non-reentrant)**.
- **Tình huống**: Một luồng đang giữ khóa Ghi (`get_mut`) trên một nhân vật. Trong khi đang giữ khóa, nó gọi một hàm khác (ví dụ: gửi hiệu ứng) và hàm đó lại cố gắng lấy khóa Đọc (`get`) trên chính nhân vật đó hoặc nhân vật khác nằm cùng shard.
- **Kết quả**: Luồng tự chờ chính mình mãi mãi (Deadlock).
- **Vị trí ví dụ**: `services/player_service.rs` trong vòng lặp `update`.

**Mã nguồn minh họa lỗi:**
```rust
// Thread 1
if let Some(mut player) = PLAYER_MANAGER.get_mut(player_id) { // Giữ khóa GHI Shard A
    // ... xử lý logic ...
    
    // Hàm này bên trong lại gọi PLAYER_MANAGER.get(player_id) để lấy info
    services::effect_skill_service::send_effect_player(&player); 
    // -> Cố gắng lấy khóa ĐỌC Shard A -> DEADLOCK vì Shard A đang bị khóa GHI
}
```

### 2. Đảo Ngược Thứ Tự Khóa (Lock Inversion)
**Mức độ: CAO**

- **Luồng A**: Khóa **Player** trước -> Sau đó cố gắng khóa **Zone Mobs**.
- **Luồng B**: Khóa **Zone Mobs** trước -> Sau đó cố gắng khóa **Player**.
- **Kết quả**: A chờ B, B chờ A.

**Mã nguồn minh họa lỗi:**
```rust
// Thread A (Ví dụ: Player Update)
let mut player = PLAYER_MANAGER.get_mut(id); // 1. Khóa Player
let mobs = zone.active_mobs.read();          // 2. Chờ khóa Zone...

// Thread B (Ví dụ: Mob AI)
let mut mobs = zone.active_mobs.write();     // 1. Khóa Zone
let player = PLAYER_MANAGER.get(id);         // 2. Chờ khóa Player... -> DEADLOCK
```

### 3. Xung Đột Khi Duyệt Danh Sách (Iterator Contention)
**Mức độ: TRUNG BÌNH**

- **Tình huống**: Duyệt qua `zone.player_ids` (`DashSet`) sẽ giữ khóa Đọc trên shard của tập hợp đó. Nếu trong vòng lặp lại gọi `PLAYER_MANAGER.get_mut(id)`, và cả hai cấu trúc này vô tình dùng chung shard ID, sẽ gây deadlock.
- **Vị trí ví dụ**: `map/services/mob_service.rs` trong hàm `find_target_in_range`.

**Mã nguồn minh họa lỗi:**
```rust
// DashSet lock shard để duyệt (Read Lock)
for player_id in zone.player_ids.iter() { 
    // Cố gắng lấy Write Lock trên Player Manager
    // Nếu DashSet và PlayerManager dùng chung thuật toán hash & shard -> Có thể trúng cùng Shard
    if let Some(mut p) = PLAYER_MANAGER.get_mut(*player_id) { 
        // -> DEADLOCK
    }
}
```

### 4. Lỗi Broadcast Khi Đang Giữ Khóa (Cross-Manager Broadcast)
**Mức độ: NGHIÊM TRỌNG**

- **Tình huống**: Một Manager (như `SessionManager`) gọi `modify_player` (giữ khóa Ghi). Trong closure đó lại gọi các hàm broadcast gửi tin cho toàn map. Hàm broadcast này lại đi scan danh sách người chơi và `get` (khóa Đọc) từng người.
- **Kết quả**: Treo cứng shard chứa người chơi đang bị khóa Ghi.
- **Khu vực ảnh hưởng**: `SessionManager::kick_player`, `ChangeMapService::exit_map`, `UseItemService`.

**Mã nguồn minh họa lỗi:**
```rust
// Controller hoặc Service
session.modify_player(|player| { // 1. Giữ khóa GHI nhân vật A
    
    // Logic xử lý...
    // Gọi hàm exit map, hàm này gửi thông báo cho người khác
    crate::map::services::change_map_service::exit_map(player);
    
    // Bên trong exit_map gọi:
    // ServiceHandles::send_mess_another_not_me_in_map(player, msg)
    // -> Hàm này duyệt qua tất cả player trong zone và gọi PLAYER_MANAGER.get(id)
    // -> Khi duyệt đến id của nhân vật A -> Cố lấy khóa ĐỌC -> DEADLOCK
    
    Ok(())
});
```

---

## Kết Quả Rà Soát Manager

| Manager | Loại Khóa | Mức Độ Rủi Ro |
| :--- | :--- | :--- |
| `PlayerManager` | `DashMap` | **Cực Cao**. Khóa shard thường bị giữ lâu trong các closure xử lý logic. |
| `SessionManager` | `DashMap` | **Cao**. Dễ gây deadlock nếu các hàm async như `kick_player` khóa lại tài nguyên cũ. |
| `ZoneManager` | `RwLock<HashMap>` | **Thấp**. Thường chỉ khóa trong thời gian ngắn để lấy tham chiếu Zone. |
| `MapManager` | `DashMap` | **Thấp**. Dữ liệu chủ yếu là tĩnh sau khi khởi tạo. |

---

## Giải Pháp Đề Xuất & Hướng Dẫn

### A. Đệm Các Tác Vụ Phụ (Mô hình "Side-Effect")
Thay vì thực hiện các hành động liên quan đến tài nguyên khác (như gửi gói tin, update mob) ngay khi đang giữ khóa, hãy thu thập các hành động đó vào một danh sách.
- **Quy trình chuẩn**:
  1. Khóa Entity -> Cập nhật trạng thái -> Trả về `Vec<UpdateEvent>`.
  2. **Nhả Khóa**.
  3. Duyệt danh sách `Vec<UpdateEvent>` và thực thi (ví dụ: gửi broadcast).

### B. Tuân Thủ Thứ Tự Khóa
Luôn lấy khóa theo thứ tự toàn cục nghiêm ngặt:
1. **Maps** (Tần suất thấp nhất)
2. **Zones**
3. **Mobs**/Items (Trong Zones)
4. **Players** (Tần suất cao nhất)

> [!CAUTION]
> Tuyệt đối không bao giờ cố khóa Zone hoặc Mob khi đang giữ khóa Player.

### C. Giảm Thiểu Phạm Vi Khóa
Sử dụng các scope tạm thời `{ ... }` để đảm bảo khóa được nhả ngay khi thao tác dữ liệu xong. Dùng `DashMap::get` để clone dữ liệu nhỏ ra ngoài thay vì giữ `Ref` lâu.

### D. Ưu Tiên `try_send` Cho Giao Tiếp
Khi giao tiếp giữa các Actor (hoặc kênh tin nhắn Session), hãy dùng `try_send` (không chặn). Nếu kênh đầy, hãy ghi log cảnh báo thay vì làm treo cả vòng lặp update.
