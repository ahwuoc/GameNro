# 🗺️ Refactoring Roadmap - GameNro Project

## 📊 Current State Analysis

### Code Metrics (Before Refactoring)
```
Total Lines of Code: 32,714
Average File Size: 154 lines
Largest File: 1,218 lines (player_actor.rs)
Files > 500 lines: 15 files
Files > 300 lines: 45 files
Estimated Technical Debt: ~3 months
```

### Problem Files (Top 15)
| # | File | Lines | Priority | Estimated Effort |
|---|------|-------|----------|------------------|
| 1 | player_actor.rs | 1,218 | P0 | 3 days |
| 2 | clan_service.rs | 979 | P0 | 4 days |
| 3 | change_map_service.rs | 852 | P0 | 3 days |
| 4 | boss_actor.rs | 767 | P1 | 3 days |
| 5 | skill_service.rs | 764 | P1 | 4 days |
| 6 | player.rs | 705 | P1 | 3 days |
| 7 | zone_actor.rs | 692 | P1 | 3 days |
| 8 | data_game.rs | 657 | P2 | 2 days |
| 9 | task_service.rs | 625 | P2 | 3 days |
| 10 | mob_service.rs | 592 | P2 | 2 days |
| 11 | n_point.rs | 556 | P2 | 2 days |
| 12 | pet_actor.rs | 534 | P2 | 2 days |
| 13 | dhvt/manager.rs | 496 | P3 | 2 days |
| 14 | effect_skill_service.rs | 469 | P3 | 2 days |
| 15 | pvp_service.rs | 435 | P3 | 2 days |

**Total Effort: ~40 days (8 weeks)**

---

## 🎯 Goals & Success Metrics

### Target Metrics (After Refactoring)
```
Average File Size: < 200 lines ✅
Largest File: < 400 lines ✅
Files > 500 lines: 0 files ✅
Files > 300 lines: < 10 files ✅
Code Duplication: < 10% ✅
Test Coverage: > 60% ✅
```

### Success Criteria
- ✅ All files under 400 lines
- ✅ Clear separation of concerns
- ✅ No code duplication > 3 times
- ✅ All modules have unit tests
- ✅ Documentation updated
- ✅ No performance regression

---

## 📅 Timeline: 8 Weeks (2 Months)

### Week 1-2: Foundation & Critical Files (P0)
### Week 3-4: High Priority Files (P1)
### Week 5-6: Medium Priority Files (P2)
### Week 7: Low Priority Files (P3)
### Week 8: Testing, Documentation & Cleanup

---

## 🚀 Week 1-2: Foundation & Critical Files (P0)

### Week 1: Setup & Player Actor

#### Day 1-2: Setup Infrastructure
**Goal:** Tạo foundation cho refactoring

**Tasks:**
- [ ] Create refactoring branch: `refactor/maintainability`
- [ ] Setup code metrics tracking
- [ ] Create test harness for regression testing
- [ ] Document current behavior (integration tests)

**Deliverables:**
```bash
# Branch structure
refactor/maintainability
├── .github/workflows/metrics.yml
├── tests/regression/
│   ├── player_actor_tests.rs
│   ├── clan_service_tests.rs
│   └── ...
└── docs/metrics/
    └── baseline.md
```

**Commands:**
```bash
git checkout -b refactor/maintainability
cargo test --all # Baseline tests
tokei src/ > docs/metrics/baseline.md
```

---

#### Day 3-5: Refactor player_actor.rs (1,218 → ~300 lines)
**Goal:** Complete PlayerActor refactoring

**Current Structure:**
```
player_actor.rs (1,218 lines)
└── Everything in one file
```

