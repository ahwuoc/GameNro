# Phương hướng Refactor và Scale

## Tổng quan chiến lược

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          REFACTORING ROADMAP                                │
│                                                                             │
│   Phase 1 (1-2 tuần)      Phase 2 (2-4 tuần)      Phase 3 (1-2 tháng)      │
│   ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐        │
│   │   Stabilize     │───►│  Event System   │───►│   Full Scale    │        │
│   │  Two-Phase      │    │  Introduction   │    │   Migration     │        │
│   └─────────────────┘    └─────────────────┘    └─────────────────┘        │
│                                                                             │
│   • Fix deadlocks        • GameEvent enum       • All services async       │
│   • Lock audit           • Event channel        • Actor-like players       │
│   • Message queuing      • Dispatcher           • Distributed ready        │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Stabilize (Tuần 1-2)

### 1.1 Apply Two-Phase Pattern Everywhere

**Mục tiêu**: Loại bỏ tất cả deadlock tiềm ẩn

**Files cần refactor:**

| File | Priority | Complexity |
|------|----------|------------|
| `skill_service.rs` | 🔴 High | Medium |
| `mob_service.rs` | 🔴 High | Medium |
| `effect_skill_service.rs` | ✅ Done | - |
| `player_service.rs` | ✅ Done | - |

**Pattern áp dụng:**
```rust
// BEFORE ❌
fn do_something(player: &mut Player) {
    update_state(player);
    send_message(player);  // Danger: nested lock
}

// AFTER ✅
struct UpdateResult { /* data for messages */ }

fn do_something_state(player: &mut Player) -> UpdateResult {
    update_state(player);
    UpdateResult { ... }
}

fn send_update_messages(update: &UpdateResult) {
    send_message(...);  // Safe: no lock held
}
```

### 1.2 Tạo Message Queue

**Mục tiêu**: Buffer messages thay vì gửi trực tiếp

```rust
// src/services/message_queue.rs
use tokio::sync::mpsc;

pub struct OutgoingMessage {
    pub zone_id: (i32, i32),        // (map_id, zone_id)
    pub target: MessageTarget,
    pub msg: Message,
}

pub enum MessageTarget {
    AllPlayers,
    ExceptPlayer(u64),
    SinglePlayer(u64),
}

pub struct MessageQueue {
    tx: mpsc::Sender<OutgoingMessage>,
}

impl MessageQueue {
    pub fn send(&self, msg: OutgoingMessage) {
        let _ = self.tx.try_send(msg);
    }
}

// Background task to process queue
pub fn spawn_message_dispatcher(mut rx: mpsc::Receiver<OutgoingMessage>) {
    tokio::spawn(async move {
        while let Some(msg) = rx.recv().await {
            dispatch_message(msg);
        }
    });
}
```

### 1.3 Lock Audit & Logging

**Thêm debug logging cho tất cả lock acquisitions:**

```rust
// Tạo wrapper cho PLAYER_MANAGER
pub fn get_mut_traced(&self, id: u64, caller: &str) -> Option<RefMut<...>> {
    #[cfg(debug_assertions)]
    println!("[LOCK] {} acquiring PLAYER_MANAGER.get_mut({})", caller, id);
    
    let result = self.players.get_mut(&id);
    
    #[cfg(debug_assertions)]
    println!("[LOCK] {} acquired lock for player {}", caller, id);
    
    result
}
```

---

## Phase 2: Event System (Tuần 2-4)

### 2.1 Define Game Events

```rust
// src/events/mod.rs
pub mod game_event;
pub mod dispatcher;

// src/events/game_event.rs
use serde::{Serialize, Deserialize};

#[derive(Clone, Debug)]
pub enum GameEvent {
    // Player events
    PlayerJoinedZone { player_id: u64, map_id: i32, zone_id: i32 },
    PlayerLeftZone { player_id: u64, map_id: i32, zone_id: i32 },
    PlayerMoved { player_id: u64, x: i16, y: i16 },
    PlayerDied { player_id: u64, killer_id: Option<u64> },
    PlayerRevived { player_id: u64 },
    
    // Combat events
    DamageDealt { attacker_id: u64, target_id: u64, damage: i32, skill_id: i16 },
    SkillUsed { player_id: u64, skill_id: i16, skill_type: u8 },
    EffectApplied { player_id: u64, effect_type: u8, duration_ms: u64 },
    EffectRemoved { player_id: u64, effect_type: u8 },
    
    // Special events
    PlayerTransformed { player_id: u64, is_monkey: bool, hp: i32, hp_max: i32 },
    ChargingStarted { player_id: u64, skill_id: i16 },
    ChargingStopped { player_id: u64 },
    
    // Mob events
    MobDied { mob_id: u8, zone_key: (i32, i32) },
    MobRespawned { mob_id: u8, zone_key: (i32, i32), hp: i32 },
    MobAttacked { mob_id: u8, target_id: u64, damage: i32 },
}
```

