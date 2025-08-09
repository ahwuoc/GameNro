use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use once_cell::sync::Lazy;
use chrono::{DateTime, Utc};
use crate::models::zone::{Zone, ZoneManager};
use crate::models::mob::Mob;
use crate::entities::map_template::Model as MapTemplate;
use crate::entities::mob_template::Model as MobTemplate;

/// WayPoint represents a teleport point in the map
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
}

#[derive(Debug)]
pub struct Map {
    pub map_id: i32,
    pub map_name: String,
    pub planet_id: i32,
    pub planet_name: String,
    pub tile_id: i32,
    pub bg_id: i32,
    pub bg_type: i32,
    pub r#type: i32,
    
    // Map dimensions
    pub map_width: i32,
    pub map_height: i32,
    pub tile_map: Vec<Vec<i32>>,
    pub tile_top: Vec<i32>,
    
    // Map content
    pub zones: Arc<RwLock<Vec<Zone>>>,
    pub way_points: Arc<RwLock<Vec<WayPoint>>>,
    pub npcs: Arc<RwLock<Vec<i32>>>,
    
    // Map state
    pub is_active: Arc<RwLock<bool>>,
    pub last_update: Arc<RwLock<DateTime<Utc>>>,
}

impl Map {
    /// Create a new map from template
    pub fn from_template(template: &MapTemplate) -> Self {
        let current_time = Utc::now();
        
        Self {
            map_id: template.id,
            map_name: template.name.clone(),
            planet_id: template.planet_id as i32,
            planet_name: format!("Planet {}", template.planet_id),
            tile_id: template.tile_id as i32,
            bg_id: template.bg_id as i32,
            bg_type: template.bg_type as i32,
            r#type: template.r#type as i32,
            map_width: 0, // Will be calculated
            map_height: 0, // Will be calculated
            tile_map: Vec::new(),
            tile_top: Vec::new(),
            zones: Arc::new(RwLock::new(Vec::new())),
            way_points: Arc::new(RwLock::new(Vec::new())),
            npcs: Arc::new(RwLock::new(Vec::new())),
            is_active: Arc::new(RwLock::new(true)),
            last_update: Arc::new(RwLock::new(current_time)),
        }
    }

    /// Initialize map with zones
    pub async fn init_zones(&self, zone_manager: &ZoneManager) -> Result<(), Box<dyn std::error::Error>> {
        let n_zones = self.get_zone_count();
        let max_player = self.get_max_player_per_zone();
        
        let mut zones = self.zones.write().await;
        
        for i in 0..n_zones {
            // Create zone in zone manager
            zone_manager.create_zone(self.map_id, i, max_player).await?;
            
            // Create zone instance for this map
            let zone = Zone::new(self.map_id, i, max_player);
            zones.push(zone);
        }
        
        println!("Initialized {} zones for map {}", n_zones, self.map_name);
        Ok(())
    }

    pub async fn init_mobs(&self, mob_templates: &HashMap<i32, MobTemplate>) -> Result<(), Box<dyn std::error::Error>> {
    
        let zones = self.zones.read().await;
        for (zone_index, zone) in zones.iter().enumerate() {
            // Create sample mobs for each zone
            for i in 0..5 {
                if let Some(template) = mob_templates.get(&(i + 1)) {
                    let mob = Mob::from_template(
                        template,
                        i as i32,
                        self.map_id,
                        zone_index as i32,
                        100 + (i * 50) as i32,
                        100 + (i * 50) as i32,
                    );
                    
                    zone.add_mob(mob).await?;
                }
            }
        }
        
        println!("Initialized mobs for map {}", self.map_name);
        Ok(())
    }

    /// Initialize NPCs in the map
    pub async fn init_npcs(&self, npc_ids: &[i32], npc_x: &[i16], npc_y: &[i16]) -> Result<(), Box<dyn std::error::Error>> {
        {
            let mut npcs = self.npcs.write().await;
            npcs.clear();
            for &npc_id in npc_ids {
                npcs.push(npc_id);
            }
        }
        let zones = self.zones.read().await;
        for zone in zones.iter() {
            let _ = (npc_ids, npc_x, npc_y);
        }
        println!("Initialized {} NPC templates for map {}", npc_ids.len(), self.map_name);
        Ok(())
    }

    /// Add waypoint to map
    pub async fn add_waypoint(&self, waypoint: WayPoint) -> Result<(), Box<dyn std::error::Error>> {
        let mut way_points = self.way_points.write().await;
        way_points.push(waypoint);
        Ok(())
    }

    /// Get waypoint at position
    pub async fn get_waypoint_at_position(&self, x: i16, y: i16) -> Option<WayPoint> {
        let way_points = self.way_points.read().await;
        
        for waypoint in way_points.iter() {
            if waypoint.contains_position(x, y) {
                return Some(waypoint.clone());
            }
        }
        
        None
    }

    /// Get zone by ID
    pub async fn get_zone(&self, zone_id: i32) -> Option<Zone> {
        let zones = self.zones.read().await;
        zones.get(zone_id as usize).cloned()
    }

    /// Get best zone for player (least populated)
    pub async fn get_best_zone(&self) -> Option<Zone> {
        let zones = self.zones.read().await;
        
        let mut best_zone: Option<&Zone> = None;
        let mut min_players = i32::MAX;
        
        for zone in zones.iter() {
            let player_count = zone.get_num_players().await as i32;
            if player_count < min_players && player_count < zone.max_player {
                min_players = player_count;
                best_zone = Some(zone);
            }
        }
        
        best_zone.cloned()
    }

