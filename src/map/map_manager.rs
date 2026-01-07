use once_cell::sync::Lazy;
use dashmap::DashMap;
use crate::map::Map;
use crate::entities::map_template::Model as MapTemplate;

static MAPS: Lazy<DashMap<i32, Map>> = Lazy::new(|| DashMap::new());

pub async fn create_map(template: &MapTemplate) -> anyhow::Result<()> {
    let map = Map::from_template(template);
    let zone_manager = crate::map::zone_manager::ZONE_MANAGER.read().await;
    map.init_zones(&zone_manager).await?;
    MAPS.insert(map.map_id, map);
    Ok(())
}

pub fn get_map(map_id: i32) -> Option<Map> {
    MAPS.get(&map_id).map(|v| v.clone())
}

pub fn get_all_maps() -> Vec<Map> {
    MAPS.iter().map(|kv| kv.value().clone()).collect()
}

pub async fn update_all_maps() -> anyhow::Result<()> {
    for map in MAPS.iter() {
        map.value().update().await?;
    }
    Ok(())
}

pub fn load_tiles_for_map(map_id: i32, tile_id: i32) -> anyhow::Result<()> {
    if let Some(mut map) = MAPS.get_mut(&map_id) {
        if let Some((w, h, tile_map)) = crate::map::tile_loader::TileLoader::read_tile_map_file(map_id) {
            map.map_width = w;
            map.map_height = h;
            map.tile_map = tile_map;
        }
        if let Some(tile_top) = crate::map::tile_loader::TileLoader::read_tile_top_file(tile_id) {
            map.tile_top = tile_top;
        }
    }
    Ok(())
}

pub fn get_maps_by_planet(planet_id: i32) -> Vec<Map> {
    MAPS.iter()
        .filter(|kv| kv.value().planet_id == planet_id)
        .map(|kv| kv.value().clone())
        .collect()
}

pub fn get_maps_by_type(map_type: i32) -> Vec<Map> {
    MAPS.iter()
        .filter(|kv| kv.value().r#type == map_type)
        .map(|kv| kv.value().clone())
        .collect()
}

pub fn get_map_by_name(name: &str) -> Option<Map> {
    MAPS.iter()
        .find(|kv| kv.value().map_name == name)
        .map(|kv| kv.value().clone())
}

pub fn remove_map(map_id: i32) -> bool {
    MAPS.remove(&map_id).is_some()
}

pub fn get_map_count() -> usize {
    MAPS.len()
}

pub fn clear_all_maps() {
    MAPS.clear();
}

pub fn is_map_exists(map_id: i32) -> bool {
    MAPS.contains_key(&map_id)
}

pub async fn get_active_maps() -> Vec<Map> {
    let mut active_maps = Vec::new();
    for map in MAPS.iter() {
        if map.value().is_active().await {
            active_maps.push(map.value().clone());
        }
    }
    active_maps
}
