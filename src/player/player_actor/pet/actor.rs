use crate::map;
use crate::map::models::zone::ZoneHandle;
use crate::map::zone_manager::{self, ZONE_MANAGER};
use crate::network::message::Message;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::PlayerHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::ServiceHandles;
use crate::utils::time;
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::info;

use super::{Pet, PetStatus};

pub struct PetActor {
    pub pet: Pet,
    pub receiver: mpsc::Receiver<PlayerMessage>,
}

impl PetActor {
    pub fn new(pet: Pet, receiver: mpsc::Receiver<PlayerMessage>) -> Self {
        Self { pet, receiver }
    }

    pub async fn run(mut self) {
        info!(
            "PetActor started for {} (Master: {})",
            self.pet.player.name, self.pet.master_id
        );

        let mut interval = tokio::time::interval(Duration::from_millis(500));
        loop {
            tokio::select! {
                Some(msg) = self.receiver.recv() => {
                    match msg {
                        PlayerMessage::Logout => break,
                        PlayerMessage::Pet(pet_msg) => self.handle_pet_message(pet_msg).await,
                        PlayerMessage::UpdateTick => self.update().await,
                        PlayerMessage::Injured {
                            damage,
                            piercing,
                            from_mob: _,
                            attacker_id: _,
                        } => {
                            let actual_damage = self.pet.player.injured(damage, piercing);
                            info!(
                                "Pet {} took {} damage (after armor: {}). HP left: {}",
                                self.pet.player.id,
                                damage,
                                actual_damage,
                                self.pet.player.n_point.hp_current
                            );
                        }
                        PlayerMessage::GetSnapshot(tx) => {
                            let _ = tx.send(self.pet.player.clone());
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
                        _ => {}
                    }
                }
                _ = interval.tick() => {
                    self.update().await;
                }
            }
        }
        self.dispose().await;
    }

    async fn dispose(&mut self) {
        info!(
            "PetActor disposing for {} (ID: {})",
            self.pet.player.name, self.pet.player.id
        );

        let _ = crate::map::ChangeMapService::exit_map_actor(&mut self.pet.player).await;

        crate::player::player_manager::PLAYER_MANAGER.remove(self.pet.player.id);
    }

    async fn handle_change_map(
        &mut self,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        _space_type: crate::map::services::change_map_models::SpaceShipType,
    ) {
        if self.pet.status == PetStatus::GoHome || self.pet.status == PetStatus::Fusion {
            return;
        }

        if self.pet.player.is_die() {
            return;
        }

        if let Some(zone) = zone_manager::ZONE_MANAGER.get_zone(map_id, zone_id) {
            let _ = map::ChangeMapService::exit_map_actor(&mut self.pet.player).await;
            self.pet.player.location.x = x;
            self.pet.player.location.y = y;
            let _ = map::ChangeMapService::go_to_map(&mut self.pet.player, &zone, None).await;
            tracing::info!(
                "Pet {} changed map to {} zone {}",
                self.pet.player.id,
                map_id,
                zone_id
            );
        }
    }

    async fn handle_pet_message(&mut self, msg: PetMessage) {
        match msg {
            PetMessage::ChangeStatus(status) => {
                self.pet.status = status;
                match status {
                    PetStatus::Follow => {
                        self.pet_chat(Some("Ok con theo sư phụ")).await;
                    }
                    PetStatus::Protect => {
                        self.pet_chat(Some("Ok con sẽ bảo vệ sư phụ")).await;
                    }
                    PetStatus::Attack => {
                        self.pet_chat(Some("Ok sư phụ để con lo cho")).await;
                    }
                    PetStatus::GoHome => {
                        self.pet_chat(Some("OK con về, bibi sư phụ")).await;

                        tokio::time::sleep(std::time::Duration::from_secs(2)).await;

                        let old_zone_opt = zone_manager::ZONE_MANAGER
                            .get_zone(self.pet.player.map_id, self.pet.player.zone_id);
                        if let Some(old_zone) = old_zone_opt {
                            let mut msg = Message::new(-6);
                            let _ = msg.write_int(self.pet.player.id as i32);
                            let _ = ServiceHandles::send_to_all_in_zone(&old_zone, msg);
                            let _ = old_zone.remove_player(self.pet.player.id).await;
                        }
                        let home_map_id = 21 + self.pet.player.gender as i32;
                        self.pet.player.map_id = home_map_id;
                        self.pet.player.zone_id = 0;
                        self.pet.player.location.x = 200;
                        self.pet.player.location.y = 336;

                        self.pet.is_gohome = true;
                        info!(
                            "Pet {} went home to map {} (master: {})",
                            self.pet.player.id, home_map_id, self.pet.master_id
                        );
                    }
                    PetStatus::Fusion => {
                        self.pet_chat(Some("Hợp thể lẹ đi sư phụ")).await;
                    }
                    _ => {}
                }
            }
            PetMessage::UpdateTick => {
                self.update().await;
            }
            PetMessage::MasterLocation(x, y) => {
                self.pet.master_location = Some((x, y));
            }
            PetMessage::MasterAttackTarget(_target_id, _target_type) => {
                // TODO: Sẽ xử lý logic tấn công mục tiêu cùng chủ nhân
            }
            PetMessage::Fusion(_is_porata) => {
                self.pet.status = PetStatus::Fusion;
                let _ = crate::map::ChangeMapService::exit_map_actor(&mut self.pet.player).await;
            }
            PetMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.pet.clone());
            }
            PetMessage::HealPet { hp, mp, stamina } => {
                self.pet
                    .player
                    .n_point
                    .set_hp(self.pet.player.n_point.hp_current + hp);
                self.pet
                    .player
                    .n_point
                    .set_mp(self.pet.player.n_point.mp_current + mp);
                self.pet.player.n_point.stamina = self
                    .pet
                    .player
                    .n_point
                    .stamina
                    .saturating_add(stamina)
                    .min(self.pet.player.n_point.max_stamina);

                if let Some(master_handle) = PLAYER_MANAGER.get(self.pet.master_id) {
                    let pet_snapshot = self.pet.clone();
                    let _ =
                        master_handle.send_forget(PlayerMessage::Modify(Box::new(move |master| {
                            let _ = crate::services::player_info_service::send_info_pet(
                                master,
                                &pet_snapshot,
                            );
                            let _ = ServiceHandles::send_message_chat_just_for_me(
                                master,
                                &pet_snapshot.player,
                                "Cám ơn sư phụ",
                            );
                        })));
                }
                if let Some(zone) =
                    ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id)
                {
                    let _ = ServiceHandles::send_message_eat_dauthan(&self.pet.player);
                }
            }
        }
    }
    async fn idle_move(&mut self) {
        let now = time::current_time_millis();
        if now - self.pet.last_time_idle_move < 5000 {
            return;
        }
        self.pet.last_time_idle_move = now;

        if let Some((m_x, m_y)) = self.pet.master_location {
            let target_x = {
                use rand::Rng;
                let mut rng = rand::rng();
                let offset = rng.random_range(30..60);
                let direction = if rng.random_bool(0.5) { 1 } else { -1 };
                m_x + direction * offset
            };
            self.move_to(target_x, m_y).await;
        }
    }

    async fn update(&mut self) {
        if self.pet.player.n_point.hp_current <= 0 && !self.pet.player.dead_flag {
            info!(
                "Pet {} died (HP <= 0). Setting die status.",
                self.pet.player.id
            );
            self.pet.player.set_die();
            self.pet.last_time_die = time::current_time_millis();
            return;
        }

        if self.pet.player.is_die() {
            let now = time::current_time_millis();
            if self.pet.last_time_die == 0 {
                self.pet.last_time_die = now;
            }

            let time_dead = now - self.pet.last_time_die;
            if time_dead % 10000 < 500 {
                info!(
                    "Pet {} is dead. Waiting for revival: {}/50000ms",
                    self.pet.player.id, time_dead
                );
            }

            if now - self.pet.last_time_die > 50000 {
                info!("Pet {} reviving...", self.pet.player.id);
                let hp_max = self.pet.player.n_point.hp_max;
                let mp_max = self.pet.player.n_point.mp_max;
                self.pet.player.revive();
                self.pet.player.n_point.set_hp(hp_max);
                self.pet.player.n_point.set_mp(mp_max);
                self.pet.last_time_die = 0;
            }
            return;
        }

        match self.pet.status {
            PetStatus::Follow => {
                self.follow_master(60).await;
                self.idle_move().await;
            }
            PetStatus::Protect | PetStatus::Attack => {
                if let Some(target_mob_id) = self.find_mob_attack().await {
                    self.ai_attack_mob(target_mob_id).await;
                } else {
                    self.follow_master(60).await;
                    self.idle_move().await;
                }
            }
            _ => {}
        }

        self.stamina_tick().await;

        if (self.pet.player.n_point.hp_current as f32) / (self.pet.player.n_point.hp_max as f32)
            <= 0.2
            || (self.pet.player.n_point.mp_current as f32) / (self.pet.player.n_point.mp_max as f32)
                <= 0.2
            || (self.pet.player.n_point.stamina as f32)
                / (self.pet.player.n_point.max_stamina as f32)
                <= 0.2
        {
            self.ask_pea().await;
        }
    }

    async fn stamina_tick(&mut self) {
        let now = time::current_time_millis();
        if now - self.pet.last_time_stamina_update >= 10000 {
            self.pet.last_time_stamina_update = now;
            if self.pet.player.n_point.stamina > 0 {
                self.pet.player.n_point.stamina = self.pet.player.n_point.stamina.saturating_sub(1);
            }
        }
    }

    async fn ask_pea(&mut self) {
        let now = time::current_time_millis();
        if now - self.pet.last_time_ask_pea < 15000 {
            return;
        }
        self.pet.last_time_ask_pea = now;

        // Chat
        self.pet_chat(Some("Sư phụ ơi cho con đậu thần")).await;

        if let Some(master_handle) = PLAYER_MANAGER.get(self.pet.master_id) {
            master_handle.send_forget(PlayerMessage::PetAskPea {
                pet_id: self.pet.player.id,
            });
        }
    }

    async fn ai_attack_mob(&mut self, mob_id: u64) {
        if self.pet.player.is_die() {
            info!(
                "Pet {} attempted to attack mob {} while dead. Aborting.",
                self.pet.player.id, mob_id
            );
            return;
        }
        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(mobs) = zone.get_all_mobs().await {
                if let Some(mob) = mobs.into_iter().find(|m| m.id == mob_id) {
                    let dx = self.pet.player.location.x - mob.location.x;
                    let dist = dx.abs() as f32;

                    let skill_index = if dist <= 60.0 {
                        0
                    } else if self.pet.player.player_skill.skills.len() > 1 {
                        1
                    } else {
                        0
                    };

                    if let Some(skill) = self.pet.player.player_skill.skills.get(skill_index) {
                        self.pet.player.player_skill.skill_select = Some(skill.clone());
                    }

                    if skill_index == 0 && dist > 60.0 {
                        let target_x = if self.pet.player.location.x < mob.location.x {
                            mob.location.x - 40
                        } else {
                            mob.location.x + 40
                        };
                        self.move_to(target_x, mob.location.y).await;
                    } else if dist > 350.0 {
                        self.move_to(mob.location.x + 100, mob.location.y).await;
                    }
                    if (skill_index == 0 && dist <= 80.0) || (skill_index == 1 && dist <= 350.0) {
                        let mut mob_clone = mob.clone();
                        crate::services::skill_service::execute_skill(
                            &mut self.pet.player,
                            None,
                            Some(&mut mob_clone),
                        )
                        .await;
                        zone.sync_mob_effects(mob_clone.id, mob_clone.effect_skill.clone());
                        self.pet.player.n_point.stamina =
                            self.pet.player.n_point.stamina.saturating_sub(1);
                        self.pet_chat(None).await;
                    }
                }
            }
        }
    }

    async fn move_to(&mut self, x: i16, y: i16) {
        self.pet.player.location.x = x;
        self.pet.player.location.y = y;

        let zone_opt = crate::map::zone_manager::ZONE_MANAGER
            .get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            let mut msg = Message::new(-7);
            let _ = msg.write_int(self.pet.player.id as i32);
            let _ = msg.write_short(self.pet.player.location.x);
            let _ = msg.write_short(self.pet.player.location.y);
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
        }
    }

    async fn follow_master(&mut self, dis: i16) {
        if self.pet.player.is_die() {
            return;
        }
        if let Some((m_x, m_y)) = self.pet.master_location {
            let p_x = self.pet.player.location.x;
            let p_y = self.pet.player.location.y;

            let dx = p_x - m_x;
            let dy = p_y - m_y;
            let distance = ((dx as f32).powi(2) + (dy as f32).powi(2)).sqrt();

            if distance >= dis as f32 || dx.abs() < 50 {
                let target_x = if dx < 0 { m_x - 50 } else { m_x + 50 };
                let target_y = m_y;

                self.pet.player.location.x = target_x;
                self.pet.player.location.y = target_y;

                let zone_opt =
                    ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);

                if let Some(zone) = zone_opt {
                    let mut msg = Message::new(-7);
                    let _ = msg.write_int(self.pet.player.id as i32);
                    let _ = msg.write_short(self.pet.player.location.x);
                    let _ = msg.write_short(self.pet.player.location.y);
                    let _ = ServiceHandles::send_to_all_in_zone(&zone, msg);
                }
            }
        }
    }

    async fn find_mob_attack(&mut self) -> Option<u64> {
        let zone_opt = ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(mobs) = zone.get_all_mobs().await {
                let mut closest_mob = None;
                let mut min_dist = 300.0;

                for mob in mobs {
                    if mob.is_dead() {
                        continue;
                    }
                    let dx = self.pet.player.location.x - mob.location.x;
                    let dy = self.pet.player.location.y - mob.location.y;
                    let dist = ((dx as f32).powi(2) + (dy as f32).powi(2)).sqrt();
                    if dist < min_dist {
                        min_dist = dist;
                        closest_mob = Some(mob.id);
                    }
                }
                return closest_mob;
            }
        }
        None
    }

    async fn find_player_attack(&mut self) -> Option<u64> {
        None
    }

    async fn pet_chat(&mut self, text: Option<&str>) {
        let now = crate::utils::time::current_time_millis();
        if text.is_none() && now - self.pet.last_time_chat < 5000 {
            return;
        }

        let chats = [
            "Mày chán sống rồi à?",
            "Sư phụ ơi, xem con xử nó nè!",
            "Ngày này năm sau sẽ là ngày giỗ của mày",
            "Đừng hòng đụng vào sư phụ ta!",
        ];

        let chat_text = if let Some(t) = text {
            t
        } else {
            chats[self.pet.chat_index % chats.len()]
        };
        if text.is_none() {
            self.pet.chat_index += 1;
        }
        self.pet.last_time_chat = now;

        let mut msg = Message::new(44);
        let _ = msg.write_int(self.pet.player.id as i32);
        let _ = msg.write_utf(chat_text);

        let zone_opt = ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);
        if let Some(zone) = zone_opt {
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
        }
    }
}
