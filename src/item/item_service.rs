use std::collections::HashMap;
use crate::item::item::Item;
use crate::item::item_option::ItemOption;
use crate::entities::item_template::Model as ItemTemplate;
use crate::entities::item_option_template::Model as ItemOptionTemplate;

pub struct ItemService {
}

impl ItemService {
    pub fn new() -> Self {
        Self {}
    }

    /// Get item template by ID
    pub fn get_template(&self, id: i32) -> Option<ItemTemplate> {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        guard.item_templates_by_id.get(&id).cloned()
    }

    pub fn get_item_option_template(&self, id: i32) -> Option<ItemOptionTemplate> {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        guard.item_option_templates_by_id.get(&id).cloned()
    }

    pub fn get_item_id_by_icon(&self, icon_id: i32) -> Option<i32> {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        for template in guard.item_templates.iter() {
            if template.icon_id == icon_id {
                return Some(template.id);
            }
        }
        None
    }

    pub fn create_item_null(&self) -> Item {
        Item::new()
    }

    pub fn create_new_item(&self, template_id: i32) -> Option<Item> {
        self.create_new_item_with_quantity(template_id, 1)
    }

    /// Create new item with quantity
    pub fn create_new_item_with_quantity(&self, template_id: i32, quantity: i32) -> Option<Item> {
        if let Some(template) = self.get_template(template_id) {
            Some(Item::with_template(template.clone(), quantity))
        } else {
            println!("Warning: Item template not found for ID: {}", template_id);
            None
        }
    }

    /// Copy item
    pub fn copy_item(&self, item: &Item) -> Item {
        item.clone_item()
    }

    /// Create item with activation set
    pub fn create_item_set_kich_hoat(&self, template_id: i32, quantity: i32) -> Option<Item> {
        if let Some(mut item) = self.create_new_item_with_quantity(template_id, quantity) {
            // Add activation options
            item.set_content(item.get_content());
            item.info = item.get_info();
            Some(item)
        } else {
            None
        }
    }

    /// Create item for destruction
    pub fn create_item_do_huy_diet(&self, template_id: i32, quantity: i32) -> Option<Item> {
        if let Some(mut item) = self.create_new_item_with_quantity(template_id, quantity) {
            // Add destruction options
            item.set_content(item.get_content());
            item.info = item.get_info();
            Some(item)
        } else {
            None
        }
    }

    /// Random SKH (Special Item) ID based on gender
    pub fn random_skh_id(&self, gender: i32) -> i32 {
        let adjusted_gender = if gender == 3 { 2 } else { gender };
        
        let options = vec![
            vec![128, 129, 127], // Male
            vec![130, 131, 132], // Female
            vec![133, 135, 134], // Neutral
        ];
        
        let skh_v1 = 25; // 25% chance
        let skh_v2 = 35; // 35% chance
        let skh_c = 40;  // 40% chance
        
        let random = (std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() % 100) as i32;
        
        let skh_id = if random <= skh_v1 {
            0
        } else if random <= skh_v1 + skh_v2 {
            1
        } else if random <= skh_v1 + skh_v2 + skh_c {
            2
        } else {
            0
        };
        
        if adjusted_gender < options.len() as i32 && skh_id < options[adjusted_gender as usize].len() as i32 {
            options[adjusted_gender as usize][skh_id as usize]
        } else {
            127 // Default
        }
    }

    /// Get option ID for SKH
    pub fn option_id_skh(&self, skh_id: i32) -> i32 {
        // Map SKH ID to option ID
        match skh_id {
            127 => 30, // SKH V1
            128 => 31, // SKH V1
            129 => 32, // SKH V1
            130 => 33, // SKH V2
            131 => 34, // SKH V2
            132 => 35, // SKH V2
            133 => 36, // SKH C
            134 => 37, // SKH C
            135 => 38, // SKH C
            _ => 30,   // Default
        }
    }

    pub fn is_item_activation(&self, _item: &Item) -> bool {
        false
    }

    pub fn get_all_item_templates_count(&self) -> usize {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        guard.item_templates_by_id.len()
    }

    pub fn get_all_item_option_templates_count(&self) -> usize {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        guard.item_option_templates_by_id.len()
    }

    pub fn can_item_stack(&self, template_id: i32, item_type: i32) -> bool {
        // Items that can be stacked
        template_id == 457 || template_id == 590 || template_id == 610 ||
        item_type == 14 || item_type == 933 || item_type == 934 ||
        template_id == 537 || template_id == 538 || item_type == 539 ||
        item_type == 541 || item_type == 542 || template_id == 2069 ||
        item_type == 540 || (template_id >= 1268 && template_id <= 1273)
    }

    /// Get item template count
    pub fn get_item_template_count(&self) -> usize {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        guard.item_templates.len()
    }

    /// Get item option template count
    pub fn get_item_option_template_count(&self) -> usize {
        let manager = crate::services::Manager::get_instance();
        let guard = manager.lock().unwrap();
        guard.item_option_templates.len()
    }
}
