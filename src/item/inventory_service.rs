use std::collections::HashMap;
use crate::item::inventory::Inventory;
use crate::item::item::Item;

/// InventoryService manages inventory operations
pub struct InventoryService {
    // Service state
    initialized: bool,
}

impl InventoryService {
    pub fn new() -> Self {
        Self {
            initialized: false,
        }
    }

    pub fn get_instance() -> &'static mut InventoryService {
        static mut INSTANCE: Option<InventoryService> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(InventoryService::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    /// Initialize service
    pub fn init(&mut self) {
        self.initialized = true;
        println!("InventoryService initialized");
    }

    /// Check if service is initialized
    pub fn is_initialized(&self) -> bool {
        self.initialized
    }

    /// Add item to player's bag
    pub fn add_item_to_bag(&self, inventory: &mut Inventory, item: Item) -> bool {
        inventory.add_item_bag(item)
    }

    /// Add item to player's body
    pub fn add_item_to_body(&self, inventory: &mut Inventory, item: Item) -> bool {
        inventory.add_item_body(item)
    }

    /// Add item to player's box
    pub fn add_item_to_box(&self, inventory: &mut Inventory, item: Item) -> bool {
        inventory.add_item_box(item)
    }

    /// Remove item from bag by index
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
    pub fn get_item_from_bag<'a>(&self, inventory: &'a Inventory, index: usize) -> Option<&'a Item> {
        inventory.get_item_bag(index)
    }

    /// Get item from body by index
    pub fn get_item_from_body<'a>(&self, inventory: &'a Inventory, index: usize) -> Option<&'a Item> {
        inventory.get_item_body(index)
    }

    /// Get item from box by index
    pub fn get_item_from_box<'a>(&self, inventory: &'a Inventory, index: usize) -> Option<&'a Item> {
        inventory.get_item_box(index)
    }

    /// Get item count by template ID
    pub fn get_item_count_by_id(&self, inventory: &Inventory, template_id: i32) -> i32 {
        inventory.get_item_count_by_id(template_id)
    }

    /// Subtract item quantity by template ID
    pub fn sub_quantity_item_by_id(&self, inventory: &mut Inventory, template_id: i32, quantity: i32) -> bool {
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

    /// Dispose inventory
    pub fn dispose_inventory(&self, inventory: &mut Inventory) {
        inventory.dispose();
    }

    /// Find item index in bag
    pub fn find_item_index_in_bag(&self, inventory: &Inventory, target_item: &Item) -> Option<usize> {
        for (index, item) in inventory.items_bag.iter().enumerate() {
            if item.is_not_null_item() && target_item.is_not_null_item() {
                if let (Some(item_id), Some(target_id)) = (item.get_template_id(), target_item.get_template_id()) {
                    if item_id == target_id && item.quantity == target_item.quantity {
                        return Some(index);
                    }
                }
            }
        }
        None
    }

    /// Check if inventory has specific item
    pub fn has_item(&self, inventory: &Inventory, template_id: i32) -> bool {
        inventory.get_item_count_by_id(template_id) > 0
    }

    /// Check if inventory has enough items
    pub fn has_enough_items(&self, inventory: &Inventory, template_id: i32, quantity: i32) -> bool {
        inventory.get_item_count_by_id(template_id) >= quantity
    }
}
