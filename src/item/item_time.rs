use chrono::{DateTime, Utc};

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
