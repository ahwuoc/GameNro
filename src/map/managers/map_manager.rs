use crate::entities::map_template::Model as MapTemplate;
use crate::map::Map;
use dashmap::DashMap;
use once_cell::sync::Lazy;

pub struct MapManager {
    instances: DashMap<i32, Map>,
}

pub static MAP_MANAGER: Lazy<MapManager> = Lazy::new(|| MapManager::new());

impl MapManager {
    fn new() -> Self {
        Self {
            instances: DashMap::new(),
        }
    }

    pub async fn init_and_register_map(&self, template: &MapTemplate) -> anyhow::Result<()> {
        let map = Map::from_template(template);
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        map.init_zones(&zone_manager)?;
        map.init_mobs().await?;

        self.instances.insert(map.info.id, map);
        Ok(())
    }

    pub fn find_by_id(&self, map_id: i32) -> Option<Map> {
        self.instances.get(&map_id).map(|v| v.clone())
    }

    pub fn get_all(&self) -> Vec<Map> {
        self.instances.iter().map(|kv| kv.value().clone()).collect()
    }

    pub fn find_maps_by_planet(&self, planet_id: i32) -> Vec<Map> {
        self.instances
            .iter()
            .filter(|kv| kv.value().info.planet_id == planet_id)
            .map(|kv| kv.value().clone())
            .collect()
    }

    pub fn find_maps_by_type(&self, map_type: i32) -> Vec<Map> {
        self.instances
            .iter()
            .filter(|kv| kv.value().info.r#type == map_type)
            .map(|kv| kv.value().clone())
            .collect()
    }

    pub fn find_map_by_name(&self, name: &str) -> Option<Map> {
        self.instances
            .iter()
            .find(|kv| kv.value().info.name == name)
            .map(|kv| kv.value().clone())
    }

    pub fn unregister_map(&self, map_id: i32) -> bool {
        self.instances.remove(&map_id).is_some()
    }
    pub fn count(&self) -> usize {
        self.instances.len()
    }

    pub fn clear(&self) {
        self.instances.clear();
    }

    pub fn exists(&self, map_id: i32) -> bool {
        self.instances.contains_key(&map_id)
    }

    pub fn load_tiles(_map_id: i32, _tile_id: i32) -> anyhow::Result<()> {
        Ok(())
    }
}
