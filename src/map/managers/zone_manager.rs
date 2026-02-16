use crate::map::models::zone::{Zone, ZoneHandle};
use crate::services::task_utils::TaskUtils;
use dashmap::DashMap;
use std::sync::LazyLock;
use tokio::sync::mpsc;

pub struct ZoneManager {
    zones: DashMap<String, ZoneHandle>,
}

impl ZoneManager {
    pub fn new() -> Self {
        Self {
            zones: DashMap::new(),
        }
    }

    pub fn create_zone(&self, map_id: i32, zone_id: i32, max_player: i32) -> anyhow::Result<()> {
        let zone_key = format!("{}_{}", map_id, zone_id);
        let (tx, rx) = mpsc::channel(1000);
        let zone = Zone::new(map_id, zone_id, max_player, rx);
        let handle = ZoneHandle {
            map_id,
            zone_id,
            tx,
        };
        tokio::spawn(zone.run());
        self.zones.insert(zone_key, handle);
        Ok(())
    }

    pub fn get_zone(&self, map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        let key = format!("{}_{}", map_id, zone_id);
        self.zones.get(&key).map(|z| z.value().clone())
    }

    pub fn get_best_zone(&self, map_id: i32) -> Option<ZoneHandle> {
        let prefix = format!("{}_", map_id);

        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .min_by_key(|entry| {
                // This is a bit tricky now since we can't easily get the player count synchronously
                // For now, we return based on zone_id order or just any available
                // In a real actor system, you'd probably maintain a local cache of counts
                0
            })
            .map(|entry| entry.value().clone())
    }

    pub fn get_zones_for_map(&self, map_id: i32) -> Vec<ZoneHandle> {
        let prefix = format!("{}_", map_id);
        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .map(|entry| entry.value().clone())
            .collect()
    }

    pub fn get_total_players_in_map(&self, _map_id: i32) -> usize {
        // This should be tracked elsewhere or we need to await multiple handles
        0
    }

    pub fn get_zone_count_for_map(&self, map_id: i32) -> usize {
        let prefix = format!("{}_", map_id);
        self.zones
            .iter()
            .filter(|entry| entry.key().starts_with(&prefix))
            .count()
    }

    pub fn remove_zone(&self, map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        let zone_key = format!("{}_{}", map_id, zone_id);
        self.zones.remove(&zone_key).map(|(_, zone)| zone)
    }

    pub fn clear_zones_for_map(&self, map_id: i32) {
        let prefix = format!("{}_", map_id);
        self.zones.retain(|key, _| !key.starts_with(&prefix));
    }

    pub fn get_all_zones(&self) -> Vec<ZoneHandle> {
        self.zones
            .iter()
            .map(|entry| entry.value().clone())
            .collect()
    }

    pub fn get_zone_count(&self) -> usize {
        self.zones.len()
    }

    pub async fn load_player_to_best_zone(
        &self,
        player: crate::player::player::Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        if let Some(zone) = self.get_best_zone(player.map_id as i32) {
            let player_id = player.id;
            if let Some(handle) = crate::player::player_manager::PLAYER_MANAGER.get(player_id) {
                zone.add_player(handle).await?;
            } else {
                anyhow::bail!("PlayerHandle not found for player_id: {}", player_id);
            }
            zone.load_another_to_me(player_id).await?;
            zone.load_me_to_another(player_id).await?;
            let task_info = Some((
                TaskUtils::get_id_task(&player),
                TaskUtils::get_task_index(&player),
            ));
            let spaceship_id = player.spaceship_id;
            zone.map_info(
                session.clone(),
                player_id,
                player.location.x,
                player.location.y,
                task_info,
                spaceship_id,
            )
            .await?;
        }
        Ok(())
    }
}

pub static ZONE_MANAGER: LazyLock<ZoneManager> = LazyLock::new(|| ZoneManager::new());
