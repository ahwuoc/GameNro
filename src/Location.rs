use chrono::Local;

#[derive(Debug, Clone)]
pub struct Location {
    pub x: i32,
    pub y: i32,
    pub last_time_player_move: u64,
}

impl Location {
    pub fn new(x: i32, y: i32) -> Self {
        Location {
            x,
            y,
            last_time_player_move: 0,
        }
    }
}
