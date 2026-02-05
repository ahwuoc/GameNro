#[derive(Debug, Clone, Copy, Default)]
pub struct Location {
    pub x: i16,
    pub y: i16,
}

impl Location {
    pub fn new() -> Self {
        Location { x: 0, y: 0 }
    }

    pub fn set_position(&mut self, x: i16, y: i16) {
        self.x = x;
        self.y = y;
    }

    pub fn get_position(&self) -> (i16, i16) {
        (self.x, self.y)
    }
}
