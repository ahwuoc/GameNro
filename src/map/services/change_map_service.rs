//! ChangeMapService – thin facade delegating to the sub-modules in `change_map/`.
//!
//! All existing call sites are preserved; they all compile against this facade.

use crate::map::{
    models::zone::ZoneHandle,
    services::change_map_models::*,
};
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::PlayerHandle;

use super::change_map::*;

pub struct ChangeMapService;

impl ChangeMapService {
    // ── Core pipeline ─────────────────────────────────────────────────────────

    pub async fn change_map_to_zone(
        player: &mut Player,
        zone: &ZoneHandle,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
        session: Option<&SessionArc>,
    ) -> anyhow::Result<ChangeMapResult> {
        CoreService::change_map_to_zone(player, zone, x, y, space_type, session).await
    }

    pub async fn exit_current_map(player: &mut Player) -> anyhow::Result<()> {
        CoreService::exit_current_map(player).await
    }

    pub async fn go_to_map(
        player: &mut Player,
        zone: &ZoneHandle,
        session: Option<&SessionArc>,
    ) -> anyhow::Result<()> {
        CoreService::go_to_map(player, zone, session).await
    }

    pub fn get_available_zone(map_id: i32) -> Option<ZoneHandle> {
        CoreService::get_available_zone(map_id)
    }

    pub fn get_specific_zone(map_id: i32, zone_id: i32) -> Option<ZoneHandle> {
        CoreService::get_specific_zone(map_id, zone_id)
    }

    // ── Spaceship ─────────────────────────────────────────────────────────────

    pub fn handle_spaceship_healing(
        player: &mut Player,
        space_type: SpaceShipType,
    ) -> SpaceshipHealingResult {
        SpaceshipService::handle_healing(player, space_type)
    }

    pub fn spaceship_arrive(
        player: &Player,
        send_type: SpaceshipSendType,
        space_type: SpaceShipType,
    ) -> anyhow::Result<()> {
        SpaceshipService::arrive(player, send_type, space_type)
    }

    // ── Capsule ───────────────────────────────────────────────────────────────

    pub fn open_capsule_menu(player: &Player) -> anyhow::Result<()> {
        CapsuleService::open_capsule_menu(player)
    }

    pub async fn change_map_capsule(
        player: &mut Player,
        destination_index: i32,
        session: &SessionArc,
    ) -> anyhow::Result<CapsuleChangeResult> {
        CapsuleService::change_map_capsule(player, destination_index, session).await
    }

    // ── Waypoint ──────────────────────────────────────────────────────────────

    pub async fn change_map_waypoint_handler(
        player: &mut Player,
        session: &SessionArc,
    ) -> anyhow::Result<()> {
        WaypointService::change_map_waypoint_handler(player, session).await
    }

    pub fn change_map_waypoint(player: &mut Player) -> WaypointChangeResult {
        WaypointService::change_map_waypoint(player)
    }

    // ── Home & Zone UI ────────────────────────────────────────────────────────

    pub async fn go_home_handler(player: &mut Player, session: &SessionArc) -> anyhow::Result<()> {
        HomeService::go_home_handler(player, session).await
    }

    pub fn go_home(player: &mut Player) -> GoHomeResult {
        HomeService::go_home(player)
    }

    pub fn calculate_home_map(gender: i8, is_in_mabu_map: bool) -> i32 {
        HomeService::calculate_home_map(gender, is_in_mabu_map)
    }

    pub async fn open_zone_ui(player: &Player) -> anyhow::Result<()> {
        ZoneUiService::open_zone_ui(player).await
    }

    pub async fn change_zone(
        player: &mut Player,
        zone_id: i32,
        session: &SessionArc,
    ) -> anyhow::Result<()> {
        ZoneUiService::change_zone(player, zone_id, session).await
    }

    // ── Validation ────────────────────────────────────────────────────────────

    pub fn check_map_access(player: &Player, map_id: i32) -> MapAccessResult {
        ValidationService::check_map_access(player, map_id)
    }

    pub fn get_required_task_id_for_map(map_id: i32) -> i32 {
        ValidationService::get_required_task_id_for_map(map_id)
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    pub async fn finish_load_map(
        player: &Player,
        session: Option<&SessionArc>,
    ) -> anyhow::Result<()> {
        SyncService::finish_load_map(player, session).await
    }

    pub fn send_effect_map_to_me(player: &Player) -> anyhow::Result<()> {
        SyncService::send_effect_map_to_me(player)
    }

    pub fn send_effect_me_to_map(player: &Player) -> anyhow::Result<()> {
        SyncService::send_effect_me_to_map(player)
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    pub fn is_cold_planet_map(map_id: i32) -> bool {
        is_cold_planet_map(map_id)
    }

    pub fn get_y_physic_in_top(map_id: i32, x: i16, y: i16) -> i16 {
        super::change_map::utils::get_y_physic_in_top(map_id, x, y)
    }

    pub fn calculate_random_x_position(map_width: i32) -> i16 {
        super::change_map::utils::calculate_random_x_position(map_width)
    }
}
