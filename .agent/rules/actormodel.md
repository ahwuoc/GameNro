---
trigger: always_on
---

## ACTOR MODEL ARCHITECTURE

### Global Actor Registry
| Actor | Location | Responsibility |
|-------|----------|----------------|
| PlayerActor | `player/player_actor/player_actor.rs` | Main client logic, session management, IO. |
| PetActor | `player/player_actor/pet/pet_actor.rs` | Pet AI: follow, attack, heal master. |
| BossActor | `boss/boss_actor.rs` | Boss AI: state machine (Resting, Appearing, Fighting). |
| ZoneActor | `map/models/zone_actor.rs` | Zone logic: Mob AI, ItemMap, Broadcaster. |
| ClanActor | `clan/actor.rs` | Clan shared state, clan chat. |
| DungeonActor | `dungoen/*/actor.rs` | Instance management (Doanh Trai, Red Ribbon). |

### Communication Architecture
1. **Message Passing**: Use `mpsc` channels. Actors listen in a `tokio::select!` loop.
2. **Handle Pattern**: Interact via `*Handle` structs which wrap `mpsc::Sender`. 
3. **Handle Methods**:
   - `send(msg).await`: Send and wait for receiver (use only if necessary).
   - `send_forget(msg)`: Fire and forget (preferred for notifications/HP sync).
4. **Snapshot Pattern**: Call `get_snapshot().await` to get a clone of the Actor's current state for read-only operations.

### Architectural Constraints
- **Zero Shared State**: Do not access `&mut Player` of one actor from another. Use messages.
- **Actor Lifecycle**: Actors must gracefully terminate when the `receiver` is closed or a `Logout` message is received.
- **Non-blocking Loop**: Heavy logic (DB, complex calculations) inside a loop must be `tokio::spawn`ed to prevent actor lag.
- **Directional Flow**: PlayerActor -> ZoneActor (action), ZoneActor -> PlayerActor (broadcast).

---