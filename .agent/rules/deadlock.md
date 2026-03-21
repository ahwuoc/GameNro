---
trigger: always_on
---

# DEADLOCK PREVENTION RULES

## 1. No Nested PLAYER_MANAGER Locks
NEVER call `PLAYER_MANAGER.get*` inside a closure of another `PLAYER_MANAGER` call.
```rust
// WRONG
PLAYER_MANAGER.get_mut(id1, |p1| {
    PLAYER_MANAGER.get_mut(id2, |p2| { ... }); // DEADLOCK
});

// CORRECT
let p1_data = PLAYER_MANAGER.get(id1);
let p2_data = PLAYER_MANAGER.get(id2);
```

## 2. No Broadcast in write-lock Closures
Do NOT call broadcast/ServiceHandles functions inside `modify_player` or `get_mut` closures.
```rust
// WRONG
session.modify_player(|player| {
    ServiceHandles::send_to_all_in_zone(...); // Deadlock risk
    Ok(())
});
```

## 3. Use get_ref in Loops
Prefer `get_ref` (read-only, zero-copy) over `get` (clones data) when iterating.

## 4. Lock Hierarchy Order
Always acquire locks in this order:
1. `ZONE_MANAGER`
2. `PLAYER_MANAGER`
3. Player-specific locks/actors

## 5. No Circular Actor Await
Actor A must not await Actor B if Actor B might await Actor A. Use **Push Model** (SendInfoTo) instead of pull/get.
