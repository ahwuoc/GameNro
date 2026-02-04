#![allow(dead_code)]
use std::sync::Arc;

use crate::{
    constant::{
        cmd::cmd,
        const_map::{
            GENDER_NAMEC, GENDER_TRAI_DAT, GENDER_XAYDA, MABU_HOME_MAP_ID, TASK_13_0, TASK_15_0,
            TASK_16_0, TASK_18_0, TASK_19_0, TASK_1_0, TASK_20_0, TASK_21_0, TASK_24_0, TASK_27_0,
            TASK_2_0, TASK_3_0, TASK_4_0, TASK_7_0,
        },
    },
    map::{map_manager, models::zone::ZoneHandle, services::change_map_models::*},
    network::message::Message,
    player::player::Player,
    player::player_actor::PlayerHandle,
    services::ServiceHandles,
};

use crate::map::WayPoint;

pub struct ChangeMapService;

impl ChangeMapService {
    pub async fn change_map_to_zone(
        player: &mut Player,
        zone: &ZoneHandle,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        session: Option<&crate::network::session::SessionArc>,
    ) -> anyhow::Result<ChangeMapResult> {
        tracing::info!(
            "ENTERING: change_map_to_zone (player: {}, map: {})",
            player.name,
            zone.map_id
        );
        let current_map_is_cold = Self::is_cold_planet_map(player.map_id);
        let next_map_is_cold = Self::is_cold_planet_map(zone.map_id);
        if space_type == SpaceShipType::Auto {
            let actual_space_type = if player.has_tennis_spaceship() {
                SpaceShipType::Tennis
            } else {
                SpaceShipType::Default
            };
            if let Some(sess) = session {
                Self::spaceship_arrive(sess, SpaceshipSendType::AllPlayersInMap, actual_space_type)
                    .await?;
            }
        }

        Self::exit_map(player).await?;
        let final_x = if x != -1 {
            x
        } else {
            Self::calculate_random_x_position(2000)
        };
        let final_y = y;
        player.location.set_position(final_x, final_y);
        Self::go_to_map(player, zone, session).await?;
        if let Some(sess) = session {
            zone.map_info(sess.clone(), player.id).await?;
        }
        zone.load_another_to_me(player.id).await?;
        zone.load_me_to_another(player.id).await?;
        let cold_planet_effect = if current_map_is_cold != next_map_is_cold && !player.is_boss {
            if !current_map_is_cold && next_map_is_cold {
                Some(ColdPlanetEffect::Entering)
            } else {
                Some(ColdPlanetEffect::Leaving)
            }
        } else {
            None
        };

        tracing::info!(
            "EXITING: change_map_to_zone (player: {}, map: {})",
            player.name,
            zone.map_id
        );
        Ok(ChangeMapResult::Success {
            map_id: zone.map_id,
            zone_id: zone.zone_id,
            x: final_x,
            y: final_y,
            cold_planet_effect,
        })
    }

    /// Checks if a map ID corresponds to a Cold Planet map.
    pub fn is_cold_planet_map(map_id: i32) -> bool {
        matches!(map_id, 105 | 106 | 107 | 108 | 109 | 110)
    }

