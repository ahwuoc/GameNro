#[derive(Debug, Clone)]
pub struct WayPoint {
    pub min_x: i16,
    pub min_y: i16,
    pub max_x: i16,
    pub max_y: i16,
    pub is_enter: bool,
    pub is_offline: bool,
    pub name: String,
    pub go_map: i32,
    pub go_x: i16,
    pub go_y: i16,
}

impl WayPoint {
    pub fn new(
        min_x: i16,
        min_y: i16,
        max_x: i16,
        max_y: i16,
        is_enter: bool,
        is_offline: bool,
        name: String,
        go_map: i32,
        go_x: i16,
        go_y: i16,
    ) -> Self {
        Self {
            min_x,
            min_y,
            max_x,
            max_y,
            is_enter,
            is_offline,
            name,
            go_map,
            go_x,
            go_y,
        }
    }

    pub fn contains_position(&self, x: i16, y: i16) -> bool {
        x >= self.min_x && x <= self.max_x && y >= self.min_y && y <= self.max_y
    }

    pub fn get_center_position(&self) -> (i16, i16) {
        let center_x = (self.min_x + self.max_x) / 2;
        let center_y = (self.min_y + self.max_y) / 2;
        (center_x, center_y)
    }

    pub fn get_area(&self) -> i32 {
        let width = (self.max_x - self.min_x) as i32;
        let height = (self.max_y - self.min_y) as i32;
        width * height
    }

    pub fn is_valid(&self) -> bool {
        self.min_x <= self.max_x && self.min_y <= self.max_y && self.go_map > 0
    }

    pub fn get_destination_info(&self) -> String {
        format!("{} -> Map {} at ({}, {})", self.name, self.go_map, self.go_x, self.go_y)
    }

    pub fn is_enter_waypoint(&self) -> bool {
        self.is_enter
    }

    pub fn is_offline_waypoint(&self) -> bool {
        self.is_offline
    }

    pub fn can_teleport(&self) -> bool {
        !self.is_offline && self.is_valid()
    }
}
