use crate::map::ChangeMapService;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::pet::PetHandle;

pub struct NetworkHandler;

impl NetworkHandler {
    pub async fn handle_network_command(
        player: &mut Player,
        session: &SessionArc,
        pet_handle: &Option<PetHandle>,
        mut msg: Message,
    ) -> anyhow::Result<()> {
        let command = msg.command;
        
        match command {
            crate::constant::cmd::cmd::ATTACK_MOB => {
                let mob_id = msg.read_byte()? as i32;
                crate::player::player_actor::handlers::combat::CombatHandler::handle_attack_mob(
                    player, mob_id
                ).await;
            }
            crate::constant::cmd::cmd::CHANGE_MAP_WAYPOINT
            | crate::constant::cmd::cmd::CHANGE_MAP_WAYPOINT_ALT => {
                crate::player::player_actor::handlers::map::MapHandler::prepare_for_map_change(player.id);
                ChangeMapService::change_map_waypoint_handler(player, session).await?;
                crate::player::player_actor::handlers::map::MapHandler::sync_pet_map(player, pet_handle).await;
            }
            crate::constant::cmd::cmd::GO_HOME => {
                crate::player::player_actor::handlers::map::MapHandler::prepare_for_map_change(player.id);
                ChangeMapService::go_home_handler(player, session).await?;
                crate::player::player_actor::handlers::map::MapHandler::sync_pet_map(player, pet_handle).await;
            }
            crate::constant::cmd::cmd::CHANGE_ZONE => {
                let zone_id = msg.read_byte()? as i32;
                crate::player::player_actor::handlers::map::MapHandler::prepare_for_map_change(player.id);
                ChangeMapService::change_zone(player, zone_id, session).await?;
                crate::player::player_actor::handlers::map::MapHandler::sync_pet_map(player, pet_handle).await;
            }
            _ => {
                tracing::warn!("Actor doesn't handle command {} yet", command);
            }
        }
        
        Ok(())
    }
}