    /// Sends the capsule travel menu to the player.
    pub fn open_capsule_menu(player: &Player) -> anyhow::Result<()> {
        let destinations = Self::get_capsule_destinations(player);
        let mut msg = Message::new(cmd::CAPSULE_MENU);
        msg.write_byte(destinations.len() as i8)?;

        for (i, destination) in destinations.iter().enumerate() {
            if i == 0 && player.has_previous_capsule_location() {
                msg.write_utf(&format!("Về chỗ cũ: {}", destination.map_name))?;
            } else if Self::is_home_map_name(&destination.map_name) {
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
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<CapsuleChangeResult> {
        let destinations = Self::get_capsule_destinations(player);

        if destination_index < 0 || destination_index >= destinations.len() as i32 {
            return Ok(CapsuleChangeResult::InvalidDestination);
        }

        let destination = &destinations[destination_index as usize];

        let target_zone = if destination_index == 0 && player.has_previous_capsule_location() {
            Self::get_previous_capsule_zone(player)
        } else {
            Self::get_available_zone(destination.map_id)
        };

        match target_zone {
            Some(zone) => {
                player.save_capsule_location(player.map_id, player.zone_id);

                Self::change_map_to_zone(
                    player,
                    &zone,
                    -1,  // Random x
                    100, // Standard y
                    SpaceShipType::None,
                    Some(session),
                )
                .await?;

                Ok(CapsuleChangeResult::Success {
                    map_id: zone.map_id,
                    zone_id: zone.zone_id,
                })
            }
            None => Ok(CapsuleChangeResult::DestinationUnavailable),
        }
    }

    /// Returns a list of available capsule destinations.
    fn get_capsule_destinations(player: &Player) -> Vec<CapsuleDestination> {
        let mut destinations = Vec::new();

        // 1. Home map (Always available)
        let home_map_id = Self::calculate_home_map(player.gender, false);
        if let Some(zone) = Self::get_available_zone(home_map_id) {
            destinations.push(CapsuleDestination {
                map_id: zone.map_id,
                map_name: Self::get_home_map_name(player.gender),
                planet_name: Self::get_planet_name(player.gender),
            });
        }
        destinations
    }

    fn get_previous_capsule_zone(player: &Player) -> Option<ZoneHandle> {
        if let Some((map_id, zone_id)) = player.get_previous_capsule_location() {
            Self::get_specific_zone(map_id, zone_id)
        } else {
            None
        }
    }

    fn is_home_map_name(map_name: &str) -> bool {
        matches!(map_name, "Nhà Broly" | "Nhà Gôhan" | "Nhà Moori")
    }

    fn get_home_map_name(gender: i8) -> String {
        match gender {
            GENDER_TRAI_DAT => "Nhà Gôhan".to_string(),
            GENDER_NAMEC => "Nhà Moori".to_string(),
            GENDER_XAYDA => "Nhà Broly".to_string(),
            _ => "Nhà".to_string(),
        }
    }

    fn get_planet_name(gender: i8) -> String {
        match gender {
            GENDER_TRAI_DAT => "Trái Đất".to_string(),
            GENDER_NAMEC => "Namếc".to_string(),
            GENDER_XAYDA => "Xayda".to_string(),
            _ => "Unknown".to_string(),
        }
    }

    /// Opens the Zone Selection UI for the player.
    pub async fn open_zone_ui(player: &Player) -> anyhow::Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) else {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đổi khu vực trong map này")?;
            player.send_to_client(msg)?;
            return Ok(());
        };

        if Self::is_special_map(zone.map_id) && !player.is_admin {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đổi khu vực trong map này")?;
            player.send_to_client(msg)?;
            return Ok(());
        }

        let zones = Self::get_zones_for_map(zone.map_id);
        let mut msg = Message::new(cmd::OPEN_ZONE_UI);
        msg.write_byte(zones.len() as i8)?;

        for zone in zones {
            msg.write_byte(zone.zone_id as i8)?;
            let (player_count, max_player) = if let Ok(info) = zone.get_zone_info().await {
                (info.current_players as i8, info.max_player as i8)
            } else {
                (0, 5)
            };

            let status = if player_count < 5 {
                0
            } else if player_count < 8 {
                1
            } else {
                2
            };
            msg.write_byte(status)?;
            msg.write_byte(player_count)?;
            msg.write_byte(max_player)?;
            msg.write_byte(0)?;
        }

        player.send_to_client(msg)?;
        Ok(())
    }

    /// Handles the request to change to a specific zone ID within the current map.
    pub async fn change_zone(
        player: &mut Player,
        zone_id: i32,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        let session_opt = Some(session);
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        let Some(current_zone) = zone_manager.get_zone(player.map_id, player.zone_id) else {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đến khu vực này @")?;
            player.send_to_client(msg)?;
            return Ok(());
        };

        if !player.is_admin && !player.is_boss && !Self::can_change_zone_now(player) {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Chưa thể chuyển khu vực lúc này vui lòng chờ")?;
            player.send_to_client(msg)?;
            return Ok(());
        }

        if Self::is_special_map(current_zone.map_id) && !player.is_admin && !player.is_boss {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đến khu vực này")?;
            player.send_to_client(msg)?;
            return Ok(());
        }

        if let Some(target_zone) = Self::get_specific_zone(current_zone.map_id, zone_id) {
            let info = target_zone.get_zone_info().await?;
            if info.current_players >= info.max_player && !player.is_admin && !player.is_boss {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Khu vực này đã đầy")?;
                player.send_to_client(msg)?;
                return Ok(());
            }

            Self::change_map_to_zone(
                player,
                &target_zone,
                player.location.x,
                player.location.y,
                SpaceShipType::None,
                session_opt,
            )
            .await?;
            player.update_zone_change_time();
        } else {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể thực hiện")?;
            player.send_to_client(msg)?;
        }

        Ok(())
    }

    pub async fn change_map_waypoint_handler(
        player: &mut Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        match Self::change_map_waypoint(player) {
            WaypointChangeResult::Success {
                destination_map_id,
                destination_zone_id,
                x,
                y,
            } => {
                tracing::debug!(
                    "DEBUG_WAYPOINT: Player {} found waypoint to map {} zone {} at ({}, {}) for player at ({}, {})",
                    player.name, destination_map_id, destination_zone_id, x, y, player.location.x, player.location.y
                );
                if let Some(zone) = Self::get_specific_zone(destination_map_id, destination_zone_id)
                {
                    Self::exit_map(player).await?;
                    player.location.set_position(x, y);
                    Self::go_to_map(player, &zone, Some(session)).await?;
                    zone.map_info(session.clone(), player.id).await?;
                } else {
                    ServiceHandles::send_message_alert(player, "Lỗi khi chuyển map")?;
                }
            }
            WaypointChangeResult::NoWaypointFound => {
                tracing::debug!(
                    "DEBUG_WAYPOINT: No waypoint found for player {} at ({}, {}) map {}",
                    player.name,
                    player.location.x,
                    player.location.y,
                    player.map_id
                );
                ServiceHandles::send_message_alert(player, "Waypoint not found")?;
            }
            WaypointChangeResult::TaskRequirementNotMet { .. } => {
                ServiceHandles::send_message_alert(player, "Bạn chưa thể đến khu vực này")?;
            }
            WaypointChangeResult::InvalidPlayerZone => {
                ServiceHandles::send_message_alert(player, "Lỗi hệ thống")?;
            }
            WaypointChangeResult::DestinationUnavailable => {
                ServiceHandles::send_message_alert(player, "Khu vực không khả dụng")?;
            }
        }
        Ok(())
    }

    pub async fn go_home_handler(
        player: &mut Player,
        session: &crate::network::session::SessionArc,
    ) -> anyhow::Result<()> {
        match Self::go_home(player) {
            GoHomeResult::Success {
                home_map_id,
                zone_id,
                x,
                y,
                space_type,
            } => {
                if let Some(target_zone) = Self::get_specific_zone(home_map_id, zone_id) {
                    Self::change_map_to_zone(
                        player,
                        &target_zone,
                        -1,
                        -1,
                        space_type,
                        Some(session),
                    )
                    .await?;
                }
            }
            GoHomeResult::NoAvailableZone => {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Không thể về nhà lúc này")?;
                session.transmit(msg);
            }
            GoHomeResult::PlayerIsBoss => {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Boss không thể sử dụng chức năng này")?;
                session.transmit(msg);
            }
        }

        Ok(())
    }

    fn get_zones_for_map(map_id: i32) -> Vec<ZoneHandle> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(map_id) {
            return map.get_all_zones();
        }
        Vec::new()
    }