**Target Structure:**
```
player/player_actor/
├── player_actor.rs (300 lines) - Core actor loop
├── handlers/ (already created ✅)
│   ├── combat.rs (180 lines)
│   ├── inventory.rs (220 lines)
│   ├── map.rs (120 lines)
│   ├── pet.rs (40 lines)
│   ├── skill.rs (20 lines)
│   ├── task.rs (30 lines)
│   ├── fusion.rs (120 lines)
│   ├── magic_tree.rs (60 lines)
│   ├── network.rs (50 lines)
│   └── misc.rs (140 lines)
└── lifecycle.rs (80 lines) - Startup/shutdown
```

**Tasks:**
- [x] Create handler modules (DONE ✅)
- [ ] Refactor `handle_message()` to use handlers
- [ ] Extract lifecycle logic to `lifecycle.rs`
- [ ] Update imports in `player_actor.rs`
- [ ] Run tests: `cargo test player_actor`
- [ ] Verify no performance regression

**Code Changes:**
```rust
// Before (player_actor.rs - 1,218 lines)
async fn handle_message(&mut self, msg: PlayerMessage) {
    match msg {
        PlayerMessage::AttackMob { mob_id } => {
            // 50 lines of logic here
        }
        // ... 40+ more messages
    }
}

// After (player_actor.rs - 300 lines)
async fn handle_message(&mut self, msg: PlayerMessage) {
    use handlers::*;
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

**Testing:**
```bash
# Run specific tests
cargo test --package arc_nro --lib player::player_actor

# Performance benchmark
cargo bench player_actor

# Integration test
cargo test --test integration_player_actor
```

**Success Criteria:**
- ✅ player_actor.rs < 400 lines
- ✅ All tests pass
- ✅ No performance regression (< 5% slower)
- ✅ Code coverage > 50%

---

### Week 2: Clan & Map Services

#### Day 6-8: Refactor clan_service.rs (979 → ~150 lines/module)
**Goal:** Tách clan service thành modules

**Current Structure:**
```
clan/clan_service.rs (979 lines)
└── All clan logic
```

**Target Structure:**
```
clan/
├── clan_service.rs (150 lines) - Main coordinator
├── services/
│   ├── mod.rs
│   ├── creation.rs (120 lines) - Create/delete clan
│   ├── members.rs (180 lines) - Member management
│   ├── chat.rs (100 lines) - Clan chat
│   ├── wars.rs (150 lines) - Clan wars
│   ├── upgrades.rs (120 lines) - Clan upgrades
│   ├── storage.rs (100 lines) - Clan storage
│   └── permissions.rs (80 lines) - Permission system
└── models/
    └── clan_state.rs (80 lines) - Clan state management
```

**Tasks:**
- [ ] Analyze current clan_service.rs functions
- [ ] Group functions by domain
- [ ] Create service modules
- [ ] Extract to separate files
- [ ] Update imports across codebase
- [ ] Run tests: `cargo test clan`

**Migration Strategy:**
```rust
// Step 1: Create new modules (keep old code)
// Step 2: Copy functions to new modules
// Step 3: Update callers to use new modules
// Step 4: Delete old code
// Step 5: Test everything
```

**Commands:**
```bash
# Find all usages
rg "clan_service::" --type rust

# Create structure
mkdir -p src/clan/services
touch src/clan/services/{mod,creation,members,chat,wars,upgrades,storage,permissions}.rs