### 2.2 Event Dispatcher

```rust
// src/events/dispatcher.rs
use tokio::sync::mpsc;
use super::game_event::GameEvent;

pub struct EventDispatcher {
    rx: mpsc::Receiver<GameEvent>,
}

impl EventDispatcher {
    pub async fn run(mut self) {
        while let Some(event) = self.rx.recv().await {
            self.handle_event(event).await;
        }
    }
    
    async fn handle_event(&self, event: GameEvent) {
        match event {
            GameEvent::PlayerTransformed { player_id, is_monkey, hp, hp_max } => {
                self.send_monkey_transformation(player_id, is_monkey, hp, hp_max).await;
            }
            GameEvent::DamageDealt { attacker_id, target_id, damage, skill_id } => {
                self.broadcast_damage(attacker_id, target_id, damage, skill_id).await;
            }
            GameEvent::PlayerMoved { player_id, x, y } => {
                self.broadcast_movement(player_id, x, y).await;
            }
            // ... handle other events
            _ => {}
        }
    }
    
    async fn send_monkey_transformation(&self, player_id: u64, is_monkey: bool, hp: i32, hp_max: i32) {
        // Get zone from player (no lock conflict here)
        if let Some(player) = PLAYER_MANAGER.get(player_id) {
            if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
                // Build and send messages
                let mut msg = Message::new(-45);
                // ...
                let _ = zone.send_message_to_all_players(msg);
            }
        }
    }
}
```

### 2.3 Refactor Services to Return Events

```rust
// BEFORE
pub fn set_is_monkey(player: &mut Player) {
    player.effect_skill.is_monkey = true;
    player.n_point.hp_current *= 2;
    send_effect_monkey(player);      // Direct I/O
    send_cai_trang(player);          // Direct I/O
}

// AFTER
pub fn set_is_monkey(player: &mut Player) -> Vec<GameEvent> {
    player.effect_skill.is_monkey = true;
    let old_hp = player.n_point.hp_current;
    player.n_point.hp_current *= 2;
    
    vec![
        GameEvent::PlayerTransformed {
            player_id: player.id,
            is_monkey: true,
            hp: player.n_point.hp_current,
            hp_max: player.n_point.hp_max * 2,
        }
    ]
}

// Usage in update loop
let events = set_is_monkey(&mut player);
drop(player);  // Release lock
for event in events {
    event_tx.send(event).await;
}
```

---

## Phase 3: Full Scale Architecture (Tháng 1-2)

### 3.1 Player Actor Pattern

```rust
// src/player/player_actor.rs
use tokio::sync::mpsc;

pub struct PlayerActor {
    id: u64,
    state: Player,
    mailbox: mpsc::Receiver<PlayerCommand>,
    event_tx: mpsc::Sender<GameEvent>,
}

pub enum PlayerCommand {
    UseSkill { skill_id: i16, target: Option<u64> },
    Move { x: i16, y: i16 },
    TakeDamage { amount: i32, attacker_id: u64 },
    Heal { amount: i32 },
    UpdateEffects,
    GetState { response: oneshot::Sender<PlayerSnapshot> },
}

impl PlayerActor {
    pub async fn run(mut self) {
        let mut update_interval = tokio::time::interval(Duration::from_millis(100));
        
        loop {
            tokio::select! {
                Some(cmd) = self.mailbox.recv() => {
                    self.handle_command(cmd).await;
                }
                _ = update_interval.tick() => {
                    self.update_effects().await;
                }
            }
        }
    }
    
    async fn handle_command(&mut self, cmd: PlayerCommand) {
        let events = match cmd {
            PlayerCommand::UseSkill { skill_id, target } => {
                skill_service::execute(&mut self.state, skill_id, target)
            }
            PlayerCommand::Move { x, y } => {
                self.state.location = Location { x, y };
                vec![GameEvent::PlayerMoved { player_id: self.id, x, y }]
            }
            // ...
        };
        
        for event in events {
            let _ = self.event_tx.send(event).await;
        }
    }
}
```

