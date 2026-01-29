# Đánh giá Architecture và Khuyến nghị Refactor

## Tổng quan Codebase

| Module | Files | Vai trò |
|--------|-------|---------|
| `player/` | 13 files | Player struct, manager, mapper |
| `services/` | 10 files | Game logic, skill, effect |
| `map/` | 18 files | Zone, map, waypoint |
| `network/` | 5 files | Session, controller |
| `entities/` | 65 files | Database models |

---

## Vấn đề Architecture Hiện Tại

### 1. Global Mutable State với Nested Locks

```
┌──────────────────────────────────────────────────────────┐
│                    PLAYER_MANAGER                        │
│                  (Global DashMap)                        │
│                                                          │
│   ┌─────────────────┐  ┌─────────────────┐               │
│   │ player_service  │  │     Zone        │               │
│   │ get_mut(id) ────┼──┼→ get(*id)       │ ← Deadlock!   │
│   │                 │  │                 │               │
│   └─────────────────┘  └─────────────────┘               │
└──────────────────────────────────────────────────────────┘
```

**Vấn đề**: 28 chỗ dùng `PLAYER_MANAGER`, nhiều nơi nested access.

### 2. Mixed Concerns trong Services

```rust
// ❌ Service làm cả 2 việc
fn set_is_monkey(player: &mut Player) {
    player.effect_skill.is_monkey = true;     // 1. State update
    send_effect_monkey(player);                // 2. Network I/O
    send_cai_trang(player);                    // 3. More I/O
}
```

### 3. Zone Coupling với PLAYER_MANAGER

```rust
// Zone lưu ID nhưng access thông qua global manager
pub struct Zone {
    pub player_ids: Arc<DashSet<u64>>,  // Chỉ lưu IDs
    // Mỗi lần access phải gọi PLAYER_MANAGER.get()
}
```

### 4. Thiếu Separation of Concerns

```
Controller → Service → Manager → Zone → PLAYER_MANAGER
                ↓
            Network I/O (trực tiếp trong service!)
```

---

## Khuyến nghị Refactor

### Option A: Event-Driven Architecture (Khuyến nghị)

```
┌─────────┐    ┌─────────────┐    ┌────────────┐    ┌──────────┐
│ Service │───►│ Event Queue │───►│ Dispatcher │───►│ Network  │
│ (pure)  │    │ (channel)   │    │ (async)    │    │ (send)   │
└─────────┘    └─────────────┘    └────────────┘    └──────────┘
      │
      ▼
  State only, no I/O
```

**Lợi ích:**
- Không có nested locks
- Services trở thành pure functions
- Dễ test, dễ debug
- Scale tốt hơn

**Implementation:**
```rust
// events.rs
pub enum GameEvent {
    PlayerTransformed { player_id: u64, to_monkey: bool },
    PlayerDamaged { player_id: u64, amount: i32 },
    SkillUsed { player_id: u64, skill_id: i16 },
}

// service (pure, no I/O)
fn transform_to_monkey(player: &mut Player) -> Vec<GameEvent> {
    player.effect_skill.is_monkey = true;
    player.n_point.hp_current *= 2;
    vec![GameEvent::PlayerTransformed { 
        player_id: player.id, 
        to_monkey: true 
    }]
}

// dispatcher (handles events)
async fn dispatch_event(event: GameEvent) {
    match event {
        GameEvent::PlayerTransformed { player_id, to_monkey } => {
            // Safe to access zone/send messages here
            send_monkey_effect(player_id, to_monkey).await;
        }
    }
}
```

---

### Option B: Actor Model với Tokio Actors

```
┌─────────────────────────────────────────────────────────┐
│                     Actor System                        │
│                                                         │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐            │
│  │ Player   │   │ Player   │   │ Zone     │            │
│  │ Actor 1  │   │ Actor 2  │   │ Actor    │            │
│  │          │   │          │   │          │            │
│  │ Mailbox  │   │ Mailbox  │   │ Mailbox  │            │
│  └──────────┘   └──────────┘   └──────────┘            │
│        ▲              ▲              ▲                  │
│        └──────────────┼──────────────┘                  │
│                       │                                 │
│                   Messages                              │
└─────────────────────────────────────────────────────────┘
```

**Lợi ích:**
- Mỗi actor có own state, không cần shared locks
- Message passing thay vì shared memory
- Built-in concurrency

**Nhược điểm:**
- Refactor lớn
- Learning curve cao
- Overhead message passing

---

### Option C: Improved Two-Phase Pattern (Quick Fix)

Áp dụng pattern đã dùng cho BIEN_KHI cho **TẤT CẢ** services.

**Rule:**
```
1. Collect data (hold lock)
2. Release lock
3. Send messages (no lock)
```

**Lợi ích:**
- Ít thay đổi nhất
- Đã proven work

**Nhược điểm:**
- Manual, dễ quên
- Code verbose

---

## So sánh Options

| Tiêu chí | Option A (Events) | Option B (Actors) | Option C (Two-Phase) |
|----------|-------------------|-------------------|----------------------|
| Effort | Medium | High | Low |
| Scalability | Excellent | Excellent | Good |
| Maintainability | Excellent | Good | Fair |
| Learning Curve | Medium | High | Low |
| Risk | Medium | High | Low |

---

## Đề xuất Lộ trình

### Phase 1: Stabilize (Now - 2 weeks)
- Áp dụng **Option C** (Two-Phase) cho tất cả deadlock-prone areas
- Document các patterns cần follow
- Add logging để detect nested locks

### Phase 2: Introduce Events (2-4 weeks)
- Tạo `GameEvent` enum
- Tạo event channel (tokio::mpsc)
- Migrate 1-2 features sang event-driven

### Phase 3: Full Migration (1-2 months)
- Convert tất cả services sang pure functions returning events
- Remove direct I/O calls từ services
- Performance tuning

---

## Immediate Actions Needed

### 1. Audit các vị trí nguy hiểm

```bash
# Tìm tất cả get_mut calls
grep -rn "get_mut" src/services/
grep -rn "get_mut" src/map/services/
```

### 2. Các files cần fix theo Two-Phase pattern:
- `src/services/effect_skill_service.rs` ✅ (đã fix)
- `src/services/skill_service.rs` ⚠️ (cần review)
- `src/services/player_service.rs` ✅ (đã fix)
- `src/map/services/mob_service.rs` ⚠️ (cần review)

### 3. Tạo lint/review checklist

Mỗi PR cần verify:
- [ ] Functions với `&mut Player` không gọi `send_*` functions
- [ ] Tất cả I/O sau khi release lock
- [ ] No nested `PLAYER_MANAGER.get*` calls

---

## Kết luận

Architecture hiện tại **acceptable cho prototype/MVP** nhưng sẽ gặp nhiều issues khi scale. 

**Khuyến nghị:** 
1. Short-term: Apply Two-Phase consistently
2. Medium-term: Migrate to Event-Driven

Bạn chọn hướng nào để tôi tạo implementation plan chi tiết?
