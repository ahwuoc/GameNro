# Skill: Packet Handling Protocol

## 1. Core Architecture
The communication between Client and Server is based on a **Command-Payload** system.

- **Packet Definition**: `src/network/message.rs` (Struct `Message`).
- **Command Constants**: `src/constant/cmd.rs`.
- **Packet Router**: `src/network/controller.rs` (Function `AsyncController::process`).
- **Internal Actor Message**: `src/player/player_actor/message.rs` (Enum `PlayerMessage`).

## 2. Packet Flow (Incoming)
1. **Network Thread**: Receives bytes, creates `Message`.
2. **Controller**: `AsyncController::process` switches on `msg.command`.
3. **Dispatch**:
   - For **Sync** actions (DB, purely static): Handle directly in `Controller`.
   - For **Async/State** actions: Send a `PlayerMessage` to `PlayerHandle`.
4. **Actor Loop**: `PlayerActor` receives `PlayerMessage` and modifies internal state.

## 3. Packet Flow (Outgoing)
- Use `session.transmit(Message)` to send bytes back to the specific client.
- Use `ServiceHandles` to broadcast packets to multiple players in a zone.

## 4. Safety Rules
- **Never block** the `AsyncController` with long DB queries; always use `tokio::spawn` or Actor handles.
- **Always check session state**: Use `session.get_player_snapshot()` to verify if the player is in the correct state (e.g., logged in) before processing game-logic packets.
- **Read data in order**: Follow the Client-side writing order exactly (Byte -> Short -> Int -> UTF).
