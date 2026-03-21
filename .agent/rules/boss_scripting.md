---
trigger: always_on
---

# BOSS & ACTOR SCRIPTING

## 1. Script Decoupling
Logic for specific Bosses MUST be in `BossScript` (`src/boss/scripts/boss_*.rs`) and registered in `register.rs`. Keep `BossActor` generic.

## 2. Sub-boss Spawning
Always use `BossManager::spawn_boss_async` to prevent blocking the parent actor.

## 3. Injury Logic Template
```rust
async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
    let final_damage = apply_reduction(damage);
    let real_damage = actor.player.injured(final_damage, piercing);
    ServiceHandles::send_player_injured(&actor.player, real_damage as i32, false, 0);
    ServiceHandles::send_hp_sync(&actor.player);
    real_damage
}
```

## 4. State Management
Use `AtomicBool` or `AtomicU64` for script-local state (Scripts are wrapped in `Arc` and shared).
