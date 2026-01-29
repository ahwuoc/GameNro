# Fix Deadlock trong Skill BIEN_KHI (Biến Khỉ)

## Vấn đề gốc

Khi dùng skill BIEN_KHI, server bị **DEADLOCK** - player freeze mãi mãi, không biến thành khỉ được.

## Phân tích nguyên nhân

### Luồng xử lý trước khi fix:

```
player_service::update()
    │
    ├── PLAYER_MANAGER.get_mut(player_id)  ← Lấy WRITE LOCK trên player
    │       │
    │       └── EffectSkillService::finish_use_monkey(&mut player)
    │               │
    │               └── set_is_monkey(&mut player)
    │                       │
    │                       └── send_effect_monkey(&player)
    │                               │
    │                               └── send_mess_all_player_in_map()
    │                                       │
    │                                       └── zone.send_message_to_all_players()
    │                                               │
    │                                               └── Cố gắng iterate qua players
    │                                                   trong zone → CẦN LOCK KHÁC!
    │
    └── DEADLOCK! Đang giữ player lock, cố lấy thêm lock khác
```

### Tại sao xảy ra deadlock?

1. **`update()` giữ write lock** trên player thông qua `PLAYER_MANAGER.get_mut(player_id)`

2. **Gọi `send_mess_all_player_in_map()`** trong khi vẫn giữ lock đó

3. **`send_message_to_all_players()`** cần iterate qua tất cả players trong zone, có thể cần acquire thêm locks

4. **Kết quả**: Thread bị block mãi mãi chờ lock mà không bao giờ được release → **DEADLOCK**

## Giải pháp: Two-Phase Pattern

Tách việc xử lý thành 2 phase riêng biệt:

### Phase 1: Update State (giữ lock)
```rust
// Chỉ update player state, KHÔNG gửi message
let update = EffectSkillService::finish_use_monkey_state(&mut player);
// Lock được release sau khi ra khỏi scope
```

### Phase 2: Send Messages (sau khi release lock)
```rust
// Giờ không còn giữ lock nào, an toàn để gửi messages
EffectSkillService::send_monkey_messages(&update);
```

### Luồng xử lý sau khi fix:

```
player_service::update()
    │
    │   ╔═══════════════════════════════════════════╗
    │   ║ PHASE 1: Update State (giữ lock)          ║
    │   ╚═══════════════════════════════════════════╝
    │
    ├── PLAYER_MANAGER.get_mut(player_id)  ← Lấy WRITE LOCK
    │       │
    │       └── finish_use_monkey_state(&mut player)
    │               │
    │               └── set_is_monkey_state(&mut player)
    │                       │
    │                       └── Update các field trong player
    │                       └── Return MonkeyStateUpdate struct với data cần thiết
    │
    │   ← LOCK ĐƯỢC RELEASE ở đây (ra khỏi scope)
    │
    │   ╔═══════════════════════════════════════════╗
    │   ║ PHASE 2: Send Messages (không có lock)    ║
    │   ╚═══════════════════════════════════════════╝
    │
    └── send_monkey_messages(&update)  ← Không giữ lock nào!
            │
            └── zone.send_message_to_all_players() ← An toàn!
```

## Cấu trúc code mới

### MonkeyStateUpdate struct
Chứa tất cả data cần thiết để gửi messages sau khi release lock:

```rust
pub struct MonkeyStateUpdate {
    pub player_id: u64,
    pub map_id: i32,
    pub zone_id: i32,
    pub skill_id: i16,
    pub is_monkey: bool,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub speed: i8,
    pub hp_current: i32,
    pub hp_max: i32,
}
```

### Các function mới

| Function | Mục đích |
|----------|----------|
| `finish_use_monkey_state()` | Update state, return data |
| `set_is_monkey_state()` | Update monkey state, return data |
| `monkey_down_state()` | Update revert state, return data |
| `send_monkey_messages()` | Gửi tất cả messages từ data |

## Bài học rút ra

> **Rule**: Khi đang giữ lock trên một resource, **KHÔNG BAO GIỜ** gọi function có thể cần acquire lock khác.

### Pattern tránh deadlock:

1. **Collect data** trong khi giữ lock
2. **Release lock** trước khi làm I/O hoặc gọi external functions
3. **Process/Send** data sau khi đã release lock

### Áp dụng trong Rust:
```rust
// Tốt ✅
let update = {
    let mut player = PLAYER_MANAGER.get_mut(id);
    update_state(&mut player)  // Return data, không gửi message
};  // Lock released here
send_messages(&update);  // An toàn

// Xấu ❌
let mut player = PLAYER_MANAGER.get_mut(id);
update_and_send(&mut player);  // Gửi message trong khi giữ lock → Deadlock!
```

## Files đã sửa

- `src/services/effect_skill_service.rs` - Thêm `*_state` functions và `send_monkey_messages`
- `src/services/player_service.rs` - Sử dụng two-phase pattern trong update loop
