//! Shared utility functions used across change_map sub-modules

use crate::map::map_manager::MAP_MANAGER;
use crate::utils;

pub fn is_cold_planet_map(map_id: i32) -> bool {
    matches!(map_id, 105 | 106 | 107 | 108 | 109 | 110)
}

pub fn is_special_map(map_id: i32) -> bool {
    crate::map::services::map_service::is_map_pho_ban(map_id)
        || crate::map::services::map_service::is_map_ma_bu(map_id)
        || crate::map::services::map_service::is_map_offline(map_id)
        || crate::map::services::map_service::is_map_tap_luyen(map_id)
}

pub fn is_doanh_trai_map(map_id: i32) -> bool {
    (53..=62).contains(&map_id)
}

pub fn can_change_zone_now(_player: &crate::player::player::Player) -> bool {
    true
}

pub fn calculate_random_x_position(map_width: i32) -> i16 {
    if map_width <= 200 {
        return 100;
    }
    let usable = (map_width - 200) as u32;
    let seed = utils::time::current_time_millis() as u32;
    (100 + (seed % usable)) as i16
}

pub fn get_y_physic_in_top(map_id: i32, x: i16, y: i16) -> i16 {
    let Some(map) = MAP_MANAGER.find_by_id(map_id) else { return y; };
    if map.info.tile_map.is_empty() || map.info.tile_map[0].is_empty() {
        return y;
    }
    let tile_size = 24;
    let r_x = (x as i32 / tile_size) as usize;
    let r_y_start = (y as i32 / tile_size) as usize;
    if r_y_start >= map.info.tile_map.len() || r_x >= map.info.tile_map[0].len() {
        return y;
    }
    for i in r_y_start..map.info.tile_map.len() {
        let tile = map.info.tile_map[i][r_x];
        if map.info.tile_top.contains(&tile) {
            return (i as i16) * tile_size as i16;
        }
    }
    y
}

pub fn get_zones_for_map(map_id: i32) -> Vec<crate::map::models::zone::ZoneHandle> {
    if let Some(map) = MAP_MANAGER.find_by_id(map_id) {
        return map.get_all_zones();
    }
    Vec::new()
}

pub fn is_home_map_name(map_name: &str) -> bool {
    matches!(map_name, "Nhà Broly" | "Nhà Gôhan" | "Nhà Moori")
}
