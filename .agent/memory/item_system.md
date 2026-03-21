# Skill: Item & Inventory System

## 1. Item Architecture
- **Item Struct**: `src/item/item.rs`.
- **Item Option**: `src/item/item_option.rs`. Each item has a `Vec<ItemOption>` (ID + Param).
- **Templates**: Real data (Name, Icon, Type) is stored in `ItemTemplate` (DB) and managed via `item_template_manager`.

## 2. Inventory Structure
Located in `Player.inventory` (`src/item/inventory.rs`):
- `items_body`: Equipped items (Body slots).
- `items_bag`: Main inventory slots.
- `items_box`: Storage slots.
- Currencies: `gold`, `gem` (Diamond), `ruby` (Lock Gem).

## 3. Basic Operations (InventoryService)
Located in `src/item/inventory_service.rs`:

### Adding Items
```rust
// Safely adds an item to bag, handling stacking and notifying client
InventoryService::add_item_bag(player, item)?;
```
- **Currency Items**: Types 9 (Gold), 10 (Gem), 34 (Ruby) are automatically converted to currency amounts.
- **Stacking**: Items with `is_up_to == 1` will stack up to `MAX_ITEM_STACK_SIZE`.

### Removing/Checking Items
- `count_item_bag_with_id(player, id)`: Returns total quantity of an item template.
- `sub_item_bag_with_id(player, id, quantity)`: Removes items by template ID.
- `sub_quantity_item_bag(player, index, quantity)`: Removes items by slot index.

## 4. Item Slot Mapping (Body)
- Slot 0-5: Standard equipment (Head, Body, Leg, etc.).
- Slot 6: Type 32.
- Slot 7: Type 23, 24.

## 5. Persistence Workflow
- Items are stored as JSON blobs in the `players` table (handled by `player_mapper` and `player_parser`).
- **Safety**: When modifying items inside a `modify_player` closure, ensure you don't call broadcasting functions (Deadlock Rule). Use `ServiceHandles` after the lock is released or via the Actor's message loop.
