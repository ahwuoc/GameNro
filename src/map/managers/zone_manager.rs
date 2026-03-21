use crate::map::models::zone::ZoneHandle;
use crate::map::models::zone_actor::ZoneActor;
use crate::services::task_utils::TaskUtils;
use dashmap::DashMap;
use std::sync::LazyLock;
use tokio::sync::mpsc;

pub struct ZoneManager {
    zones: DashMap<(i32, i32), ZoneHandle>,
}

impl ZoneManager {
    pub fn new() -> Self {
        Self {
            zones: DashMap::new(),
        }
    }

    pub fn create_zone(&self, map_id: i32, zone_id: i32, max_player: i32) -> anyhow::Result<()> {
        let (tx, rx) = mpsc::channel(1000);
        let public_state = std::sync::Arc::new(tokio::sync::RwLock::new(
            crate::map::models::zone::ZonePublicState::default(),
        ));
        let mut zone = ZoneActor::new(map_id, zone_id, max_player, rx);
        zone.public_state = public_state.clone();

        let handle = ZoneHandle {
            map_id,
            zone_id,
            tx,
            public_state,
        };
        tokio::spawn(zone.run());
        self.zones.insert((map_id, zone_id), handle);
        Ok(())
    }

    pub fn get_zone(&self, map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        self.zones
            .get(&(map_id, zone_id))
            .map(|z| z.value().clone())
    }

    pub fn get_best_zone(&self, map_id: i32) -> Option<ZoneHandle> {
        self.zones
            .iter()
            .filter(|entry| entry.key().0 == map_id)
            .min_by_key(|entry| {
                entry
                    .value()
                    .public_state
                    .try_read()
                    .map(|s| s.player_count)
                    .unwrap_or(i32::MAX)
            })
            .map(|entry| entry.value().clone())
    }

    pub fn get_zones_for_map(&self, map_id: i32) -> Vec<ZoneHandle> {
        self.zones
            .iter()
            .filter(|entry| entry.key().0 == map_id)
            .map(|entry| entry.value().clone())
            .collect()
    }

    pub fn get_total_players_in_map(&self, map_id: i32) -> usize {
        self.zones
            .iter()
            .filter(|entry| entry.key().0 == map_id)
            .map(|entry| {
                entry
                    .value()
                    .public_state
                    .try_read()
                    .map(|s| s.player_count as usize)
                    .unwrap_or(0)
            })
            .sum()
    }

    pub fn get_zone_count_for_map(&self, map_id: i32) -> usize {
        self.zones
            .iter()
            .filter(|entry| entry.key().0 == map_id)
            .count()
    }

    pub fn remove_zone(&self, map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        self.zones.remove(&(map_id, zone_id)).map(|(_, zone)| zone)
    }

    pub fn clear_zones_for_map(&self, map_id: i32) {
        self.zones.retain(|key, _| key.0 != map_id);
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