# Run tests
cargo test clan
```

---

#### Day 9-10: Refactor change_map_service.rs (852 → ~150 lines/module)
**Goal:** Tách map change logic

**Current Structure:**
```
map/services/change_map_service.rs (852 lines)
└── All map change logic
```

**Target Structure:**
```
map/services/change_map/
├── mod.rs (100 lines) - Main coordinator
├── waypoint.rs (150 lines) - Waypoint teleport
├── zone_change.rs (120 lines) - Zone switching
├── validation.rs (100 lines) - Map validation
├── sync.rs (150 lines) - Player/pet/item sync
├── transitions.rs (120 lines) - Map transitions
└── capsule.rs (80 lines) - Capsule/spaceship
```

**Tasks:**
- [ ] Identify all map change scenarios
- [ ] Group by functionality
- [ ] Create module structure
- [ ] Extract functions
- [ ] Update callers
- [ ] Test all map change scenarios

**Testing Scenarios:**
```rust
#[cfg(test)]
mod tests {
    // Test waypoint teleport
    // Test zone change
    // Test map validation
    // Test player sync
    // Test pet sync
    // Test capsule travel
}
```

---

## 🚀 Week 3-4: High Priority Files (P1)

### Week 3: Combat & Player Systems

#### Day 11-13: Refactor skill_service.rs (764 → ~150 lines/module)
**Goal:** Tách skill system

**Target Structure:**
```
services/combat/skills/
├── mod.rs (100 lines)
├── execution.rs (150 lines) - Skill execution
├── damage.rs (120 lines) - Damage calculation
├── effects.rs (150 lines) - Effect application
├── cooldown.rs (80 lines) - Cooldown management
├── targeting.rs (100 lines) - Target selection
└── validation.rs (80 lines) - Skill validation
```

**Tasks:**
- [ ] Map all skill types
- [ ] Extract damage calculation
- [ ] Extract effect system
- [ ] Create skill registry
- [ ] Add skill tests
- [ ] Performance benchmark

**Pattern: Strategy Pattern**
```rust
pub trait SkillExecutor {
    fn execute(&self, caster: &Player, target: Option<&Player>) -> Result<SkillResult>;
}

pub struct DamageSkill;
impl SkillExecutor for DamageSkill {
    fn execute(&self, caster: &Player, target: Option<&Player>) -> Result<SkillResult> {
        // Damage logic
    }
}

pub struct BuffSkill;
impl SkillExecutor for BuffSkill {
    fn execute(&self, caster: &Player, target: Option<&Player>) -> Result<SkillResult> {
        // Buff logic
    }
}
```

---

#### Day 14-16: Refactor player.rs (705 → ~200 lines + traits)
**Goal:** Separate data from behavior

**Current Structure:**
```rust
pub struct Player {
    // 100+ fields
    // 50+ methods mixed together
}
```

**Target Structure:**
```
player/
├── player.rs (200 lines) - Data model only
├── traits/
│   ├── mod.rs
│   ├── combat.rs (100 lines) - Combat behavior
│   ├── movement.rs (80 lines) - Movement behavior
│   ├── inventory.rs (100 lines) - Inventory behavior
│   ├── stats.rs (120 lines) - Stats calculation
│   └── sync.rs (80 lines) - Sync behavior
└── builder.rs (100 lines) - Player builder pattern
```

**Tasks:**
- [ ] Identify all player methods
- [ ] Group by domain
- [ ] Create trait definitions
- [ ] Implement traits
- [ ] Update callers
- [ ] Add trait tests

**Pattern: Trait-based Composition**
```rust
// player.rs - Data only
pub struct Player {
    pub id: u64,
    pub name: String,
    // ... fields only
}

// traits/combat.rs
pub trait PlayerCombat {
    fn injured(&mut self, damage: u64, piercing: bool) -> u64;
    fn is_die(&self) -> bool;
    fn revive(&mut self);
}

impl PlayerCombat for Player {
    fn injured(&mut self, damage: u64, piercing: bool) -> u64 {
        // Implementation
    }
}

// Usage
use player::traits::PlayerCombat;
player.injured(100, false);
```

---

#### Day 17-18: Refactor boss_actor.rs (767 → ~250 lines + modules)
**Goal:** Separate actor from AI

**Target Structure:**
```
boss/
├── boss_actor.rs (250 lines) - Actor loop only
├── ai/
│   ├── mod.rs
│   ├── state_machine.rs (150 lines) - AI states
│   ├── targeting.rs (100 lines) - Target selection
│   ├── movement.rs (100 lines) - Movement AI
│   └── decision.rs (120 lines) - Decision making
└── behaviors/
    ├── mod.rs
    ├── aggressive.rs (80 lines)
    ├── defensive.rs (80 lines)
    └── support.rs (80 lines)
