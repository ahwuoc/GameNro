use crate::constant::const_map::{GENDER_NAMEC, GENDER_TRAI_DAT, GENDER_XAYDA, MABU_HOME_MAP_ID};
use crate::map::services::{change_map_models::*, map_service, change_map_service::ChangeMapService};
use crate::network::message::Message;
use crate::constant::cmd::cmd;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::services::ServiceHandles;

pub struct HomeService;

impl HomeService {
    pub async fn go_home_handler(player: &mut Player, session: &SessionArc) -> anyhow::Result<()> {
        match Self::go_home(player) {
            GoHomeResult::Success { home_map_id, zone_id, x, y, space_type } => {
                if let Some(target_zone) = super::core::CoreService::get_specific_zone(home_map_id, zone_id) {
                    super::core::CoreService::change_map_to_zone(player, &target_zone, x, y, space_type, Some(session)).await?;
                }
            }
            GoHomeResult::NoAvailableZone => {
                ServiceHandles::send_thong_bao_to_player(player, "Không thể về nhà lúc này")?;
            }
            GoHomeResult::PlayerIsBoss => {
                ServiceHandles::send_thong_bao_to_player(player, "Boss không thể sử dụng chức năng này")?;
            }
        }
        Ok(())
    }

    pub fn go_home(player: &mut Player) -> GoHomeResult {
        let is_in_mabu = map_service::is_mapa_mabu(player.map_id);
        let home_map_id = Self::calculate_home_map(player.gender, is_in_mabu);
        match super::core::CoreService::get_available_zone(home_map_id) {
            Some(target_zone) => {
                let space_type = if player.has_tennis_spaceship() {
                    SpaceShipType::Tennis
                } else {
                    SpaceShipType::Default
                };
                GoHomeResult::Success {
                    home_map_id,
                    zone_id: target_zone.zone_id,
                    x: super::utils::calculate_random_x_position(300),
                    y: 5,
                    space_type,
                }
            }
            None => GoHomeResult::NoAvailableZone,
        }
    }

    pub fn calculate_home_map(gender: i8, is_in_mabu_map: bool) -> i32 {
        if is_in_mabu_map {
            MABU_HOME_MAP_ID
        } else {
            gender as i32 + 21
        }
    }
}

pub struct ZoneUiService;

impl ZoneUiService {
    pub async fn open_zone_ui(player: &Player) -> anyhow::Result<()> {
        let Some(zone) = crate::map::zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id) else {
            ServiceHandles::send_thong_bao_to_player(player, "Không thể đổi khu vực trong map này")?;
            return Ok(());
        };
        if super::utils::is_special_map(zone.map_id) && !player.is_admin {
            ServiceHandles::send_thong_bao_to_player(player, "Không thể đổi khu vực trong map này")?;
            return Ok(());
        }
        let zones = super::utils::get_zones_for_map(zone.map_id);
        let mut msg = Message::new(cmd::OPEN_ZONE_UI);
        msg.write_byte(zones.len() as i8)?;
        for zone in zones {
            msg.write_byte(zone.zone_id as i8)?;
            let (player_count, max_player) = if let Ok(info) = zone.get_zone_info().await {
                (info.current_players as i8, info.max_player as i8)
            } else {
                (0, 5)
            };
            let status = if player_count < 5 { 0 } else if player_count < 8 { 1 } else { 2 };
            msg.write_byte(status)?;
            msg.write_byte(player_count)?;
            msg.write_byte(max_player)?;
            msg.write_byte(0)?;
        }
        player.send_to_client(msg)?;
        Ok(())
    }

    pub async fn change_zone(player: &mut Player, zone_id: i32, session: &SessionArc) -> anyhow::Result<()> {
        let Some(current_zone) = crate::map::zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id) else {
            ServiceHandles::send_thong_bao_to_player(player, "Không thể đến khu vực này @")?;
            return Ok(());
        };
        if !player.is_admin && !player.is_boss && !super::utils::can_change_zone_now(player) {
            ServiceHandles::send_thong_bao_to_player(player, "Chưa thể chuyển khu vực lúc này vui lòng chờ")?;
            return Ok(());
        }
        if super::utils::is_special_map(current_zone.map_id) && !player.is_admin && !player.is_boss {
            ServiceHandles::send_thong_bao_to_player(player, "Không thể đến khu vực này")?;
            return Ok(());
        }
        if let Some(target_zone) = super::core::CoreService::get_specific_zone(current_zone.map_id, zone_id) {
            let info = target_zone.get_zone_info().await?;
            if info.current_players >= info.max_player && !player.is_admin && !player.is_boss {
                ServiceHandles::send_thong_bao_to_player(player, "Khu vực này đã đầy")?;
                return Ok(());
            }
            super::core::CoreService::change_map_to_zone(
                player, &target_zone, player.location.x, player.location.y,
                SpaceShipType::None, Some(session),
            ).await?;
            player.update_zone_change_time();
        } else {
            ServiceHandles::send_thong_bao_to_player(player, "Không thể thực hiện")?;
        }
        Ok(())
    }
}