    fn is_special_map(_map_id: i32) -> bool {
        false
    }

    fn can_change_zone_now(_player: &Player) -> bool {
        true
    }

    pub fn change_map_waypoint(player: &mut Player) -> WaypointChangeResult {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if zone_manager
            .get_zone(player.map_id, player.zone_id)
            .is_none()
        {
            return WaypointChangeResult::InvalidPlayerZone;
        }

        let Some(wp) = Self::get_waypoint_at_player_position(player) else {
            return WaypointChangeResult::NoWaypointFound;
        };

        if !Self::check_task_requirement(player, wp.go_map) {
            let required_task_id = Self::get_required_task_id(wp.go_map);
            return WaypointChangeResult::TaskRequirementNotMet { required_task_id };
        }

        if let Some(zone) = Self::get_available_zone(wp.go_map) {
            WaypointChangeResult::Success {
                destination_map_id: wp.go_map,
                destination_zone_id: zone.zone_id,
                x: wp.go_x,
                y: wp.go_y,
            }
        } else {
            WaypointChangeResult::DestinationUnavailable
        }
    }

    fn get_waypoint_at_player_position(player: &Player) -> Option<WayPoint> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(player.map_id) {
            return map.get_waypoint_at_position(player.location.x, player.location.y);
        }
        None
    }

    pub fn get_available_zone(map_id: i32) -> Option<ZoneHandle> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(map_id) {
            return map.get_best_zone();
        }
        None
    }

    pub fn check_task_requirement(player: &Player, map_id: i32) -> bool {
        if player.is_admin {
            return true;
        }

        let required_task_id = Self::get_required_task_id_for_map(map_id);
        if required_task_id == 0 {
            return true;
        }

        player.get_task_id() >= required_task_id
    }

    pub fn get_required_task_id(map_id: i32) -> i32 {
        Self::get_required_task_id_for_map(map_id)
    }

    pub fn get_required_task_id_for_map(map_id: i32) -> i32 {
        match map_id {
            1 | 8 | 15 => TASK_1_0,
            42 | 43 | 44 => TASK_2_0,
            2 | 9 | 16 => TASK_3_0,
            24 | 25 | 26 => TASK_4_0,
            3 | 11 | 17 => TASK_7_0,
            27 | 28 | 31 | 32 | 35 | 36 => TASK_13_0,
            30 | 34 | 38 => TASK_15_0,
            6 | 10 | 19 => TASK_16_0,
            68 | 69 | 70 | 71 | 72 | 64 | 65 => TASK_18_0,
            63 | 66 | 67 | 73 | 74 | 75 | 76 | 77 | 81 | 82 | 83 | 79 => TASK_19_0,
            80 => TASK_20_0,
            102 | 92 | 93 | 94 | 96 => TASK_21_0,
            97 | 98 | 99 | 100 => TASK_24_0,
            105 | 106 | 107 | 108 | 109 | 110 | 103 | 154 => TASK_27_0,
            _ => 0,
        }
    }

    pub fn reset_player_position(player: &mut Player, map_width: i32) {
        let mut x = player.location.x;
        if x >= (map_width - 60) as i16 {
            x = (map_width - 60) as i16;
        } else if x <= 60 {
            x = 60;
        }
        player.location.set_position(x, player.location.y);
    }

    pub async fn exit_map(player: &mut Player) -> anyhow::Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
            tracing::info!(
                "ENTERING: exit_map (player: {}, current_map: {})",
                player.name,
                player.map_id
            );
            let mut msg = Message::new(cmd::PLAYER_LEAVE);
            msg.write_int(player.id as i32)?;
            ServiceHandles::send_mess_another_not_me_in_map(player, msg)?;
            zone.remove_player(player.id).await?;
            tracing::info!("EXITING: exit_map (player: {})", player.name);
        }
        player.zone_id = 0;
        Ok(())
    }

    pub async fn exit_map_actor(player: &mut Player) -> anyhow::Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
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
        session: Option<&crate::network::session::SessionArc>,
    ) -> anyhow::Result<()> {
        tracing::info!(
            "ENTERING: go_to_map (player: {}, target_map: {})",
            player.name,
            zone.map_id
        );
        player.zone_id = zone.zone_id;
        player.map_id = zone.map_id;

        if let Some(handle) = crate::player::player_manager::PLAYER_MANAGER.get(player.id) {
            zone.add_player(handle).await?;
        } else {
            anyhow::bail!("PlayerHandle not found for player: {}", player.id);
        }
        Self::finish_load_map(player, session).await?;
        tracing::info!("EXITING: go_to_map (player: {})", player.name);
        Ok(())
    }

    pub async fn finish_load_map(
        player: &Player,
        session: Option<&crate::network::session::SessionArc>,
    ) -> anyhow::Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
            zone.load_another_to_me(player.id).await?;
            zone.load_me_to_another(player.id).await?;
        }
        Self::send_effect_map_to_me(player)?;
        Self::send_effect_me_to_map(player)?;
        Ok(())
    }

    pub fn send_effect_map_to_me(player: &Player) -> anyhow::Result<()> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) else {
            return Ok(());
        };
        Ok(())
    }

    pub fn send_effect_me_to_map(player: &Player) -> anyhow::Result<()> {
        Ok(())
    }

    pub fn calculate_home_map(gender: i8, is_in_mabu_map: bool) -> i32 {
        if is_in_mabu_map {
            MABU_HOME_MAP_ID
        } else {
            (gender as i32) + 21
        }
    }
    pub fn is_mabu_map(map_id: i32) -> bool {
        matches!(map_id, 114 | 115 | 117 | 118 | 119 | 120)
    }
    pub fn go_home(player: &mut Player) -> GoHomeResult {
        let is_in_mabu = Self::is_mabu_map(player.map_id);
        let home_map_id = Self::calculate_home_map(player.gender, is_in_mabu);
        let zone = Self::get_available_zone(home_map_id);

        match zone {
            Some(target_zone) => {
                let space_type = if player.has_tennis_spaceship() {
                    SpaceShipType::Tennis
                } else {
                    SpaceShipType::Default
                };
                let x = Self::calculate_random_x_position(300);

                GoHomeResult::Success {
                    home_map_id,
                    zone_id: target_zone.zone_id,
                    x,
                    y: 5,
                    space_type,
                }
            }
            None => GoHomeResult::NoAvailableZone,
        }
    }

    /// Moves player to a specific map via spaceship.
    pub fn change_map_by_spaceship(
        player: &mut Player,
        map_id: i32,
        zone_id: i32,
        x: i16,
    ) -> SpaceshipTravelResult {
        let target_zone = if zone_id == -1 {
            Self::get_available_zone(map_id)
        } else {
            Self::get_specific_zone(map_id, zone_id)
        };

        let Some(zone) = target_zone else {
            return SpaceshipTravelResult::NoAvailableZone;
        };

        let space_type = if player.has_tennis_spaceship() {
            SpaceShipType::Tennis
        } else {
            SpaceShipType::Default
        };

        let final_x = if x != -1 {
            x
        } else {
            Self::calculate_random_x_position(2000)
        };

        let healing_result = Self::handle_spaceship_healing(player, space_type);

        player.location.set_position(final_x, 5);
        player.map_id = zone.map_id;
        player.zone_id = zone.zone_id;

        SpaceshipTravelResult::Success {
            map_id: zone.map_id,
            zone_id: zone.zone_id,
            x: final_x,
            y: 5,
            space_type,
            healing_result,
        }
    }

    fn handle_spaceship_healing(
        player: &mut Player,
        space_type: SpaceShipType,
    ) -> SpaceshipHealingResult {
        let was_dead = player.is_die();

        if was_dead {
            if space_type == SpaceShipType::Tennis {
                player.n_point.hp_current = player.n_point.hp_max;
                player.n_point.mp_current = player.n_point.mp_max;
                player.dead_flag = false;
                SpaceshipHealingResult::RevivedFullHp
            } else {
                player.n_point.hp_current = 1;
                player.n_point.mp_current = 1;
                player.dead_flag = false;
                SpaceshipHealingResult::RevivedMinimalHp
            }
        } else if space_type == SpaceShipType::Tennis {
            player.n_point.hp_current = player.n_point.hp_max;
            player.n_point.mp_current = player.n_point.mp_max;
            SpaceshipHealingResult::HealedToFull
        } else {
            SpaceshipHealingResult::NoHealing
        }
    }

    /// Sends spaceship arrival effect to players.
    pub async fn spaceship_arrive(
        session: &crate::network::session::SessionArc,
        send_type: SpaceshipSendType,
        space_type: SpaceShipType,
    ) -> anyhow::Result<()> {
        let player_id = {
            let state = session.state.read().await;
            state
                .player_id
                .ok_or_else(|| anyhow::anyhow!("No player ID"))?
        };
        let mut msg = Message::new(cmd::SPACESHIP_ARRIVE);
        msg.write_int(player_id as i32)?;
        msg.write_byte(space_type as i8)?;

        let response = msg.clone();

        match send_type {
            SpaceshipSendType::AllPlayersInMap => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    let map_id = snapshot.map_id;
                    let zone_id = snapshot.zone_id;

                    let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                    if let Some(zone) = zone_manager.get_zone(map_id, zone_id) {
                        zone.broadcast(response);
                    }
                }
            }
            SpaceshipSendType::SelfOnly => {
                session.transmit(msg);
            }
            SpaceshipSendType::OthersInMap => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    let map_id = snapshot.map_id;
                    let zone_id = snapshot.zone_id;
                    let player_id = snapshot.id;

                    let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                    if let Some(zone) = zone_manager.get_zone(map_id, zone_id) {
                        zone.broadcast_except(response, player_id);
                    }
                }
            }
        }
        Ok(())
    }

    fn get_specific_zone(map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        zone_manager.get_zone(map_id, zone_id)
    }

    fn calculate_random_x_position(map_width: i32) -> i16 {
        use std::time::SystemTime;
        let usable = (map_width.max(200) - 200) as u32;
        let seed = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u32;
        (100 + (seed % usable)) as i16
    }

    pub fn check_map_can_join(player: &Player, zone: &ZoneHandle) -> MapAccessResult {
        if zone.map_id == -1 {
            return MapAccessResult::InvalidZone;
        }

        if player.is_boss || player.is_admin {
            return MapAccessResult::Allowed;
        }
        let required_task_id = Self::get_required_task_id_for_map(zone.map_id);
        if required_task_id > 0 && player.get_task_id() < required_task_id {
            return MapAccessResult::TaskRequirementNotMet { required_task_id };
        }
        if let Some(result) = Self::check_gender_restriction(player, zone.map_id) {
            return result;
        }

        MapAccessResult::Allowed
    }

    fn check_gender_restriction(player: &Player, map_id: i32) -> Option<MapAccessResult> {
        match player.gender {
            GENDER_TRAI_DAT => {
                if map_id == 22 || map_id == 23 {
                    return Some(MapAccessResult::GenderRestricted {
                        player_gender: player.gender,
                        allowed_gender: (map_id - 21) as i8,
                    });
                }
            }
            GENDER_NAMEC => {
                if map_id == 21 || map_id == 23 {
                    return Some(MapAccessResult::GenderRestricted {
                        player_gender: player.gender,
                        allowed_gender: (map_id - 21) as i8,
                    });
                }
            }
            GENDER_XAYDA => {
                if map_id == 21 || map_id == 22 {
                    return Some(MapAccessResult::GenderRestricted {
                        player_gender: player.gender,
                        allowed_gender: (map_id - 21) as i8,
                    });
                }
            }
            _ => {}
        }
        None
    }

    pub fn is_home_map(map_id: i32) -> bool {
        matches!(map_id, 21 | 22 | 23)
    }

    pub fn get_home_map_gender(map_id: i32) -> Option<i8> {
        match map_id {
            21 => Some(GENDER_TRAI_DAT),
            22 => Some(GENDER_NAMEC),
            23 => Some(GENDER_XAYDA),
            _ => None,
        }
    }
}
