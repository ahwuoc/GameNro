use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use once_cell::sync::Lazy;
use crate::map::Map;

use crate::entities::map_template::Model as MapTemplate;

pub struct MapManager {
    maps: Arc<RwLock<HashMap<i32, Map>>>,
}

impl MapManager {
    pub fn new() -> Self {
        Self {
            maps: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn create_map(&self, template: &MapTemplate) -> Result<(), Box<dyn std::error::Error>> {
        let map = Map::from_template(template);
        let zone_manager = crate::map::zone_manager::ZONE_MANAGER.read().await;
        map.init_zones(&zone_manager).await?;
        let mut maps = self.maps.write().await;
        maps.insert(map.map_id, map);
        Ok(())
    }

    pub async fn get_map(&self, map_id: i32) -> Option<Map> {
        let maps = self.maps.read().await;
        maps.get(&map_id).cloned()
    }

    pub async fn get_all_maps(&self) -> Vec<Map> {
        let maps = self.maps.read().await;
        maps.values().cloned().collect()
    }



    pub async fn update_all_maps(&self) -> Result<(), Box<dyn std::error::Error>> {
        let maps = self.maps.read().await;
        
        for map in maps.values() {
            map.update().await?;
        }
        
        Ok(())
    }

    pub async fn load_tiles_for_map(
        &self,
        map_id: i32,
        tile_id: i32,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut maps = self.maps.write().await;
        if let Some(map) = maps.get_mut(&map_id) {
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

    pub async fn get_maps_by_planet(&self, planet_id: i32) -> Vec<Map> {
        let maps = self.maps.read().await;
        maps.values()
            .filter(|map| map.planet_id == planet_id)
            .cloned()
            .collect()
    }

    pub async fn get_maps_by_type(&self, map_type: i32) -> Vec<Map> {
        let maps = self.maps.read().await;
        maps.values()
            .filter(|map| map.r#type == map_type)
            .cloned()
            .collect()
    }

    pub async fn get_map_by_name(&self, name: &str) -> Option<Map> {
        let maps = self.maps.read().await;
        for map in maps.values() {
            if map.map_name == name {
                return Some(map.clone());
            }
        }
        None
    }

    pub async fn remove_map(&self, map_id: i32) -> bool {
        let mut maps = self.maps.write().await;
        maps.remove(&map_id).is_some()
    }

    pub async fn get_map_count(&self) -> usize {
        let maps = self.maps.read().await;
        maps.len()
    }

    pub async fn clear_all_maps(&self) {
        let mut maps = self.maps.write().await;
        maps.clear();
    }

    pub async fn is_map_exists(&self, map_id: i32) -> bool {
        let maps = self.maps.read().await;
        maps.contains_key(&map_id)
    }

    pub async fn get_active_maps(&self) -> Vec<Map> {
        let maps = self.maps.read().await;
        let mut active_maps = Vec::new();
        for map in maps.values() {
            if map.is_active().await {
                active_maps.push(map.clone());
            }
        }
        active_maps
    }
}

// Global map manager
pub static MAP_MANAGER: Lazy<RwLock<MapManager>> = Lazy::new(|| RwLock::new(MapManager::new()));

impl Clone for MapManager {
    fn clone(&self) -> Self {
        Self {
            maps: Arc::clone(&self.maps),
        }
    }
}
