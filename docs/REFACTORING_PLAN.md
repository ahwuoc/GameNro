# 📋 Player Actor Refactoring Plan

## Vấn Đề Hiện Tại
- `player_actor.rs`: 1219 dòng - quá dài, khó maintain
- Tất cả logic xử lý message nằm trong 1 file
- Khó test từng phần riêng biệt

## Giải Pháp: Module Hóa Handlers

### Cấu Trúc Mới
```
src/player/player_actor/
├── player_actor.rs          # Core actor loop (giảm xuống ~300 dòng)
├── message.rs               # Message definitions
├── handle.rs                # PlayerHandle
├── mod.rs                   # Module exports
└── handlers/                # ✨ NEW: Handler modules
    ├── mod.rs
    ├── combat.rs            # Combat, attack, injured
    ├── inventory.rs         # Item pickup, use, management
    ├── map.rs               # Map change, movement
    ├── pet.rs               # Pet interactions
    ├── skill.rs             # Skill selection, usage
    ├── task.rs              # Task progression
    ├── fusion.rs            # Fusion/unfusion logic
    ├── magic_tree.rs        # Magic tree actions
    └── network.rs           # Network command routing
```

### Lợi Ích

1. **Separation of Concerns**
   - Mỗi handler chịu trách nhiệm 1 domain cụ thể
   - Dễ tìm và sửa bug

2. **Testability**
   - Test từng handler độc lập
   - Mock dependencies dễ dàng

3. **Maintainability**
   - File nhỏ hơn, dễ đọc
   - Thêm feature mới không làm file chính phình to

4. **Team Collaboration**
   - Nhiều người có thể làm việc song song
   - Ít conflict khi merge code

### Implementation Status

#### ✅ Đã Hoàn Thành
- [x] `handlers/mod.rs` - Module structure
- [x] `handlers/combat.rs` - Combat logic (180 dòng)
- [x] `handlers/inventory.rs` - Inventory logic (220 dòng)

#### 🚧 Đang Làm
- [ ] `handlers/map.rs` - Map & movement logic
- [ ] `handlers/pet.rs` - Pet management
- [ ] `handlers/skill.rs` - Skill system
- [ ] `handlers/task.rs` - Task progression
- [ ] `handlers/fusion.rs` - Fusion system
- [ ] `handlers/magic_tree.rs` - Magic tree
- [ ] `handlers/network.rs` - Network routing

#### 📝 Cần Làm
- [ ] Refactor `player_actor.rs` để sử dụng handlers
- [ ] Update tests
- [ ] Update documentation

### Migration Strategy

**Phase 1: Create Handlers** (Current)
- Tạo các handler modules
- Copy logic từ `player_actor.rs`
- Giữ nguyên `player_actor.rs` để không break code

**Phase 2: Integration**
- Update `player_actor.rs` để gọi handlers
- Test từng handler
- Đảm bảo không có regression

**Phase 3: Cleanup**
- Xóa code cũ trong `player_actor.rs`
- Update imports
- Final testing

### Code Pattern

#### Before (player_actor.rs)
```rust
async fn handle_message(&mut self, msg: PlayerMessage) {
    match msg {
        PlayerMessage::AttackMob { mob_id } => {
            // 50 dòng logic ở đây
        }
        PlayerMessage::PickItem { item_map_id } => {
            // 100 dòng logic ở đây
        }
        // ... 40+ messages
    }
}
```

#### After (player_actor.rs)
```rust
async fn handle_message(&mut self, msg: PlayerMessage) {
    match msg {
        PlayerMessage::AttackMob { mob_id } => {
            CombatHandler::handle_attack_mob(&mut self.player, mob_id).await;
        }
        PlayerMessage::PickItem { item_map_id } => {
            InventoryHandler::handle_pick_item(
                &mut self.player,
                &self.session,
                item_map_id
            ).await;
        }
        // ... clean delegation
    }
}
```

### Testing Strategy

```rust
#[cfg(test)]
mod tests {
    use super::*;
    
    #[tokio::test]
    async fn test_combat_injured() {
        let mut player = create_test_player();
        CombatHandler::handle_injured(&mut player, 100, false, true).await;
        assert!(player.n_point.hp_current < player.n_point.hp_max);
    }
}
```

### Timeline

- **Week 1**: Create all handler modules
- **Week 2**: Integration & testing
- **Week 3**: Cleanup & documentation

### Notes

- Giữ backward compatibility trong quá trình refactor
- Không thay đổi message protocol
- Không ảnh hưởng đến Actor Model architecture
- Handlers là stateless, chỉ nhận parameters

---

**Status**: 🚧 In Progress  
**Last Updated**: 2026-04-01  
**Owner**: Development Team
