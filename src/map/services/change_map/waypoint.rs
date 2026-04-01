//! Waypoint-based map transitions

use crate::map::{
    map_manager::MAP_MANAGER,
    models::zone::ZoneHandle,
    services::{change_map_models::*, map_service, change_map_service::ChangeMapService},
    zone_manager::ZONE_MANAGER,
    WayPoint,
};
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::services::ServiceHandles;

pub struct WaypointService;

impl WaypointService {
    pub async fn change_map_waypoint_handler(
        player: &mut Player,
        session: &SessionArc,
    ) -> anyhow::Result<()> {
        if let Some(msg) = Self::check_map_conditions(player).await? {
            super::sync::SyncService::send_reset_point(player, session);
            ServiceHandles::send_thong_bao_to_player(player, &msg)?;
            return Ok(());
        }

        match ChangeMapService::change_map_waypoint(player) {
            WaypointChangeResult::Success { destination_map_id, destination_zone_id, x, y } => {
                if let Some(zone) = ZONE_MANAGER.get_zone(destination_map_id, destination_zone_id) {
                    super::core::CoreService::change_map_to_zone(
                        player, &zone, x, y, SpaceShipType::None, Some(session),
                    ).await?;
                } else {
                    ServiceHandles::send_thong_bao_to_player(player, "Lỗi khi chuyển map")?;
                }
            }
            WaypointChangeResult::NoWaypointFound => {
                tracing::debug!(
                    "DEBUG_WAYPOINT: No waypoint for {} at ({},{}) map {}",
                    player.name, player.location.x, player.location.y, player.map_id
                );
                ServiceHandles::send_thong_bao_to_player(player, "Waypoint not found")?;
            }
            WaypointChangeResult::TaskRequirementNotMet { .. } => {
                ServiceHandles::send_thong_bao_to_player(player, "Bạn chưa thể đến khu vực này")?;
            }
            WaypointChangeResult::InvalidPlayerZone => {
                ServiceHandles::send_thong_bao_to_player(player, "Lỗi hệ thống")?;
            }
            WaypointChangeResult::DestinationUnavailable => {
                ServiceHandles::send_thong_bao_to_player(player, "Khu vực không khả dụng")?;
            }
        }
        Ok(())
    }

    pub fn change_map_waypoint(player: &mut Player) -> WaypointChangeResult {
        if ZONE_MANAGER.get_zone(player.map_id, player.zone_id).is_none() {
            return WaypointChangeResult::InvalidPlayerZone;
        }
        let Some(wp) = Self::get_waypoint_at_player_position(player) else {
            return WaypointChangeResult::NoWaypointFound;
        };
        let access = ChangeMapService::check_map_access(player, wp.go_map);
        if access != MapAccessResult::Allowed {
            return match access {
                MapAccessResult::TaskRequirementNotMet { required_task_id } =>
                    WaypointChangeResult::TaskRequirementNotMet { required_task_id },
                _ => WaypointChangeResult::DestinationUnavailable,
            };
        }
        if player.zone_id >= 100 && super::utils::is_doanh_trai_map(wp.go_map) {
            if let Some(zone) = ZONE_MANAGER.get_zone(wp.go_map, player.zone_id) {
                return WaypointChangeResult::Success {
                    destination_map_id: wp.go_map,
                    destination_zone_id: zone.zone_id,
                    x: wp.go_x,
                    y: wp.go_y,
                };
            }
        }
        if let Some(zone) = super::core::CoreService::get_available_zone(wp.go_map) {
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
        MAP_MANAGER.find_by_id(player.map_id)?.get_waypoint_at_position(player.location.x, player.location.y)
    }

    async fn check_map_conditions(player: &Player) -> anyhow::Result<Option<String>> {
        if map_service::is_map_doanh_trai(player.map_id) {
            if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
                let state = zone.public_state.read().await;
                if state.mob_alive_count > 0 || state.has_boss {
                    return Ok(Some("Chưa hạ hết đối thủ".to_string()));
                }
            }
        }
        Ok(None)
    }
}
