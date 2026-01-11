use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::Zone;
use crate::network::session::AsyncSession;
use crate::player::Player;

pub struct ZoneService;

impl ZoneService {
    pub async fn load_player_to_best_zone(
        player: Player,
        session: &mut AsyncSession,
    ) -> anyhow::Result<()> {
        let zone_manager = ZONE_MANAGER.read().await;
        if let Some(zone) = zone_manager.get_best_zone(player.map_id as i32).await {
            zone.load_player_to_zone(player, session).await?;
        }
        Ok(())
    }
    pub async fn create_zone(map_id: i32, zone_id: i32, max_player: i32) -> anyhow::Result<()> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.create_zone(map_id, zone_id, max_player).await
    }
    pub async fn get_zone(map_id: i32, zone_id: i32) -> Option<Zone> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.get_zone(map_id, zone_id).await
    }

    /// Get best zone for a map
    pub async fn get_best_zone(map_id: i32) -> Option<Zone> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.get_best_zone(map_id).await
    }

    /// Send message to all players in a map
    pub async fn send_message_to_all_players_in_map(
        map_id: i32,
        msg: crate::network::message::Message,
    ) -> anyhow::Result<()> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager
            .send_message_to_all_players_in_map(map_id, msg)
            .await
    }

    pub async fn send_message_to_other_players_in_map(
        map_id: i32,
        except_player_id: u64,
        msg: crate::network::message::Message,
    ) -> anyhow::Result<()> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager
            .send_message_to_other_players_in_map(map_id, except_player_id, msg)
            .await
    }
    pub async fn get_total_players_in_map(map_id: i32) -> usize {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.get_total_players_in_map(map_id).await
    }
}
