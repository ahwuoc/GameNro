#![allow(dead_code)]
use crate::map::Zone;
use once_cell::sync::Lazy;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

pub struct ZoneManager {
    zones: Arc<RwLock<HashMap<String, Zone>>>,
}

impl ZoneManager {
    pub fn new() -> Self {
        Self {
            zones: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn create_zone(
        &self,
        map_id: i32,
        zone_id: i32,
        max_player: i32,
    ) -> anyhow::Result<()> {
        let zone_key = format!("{}_{}", map_id, zone_id);
        let zone = Zone::new(map_id, zone_id, max_player);
        let mut zones = self.zones.write().await;
        zones.insert(zone_key, zone);
        Ok(())
    }

    pub async fn get_zone(&self, map_id: i32, zone_id: i32) -> Option<Zone> {
        let zone_key = format!("{}_{}", map_id, zone_id);
        let zones = self.zones.read().await;
        zones.get(&zone_key).cloned()
    }

    pub async fn get_best_zone(&self, map_id: i32) -> Option<Zone> {
        let zones = self.zones.read().await;
        let mut best_zone: Option<&Zone> = None;
        let mut min_players = i32::MAX;

        for (key, zone) in zones.iter() {
            if key.starts_with(&format!("{}_", map_id)) {
                let player_count = zone.get_num_players().await as i32;
                if player_count < min_players && player_count < zone.max_player {
                    min_players = player_count;
                    best_zone = Some(zone);
                }
            }
        }

        best_zone.cloned()
    }

    pub async fn get_zones_for_map(&self, map_id: i32) -> Vec<Zone> {
        let zones = self.zones.read().await;

        zones
            .iter()
            .filter(|(key, _)| key.starts_with(&format!("{}_", map_id)))
            .map(|(_, zone)| zone.clone())
            .collect()
    }

    pub async fn send_to_players_in_map(
        &self,
        map_id: i32,
        msg: crate::network::message::Message,
    ) -> anyhow::Result<()> {
        let zones = self.get_zones_for_map(map_id).await;
        for zone in zones.into_iter() {
            zone.send_message_to_all_players(msg.clone()).await?;
        }
        Ok(())
    }

    pub async fn send_to_players_in_map_except(
        &self,
        map_id: i32,
        except_player_id: u64,
        msg: crate::network::message::Message,
    ) -> anyhow::Result<()> {
        let zones = self.get_zones_for_map(map_id).await;
        for zone in zones.into_iter() {
            zone.send_message_to_other_players(except_player_id, msg.clone())
                .await?;
        }
        Ok(())
    }

    pub async fn get_total_players_in_map(&self, map_id: i32) -> usize {
        let zones = self.get_zones_for_map(map_id).await;
        let mut total = 0;
        for zone in zones {
            total += zone.get_num_players().await;
        }
        total
    }

    pub async fn get_zone_count_for_map(&self, map_id: i32) -> usize {
        let zones = self.zones.read().await;
        zones
            .keys()
            .filter(|key| key.starts_with(&format!("{}_", map_id)))
            .count()
    }

    pub async fn remove_zone(&self, map_id: i32, zone_id: i32) -> bool {
        let zone_key = format!("{}_{}", map_id, zone_id);
        let mut zones = self.zones.write().await;
        zones.remove(&zone_key).is_some()
    }

    pub async fn clear_zones_for_map(&self, map_id: i32) {
        let mut zones = self.zones.write().await;
        zones.retain(|key, _| !key.starts_with(&format!("{}_", map_id)));
    }

    pub async fn get_all_zones(&self) -> Vec<Zone> {
        let zones = self.zones.read().await;
        zones.values().cloned().collect()
    }

    pub async fn get_zone_count(&self) -> usize {
        let zones = self.zones.read().await;
        zones.len()
    }

    pub async fn load_player_to_best_zone(
        &self,
        player: crate::player::Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        if let Some(zone) = self.get_best_zone(player.map_id as i32).await {
            zone.load_player_to_zone(player, session).await?;
        }
        Ok(())
    }
}

impl Clone for ZoneManager {
    fn clone(&self) -> Self {
        Self {
            zones: Arc::clone(&self.zones),
        }
    }
}

pub static ZONE_MANAGER: Lazy<RwLock<ZoneManager>> = Lazy::new(|| RwLock::new(ZoneManager::new()));
