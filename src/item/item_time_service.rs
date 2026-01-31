#![allow(dead_code)]
use crate::item::item_time::ItemTime;
use std::collections::HashMap;

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

    /// Get or create item time for player
    pub fn get_player_item_time(&mut self, player_id: i64) -> &mut ItemTime {
        self.player_item_times
            .entry(player_id)
            .or_insert_with(ItemTime::new)
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
