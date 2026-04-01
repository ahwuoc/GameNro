//! Core map-change pipeline: change_map_to_zone, exit_current_map, go_to_map

use crate::{
    constant::{cmd::cmd, const_map::TASK_27_0, task_type::TaskType},
    map::{
        map_manager::MAP_MANAGER,
        models::zone::ZoneHandle,
        services::change_map_models::*,
        zone_manager::ZONE_MANAGER,
    },
    network::{message::Message, session::SessionArc},
    player::{
        player::Player,
        player_actor::{message::PlayerMessage, PlayerHandle},
        player_manager::PLAYER_MANAGER,
    },
    services::{task_utils::TaskUtils, ServiceHandles},
    utils,
};

pub struct CoreService;

impl CoreService {
    pub async fn change_map_to_zone(
        player: &mut Player,
        zone: &ZoneHandle,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        session: Option<&SessionArc>,
    ) -> anyhow::Result<ChangeMapResult> {
        let current_map_is_cold = super::utils::is_cold_planet_map(player.map_id);
        let next_map_is_cold = super::utils::is_cold_planet_map(zone.map_id);

        if space_type == SpaceShipType::Auto {
            let actual_space_type = if player.has_tennis_spaceship() {
                SpaceShipType::Tennis
            } else {
                SpaceShipType::Default
            };
            if session.is_some() {
                player.spaceship_id = actual_space_type as i8;
                let healing = super::spaceship::SpaceshipService::handle_healing(player, actual_space_type);
                tracing::debug!("change_map_to_zone: Auto healing for {}: {:?}", player.name, healing);
                super::spaceship::SpaceshipService::arrive(player, SpaceshipSendType::AllPlayersInMap, actual_space_type)?;
            }
        } else {
            player.spaceship_id = space_type as i8;
            if space_type != SpaceShipType::None {
                let healing = super::spaceship::SpaceshipService::handle_healing(player, space_type);
                tracing::debug!("change_map_to_zone: healing for {}: {:?}", player.name, healing);
            }
        }

        Self::exit_current_map(player).await?;
        if let Some(sess) = session {
            sess.transmit(Message::new(cmd::MAP_CLEAR));
        }

        let map_width = MAP_MANAGER.find_by_id(zone.map_id)
            .map(|m| m.info.map_width)
            .unwrap_or(2000);

        let final_x = if x != -1 { x } else { super::utils::calculate_random_x_position(map_width) };
        let final_y = if y != -1 { y } else { super::utils::get_y_physic_in_top(zone.map_id, final_x, 100) };

        tracing::info!("change_map_to_zone: player: {}, coords: ({}, {})", player.name, final_x, final_y);
        player.location.set_position(final_x, final_y);
        Self::go_to_map(player, zone, session).await?;

        if let Some(sess) = session {
            let task_info = Some((TaskUtils::get_id_task(player), TaskUtils::get_task_index(player)));
            let spaceship_id = player.spaceship_id;
            zone.map_info(sess.clone(), player.id, player.location.x, player.location.y, task_info, spaceship_id).await?;
        }

        let cold_planet_effect = if current_map_is_cold != next_map_is_cold && !player.is_boss {
            if !current_map_is_cold && next_map_is_cold {
                Some(ColdPlanetEffect::Entering)
            } else {
                Some(ColdPlanetEffect::Leaving)
            }
        } else {
            None
        };

        tracing::info!("EXITING: change_map_to_zone (player: {}, map: {})", player.name, zone.map_id);
        Ok(ChangeMapResult::Success {
            map_id: zone.map_id,
            zone_id: zone.zone_id,
            x: final_x,
            y: final_y,
            cold_planet_effect,
        })
    }

    pub async fn exit_current_map(player: &mut Player) -> anyhow::Result<()> {
        if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
            let mut msg = Message::new(cmd::PLAYER_LEAVE);
            msg.write_int(player.id as i32)?;
            let _ = ServiceHandles::send_mess_another_not_me_in_map(player, msg);
            zone.remove_player(player.id).await?;
        }
        player.zone_id = 0;
        Ok(())
    }

    pub async fn go_to_map(
        player: &mut Player,
        zone: &ZoneHandle,
        session: Option<&SessionArc>,
    ) -> anyhow::Result<()> {
        tracing::info!("ENTERING: go_to_map (player: {}, map: {})", player.name, zone.map_id);
        player.zone_id = zone.zone_id;
        player.map_id = zone.map_id;
        if let Some(handle) = PLAYER_MANAGER.get(player.id) {
            handle.send_forget(PlayerMessage::TaskAction(TaskType::GoToMap, zone.map_id.to_string()));
            zone.add_player(handle).await?;
        } else {
            anyhow::bail!("PlayerHandle not found for player: {}", player.id);
        }
        tracing::info!("EXITING: go_to_map (player: {})", player.name);
        Ok(())
    }

    pub fn get_available_zone(map_id: i32) -> Option<ZoneHandle> {
        MAP_MANAGER.find_by_id(map_id)?.get_best_zone()
    }

    pub fn get_specific_zone(map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        ZONE_MANAGER.get_zone(map_id, zone_id)
    }
}
