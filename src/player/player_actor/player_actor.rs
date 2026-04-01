use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::player_service;
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::{error, info};

use super::handlers::*;

pub struct PlayerActor {
    pub player: Player,
    pub receiver: mpsc::Receiver<PlayerMessage>,
    pub session: SessionArc,
    pub pet_handle: Option<PetHandle>,
    pub last_finish_load_time: Option<std::time::Instant>,
    pub last_pick_time: Option<std::time::Instant>,
}

impl PlayerActor {
    pub fn new(
        player: Player,
        session: SessionArc,
        receiver: mpsc::Receiver<PlayerMessage>,
    ) -> Self {
        Self {
            player,
            receiver,
            session,
            pet_handle: None,
            last_finish_load_time: None,
            last_pick_time: None,
        }
    }

    pub async fn run(mut self) {
        info!(
            "PlayerActor started for {} (ID: {})",
            self.player.name, self.player.id
        );

        let mut interval = tokio::time::interval(Duration::from_millis(500));

        loop {
            tokio::select! {
                Some(msg) = self.receiver.recv() => {
                    match msg {
                        PlayerMessage::Logout => break,
                        m => self.handle_message(m).await,
                    }
                }
                _ = interval.tick() => {
                    self.update().await;
                }
            }
        }

        self.dispose().await;
    }

