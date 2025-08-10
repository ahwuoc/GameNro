use std::collections::HashMap;
use crate::item::item_time::ItemTime;

/// ItemTimeService manages item time effects
pub struct ItemTimeService {
    player_item_times: HashMap<i64, ItemTime>, // player_id -> ItemTime
}

impl ItemTimeService {
    pub fn new() -> Self {
        Self {
            player_item_times: HashMap::new(),
        }
    }

    /// Get singleton instance
    pub fn get_instance() -> &'static mut ItemTimeService {
        static mut INSTANCE: Option<ItemTimeService> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(ItemTimeService::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    /// Get or create item time for player
    pub fn get_player_item_time(&mut self, player_id: i64) -> &mut ItemTime {
        if !self.player_item_times.contains_key(&player_id) {
            self.player_item_times.insert(player_id, ItemTime::new());
        }
        self.player_item_times.get_mut(&player_id).unwrap()
    }

    /// Update all player item times
    pub fn update_all_item_times(&mut self) {
        for item_time in self.player_item_times.values_mut() {
            item_time.update();
        }
    }

    /// Remove player item time
    pub fn remove_player_item_time(&mut self, player_id: i64) {
        self.player_item_times.remove(&player_id);
    }

    /// Get player count
    pub fn get_player_count(&self) -> usize {
        self.player_item_times.len()
    }

    /// Clear all item times
    pub fn clear_all_item_times(&mut self) {
        self.player_item_times.clear();
    }
}
