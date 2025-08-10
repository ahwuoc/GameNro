use std::collections::HashMap;
use crate::map::map::Map;
use crate::map::zone::Zone;
use crate::map::waypoint::WayPoint;
use crate::player::Player;
pub struct MapService {
    // Service state
    initialized: bool,
    maps: HashMap<i32, Map>,
}

impl MapService {
    pub fn new() -> Self {
        Self {
            initialized: false,
            maps: HashMap::new(),
        }
    }

    /// Get singleton instance
    pub fn get_instance() -> &'static mut MapService {
        static mut INSTANCE: Option<MapService> = None;
        unsafe {
            if INSTANCE.is_none() {
                INSTANCE = Some(MapService::new());
            }
            INSTANCE.as_mut().unwrap()
        }
    }

    /// Initialize service
    pub fn init(&mut self) {
        self.initialized = true;
        println!("MapService initialized");
    }

    /// Check if service is initialized
    pub fn is_initialized(&self) -> bool {
        self.initialized
    }

    /// Get waypoint that player is currently in
    pub fn get_waypoint_player_in(&self, player: &Player) -> Option<&WayPoint> {
        if let Some(map) = self.get_map_by_id(player.map_id as i32) {
            // TODO: Implement async waypoint access
            // For now, return None as waypoints are async
            None
        } else {
            None
        }
    }

    /// Get map by ID
    pub fn get_map_by_id(&self, map_id: i32) -> Option<&Map> {
        self.maps.get(&map_id)
    }

    /// Get map by ID (mutable)
    pub fn get_map_by_id_mut(&mut self, map_id: i32) -> Option<&mut Map> {
        self.maps.get_mut(&map_id)
    }

    /// Add map to service
    pub fn add_map(&mut self, map: Map) {
        self.maps.insert(map.map_id, map);
    }

    /// Remove map from service
    pub fn remove_map(&mut self, map_id: i32) -> bool {
        self.maps.remove(&map_id).is_some()
    }

    /// Get all maps
    pub fn get_all_maps(&self) -> &HashMap<i32, Map> {
        &self.maps
    }

    /// Get map count
    pub fn get_map_count(&self) -> usize {
        self.maps.len()
    }

    /// Get map can join for player
    pub fn get_map_can_join(&self, player: &Player, map_id: i32, zone_id: i32) -> Option<&Zone> {
        if let Some(map) = self.get_map_by_id(map_id) {
            if zone_id == -1 {
                // TODO: Implement async zone access
                // For now, return None as zones are async
                None
            } else {
                // Get specific zone
                None // TODO: Implement async get_zone
            }
        } else {
            None
        }
    }

    /// Check if map is offline
    pub fn is_map_offline(&self, map_id: i32) -> bool {
        // TODO: Implement offline map logic
        // For now, return false (map is online)
        false
    }

    /// Check if map is cold
    pub fn is_map_cold(&self, map: &Map) -> bool {
        // TODO: Implement cold map logic
        // For now, return false (map is not cold)
        false
    }

    /// Check if map is Ban Do Kho Bau
    pub fn is_map_ban_do_kho_bau(&self, map_id: i32) -> bool {
        // TODO: Implement Ban Do Kho Bau map logic
        false
    }

    /// Check if map is Doanh Trai
    pub fn is_map_doanh_trai(&self, map_id: i32) -> bool {
        // TODO: Implement Doanh Trai map logic
        false
    }

    /// Check if map is Ma Bu
    pub fn is_map_ma_bu(&self, map_id: i32) -> bool {
        // TODO: Implement Ma Bu map logic
        false
    }

    /// Check if map is Satan
    pub fn is_map_satan(&self, map_id: i32) -> bool {
        // TODO: Implement Satan map logic
        false
    }

    /// Get map capsule for player
    pub fn get_map_capsule(&self, player: &Player) -> Vec<&Zone> {
        // TODO: Implement capsule map logic
        Vec::new()
    }

    /// Get map black ball
    pub fn get_map_black_ball(&self) -> Vec<&Zone> {
        // TODO: Implement black ball map logic
        Vec::new()
    }

    /// Get map Ma Bu
    pub fn get_map_ma_bu(&self) -> Vec<&Zone> {
        // TODO: Implement Ma Bu map logic
        Vec::new()
    }

    /// Get map Satan
    pub fn get_map_satan(&self) -> Vec<&Zone> {
        // TODO: Implement Satan map logic
        Vec::new()
    }

    /// Get map by name
    pub fn get_map_by_name(&self, name: &str) -> Option<&Map> {
        for map in self.maps.values() {
            if map.map_name == name {
                return Some(map);
            }
        }
        None
    }

    /// Get maps by planet ID
    pub fn get_maps_by_planet(&self, planet_id: i32) -> Vec<&Map> {
        self.maps.values()
            .filter(|map| map.planet_id == planet_id)
            .collect()
    }

    /// Get maps by type
    pub fn get_maps_by_type(&self, map_type: i32) -> Vec<&Map> {
        self.maps.values()
            .filter(|map| map.r#type == map_type)
            .collect()
    }

    /// Check if player can join map
    pub fn can_player_join_map(&self, player: &Player, map_id: i32) -> bool {
        if let Some(map) = self.get_map_by_id(map_id) {
            // TODO: Implement async zone access
            // For now, return false as zones are async
            false
        } else {
            false
        }
    }

    /// Get best zone for player in map
    pub fn get_best_zone_for_player(&self, player: &Player, map_id: i32) -> Option<&Zone> {
        if let Some(map) = self.get_map_by_id(map_id) {
            // TODO: Implement async zone access
            // For now, return None as zones are async
            None
        } else {
            None
        }
    }

    /// Update all maps
    pub fn update_all_maps(&mut self) {
        for map in self.maps.values_mut() {
            map.update();
        }
    }

    /// Clear all maps
    pub fn clear_all_maps(&mut self) {
        self.maps.clear();
    }

    /// Get map info for player
    pub fn get_map_info(&self, player: &Player, map_id: i32) -> Option<String> {
        if let Some(map) = self.get_map_by_id(map_id) {
            Some(format!("Map: {} (ID: {})", map.map_name, map.map_id))
        } else {
            None
        }
    }
}
