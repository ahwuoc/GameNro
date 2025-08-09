# GameNro Rust Source Code Structure

## Overview
This directory contains the Rust implementation of GameNro, organized into logical modules following Rust conventions.

## Directory Structure

### `entities/` - Database Entities
- Contains all SeaORM entity definitions
- Auto-generated database models
- Used for database operations

### `network/` - Network Layer
- `async_net/` - Asynchronous network handling
  - `controller.rs` - Message controller
  - `message.rs` - Message definitions
  - `session.rs` - Session management

### `player/` - Player System
- `player.rs` - Player entity and logic
- `inventory.rs` - Inventory management
- `player_skill.rs` - Player skills
- `friend.rs` - Friend system
- `n_point.rs` - Player points/stats

### `services/` - Business Logic Services
- `player_info_service.rs` - Player information service
- `item_service.rs` - Item management service
- `item_time_service.rs` - Time-based item service
- `item_map_service.rs` - Map item service
- `inventory_service.rs` - Inventory service
- `npc_service.rs` - NPC service
- `change_map_service.rs` - Map changing service
- `map_service.rs` - Map management service
- `manager.rs` - General manager
- `god_gk.rs` - Core game logic
- `services.rs` - Service utilities

### `data/` - Game Data & Configuration
- `data_game.rs` - Core game data
- `config_manager.rs` - Configuration management
- `game_session.rs` - Session data
- `waypoint.rs` - Waypoint system

### `models/` - Core Game Models
- `item.rs` - Item model
- `mob.rs` - Mob model
- `map.rs` - Map model
- `npc.rs` - NPC model
- `zone.rs` - Zone model
- `npc_factory.rs` - NPC factory
- `item_model.rs` - Item model (legacy)
- `inventory_model.rs` - Inventory model (legacy)
- `skill_model.rs` - Skill model (legacy)
- `zone_model.rs` - Zone model (legacy)

### `utils/` - Utilities
- `database.rs` - Database utilities
- `location.rs` - Location utilities

### `features/` - Game Features
- `task_player.rs` - Player task system
- `side_task_template.rs` - Side task templates
- `option_card.rs` - Option card system

## Organization Principles

1. **Separation of Concerns**: Each module has a specific responsibility
2. **Rust Conventions**: Following Rust naming and module conventions
3. **Logical Grouping**: Related functionality is grouped together
4. **Clear Dependencies**: Import paths clearly show module relationships

## Migration from Java

This structure mirrors the organization of the original Java codebase (`src_java/`) but adapted for Rust:
- Java packages → Rust modules
- Java classes → Rust structs/traits
- Java services → Rust services
- Database entities → SeaORM entities

## Usage

To use a module, import it in your Rust file:

```rust
use crate::player::Player;
use crate::services::ItemService;
use crate::models::Item;
use crate::data::DataGame;
```
