# Skill: Combine System (Item Upgrading)

## 1. Overview
The Combine system manages all item upgrading, crystalization, and special crafting at NPCs (primarily Ba Hat Mit).

## 2. Architecture
- **State**: Stored in `Player.combine_new` (`src/combine/model.rs`). Includes items in the forge, costs (gold/gems), and calculated success rates.
- **Handlers**: Each combine type (e.g., `SaoPhaLe`) implements the `CombineHandler` trait (`src/combine/mod.rs`).
- **Dispatch**: Uses `enum_dispatch` via `CombineType` (`src/combine/combine_type.rs`) to route common operations:
  - `show_info_combine`: Triggered when items are put into the UI.
  - `confirm_combine`: Triggered when the "Confirm" button is pressed.

## 3. Communication Flow
1. **Initiation**: `NPC Handler` (e.g., `src/npc/handlers/bahatmit.rs`) calls `combine_service::open_tab_combine`.
2. **Setup**: Player sends indices of items from bag -> `combine_service::show_info_combine` -> `PlayerActor`.
3. **Execution**: `combine_service::confirm_combine` -> `PlayerActor` -> `handler.confirm_combine()`.
4. **Broadcast**: Results (Success/Failure/New Item) are sent back to the client via `Message` or `ServiceHandles`.

## 4. Key Components
- **`src/combine/handlers/saophale.rs`**: Logic for Crystalizing equipment.
- **`src/combine/combine_constants.rs`**: Logic constant like `OPEN_TAB_COMBINE`, `SHOW_INFO_COMBINE`, `DO_COMBINE`.
- **`src/combine/model.rs`**: Fields like `gold_combine`, `gem_combine`, `ratio_combine` are used to communicate costs/rates to the client.

## 5. Adding New Combine Types
To add a new crafting formula:
1. Create a new handler in `src/combine/handlers/`.
2. Implement `CombineHandler` trait.
3. Add the variant to `CombineType` enum in `src/combine/combine_type.rs`.
4. Register the trigger in the appropriate NPC handler (e.g., Ba Hat Mit).
