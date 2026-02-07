use crate::entities::item_template::Model as ItemTemplate;
use crate::map::item_map::{ItemMap, ItemMapEvent, UpdateResult};
use crate::network::message::Message;
use std::collections::HashMap;

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

    pub fn create_item_map(
        &mut self,
        template: Option<ItemTemplate>,
        quantity: i32,
        x: i32,
        y: i32,
        player_id: i64,
        map_id: i32,
        zone_id: i32,
    ) -> ItemMap {
        let mut item_map = ItemMap::new(self.next_item_map_id, template, quantity, x, y, player_id);
        item_map.set_location(map_id, zone_id, x, y);

        self.item_maps
            .insert(self.next_item_map_id, item_map.clone());
        self.next_item_map_id += 1;
        if self.next_item_map_id >= 2_000_000_000 {
            self.next_item_map_id = 1;
        }

        item_map
    }

    pub fn get_item_map(&self, item_map_id: i32) -> Option<&ItemMap> {
        self.item_maps.get(&item_map_id)
    }

    pub fn get_item_map_mut(&mut self, item_map_id: i32) -> Option<&mut ItemMap> {
        self.item_maps.get_mut(&item_map_id)
    }

    pub fn remove_item_map(&mut self, item_map_id: i32) -> Option<ItemMap> {
        self.item_maps.remove(&item_map_id)
    }

    pub fn get_items_in_zone(&self, map_id: i32, zone_id: i32) -> Vec<&ItemMap> {
        self.item_maps
            .values()
            .filter(|item| item.map_id == map_id && item.zone_id == zone_id)
            .collect()
    }

    pub fn update_zone(&mut self, map_id: i32, zone_id: i32) -> Vec<(i32, UpdateResult)> {
        let mut results = Vec::new();
        let mut to_remove = Vec::new();

        for (id, item_map) in self.item_maps.iter_mut() {
            if item_map.map_id == map_id && item_map.zone_id == zone_id {
                let update_result = item_map.update();
                if update_result.should_remove {
                    to_remove.push(*id);
                }
                results.push((*id, update_result));
            }
        }

        for id in to_remove {
            self.item_maps.remove(&id);
        }

        results
    }

    pub fn pickup_item(&mut self, item_map_id: i32, player_id: u64) -> Option<ItemMap> {
        if let Some(item) = self.item_maps.get_mut(&item_map_id) {
            if item.can_pickup(player_id, None) {
                item.is_picked_up = true;
                return self.item_maps.remove(&item_map_id);
            }
        }
        None
    }

    pub fn get_item_map_count(&self) -> usize {
        self.item_maps.len()
    }

    pub fn clear_zone(&mut self, map_id: i32, zone_id: i32) {
        self.item_maps
            .retain(|_, item| !(item.map_id == map_id && item.zone_id == zone_id));
    }

    // === Message builders ===
    pub fn build_item_appear_message(item: &ItemMap) -> Message {
        let mut msg = Message::new(68);
        let _ = msg.write_short(item.item_map_id as i16);
        let _ = msg.write_short(item.get_item_id());
        let _ = msg.write_short(item.x as i16);
        let _ = msg.write_short(item.y as i16);
        let _ = msg.write_int(item.player_id as i32);
        msg
    }

    pub fn build_item_disappear_message(item_map_id: i32) -> Message {
        let mut msg = Message::new(-21);
        let _ = msg.write_short(item_map_id as i16);
        msg
    }

    pub fn build_pickup_notification_message(item_map_id: i32, player_id: u64) -> Message {
        let mut msg = Message::new(-19);
        let _ = msg.write_short(item_map_id as i16);
        let _ = msg.write_int(player_id as i32);
        msg
    }
}

impl Default for ItemMapService {
    fn default() -> Self {
        Self::new()
    }
}
