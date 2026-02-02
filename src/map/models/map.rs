use crate::entities::map_template::Model as MapTemplate;
use crate::map::models::zone::ZoneHandle;
use crate::map::tile_loader::TileLoader;
pub use crate::map::waypoint::WayPoint;
use crate::map::zone_manager::ZoneManager;
use crate::mob::RtMob;
use crate::templates::mob_template_manager;
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use std::sync::Arc;
use std::sync::RwLock;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MobSpawn {
    pub temp_id: i32,
    pub level: i32,
    pub hp: i32,
    pub x: i16,
    pub y: i16,
    #[serde(default = "default_status")]
    pub status: i8,
}

fn default_status() -> i8 {
    5
}

impl MobSpawn {
    pub fn parse(json_str: &str) -> Vec<Self> {
        if json_str.is_empty() {
            return Vec::new();
        }
        serde_json::from_str(json_str).unwrap_or_default()
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct NpcSpawn {
    pub temp_id: i32,
    pub x: i16,
    pub y: i16,
    #[serde(skip)]
    pub location: crate::utils::Location,
}

impl NpcSpawn {
    pub fn parse(json_str: &str) -> Vec<Self> {
        if json_str.is_empty() {
            return Vec::new();
        }
        let mut npcs: Vec<Self> = serde_json::from_str(json_str).unwrap_or_default();
        for npc in npcs.iter_mut() {
            npc.location.x = npc.x;
            npc.location.y = npc.y;
        }
        npcs
    }
}

#[derive(Debug, Clone)]
pub struct MapInfo {
    pub id: i32,
    pub name: String,
    pub planet_id: i32,
    pub planet_name: String,
    pub tile_id: i32,
    pub bg_id: i32,
    pub bg_type: i32,
    pub r#type: i32,
    pub zone_count: i32,
    pub max_player: i32,
    pub map_width: i32,
    pub map_height: i32,
    pub tile_map: Vec<Vec<i32>>,
    pub tile_top: Vec<i32>,
    pub waypoints: Vec<WayPoint>,
    pub mobs: Vec<MobSpawn>,
    pub npcs: Vec<NpcSpawn>,
}

impl MapInfo {
    pub fn from_template(template: &MapTemplate) -> Self {
        let waypoints = WayPoint::parse(&template.waypoints);
        let mobs = MobSpawn::parse(&template.mobs);
        let npcs = NpcSpawn::parse(&template.npcs);

        let (map_width, map_height, tile_map) =
            if let Some((w, h, tiles)) = TileLoader::read_tile_map_file(template.id) {
                (w, h, tiles)
            } else {
                (0, 0, Vec::new())
            };

        let tile_top = TileLoader::read_tile_top_file(template.tile_id).unwrap_or_default();

        Self {
            id: template.id,
            name: template.name.clone(),
            planet_id: template.planet_id,
            planet_name: format!("Planet {}", template.planet_id),
            tile_id: template.tile_id,
            bg_id: template.bg_id,
            bg_type: template.bg_type,
            r#type: template.r#type,
            zone_count: template.zones,
            max_player: template.max_player,
            map_width,
            map_height,
            tile_map,
            tile_top,
            waypoints,
            mobs,
            npcs,
        }
    }
}

pub struct Map {
    pub info: Arc<MapInfo>,
    pub zones: Arc<RwLock<Vec<ZoneHandle>>>,
    pub is_active: Arc<RwLock<bool>>,
    pub last_update: Arc<RwLock<DateTime<Utc>>>,
}

impl Map {
    pub fn from_template(template: &MapTemplate) -> Self {
        let current_time = Utc::now();
        let info = Arc::new(MapInfo::from_template(template));

        Self {
            info,
            zones: Arc::new(RwLock::new(Vec::new())),
            is_active: Arc::new(RwLock::new(true)),
            last_update: Arc::new(RwLock::new(current_time)),
        }
    }

    pub fn init_zones(&self, zone_manager: &ZoneManager) -> anyhow::Result<()> {
        let n_zones = self.info.zone_count.max(1);
        let max_player = self.info.max_player.max(1);
        let mut zones = self.zones.write().unwrap();
        for i in 0..n_zones {
            zone_manager
                .create_zone(self.info.id, i, max_player)
                .unwrap();
            if let Some(zone) = zone_manager.get_zone(self.info.id, i) {
                zones.push(zone);
            }
        }
        Ok(())
    }

    pub async fn init_mobs(&self) -> anyhow::Result<()> {
        let zones = self.zones.read().unwrap();
        for (zone_index, zone) in zones.iter().enumerate() {
            for (idx, mob) in self.info.mobs.iter().enumerate() {
                if let Some(template) = mob_template_manager::get(mob.temp_id as i8) {
                    let mut rt_mob = RtMob::from_template(template.clone(), idx as u64);
                    rt_mob.set_location(self.info.id, zone_index.try_into().unwrap(), mob.x, mob.y);
                    rt_mob.spawn_status = mob.status;
                    rt_mob.status = mob.status;
                    if mob.level > 0 {
                        rt_mob.level = mob.level as i8;
                    }
                    if mob.hp > 0 {
                        rt_mob.max_hp = mob.hp;
                        rt_mob.hp = mob.hp;
                    }
                    zone.add_mob(rt_mob).await?;
                }
            }
        }
        Ok(())
    }

    pub fn init_npcs(
        &self,
        _npc_ids: &[i32],
        _npc_x: &[i16],
        _npc_y: &[i16],
    ) -> anyhow::Result<()> {
        Ok(())
    }

    pub fn get_waypoint_at_position(&self, x: i16, y: i16) -> Option<WayPoint> {
        for waypoint in self.info.waypoints.iter() {
            if waypoint.contains_position(x, y) {
                return Some(waypoint.clone());
            }
        }

        let tolerance = 60i16;
        for waypoint in self.info.waypoints.iter() {
            if x >= waypoint.min_x - tolerance
                && x <= waypoint.max_x + tolerance
                && y >= waypoint.min_y - tolerance
                && y <= waypoint.max_y + tolerance
            {
                return Some(waypoint.clone());
            }
        }
        None
    }

    pub fn get_zone(&self, zone_id: i32) -> Option<ZoneHandle> {
        let zones = self.zones.read().unwrap();
        zones.get(zone_id as usize).cloned()
    }

    pub fn get_best_zone(&self) -> Option<ZoneHandle> {
        let zones = self.zones.read().unwrap();

        // Since we can't easily get player count synchronously,
        // return the first zone or implement a better balancing logic later
        zones.first().cloned()
    }

    pub fn get_all_zones(&self) -> Vec<ZoneHandle> {
        let zones = self.zones.read().unwrap();
        zones.clone()
    }

    pub async fn update(&self) -> anyhow::Result<()> {
        // Zones update themselves in their own actors
        let mut last_update = self.last_update.write().unwrap();
        *last_update = Utc::now();
        Ok(())
    }
    pub fn is_active(&self) -> bool {
        let is_active = self.is_active.read().unwrap();
        *is_active
    }

    pub fn set_active(&self, active: bool) {
        let mut is_active = self.is_active.write().unwrap();
        *is_active = active;
    }
}

impl Clone for Map {
    fn clone(&self) -> Self {
        Self {
            info: Arc::clone(&self.info),
            zones: Arc::clone(&self.zones),
            is_active: Arc::clone(&self.is_active),
            last_update: Arc::clone(&self.last_update),
        }
    }
}
