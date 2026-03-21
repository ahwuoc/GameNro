---
description: How to add a new network feature or packet handler
---

# Workflow: Adding a New Packet Handler

Follow these steps to implement a new feature triggered by the client.

## Step 1: Define the Command ID
Open `src/constant/cmd.rs` and add a new constant under the appropriate section.
```rust
pub const MY_NEW_ACTION: i8 = 99; 
```

## Step 2: Define the Actor Message (If needed)
If the action affects the player's state, open `src/player/player_actor/message.rs` and add a variant to `PlayerMessage`.
```rust
pub enum PlayerMessage {
    // ...
    MyNewAction { param1: i32 },
}
```

## Step 3: Add Routing Logic
Open `src/network/controller.rs` and add a new case in the `match msg.command` block.

```rust
cmd::MY_NEW_ACTION => {
    let param1 = msg.read_int()?; // Read data from client
    if let Some(handle) = session.get_player_handle().await {
        handle.send_forget(PlayerMessage::MyNewAction { param1 });
    }
    Ok(())
}
```

## Step 4: Implement Logic in Actor
Open `src/player/player_actor/player_actor.rs` (or the relevant handler) and process the new message.

```rust
// Inside the select! loop of PlayerActor
PlayerMessage::MyNewAction { param1 } => {
    self.player.do_something(param1);
    // Send response back to client if needed
    let mut res = Message::new(cmd::MY_RESPONSE);
    res.write_utf("Success")?;
    self.session.transmit(res);
}
```

## Step 5: Verification
1. Run `cargo check` to ensure no type mismatches.
2. Trace the path from `Controller` to `Actor` to ensure no deadlocks (Don't call `PLAYER_MANAGER` inside this flow).
