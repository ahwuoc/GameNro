use std::collections::HashMap;
use crate::item::item::Item;
use crate::item::item_option::ItemOption;
use crate::entities::item_template::Model as ItemTemplate;
use crate::map::item_map::ItemMap;



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
        ItemMap::is_black_ball_template(template_id)
    }

    /// Check if item is namec ball
    pub fn is_namec_ball(&self, template_id: i32) -> bool {
        ItemMap::is_namec_ball_template(template_id)
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
