use chrono::{DateTime, Utc};

#[derive(Debug, Clone)]
pub struct PlayerItemTime {
    pub is_use_an_danh: bool,
    pub last_time_an_danh: DateTime<Utc>,
    pub is_use_an_danh_2: bool,
    pub last_time_an_danh_2: DateTime<Utc>,
    pub is_use_an_danh_3: bool,
    pub last_time_an_danh_3: DateTime<Utc>,
    pub is_use_an_danh_4: bool,
    pub last_time_an_danh_4: DateTime<Utc>,
    pub is_use_an_danh_5: bool,
    pub last_time_an_danh_5: DateTime<Utc>,
}

impl PlayerItemTime {
    pub fn new() -> Self {
        Self {
            is_use_an_danh: false,
            last_time_an_danh: Utc::now(),
            is_use_an_danh_2: false,
            last_time_an_danh_2: Utc::now(),
            is_use_an_danh_3: false,
            last_time_an_danh_3: Utc::now(),
            is_use_an_danh_4: false,
            last_time_an_danh_4: Utc::now(),
            is_use_an_danh_5: false,
            last_time_an_danh_5: Utc::now(),
        }
    }

    pub fn update(&mut self) {
        let now = Utc::now();
        
        // Check if an_danh effects have expired (30 minutes)
        if self.is_use_an_danh && (now - self.last_time_an_danh).num_minutes() >= 30 {
            self.is_use_an_danh = false;
        }
        
        if self.is_use_an_danh_2 && (now - self.last_time_an_danh_2).num_minutes() >= 30 {
            self.is_use_an_danh_2 = false;
        }
        
        if self.is_use_an_danh_3 && (now - self.last_time_an_danh_3).num_minutes() >= 30 {
            self.is_use_an_danh_3 = false;
        }
        
        if self.is_use_an_danh_4 && (now - self.last_time_an_danh_4).num_minutes() >= 30 {
            self.is_use_an_danh_4 = false;
        }
        
        if self.is_use_an_danh_5 && (now - self.last_time_an_danh_5).num_minutes() >= 30 {
            self.is_use_an_danh_5 = false;
        }
    }

    pub fn use_an_danh(&mut self, level: u8) {
        let now = Utc::now();
        match level {
            1 => {
                self.is_use_an_danh = true;
                self.last_time_an_danh = now;
            }
            2 => {
                self.is_use_an_danh_2 = true;
                self.last_time_an_danh_2 = now;
            }
            3 => {
                self.is_use_an_danh_3 = true;
                self.last_time_an_danh_3 = now;
            }
            4 => {
                self.is_use_an_danh_4 = true;
                self.last_time_an_danh_4 = now;
            }
            5 => {
                self.is_use_an_danh_5 = true;
                self.last_time_an_danh_5 = now;
            }
            _ => {}
        }
    }

    pub fn is_an_danh_active(&self, level: u8) -> bool {
        match level {
            1 => self.is_use_an_danh,
            2 => self.is_use_an_danh_2,
            3 => self.is_use_an_danh_3,
            4 => self.is_use_an_danh_4,
            5 => self.is_use_an_danh_5,
            _ => false,
        }
    }

    pub fn get_an_danh_remaining_time(&self, level: u8) -> i64 {
        let now = Utc::now();
        let last_time = match level {
            1 => self.last_time_an_danh,
            2 => self.last_time_an_danh_2,
            3 => self.last_time_an_danh_3,
            4 => self.last_time_an_danh_4,
            5 => self.last_time_an_danh_5,
            _ => return 0,
        };
        
        if self.is_an_danh_active(level) {
            let elapsed = (now - last_time).num_seconds();
            let remaining = 30 * 60 - elapsed; // 30 minutes in seconds
            remaining.max(0)
        } else {
            0
        }
    }

    pub fn has_any_an_danh_active(&self) -> bool {
        self.is_use_an_danh || self.is_use_an_danh_2 || self.is_use_an_danh_3 || 
        self.is_use_an_danh_4 || self.is_use_an_danh_5
    }

    pub fn get_active_an_danh_levels(&self) -> Vec<u8> {
        let mut levels = Vec::new();
        if self.is_use_an_danh { levels.push(1); }
        if self.is_use_an_danh_2 { levels.push(2); }
        if self.is_use_an_danh_3 { levels.push(3); }
        if self.is_use_an_danh_4 { levels.push(4); }
        if self.is_use_an_danh_5 { levels.push(5); }
        levels
    }

    pub fn clear_all_an_danh(&mut self) {
        self.is_use_an_danh = false;
        self.is_use_an_danh_2 = false;
        self.is_use_an_danh_3 = false;
        self.is_use_an_danh_4 = false;
        self.is_use_an_danh_5 = false;
    }
}
