# Design Document: Change Map Migration

## Overview

Migrate hệ thống Change Map từ Java sang Rust, bao gồm các chức năng:
- Đổi zone trong cùng map
- Di chuyển qua waypoint
- Di chuyển bằng tàu vũ trụ (spaceship)
- Di chuyển nhanh bằng capsule
- Load map và đồng bộ players
- Kiểm tra quyền truy cập map

## Architecture

```mermaid
graph TB
    subgraph Network Layer
        Controller[AsyncController]
        Message[Message]
    end
    
    subgraph Change Map Service
        CMS[ChangeMapService]
        ZoneUI[Zone UI Handler]
        WaypointHandler[Waypoint Handler]
        SpaceshipHandler[Spaceship Handler]
        CapsuleHandler[Capsule Handler]
        MapLoader[Map Loader]
    end
    
    subgraph Map Management
        MapService[MapService]
        ZoneManager[ZoneManager]
        Zone[Zone]
    end
    
    subgraph Player
        Player[Player]
        Location[Location]
    end
    
    Controller --> CMS
    CMS --> ZoneUI
    CMS --> WaypointHandler
    CMS --> SpaceshipHandler
    CMS --> CapsuleHandler
    CMS --> MapLoader
    
    CMS --> MapService
    CMS --> ZoneManager
    MapService --> Zone
    ZoneManager --> Zone
    
    CMS --> Player
    Player --> Location
```

## Components and Interfaces

### 1. ChangeMapService

```rust
pub struct ChangeMapService;

impl ChangeMapService {
    // Zone operations
    pub async fn open_zone_ui(session: &mut AsyncSession) -> Result<()>;
    pub async fn change_zone(session: &mut AsyncSession, zone_id: i32) -> Result<()>;
    
    // Waypoint operations
    pub async fn change_map_waypoint(session: &mut AsyncSession) -> Result<()>;
    
    // Spaceship operations
    pub async fn change_map_by_spaceship(
        session: &mut AsyncSession, 
        map_id: i32, 
        zone_id: i32, 
        x: i16
    ) -> Result<()>;
    pub async fn go_home(session: &mut AsyncSession) -> Result<()>;
    
    // Capsule operations
    pub async fn open_capsule_menu(session: &mut AsyncSession) -> Result<()>;
    pub async fn change_map_capsule(session: &mut AsyncSession, index: i32) -> Result<()>;
    
    // Core map change
    pub async fn change_map(
        session: &mut AsyncSession,
        zone: &Zone,
        x: i16,
        y: i16,
        space_type: SpaceShipType
    ) -> Result<()>;
    
    // Map loading
    pub async fn finish_load_map(session: &mut AsyncSession) -> Result<()>;
    
    // Validation
    pub fn check_map_can_join(player: &Player, zone: &Zone) -> Option<&Zone>;
    pub fn check_task_requirement(player: &Player, map_id: i32) -> bool;
}
```

### 2. Message Handlers (CMD constants)

```rust
pub mod cmd {
    pub const OPEN_ZONE_UI: i8 = 29;
    pub const CHANGE_ZONE: i8 = 21;
    pub const CHANGE_MAP_WAYPOINT: i8 = -33;
    pub const CHANGE_MAP_WAYPOINT_ALT: i8 = -23;
    pub const FINISH_LOAD_MAP: i8 = -39;
    pub const GO_HOME: i8 = -15;
    pub const EFFECT_CHANGE_MAP: i8 = -105;
    pub const CAPSULE_MENU: i8 = -91;
    pub const MAP_INFO: i8 = -24;
    pub const PLAYER_LEAVE: i8 = -6;
    pub const SPACESHIP_ARRIVE: i8 = -65;
}
```

### 3. SpaceShipType Enum

```rust
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum SpaceShipType {
    Auto = -1,
    None = 0,
    Default = 1,
    TeleportYardrat = 2,
    Tennis = 3,
}
```

### 4. ChangeMapType Enum

```rust
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum ChangeMapType {
    Capsule = 0,
    BlackBall = 1,
    MaBu = 2,
}
```