```

**Tasks:**
- [ ] Extract AI state machine
- [ ] Create behavior system
- [ ] Separate targeting logic
- [ ] Update boss scripts
- [ ] Test all boss types

---

### Week 4: Zone & Data Systems

#### Day 19-21: Refactor zone_actor.rs (692 → ~250 lines + modules)
**Goal:** Simplify zone actor

**Target Structure:**
```
map/models/
├── zone_actor.rs (250 lines) - Actor loop
├── zone_services/
│   ├── mod.rs
│   ├── mob_spawning.rs (120 lines)
│   ├── item_drops.rs (100 lines)
│   ├── player_sync.rs (100 lines)
│   └── broadcast.rs (80 lines)
└── zone_state.rs (100 lines)
```

---

#### Day 22-23: Refactor data_game.rs (657 → ~150 lines/module)
**Goal:** Organize game data

**Target Structure:**
```
data/
├── data_game.rs (150 lines) - Main loader
├── loaders/
│   ├── items.rs (100 lines)
│   ├── skills.rs (100 lines)
│   ├── maps.rs (100 lines)
│   ├── mobs.rs (100 lines)
│   └── npcs.rs (100 lines)
└── cache/
    └── game_cache.rs (150 lines)
```

---

## 🚀 Week 5-6: Medium Priority Files (P2)

### Week 5: Task & Mob Systems

#### Day 24-26: Refactor task_service.rs (625 → Strategy Pattern)
**Goal:** Implement task checker system

**Target Structure:**
```
services/world/tasks/
├── mod.rs (100 lines)
├── task_service.rs (150 lines) - Coordinator
├── checkers/
│   ├── mod.rs
│   ├── kill_mob.rs (80 lines)
│   ├── collect_item.rs (80 lines)
│   ├── talk_npc.rs (60 lines)
│   ├── go_to_map.rs (60 lines)
│   └── level_up.rs (60 lines)
└── registry.rs (100 lines) - Task registry
```

**Pattern:**
```rust
pub trait TaskChecker: Send + Sync {
    fn check(&self, player: &Player, target: &str) -> bool;
    fn progress(&self, player: &Player) -> (i32, i32);
}

