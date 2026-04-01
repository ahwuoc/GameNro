# AI Agent Context for GameNro

## Quick Reference

### Project
- **Type**: MMORPG Game Server
- **Language**: Rust (stable)
- **Architecture**: Actor Model
- **Database**: MySQL + SeaORM
- **Runtime**: Tokio async

### Critical Rules
1. **No cross-actor `.await`** - Use message passing
2. **Actors push info** - Don't pull/wait for responses
3. **Services are stateless** - No actor references
4. **Use DAO layer** - For all database access

### Module Map
- `src/network/` → Packet handling
- `src/player/player_actor/` → Player logic
- `src/map/managers/zone_actor.rs` → Zone/map logic
- `src/boss/` → Boss AI
- `src/item/` → Inventory system
- `src/matches/` → PvP/tournaments

### Workflows
- Add packet: See `.agent/workflows/add_packet.md`
- Add boss: See `.agent/rules/boss_scripting.md`
- Actor pattern: See `.agent/rules/actormodel.md`

### Build Commands
```bash
cargo build          # Debug build
cargo build --release # Production
cargo test           # Run tests
RUST_LOG=info cargo run # Run with logs
```

For detailed information, read:
- `.cursorrules` - Full development guide
- `.agent/rules/` - Architecture documentation
