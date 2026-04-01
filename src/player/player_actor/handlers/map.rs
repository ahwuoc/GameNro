use crate::boss::boss_id::BOSS_TAU_PAY_PAY;
use crate::constant::task_id;
use crate::map::services::training_services;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::{ChangeMapService, SpaceShipType};
use crate::matches::{pvp_manager, TypeLosePvp};
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::services::black_ball_war_service::BlackBallWarService;
use crate::services::task_service::TaskService;
use crate::services::task_utils::TaskUtils;
use crate::services::ServiceHandles;

pub struct MapHandler;

impl MapHandler {
    pub async fn handle_finish_load_map(player: &Player, session: &Option<&SessionArc>) {
        ChangeMapService::finish_load_map(player, *session).await;
        TaskService::send_info_current_task(player);
        TaskService::send_tutorial_task_0_0_0(player, "GameNro Server");
        TaskService::check_auto_skip_task_home(&mut player.clone());

        if player.map_id == 47 {
            let task_id = TaskUtils::get_id_task(player);
            let task_index = TaskUtils::get_task_index(player);
            if task_id >= task_id::TASK_7 && task_index > 0 {
                training_services::call_boss_by_id(&mut player.clone(), BOSS_TAU_PAY_PAY, false);
            }
        }
    }

    pub async fn handle_move(player: &mut Player, pet_handle: &Option<PetHandle>, x: i16, y: i16) {
        if player.is_die() {
            return;
        }

        if player.effect_skill.use_troi {
            crate::player::player_actor::handlers::combat::CombatHandler::release_hold(player);
        }

        player.location.set_position(x, y);
        let map_id = player.map_id;
        TaskService::check_done_task_go_to_map_position(player, map_id, x);
        
        if let Some(ref pet_handle) = pet_handle {
            let _ = pet_handle.send(PetMessage::MasterLocation(x, y)).await;
        }

        let zone_opt = ZONE_MANAGER.get_zone(player.map_id, player.zone_id);
        if let Some(zone) = zone_opt {
            let mut msg = Message::new(-7);
            let _ = msg.write_int(player.id as i32);
            let _ = msg.write_short(player.location.x);
            let _ = msg.write_short(player.location.y);
            let _ = ServiceHandles::send_to_all_in_zone(&zone, msg);
            player.sync_public_state();
        }
    }

    pub async fn handle_change_map(
        player: &mut Player,
        session: &SessionArc,
        pet_handle: &Option<PetHandle>,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        space_type: SpaceShipType,
    ) {
        let zone_opt = if zone_id == -1 {
            ZONE_MANAGER.get_best_zone(map_id)
        } else {
            ZONE_MANAGER.get_zone(map_id, zone_id)
        };

        if let Some(zone) = zone_opt {
            Self::sync_pet_map(player, pet_handle).await;
            ChangeMapService::change_map_to_zone(player, &zone, x, y, space_type, Some(session)).await;
        } else {
            tracing::warn!(
                "[ACTOR] ChangeMap failed: zone not found for map {} zone {}",
                map_id,
                zone_id
            );
        }
    }

    pub async fn handle_change_map_capsule(
        player: &mut Player,
        session: &SessionArc,
        pet_handle: &Option<PetHandle>,
        index: i32,
    ) {
        ChangeMapService::change_map_capsule(player, index, session).await;
        Self::sync_pet_map(player, pet_handle).await;
    }

    pub async fn handle_change_map_black_ball(
        player: &mut Player,
        session: &SessionArc,
        pet_handle: &Option<PetHandle>,
        index: i8,
    ) {
        BlackBallWarService::change_map(player, index, session);
        Self::sync_pet_map(player, pet_handle).await;
    }

    pub async fn sync_pet_map(player: &Player, pet_handle: &Option<PetHandle>) {
        if let Some(ref pet_handle) = pet_handle {
            let _ = pet_handle
                .tx
                .send(crate::player::player_actor::message::PlayerMessage::ChangeMap {
                    map_id: player.map_id,
                    zone_id: player.zone_id,
                    x: player.location.x,
                    y: player.location.y,
                    space_type: SpaceShipType::None,
                })
                .await;
        }
    }

    pub fn prepare_for_map_change(player_id: u64) {
        pvp_manager::get_pvp_handle().player_lose(player_id as i64, TypeLosePvp::RunsAway);
    }
}
