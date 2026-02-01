#![allow(dead_code)]
use crate::map::Zone;
use dashmap::DashMap;
use once_cell::sync::Lazy;
use std::sync::Arc;

pub struct ZoneManager {
    zones: DashMap<String, Arc<Zone>>,
}

impl ZoneManager {
    pub fn new() -> Self {
        Self {
            zones: DashMap::new(),
        }
    }

    pub fn create_zone(&self, map_id: i32, zone_id: i32, max_player: i32) -> anyhow::Result<()> {
        let zone_key = format!("{}_{}", map_id, zone_id);
        let zone = Arc::new(Zone::new(map_id, zone_id, max_player));
        self.zones.insert(zone_key, zone);
        Ok(())
    }

    pub fn get_zone(&self, map_id: i32, zone_id: i32) -> Option<Arc<Zone>> {
        let key = format!("{}_{}", map_id, zone_id);
        self.zones.get(&key).map(|z| Arc::clone(z.value()))
    }

    pub fn get_best_zone(&self, map_id: i32) -> Option<Arc<Zone>> {
        let prefix = format!("{}_", map_id);
        
        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .filter(|entry| {
                let zone = entry.value();
                zone.get_num_players() < zone.max_player as usize
            })
            .min_by_key(|entry| entry.value().get_num_players())
            .map(|entry| Arc::clone(entry.value()))
    }

    pub fn get_zones_for_map(&self, map_id: i32) -> Vec<Arc<Zone>> {
        let prefix = format!("{}_", map_id);
        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .map(|entry| Arc::clone(entry.value()))
            .collect()
    }

    pub fn get_total_players_in_map(&self, map_id: i32) -> usize {
        let prefix = format!("{}_", map_id);
        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .map(|entry| entry.value().get_num_players())
            .sum()
    }

    pub fn get_zone_count_for_map(&self, map_id: i32) -> usize {
        let prefix = format!("{}_", map_id);
        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .count()
    }

    pub fn remove_zone(&self, map_id: i32, zone_id: i32) -> Option<Arc<Zone>> {
        let zone_key = format!("{}_{}", map_id, zone_id);
        self.zones.remove(&zone_key).map(|(_, zone)| zone)
    }

    pub fn clear_zones_for_map(&self, map_id: i32) {
        let prefix = format!("{}_", map_id);
        self.zones.retain(|key, _| !key.starts_with(&prefix));
    }

    pub fn get_all_zones(&self) -> Vec<Arc<Zone>> {
        self.zones
            .iter()
            .map(|entry| Arc::clone(entry.value()))
            .collect()
    }

    pub fn get_zone_count(&self) -> usize {
        self.zones.len()
    }

    pub fn load_player_to_best_zone(
        &self,
        player: crate::player::Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        if let Some(zone) = self.get_best_zone(player.map_id as i32) {
            zone.load_player_to_zone(player, session)?;
        }
        Ok(())
    }
}

pub static ZONE_MANAGER: Lazy<ZoneManager> = Lazy::new(|| ZoneManager::new());
