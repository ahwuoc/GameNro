use chrono::{DateTime, Utc};
use std::collections::HashMap;

/// ItemTime represents time-based item effects
#[derive(Debug, Clone)]
pub struct ItemTime {
    // Basic time constants
    pub const_time_item: i64,
    pub const_time_open_power: i64,

    // Basic status flags
    pub is_use_bo_huyet: bool,
    pub is_use_bo_khi: bool,            
    pub is_use_giap_xen: bool,
    pub is_use_cuong_no: bool,
    pub is_use_an_danh: bool,

    // Basic time tracking
    pub last_time_bo_huyet: DateTime<Utc>,
    pub last_time_bo_khi: DateTime<Utc>,
    pub last_time_giap_xen: DateTime<Utc>,
    pub last_time_cuong_no: DateTime<Utc>,
    pub last_time_an_danh: DateTime<Utc>,
}

impl ItemTime {
    pub fn new() -> Self {
        let now = Utc::now();
        Self {
            const_time_item: 600000, // 10 minutes
            const_time_open_power: 86400000, // 24 hours
            
            is_use_bo_huyet: false,
            is_use_bo_khi: false,
            is_use_giap_xen: false,
            is_use_cuong_no: false,
            is_use_an_danh: false,

            last_time_bo_huyet: now,
            last_time_bo_khi: now,
            last_time_giap_xen: now,
            last_time_cuong_no: now,
            last_time_an_danh: now,
        }
    }

    /// Update item time effects
    pub fn update(&mut self) {
        // Basic update logic
        if self.is_use_bo_huyet {
            if self.can_do_with_time(self.last_time_bo_huyet, self.const_time_item) {
                self.is_use_bo_huyet = false;
            }
        }

        if self.is_use_bo_khi {
            if self.can_do_with_time(self.last_time_bo_khi, self.const_time_item) {
                self.is_use_bo_khi = false;
            }
        }

        if self.is_use_giap_xen {
            if self.can_do_with_time(self.last_time_giap_xen, self.const_time_item) {
                self.is_use_giap_xen = false;
            }
        }

        if self.is_use_cuong_no {
            if self.can_do_with_time(self.last_time_cuong_no, self.const_time_item) {
                self.is_use_cuong_no = false;
            }
        }

        if self.is_use_an_danh {
            if self.can_do_with_time(self.last_time_an_danh, self.const_time_item) {
                self.is_use_an_danh = false;
            }
        }
    }

    /// Check if enough time has passed since last action
    fn can_do_with_time(&self, last_time: DateTime<Utc>, required_duration: i64) -> bool {
        let now = Utc::now();
        let duration = now.signed_duration_since(last_time).num_milliseconds();
        duration >= required_duration
    }

    /// Use bo huyet
    pub fn use_bo_huyet(&mut self) {
        self.is_use_bo_huyet = true;
        self.last_time_bo_huyet = Utc::now();
    }

    /// Use bo khi
    pub fn use_bo_khi(&mut self) {
        self.is_use_bo_khi = true;
        self.last_time_bo_khi = Utc::now();
    }

    /// Use giap xen
    pub fn use_giap_xen(&mut self) {
        self.is_use_giap_xen = true;
        self.last_time_giap_xen = Utc::now();
    }

    /// Use cuong no
    pub fn use_cuong_no(&mut self) {
        self.is_use_cuong_no = true;
        self.last_time_cuong_no = Utc::now();
    }

    /// Use an danh
    pub fn use_an_danh(&mut self) {
        self.is_use_an_danh = true;
        self.last_time_an_danh = Utc::now();
    }
}

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