pub struct TaskRegistry {
    checkers: HashMap<TaskType, Arc<dyn TaskChecker>>,
}
```

---

#### Day 27-28: Refactor mob_service.rs (592 → ~150 lines/module)
**Goal:** Organize mob logic

**Target Structure:**
```
map/services/mob/
├── mod.rs
├── mob_service.rs (150 lines)
├── ai.rs (150 lines) - Mob AI
├── spawning.rs (120 lines) - Spawn logic
├── combat.rs (100 lines) - Mob combat
└── drops.rs (100 lines) - Loot drops
```

---

#### Day 29-30: Refactor n_point.rs (556 → ~150 lines/module)
**Goal:** Organize stats calculation

**Target Structure:**
```
player/components/stats/
├── mod.rs
├── n_point.rs (150 lines) - Core stats
├── calculation.rs (150 lines) - Stat calc
├── buffs.rs (120 lines) - Buff system
└── equipment.rs (100 lines) - Equipment stats
```

---

### Week 6: Pet & Match Systems

#### Day 31-32: Refactor pet_actor.rs (534 → ~200 lines + modules)
#### Day 33-34: Refactor dhvt/manager.rs (496 → modules)
#### Day 35-36: Refactor effect_skill_service.rs (469 → modules)

---

## 🚀 Week 7: Low Priority & Utilities (P3)

### Day 37-40: Refactor remaining P3 files
- [ ] pvp_service.rs
- [ ] player_mapper.rs
- [ ] command.rs
- [ ] Other files > 300 lines

### Day 41-42: Create Common Utilities

**New Utilities:**
```
utils/
├── message_builder.rs - Message construction
├── validation.rs - Common validations
├── error_handling.rs - Error types
└── patterns.rs - Common patterns
```

---

## 🚀 Week 8: Testing, Documentation & Cleanup

### Day 43-44: Testing
- [ ] Run full test suite
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Load testing

### Day 45-46: Documentation
- [ ] Update architecture docs
- [ ] Update API docs
- [ ] Create migration guide
- [ ] Update README

### Day 47-48: Code Review & Cleanup
- [ ] Code review
- [ ] Fix issues
- [ ] Remove dead code
- [ ] Final cleanup

### Day 49-50: Merge & Deploy
- [ ] Final testing
- [ ] Merge to main
- [ ] Deploy to staging
- [ ] Monitor metrics

---

## 📊 Progress Tracking

### Weekly Checklist

**Week 1:**
- [ ] Setup complete
- [ ] player_actor.rs refactored
- [ ] Tests passing

**Week 2:**
- [ ] clan_service.rs refactored
- [ ] change_map_service.rs refactored
- [ ] Tests passing

**Week 3:**
- [ ] skill_service.rs refactored
- [ ] player.rs refactored
- [ ] boss_actor.rs refactored

**Week 4:**
- [ ] zone_actor.rs refactored
- [ ] data_game.rs refactored

**Week 5:**
- [ ] task_service.rs refactored
- [ ] mob_service.rs refactored
- [ ] n_point.rs refactored

**Week 6:**
- [ ] pet_actor.rs refactored
- [ ] dhvt/manager.rs refactored
- [ ] effect_skill_service.rs refactored

**Week 7:**
- [ ] All P3 files refactored
- [ ] Utilities created

**Week 8:**
- [ ] Testing complete
- [ ] Documentation complete
- [ ] Merged to main

---

## 🎯 Success Metrics Dashboard

### Code Quality Metrics
```bash
# Run weekly
./scripts/check_metrics.sh

# Expected output:
Files > 500 lines: 0 ✅
Files > 400 lines: 0 ✅
Files > 300 lines: < 10 ✅
Average file size: < 200 lines ✅
Code duplication: < 10% ✅
Test coverage: > 60% ✅
```

### Performance Metrics
```bash
# Benchmark weekly
cargo bench

# Expected: < 5% regression
```

---

## 🚨 Risk Management

### Risks & Mitigation

**Risk 1: Breaking Changes**
- Mitigation: Comprehensive test suite
- Mitigation: Feature flags for gradual rollout

**Risk 2: Performance Regression**
- Mitigation: Continuous benchmarking
- Mitigation: Performance tests in CI

**Risk 3: Timeline Slip**
- Mitigation: Weekly reviews
- Mitigation: Adjust scope if needed

**Risk 4: Team Bandwidth**
- Mitigation: Prioritize P0/P1 only if needed
- Mitigation: Can extend to 10 weeks

---

## 📞 Communication Plan

### Daily Standups
- What was done yesterday
- What will be done today
- Any blockers

### Weekly Reviews
- Progress vs plan
- Metrics review
- Adjust priorities

### Bi-weekly Demos
- Show refactored modules
- Performance comparison
- Get feedback

---

## 🎉 Definition of Done

### Per Module
- [ ] Code refactored and under size limit
- [ ] Unit tests written and passing
- [ ] Integration tests passing
- [ ] Documentation updated
- [ ] Code reviewed
- [ ] No performance regression

### Overall Project
- [ ] All P0/P1 files refactored
- [ ] All tests passing
- [ ] Documentation complete
- [ ] Metrics targets met
- [ ] Deployed to production
- [ ] Team trained on new structure

---

**Start Date:** TBD  
**Target Completion:** 8 weeks from start  
**Owner:** Development Team  
**Status:** 🟡 Planning Phase
