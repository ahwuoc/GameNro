/// WayPoint represents a teleportation point in a map
#[derive(Debug, Clone)]
pub struct WayPoint {
    pub id: i32,
    pub map_id: i32,
    pub min_x: i32,
    pub max_x: i32,
    pub min_y: i32,
    pub max_y: i32,
    pub go_map: i32,
    pub go_x: i32,
    pub go_y: i32,
    pub name: String,
}

impl WayPoint {
    pub fn new(id: i32, map_id: i32, min_x: i32, max_x: i32, min_y: i32, max_y: i32, 
                go_map: i32, go_x: i32, go_y: i32, name: String) -> Self {
        Self {
            id,
            map_id,
            min_x,
            max_x,
            min_y,
            max_y,
            go_map,
            go_x,
            go_y,
            name,
        }
    }

    /// Check if position is within waypoint area
    pub fn is_in_area(&self, x: i32, y: i32) -> bool {
        x >= self.min_x && x <= self.max_x && y >= self.min_y && y <= self.max_y
    }

    /// Get waypoint center position
    pub fn get_center(&self) -> (i32, i32) {
        let center_x = (self.min_x + self.max_x) / 2;
        let center_y = (self.min_y + self.max_y) / 2;
        (center_x, center_y)
    }

    /// Get waypoint area width
    pub fn get_width(&self) -> i32 {
        self.max_x - self.min_x
    }

    /// Get waypoint area height
    pub fn get_height(&self) -> i32 {
        self.max_y - self.min_y
    }

    /// Check if waypoint is valid
    pub fn is_valid(&self) -> bool {
        self.go_map > 0 && self.go_x >= 0 && self.go_y >= 0
    }

    /// Get destination info
    pub fn get_destination_info(&self) -> String {
        format!("{} -> Map {} at ({}, {})", self.name, self.go_map, self.go_x, self.go_y)
    }
}

impl Default for WayPoint {
    fn default() -> Self {
        Self {
            id: 0,
            map_id: 0,
            min_x: 0,
            max_x: 0,
            min_y: 0,
            max_y: 0,
            go_map: 0,
            go_x: 0,
            go_y: 0,
            name: String::new(),
        }
    }
}
