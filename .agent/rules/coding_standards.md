# GameNro Coding Standards & Safety Rules

## 1. Deadlock Prevention (CRITICAL)
- **Rule 1**: NEVER call `PLAYER_MANAGER.get*` inside a closure of another `PLAYER_MANAGER` call.
- **Rule 2**: NO broadcast/ServiceHandles calls inside `modify_player` closures.
- **Rule 3**: Lock hierarchy must ALWAYS be: `ZONE_MANAGER` -> `PLAYER_MANAGER` -> `PlayerActor`.
- **Rule 4**: Use `get_ref` for read-only loop iterations to avoid cloning.

## 2. Actor Model Architecture
- **Communication**: Use `mpsc` channels and `handle.send_forget()` for one-way notifications (HP sync, Info push).
- **Snapshot Pattern**: Always use `get_snapshot().await` for read-only data access to avoid blocking the Actor's main loop.
- **Non-blocking**: Long-running tasks (DB, heavy math) must be `tokio::spawn`ed.

## 3. Boss Scripting
- **Decoupling**: All boss logic stays in `src/boss/scripts/`.
- **Spawning**: Always use `BossManager::spawn_boss_async`.

## 4. Code Style
- Use `tracing::debug!`, `info!`, `error!` for logging.
- Add `LOCK_ENTER`/`LOCK_EXIT` logs for sensitive sections.
- For complex modifications, use the **Take-Modify-Set** pattern: `take_player()` -> modify -> `set_player()`.
