use std::collections::HashMap;
use chrono::{DateTime, Utc};
use crate::item::item::Item;
use crate::item::item_option::ItemOption;
use crate::entities::item_template::Model as ItemTemplate;

/// ItemMap represents an item dropped on the map
#[derive(Debug, Clone)]
pub struct ItemMap {
    pub item_map_id: i32,
    pub item_template: Option<ItemTemplate>,
    pub quantity: i32,
    pub x: i32,
    pub y: i32,
    pub player_id: i64,
    pub options: Vec<ItemOption>,
    pub create_time: DateTime<Utc>,
    pub clan_id: i32,
    pub is_black_ball: bool,
    pub is_namec_ball: bool,
    pub last_time_move_to_player: DateTime<Utc>,
}

impl ItemMap {
    /// Create new ItemMap
    pub fn new(
        item_map_id: i32,
        template: Option<ItemTemplate>,
        quantity: i32,
        x: i32,
        y: i32,
        player_id: i64,
    ) -> Self {
        let current_time = Utc::now();
        let is_black_ball = if let Some(ref template) = template {
            Self::is_black_ball(template.id)
        } else {
            false
        };
        
        let is_namec_ball = if let Some(ref template) = template {
            Self::is_namec_ball(template.id)
        } else {
            false
        };

        Self {
            item_map_id,
            item_template: template,
            quantity,
            x,
            y,
            player_id: if player_id != -1 { player_id.abs() } else { player_id },
            options: Vec::new(),
            create_time: current_time,
            clan_id: -1,
            is_black_ball,
            is_namec_ball,
            last_time_move_to_player: current_time,
        }
    }

    /// Check if item is not null
    pub fn is_not_null_item(&self) -> bool {
        self.item_template.is_some()
    }

    /// Check if item is null
    pub fn is_null_item(&self) -> bool {
        self.item_template.is_none()
    }

    /// Get item name
    pub fn get_name(&self) -> String {
        if let Some(ref template) = self.item_template {
            template.name.clone()
        } else {
            String::new()
        }
    }

    /// Get item template ID
    pub fn get_template_id(&self) -> Option<i32> {
        self.item_template.as_ref().map(|t| t.id)
    }

    /// Get item type
    pub fn get_type(&self) -> Option<i32> {
        self.item_template.as_ref().map(|t| t.r#type as i32)
    }

    /// Update item map
    pub fn update(&mut self) {
        // Update logic for special items
        if self.is_black_ball {
            // Black ball moves towards player
            self.update_black_ball();
        }
        
        // Check if item should disappear
        self.check_disappear();
        
        // Update last move time
        self.last_time_move_to_player = Utc::now();
    }

    /// Update black ball movement
    fn update_black_ball(&mut self) {
        // TODO: Implement black ball movement logic
        // This would move the ball towards the nearest player
    }

    /// Check if item should disappear
    fn check_disappear(&mut self) {
        let current_time = Utc::now();
        let time_since_create = current_time.signed_duration_since(self.create_time).num_milliseconds();
        
        // Items disappear after certain time
        if time_since_create > 50000 && self.is_not_null_item() {
            // TODO: Implement item removal logic
        }
        
        // Remove player ownership after 15 seconds
        if time_since_create > 15000 {
            self.player_id = -1;
        }
    }

    /// Check if item is black ball
    pub fn is_black_ball(template_id: i32) -> bool {
        template_id >= 372 && template_id <= 378
    }

    /// Check if item is namec ball
    pub fn is_namec_ball(template_id: i32) -> bool {
        template_id >= 353 && template_id <= 360
    }

    /// Get item info
    pub fn get_info(&self) -> String {
        if let Some(ref template) = self.item_template {
            format!("{} x{}", template.name, self.quantity)
        } else {
            "Empty Item".to_string()
        }
    }

    /// Get item content
    pub fn get_content(&self) -> String {
        // TODO: Implement content generation
        String::new()
    }

    /// Add option to item
    pub fn add_option(&mut self, option: ItemOption) {
        self.options.push(option);
    }

    /// Get option parameter
    pub fn get_option_param(&self, option_id: i32) -> i32 {
        for option in &self.options {
            if option.get_option_id() == option_id {
                return option.get_param();
            }
        }
        0
    }

    /// Check if item has option
    pub fn has_option(&self, option_id: i32) -> bool {
        for option in &self.options {
            if option.get_option_id() == option_id {
                return true;
            }
        }
        false
    }

    /// Clone item map
    pub fn clone(&self) -> Self {
        Self {
            item_map_id: self.item_map_id,
            item_template: self.item_template.clone(),
            quantity: self.quantity,
            x: self.x,
            y: self.y,
            player_id: self.player_id,
            options: self.options.clone(),
            create_time: Utc::now(),
            clan_id: self.clan_id,
            is_black_ball: self.is_black_ball,
            is_namec_ball: self.is_namec_ball,
            last_time_move_to_player: self.last_time_move_to_player,
        }
    }
}

/// ItemMapService manages item maps on the map
pub struct ItemMapService {
    item_maps: HashMap<i32, ItemMap>,
    next_item_map_id: i32,
}

impl ItemMapService {
    pub fn new() -> Self {
        Self {
            item_maps: HashMap::new(),
            next_item_map_id: 1,
        }
    }

