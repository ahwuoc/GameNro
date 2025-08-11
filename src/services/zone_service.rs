use crate::map::Zone;
use crate::player::Player;
use crate::network::async_net::session::AsyncSession;
use crate::map::zone_manager::ZONE_MANAGER;

pub struct ZoneService;

impl ZoneService {
    /// Load player into the best available zone for their map
    pub async fn load_player_to_best_zone(
        player: Player,
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let zone_manager = ZONE_MANAGER.read().await;
        if let Some(zone) = zone_manager.get_best_zone(player.map_id as i32).await {
            zone.load_player_to_zone(player, session).await?;
        }
        Ok(())
    }

    /// Create a new zone for a map
    pub async fn create_zone(map_id: i32, zone_id: i32, max_player: i32) -> Result<(), Box<dyn std::error::Error>> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.create_zone(map_id, zone_id, max_player).await
    }

    /// Get zone by map_id and zone_id
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
        msg: crate::network::async_net::message::Message,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.send_message_to_all_players_in_map(map_id, msg).await
    }

    /// Send message to other players in a map (excluding one player)
    pub async fn send_message_to_other_players_in_map(
        map_id: i32,
        except_player_id: u64,
        msg: crate::network::async_net::message::Message,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.send_message_to_other_players_in_map(map_id, except_player_id, msg).await
    }

    /// Get total players in a map
    pub async fn get_total_players_in_map(map_id: i32) -> usize {
        let zone_manager = ZONE_MANAGER.read().await;
        zone_manager.get_total_players_in_map(map_id).await
    }
}
