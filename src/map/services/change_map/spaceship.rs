//! Spaceship healing and arrive effects

use crate::map::services::change_map_models::{SpaceShipType, SpaceshipHealingResult, SpaceshipSendType};
use crate::player::player::Player;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::constant::cmd::cmd;
use crate::network::message::Message;

pub struct SpaceshipService;

impl SpaceshipService {
    pub fn handle_healing(player: &mut Player, space_type: SpaceShipType) -> SpaceshipHealingResult {
        let was_dead = player.is_die();
        tracing::debug!(
            "handle_spaceship_healing: player={}, dead={}, space_type={:?}",
            player.name, was_dead, space_type
        );

        if was_dead {
            if space_type == SpaceShipType::Tennis {
                player.n_point.set_full_hp_mp();
                player.revive();
                let _ = crate::services::player_service::send_message_hs_char(player);
                tracing::info!("Revived {} with full HP/MP (Tennis)", player.name);
                SpaceshipHealingResult::RevivedFullHp
            } else {
                player.n_point.set_hp(1);
                player.n_point.set_mp(1);
                player.revive();
                let _ = crate::services::player_service::send_message_hs_char(player);
                tracing::info!("Revived {} with 1 HP/MP", player.name);
                SpaceshipHealingResult::RevivedMinimalHp
            }
        } else if space_type == SpaceShipType::Tennis {
            player.n_point.set_full_hp_mp();
            let _ = crate::services::player_info_service::send_point_info_sync(player);
            tracing::info!("Healed {} to full HP/MP (Tennis)", player.name);
            SpaceshipHealingResult::HealedToFull
        } else {
            tracing::debug!("No healing needed for {}", player.name);
            SpaceshipHealingResult::NoHealing
        }
    }

    pub fn arrive(
        player: &Player,
        send_type: SpaceshipSendType,
        space_type: SpaceShipType,
    ) -> anyhow::Result<()> {
        let mut msg = Message::new(cmd::SPACESHIP_ARRIVE);
        msg.write_int(player.id as i32)?;
        msg.write_byte(space_type as i8)?;

        match send_type {
            SpaceshipSendType::AllPlayersInMap => {
                if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
                    zone.broadcast(msg);
                }
            }
            SpaceshipSendType::SelfOnly => {
                if let Some(ref session) = player.session {
                    session.transmit(msg);
                }
            }
            SpaceshipSendType::OthersInMap => {
                if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
                    zone.broadcast_except(msg, player.id);
                }
            }
        }
        Ok(())
    }
}
