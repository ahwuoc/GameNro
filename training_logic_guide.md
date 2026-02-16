# Hướng dẫn triển khai Logic TrainingService (Rust)

Dựa trên mã nguồn Java `TrainingService.java`, đây là hướng dẫn để bạn hoàn thiện logic `call_boss_by_id` trong `src/map/services/training_services.rs`.

## 1. Phân tích Logic Java

Hàm `callBoss` trong Java thực hiện các bước sau:
1. **Thiết lập trạng thái**: Đánh dấu người chơi đang trong chế độ "Thách đấu" (`pl.isThachDau = isThachDau`).
2. **Ẩn NPC**: Gửi gói tin ẩn NPC tương ứng với Boss (ví dụ: ẩn Thần Mèo khi gọi Boss Thần Mèo).
3. **Chuyển bản đồ (Đặc biệt)**: Với Boss Thượng Đế, người chơi sẽ được dịch chuyển đến bản đồ 49.
4. **Khởi tạo Boss**: Tạo một instance Boss mới gắn với người chơi.

## 2. Triển khai trong Rust

### Cải thiện cấu trúc dữ liệu
Trước tiên, bạn có thể cần thêm các trường trạng thái vào struct `Player` (trong `src/player/player.rs`) nếu chưa có:
- `is_thach_dau: bool`
- `boss_training_id: Option<String>`

### Hiện thực hàm `call_boss_by_id`

Dưới đây là gợi ý mã nguồn cho `training_services.rs`:

```rust
use crate::boss::boss_id::BOSS_THAN_MEO_KARIN;
use crate::constant::const_npc::NpcId;
use crate::player::Player;
use crate::boss::manager::BossManager;
use crate::map::change_map_service::ChangeMapService;
use crate::services::ServiceHandles;

pub fn get_npc_by_boss_id(boss_id: &str) -> Option<NpcId> {
    match boss_id {
        BOSS_THAN_MEO_KARIN => Some(NpcId::ThanMeoKarin),
        // Thêm các boss khác vào đây:
        // "boss_thuong_de" => Some(NpcId::ThuongDe),
        _ => None,
    }
}

pub async fn call_boss_by_id(player: &mut Player, boss_id: &str, is_thach_dau: bool) -> anyhow::Result<()> {
    // 1. Cập nhật trạng thái người chơi
    // player.is_thach_dau = is_thach_dau; // Cần thêm trường này vào struct Player
    
    // 2. Ẩn NPC (Nếu game server đã hỗ trợ packet HIDE_NPC)
    if let Some(npc_id) = get_npc_by_boss_id(boss_id) {
        // Hiện tại GameNro có vẻ chưa có ServiceHandles::send_hide_npc
        // Bạn có thể cần implement nó với CMD -73 (hoặc CMD tương ứng của client)
        // send_hide_npc(player, npc_id as i16, true); 
    }

    // 3. Xử lý logic chuyển map đặc biệt
    if boss_id == "boss_thuong_de" {
        // Ví dụ chuyển map đến map 49:
        // ChangeMapService::change_map_to_coords(player, 49, 362, 408).await?;
    }

    // 4. Gọi Boss
    // Sử dụng BossManager để spawn boss tại vị trí người chơi
    BossManager::spawn_boss_async(
        boss_id.to_string(),
        player.map_id,
        player.zone_id,
        player.location.x,
        player.location.y,
        Some(player.id), // group_id = player.id để boss chỉ target người chơi này group id la ??? day la logic cua boss ko can thiet vi boss se spwawn vao map offline cua player do do
        0,
        vec![],
    );

    Ok(())
}
```

## 3. Các lưu ý quan trọng

### Ẩn/Hiện NPC (`sendHideNpc`)
Trong Java, `Service.gI().sendHideNpc(pl, npcId, isHide)` thường gửi một packet với `CMD -73`. 
Tuy nhiên, trong `src/data/data_game.rs`, `CMD -73` đang được dùng cho `send_item_data`. Bạn cần kiểm tra lại `CMD` chính xác của bản client bạn đang dùng để ẩn NPC.

Nếu implement, nó sẽ trông như thế này trong `src/services/services.rs`:
```rust
pub fn send_hide_npc(player: &Player, npc_id: i16, is_hide: bool) -> Result<()> {
    let mut msg = Message::new(-73); // Hoặc CMD ẩn NPC của bạn
    msg.write_byte(if is_hide { 1 } else { 0 })?;
    msg.write_short(npc_id)?;
    player.send_to_client(msg)?;
    Ok(())
}
```

### Boss Targeting
Trong `BossManager::spawn_boss`, khi bạn truyền `Some(player.id)` vào `group_id`, bạn có thể tùy chỉnh logic trong `BossActor` để Boss chỉ tấn công hoặc chỉ hiển thị với người chơi đó, tương tự như logic "Boss cá nhân" trong Java.

### Trạng thái Thách đấu
Đảm bảo khi Boss chết hoặc người chơi rời map, bạn gọi một hàm `luyen_tap_end` (tương tự Java) để hiện lại NPC và reset trạng thái `is_thach_dau`.
