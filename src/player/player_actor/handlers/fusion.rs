use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::player::Fusion;
use crate::services::player_tnsm_services::{self, TypeTNSM};
use crate::services::{player_info_service, ServiceHandles};
use crate::templates::fusion_template_manager;

pub struct FusionHandler;

impl FusionHandler {
    pub async fn handle_fusion(
        player: &mut Player,
        pet_handle: &Option<PetHandle>,
        type_fusion: i8,
        template_id: i32,
    ) {
        if player.fusion.type_fusion != 0 {
            return;
        }

        if let Some(ref pet_handle) = pet_handle {
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = pet_handle.send(PetMessage::GetSnapshot(tx)).await;
            
            if let Ok(pet_snapshot) = rx.await {
                // Permanent fusion for male characters
                if type_fusion == Fusion::HOP_THE_VINH_VIEN && player.gender == 1 {
                    ServiceHandles::send_fusion_effect(player, Fusion::LUONG_LONG_NHAT_THE);
                    let _ = pet_handle.tx.send(PlayerMessage::Logout).await;

                    let pet_power = pet_snapshot.player.n_point.power;
                    player_tnsm_services::tiemnang_sucmanh_add(player, TypeTNSM::All, pet_power, false);
                    player_info_service::send_pet_info(player);
                    return;
                }
                
                // Normal fusion
                if let Some(template) = fusion_template_manager::get(template_id) {
                    player.fusion.type_fusion = type_fusion;
                    player.fusion.template_id = template_id;

                    player.n_point.hp_fusion = pet_snapshot.player.n_point.hp_max / 2;
                    player.n_point.mp_fusion = pet_snapshot.player.n_point.mp_max / 2;
                    player.n_point.dame_fusion = pet_snapshot.player.n_point.dame / 2;
                    player.n_point.def_fusion = pet_snapshot.player.n_point.def / 2;
                    player.n_point.crit_fusion =
                        (pet_snapshot.player.n_point.crit / 2) + template.crit_bonus;

                    player.n_point.hp_fusion_tl = template.hp_percent as i32;
                    player.n_point.mp_fusion_tl = template.mp_percent as i32;
                    player.n_point.dame_fusion_tl = template.dame_percent as i32;

                    player.n_point.cal_point();
                    player.n_point.set_hp(player.n_point.hp_max);
                    player.n_point.set_mp(player.n_point.mp_max);

                    pet_handle.send(PetMessage::Fusion(true)).await;

                    player_info_service::send_point_info_sync(player);
                    player_info_service::send_info_hp_mp_money(player);
                    ServiceHandles::send_cai_trang(player);
                    ServiceHandles::send_fusion_effect(player, type_fusion);
                    
                    if type_fusion == Fusion::LUONG_LONG_NHAT_THE {
                        player.fusion.last_time_fusion = crate::utils::time::current_time_millis();
                        let icon_id: i16 = if player.gender == 1 { 3901 } else { 3790 };
                        let _ = ServiceHandles::send_item_time_client(player, icon_id, 600);
                    }
                }
            }
        }
    }

    pub async fn handle_unfusion(player: &mut Player, pet_handle: &Option<PetHandle>) {
        player.fusion.type_fusion = crate::player::components::fusion::Fusion::NON_FUSION;
        player.n_point.hp_fusion = 0;
        player.n_point.mp_fusion = 0;
        player.n_point.dame_fusion = 0;
        player.n_point.def_fusion = 0;
        player.n_point.crit_fusion = 0;

        player.n_point.hp_fusion_tl = 0;
        player.n_point.mp_fusion_tl = 0;
        player.n_point.dame_fusion_tl = 0;

        player.n_point.cal_point();

        if let Some(ref pet_handle) = pet_handle {
            let _ = pet_handle
                .send(PetMessage::ChangeStatus(
                    crate::player::player_actor::pet::PetStatus::Follow,
                ))
                .await;
        }

        let _ = player_info_service::send_point_info_sync(player);
        let _ = player_info_service::send_info_hp_mp_money(player);
        let _ = ServiceHandles::send_cai_trang(player);
        let _ = ServiceHandles::send_fusion_effect(player, 0);
    }
}
