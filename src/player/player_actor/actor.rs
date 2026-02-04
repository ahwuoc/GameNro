use crate::map::ChangeMapService;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::{player_info_service, player_service};
use crate::{network::message::Message, services::command::CommandService};
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::{error, info};

pub struct PlayerActor {
    pub player: Player,
    pub receiver: mpsc::Receiver<PlayerMessage>,
    pub session: SessionArc,
    pub pet_handle: Option<PetHandle>,
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

    async fn handle_message(&mut self, msg: PlayerMessage) {
        match msg {
            PlayerMessage::NetworkMessage(m) => {
                let command = m.command;
                if let Err(e) = self.handle_network_command(m).await {
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
                if let Ok(false) =
                    CommandService::check(&mut self.player, &self.session, &text).await
                {
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
                self.handle_injured(damage, piercing).await;
            }
            PlayerMessage::AttackMob { mob_id } => {
                self.handle_attack_mob(mob_id).await;
            }
            PlayerMessage::SelectSkill { skill_template_id } => {
                let _ = crate::services::skill_service::select_skill(
                    &mut self.player,
                    skill_template_id,
                );
            }
            PlayerMessage::UseSkill { msg } => {
                self.handle_use_skill(msg).await;
            }
            PlayerMessage::PickItem { item_map_id } => {
                self.handle_pick_item(item_map_id).await;
            }
            PlayerMessage::ApplyHuytSaoBuff { percent_hp } => {
                self.handle_huyt_sao_buff(percent_hp).await;
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
                        Some(&self.session),
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

                if self.player.effect_skill.use_troi {
                    let zone_opt = crate::map::zone_manager::ZONE_MANAGER
                        .get_zone(self.player.map_id, self.player.zone_id);

                    if let Some(zone) = zone_opt {
                        if let Some(mob_id) = self.player.effect_skill.mob_an_troi_id {
                            zone.remove_mob_hold(mob_id, self.player.id);
                        }
                        if let Some(target_id) = self.player.effect_skill.pl_an_troi_id {
                            zone.remove_player_hold(target_id, self.player.id);
                        }
                    }
                    crate::services::effect_skill_service::EffectSkillService::remove_use_troi(
                        &mut self.player,
                    );
                }

                self.player.location.x = x;
                self.player.location.y = y;

                if let Some(ref pet_handle) = self.pet_handle {
                    let _ = pet_handle.send(PetMessage::MasterLocation(x, y)).await;
                }

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
            PlayerMessage::ShowInfoPet => {
                if let Some(ref pet_handle) = self.pet_handle {
                    let (tx, rx) = tokio::sync::oneshot::channel();
                    if let Ok(_) = pet_handle.send(PetMessage::GetSnapshot(tx)).await {
                        let player_clone = self.player.clone();
                        tokio::spawn(async move {
                            if let Ok(pet_snapshot) = rx.await {
                                let _ = player_info_service::send_info_pet(
                                    &player_clone,
                                    &pet_snapshot,
                                );
                            }
                        });
                    }
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
                    self.sync_pet_map().await;

                    let _ = crate::map::services::change_map_service::ChangeMapService::change_map_to_zone(
                        &mut self.player,
                        &zone,
                        x,
                        y,
                        space_type,
                        Some(&self.session),
                    ).await;
                }
            }
            PlayerMessage::UpdateTick => {
                self.update().await;
            }
            PlayerMessage::Logout => {
                // Handled in run() loop
            }
            PlayerMessage::HandleAnTroi(is_an_troi, time_an_troi, caster_id) => {
                if is_an_troi {
                    self.player.effect_skill.an_troi = true;
                    self.player.effect_skill.time_an_troi = time_an_troi;
                    self.player.effect_skill.start_time_an_troi =
                        crate::utils::time::current_time_millis();
                    self.player.effect_skill.pl_troi_id = caster_id;
                } else {
                    crate::services::effect_skill_service::EffectSkillService::remove_an_troi(
                        &mut self.player,
                    );
                }
            }
            PlayerMessage::SetPetHandle(handle) => {
                self.pet_handle = Some(handle);
            }
            PlayerMessage::Fusion(type_fusion) => {
                self.handle_fusion(type_fusion).await;
            }
            PlayerMessage::Unfusion => {
                self.handle_unfusion().await;
            }
            PlayerMessage::Pet(_) => {
                // Ignore pet messages in master actor
            }
        }
    }

    async fn handle_fusion(&mut self, type_fusion: i8) {
        if let Some(ref pet_handle) = self.pet_handle {
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = pet_handle.send(PetMessage::GetSnapshot(tx)).await;
            if let Ok(pet_snapshot) = rx.await {
                self.player.fusion.type_fusion = type_fusion;
                self.player.n_point.hp_fusion = pet_snapshot.player.n_point.hp_max / 2;
                self.player.n_point.mp_fusion = pet_snapshot.player.n_point.mp_max / 2;
                self.player.n_point.dame_fusion = pet_snapshot.player.n_point.dame / 2;
                self.player.n_point.def_fusion = pet_snapshot.player.n_point.def / 2;
                self.player.n_point.crit_fusion = pet_snapshot.player.n_point.crit / 2;

                self.player.n_point.cal_point();
                self.player.n_point.set_hp(self.player.n_point.hp_max);
                self.player.n_point.set_mp(self.player.n_point.mp_max);

                let _ = pet_handle.send(PetMessage::Fusion(true)).await;

                let _ = crate::services::player_info_service::send_point_info_sync(&self.player);
                let _ = crate::services::player_info_service::send_info_hp_mp_money(&self.player);
            }
        }
    }

    async fn handle_unfusion(&mut self) {
        self.player.fusion.type_fusion = crate::player::components::fusion::Fusion::NON_FUSION;
        self.player.n_point.hp_fusion = 0;
        self.player.n_point.mp_fusion = 0;
        self.player.n_point.dame_fusion = 0;
        self.player.n_point.def_fusion = 0;
        self.player.n_point.crit_fusion = 0;

        self.player.n_point.cal_point();

        if let Some(ref pet_handle) = self.pet_handle {
            let _ = pet_handle
                .send(PetMessage::ChangeStatus(
                    crate::player::player_actor::pet::PetStatus::Follow,
                ))
                .await;
        }

        let _ = crate::services::player_info_service::send_point_info_sync(&self.player);
        let _ = crate::services::player_info_service::send_info_hp_mp_money(&self.player);
    }

    async fn update(&mut self) {
        if self.player.before_dispose {
            return;
        }

        let _ = player_service::update_player_tick(&mut self.player).await;
    }

    async fn handle_network_command(&mut self, mut msg: Message) -> anyhow::Result<()> {
        let command = msg.command;
        match command {
            crate::constant::cmd::cmd::ATTACK_MOB => {
                let mob_id = msg.read_byte()? as i32;
                self.handle_attack_mob(mob_id).await;
            }
            crate::constant::cmd::cmd::CHANGE_MAP_WAYPOINT
            | crate::constant::cmd::cmd::CHANGE_MAP_WAYPOINT_ALT => {
                crate::map::ChangeMapService::change_map_waypoint_handler(
                    &mut self.player,
                    &self.session,
                )
                .await?;
                self.sync_pet_map().await;
            }
            crate::constant::cmd::cmd::GO_HOME => {
                crate::map::ChangeMapService::go_home_handler(&mut self.player, &self.session)
                    .await?;
                self.sync_pet_map().await;
            }
            crate::constant::cmd::cmd::CHANGE_ZONE => {
                let zone_id = msg.read_byte()? as i32;
                crate::map::services::change_map_service::ChangeMapService::open_zone_ui(
                    &mut self.player,
                )
                .await?;
                crate::map::ChangeMapService::change_zone(&mut self.player, zone_id, &self.session)
                    .await?;
                self.sync_pet_map().await;
            }
            _ => {
                tracing::warn!("Actor doesn't handle command {} yet", command);
            }
        }
        Ok(())
    }

    async fn handle_injured(&mut self, damage: u64, piercing: bool) {
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

    async fn handle_attack_mob(&mut self, mob_id: i32) {
        if self.player.effect_skill.use_troi {
            self.release_hold();
        }

        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.player.map_id, self.player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(mobs) = zone.get_all_mobs().await {
                if let Some(mob) = mobs.iter().find(|m| m.id == mob_id as u64) {
                    let mut mob_clone = mob.clone();
                    crate::services::skill_service::execute_skill(
                        &mut self.player,
                        None,
                        Some(&mut mob_clone),
                    )
                    .await;
                    // Sync effect state back to zone
                    zone.sync_mob_effects(mob_clone.id, mob_clone.effect_skill.clone());
                }
            }
        }
    }

    async fn sync_pet_map(&self) {
        if let Some(ref pet_handle) = self.pet_handle {
            let _ = pet_handle
                .tx
                .send(PlayerMessage::ChangeMap {
                    map_id: self.player.map_id,
                    zone_id: self.player.zone_id,
                    x: self.player.location.x,
                    y: self.player.location.y,
                    space_type: crate::map::services::change_map_models::SpaceShipType::None,
                })
                .await;
        }
    }

    async fn handle_use_skill(&mut self, mut msg: Message) {
        if self.player.effect_skill.use_troi {
            self.release_hold();
        }

        let status = msg.read_byte().unwrap_or(0);
        let mut pl_target_snapshot = None;
        let mut mob_target = None;

        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.player.map_id, self.player.zone_id);

        if let Some(zone) = zone_opt.clone() {
            if status == 1 {
                if let Ok(mob_id) = msg.read_byte() {
                    if let Ok(mobs) = zone.get_all_mobs().await {
                        if let Some(m) = mobs.iter().find(|m| m.id == mob_id as u64) {
                            mob_target = Some(m.clone());
                        }
                    }
                }
            } else if status == 2 {
                if let Ok(player_id) = msg.read_int() {
                    if let Ok(Some(handle)) = zone.get_player(player_id as u64).await {
                        pl_target_snapshot = handle.get_snapshot().await;
                    }
                }
            }
        }

        if let Some(mut mob) = mob_target {
            let _ = crate::services::skill_service::execute_skill(
                &mut self.player,
                None,
                Some(&mut mob),
            )
            .await;
            if let Some(zone) = zone_opt.clone() {
                zone.sync_mob_effects(mob.id, mob.effect_skill.clone());
            }
        } else {
            let _ =
                crate::services::skill_service::execute_skill(&mut self.player, None, None).await;
        }
    }

    async fn handle_pick_item(&mut self, item_map_id: i32) {
        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.player.map_id, self.player.zone_id);
        if let Some(zone_handle) = zone_opt {
            match zone_handle.remove_item(item_map_id).await {
                Ok(Some(mut item_map)) => {
                    if let Some(template) = item_map.item_template {
                        let mut item =
                            crate::item::item::Item::with_template(template, item_map.quantity);
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
                _ => {}
            }
        }
    }

    async fn handle_huyt_sao_buff(&mut self, percent_hp: i32) {
        self.player.effect_skill.ti_le_hp_huyt_sao = percent_hp;
        self.player.effect_skill.last_time_huyt_sao = crate::utils::time::current_time_millis();
        self.player.n_point.huyt_sao_buff = percent_hp;
        self.player.stats_need_update = true;
        let heal_amount = (self.player.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
        self.player.n_point.hp_current = self.player.n_point.hp_current.saturating_add(heal_amount);
        if self.player.n_point.hp_current > self.player.n_point.hp_max {
            self.player.n_point.hp_current = self.player.n_point.hp_max;
        }
        let _ = crate::services::player_info_service::send_point_info_sync(&self.player);
        let _ = crate::services::player_info_service::send_info_hp_mp_money(&self.player);
    }

    fn release_hold(&mut self) {
        if let Some(zone) =
            crate::map::zone_manager::ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id)
        {
            if let Some(mob_id) = self.player.effect_skill.mob_an_troi_id {
                zone.remove_mob_hold(mob_id, self.player.id);
            }
            if let Some(target_id) = self.player.effect_skill.pl_an_troi_id {
                zone.remove_player_hold(target_id, self.player.id);
            }
        }
        crate::services::effect_skill_service::EffectSkillService::remove_use_troi(
            &mut self.player,
        );
    }
    async fn dispose(&mut self) {
        info!(
            "PlayerActor disposing for {} (ID: {})",
            self.player.name, self.player.id
        );
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

        let _ = ChangeMapService::exit_map_actor(&mut self.player).await;
        PLAYER_MANAGER.remove(self.player.id);
    }
}
