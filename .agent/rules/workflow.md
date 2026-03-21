---
trigger: always_on
---

# AGENT WORKFLOW & SAFETY CHECKLIST

## Pre-implementation Requirements
1. **Trace Callers**: `grep -rn "func_name" src/` to find if context is inside a lock.
2. **Verify Lock State**: Ask if the target function is called from `modify_player`.
3. **No New PLAYER_MANAGER in Helpers**: Helper functions should take `&Player` references.

## Implementation Patterns
- **Take-Modify-Set**: `session.take_player().await` -> Modify -> `session.set_player()`. Safest for complex logic + broadcasting.
- **Read-Only**: `PLAYER_MANAGER.get_ref(id)` for quick info access.
- **Async Info Push**: Request target to send info to self instead of waiting for snapshot.

## Pre-commit Checklist
- [ ] No nested `PLAYER_MANAGER.get*` (ignore `get_ref` in safe contexts).
- [ ] No broadcast/ServiceHandles calls inside `modify_player` closures.
- [ ] Use `send_forget` for one-way notifications.
- [ ] `cargo check` passes.
- [ ] Debug logs `tracing::debug!("LOCK_ENTER/EXIT")` added for new critical sections.

## REFACTORING RULES
- Check if function is called from a sync or async context.
- Never use `block_on` in sync code called by async runtime.
- Maintain `ZONE_MANAGER` -> `PLAYER_MANAGER` lock order.
- Move specific boss/mob logic to external script files.