    /// Get all zones
    pub async fn get_all_zones(&self) -> Vec<Zone> {
        let zones = self.zones.read().await;
        zones.clone()
    }

    /// Update map (called periodically)
    pub async fn update(&self) -> Result<(), Box<dyn std::error::Error>> {
        let zones = self.zones.read().await;
        
        // Update all zones
        for zone in zones.iter() {
            zone.update().await?;
        }
        
        // Update last update time
        let mut last_update = self.last_update.write().await;
        *last_update = Utc::now();
        
        Ok(())
    }

    /// Check if map is active
    pub async fn is_active(&self) -> bool {
        let is_active = self.is_active.read().await;
        *is_active
    }

    /// Set map active status
    pub async fn set_active(&self, active: bool) {
        let mut is_active = self.is_active.write().await;
        *is_active = active;
    }

    /// Get map info for client
    pub async fn get_map_info(&self) -> MapInfo {
        let zones = self.zones.read().await;
        let way_points = self.way_points.read().await;
        let npcs = self.npcs.read().await;
        
        MapInfo {
            map_id: self.map_id,
            map_name: self.map_name.clone(),
            planet_id: self.planet_id,
            planet_name: self.planet_name.clone(),
            tile_id: self.tile_id,
            bg_id: self.bg_id,
            bg_type: self.bg_type,
            r#type: self.r#type,
            map_width: self.map_width,
            map_height: self.map_height,
            zone_count: zones.len() as i32,
            way_point_count: way_points.len() as i32,
            npc_count: npcs.len() as i32,
        }
    }

    /// Get zone count based on map type
    fn get_zone_count(&self) -> i32 {
        match self.r#type {
            0 => 1, // MAP_OFFLINE
            1 => 3, // MAP_BLACK_BALL_WAR
            2 => 2, // MAP_MA_BU
            3 => 4, // MAP_DOANH_TRAI
            4 => 4, // MAP_BAN_DO_KHO_BAU
            5 => 3, // MAP_CON_DUONG_RAN_DOC
            6 => 2, // MAP_KHI_GAS
            7 => 1, // MAP_SATAN
            _ => 1, // Default
        }
    }

    /// Get max player per zone
    fn get_max_player_per_zone(&self) -> i32 {
        match self.r#type {
            0 => 1,  // MAP_OFFLINE
            _ => 50, // Default
        }
    }
}

/// Map information for client
#[derive(Debug, Clone)]
pub struct MapInfo {
    pub map_id: i32,
    pub map_name: String,
    pub planet_id: i32,
    pub planet_name: String,
    pub tile_id: i32,
    pub bg_id: i32,
    pub bg_type: i32,
    pub r#type: i32,
    pub map_width: i32,
    pub map_height: i32,
    pub zone_count: i32,
    pub way_point_count: i32,
    pub npc_count: i32,
}

/// Map manager to handle multiple maps
pub struct MapManager {
    maps: Arc<RwLock<HashMap<i32, Map>>>,
    zone_manager: ZoneManager,
}

impl MapManager {
    pub fn new() -> Self {
        Self {
            maps: Arc::new(RwLock::new(HashMap::new())),
            zone_manager: ZoneManager::new(),
        }
    }

    /// Create a new map
    pub async fn create_map(&self, template: &MapTemplate) -> Result<(), Box<dyn std::error::Error>> {
        let map = Map::from_template(template);
        
        // Initialize zones
        map.init_zones(&self.zone_manager).await?;
        
        // Store map
        let mut maps = self.maps.write().await;
        maps.insert(map.map_id, map);
        
        println!("Created map {}: {}", template.id, template.name);
        Ok(())
    }

    /// Get map by ID
    pub async fn get_map(&self, map_id: i32) -> Option<Map> {
        let maps = self.maps.read().await;
        maps.get(&map_id).cloned()
    }

    /// Get all maps
    pub async fn get_all_maps(&self) -> Vec<Map> {
        let maps = self.maps.read().await;
        maps.values().cloned().collect()
    }

    /// Get zone manager
    pub fn get_zone_manager(&self) -> &ZoneManager {
        &self.zone_manager
    }

    /// Update all maps
    pub async fn update_all_maps(&self) -> Result<(), Box<dyn std::error::Error>> {
        let maps = self.maps.read().await;
        
        for map in maps.values() {
            map.update().await?;
        }
        
        Ok(())
    }
}

// Global map manager
pub static MAP_MANAGER: Lazy<RwLock<MapManager>> = Lazy::new(|| RwLock::new(MapManager::new()));

impl Clone for Map {
    fn clone(&self) -> Self {
        Self {
            map_id: self.map_id,
            map_name: self.map_name.clone(),
            planet_id: self.planet_id,
            planet_name: self.planet_name.clone(),
            tile_id: self.tile_id,
            bg_id: self.bg_id,
            bg_type: self.bg_type,
            r#type: self.r#type,
            map_width: self.map_width,
            map_height: self.map_height,
            tile_map: self.tile_map.clone(),
            tile_top: self.tile_top.clone(),
            zones: Arc::clone(&self.zones),
            way_points: Arc::clone(&self.way_points),
            npcs: Arc::clone(&self.npcs),
            is_active: Arc::clone(&self.is_active),
            last_update: Arc::clone(&self.last_update),
        }
    }
}

impl Clone for MapManager {
    fn clone(&self) -> Self {
        Self {
            maps: Arc::clone(&self.maps),
            zone_manager: self.zone_manager.clone(),
        }
    }
}
