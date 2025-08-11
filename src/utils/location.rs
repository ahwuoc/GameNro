use std::time::SystemTime;

#[derive(Debug, Clone)]
pub struct Location {
    pub x: i16,
    pub y: i16,
    pub map_id: u32,
    pub zone_id: u32,
    pub last_time_player_move: u64,
}

impl Location {
    pub fn new() -> Self {
        Location {
            x: 0,
            y: 0,
            map_id: 0,
            zone_id: 0,
            last_time_player_move: 0,
        }
    }
    
    pub fn set_position(&mut self, x: i16, y: i16) {
        self.x = x;
        self.y = y;
        self.last_time_player_move = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }
    
    pub fn set_map(&mut self, map_id: u32, zone_id: u32) {
        self.map_id = map_id;
        self.zone_id = zone_id;
    }
    
    pub fn get_position(&self) -> (i16, i16) {
        (self.x, self.y)
    }
    
    pub fn get_map(&self) -> (u32, u32) {
        (self.map_id, self.zone_id)
    }
    
    pub fn update(&mut self) {
        // Update location-specific logic if needed
        // Currently no specific update logic required
    }
}