### 3.2 New Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         NEW SCALED ARCHITECTURE                             │
│                                                                             │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                    │
│   │   Network   │    │   Network   │    │   Network   │                    │
│   │   Worker 1  │    │   Worker 2  │    │   Worker N  │                    │
│   └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                    │
│          │                  │                  │                            │
│          └──────────────────┼──────────────────┘                            │
│                             │                                               │
│                             ▼                                               │
│                    ┌─────────────────┐                                      │
│                    │  Command Router │                                      │
│                    └────────┬────────┘                                      │
│                             │                                               │
│          ┌──────────────────┼──────────────────┐                           │
│          │                  │                  │                            │
│          ▼                  ▼                  ▼                            │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                    │
│   │   Player    │    │   Player    │    │   Player    │                    │
│   │   Actor 1   │    │   Actor 2   │    │   Actor N   │                    │
│   └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                    │
│          │                  │                  │                            │
│          └──────────────────┼──────────────────┘                            │
│                             │                                               │
│                             ▼                                               │
│                    ┌─────────────────┐                                      │
│                    │  Event Channel  │                                      │
│                    │   (broadcast)   │                                      │
│                    └────────┬────────┘                                      │
│                             │                                               │
│          ┌──────────────────┼──────────────────┐                           │
│          │                  │                  │                            │
│          ▼                  ▼                  ▼                            │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                    │
│   │ Zone Actor  │    │ Zone Actor  │    │ Zone Actor  │                    │
│   │  (map 1)    │    │  (map 2)    │    │  (map N)    │                    │
│   └─────────────┘    └─────────────┘    └─────────────┘                    │
│                                                                             │
│   Benefits:                                                                 │
│   • No shared mutable state                                                 │
│   • No locks, no deadlocks                                                  │
│   • Horizontally scalable                                                   │
│   • Each actor can run on different thread/core                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.3 Zone Isolation

```rust
// src/map/zone_actor.rs
pub struct ZoneActor {
    zone_key: (i32, i32),
    player_ids: HashSet<u64>,
    mobs: Vec<RtMob>,
    items: Vec<ItemMap>,
    event_rx: broadcast::Receiver<GameEvent>,
    message_tx: mpsc::Sender<OutgoingMessage>,
}

impl ZoneActor {
    pub async fn run(mut self) {
        let mut tick_interval = tokio::time::interval(Duration::from_millis(1000));
        
        loop {
            tokio::select! {
                Ok(event) = self.event_rx.recv() => {
                    self.handle_event(event).await;
                }
                _ = tick_interval.tick() => {
                    self.update_mobs().await;
                    self.update_items().await;
                }
            }
        }
    }
    
    async fn handle_event(&mut self, event: GameEvent) {
        // Only handle events relevant to this zone
        match event {
            GameEvent::PlayerJoinedZone { player_id, map_id, zone_id } 
                if (map_id, zone_id) == self.zone_key => {
                self.player_ids.insert(player_id);
                self.broadcast_player_joined(player_id).await;
            }
            // ...
        }
    }
}
```

---

## Scaling Strategies

### Horizontal Scaling

```
                    ┌─────────────────┐
                    │  Load Balancer  │
                    └────────┬────────┘
                             │
           ┌─────────────────┼─────────────────┐
           │                 │                 │
           ▼                 ▼                 ▼
    ┌─────────────┐   ┌─────────────┐   ┌─────────────┐
    │  Server 1   │   │  Server 2   │   │  Server 3   │
    │  Maps 1-50  │   │  Maps 51-100│   │ Maps 101-150│
    └──────┬──────┘   └──────┬──────┘   └──────┬──────┘
           │                 │                 │
           └─────────────────┼─────────────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │     Redis       │
                    │  (Cross-server  │
                    │   messaging)    │
                    └─────────────────┘
```

### Vertical Scaling

```rust
// Use tokio runtime with multiple worker threads
#[tokio::main(flavor = "multi_thread", worker_threads = 8)]
async fn main() {
    // Each zone runs on separate thread
    // No shared state between zones = no contention
}
```

---

## Migration Checklist

### Phase 1 Checklist
- [ ] Refactor `skill_service.rs` to Two-Phase
- [ ] Refactor `mob_service.rs` to Two-Phase  
- [ ] Create `MessageQueue` module
- [ ] Add lock tracing in debug mode
- [ ] Test all skills work without deadlock
- [ ] Document all lock acquisition paths

### Phase 2 Checklist
- [ ] Create `GameEvent` enum
- [ ] Create `EventDispatcher`
- [ ] Refactor 3 highest-frequency services to return events
- [ ] Create event channel infrastructure
- [ ] Migrate monkey transformation to events
- [ ] Migrate damage dealing to events
- [ ] Migrate movement to events

### Phase 3 Checklist
- [ ] Design PlayerActor
- [ ] Design ZoneActor
- [ ] Create command routing system
- [ ] Migrate player update to actor
- [ ] Migrate zone update to actor
- [ ] Performance benchmarking
- [ ] Load testing với 1000+ concurrent players

---

## Timeline Summary

| Phase | Duration | Deliverables | Risk |
|-------|----------|--------------|------|
| Phase 1 | 1-2 tuần | No deadlocks, stable | Low |
| Phase 2 | 2-4 tuần | Event system working | Medium |
| Phase 3 | 1-2 tháng | Fully scalable | High |

**Khuyến nghị**: Hoàn thành Phase 1 và 2, evaluate performance trước khi quyết định Phase 3. Phase 3 chỉ cần nếu concurrent players > 500 hoặc có distributed requirements.
