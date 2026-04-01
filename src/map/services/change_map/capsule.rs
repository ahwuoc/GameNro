//! Capsule travel: menu display and map transitions

use crate::map::{
    map_manager::MAP_MANAGER,
    models::zone::ZoneHandle,
    services::{change_map_models::*, change_map_service::ChangeMapService, map_service},
    zone_manager::ZONE_MANAGER,
};
use crate::constant::cmd::cmd;
use crate::network::{message::Message, session::SessionArc};
use crate::player::player::Player;

pub struct CapsuleService;

impl CapsuleService {
    pub fn open_capsule_menu(player: &Player) -> anyhow::Result<()> {
        let destinations = Self::get_capsule_destinations(player);
        let mut msg = Message::new(cmd::CAPSULE_MENU);
        msg.write_byte(destinations.len() as i8)?;
        for (i, destination) in destinations.iter().enumerate() {
            if i == 0 && player.has_previous_capsule_location() {
                msg.write_utf(&format!("Về chỗ cũ: {}", destination.map_name))?;
            } else if super::utils::is_home_map_name(&destination.map_name) {
                msg.write_utf("Về nhà")?;
            } else {
                msg.write_utf(&destination.map_name)?;
            }
            msg.write_utf(&destination.planet_name)?;
        }
        player.send_to_client(msg)?;
        Ok(())
    }

    pub async fn change_map_capsule(
        player: &mut Player,
        destination_index: i32,
        session: &SessionArc,
    ) -> anyhow::Result<CapsuleChangeResult> {
        let destinations = Self::get_capsule_destinations(player);
        if destination_index < 0 || destination_index >= destinations.len() as i32 {
            return Ok(CapsuleChangeResult::InvalidDestination);
        }
        let destination = &destinations[destination_index as usize];
        let target_zone = if destination_index == 0 && player.has_previous_capsule_location() {
            Self::get_previous_capsule_zone(player)
        } else {
            super::core::CoreService::get_available_zone(destination.map_id)
        };
        match target_zone {
            Some(zone) => {
                player.save_capsule_location(player.map_id, player.zone_id);
                super::core::CoreService::change_map_to_zone(
                    player, &zone, -1, 5, SpaceShipType::Auto, Some(session),
                ).await?;
                Ok(CapsuleChangeResult::Success { map_id: zone.map_id, zone_id: zone.zone_id })
            }
            None => Ok(CapsuleChangeResult::DestinationUnavailable),
        }
    }

    fn get_capsule_destinations(player: &Player) -> Vec<CapsuleDestination> {
        let mut destinations = Vec::new();
        if let Some((map_id, _)) = player.get_previous_capsule_location() {
            if !matches!(map_id, 21 | 22 | 23) && map_service::is_future_map(map_id) {
                Self::add_list_map_capsule(player, &mut destinations, map_id);
            }
        }
        Self::add_list_map_capsule(player, &mut destinations, 21 + player.gender as i32);
        let map_ids = [47, 45, 0, 7, 14, 5, 20, 13, 24 + player.gender as i32, 27, 19, 79, 84, 154, 52];
        for map_id in map_ids {
            Self::add_list_map_capsule(player, &mut destinations, map_id);
        }
        destinations
    }

    fn add_list_map_capsule(player: &Player, destinations: &mut Vec<CapsuleDestination>, map_id: i32) {
        if destinations.iter().any(|d| d.map_id == map_id) {
            return;
        }
        if ChangeMapService::check_map_access(player, map_id) == MapAccessResult::Allowed {
            if let Some(map) = MAP_MANAGER.find_by_id(map_id) {
                destinations.push(CapsuleDestination {
                    map_id,
                    map_name: map.info.name.clone(),
                    planet_name: map.info.planet_name.clone(),
                });
            }
        }
    }

    fn get_previous_capsule_zone(player: &Player) -> Option<ZoneHandle> {
        let (map_id, zone_id) = player.get_previous_capsule_location()?;
        ZONE_MANAGER.get_zone(map_id, zone_id)
    }
}
