#![allow(dead_code)]
use crate::entities::map_template::Model as MapTemplate;
use crate::entities::mob_template::Model as MobTemplate;
use crate::map::tile_loader::TileLoader;
use crate::map::zone::Zone;
use crate::map::zone_manager::ZoneManager;
use crate::mob::{mob_template_manager, RtMob};
use chrono::{DateTime, Utc};
use sea_orm::prelude::*;
use serde_json::Value;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

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
    pub mobs: Vec<(i32, i32, i32, i32, i32)>,
    pub npcs: Vec<(i32, i16, i16)>,
}

impl MapInfo {
    pub fn from_template(template: &MapTemplate) -> Self {
        let mut waypoints = Vec::new();
        if !template.waypoints.is_empty() {
            let cleaned = template
                .waypoints
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
        }

        let mut mobs = Vec::new();
        if !template.mobs.is_empty() {
            let outer_json: serde_json::Value = match serde_json::from_str(&template.mobs) {
                Ok(v) => v,
                Err(_) => {
                    let cleaned = template.mobs.replace('\"', "");
                    serde_json::from_str(&cleaned).unwrap_or(Value::Array(vec![]))
                }
            };

            if let Some(arr) = outer_json.as_array() {
                for element in arr {
                    let inner_value = match element {
                        Value::String(s) => serde_json::from_str::<Value>(s).ok(),
                        _ => Some(element.clone()),
                    };

                    if let Some(val) = inner_value {
                        // Try parsing as object keys
                        let t = val
                            .get("template")
                            .and_then(|v| v.as_i64())
                            .map(|v| v as i32);
                        let l = val.get("level").and_then(|v| v.as_i64()).map(|v| v as i32);
                        let h = val.get("hp").and_then(|v| v.as_i64()).map(|v| v as i32);
                        let x = val.get("x").and_then(|v| v.as_i64()).map(|v| v as i32);
                        let y = val.get("y").and_then(|v| v.as_i64()).map(|v| v as i32);

                        if let (Some(temp), Some(level), Some(hp), Some(x), Some(y)) =
                            (t, l, h, x, y)
                        {
                            mobs.push((temp, level, hp, x, y));
                            continue;
                        }

                        if let Some(ma) = val.as_array() {
                            if ma.len() >= 5 {
                                let temp = ma[0].as_i64().unwrap_or(0) as i32;
                                let level = ma[1].as_i64().unwrap_or(1) as i32;
                                let hp = ma[2].as_i64().unwrap_or(0) as i32;
                                let x = ma[3].as_i64().unwrap_or(0) as i32;
                                let y = ma[4].as_i64().unwrap_or(0) as i32;
                                mobs.push((temp, level, hp, x, y));
                            }
                        }
                    }
                }
            }
        }

        let mut npcs = Vec::new();
        if !template.npcs.is_empty() {
            let cleaned = template.npcs.replace('\"', "");
            if let Ok(json) = serde_json::from_str::<Value>(&cleaned) {
                if let Some(arr) = json.as_array() {
                    for nv in arr {
                        match nv {
                            Value::Array(a) => {
                                if a.len() >= 3 {
                                    let id = a[0].as_i64().unwrap_or(0) as i32;
                                    let x = a[1].as_i64().unwrap_or(0) as i16;
                                    let y = a[2].as_i64().unwrap_or(0) as i16;
                                    npcs.push((id, x, y));
                                }
                            }
                            Value::String(s) => {
                                if let Ok(val) = serde_json::from_str::<Value>(s) {
                                    if let Some(a) = val.as_array() {
                                        if a.len() >= 3 {
                                            let id = a[0].as_i64().unwrap_or(0) as i32;
                                            let x = a[1].as_i64().unwrap_or(0) as i16;
                                            let y = a[2].as_i64().unwrap_or(0) as i16;
                                            npcs.push((id, x, y));
                                        }
                                    }
                                }
                            }
                            _ => {}
                        }
                    }
                }
            }
        }

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
            planet_id: template.planet_id as i32,
            planet_name: format!("Planet {}", template.planet_id),
            tile_id: template.tile_id as i32,
            bg_id: template.bg_id as i32,
            bg_type: template.bg_type as i32,
            r#type: template.r#type as i32,
            zone_count: template.zones as i32,
            max_player: template.max_player as i32,
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
    pub zones: Arc<RwLock<Vec<Zone>>>,
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

    pub async fn init_zones(&self, zone_manager: &ZoneManager) -> anyhow::Result<()> {
        let n_zones = self.info.zone_count.max(1);
        let max_player = self.info.max_player.max(1);
        let mut zones = self.zones.write().await;
        for i in 0..n_zones {
            zone_manager
                .create_zone(self.info.id, i, max_player)
                .await?;
            let zone = Zone::new(self.info.id, i, max_player);
            zones.push(zone);
        }
        Ok(())
    }

    pub async fn init_mobs(&self) -> anyhow::Result<()> {
        let zones = self.zones.read().await;
        for (zone_index, zone) in zones.iter().enumerate() {
            for (idx, (temp_id, level, hp, x, y)) in self.info.mobs.iter().cloned().enumerate() {
                if let Some(template) = mob_template_manager::get(temp_id as i8) {
                    let mut mob = RtMob::from_template(template.clone(), idx as u64);
                    mob.set_location(
                        self.info.id,
                        zone_index.try_into().unwrap(),
                        x as i16,
                        y as i16,
                    );
                    if level > 0 {
                        mob.level = level as i8;
                    }
                    if hp > 0 {
                        mob.max_hp = hp;
                        mob.hp = hp;
                    }
                    zone.add_mob(mob).await?;
                }
            }
        }
        Ok(())
    }

    pub async fn init_npcs(
        &self,
        _npc_ids: &[i32],
        _npc_x: &[i16],
        _npc_y: &[i16],
    ) -> anyhow::Result<()> {
        // NPCs are now part of MapInfo and handled in Zone::map_info
        Ok(())
    }

    pub async fn add_waypoint(&self, _wp: WayPoint) {
        // Waypoints are now part of MapInfo
    }

    /// Get waypoint at position
    pub fn get_waypoint_at_position(&self, x: i16, y: i16) -> Option<WayPoint> {
        for waypoint in self.info.waypoints.iter() {
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

    pub async fn update(&self) -> anyhow::Result<()> {
        let zones = self.zones.read().await;

        for zone in zones.iter() {
            zone.update().await?;
        }

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
