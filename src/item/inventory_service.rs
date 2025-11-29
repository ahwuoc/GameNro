use crate::entities::player;
use crate::item;
use crate::item::inventory::Inventory;
use crate::item::item::Item;
use crate::network::message::Message;
use crate::player::Player;
use std::sync::Mutex;

use once_cell::sync::Lazy;

static INVENTORY_SERVICE: Lazy<Mutex<InventoryService>> =
    Lazy::new(|| Mutex::new(InventoryService));
pub struct InventoryService;

impl InventoryService {
    pub fn get_instance() -> std::sync::MutexGuard<'static, InventoryService> {
        INVENTORY_SERVICE.lock().unwrap()
    }

    pub fn add_item_to_bag(&self, inventory: &mut Inventory, item: Item) -> bool {
        inventory.add_item_bag(item)
    }

    pub fn add_item_to_body(&self, inventory: &mut Inventory, item: Item) -> bool {
        inventory.add_item_body(item)
    }
    pub fn add_item_to_box(&self, inventory: &mut Inventory, item: Item) -> bool {
        inventory.add_item_box(item)
    }
    pub fn remove_item_from_bag(&self, inventory: &mut Inventory, index: usize) -> Option<Item> {
        inventory.remove_item_bag(index)
    }

    /// Remove item from body by index
    pub fn remove_item_from_body(&self, inventory: &mut Inventory, index: usize) -> Option<Item> {
        inventory.remove_item_body(index)
    }

    /// Remove item from box by index
    pub fn remove_item_from_box(&self, inventory: &mut Inventory, index: usize) -> Option<Item> {
        inventory.remove_item_box(index)
    }

    /// Get item from bag by index
    pub fn get_item_from_bag<'a>(
        &self,
        inventory: &'a Inventory,
        index: usize,
    ) -> Option<&'a Item> {
        inventory.get_item_bag(index)
    }

    /// Get item from body by index
    pub fn get_item_from_body<'a>(
        &self,
        inventory: &'a Inventory,
        index: usize,
    ) -> Option<&'a Item> {
        inventory.get_item_body(index)
    }

    /// Get item from box by index
    pub fn get_item_from_box<'a>(
        &self,
        inventory: &'a Inventory,
        index: usize,
    ) -> Option<&'a Item> {
        inventory.get_item_box(index)
    }

    /// Get item count by template ID
    pub fn get_item_count_by_id(&self, inventory: &Inventory, template_id: i16) -> i32 {
        inventory.get_item_count_by_id(template_id)
    }

    /// Subtract item quantity by template ID
    pub fn sub_quantity_item_by_id(
        &self,
        inventory: &mut Inventory,
        template_id: i16,
        quantity: i32,
    ) -> bool {
        inventory.sub_quantity_item_by_id(template_id, quantity)
    }

    /// Add gold to inventory
    pub fn add_gold(&self, inventory: &mut Inventory, amount: i64) {
        inventory.add_gold(amount);
    }

    /// Subtract gold from inventory
    pub fn sub_gold(&self, inventory: &mut Inventory, amount: i64) {
        inventory.sub_gold(amount);
    }

    /// Add gem to inventory
    pub fn add_gem(&self, inventory: &mut Inventory, amount: i32) {
        inventory.add_gem(amount);
    }

    /// Subtract gem from inventory
    pub fn sub_gem(&self, inventory: &mut Inventory, amount: i32) {
        inventory.sub_gem(amount);
    }

    /// Add ruby to inventory
    pub fn add_ruby(&self, inventory: &mut Inventory, amount: i32) {
        inventory.add_ruby(amount);
    }

    /// Subtract ruby from inventory
    pub fn sub_ruby(&self, inventory: &mut Inventory, amount: i32) {
        inventory.sub_ruby(amount);
    }

    /// Subtract gem and ruby
    pub fn sub_gem_and_ruby(&self, inventory: &mut Inventory, amount: i32) {
        inventory.sub_gem_and_ruby(amount);
    }

    /// Check if bag is full
    pub fn is_bag_full(&self, inventory: &Inventory) -> bool {
        inventory.is_bag_full()
    }

    /// Check if box is full
    pub fn is_box_full(&self, inventory: &Inventory) -> bool {
        inventory.is_box_full()
    }

    /// Get bag item count
    pub fn get_bag_item_count(&self, inventory: &Inventory) -> usize {
        inventory.get_bag_item_count()
    }

    /// Get body item count
    pub fn get_body_item_count(&self, inventory: &Inventory) -> usize {
        inventory.get_body_item_count()
    }

    /// Get box item count
    pub fn get_box_item_count(&self, inventory: &Inventory) -> usize {
        inventory.get_box_item_count()
    }

    /// Add gift code
    pub fn add_gift_code(&self, inventory: &mut Inventory, code: String) {
        inventory.add_gift_code(code);
    }

    /// Check if has gift code
    pub fn has_gift_code(&self, inventory: &Inventory, code: &str) -> bool {
        inventory.has_gift_code(code)
    }

    /// Clear all items
    pub fn clear_all_items(&self, inventory: &mut Inventory) {
        inventory.clear_all_items();
    }

    /// Find item index in bag
    pub fn find_item_index_in_bag(
        &self,
        inventory: &Inventory,
        target_item: &Item,
    ) -> Option<usize> {
        for (index, item) in inventory.items_bag.iter().enumerate() {
            if item.is_not_null_item() && target_item.is_not_null_item() {
                if let (Some(item_id), Some(target_id)) =
                    (item.get_template_id(), target_item.get_template_id())
                {
                    if item_id == target_id && item.quantity == target_item.quantity {
                        return Some(index);
                    }
                }
            }
        }
        None
    }
    pub fn send_item_bag_to_client(pl: &mut Player) -> anyhow::Result<()> {
        let mut response = Message::new(-36);
        response.write_byte(0);
        response.write_byte(pl.inventory.items_bag.len() as i8);
        for item in &pl.inventory.items_bag {
            if item.is_not_null_item() {
                continue;
            }
            response.write_short(item.get_template_id().unwrap_or(1));
            response.write_int(item.quantity);
            response.write_utf(&item.get_description());
            response.write_utf(&item.get_content())?;
            response.write_byte(item.item_options.len() as i8);
            for option in &item.item_options {
                response.write_byte(option.option_id);
                response.write_short(option.param);
            }
        }
        pl.send_message(response);
        Ok(())
    }
    pub fn send_item_body_to_client(pl: &mut Player) -> anyhow::Result<()> {
        let mut response = Message::new(-37);
        response.write_byte(0);
        response.write_short(pl.get_head());
        response.write_byte(pl.inventory.items_body.len() as i8);
        for item in &pl.inventory.items_body {
            if item.is_not_null_item() {
                continue;
            };
            response.write_short(item.get_template_id().unwrap_or(1));
            response.write_int(item.quantity);
            response.write_utf(&item.get_description());
            response.write_utf(&item.get_content());
            for option in &item.item_options {
                response.write_byte(option.option_id);
                response.write_short(option.param);
            }
        }
        pl.send_message(response);

        Ok(())
    }
}
