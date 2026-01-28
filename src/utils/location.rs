use std::time::SystemTime;

use crate::utils::time;

#[derive(Debug, Clone)]
pub struct Location {
    pub x: i16,
    pub y: i16,
    pub last_time_player_move: u64,
}

impl Location {
    pub fn new() -> Self {
        Location {
            x: 0,
            y: 0,
            last_time_player_move: 0,
        }
    }

    pub fn set_position(&mut self, x: i16, y: i16) {
        self.x = x;
        self.y = y;
        self.last_time_player_move = time::current_time_millis();
    }

    pub fn get_position(&self) -> (i16, i16) {
        (self.x, self.y)
    }
}
