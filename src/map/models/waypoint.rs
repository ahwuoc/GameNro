#![allow(dead_code)]
use serde_json::Value;

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
        format!(
            "{} -> Map {} at ({}, {})",
            self.name, self.go_map, self.go_x, self.go_y
        )
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

    pub fn parse(json_str: &str) -> Vec<Self> {
        let mut waypoints = Vec::new();
        let cleaned = json_str
            .replace("[\"[", "[[")
            .replace("]\"]", "]]")
            .replace("\",\"", ",");

        if let Ok(json) = serde_json::from_str::<Value>(&cleaned) {
            if let Some(arr) = json.as_array() {
                for wpv in arr {
                    if let Some(wp_arr) = wpv.as_array() {
                        if wp_arr.len() >= 10 {
                            let name = wp_arr[0].as_str().unwrap_or("").to_string();
                            let min_x = wp_arr[1].as_i64().unwrap_or(0) as i16;
                            let min_y = wp_arr[2].as_i64().unwrap_or(0) as i16;
                            let max_x = wp_arr[3].as_i64().unwrap_or(0) as i16;
                            let max_y = wp_arr[4].as_i64().unwrap_or(0) as i16;
                            let is_enter = (wp_arr[5].as_i64().unwrap_or(0) as i8) == 1;
                            let is_offline = (wp_arr[6].as_i64().unwrap_or(0) as i8) == 1;
                            let go_map = wp_arr[7].as_i64().unwrap_or(0) as i32;
                            let go_x = wp_arr[8].as_i64().unwrap_or(0) as i16;
                            let go_y = wp_arr[9].as_i64().unwrap_or(0) as i16;
                            waypoints.push(WayPoint::new(
                                min_x, min_y, max_x, max_y, is_enter, is_offline, name, go_map,
                                go_x, go_y,
                            ));
                        }
                    }
                }
            }
        }
        waypoints
    }
}
