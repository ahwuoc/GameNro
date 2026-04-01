# GitHub Copilot Instructions for GameNro

## Project Type
Rust game server using Actor Model architecture with Tokio async runtime.

## Key Constraints

### Actor Model Rules
- PlayerActor and ZoneActor communicate via message passing only
- NEVER use `.await` calls between different actors
- Use `tokio::mpsc` channels for inter-actor communication

### Code Structure
- Handlers in `src/network/handlers/` process packets
- Actors in `src/player/player_actor/` and `src/map/managers/zone_actor.rs`
- Services are stateless business logic
- DAOs handle database operations

### Common Patterns
```rust
// Message passing between actors
actor.send(Message::Action { data }).await?;

// Service usage (stateless)
let result = ItemService::use_item(&player, item_id)?;

// Database access via DAO
let player = PlayerDao::find_by_id(&db, player_id).await?;
```

## Reference Files
- `.agent/rules/` - Architecture rules
- `.agent/workflows/` - Development workflows
- `.cursorrules` - Detailed development guide