    /// Get singleton instance
    pub fn get_instance() -> &'static mut ItemMapService {
        static mut INSTANCE: Option<ItemMapService> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(ItemMapService::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    /// Create item map
    pub fn create_item_map(
        &mut self,
        template: Option<ItemTemplate>,
        quantity: i32,
        x: i32,
        y: i32,
        player_id: i64,
    ) -> ItemMap {
        let item_map = ItemMap::new(
            self.next_item_map_id,
            template,
            quantity,
            x,
            y,
            player_id,
        );
        
        self.item_maps.insert(self.next_item_map_id, item_map.clone());
        self.next_item_map_id += 1;
        
        // Reset ID counter if it gets too large
        if self.next_item_map_id >= 2000000000 {
            self.next_item_map_id = 1;
        }
        
        item_map
    }

    /// Get item map by ID
    pub fn get_item_map(&self, item_map_id: i32) -> Option<&ItemMap> {
        self.item_maps.get(&item_map_id)
    }

    /// Remove item map
    pub fn remove_item_map(&mut self, item_map_id: i32) -> bool {
        self.item_maps.remove(&item_map_id).is_some()
    }

    /// Get all item maps
    pub fn get_all_item_maps(&self) -> &HashMap<i32, ItemMap> {
        &self.item_maps
    }

    /// Update all item maps
    pub fn update_all_item_maps(&mut self) {
        for item_map in self.item_maps.values_mut() {
            item_map.update();
        }
    }

    /// Check if item is black ball
    pub fn is_black_ball(&self, template_id: i32) -> bool {
        ItemMap::is_black_ball(template_id)
    }

    /// Check if item is namec ball
    pub fn is_namec_ball(&self, template_id: i32) -> bool {
        ItemMap::is_namec_ball(template_id)
    }

    /// Get item maps in area
    pub fn get_item_maps_in_area(&self, x: i32, y: i32, radius: i32) -> Vec<&ItemMap> {
        let mut items = Vec::new();
        
        for item_map in self.item_maps.values() {
            let distance = ((item_map.x - x).pow(2) + (item_map.y - y).pow(2)) as f64;
            if distance.sqrt() <= radius as f64 {
                items.push(item_map);
            }
        }
        
        items
    }

    /// Get item map count
    pub fn get_item_map_count(&self) -> usize {
        self.item_maps.len()
    }

    /// Clear all item maps
    pub fn clear_all_item_maps(&mut self) {
        self.item_maps.clear();
    }
}
