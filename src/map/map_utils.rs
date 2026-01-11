#![allow(dead_code)]
use crate::map::Map;
use crate::map::Zone;
use crate::player::Player;

pub struct MapUtils;

impl MapUtils {
    pub fn calculate_distance(x1: i32, y1: i32, x2: i32, y2: i32) -> f64 {
        let dx = (x2 - x1) as f64;
        let dy = (y2 - y1) as f64;
        (dx * dx + dy * dy).sqrt()
    }

    pub fn is_position_in_range(x1: i32, y1: i32, x2: i32, y2: i32, range: i32) -> bool {
        Self::calculate_distance(x1, y1, x2, y2) <= range as f64
    }

    pub fn get_random_position_in_map(map: &Map) -> (i16, i16) {
        use std::time::SystemTime;
        let seed = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u32;

        let x = (seed % (map.info.map_width as u32)) as i16;
        let y = (seed % (map.info.map_height as u32)) as i16;
        (x, y)
    }

    pub fn get_safe_spawn_position(map: &Map) -> (i16, i16) {
        for _ in 0..100 {
            let (x, y) = Self::get_random_position_in_map(map);
            if Self::is_safe_position(map, x, y) {
                return (x, y);
            }
        }
        (
            map.info.map_width as i16 / 2,
            map.info.map_height as i16 / 2,
        )
    }

    pub fn is_safe_position(map: &Map, x: i16, y: i16) -> bool {
        if x < 0 || x >= map.info.map_width as i16 || y < 0 || y >= map.info.map_height as i16 {
            return false;
        }

        if let Some(tile_id) = Self::get_tile_at_position(map, x as i32, y as i32) {
            return crate::map::tile_loader::TileLoader::is_walkable_tile(tile_id);
        }

        false
    }

    pub fn get_tile_at_position(map: &Map, x: i32, y: i32) -> Option<i32> {
        if x < 0 || x >= map.info.map_width || y < 0 || y >= map.info.map_height {
            return None;
        }

        if y < map.info.tile_map.len() as i32 && x < map.info.tile_map[y as usize].len() as i32 {
            Some(map.info.tile_map[y as usize][x as usize])
        } else {
            None
        }
    }

    pub fn find_path_to_target(
        map: &Map,
        start_x: i16,
        start_y: i16,
        target_x: i16,
        target_y: i16,
    ) -> Option<Vec<(i16, i16)>> {
        if Self::is_direct_path_clear(map, start_x, start_y, target_x, target_y) {
            return Some(vec![(target_x, target_y)]);
        }
        None
    }

    pub fn is_direct_path_clear(
        map: &Map,
        start_x: i16,
        start_y: i16,
        end_x: i16,
        end_y: i16,
    ) -> bool {
        let dx = (end_x - start_x).abs();
        let dy = (end_y - start_y).abs();
        let steps = dx.max(dy);

        if steps == 0 {
            return true;
        }

        for i in 1..=steps {
            let x = start_x + (i * (end_x - start_x) / steps);
            let y = start_y + (i * (end_y - start_y) / steps);

            if !Self::is_safe_position(map, x, y) {
                return false;
            }
        }

        true
    }

    pub fn get_players_in_range(
        _zone: &Zone,
        _center_x: i16,
        _center_y: i16,
        _range: i32,
    ) -> Vec<Player> {
        // TODO: Implement async player access
        Vec::new()
    }

    pub fn get_mobs_in_range(
        _zone: &Zone,
        _center_x: i16,
        _center_y: i16,
        _range: i32,
    ) -> Vec<crate::mob::RtMob> {
        // TODO: Implement async mob access
        Vec::new()
    }

    pub fn get_items_in_range(
        _zone: &Zone,
        _center_x: i16,
        _center_y: i16,
        _range: i32,
    ) -> Vec<crate::map::ItemMap> {
        // TODO: Implement async item access
        Vec::new()
    }

    pub fn validate_map_coordinates(map: &Map, x: i16, y: i16) -> bool {
        x >= 0 && x < map.info.map_width as i16 && y >= 0 && y < map.info.map_height as i16
    }

    pub fn get_map_center(map: &Map) -> (i16, i16) {
        (
            map.info.map_width as i16 / 2,
            map.info.map_height as i16 / 2,
        )
    }

    pub fn get_map_corners(map: &Map) -> [(i16, i16); 4] {
        [
            (0, 0),                              // Top-left
            (map.info.map_width as i16 - 1, 0),  // Top-right
            (0, map.info.map_height as i16 - 1), // Bottom-left
            (
                map.info.map_width as i16 - 1,
                map.info.map_height as i16 - 1,
            ), // Bottom-right
        ]
    }

    pub fn is_map_full(_map: &Map) -> bool {
        // TODO: Implement async zone access to check if all zones are full
        false
    }

    pub fn get_map_population(_map: &Map) -> usize {
        // TODO: Implement async zone access to count total players
        0
    }

    pub fn get_map_info_string(map: &Map) -> String {
        format!(
            "Map: {} (ID: {}) - Size: {}x{} - Zones: {} - Max Players: {}",
            map.info.name,
            map.info.id,
            map.info.map_width,
            map.info.map_height,
            map.info.zone_count,
            map.info.max_player
        )
    }
}
