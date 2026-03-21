use crate::boss::boss_id::BOSS_TAU_PAY_PAY;
use crate::constant::const_item::{ITEM_DUI_GA_NUONG, ITEM_EM_BE};
use crate::constant::task_id;
use crate::item::item_controller::ItemController;
use crate::item::use_item_service::UseItemResult;
use crate::item::{InventoryService, Item};
use crate::map::services::training_services;
use crate::map::zone_manager::ZONE_MANAGER;

use crate::map::{item_map_service, ChangeMapService, ItemMapService, SpaceShipType};
use crate::matches::{pvp_manager, TypeLosePvp};
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Fusion;
use crate::services::black_ball_war_service::BlackBallWarService;
use crate::player::command::CommandService;
use crate::services::effect_skill_service::EffectSkillService;
use crate::services::player_tnsm_services::{tiemnang_sucmanh_add, TypeTNSM};
use crate::services::task_service::TaskService;
use crate::services::task_utils::TaskUtils;
use crate::services::{player_info_service, player_service, player_tnsm_services, ServiceHandles};
use crate::templates::fusion_template_manager;
use std::time::{Duration, Instant};
use tokio::sync::mpsc;
use tracing::{error, info};

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
    async fn handle_message(&mut self, msg: PlayerMessage) {
        match msg {
            PlayerMessage::TaskAction(task_type, target_id) => {
                self.handle_task_action(task_type, target_id).await;
            }
            PlayerMessage::NetworkMessage(m) => {
                self.handle_network_message(m).await;
            }
            PlayerMessage::Chat { text } => {
                self.handle_chat(text).await;
            }
            PlayerMessage::SendPacket(m) => {
                self.session.transmit(m);
            }
            PlayerMessage::Injured {
                damage,
                piercing,
                from_mob,
                attacker_id: _,
            } => {
                self.handle_injured(damage, piercing, from_mob).await;
            }
            PlayerMessage::AttackMob { mob_id } => {
                self.handle_attack_mob(mob_id).await;
            }
            PlayerMessage::AttackPlayer { player_id } => {
                self.handle_attack_player(player_id).await;
            }
            PlayerMessage::SelectSkill { skill_template_id } => {
                self.handle_select_skill(skill_template_id);
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
                self.handle_combine_open_tab(type_combine, npc_id).await;
            }
            PlayerMessage::CombineShowInfo { index } => {
                self.handle_combine_show_info(index).await;
            }
            PlayerMessage::CombineConfirm => {
                self.handle_combine_confirm().await;
            }
            PlayerMessage::ItemAction {
                type_action,
                where_item,
                index,
            } => {
                self.handle_item_action(type_action, where_item, index)
                    .await;
            }
            PlayerMessage::GetItem {
                type_item_inventory,
                index,
            } => {
                self.handle_get_item(type_item_inventory, index).await;
            }
            PlayerMessage::HoiSinh => {
                let _ = player_service::hoi_sinh(&mut self.player);
            }
            PlayerMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.player.clone());
            }
            PlayerMessage::UpdateSkillShortcuts { shortcuts } => {
                self.handle_update_skill_shortcuts(shortcuts);
            }
            PlayerMessage::IncreasePoint {
                type_increment,
                point,
            } => {
                self.handle_increase_point(type_increment, point).await;
            }
            PlayerMessage::AddTNSM {
                type_tnsm,
                param,
                is_ori,
            } => {
                self.handle_add_tnsm(type_tnsm, param, is_ori);
            }
            PlayerMessage::CreateMenu {
                npc_id,
                npc_say,
                menu_options,
                state,
            } => {
                self.handle_create_menu(npc_id, npc_say, menu_options, state);
            }
            PlayerMessage::FinishLoadMap => {
                self.handle_finish_load_map().await;
            }
            PlayerMessage::Modify(f) => {
                f(&mut self.player);
            }
            PlayerMessage::Move { x, y } => {
                self.handle_move(x, y).await;
            }
            PlayerMessage::ShowInfoPet => {
                self.handle_show_info_pet().await;
            }
            PlayerMessage::ChangeMap {
                map_id,
                zone_id,
                x,
                y,
                space_type,
            } => {
                self.handle_change_map(map_id, zone_id, x, y, space_type)
                    .await;
            }
            PlayerMessage::UpdateTick => {
                self.update().await;
            }
            PlayerMessage::Logout => {}
            PlayerMessage::HandleAnTroi(is_an_troi, time_an_troi, caster_id) => {
                self.handle_an_troi(is_an_troi, time_an_troi, caster_id);
            }
            PlayerMessage::SetPetHandle(handle) => {
                self.pet_handle = Some(handle);
            }
            PlayerMessage::Fusion {
                type_fusion,
                template_id,
            } => {
                self.handle_fusion(type_fusion, template_id).await;
            }
            PlayerMessage::Unfusion => {
                self.handle_unfusion().await;
            }
            PlayerMessage::Pet(pet_msg) => {
                self.handle_pet_forward(pet_msg);
            }
            PlayerMessage::PetAskPea { pet_id } => {
                self.handle_pet_ask_pea(pet_id).await;
            }
            PlayerMessage::ClearPetHandle => {
                self.pet_handle = None;
                self.player.pet_id = None;
                info!("Cleared pet handle for player {}", self.player.id);
            }
            PlayerMessage::MagicTree(magic_tree_msg) => {
                use crate::player::player_actor::MagicTreeMsg;
                match magic_tree_msg {
                    MagicTreeMsg::OpenOrLoad(action) => self.handle_magic_tree_action(action),
                    MagicTreeMsg::Harvest => self.handle_magic_tree_harvest(),
                    MagicTreeMsg::FastRespawn => self.handle_magic_tree_fast_respawn(),
                    MagicTreeMsg::Upgrade => self.handle_magic_tree_upgrade(),
                    MagicTreeMsg::FastUpgrade => self.handle_magic_tree_fast_upgrade(),
                    MagicTreeMsg::Unupgrade => self.handle_magic_tree_unupgrade(),
                }
            }
            PlayerMessage::RadarAction(action, mut msg) => {
                let _ = self.handle_radar_action(action, &mut msg).await;
            }
            PlayerMessage::ChangeMapCapsule(index) => {
                self.handle_change_map_capsule(index).await;
            }
            PlayerMessage::ChangeMapBlackBall(index) => {
                self.handle_change_map_black_ball(index).await;
            }
            PlayerMessage::SendInfoTo(target_handle) => {
                self.handle_send_info_to(target_handle);
            }
            PlayerMessage::SendInfoToAll(targets) => {
                self.handle_send_info_to_all(targets);
            }
            PlayerMessage::CallTrainingBoss {
                boss_id,
                is_thachdau,
            } => {
                self.handle_call_training_boss(boss_id, is_thachdau);
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Handler functions
    // ─────────────────────────────────────────────────────────

    async fn handle_task_action(
        &mut self,
        task_type: crate::constant::task_type::TaskType,
        target_id: String,
    ) {
        let old_task = (
            TaskUtils::get_id_task(&self.player),
            TaskUtils::get_task_index(&self.player),
        );
        TaskService::check_done_task(&mut self.player, task_type, &target_id);
        self.handle_task_advance(old_task).await;
    }

    async fn handle_network_message(&mut self, m: Message) {
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

    async fn handle_chat(&mut self, text: String) {
        if let Ok(false) = CommandService::check(&mut self.player, &self.session, &text).await {
            let _ = ServiceHandles::chat(
                &self.session,
                self.player.id,
                self.player.map_id,
                self.player.zone_id,
                &text,
            );
        }
    }

    fn handle_select_skill(&mut self, skill_template_id: i32) {
        let _ = crate::services::skill_service::select_skill(&mut self.player, skill_template_id);
    }

    async fn handle_combine_open_tab(
        &mut self,
        type_combine: crate::combine::combine_type::CombineType,
        npc_id: i16,
    ) {
        let _ = crate::combine::combine_service::handle_open_tab_actor(
            &mut self.player,
            &self.session,
            type_combine,
            npc_id,
        );
    }

    async fn handle_combine_show_info(&mut self, index: Vec<i16>) {
        let _ = crate::combine::combine_service::handle_show_info_actor(
            &mut self.player,
            &self.session,
            index,
        )
        .await;
    }

    async fn handle_combine_confirm(&mut self) {
        let _ =
            crate::combine::combine_service::handle_confirm_actor(&mut self.player, &self.session)
                .await;
    }

    async fn handle_item_action(
        &mut self,
        type_action: crate::item::type_item_inventory::TypeItemAction,
        where_item: i8,
        index: i8,
    ) {
        if let Ok(Some(use_result)) = ItemController::handle_item_action_actor(
            &self.session,
            &mut self.player,
            type_action,
            where_item,
            index,
        )
        .await
        {
            if let UseItemResult::RecoveredHpMp { hp_ki, stamina, .. } = use_result {
                if let Some(ref pet_handle) = self.pet_handle {
                    let _ = pet_handle
                        .send(PetMessage::HealPet {
                            hp: hp_ki,
                            mp: hp_ki,
                            stamina,
                        })
                        .await;
                }
            }
        }
    }

    async fn handle_get_item(
        &mut self,
        type_item_inventory: crate::item::type_item_inventory::TypeItemInventory,
        index: i8,
    ) {
        let _ = ItemController::handle_get_item_actor(
            &self.session,
            &mut self.player,
            type_item_inventory,
            index,
        )
        .await;
    }

    fn handle_update_skill_shortcuts(&mut self, shortcuts: Vec<i8>) {
        self.player.player_skill.skill_shortcut = shortcuts;
        let _ = crate::services::skill_service::send_skill_shortcut(&self.player);
    }

    async fn handle_increase_point(&mut self, type_increment: u8, point: i16) {
        let old_task = (
            TaskUtils::get_id_task(&self.player),
            TaskUtils::get_task_index(&self.player),
        );
        self.player.n_point.increase_point(type_increment, point);
        self.player.n_point.cal_point();
        player_info_service::send_point_info_sync(&self.player);

        let _ = TaskService::check_done_task_scripts(&mut self.player, "2");
        let _ = TaskService::check_done_task_scripts(&mut self.player, "1");
        self.handle_task_advance(old_task).await;
    }

    fn handle_add_tnsm(&mut self, type_tnsm: TypeTNSM, param: i64, is_ori: bool) {
        player_tnsm_services::tiemnang_sucmanh_add(&mut self.player, type_tnsm, param, is_ori);
    }

    fn handle_create_menu(
        &mut self,
        npc_id: i16,
        npc_say: String,
        menu_options: Vec<String>,
        state: crate::constant::menu_enum::MenuId,
    ) {
        let options: Vec<&str> = menu_options.iter().map(|s| s.as_str()).collect();
        let _ = crate::npc::npc_service::npc_service::create_menu_player(
            &mut self.player,
            npc_id,
            &npc_say,
            options,
            state,
        );
    }

    async fn handle_finish_load_map(&mut self) {
        ChangeMapService::finish_load_map(&self.player, Some(&self.session)).await;
        TaskService::send_info_current_task(&self.player);
        TaskService::send_tutorial_task_0_0_0(&self.player, "GameNro Server");
        TaskService::check_auto_skip_task_home(&mut self.player);

        if self.player.map_id == 47 {
            let task_id = TaskUtils::get_id_task(&self.player);
            let task_index = TaskUtils::get_task_index(&self.player);
            if task_id >= task_id::TASK_7 && task_index > 0 {
                training_services::call_boss_by_id(&mut self.player, BOSS_TAU_PAY_PAY, false);
            }
        }

        tracing::info!(
            "EXITING: PlayerMessage::FinishLoadMap (player: {})",
            self.player.id
        );
    }

    async fn handle_move(&mut self, x: i16, y: i16) {
        if self.player.is_die() {
            return;
        }

        if self.player.effect_skill.use_troi {
            self.release_hold();
        }

        self.player.location.set_position(x, y);
        let map_id = self.player.map_id;
        TaskService::check_done_task_go_to_map_position(&mut self.player, map_id, x);
        if let Some(ref pet_handle) = self.pet_handle {
            let _ = pet_handle.send(PetMessage::MasterLocation(x, y)).await;
        }

        let zone_opt = ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id);
        if let Some(zone) = zone_opt {
            let mut msg = Message::new(-7);
            let _ = msg.write_int(self.player.id as i32);
            let _ = msg.write_short(self.player.location.x);
            let _ = msg.write_short(self.player.location.y);
            let _ = ServiceHandles::send_to_all_in_zone(&zone, msg);
            self.player.sync_public_state();
        }
    }

    async fn handle_show_info_pet(&mut self) {
        if let Some(ref pet_handle) = self.pet_handle {
            let (tx, rx) = tokio::sync::oneshot::channel();
            if let Ok(_) = pet_handle.send(PetMessage::GetSnapshot(tx)).await {
                let player_clone = self.player.clone();
                tokio::spawn(async move {
                    if let Ok(pet_snapshot) = rx.await {
                        let _ = player_info_service::send_info_pet(&player_clone, &pet_snapshot);
                    }
                });
            }
        }
    }

    async fn handle_change_map(
        &mut self,
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
            self.sync_pet_map().await;
            ChangeMapService::change_map_to_zone(
                &mut self.player,
                &zone,
                x,
                y,
                space_type,
                Some(&self.session),
            )
            .await;
        } else {
            tracing::warn!(
                "[ACTOR] ChangeMap failed: zone not found for map {} zone {}",
                map_id,
                zone_id
            );
        }
    }

    fn handle_an_troi(&mut self, is_an_troi: bool, time_an_troi: u64, caster_id: Option<u64>) {
        if is_an_troi {
            self.player.effect_skill.an_troi = true;
            self.player.effect_skill.time_an_troi = time_an_troi;
            self.player.effect_skill.start_time_an_troi = crate::utils::time::current_time_millis();
            self.player.effect_skill.pl_troi_id = caster_id;
        } else {
            EffectSkillService::remove_an_troi(&mut self.player);
        }
    }

    fn handle_pet_forward(&mut self, pet_msg: PetMessage) {
        if self.player.fusion.type_fusion != 0 {
            if matches!(pet_msg, PetMessage::ChangeStatus(_)) {
                tracing::info!(
                    "[PET] Blocked status change while player {} is fused",
                    self.player.id
                );
                return;
            }
        }
        if let Some(handle) = &self.pet_handle {
            handle.send_forget(pet_msg);
        }
    }

    fn handle_magic_tree_action(&mut self, action: u8) {
        info!(
            "PlayerActor: MagicTreeAction({}) for player {}",
            action, self.player.id
        );
        match action {
            1 => {
                let menu_id = self.player.magic_tree.get_menu_id();
                self.player.interaction_state.set_index_menu(menu_id);
                if let Ok(msg) = self.player.magic_tree.create_menu_message(&self.player) {
                    self.session.transmit(msg);
                }
            }
            2 => {
                if let Ok(msg) = self.player.magic_tree.create_load_message(&self.player) {
                    self.session.transmit(msg);
                }
            }
            _ => {}
        }
    }

    fn handle_magic_tree_harvest(&mut self) {
        info!(
            "PlayerActor: MagicTreeHarvest for player {}",
            self.player.id
        );
        crate::services::magic_tree_service::harvest_pea(&mut self.player);
    }

    fn handle_magic_tree_fast_respawn(&mut self) {
        info!(
            "PlayerActor: MagicTreeFastRespawn for player {}",
            self.player.id
        );
        crate::services::magic_tree_service::fast_respawn_pea(&mut self.player);
    }

    fn handle_magic_tree_upgrade(&mut self) {
        info!(
            "PlayerActor: MagicTreeUpgrade for player {}",
            self.player.id
        );
        crate::services::magic_tree_service::upgrade_magic_tree(&mut self.player);
    }

    fn handle_magic_tree_fast_upgrade(&mut self) {
        info!(
            "PlayerActor: MagicTreeFastUpgrade for player {}",
            self.player.id
        );
        crate::services::magic_tree_service::fast_upgrade_magic_tree(&mut self.player);
    }

    fn handle_magic_tree_unupgrade(&mut self) {
        info!(
            "PlayerActor: MagicTreeUnupgrade for player {}",
            self.player.id
        );
        crate::services::magic_tree_service::unupgrade_magic_tree(&mut self.player);
    }

    async fn handle_change_map_capsule(&mut self, index: i32) {
        ChangeMapService::change_map_capsule(&mut self.player, index, &self.session).await;
        self.sync_pet_map().await;
    }

    async fn handle_change_map_black_ball(&mut self, index: i8) {
        BlackBallWarService::change_map(&mut self.player, index, &self.session);
        self.sync_pet_map().await;
    }

    fn handle_send_info_to(&self, target_handle: crate::player::player_actor::PlayerHandle) {
        let _ = ServiceHandles::send_player_info_to_handle(&target_handle, &self.player);
    }

    fn handle_send_info_to_all(&self, targets: Vec<crate::player::player_actor::PlayerHandle>) {
        for target_handle in targets {
            let _ = ServiceHandles::send_player_info_to_handle(&target_handle, &self.player);
        }
    }

    fn handle_call_training_boss(&mut self, boss_id: String, is_thachdau: bool) {
        if let Err(e) = training_services::call_boss_by_id(&mut self.player, &boss_id, is_thachdau)
        {
            error!(
                "Error calling training boss for player {}: {:?}",
                self.player.id, e
            );
        }
    }

    // ─────────────────────────────────────────────────────────
    //  Internal helpers
    // ─────────────────────────────────────────────────────────

    async fn handle_task_advance(&mut self, old_task: (i32, i32)) {
        let new_task = (
            TaskUtils::get_id_task(&self.player),
            TaskUtils::get_task_index(&self.player),
        );
        if old_task != new_task {
            if let Some(zone) = ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id) {
                let _ = zone.check_spawn_task_item(self.player.id, new_task).await;
            }
        }
    }

    async fn handle_pet_ask_pea(&mut self, _pet_id: u64) {
        if let Some(index) = self
            .player
            .inventory
            .items_bag
            .iter()
            .position(|it| it.is_not_null_item() && it.get_type() == 6)
        {
            if let Some(recovery) =
                crate::item::use_item_service::UseItemService::eat_pea(&mut self.player, index)
            {
                let _ = player_info_service::send_point_info_sync(&self.player);
                let _ = player_info_service::send_current_stamina(&self.player);
                let _ = InventoryService::send_item_bag(&self.player);
                if let Some(ref pet_handle) = self.pet_handle {
                    let _ = pet_handle
                        .send(PetMessage::HealPet {
                            hp: recovery.hp_ki,
                            mp: recovery.hp_ki,
                            stamina: recovery.stamina,
                        })
                        .await;
                }
            }
        }
    }

    async fn handle_fusion(&mut self, type_fusion: i8, template_id: i32) {
        if self.player.fusion.type_fusion != 0 {
            return;
        }

        if let Some(ref pet_handle) = self.pet_handle {
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = pet_handle.send(PetMessage::GetSnapshot(tx)).await;
            if let Ok(pet_snapshot) = rx.await {
                if type_fusion == Fusion::HOP_THE_VINH_VIEN && self.player.gender == 1 {
                    ServiceHandles::send_fusion_effect(&self.player, Fusion::LUONG_LONG_NHAT_THE);
                    let _ = pet_handle.tx.send(PlayerMessage::Logout).await;
                    self.pet_handle = None;

                    let pet_power = pet_snapshot.player.n_point.power;
                    player_tnsm_services::tiemnang_sucmanh_add(
                        &mut self.player,
                        TypeTNSM::All,
                        pet_power,
                        false,
                    );
                    player_info_service::send_pet_info(&self.player);
                    return;
                }
                if let Some(template) = fusion_template_manager::get(template_id) {
                    self.player.fusion.type_fusion = type_fusion;
                    self.player.fusion.template_id = template_id;

                    self.player.n_point.hp_fusion = pet_snapshot.player.n_point.hp_max / 2;
                    self.player.n_point.mp_fusion = pet_snapshot.player.n_point.mp_max / 2;
                    self.player.n_point.dame_fusion = pet_snapshot.player.n_point.dame / 2;
                    self.player.n_point.def_fusion = pet_snapshot.player.n_point.def / 2;
                    self.player.n_point.crit_fusion =
                        (pet_snapshot.player.n_point.crit / 2) + template.crit_bonus;

                    self.player.n_point.hp_fusion_tl = template.hp_percent as i32;
                    self.player.n_point.mp_fusion_tl = template.mp_percent as i32;
                    self.player.n_point.dame_fusion_tl = template.dame_percent as i32;

                    self.player.n_point.cal_point();
                    self.player.n_point.set_hp(self.player.n_point.hp_max);
                    self.player.n_point.set_mp(self.player.n_point.mp_max);

                    pet_handle.send(PetMessage::Fusion(true)).await;

                    player_info_service::send_point_info_sync(&self.player);
                    player_info_service::send_info_hp_mp_money(&self.player);
                    ServiceHandles::send_cai_trang(&self.player);
                    ServiceHandles::send_fusion_effect(&self.player, type_fusion);
                    if type_fusion == Fusion::LUONG_LONG_NHAT_THE {
                        self.player.fusion.last_time_fusion =
                            crate::utils::time::current_time_millis();
                        let icon_id: i16 = if self.player.gender == 1 { 3901 } else { 3790 };
                        let _ = ServiceHandles::send_item_time_client(&self.player, icon_id, 600);
                    }
                }
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

        self.player.n_point.hp_fusion_tl = 0;
        self.player.n_point.mp_fusion_tl = 0;
        self.player.n_point.dame_fusion_tl = 0;

        self.player.n_point.cal_point();

        if let Some(ref pet_handle) = self.pet_handle {
            let _ = pet_handle
                .send(PetMessage::ChangeStatus(
                    crate::player::player_actor::pet::PetStatus::Follow,
                ))
                .await;
        }

        let _ = player_info_service::send_point_info_sync(&self.player);
        let _ = player_info_service::send_info_hp_mp_money(&self.player);
        let _ = ServiceHandles::send_cai_trang(&self.player);
        let _ = ServiceHandles::send_fusion_effect(&self.player, 0);
    }

    async fn update(&mut self) {
        self.player.magic_tree.update();
        if self.player.before_dispose {
            return;
        }

        if self.player.n_point.hp_current <= 0 && !self.player.dead_flag {
            self.player.set_die();
        };

        if self.player.fusion.is_timed_fusion() {
            let now = crate::utils::time::current_time_millis();
            if self.player.fusion.is_fusion_expired(now) {
                tracing::info!(
                    "[FUSION] Player {} fusion timer expired, auto-unfusion",
                    self.player.id
                );
                self.handle_unfusion().await;
            }
        }

        player_service::update_player_tick(&mut self.player);

        self.player.sync_public_state();
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
                self.prepare_for_map_change();
                ChangeMapService::change_map_waypoint_handler(&mut self.player, &self.session)
                    .await?;
                self.sync_pet_map().await;
            }
            crate::constant::cmd::cmd::GO_HOME => {
                self.prepare_for_map_change();
                ChangeMapService::go_home_handler(&mut self.player, &self.session).await?;
                self.sync_pet_map().await;
            }
            crate::constant::cmd::cmd::CHANGE_ZONE => {
                let zone_id = msg.read_byte()? as i32;
                self.prepare_for_map_change();
                ChangeMapService::change_zone(&mut self.player, zone_id, &self.session).await?;
                self.sync_pet_map().await;
            }
            _ => {
                tracing::warn!("Actor doesn't handle command {} yet", command);
            }
        }
        Ok(())
    }

    fn prepare_for_map_change(&self) {
        pvp_manager::get_pvp_handle().player_lose(self.player.id as i64, TypeLosePvp::RunsAway);
    }

    async fn handle_radar_action(&mut self, action: i8, msg: &mut Message) -> anyhow::Result<()> {
        match action {
            0 => {
                crate::services::radar_service::RadarService::send_radar(
                    &self.player,
                    &self.player.radar_cards,
                )?;
            }
            1 => {
                let card_id = msg.read_short()?;
                let any_other_used = self
                    .player
                    .radar_cards
                    .iter()
                    .any(|c| c.id != card_id && c.used == 1);
                let mut new_used = 0;
                let mut updated = false;

                if let Some(card) = self.player.radar_cards.iter_mut().find(|c| c.id == card_id) {
                    if card.level == 0 {
                        return Ok(());
                    }

                    if card.used == 0 {
                        if any_other_used {
                            ServiceHandles::send_message_alert(
                                &self.player,
                                "Số thẻ sử dụng đã đạt tối đa",
                            )?;
                            return Ok(());
                        }
                        card.used = 1;
                    } else {
                        card.used = 0;
                    }
                    new_used = card.used;
                    updated = true;
                }

                if updated {
                    crate::services::radar_service::RadarService::send_radar_1(
                        &self.player,
                        card_id,
                        new_used,
                    )?;
                    self.player.n_point.cal_point();
                    player_info_service::send_point_info_sync(&self.player)?;
                }
            }
            _ => {}
        }
        Ok(())
    }

    async fn handle_injured(&mut self, mut damage: u64, piercing: bool, from_mob: bool) {
        let was_alive = !self.player.is_die();
        let curr_time = crate::utils::time::current_time_millis();
        if from_mob {
            if self.player.charms.td_da_trau > curr_time {
                damage /= 2;
            }
            if self.player.charms.td_bat_tu > curr_time {
                let hp = self.player.n_point.hp_current as u64;
                if damage >= hp {
                    damage = hp.saturating_sub(1);
                }
            }
        }
        let real_damage = self.player.injured(damage, piercing);
        if !from_mob {
            player_info_service::send_info_hp_mp_money(&self.player);
            ServiceHandles::send_player_injured(&self.player, real_damage as i32, false, 255);
            ServiceHandles::send_hp_sync(&self.player);
        }
        if was_alive && self.player.is_die() {
            let pvp_handle = pvp_manager::get_pvp_handle();
            pvp_handle.player_lose(self.player.id as i64, TypeLosePvp::Dead);
        }
    }

    async fn handle_attack_mob(&mut self, mob_id: i32) {
        if self.player.effect_skill.use_troi {
            self.release_hold();
        }
        let zone_opt = ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id);

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
                    zone.mob_effects(mob_clone.id, mob_clone.effect_skill.clone());
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
                    space_type: SpaceShipType::None,
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

        let zone_opt = ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id);

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
                zone.mob_effects(mob.id, mob.effect_skill.clone());
            }
        } else if let Some(mut pl_target) = pl_target_snapshot {
            let _ = crate::services::skill_service::execute_skill(
                &mut self.player,
                Some(&mut pl_target),
                None,
            )
            .await;
        } else {
            let _ =
                crate::services::skill_service::execute_skill(&mut self.player, None, None).await;
        }
    }

    async fn handle_attack_player(&mut self, player_id: i32) {
        if self.player.effect_skill.use_troi {
            self.release_hold();
        }

        let zone_opt = ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(Some(target_handle)) = zone.get_player(player_id as u64).await {
                if let Some(mut target_snapshot) = target_handle.get_snapshot().await {
                    let _ = crate::services::skill_service::execute_skill(
                        &mut self.player,
                        Some(&mut target_snapshot),
                        None,
                    )
                    .await;
                }
            }
        }
    }

    async fn handle_pick_item(&mut self, item_map_id: i32) {
        let zone_handle = match ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id) {
            Some(zh) => zh,
            None => return,
        };
        let item_map_peek = match zone_handle.get_item(item_map_id).await {
            Ok(Some(it)) => it,
            _ => return,
        };
        if !item_map_peek.can_pickup(self.player.id, Some(self.player.clan_id)) {
            let _ = ServiceHandles::send_thong_bao_to_player(
                &self.player,
                "Không thể nhặt vật phẩm của người khác",
            );
            return;
        }

        match zone_handle.remove_item(item_map_id).await {
            Ok(Some(item_map)) => {
                let item_id = item_map.get_item_id();
                let item_type = item_map.get_item_type();

                if item_id == ITEM_DUI_GA_NUONG && matches!(self.player.map_id, 21 | 22 | 23) {
                    self.player.n_point.set_hp(self.player.n_point.hp_max);
                    self.player.n_point.set_mp(self.player.n_point.mp_max);
                    player_info_service::send_point_info_sync(&self.player);
                    player_info_service::send_info_hp_mp_money(&self.player);

                    let mut msg = Message::new(-20);
                    let _ = msg.write_short(item_map_id as i16);
                    let _ = msg
                        .write_utf("Bạn vừa ăn đùi gà nướng, HP và KI đã được hồi phục hoàn toàn");
                    self.session.transmit(msg);

                    let pickup_msg = ItemMapService::build_pickup_notification_message(
                        item_map_id,
                        self.player.id,
                    );
                    let _ = ServiceHandles::send_to_other_in_zone(
                        &zone_handle,
                        pickup_msg,
                        self.player.id,
                    );
                    let disappear_msg = ItemMapService::build_item_disappear_message(item_map_id);
                    let _ = ServiceHandles::send_to_all_in_zone(&zone_handle, disappear_msg);
                    return;
                }
                if item_id == ITEM_EM_BE && matches!(self.player.map_id, 42 | 43 | 44) {
                    let mut msg = Message::new(-20);
                    let _ = msg.write_short(item_map_id as i16);
                    let _ = msg.write_utf("Wow, một em bé dễ thương!");
                    self.session.transmit(msg);

                    TaskService::check_done_task_pick_item(&mut self.player, &item_id.to_string());

                    let pickup_msg = ItemMapService::build_pickup_notification_message(
                        item_map_id,
                        self.player.id,
                    );
                    let _ = ServiceHandles::send_to_other_in_zone(
                        &zone_handle,
                        pickup_msg,
                        self.player.id,
                    );
                    let disappear_msg = ItemMapService::build_item_disappear_message(item_map_id);
                    let _ = ServiceHandles::send_to_all_in_zone(&zone_handle, disappear_msg);
                    return;
                }
                if let Some(template) = item_map.item_template.clone() {
                    let mut item = Item::with_template(template, item_map.quantity);
                    item.item_options = item_map.options.clone();
                    let item_template_id = item.template.as_ref().map(|t| t.id as i32).unwrap_or(0);

                    match InventoryService::add_item_bag(&mut self.player, item) {
                        Ok(_) => {
                            let msg = ItemMapService::build_pickup_notification_message(
                                item_map_id,
                                self.player.id,
                            );
                            ServiceHandles::send_to_other_in_zone(
                                &zone_handle,
                                msg,
                                self.player.id,
                            );

                            let disappearing_msg =
                                ItemMapService::build_item_disappear_message(item_map_id);
                            ServiceHandles::send_to_all_in_zone(&zone_handle, disappearing_msg);

                            if item_type >= 0 && item_type < 5 {
                                let mut msg = Message::new(-20);
                                let _ = msg.write_short(item_map_id as i16);
                                let _ = msg.write_utf(&format!(
                                    "Bạn nhận được {}",
                                    item_map
                                        .item_template
                                        .as_ref()
                                        .map(|t| t.name.clone())
                                        .unwrap_or_default()
                                ));
                                self.session.transmit(msg);
                            } else if matches!(item_type, 9 | 10 | 34) && item_map.quantity > 30000
                            {
                                let mut msg = Message::new(-20);
                                let _ = msg.write_short(item_map_id as i16);
                                let _ = msg.write_utf(&format!(
                                    "Bạn vừa nhận được {} {}",
                                    item_map.quantity,
                                    item_map
                                        .item_template
                                        .as_ref()
                                        .map(|t| t.name.clone())
                                        .unwrap_or_default()
                                ));
                                self.session.transmit(msg);
                            }

                            TaskService::check_done_task_pick_item(
                                &mut self.player,
                                &item_template_id.to_string(),
                            );
                        }
                        Err(_) => {
                            let _ = zone_handle.add_item(item_map).await;
                        }
                    }
                }
            }
            _ => {}
        }
    }

    async fn handle_huyt_sao_buff(&mut self, percent_hp: i32) {
        self.player.effect_skill.ti_le_hp_huyt_sao = percent_hp;
        self.player.effect_skill.last_time_huyt_sao = crate::utils::time::current_time_millis();
        self.player.n_point.huyt_sao_buff = percent_hp;
        self.player.stats_need_update = true;
        let heal_amount = (self.player.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
        self.player.n_point.current_hp_add(heal_amount);
        let _ = player_info_service::send_point_info_sync(&self.player);
        let _ = player_info_service::send_info_hp_mp_money(&self.player);
    }

    fn release_hold(&mut self) {
        if let Some(zone) = ZONE_MANAGER.get_zone(self.player.map_id, self.player.zone_id) {
            if let Some(mob_id) = self.player.effect_skill.mob_an_troi_id {
                zone.remove_mob_hold(mob_id, self.player.id);
            }
            if let Some(target_id) = self.player.effect_skill.pl_an_troi_id {
                zone.remove_player_hold(target_id, self.player.id);
            }
        }
        EffectSkillService::remove_use_troi(&mut self.player);
    }

    async fn dispose(&mut self) {
        info!(
            "PlayerActor disposing for {} (ID: {})",
            self.player.name, self.player.id
        );
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