## Data Models

### ZoneInfo (for CMD 29 response)

```rust
pub struct ZoneInfo {
    pub zone_id: i8,
    pub status: i8,        // 0: empty, 1: medium, 2: full
    pub player_count: i8,
    pub max_capacity: i8,
    pub is_competing: bool,
    pub rank_name_1: Option<String>,
    pub rank_1: Option<i32>,
    pub rank_name_2: Option<String>,
    pub rank_2: Option<i32>,
}
```

### MapChangeRequest

```rust
pub struct MapChangeRequest {
    pub target_map_id: i32,
    pub target_zone_id: i32,
    pub target_x: i16,
    pub target_y: i16,
    pub space_type: SpaceShipType,
}
```

### MapChangeResult

```rust
pub enum MapChangeResult {
    Success { zone: Zone, x: i16, y: i16 },
    Cooldown { remaining_seconds: i32 },
    ZoneFull,
    TaskRequired { task_id: i32 },
    GenderRestricted,
    SpecialMapBlocked,
    InvalidZone,
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, the following properties have been identified after removing redundancies:

### Property 1: Zone change respects capacity
*For any* zone with player_count >= max_capacity, attempting to join that zone SHALL return ZoneFull result.
**Validates: Requirements 1.2**

### Property 2: Zone change cooldown enforcement
*For any* player with last_zone_change_time within 5 seconds, attempting zone change SHALL return Cooldown result with correct remaining time.
**Validates: Requirements 1.3**

### Property 3: Special map zone change blocking
*For any* player in an offline or dungeon map, attempting zone change SHALL return SpecialMapBlocked result.
**Validates: Requirements 1.4**

### Property 4: Waypoint position update
*For any* successful waypoint change, player position SHALL equal waypoint destination coordinates (go_x, go_y).
**Validates: Requirements 2.3**

### Property 5: Home map calculation by gender
*For any* player with gender G, home map SHALL be calculated as (G + 21) for normal maps, or 114 for MaBu maps.
**Validates: Requirements 3.1**

### Property 6: Tennis spaceship healing
*For any* player using tennis spaceship, HP and MP SHALL be restored to max values after travel completes.
**Validates: Requirements 3.3, 3.4**

### Property 7: Task requirement validation
*For any* map with task requirement T, player with task progress < T SHALL be denied access (return null zone).
**Validates: Requirements 7.1, 7.2**

### Property 8: Gender restriction validation
*For any* gender-restricted map, player with non-matching gender SHALL be denied access.
**Validates: Requirements 7.3**

### Property 9: Admin bypass all restrictions
*For any* admin player, all map access checks SHALL return success regardless of task progress or gender.
**Validates: Requirements 7.4**

### Property 10: Message round-trip consistency
*For any* ZoneInfo, serializing to Message then deserializing SHALL produce equivalent ZoneInfo.
**Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5**

## Error Handling

| Error Case | Response |
|------------|----------|
| Zone full | Send "Khu vực này đã đầy" message |
| Cooldown active | Send "Chưa thể chuyển khu vực lúc này vui lòng chờ X nữa" |
| Task not met | Send "Bạn chưa thể đến khu vực này" |
| Special map blocked | Send "Không thể đổi khu vực trong map này" |
| Invalid zone | Reset player position, send error |

## Testing Strategy

### Property-Based Testing

Sử dụng thư viện `proptest` cho Rust để implement property-based tests.

Cấu hình:
- Minimum 100 iterations per property test
- Custom generators cho Player, Zone, MapChangeRequest

Mỗi property test PHẢI được annotate với format:
```rust
// **Feature: change-map-migration, Property {number}: {property_text}**
```

### Unit Tests

Unit tests sẽ cover:
- Message serialization/deserialization cho từng CMD
- Edge cases: empty zone list, max capacity zones
- Integration với MapService và ZoneManager

### Test Structure

```
src/map/
├── change_map_service.rs
├── change_map_service_test.rs  // Unit tests
└── change_map_service_props.rs // Property-based tests
```
