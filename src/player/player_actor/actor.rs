use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use crate::services::player_service;
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::{error, info};

pub struct PlayerActor {
    pub player: Player,
    pub receiver: mpsc::Receiver<PlayerMessage>,
    pub session: SessionArc,
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
        }
    }

    /// The main loop of the PlayerActor
    pub async fn run(mut self) {
        info!(
            "PlayerActor started for {} (ID: {})",
            self.player.name, self.player.id
        );

        let mut interval = tokio::time::interval(Duration::from_millis(500));

        loop {
            tokio::select! {
                // Handle incoming mailbox messages
                Some(msg) = self.receiver.recv() => {
                    match msg {
                        PlayerMessage::Logout => break,
                        m => self.handle_message(m).await,
                    }
                }

                // Periodic update
                _ = interval.tick() => {
                    self.update().await;
                }
            }
        }

        self.dispose().await;
    }

    async fn handle_message(&mut self, msg: PlayerMessage) {
        match msg {
            PlayerMessage::NetworkMessage(m) => {
                let command = m.command;
                if let Err(e) = crate::network::controller::AsyncController::process_actor(
                    &mut self.player,
                    &self.session,
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
                if text.starts_with("/") {
                    let cmd_text = &text[1..];
                    match crate::services::command::CommandService::check(
                        &mut self.player,
                        &self.session,
                        cmd_text,
                    )
                    .await
                    {
                        Ok(true) => return,
                        Ok(false) => {
                            let _ = crate::services::ServiceHandles::send_message_alert(
                                &self.player,
                                "Lệnh không tồn tại!",
                            );
                        }
                        Err(e) => {
                            error!("Command error: {:?}", e);
                        }
                    }
                } else {
                    let _ = crate::services::ServiceHandles::chat(
                        &self.session,
                        self.player.id,
                        self.player.map_id,
                        self.player.zone_id,
                        &text,
                    )
                    .await;
                }
            }
            PlayerMessage::SendPacket(m) => {
                self.session.transmit(m);
            }
            PlayerMessage::Injured { damage, piercing } => {
                let real_damage = self.player.injured(damage, piercing);
                let _ = crate::services::player_info_service::send_info_hp_mp_money(&self.player);

                let zone_opt = crate::map::zone_manager::ZONE_MANAGER
                    .get_zone(self.player.map_id, self.player.zone_id);
                if let Some(zone) = zone_opt {
                    let mut msg = Message::new(-11);
                    let _ = msg.write_int(self.player.id as i32);
                    let _ = msg.write_int(self.player.n_point.hp_current as i32);
                    let _ = msg.write_int(real_damage as i32);
                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
                }
            }
            PlayerMessage::AttackMob { mob_id } => {
                if let Err(e) =
                    crate::network::controller::AsyncController::handle_attack_mob_actor(
                        &mut self.player,
                        &self.session,
                        mob_id,
                    )
                    .await
                {
                    error!(
                        "Error handling attack mob for player {}: {:?}",
                        self.player.id, e
                    );
                }
            }
            PlayerMessage::SelectSkill { skill_template_id } => {
                let _ = crate::services::skill_service::select_skill(
                    &mut self.player,
                    skill_template_id,
                );
            }
            PlayerMessage::UseSkill { msg } => {
                let _ = crate::services::skill_service::execute_skill(&mut self.player, None, None)
                    .await;
            }
            PlayerMessage::PickItem { item_map_id } => {
                let zone_opt = crate::map::zone_manager::ZONE_MANAGER
                    .get_zone(self.player.map_id, self.player.zone_id);
                if let Some(zone_handle) = zone_opt {
                    match zone_handle.remove_item(item_map_id).await {
                        Ok(Some(mut item_map)) => {
                            if let Some(template) = item_map.item_template {
                                let mut item = crate::item::item::Item::with_template(
                                    template,
                                    item_map.quantity,
                                );
                                item.item_options = item_map.options.clone();

                                if crate::item::inventory_service::InventoryService::add_item_bag(
                                    &mut self.player,
                                    item,
                                )
                                .is_ok()
                                {
                                    let msg = crate::map::services::item_map_service::ItemMapService::build_pickup_notification_message(
                                        item_map_id,
                                        self.player.id,
                                    );
                                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(
                                        &zone_handle,
                                        msg,
                                    );

                                    let disappear_msg = crate::map::services::item_map_service::ItemMapService::build_item_disappear_message(item_map_id);
                                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(
                                        &zone_handle,
                                        disappear_msg,
                                    );
                                }
                            }
                        }
                        Ok(None) => {
                            // Item not found
                        }
                        Err(e) => {
                            tracing::error!("Error picking item: {:?}", e);
                        }
                    }
                }
            }
            PlayerMessage::ApplyHuytSaoBuff { percent_hp } => {
                self.player.effect_skill.ti_le_hp_huyt_sao = percent_hp;
                self.player.effect_skill.last_time_huyt_sao =
                    crate::utils::time::current_time_millis();
                self.player.n_point.huyt_sao_buff = percent_hp;
                self.player.n_point.set_base_point();
                let heal_amount =
                    (self.player.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
                self.player.n_point.hp_current =
                    self.player.n_point.hp_current.saturating_add(heal_amount);
                if self.player.n_point.hp_current > self.player.n_point.hp_max {
                    self.player.n_point.hp_current = self.player.n_point.hp_max;
                }
                let _ = crate::services::player_info_service::send_point_info_sync(&self.player);
                let _ = crate::services::player_info_service::send_info_hp_mp_money(&self.player);
            }
            PlayerMessage::CombineOpenTab {
                type_combine,
                npc_id,
            } => {
                let _ = crate::combine::combine_service::handle_open_tab_actor(
                    &mut self.player,
                    &self.session,
                    type_combine,
                    npc_id,
                )
                .await;
            }
            PlayerMessage::CombineShowInfo { index } => {
                let _ = crate::combine::combine_service::handle_show_info_actor(
                    &mut self.player,
                    &self.session,
                    index,
                )
                .await;
            }
            PlayerMessage::CombineConfirm => {
                let _ = crate::combine::combine_service::handle_confirm_actor(
                    &mut self.player,
                    &self.session,
                )
                .await;
            }
            PlayerMessage::ItemAction {
                type_action,
                where_item,
                index,
            } => {
                let _ = crate::item::item_controller::ItemController::handle_item_action_actor(
                    &self.session,
                    &mut self.player,
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
                let _ = crate::item::item_controller::ItemController::handle_get_item_actor(
                    &self.session,
                    &mut self.player,
                    type_item_inventory,
                    index,
                )
                .await;
            }
            PlayerMessage::HoiSinh => {
                let _ = crate::services::player_service::hoi_sinh(&mut self.player);
            }
            PlayerMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.player.clone());
            }
            PlayerMessage::UpdateSkillShortcuts { shortcuts } => {
                self.player.player_skill.skill_shortcut = shortcuts;
                let _ = crate::services::skill_service::send_skill_shortcut(&self.player);
            }
            PlayerMessage::IncreasePoint {
                type_increment,
                point,
            } => {
                self.player.n_point.increase_point(type_increment, point);
                let _ = crate::services::player_info_service::send_point_info_sync(&self.player);
            }
            PlayerMessage::CreateMenu {
                npc_id,
                npc_say,
                menu_options,
                state,
            } => {
                let options: Vec<&str> = menu_options.iter().map(|s| s.as_str()).collect();
                let _ = crate::npc::npc_service::npc_service::create_menu_player(
                    &mut self.player,
                    npc_id,
                    &npc_say,
                    options,
                    state,
                );
            }
            PlayerMessage::FinishLoadMap => {
                tracing::info!(
                    "ENTERING: PlayerMessage::FinishLoadMap (player: {})",
                    self.player.id
                );
                let _ =
                    crate::map::services::change_map_service::ChangeMapService::finish_load_map(
                        &self.player,
                        &self.session,
                    )
                    .await;
                tracing::info!(
                    "EXITING: PlayerMessage::FinishLoadMap (player: {})",
                    self.player.id
                );
            }
            PlayerMessage::Modify(f) => {
                f(&mut self.player);
            }
            PlayerMessage::Move { x, y } => {
                if self.player.is_die() {
                    return;
                }
                self.player.location.x = x;
                self.player.location.y = y;

                let zone_opt = crate::map::zone_manager::ZONE_MANAGER
                    .get_zone(self.player.map_id, self.player.zone_id);
                if let Some(zone) = zone_opt {
                    let mut msg = Message::new(-7);
                    let _ = msg.write_int(self.player.id as i32);
                    let _ = msg.write_short(self.player.location.x);
                    let _ = msg.write_short(self.player.location.y);
                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
                }
            }
            PlayerMessage::ChangeMap {
                map_id,
                zone_id,
                x,
                y,
                space_type,
            } => {
                if let Some(zone) = crate::map::zone_manager::ZONE_MANAGER.get_zone(map_id, zone_id)
                {
                    let _ = crate::map::services::change_map_service::ChangeMapService::change_map_to_zone(
                        &mut self.player,
                        &zone,
                        x,
                        y,
                        space_type,
                        &self.session,
                    ).await;
                }
            }
            PlayerMessage::UpdateTick => {
                self.update().await;
            }
            PlayerMessage::Logout => {
                // Handled in run() loop
            }
        }
    }

    async fn update(&mut self) {
        if self.player.before_dispose {
            return;
        }

        let events = self.player.update();
        if !events.is_empty() {
            player_service::handle_player_events(&mut self.player, events).await;
        }
    }

    async fn dispose(&mut self) {
        info!(
            "PlayerActor disposing for {} (ID: {})",
            self.player.name, self.player.id
        );
        if let Err(e) = player_service::save_player(&self.player).await {
            error!(
                "Failed to save player {} on logout: {:?}",
                self.player.id, e
            );
        }

        let _ = crate::map::services::change_map_service::ChangeMapService::exit_map_actor(
            &mut self.player,
        )
        .await;

        crate::player::player_manager::PLAYER_MANAGER.remove(self.player.id);
    }
}