    /// Route messages to appropriate handlers
    async fn handle_message(&mut self, msg: PlayerMessage) {
        match msg {
            // ═══════════════════════════════════════════════════
            // Task System
            // ═══════════════════════════════════════════════════
            PlayerMessage::TaskAction(task_type, target_id) => {
                TaskHandler::handle_task_action(&mut self.player, task_type, target_id).await;
            }

            // ═══════════════════════════════════════════════════
            // Network & Communication
            // ═══════════════════════════════════════════════════
            PlayerMessage::NetworkMessage(m) => {
                let command = m.command;
                if let Err(e) = NetworkHandler::handle_network_command(
                    &mut self.player,
                    &self.session,
                    &self.pet_handle,
                    m,
                )
                .await
                {
                    error!(
                        "Error processing network message for player {}: {:?}",
                        self.player.id, e
                    );
                }
                match command {
                    -33 | -23 | -15 | 21 => {
                        tracing::info!(
                            "EXITING: PlayerMessage::NetworkMessage (command: {}, player: {})",
                            command,
                            self.player.id
                        );
                    }
                    _ => {}
                }
            }
            PlayerMessage::Chat { text } => {
                MiscHandler::handle_chat(&mut self.player, &self.session, text).await;
            }
            PlayerMessage::SendPacket(m) => {
                self.session.transmit(m);
            }

            // ═══════════════════════════════════════════════════
            // Combat System
            // ═══════════════════════════════════════════════════
            PlayerMessage::Injured {
                damage,
                piercing,
                from_mob,
                attacker_id: _,
            } => {
                CombatHandler::handle_injured(&mut self.player, damage, piercing, from_mob).await;
            }
            PlayerMessage::AttackMob { mob_id } => {
                CombatHandler::handle_attack_mob(&mut self.player, mob_id).await;
            }
            PlayerMessage::AttackPlayer { player_id } => {
                CombatHandler::handle_attack_player(&mut self.player, player_id).await;
            }
            PlayerMessage::UseSkill { msg } => {
                CombatHandler::handle_use_skill(&mut self.player, msg).await;
            }
            PlayerMessage::ApplyHuytSaoBuff { percent_hp } => {
                CombatHandler::handle_huyt_sao_buff(&mut self.player, percent_hp).await;
            }
            PlayerMessage::HandleAnTroi(is_an_troi, time_an_troi, caster_id) => {
                CombatHandler::handle_an_troi(
                    &mut self.player,
                    is_an_troi,
                    time_an_troi,
                    caster_id,
                );
            }

            // ═══════════════════════════════════════════════════
            // Skill System
            // ═══════════════════════════════════════════════════
            PlayerMessage::SelectSkill { skill_template_id } => {
                SkillHandler::handle_select_skill(&mut self.player, skill_template_id);
            }
            PlayerMessage::UpdateSkillShortcuts { shortcuts } => {
                SkillHandler::handle_update_skill_shortcuts(&mut self.player, shortcuts);
            }

            // ═══════════════════════════════════════════════════
            // Inventory System
            // ═══════════════════════════════════════════════════
            PlayerMessage::PickItem { item_map_id } => {
                InventoryHandler::handle_pick_item(&mut self.player, &self.session, item_map_id)
                    .await;
            }
            PlayerMessage::ItemAction {
                type_action,
                where_item,
                index,
            } => {
                InventoryHandler::handle_item_action(
                    &self.session,
                    &mut self.player,
                    &self.pet_handle,
                    type_action,
                    where_item,
                    index,
                )
                .await;
            }
            PlayerMessage::GetItem {
                type_item_inventory,
                index,
            } => {
                InventoryHandler::handle_get_item(
                    &self.session,
                    &mut self.player,
                    type_item_inventory,
                    index,
                )
                .await;
            }

            // ═══════════════════════════════════════════════════
            // Combine System
            // ═══════════════════════════════════════════════════
            PlayerMessage::CombineOpenTab {
                type_combine,
                npc_id,
            } => {
                MiscHandler::handle_combine_open_tab(
                    &mut self.player,
                    &self.session,
                    type_combine,
                    npc_id,
                )
                .await;
            }
            PlayerMessage::CombineShowInfo { index } => {
                MiscHandler::handle_combine_show_info(&mut self.player, &self.session, index).await;
            }
            PlayerMessage::CombineConfirm => {
                MiscHandler::handle_combine_confirm(&mut self.player, &self.session).await;
            }

            // ═══════════════════════════════════════════════════
            // Map & Movement
            // ═══════════════════════════════════════════════════
            PlayerMessage::Move { x, y } => {
                MapHandler::handle_move(&mut self.player, &self.pet_handle, x, y).await;
            }
            PlayerMessage::ChangeMap {
                map_id,
                zone_id,
                x,
                y,
                space_type,
            } => {
                MapHandler::handle_change_map(
                    &mut self.player,
                    &self.session,
                    &self.pet_handle,
                    map_id,
                    zone_id,
                    x,
                    y,
                    space_type,
                )
                .await;
            }
            PlayerMessage::FinishLoadMap => {
                MapHandler::handle_finish_load_map(&mut self.player, Some(&self.session)).await;
                tracing::info!(
                    "EXITING: PlayerMessage::FinishLoadMap (player: {})",
                    self.player.id
                );
            }
            PlayerMessage::ChangeMapCapsule(index) => {
                MapHandler::handle_change_map_capsule(
                    &mut self.player,
                    &self.session,
                    &self.pet_handle,
                    index,
                )
                .await;
            }
            PlayerMessage::ChangeMapBlackBall(index) => {
                MapHandler::handle_change_map_black_ball(
                    &mut self.player,
                    &self.session,
                    &self.pet_handle,
                    index,
                )
                .await;
            }

            // ═══════════════════════════════════════════════════
            // Pet System
            // ═══════════════════════════════════════════════════
            PlayerMessage::ShowInfoPet => {
                PetHandler::handle_show_info_pet(&self.player, &self.pet_handle).await;
            }
            PlayerMessage::Pet(pet_msg) => {
                PetHandler::handle_pet_forward(&self.player, &self.pet_handle, pet_msg);
            }
            PlayerMessage::PetAskPea { pet_id } => {
                InventoryHandler::handle_pet_ask_pea(&mut self.player, &self.pet_handle, pet_id)
                    .await;
            }
            PlayerMessage::SetPetHandle(handle) => {
                self.pet_handle = Some(handle);
            }
            PlayerMessage::ClearPetHandle => {
                self.pet_handle = None;
                self.player.pet_id = None;
                info!("Cleared pet handle for player {}", self.player.id);
            }

            // ═══════════════════════════════════════════════════
            // Fusion System
            // ═══════════════════════════════════════════════════
            PlayerMessage::Fusion {
                type_fusion,
                template_id,
            } => {
                FusionHandler::handle_fusion(
                    &mut self.player,
                    &self.pet_handle,
                    type_fusion,
                    template_id,
                )
                .await;
            }
            PlayerMessage::Unfusion => {
                FusionHandler::handle_unfusion(&mut self.player, &self.pet_handle).await;
            }

            // ═══════════════════════════════════════════════════
            // Magic Tree
            // ═══════════════════════════════════════════════════
            PlayerMessage::MagicTree(magic_tree_msg) => {
                use crate::player::player_actor::MagicTreeMsg;
                match magic_tree_msg {
                    MagicTreeMsg::OpenOrLoad(action) => {
                        MagicTreeHandler::handle_action(&mut self.player, &self.session, action);
                    }
                    MagicTreeMsg::Harvest => {
                        MagicTreeHandler::handle_harvest(&mut self.player);
                    }
                    MagicTreeMsg::FastRespawn => {
                        MagicTreeHandler::handle_fast_respawn(&mut self.player);
                    }
                    MagicTreeMsg::Upgrade => {
                        MagicTreeHandler::handle_upgrade(&mut self.player);
                    }
                    MagicTreeMsg::FastUpgrade => {
                        MagicTreeHandler::handle_fast_upgrade(&mut self.player);
                    }
                    MagicTreeMsg::Unupgrade => {
                        MagicTreeHandler::handle_unupgrade(&mut self.player);
                    }
                }
            }

            // ═══════════════════════════════════════════════════
            // Miscellaneous
            // ═══════════════════════════════════════════════════
            PlayerMessage::HoiSinh => {
                let _ = player_service::hoi_sinh(&mut self.player);
            }
            PlayerMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.player.clone());
            }
            PlayerMessage::IncreasePoint {
                type_increment,
                point,
            } => {
                let old_task =
                    MiscHandler::handle_increase_point(&mut self.player, type_increment, point)
                        .await;
                TaskHandler::handle_task_advance(&self.player, old_task).await;
            }
            PlayerMessage::AddTNSM {
                type_tnsm,
                param,
                is_ori,
            } => {
                MiscHandler::handle_add_tnsm(&mut self.player, type_tnsm, param, is_ori);
            }
            PlayerMessage::CreateMenu {
                npc_id,
                npc_say,
                menu_options,
                state,
            } => {
                MiscHandler::handle_create_menu(
                    &mut self.player,
                    npc_id,
                    npc_say,
                    menu_options,
                    state,
                );
            }
            PlayerMessage::Modify(f) => {
                f(&mut self.player);
            }
            PlayerMessage::RadarAction(action, mut msg) => {
                let _ =
                    MiscHandler::handle_radar_action(&mut self.player, action, &mut msg).await;
            }
            PlayerMessage::SendInfoTo(target_handle) => {
                MiscHandler::handle_send_info_to(&self.player, target_handle);
            }
            PlayerMessage::SendInfoToAll(targets) => {
                MiscHandler::handle_send_info_to_all(&self.player, targets);
            }
            PlayerMessage::CallTrainingBoss {
                boss_id,
                is_thachdau,
            } => {
                MiscHandler::handle_call_training_boss(&mut self.player, boss_id, is_thachdau);
            }
            PlayerMessage::UpdateTick => {
                self.update().await;
            }
            PlayerMessage::Logout => {
                // Handled in main loop
            }
        }
    }

    /// Periodic update tick (500ms)
    async fn update(&mut self) {
        // Update magic tree
        self.player.magic_tree.update();

        // Check if player should be disposed
        if self.player.before_dispose {
            return;
        }

        // Check death
        if self.player.n_point.hp_current <= 0 && !self.player.dead_flag {
            self.player.set_die();
        }

        // Check fusion expiration
        if self.player.fusion.is_timed_fusion() {
            let now = crate::utils::time::current_time_millis();
            if self.player.fusion.is_fusion_expired(now) {
                tracing::info!(
                    "[FUSION] Player {} fusion timer expired, auto-unfusion",
                    self.player.id
                );
                FusionHandler::handle_unfusion(&mut self.player, &self.pet_handle).await;
            }
        }

        player_service::update_player_tick(&mut self.player);
        self.player.sync_public_state();
    }

    async fn dispose(&mut self) {
        info!(
            "PlayerActor disposing for {} (ID: {})",
            self.player.name, self.player.id
        );

        use crate::map::ChangeMapService;
        use crate::matches::{pvp_manager, TypeLosePvp};
        use crate::player::player_actor::pet::message::PetMessage;
        pvp_manager::get_pvp_handle().player_lose(self.player.id as i64, TypeLosePvp::RunsAway);
        if let Some(ref pet_handle) = self.pet_handle {
            let (tx, rx) = tokio::sync::oneshot::channel();
            if let Ok(_) = pet_handle.send(PetMessage::GetSnapshot(tx)).await {
                if let Ok(pet_snapshot) = rx.await {
                    let mut pet_data =
                        crate::player::player_mapper::player_to_pet_data(&pet_snapshot.player);
                    pet_data.status = pet_snapshot.status as i8;
                    pet_data.type_pet = pet_snapshot.type_pet;
                    self.player.pet_data = Some(pet_data);
                }
            }
        }
        if let Err(e) = player_service::save_player(&self.player).await {
            error!(
                "Failed to save player {} on logout: {:?}",
                self.player.id, e
            );
        }

        if let Some(ref pet_handle) = self.pet_handle {
            let _ = pet_handle.tx.send(PlayerMessage::Logout).await;
        }
        crate::clan::clan_service::ClanService::remove_player_from_clan_online(
            self.player.id,
            self.player.clan_id,
        )
        .await;
        let _ = ChangeMapService::exit_current_map(&mut self.player).await;
        PLAYER_MANAGER.remove(self.player.id);
    }
}
