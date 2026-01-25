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
        if let Ok(json) = serde_json::from_str::<Value>(json_str) {
            if let Some(arr) = json.as_array() {
                for wpv in arr {
                    if let (Some(area), Some(flags), Some(target)) = (
                        wpv.get("area").and_then(|v| v.as_array()),
                        wpv.get("flags").and_then(|v| v.as_i64()),
                        wpv.get("target"),
                    ) {
                        if area.len() >= 4 {
                            let min_x = area[0].as_i64().unwrap_or(0) as i16;
                            let min_y = area[1].as_i64().unwrap_or(0) as i16;
                            let max_x = area[2].as_i64().unwrap_or(0) as i16;
                            let max_y = area[3].as_i64().unwrap_or(0) as i16;

                            let is_enter = (flags & 1) != 0;
                            let is_offline = (flags & 2) != 0;

                            let go_map: i32 =
                                target.get("map").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                            let go_x: i16 =
                                target.get("x").and_then(|v| v.as_i64()).unwrap_or(0) as i16;
                            let go_y: i16 =
                                target.get("y").and_then(|v| v.as_i64()).unwrap_or(0) as i16;

                            let name = crate::templates::map_template_manager::get(go_map)
                                .map(|t| t.name)
                                .unwrap_or_else(|| "".to_string());

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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::entities::map_template::Model as MapTemplate;
    use crate::templates::map_template_manager;

    #[test]
    fn test_parse_waypoint() {
        let template = MapTemplate {
            id: 1,
            name: "Target Map".to_string(),
            zones: 1,
            max_player: 10,
            data: "".to_string(),
            r#type: 0,
            planet_id: 0,
            bg_type: 0,
            tile_id: 0,
            bg_id: 0,
            waypoints: "".to_string(),
            mobs: "".to_string(),
            npcs: "".to_string(),
        };

        {
            let mut templates = map_template_manager::MAP_TEMPLATES.write().unwrap();
            templates.retain(|t| t.id != 1);
            templates.push(template);
        }

        let json = r#"[{"area":[1224,408,1248,432],"flags":0,"target":{"map":1,"x":60,"y":384}},{"area":[288,408,360,432],"flags":3,"target":{"map":21,"x":489,"y":336}},{"area":[0,408,24,432],"flags":0,"target":{"map":42,"x":1380,"y":432}}]"#;

        let waypoints = WayPoint::parse(json);

        assert_eq!(waypoints.len(), 3);

        // Check first waypoint
        let wp1 = &waypoints[0];
        assert_eq!(wp1.min_x, 1224);
        assert_eq!(wp1.min_y, 408);
        assert_eq!(wp1.max_x, 1248);
        assert_eq!(wp1.max_y, 432);
        assert_eq!(wp1.is_enter, false);
        assert_eq!(wp1.is_offline, false);
        assert_eq!(wp1.go_map, 1);
        assert_eq!(wp1.name, "Target Map");

        let wp2 = &waypoints[1];
        assert_eq!(wp2.is_enter, true);
        assert_eq!(wp2.is_offline, true);
        assert_eq!(wp2.go_map, 21);
    }
}
