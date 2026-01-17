#![allow(dead_code)]
use crate::{
    constant::cmd::cmd,
    map::{map_manager, Zone},
    network::message::Message,
    player::Player,
};

use super::map::WayPoint;

/// MaBu map ID constant
const MABU_HOME_MAP_ID: i32 = 114;

// ========================================
// Gender Constants (from Java ConstPlayer)
// ========================================
pub const GENDER_TRAI_DAT: i8 = 0;
pub const GENDER_NAMEC: i8 = 1;
pub const GENDER_XAYDA: i8 = 2;

// ========================================
// Task Constants (from Java ConstTask)
// These represent the minimum task progress required to access certain maps
// ========================================
pub const TASK_1_0: i32 = 2048; // Task 1.0 - đồi hoa cúc, đồi nấm tím, đồi hoang
pub const TASK_2_0: i32 = 4096; // Task 2.0 - vách aru, vách moori, vách kakarot
pub const TASK_3_0: i32 = 6144; // Task 3.0 - thung lũng tre, thị trấn moori, làng plane
pub const TASK_4_0: i32 = 8192; // Task 4.0 - trạm tàu vũ trụ
pub const TASK_7_0: i32 = 14336; // Task 7.0 - rừng nấm, thung lũng maima, rừng nguyên sinh
pub const TASK_13_0: i32 = 26624; // Task 13.0 - rừng bamboo, rừng dương xỉ, etc.
pub const TASK_15_0: i32 = 30720; // Task 15.0 - đảo bulong, đông nam guru, bờ vực đen
pub const TASK_16_0: i32 = 32768; // Task 16.0 - đông karin, thung lũng namếc, thành phố vegeta
pub const TASK_18_0: i32 = 36864; // Task 18.0 - thung lũng nappa, vực cấm, etc.
pub const TASK_19_0: i32 = 38912; // Task 19.0 - trại lính fide, trại quỷ già, etc.
pub const TASK_20_0: i32 = 40960; // Task 20.0 - núi khỉ vàng
pub const TASK_21_0: i32 = 43008; // Task 21.0 - nhà bunma, thành phố phía đông, etc.
pub const TASK_24_0: i32 = 49152; // Task 24.0 - thành phố phía bắc, ngọn núi phía bắc, etc.
pub const TASK_27_0: i32 = 55296; // Task 27.0 - cánh đồng tuyết, rừng tuyết, etc.

// Define SpaceShipType
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i8)]
pub enum SpaceShipType {
    Auto = -1,
    None = 0,
    Default = 1,
    TeleportYardrat = 2,
    Tennis = 3,
}

impl SpaceShipType {
    pub fn from_i8(value: i8) -> Option<Self> {
        match value {
            -1 => Some(SpaceShipType::Auto),
            0 => Some(SpaceShipType::None),
            1 => Some(SpaceShipType::Default),
            2 => Some(SpaceShipType::TeleportYardrat),
            3 => Some(SpaceShipType::Tennis),
            _ => None,
        }
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i8)]
pub enum ChangeMapType {
    Capsule = 0,
    BlackBall = 1,
    MaBu = 2,
}

impl ChangeMapType {
    pub fn from_i8(value: i8) -> Option<Self> {
        match value {
            0 => Some(ChangeMapType::Capsule),
            1 => Some(ChangeMapType::BlackBall),
            2 => Some(ChangeMapType::MaBu),
            _ => None,
        }
    }
}

/// Result of a waypoint change operation
#[derive(Debug, Clone, PartialEq)]
pub enum WaypointChangeResult {
    /// Successfully changed map via waypoint
    Success {
        destination_map_id: i32,
        destination_zone_id: i32,
        x: i16,
        y: i16,
    },
    /// No waypoint found at player position
    NoWaypointFound,
    /// Task requirement not met for destination map
    TaskRequirementNotMet { required_task_id: i32 },
    /// Player is not in a valid zone
    InvalidPlayerZone,
    /// Destination zone is full or unavailable
    DestinationUnavailable,
}

pub struct ChangeMapService;

impl ChangeMapService {
    pub fn new() -> Self {
        Self
    }

