# Implementation Plan

## Change Map Migration - Java to Rust

- [x] 1. Define constants and enums for change map system
  - [x] 1.1 Add CMD constants for change map messages (29, 21, -33, -23, -39, -15, -105, -91, -24, -6, -65)
    - Add to `src/constant/cmd.rs`
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  - [x] 1.2 Create SpaceShipType enum (Auto, None, Default, TeleportYardrat, Tennis)
    - Create in `src/map/change_map_service.rs`
    - _Requirements: 3.1, 3.2_
  - [x] 1.3 Create ChangeMapType enum (Capsule, BlackBall, MaBu)
    - _Requirements: 4.1_

- [ ] 2. Implement data models for change map
  - [ ] 2.1 Create ZoneInfo struct with serialization methods
    - Fields: zone_id, status, player_count, max_capacity, is_competing, rank info
    - Implement `to_message()` and `from_message()` methods
    - _Requirements: 6.1, 6.2_
  - [ ]* 2.2 Write property test for ZoneInfo round-trip
    - **Property 10: Message round-trip consistency**
    - **Validates: Requirements 6.1, 6.2, 6.3, 6.4, 6.5**
  - [ ] 2.3 Create MapChangeRequest and MapChangeResult types
    - _Requirements: 1.2, 2.1_

- [ ] 3. Implement zone operations
  - [ ] 3.1 Implement `open_zone_ui` - send zone list to client (CMD 29)
    - Query all zones for current map
    - Calculate player count and status for each zone
    - Serialize and send message
    - _Requirements: 1.1_
  - [ ] 3.2 Implement `change_zone` - handle zone change request (CMD 21)
    - Validate cooldown (5 seconds)
    - Check zone capacity
    - Check special map restrictions
    - Execute zone change if valid
    - _Requirements: 1.2, 1.3, 1.4_
  - [ ]* 3.3 Write property tests for zone operations
    - **Property 1: Zone change respects capacity**
    - **Property 2: Zone change cooldown enforcement**
    - **Property 3: Special map zone change blocking**
    - **Validates: Requirements 1.2, 1.3, 1.4**

- [ ] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Implement waypoint operations
  - [x] 5.1 Implement `change_map_waypoint` - handle waypoint travel (CMD -33/-23)
    - Detect waypoint at player position
    - Validate task requirements for destination map
    - Execute map change or reset position on failure
    - _Requirements: 2.1, 2.2, 2.3, 2.4_
  - [ ]* 5.2 Write property test for waypoint position update
    - **Property 4: Waypoint position update**
    - **Validates: Requirements 2.3**

- [x] 6. Implement spaceship operations
  - [x] 6.1 Implement `go_home` - return to home map (CMD -15)
    - Calculate home map based on gender (gender + 21)
    - Handle MaBu map special case (map 114)
    - Initiate spaceship travel
    - _Requirements: 3.1_
  - [x] 6.2 Implement `change_map_by_spaceship` - spaceship travel with animation
    - Send spaceship animation effect (CMD -65)
    - Handle tennis spaceship healing
    - Handle dead player revival
    - _Requirements: 3.2, 3.3, 3.4_
  - [x] 6.3 Implement `spaceship_arrive` - send spaceship effect to zone
    - Broadcast to all players in zone
    - _Requirements: 3.2_
  - [ ]* 6.4 Write property tests for spaceship operations
    - **Property 5: Home map calculation by gender**
    - **Property 6: Tennis spaceship healing**
    - **Validates: Requirements 3.1, 3.3, 3.4**

- [ ] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 8. Implement map access validation
  - [ ] 8.1 Implement `check_map_can_join` - validate map access
    - Check task requirements based on map_id
    - Check gender restrictions
    - Bypass for admin/boss players
    - _Requirements: 7.1, 7.2, 7.3, 7.4_
  - [x] 8.2 Create task requirement mapping (map_id -> required_task_id)
    - Based on Java ConstTask mappings
    - _Requirements: 7.1_
  - [ ]* 8.3 Write property tests for map access validation
    - **Property 7: Task requirement validation**
    - **Property 8: Gender restriction validation**
    - **Property 9: Admin bypass all restrictions**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4**

- [x] 9. Implement core map change logic
  - [x] 9.1 Implement `change_map` - core map change function
    - Exit current map (send CMD -6 to other players)
    - Update player position and zone
    - Send map info (CMD -24)
    - Handle special map effects (Cold planet stat modifiers)
    - _Requirements: 5.1, 5.4_
  - [x] 9.2 Implement `exit_map` - leave current zone
    - Remove player from zone
    - Broadcast leave message to other players
    - _Requirements: 5.1_
  - [x] 9.3 Implement `go_to_map` - enter new zone
    - Add player to zone
    - Update player zone reference
    - _Requirements: 5.1_

- [x] 10. Implement map loading
  - [x] 10.1 Implement `finish_load_map` - handle client map load complete (CMD -39)
    - Load other players to client
    - Load client to other players
    - Send active effects (shield, stun, etc.)
    - _Requirements: 5.2, 5.3_
  - [x] 10.2 Implement `send_effect_map_to_me` - send zone effects to player
    - Iterate all mobs and players with active effects
    - Send effect messages
    - _Requirements: 5.3_
  - [x] 10.3 Implement `send_effect_me_to_map` - send player effects to zone
    - Send player's active effects to all others in zone
    - _Requirements: 5.3_

- [x] 11. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 12. Implement capsule operations (optional feature)
  - [x] 12.1 Implement `open_capsule_menu` - show capsule destinations (CMD -91)
    - Get available destinations from MapService
    - Include "return to previous" option
    - _Requirements: 4.1_
  - [x] 12.2 Implement `change_map_capsule` - teleport via capsule
    - Validate selected destination
    - Handle "return to previous" case
    - Execute teleport
    - _Requirements: 4.2, 4.3_

- [x] 13. Wire up message handlers in Controller
  - [x] 13.1 Add CMD 29 handler (open_zone_ui)
    - _Requirements: 1.1_
  - [x] 13.2 Add CMD 21 handler (change_zone)
    - _Requirements: 1.2_
  - [x] 13.3 Add CMD -33/-23 handler (change_map_waypoint)
    - _Requirements: 2.1_
  - [x] 13.4 Add CMD -15 handler (go_home)
    - _Requirements: 3.1_
  - [x] 13.5 Add CMD -91 handler (capsule menu)
    - _Requirements: 4.1_
  - [x] 13.6 Update CMD -39 handler (finish_load_map)
    - _Requirements: 5.2_

- [x] 14. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
