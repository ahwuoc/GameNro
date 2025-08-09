# IntrinsicService

## Overview
The `IntrinsicService` is a Rust implementation of the Java `IntrinsicService` that manages player intrinsic abilities in the GameNro game. Intrinsics are passive skills that provide special support abilities to players.

## Features

### Core Functionality
- **Load intrinsic templates** from database through Manager
- **Filter intrinsics by gender** (male, female, neutral)
- **Get intrinsic by ID** for specific lookups
- **Send intrinsic information** to clients via network messages
- **Show all intrinsics** for a specific gender
- **Open intrinsics** using gold or VIP (gems)
- **Random parameter generation** for intrinsic abilities

### Cost System
- **Gold-based opening**: Progressive cost system [10, 20, 40, 80, 160, 320, 640, 1280] Tr vàng
- **VIP opening**: Fixed cost of 100 gems, resets gold cost counter
- **Power requirement**: Minimum 10 billion power required

## Structure

### Models
- `Intrinsic`: Represents an intrinsic ability with parameters and metadata
- `IntrinsicPlayer`: Player's intrinsic data including current intrinsic and open count

### Service Methods

#### Data Retrieval
```rust
// Get all intrinsics for a specific gender
let intrinsics = intrinsic_service.get_intrinsics(player_gender);

// Get specific intrinsic by ID
let intrinsic = intrinsic_service.get_intrinsic_by_id(id);
```

#### Network Communication
```rust
// Send intrinsic info to client
intrinsic_service.send_info_intrinsic(session, player_intrinsic).await?;

// Show all intrinsics for gender
intrinsic_service.show_all_intrinsic(session, player_gender).await?;
```

#### Intrinsic Operations
```rust
// Open intrinsic with gold
let result = intrinsic_service.open(
    &mut player_intrinsic,
    player_gender,
    player_power,
    player_gold
);

// Open intrinsic with VIP (gems)
let result = intrinsic_service.open_vip(
    &mut player_intrinsic,
    player_gender,
    player_power,
    player_gems
);
```

## Database Integration

The service loads intrinsic templates from the database through the `Manager`:

```rust
// In Manager::load_intrinsic_templates()
let intrinsic_templates = intrinsic::Entity::find().all(&database.connection).await?;
```

## Usage Example

```rust
use crate::services::IntrinsicService;
use crate::models::IntrinsicPlayer;

let intrinsic_service = IntrinsicService;
let mut player_intrinsic = IntrinsicPlayer::new();

// Open intrinsic with gold
match intrinsic_service.open(
    &mut player_intrinsic,
    0, // male gender
    15_000_000_000, // 15 billion power
    50_000_000 // 50 million gold
) {
    Ok(message) => println!("Success: {}", message),
    Err(error) => println!("Error: {}", error),
}
```

## Migration from Java

This service mirrors the Java `IntrinsicService` with the following adaptations:

- **Singleton pattern**: Replaced with Rust's module-level static service
- **Exception handling**: Converted to Rust's `Result<T, E>` pattern
- **Database access**: Uses SeaORM instead of direct JDBC
- **Thread safety**: Uses Rust's ownership and borrowing rules
- **Async/await**: Network operations are async for better performance

## Dependencies

- `rand`: For random parameter generation
- `sea-orm`: For database entity access
- `tokio`: For async runtime support