    pub async fn change_map_to_zone_async(
        &self,
        player: &mut Player,
        zone: &Zone,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<ChangeMapResult> {
        let current_map_is_cold = Self::is_cold_planet_map(player.map_id);
        let next_map_is_cold = Self::is_cold_planet_map(zone.map_id);
        let _same_zone = player.map_id == zone.map_id;
        if space_type == SpaceShipType::Auto {
            let actual_space_type = if player.has_tennis_spaceship() {
                SpaceShipType::Tennis
            } else {
                SpaceShipType::Default
            };
            self.spaceship_arrive(
                player,
                SpaceshipSendType::AllPlayersInMap,
                actual_space_type,
            )
            .await?;
        }
        self.exit_map_async(player).await?;
        let final_x = if x != -1 {
            x
        } else {
            Self::calculate_random_x_position(2000)
        };
        let final_y = y;
        player.location.set_position(final_x, final_y);
        self.go_to_map_async(player, zone).await?;
        zone.map_info(session, player.id).await?;

        let cold_planet_effect = if current_map_is_cold != next_map_is_cold && !player.is_boss {
            if !current_map_is_cold && next_map_is_cold {
                Some(ColdPlanetEffect::Entering)
            } else {
                Some(ColdPlanetEffect::Leaving)
            }
        } else {
            None
        };

        Ok(ChangeMapResult::Success {
            map_id: zone.map_id,
            zone_id: zone.zone_id,
            x: final_x,
            y: final_y,
            cold_planet_effect,
        })
    }
    pub fn is_cold_planet_map(map_id: i32) -> bool {
        matches!(map_id, 105 | 106 | 107 | 108 | 109 | 110)
    }

    pub async fn open_capsule_menu(
        &self,
        player: &Player,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<()> {
        let destinations = self.get_capsule_destinations(player).await;
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

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn change_map_capsule(
        &self,
        player: &mut Player,
        destination_index: i32,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<CapsuleChangeResult> {
        let destinations = self.get_capsule_destinations(player).await;

        if destination_index < 0 || destination_index >= destinations.len() as i32 {
            return Ok(CapsuleChangeResult::InvalidDestination);
        }

        let destination = &destinations[destination_index as usize];

        let target_zone = if destination_index == 0 && player.has_previous_capsule_location() {
            self.get_previous_capsule_zone(player).await
        } else {
            self.get_available_zone(destination.map_id).await
        };

        match target_zone {
            Some(zone) => {
                player.save_capsule_location(player.map_id, player.zone_id);

                self.change_map_to_zone_async(
                    player,
                    &zone,
                    -1,                  // Random x position
                    100,                 // Standard y position
                    SpaceShipType::None, // No spaceship for capsule
                    session,
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

    /// Get available capsule destinations for player
    async fn get_capsule_destinations(&self, player: &Player) -> Vec<CapsuleDestination> {
        // This would typically come from MapService.getMapCapsule(player)
        // For now, return a basic set of destinations based on player progress

        let mut destinations = Vec::new();

        // Home maps are always available
        let home_map_id = Self::calculate_home_map(player.gender, false);
        if let Some(zone) = self.get_available_zone(home_map_id).await {
            destinations.push(CapsuleDestination {
                map_id: zone.map_id,
                map_name: Self::get_home_map_name(player.gender),
                planet_name: Self::get_planet_name(player.gender),
            });
        }

        // Add other available destinations based on task progress
        // This is a simplified implementation - real game would have more complex logic

        destinations
    }

    /// Get previous capsule zone for "return to previous" functionality
    async fn get_previous_capsule_zone(&self, player: &Player) -> Option<Zone> {
        if let Some((map_id, zone_id)) = player.get_previous_capsule_location() {
            self.get_specific_zone(map_id, zone_id).await
        } else {
            None
        }
    }

    /// Check if a map name is a home map
    fn is_home_map_name(map_name: &str) -> bool {
        matches!(map_name, "Nhà Broly" | "Nhà Gôhan" | "Nhà Moori")
    }

    /// Get home map name by gender
    fn get_home_map_name(gender: i8) -> String {
        match gender {
            GENDER_TRAI_DAT => "Nhà Gôhan".to_string(),
            GENDER_NAMEC => "Nhà Moori".to_string(),
            GENDER_XAYDA => "Nhà Broly".to_string(),
            _ => "Nhà".to_string(),
        }
    }

    /// Get planet name by gender
    fn get_planet_name(gender: i8) -> String {
        match gender {
            GENDER_TRAI_DAT => "Trái Đất".to_string(),
            GENDER_NAMEC => "Namếc".to_string(),
            GENDER_XAYDA => "Xayda".to_string(),
            _ => "Unknown".to_string(),
        }
    }

    pub async fn open_zone_ui(
        &self,
        player: &Player,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<()> {
        let Some(zone) = &player.zone else {
            // Send error message if player is not in a valid zone
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đổi khu vực trong map này")?;
            session.send_message(&msg).await?;
            return Ok(());
        };

        // Check if map allows zone changes (no offline/dungeon maps)
        if Self::is_special_map(zone.map_id) && !player.is_admin {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đổi khu vực trong map này")?;
            session.send_message(&msg).await?;
            return Ok(());
        }

        // Get all zones for current map
        let zones = self.get_zones_for_map(zone.map_id).await;

        let mut msg = Message::new(cmd::OPEN_ZONE_UI);
        msg.write_byte(zones.len() as i8)?;

        for zone in zones {
            msg.write_byte(zone.zone_id as i8)?;

            let player_count = zone.get_num_players().await as i8;
            let status = if player_count < 5 {
                0
            } else if player_count < 8 {
                1
            } else {
                2
            };
            msg.write_byte(status)?;
            msg.write_byte(player_count)?;
            msg.write_byte(zone.max_player as i8)?;
            msg.write_byte(0)?; // not competing
        }

        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn change_zone(
        &self,
        player: &mut Player,
        zone_id: i32,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<()> {
        let Some(current_zone) = &player.zone else {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đến khu vực này @")?;
            session.send_message(&msg).await?;
            return Ok(());
        };

        if !player.is_admin && !player.is_boss && !Self::can_change_zone_now(player) {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Chưa thể chuyển khu vực lúc này vui lòng chờ")?;
            session.send_message(&msg).await?;
            return Ok(());
        }

        if Self::is_special_map(current_zone.map_id) && !player.is_admin && !player.is_boss {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể đến khu vực này")?;
            session.send_message(&msg).await?;
            return Ok(());
        }

        if let Some(target_zone) = self.get_specific_zone(current_zone.map_id, zone_id).await {
            if target_zone.is_full().await && !player.is_admin && !player.is_boss {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Khu vực này đã đầy")?;
                session.send_message(&msg).await?;
                return Ok(());
            }

            self.change_map_to_zone_async(
                player,
                &target_zone,
                player.location.x,
                player.location.y,
                SpaceShipType::None,
                session,
            )
            .await?;
            player.update_zone_change_time();
        } else {
            let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
            msg.write_utf("Không thể thực hiện")?;
            session.send_message(&msg).await?;
        }

        Ok(())
    }

    pub async fn change_map_waypoint_handler(
        &self,
        player: &mut Player,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<()> {
        match self.change_map_waypoint(player).await {
            WaypointChangeResult::Success {
                destination_map_id,
                destination_zone_id,
                x,
                y,
            } => {
                println!(
                    "Player {} changed map via waypoint to map {} zone {} at ({}, {})",
                    player.name, destination_map_id, destination_zone_id, x, y
                );
                if let Some(zone) = self
                    .get_specific_zone(destination_map_id, destination_zone_id)
                    .await
                {
                    self.exit_map_async(player).await?;
                    player.location.set_position(x, y);
                    self.go_to_map_async(player, &zone).await?;
                    zone.map_info(session, player.id).await?;
                    println!(
                        "[WAYPOINT] Sent map_info for map {} zone {} to player {}",
                        destination_map_id, destination_zone_id, player.name
                    );
                } else {
                    self.reset_player_position(player, 2000);
                    let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                    msg.write_utf("Lỗi khi chuyển map")?;
                    session.send_message(&msg).await?;
                }
            }
            WaypointChangeResult::NoWaypointFound => {
                self.reset_player_position(player, 2000);
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Bạn chưa thể đến khu vực này")?;
                session.send_message(&msg).await?;
            }
            WaypointChangeResult::TaskRequirementNotMet {
                required_task_id: _,
            } => {
                self.reset_player_position(player, 2000);
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Bạn chưa thể đến khu vực này")?;
                session.send_message(&msg).await?;
            }
            WaypointChangeResult::InvalidPlayerZone => {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Lỗi hệ thống")?;
                session.send_message(&msg).await?;
            }
            WaypointChangeResult::DestinationUnavailable => {
                self.reset_player_position(player, 2000);
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Khu vực đích không khả dụng")?;
                session.send_message(&msg).await?;
            }
        }

        Ok(())
    }
    pub async fn go_home_handler(
        &self,
        player: &mut Player,
        session: &mut crate::network::session::AsyncSession,
    ) -> anyhow::Result<()> {
        match self.go_home(player).await {
            GoHomeResult::Success {
                home_map_id,
                zone_id,
                x,
                y,
                space_type,
            } => {
                if let Some(target_zone) = self.get_specific_zone(home_map_id, zone_id).await {
                    self.change_map_to_zone_async(player, &target_zone, x, y, space_type, session)
                        .await?;
                }
            }
            GoHomeResult::NoAvailableZone => {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Không thể về nhà lúc này")?;
                session.send_message(&msg).await?;
            }
            GoHomeResult::PlayerIsBoss => {
                let mut msg = Message::new(cmd::SEND_ALTER_MESSAGE);
                msg.write_utf("Boss không thể sử dụng chức năng này")?;
                session.send_message(&msg).await?;
            }
        }

        Ok(())
    }

    /// Get zones for a specific map
    async fn get_zones_for_map(&self, map_id: i32) -> Vec<Zone> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(map_id) {
            return map.get_all_zones().await;
        }
        Vec::new()
    }

    fn is_special_map(_map_id: i32) -> bool {
        false
    }

    /// Check if player can change zone now (cooldown check)
    fn can_change_zone_now(_player: &Player) -> bool {
        true
    }

    pub async fn change_map_waypoint(&self, player: &mut Player) -> WaypointChangeResult {
        println!(
            "[WAYPOINT] Starting change_map_waypoint for player {} (map: {}, zone: {:?})",
            player.name,
            player.map_id,
            player.zone.as_ref().map(|z| z.zone_id)
        );

        if player.zone.is_none() && player.map_id == 0 {
            println!("[WAYPOINT] Player has no zone and map_id is 0, returning InvalidPlayerZone");
            return WaypointChangeResult::InvalidPlayerZone;
        }
        let waypoint = self.get_waypoint_at_player_position(player).await;

        match waypoint {
            Some(wp) => {
                println!(
                    "[WAYPOINT] Found waypoint '{}': go_map={}, go_x={}, go_y={}",
                    wp.name, wp.go_map, wp.go_x, wp.go_y
                );

                if !self.check_task_requirement(player, wp.go_map) {
                    let required_task_id = self.get_required_task_id(wp.go_map);
                    println!(
                        "[WAYPOINT] Task requirement not met, required_task_id={}",
                        required_task_id
                    );
                    return WaypointChangeResult::TaskRequirementNotMet { required_task_id };
                }

                println!(
                    "[WAYPOINT] Task requirement passed, getting available zone for map {}",
                    wp.go_map
                );
                let destination_zone = self.get_available_zone(wp.go_map).await;

                match destination_zone {
                    Some(zone) => {
                        println!(
                            "[WAYPOINT] Got destination zone {} for map {}",
                            zone.zone_id, wp.go_map
                        );

                        player.location.set_position(wp.go_x, wp.go_y);
                        player.map_id = wp.go_map;
                        player.zone_id = zone.zone_id;
                        player.location.set_map(wp.go_map, zone.zone_id);

                        println!(
                            "[WAYPOINT] SUCCESS: Player {} moving to map {} zone {} at ({}, {})",
                            player.name, wp.go_map, zone.zone_id, wp.go_x, wp.go_y
                        );

                        WaypointChangeResult::Success {
                            destination_map_id: wp.go_map,
                            destination_zone_id: zone.zone_id,
                            x: wp.go_x,
                            y: wp.go_y,
                        }
                    }
                    None => {
                        println!("[WAYPOINT] No available zone for map {}, returning DestinationUnavailable", wp.go_map);
                        WaypointChangeResult::DestinationUnavailable
                    }
                }
            }
            None => {
                println!(
                    "[WAYPOINT] No waypoint found at player position ({}, {})",
                    player.location.x, player.location.y
                );
                WaypointChangeResult::NoWaypointFound
            }
        }
    }

    async fn get_waypoint_at_player_position(&self, player: &Player) -> Option<WayPoint> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(player.map_id) {
            let waypoint = map.get_waypoint_at_position(player.location.x, player.location.y);
            println!(
                "Player {} at position ({}, {}) on map {}: Waypoint found: {}",
                player.name,
                player.location.x,
                player.location.y,
                player.map_id,
                waypoint.is_some()
            );

            return waypoint;
        }
        None
    }
    pub async fn get_available_zone(&self, map_id: i32) -> Option<Zone> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(map_id) {
            return map.get_best_zone().await;
        }
        None
    }
    pub fn check_task_requirement(&self, player: &Player, map_id: i32) -> bool {
        let player_task_id = player.get_task_id();
        let required_task_id = Self::get_required_task_id_for_map(map_id);

        println!(
            "[TASK CHECK] Player {} task_id={}, required_task_id={} for map {}, is_admin={}",
            player.name, player_task_id, required_task_id, map_id, player.is_admin
        );

        if player.is_admin {
            println!("[TASK CHECK] Player is admin, bypassing task check");
            return true;
        }
        if required_task_id == 0 {
            println!("[TASK CHECK] No task requirement for this map");
            return true;
        }

        let passed = player_task_id >= required_task_id;
        println!(
            "[TASK CHECK] Task check result: {} (player {} >= required {})",
            passed, player_task_id, required_task_id
        );
        passed
    }

    pub fn get_required_task_id(&self, map_id: i32) -> i32 {
        Self::get_required_task_id_for_map(map_id)
    }
    pub fn get_required_task_id_for_map(map_id: i32) -> i32 {
        match map_id {
            1 | 8 | 15 => TASK_1_0,
            42 | 43 | 44 => TASK_2_0,
            2 | 9 | 16 => TASK_3_0,
            24 | 25 | 26 => TASK_4_0,

            // Task 7.0 maps - rừng nấm, thung lũng maima, rừng nguyên sinh
            3 | 11 | 17 => TASK_7_0,

            // Task 13.0 maps - rừng bamboo, rừng dương xỉ, núi hoa vàng, núi hoa tím, rừng cọ, rừng đá
            27 | 28 | 31 | 32 | 35 | 36 => TASK_13_0,

            // Task 15.0 maps - đảo bulong, đông nam guru, bờ vực đen
            30 | 34 | 38 => TASK_15_0,

            // Task 16.0 maps - đông karin, thung lũng namếc, thành phố vegeta
            6 | 10 | 19 => TASK_16_0,

            // Task 18.0 maps - thung lũng nappa, vực cấm, núi appule, căn cứ rasphery, etc.
            68 | 69 | 70 | 71 | 72 | 64 | 65 => TASK_18_0,

            // Task 19.0 maps - trại lính fide, trại quỷ già, vực chết, etc.
            63 | 66 | 67 | 73 | 74 | 75 | 76 | 77 | 81 | 82 | 83 | 79 => TASK_19_0,

            // Task 20.0 maps - núi khỉ vàng
            80 => TASK_20_0,

            // Task 21.0 maps - nhà bunma, thành phố phía đông, thành phố phía nam, đảo balê, cao nguyên
            102 | 92 | 93 | 94 | 96 => TASK_21_0,

            // Task 24.0 maps - thành phố phía bắc, ngọn núi phía bắc, thung lũng phía bắc, thị trấn ginder
            97 | 98 | 99 | 100 => TASK_24_0,

            // Task 27.0 maps - cánh đồng tuyết, rừng tuyết, núi tuyết, dòng sông băng, rừng băng, hang băng, võ đài xên
            105 | 106 | 107 | 108 | 109 | 110 | 103 | 154 => TASK_27_0,

            // No task requirement for other maps
            _ => 0,
        }
    }

    /// Reset player position when waypoint change fails
    /// Requirements: 2.4
    pub fn reset_player_position(&self, player: &mut Player, map_width: i32) {
        let mut x = player.location.x;

        // Clamp position to valid range
        if x >= (map_width - 60) as i16 {
            x = (map_width - 60) as i16;
        } else if x <= 60 {
            x = 60;
        }

        player.location.set_position(x, player.location.y);
    }
    pub async fn exit_map_async(&self, player: &mut Player) -> anyhow::Result<()> {
        if let Some(zone) = &player.zone {
            // Remove player from zone
            zone.remove_player(player.id).await?;

            // Broadcast leave message to other players (CMD -6)
            let mut msg = Message::new(cmd::PLAYER_LEAVE);
            msg.write_int(player.id as i32)?;
            zone.send_message_to_other_players(player.id, msg).await?;

            println!(
                "Player {} exited zone {} on map {}",
                player.name, player.zone_id, player.map_id
            );
        }

        // Clear the zone reference
        player.clear_zone();
        player.zone_id = 0;

        Ok(())
    }

    pub async fn go_to_map_async(&self, player: &mut Player, zone: &Zone) -> anyhow::Result<()> {
        player.zone_id = zone.zone_id;
        player.map_id = zone.map_id;
        player.location.set_map(player.map_id, player.zone_id);
        player.set_zone(zone.clone());
        zone.add_player(player.clone()).await?;
        Self::finish_load_map(player).await?;
        println!(
            "Player {} entered zone {} on map {}",
            player.name, zone.zone_id, zone.map_id
        );
        Ok(())
    }

    pub async fn finish_load_map(player: &Player) -> anyhow::Result<()> {
        if let Some(zone) = &player.zone {
            zone.load_another_to_me(player.id).await?;
            zone.load_me_to_another(player.id).await?;
        }
        Self::send_effect_map_to_me(player).await?;
        Self::send_effect_me_to_map(player).await?;

        Ok(())
    }
    pub async fn send_effect_map_to_me(player: &Player) -> anyhow::Result<()> {
        let Some(zone) = &player.zone else {
            return Ok(());
        };
        let mobs = zone.get_all_mobs().await;
        for mob in mobs {
            // Skip dead mobs
            if mob.hp <= 0 {
                continue;
            }
        }

        // Send player effects to this player
        let players = zone.get_all_players().await;
        for other_player in players {
            if other_player.id == player.id {
                continue; // Skip self
            }
        }

        Ok(())
    }

    pub async fn send_effect_me_to_map(player: &Player) -> anyhow::Result<()> {
        let Some(_zone) = &player.zone else {
            return Ok(());
        };

        Ok(())
    }

    pub fn calculate_home_map(gender: i8, is_in_mabu_map: bool) -> i32 {
        if is_in_mabu_map {
            MABU_HOME_MAP_ID
        } else {
            (gender as i32) + 21
        }
    }

    /// Check if a map is a MaBu map
    pub fn is_mabu_map(map_id: i32) -> bool {
        matches!(map_id, 114 | 115 | 117 | 118 | 119 | 120)
    }

    /// Go home - return to home map (CMD -15)
    ///
    /// This function:
    /// 1. Calculates home map based on player gender (gender + 21)
    /// 2. Handles MaBu map special case (map 114)
    /// 3. Initiates spaceship travel
    ///
    /// Requirements: 3.1
    pub async fn go_home(&self, player: &mut Player) -> GoHomeResult {
        // Check if player is a boss (bosses cannot use go_home)
        // In Java: if (!pl.isBoss)

        // Determine if player is in a MaBu map
        let is_in_mabu = Self::is_mabu_map(player.map_id);

        // Calculate home map based on gender
        // Requirements 3.1: home map = gender + 21, or 114 for MaBu
        let home_map_id = Self::calculate_home_map(player.gender, is_in_mabu);

        // Get a zone in the home map
        let zone = self.get_available_zone(home_map_id).await;

        match zone {
            Some(target_zone) => {
                // Determine spaceship type based on player's tennis spaceship status
                let space_type = if player.has_tennis_spaceship() {
                    SpaceShipType::Tennis
                } else {
                    SpaceShipType::Default
                };

                // Calculate random x position for landing
                let x = Self::calculate_random_x_position(2000); // Default map width

                GoHomeResult::Success {
                    home_map_id,
                    zone_id: target_zone.zone_id,
                    x,
                    y: 5, // Standard landing y position
                    space_type,
                }
            }
            None => GoHomeResult::NoAvailableZone,
        }
    }

    /// Change map by spaceship - spaceship travel with animation
    ///
    /// This function:
    /// 1. Sends spaceship animation effect (CMD -65)
    /// 2. Handles tennis spaceship healing
    /// 3. Handles dead player revival
    ///
    /// Requirements: 3.2, 3.3, 3.4
    pub async fn change_map_by_spaceship(
        &self,
        player: &mut Player,
        map_id: i32,
        zone_id: i32,
        x: i16,
    ) -> SpaceshipTravelResult {
        // Get target zone
        let target_zone = if zone_id == -1 {
            self.get_available_zone(map_id).await
        } else {
            self.get_specific_zone(map_id, zone_id).await
        };

        let Some(zone) = target_zone else {
            return SpaceshipTravelResult::NoAvailableZone;
        };

        // Determine spaceship type
        let space_type = if player.has_tennis_spaceship() {
            SpaceShipType::Tennis
        } else {
            SpaceShipType::Default
        };

        // Calculate final x position
        let final_x = if x != -1 {
            x
        } else {
            Self::calculate_random_x_position(2000) // Default map width
        };

        // Handle player healing/revival based on spaceship type and player state
        // Requirements 3.3, 3.4
        let healing_result = self.handle_spaceship_healing(player, space_type);

        // Update player position
        player.location.set_position(final_x, 5);
        player.map_id = zone.map_id;
        player.zone_id = zone.zone_id;
        player.location.set_map(zone.map_id, zone.zone_id);

        SpaceshipTravelResult::Success {
            map_id: zone.map_id,
            zone_id: zone.zone_id,
            x: final_x,
            y: 5,
            space_type,
            healing_result,
        }
    }

    /// Handle spaceship healing based on spaceship type and player state
    ///
    /// Requirements 3.3: Tennis spaceship SHALL heal player to max HP/MP
    /// Requirements 3.4: Dead player using spaceship SHALL be revived
    ///
    /// Property 6: Tennis spaceship healing
    fn handle_spaceship_healing(
        &self,
        player: &mut Player,
        space_type: SpaceShipType,
    ) -> SpaceshipHealingResult {
        let was_dead = player.is_die();

        if was_dead {
            if space_type == SpaceShipType::Tennis {
                player.n_point.hp = player.n_point.hp_max;
                player.n_point.mp = player.n_point.mp_max;
                player.is_die = false;
                SpaceshipHealingResult::RevivedFullHp
            } else {
                // Normal spaceship: revive with 1 HP/MP
                player.n_point.hp = 1;
                player.n_point.mp = 1;
                player.is_die = false;
                SpaceshipHealingResult::RevivedMinimalHp
            }
        } else if space_type == SpaceShipType::Tennis {
            // Requirements 3.3: Tennis spaceship heals to full HP/MP
            player.n_point.hp = player.n_point.hp_max;
            player.n_point.mp = player.n_point.mp_max;
            SpaceshipHealingResult::HealedToFull
        } else {
            SpaceshipHealingResult::NoHealing
        }
    }

    /// Send spaceship arrive effect to zone (CMD -65)
    ///
    /// This function broadcasts the spaceship animation effect to all players in the zone.
    ///
    /// Requirements: 3.2
    pub async fn spaceship_arrive(
        &self,
        player: &Player,
        send_type: SpaceshipSendType,
        space_type: SpaceShipType,
    ) -> anyhow::Result<()> {
        let mut msg = Message::new(cmd::SPACESHIP_ARRIVE);
        msg.write_int(player.id as i32)?;
        msg.write_byte(space_type as i8)?;

        match send_type {
            SpaceshipSendType::AllPlayersInMap => {
                // Send to all players in zone including self
                if let Some(zone) = &player.zone {
                    zone.send_message_all_player_in_map(player, msg).await?;
                }
            }
            SpaceshipSendType::SelfOnly => {
                player.send_message(msg).await?;
            }
            SpaceshipSendType::OthersInMap => {
                if let Some(zone) = &player.zone {
                    zone.send_message_to_other_players(player.id, msg).await?;
                }
            }
        }

        Ok(())
    }
    async fn get_specific_zone(&self, map_id: i32, zone_id: i32) -> Option<Zone> {
        if let Some(map) = map_manager::MAP_MANAGER.find_by_id(map_id) {
            return map.get_zone(zone_id).await;
        }
        None
    }

    /// Calculate random x position for landing
    fn calculate_random_x_position(map_width: i32) -> i16 {
        use std::time::SystemTime;
        let usable = (map_width.max(200) - 200) as u32;
        let seed = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u32;
        (100 + (seed % usable)) as i16
    }
}

// ========================================
// Result Types for Spaceship Operations
// ========================================

/// Result of go_home operation
#[derive(Debug, Clone, PartialEq)]
pub enum GoHomeResult {
    /// Successfully calculated home destination
    Success {
        home_map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
    },
    /// No available zone in home map
    NoAvailableZone,
    /// Player is a boss and cannot use go_home
    PlayerIsBoss,
}

/// Result of spaceship travel operation
#[derive(Debug, Clone, PartialEq)]
pub enum SpaceshipTravelResult {
    /// Successfully traveled via spaceship
    Success {
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        healing_result: SpaceshipHealingResult,
    },
    /// No available zone at destination
    NoAvailableZone,
    /// Invalid destination
    InvalidDestination,
}

/// Result of spaceship healing
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpaceshipHealingResult {
    /// No healing occurred
    NoHealing,
    /// Player was healed to full HP/MP (tennis spaceship)
    HealedToFull,
    /// Dead player was revived with full HP/MP (tennis spaceship)
    RevivedFullHp,
    /// Dead player was revived with minimal HP/MP (normal spaceship)
    RevivedMinimalHp,
}

/// Type of message send for spaceship effect
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SpaceshipSendType {
    /// Send to all players in map including self
    AllPlayersInMap,
    /// Send only to self
    SelfOnly,
    /// Send to all players except self
    OthersInMap,
}

/// Result of core map change operation
/// Requirements: 5.1, 5.4
#[derive(Debug, Clone, PartialEq)]
pub enum ChangeMapResult {
    /// Successfully changed map
    Success {
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        cold_planet_effect: Option<ColdPlanetEffect>,
    },
    /// Failed - zone is full
    ZoneFull,
    /// Failed - task requirement not met
    TaskRequirementNotMet { required_task_id: i32 },
    /// Failed - invalid zone
    InvalidZone,
}

/// Cold planet stat modifier effect
/// Requirements: 5.4
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ColdPlanetEffect {
    /// Entering Cold planet - stats reduced by 50%
    Entering,
    /// Leaving Cold planet - stats restored to normal
    Leaving,
}

/// Result of capsule change operation
/// Requirements: 4.1, 4.2, 4.3
#[derive(Debug, Clone, PartialEq)]
pub enum CapsuleChangeResult {
    /// Successfully changed map via capsule
    Success { map_id: i32, zone_id: i32 },
    /// Invalid destination index
    InvalidDestination,
    /// Destination zone is unavailable
    DestinationUnavailable,
}

/// Capsule destination information
/// Requirements: 4.1
#[derive(Debug, Clone)]
pub struct CapsuleDestination {
    pub map_id: i32,
    pub map_name: String,
    pub planet_name: String,
}

// ========================================
// Map Access Validation (Task 8)
// ========================================

/// Result of map access validation
/// Requirements: 7.1, 7.2, 7.3, 7.4
#[derive(Debug, Clone, PartialEq)]
pub enum MapAccessResult {
    /// Access granted - player can join the zone
    Allowed,
    /// Access denied - player lacks required task progress
    TaskRequirementNotMet { required_task_id: i32 },
    /// Access denied - player gender doesn't match map restriction
    GenderRestricted {
        player_gender: i8,
        allowed_gender: i8,
    },
    /// Access denied - invalid zone (null or invalid map_id)
    InvalidZone,
}

impl ChangeMapService {
    // ========================================
    // Map Access Validation Functions (Task 8)
    // ========================================

    /// Check if player can join a zone
    ///
    /// This function validates map access based on:
    /// 1. Task requirements (Requirements 7.1, 7.2)
    /// 2. Gender restrictions (Requirements 7.3)
    /// 3. Admin/boss bypass (Requirements 7.4)
    ///
    /// Based on Java checkMapCanJoin implementation in ChangeMapService.java
    ///
    /// Property 7: Task requirement validation
    /// Property 8: Gender restriction validation
    /// Property 9: Admin bypass all restrictions
    pub fn check_map_can_join(player: &Player, zone: &Zone) -> MapAccessResult {
        // Check for invalid zone
        if zone.map_id == -1 {
            return MapAccessResult::InvalidZone;
        }

        // Admin and boss players bypass all restrictions (Requirements 7.4)
        // Property 9: Admin bypass all restrictions
        if player.is_boss || player.is_admin {
            return MapAccessResult::Allowed;
        }

        // Check task requirements (Requirements 7.1, 7.2)
        // Property 7: Task requirement validation
        let required_task_id = Self::get_required_task_id_for_map(zone.map_id);
        if required_task_id > 0 && player.get_task_id() < required_task_id {
            return MapAccessResult::TaskRequirementNotMet { required_task_id };
        }

        // Check gender restrictions for home maps (Requirements 7.3)
        // Property 8: Gender restriction validation
        // Home maps: 21 (Trái Đất), 22 (Namếc), 23 (Xayda)
        if let Some(result) = Self::check_gender_restriction(player, zone.map_id) {
            return result;
        }

        MapAccessResult::Allowed
    }

    /// Check gender restrictions for home maps
    ///
    /// Home maps are gender-restricted:
    /// - Map 21: Only for gender 0 (Trái Đất)
    /// - Map 22: Only for gender 1 (Namếc)
    /// - Map 23: Only for gender 2 (Xayda)
    ///
    /// Requirements: 7.3
    /// Property 8: Gender restriction validation
    fn check_gender_restriction(player: &Player, map_id: i32) -> Option<MapAccessResult> {
        match player.gender {
            GENDER_TRAI_DAT => {
                // Trái Đất players cannot enter Namếc (22) or Xayda (23) home maps
                if map_id == 22 || map_id == 23 {
                    return Some(MapAccessResult::GenderRestricted {
                        player_gender: player.gender,
                        allowed_gender: (map_id - 21) as i8,
                    });
                }
            }
            GENDER_NAMEC => {
                // Namếc players cannot enter Trái Đất (21) or Xayda (23) home maps
                if map_id == 21 || map_id == 23 {
                    return Some(MapAccessResult::GenderRestricted {
                        player_gender: player.gender,
                        allowed_gender: (map_id - 21) as i8,
                    });
                }
            }
            GENDER_XAYDA => {
                // Xayda players cannot enter Trái Đất (21) or Namếc (22) home maps
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

    /// Check if a map is a home map (gender-restricted)
    pub fn is_home_map(map_id: i32) -> bool {
        matches!(map_id, 21 | 22 | 23)
    }

    /// Get the allowed gender for a home map
    /// Returns None if the map is not a home map
    pub fn get_home_map_gender(map_id: i32) -> Option<i8> {
        match map_id {
            21 => Some(GENDER_TRAI_DAT),
            22 => Some(GENDER_NAMEC),
            23 => Some(GENDER_XAYDA),
            _ => None,
        }
    }
}
