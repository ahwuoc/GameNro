# Requirements Document

## Introduction

Migrate hệ thống Change Map từ Java sang Rust cho game server. Hệ thống này xử lý việc di chuyển player giữa các map, zone, và các loại chuyển map đặc biệt (tàu vũ trụ, waypoint, capsule...).

## Glossary

- **ChangeMapService**: Service xử lý logic chuyển map cho player
- **Zone**: Một khu vực trong map, mỗi map có nhiều zone
- **Waypoint**: Điểm chuyển map tự động khi player đi đến vị trí nhất định
- **SpaceShip**: Tàu vũ trụ - phương tiện chuyển map với animation
- **Capsule**: Item cho phép chuyển map nhanh đến các địa điểm đã lưu
- **MapInfo**: Message gửi thông tin map cho client khi player vào map mới

## Requirements

### Requirement 1

**User Story:** As a player, I want to change zones within the same map, so that I can find less crowded areas or join friends.

#### Acceptance Criteria

1. WHEN a player requests zone list (CMD 29) THEN the ChangeMapService SHALL send zone info including zone_id, player count, and max capacity
2. WHEN a player selects a zone (CMD 21) THEN the ChangeMapService SHALL validate zone availability and move player if zone is not full
3. WHEN a player attempts to change zone within 5 seconds of last change THEN the ChangeMapService SHALL reject the request with cooldown message
4. WHEN a player is in a special map (offline/dungeon) THEN the ChangeMapService SHALL prevent zone change

### Requirement 2

**User Story:** As a player, I want to move between maps using waypoints, so that I can explore the game world naturally.

#### Acceptance Criteria

1. WHEN a player reaches a waypoint position (CMD -33/-23) THEN the ChangeMapService SHALL detect waypoint and initiate map change
2. WHEN a waypoint leads to a restricted map THEN the ChangeMapService SHALL check player task progress and deny if requirements not met
3. WHEN waypoint change succeeds THEN the ChangeMapService SHALL update player position to waypoint destination coordinates
4. WHEN waypoint change fails THEN the ChangeMapService SHALL reset player position and send error message

### Requirement 3

**User Story:** As a player, I want to travel using spaceship, so that I can return home or travel to distant planets.

#### Acceptance Criteria

1. WHEN a player requests home return (CMD -15) THEN the ChangeMapService SHALL calculate home map based on player gender and initiate spaceship travel
2. WHEN spaceship travel starts THEN the ChangeMapService SHALL send spaceship animation effect to all players in zone
3. WHEN spaceship travel completes THEN the ChangeMapService SHALL heal player if using tennis spaceship
4. WHEN a player dies and uses spaceship THEN the ChangeMapService SHALL revive player with appropriate HP

### Requirement 4

**User Story:** As a player, I want to use capsule for quick travel, so that I can return to saved locations instantly.

#### Acceptance Criteria

1. WHEN a player opens capsule menu (CMD -91 with CHANGE_CAPSULE) THEN the ChangeMapService SHALL send list of available destinations
2. WHEN a player selects capsule destination THEN the ChangeMapService SHALL validate and teleport player to selected zone
3. WHEN capsule destination is "return to previous" THEN the ChangeMapService SHALL use saved mapBeforeCapsule location

### Requirement 5

**User Story:** As a player, I want the map to load properly after changing, so that I can see other players and interact with the environment.

#### Acceptance Criteria

1. WHEN map change completes THEN the ChangeMapService SHALL send map info message (-24) to player
2. WHEN player finishes loading map (CMD -39) THEN the ChangeMapService SHALL load other players to client and vice versa
3. WHEN player enters map THEN the ChangeMapService SHALL send all active effects (shield, stun, etc.) to player
4. WHEN player enters special map (Cold planet) THEN the ChangeMapService SHALL apply stat modifiers and notify player

### Requirement 6

**User Story:** As a developer, I want map change messages to be serialized correctly, so that the client can parse them properly.

#### Acceptance Criteria

1. WHEN serializing zone list message THEN the ChangeMapService SHALL write zone_id as byte, player count as byte, max capacity as byte
2. WHEN serializing map info message THEN the ChangeMapService SHALL include map_id, zone_id, player position, and map dimensions
3. WHEN deserializing change zone request THEN the ChangeMapService SHALL read zone_id as byte from message
4. WHEN serializing spaceship effect THEN the ChangeMapService SHALL write player_id as int and spaceship_type as byte
5. WHEN serializing then deserializing any map change message THEN the ChangeMapService SHALL produce equivalent data (round-trip)

### Requirement 7

**User Story:** As a system, I want to validate map access based on player progress, so that game progression is enforced.

#### Acceptance Criteria

1. WHEN a player attempts to enter a map THEN the ChangeMapService SHALL check task requirements for that map
2. WHEN a player lacks required task progress THEN the ChangeMapService SHALL deny access and return null zone
3. WHEN a player attempts to enter gender-restricted map THEN the ChangeMapService SHALL validate player gender matches map requirement
4. WHEN an admin player attempts map change THEN the ChangeMapService SHALL bypass all restrictions
