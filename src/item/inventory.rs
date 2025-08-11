use std::collections::HashMap;
use crate::item::item::Item;

#[derive(Debug, Clone)]
pub struct Inventory {
    pub gold: i64,
    pub gem: i32,
    pub ruby: i32,
    pub coupon: i32,
    pub event: i32,

    // Items
    pub items_body: Vec<Item>,
    pub items_bag: Vec<Item>,
    pub items_box: Vec<Item>,
    pub items_box_crack_ball: Vec<Item>,
    pub train_armor: Option<Item>,

    // Gift codes
    pub gift_codes: Vec<String>,

    // Constants
    pub const_limit_gold: i64,
    pub const_max_items_bag: usize,
    pub const_max_items_box: usize,
}

impl Inventory {
    pub fn new() -> Self {
        Self {
            gold: 0,
            gem: 0,
            ruby: 0,
            coupon: 0,
            event: 0,
            items_body: Vec::new(),
            items_bag: Vec::new(),
            items_box: Vec::new(),
            items_box_crack_ball: Vec::new(),
            train_armor: None,
            gift_codes: Vec::new(),
            const_limit_gold: 2000000000000,
            const_max_items_bag: 100,
            const_max_items_box: 100,
        }
    }

    pub fn get_gem_and_ruby(&self) -> i32 {
        self.gem + self.ruby
    }

    pub fn get_gem(&self) -> i32 {
        self.gem
    }

    pub fn get_ruby(&self) -> i32 {
        self.ruby
    }

    pub fn get_gold(&self) -> i64 {
        self.gold
    }

    pub fn add_gold(&mut self, amount: i64) {
        self.gold += amount;
        if self.gold > self.const_limit_gold {
            self.gold = self.const_limit_gold;
        }
    }

    pub fn sub_gold(&mut self, amount: i64) {
        self.gold = (self.gold - amount).max(0);
    }

    pub fn add_gem(&mut self, amount: i32) {
        self.gem += amount;
    }

    pub fn sub_gem(&mut self, amount: i32) {
        self.gem = (self.gem - amount).max(0);
    }

    pub fn add_ruby(&mut self, amount: i32) {
        self.ruby += amount;
    }

    pub fn sub_ruby(&mut self, amount: i32) {
        self.ruby = (self.ruby - amount).max(0);
    }

    pub fn sub_gem_and_ruby(&mut self, amount: i32) {
        self.ruby -= amount;
        if self.ruby < 0 {
            self.gem += self.ruby;
            self.ruby = 0;
        }
    }

    pub fn add_item_bag(&mut self, item: Item) -> bool {
        if self.items_bag.len() < self.const_max_items_bag {
            self.items_bag.push(item);
            true
        } else {
            false
        }
    }

    pub fn add_item_body(&mut self, item: Item) -> bool {
        self.items_body.push(item);
        true
    }

    pub fn add_item_box(&mut self, item: Item) -> bool {
        if self.items_box.len() < self.const_max_items_box {
            self.items_box.push(item);
            true
        } else {
            false
        }
    }

    pub fn remove_item_bag(&mut self, index: usize) -> Option<Item> {
        if index < self.items_bag.len() {
            Some(self.items_bag.remove(index))
        } else {
            None
        }
    }

    pub fn remove_item_body(&mut self, index: usize) -> Option<Item> {
        if index < self.items_body.len() {
            Some(self.items_body.remove(index))
        } else {
            None
        }
    }

    pub fn remove_item_box(&mut self, index: usize) -> Option<Item> {
        if index < self.items_box.len() {
            Some(self.items_box.remove(index))
        } else {
            None
        }
    }

    /// Get item from bag by index
    pub fn get_item_bag(&self, index: usize) -> Option<&Item> {
        self.items_bag.get(index)
    }

    /// Get item from body by index
    pub fn get_item_body(&self, index: usize) -> Option<&Item> {
        self.items_body.get(index)
    }

    /// Get item from box by index
    pub fn get_item_box(&self, index: usize) -> Option<&Item> {
        self.items_box.get(index)
    }

    /// Get item count by template ID from bag
    pub fn get_item_count_by_id(&self, template_id: i32) -> i32 {
        let mut count = 0;
        for item in &self.items_bag {
            if item.is_not_null_item() {
                if let Some(item_template_id) = item.get_template_id() {
                    if item_template_id == template_id {
                        count += item.quantity;
                    }
                }
            }
        }
        count
    }

    /// Subtract item quantity by template ID
    pub fn sub_quantity_item_by_id(&mut self, template_id: i32, quantity: i32) -> bool {
        let mut remaining = quantity;
        let mut i = 0;
        
        while i < self.items_bag.len() && remaining > 0 {
            let item = &mut self.items_bag[i];
            if item.is_not_null_item() {
                if let Some(item_template_id) = item.get_template_id() {
                    if item_template_id == template_id {
                        if remaining >= item.quantity {
                            remaining -= item.quantity;
                            self.items_bag.remove(i);
                        } else {
                            item.quantity -= remaining;
                            remaining = 0;
                        }
                    } else {
                        i += 1;
                    }
                } else {
                    i += 1;
                }
            } else {
                i += 1;
            }
        }
        
        remaining == 0
    }

    /// Check if bag is full
    pub fn is_bag_full(&self) -> bool {
        self.items_bag.len() >= self.const_max_items_bag
    }

    /// Check if box is full
    pub fn is_box_full(&self) -> bool {
        self.items_box.len() >= self.const_max_items_box
    }

    /// Get bag item count
    pub fn get_bag_item_count(&self) -> usize {
        self.items_bag.len()
    }

    /// Get body item count
    pub fn get_body_item_count(&self) -> usize {
        self.items_body.len()
    }

    /// Get box item count
    pub fn get_box_item_count(&self) -> usize {
        self.items_box.len()
    }

    /// Add gift code
    pub fn add_gift_code(&mut self, code: String) {
        self.gift_codes.push(code);
    }

    /// Check if has gift code
    pub fn has_gift_code(&self, code: &str) -> bool {
        self.gift_codes.contains(&code.to_string())
    }

    /// Clear all items
    pub fn clear_all_items(&mut self) {
        self.items_body.clear();
        self.items_bag.clear();
        self.items_box.clear();
        self.items_box_crack_ball.clear();
        self.train_armor = None;
    }

    pub fn dispose(&mut self) {
        self.clear_all_items();
        self.gift_codes.clear();
    }
}

impl Default for Inventory {
    fn default() -> Self {
        Self::new()
    }
}
