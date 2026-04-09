use crate::map::models::zone::ZoneHandle;
use crate::map::zone_manager::{self, ZONE_MANAGER};
use crate::map::{self, ChangeMapService, SpaceShipType};
use crate::mob;
use crate::models::skill_model::Skill;
use crate::network::message::Message;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::PlayerHandle;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::{player_tnsm_services, skill_service, ServiceHandles};
use crate::utils::time;
use std::time::Duration;
use tokio::sync::mpsc;
use tracing::info;

use super::{Pet, PetStatus};

pub struct PetActor {
    pub pet: Pet,
    pub receiver: mpsc::Receiver<PlayerMessage>,
}

const MELEE_RANGE: f32 = 60.0;
const RANGED_MAX_DISTANCE: f32 = 350.0;
const MELEE_ATTACKABLE_RANGE: f32 = 80.0;
const APPROACH_THRESHOLD: f32 = 200.0;
const MOVE_OFFSET_CLOSE: i16 = 40;
const MOVE_OFFSET_FAR: i16 = 100;

#[derive(Debug, Clone, Copy, PartialEq)]
enum PetSkillType {
    Melee,
    Ranged,
}

impl PetSkillType {
    fn from_dis(dis: f32, has_multiple_skills: bool) -> Self {
        if dis <= MELEE_RANGE || !has_multiple_skills {
            Self::Melee
        } else {
            Self::Ranged
        }
    }

    fn to_index(&self) -> usize {
        match self {
            Self::Melee => 0,
            Self::Ranged => 1,
        }
    }

    fn can_attack(&self, dist: f32) -> bool {
        match self {
            Self::Melee => dist <= MELEE_ATTACKABLE_RANGE,
            Self::Ranged => dist <= RANGED_MAX_DISTANCE,
        }
    }
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
                            let _ = self.pet.player.injured(damage, piercing);
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
                        PlayerMessage::SendInfoTo(target_handle) => {
                            let _ = ServiceHandles::send_player_info_to_handle(
                                &target_handle,
                                &self.pet.player,
                            );
                        }
                        PlayerMessage::SendInfoToAll(targets) => {
                            for target_handle in targets {
                                let _ = ServiceHandles::send_player_info_to_handle(
                                    &target_handle,
                                    &self.pet.player,
                                );
                            }
                        }
                        PlayerMessage::AddTNSM {
                            type_tnsm,
                            param,
                            is_ori,
                        } => {
                            player_tnsm_services::tiemnang_sucmanh_add(
                                &mut self.pet.player,
                                type_tnsm,
                                param,
                                is_ori,
                            );
                        }
                        _ => {}
                    }
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

        let _ = ChangeMapService::exit_current_map(&mut self.pet.player).await;

        PLAYER_MANAGER.remove(self.pet.player.id);
    }

    async fn handle_change_map(
        &mut self,
        map_id: i32,
        zone_id: i32,
        x: i16,
        y: i16,
        _space_type: SpaceShipType,
    ) {
        if self.pet.status == PetStatus::GoHome || self.pet.status == PetStatus::Fusion {
            return;
        }

        if self.pet.player.is_die() {
            return;
        }

        if let Some(zone) = ZONE_MANAGER.get_zone(map_id, zone_id) {
            ChangeMapService::exit_current_map(&mut self.pet.player).await;
            self.pet.player.location.set_position(x, y);
            ChangeMapService::go_to_map(&mut self.pet.player, &zone, None).await;
            ChangeMapService::finish_load_map(&self.pet.player, None).await;
        }
    }

    async fn handle_go_home_finish(&mut self) {
        let old_zone_opt =
            zone_manager::ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);

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
        self.pet.last_time_gohome = 0;

        info!(
            "Pet {} went home to map {} (master: {})",
            self.pet.player.id, home_map_id, self.pet.master_id
        );
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
                        self.pet.last_time_gohome = time::current_time_millis();
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
                let _ = ChangeMapService::exit_current_map(&mut self.pet.player).await;
            }
            PetMessage::GetSnapshot(tx) => {
                let _ = tx.send(self.pet.clone());
            }
            PetMessage::HealPet { hp, mp, stamina } => {
                self.pet.player.n_point.current_hp_add(hp);
                self.pet.player.n_point.current_mp_add(mp);
                self.pet.player.n_point.current_stamina_add(stamina);

                if let Some(master_handle) = PLAYER_MANAGER.get(self.pet.master_id) {
                    let pet_snapshot = Box::new(self.pet.clone());
                    let _ = master_handle.send_forget(PlayerMessage::UpdatePetUI(
                        pet_snapshot,
                        Some("Cám ơn sư phụ".to_string()),
                    ));
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
        if self.pet.status == PetStatus::GoHome && self.pet.last_time_gohome > 0 {
            let now = time::current_time_millis();
            if now - self.pet.last_time_gohome >= 2000 {
                self.handle_go_home_finish().await;
                return;
            }
        }

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
                if self.use_skill_3().await
                    || self.use_skill_4().await
                    || self.use_skill_5().await
                    || self.use_skill_6().await
                    || self.use_skill_7().await
                {
                    return;
                }
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

    async fn get_skill_by_index(&self, index: usize) -> Option<Skill> {
        let skills = &self.pet.player.player_skill.skills;
        if skills.len() <= index {
            return None;
        }
        let skill = &skills[index];
        if skill.template_id == -1 {
            return None;
        }
        Some(skill.clone())
    }

    async fn use_skill_3(&mut self) -> bool {
        let Some(skill) = self.get_skill_by_index(2).await else {
            return false;
        };

        match skill.template_id {
            Skill::THAI_DUONG_HA_SAN => {
                if self.pet.player.is_skill_ready_by_index(2)
                    && self.pet.player.has_enough_mana_by_index(2)
                {
                    if self.find_mob_attack().await.is_some() {
                        self.pet.player.player_skill.skill_select = Some(skill);
                        skill_service::execute_skill(&mut self.pet.player, None, None).await;
                        return true;
                    }
                }
            }
            Skill::TAI_TAO_NANG_LUONG => {
                let hp_percent = self.pet.player.n_point.hp_current as f32
                    / self.pet.player.n_point.hp_max as f32;
                let mp_percent = self.pet.player.n_point.mp_current as f32
                    / self.pet.player.n_point.mp_max as f32;

                if (hp_percent <= 0.3 || mp_percent <= 0.3)
                    && self.pet.player.is_skill_ready_by_index(2)
                    && self.pet.player.has_enough_mana_by_index(2)
                {
                    self.pet.player.player_skill.skill_select = Some(skill);
                    skill_service::execute_skill(&mut self.pet.player, None, None).await;
                    return true;
                }
            }
            Skill::SOCOLA => {
                if self.pet.player.is_skill_ready_by_index(2)
                    && self.pet.player.has_enough_mana_by_index(2)
                {
                    if let Some(target_mob_id) = self.find_mob_attack().await {
                        if let Some(zone) =
                            ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id)
                        {
                            if let Ok(mobs) = zone.get_all_mobs().await {
                                if let Some(mut mob) =
                                    mobs.into_iter().find(|m| m.id == target_mob_id)
                                {
                                    let dist =
                                        (self.pet.player.location.x - mob.location.x).abs() as f32;
                                    if dist <= 300.0 {
                                        self.pet.player.player_skill.skill_select = Some(skill);
                                        skill_service::execute_skill(
                                            &mut self.pet.player,
                                            None,
                                            Some(&mut mob),
                                        )
                                        .await;
                                        zone.mob_effects(mob.id, mob.effect_skill.clone());
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Skill::KAIOKEN => {
                if self.pet.player.is_skill_ready_by_index(2)
                    && self.pet.player.has_enough_mana_by_index(2)
                {
                    self.pet.player.player_skill.skill_select = Some(skill);
                    skill_service::execute_skill(&mut self.pet.player, None, None).await;
                    return true;
                }
            }
            _ => {}
        }
        false
    }

    async fn use_skill_4(&mut self) -> bool {
        let Some(skill) = self.get_skill_by_index(3).await else {
            return false;
        };

        match skill.template_id {
            Skill::BIEN_KHI => {
                if !self.pet.player.effect_skill.is_monkey
                    && self.pet.player.is_skill_ready_by_index(3)
                    && self.pet.player.has_enough_mana_by_index(3)
                {
                    self.pet.player.player_skill.skill_select = Some(skill);
                    skill_service::execute_skill(&mut self.pet.player, None, None).await;
                    return true;
                }
            }
            Skill::KHIEN_NANG_LUONG => {
                if !self.pet.player.effect_skill.is_shield
                    && self.pet.player.is_skill_ready_by_index(3)
                    && self.pet.player.has_enough_mana_by_index(3)
                {
                    self.pet.player.player_skill.skill_select = Some(skill);
                    skill_service::execute_skill(&mut self.pet.player, None, None).await;
                    return true;
                }
            }
            Skill::DE_TRUNG => {
                if self.pet.player.is_skill_ready_by_index(3)
                    && self.pet.player.has_enough_mana_by_index(3)
                {
                    self.pet.player.player_skill.skill_select = Some(skill);
                    skill_service::execute_skill(&mut self.pet.player, None, None).await;
                    return true;
                }
            }
            _ => {}
        }
        false
    }

    async fn use_skill_5(&mut self) -> bool {
        let Some(skill) = self.get_skill_by_index(4).await else {
            return false;
        };

        match skill.template_id {
            Skill::TROI => {
                if self.pet.player.is_skill_ready_by_index(4)
                    && self.pet.player.has_enough_mana_by_index(4)
                {
                    if let Some(target_mob_id) = self.find_mob_attack().await {
                        if let Some(zone) =
                            ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id)
                        {
                            if let Ok(mobs) = zone.get_all_mobs().await {
                                if let Some(mut mob) =
                                    mobs.into_iter().find(|m| m.id == target_mob_id)
                                {
                                    let dist =
                                        (self.pet.player.location.x - mob.location.x).abs() as f32;
                                    if dist <= 300.0 {
                                        self.pet.player.player_skill.skill_select = Some(skill);
                                        skill_service::execute_skill(
                                            &mut self.pet.player,
                                            None,
                                            Some(&mut mob),
                                        )
                                        .await;
                                        zone.mob_effects(mob.id, mob.effect_skill.clone());
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Skill::MAKANKOSAPPO => {
                if self.pet.player.is_skill_ready_by_index(4)
                    && self.pet.player.has_enough_mana_by_index(4)
                {
                    if let Some(target_mob_id) = self.find_mob_attack().await {
                        if let Some(zone) =
                            ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id)
                        {
                            if let Ok(mobs) = zone.get_all_mobs().await {
                                if let Some(mut mob) =
                                    mobs.into_iter().find(|m| m.id == target_mob_id)
                                {
                                    let dist =
                                        (self.pet.player.location.x - mob.location.x).abs() as f32;
                                    if dist <= 300.0 {
                                        self.pet.player.player_skill.skill_select = Some(skill);
                                        skill_service::execute_skill(
                                            &mut self.pet.player,
                                            None,
                                            Some(&mut mob),
                                        )
                                        .await;
                                        zone.mob_effects(mob.id, mob.effect_skill.clone());
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Skill::DICH_CHUYEN_TUC_THOI => {
                if self.pet.player.is_skill_ready_by_index(4)
                    && self.pet.player.has_enough_mana_by_index(4)
                {
                    if let Some(target_mob_id) = self.find_mob_attack().await {
                        if let Some(zone) =
                            ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id)
                        {
                            if let Ok(mobs) = zone.get_all_mobs().await {
                                if let Some(mut mob) =
                                    mobs.into_iter().find(|m| m.id == target_mob_id)
                                {
                                    self.pet.player.player_skill.skill_select = Some(skill);
                                    skill_service::execute_skill(
                                        &mut self.pet.player,
                                        None,
                                        Some(&mut mob),
                                    )
                                    .await;
                                    zone.mob_effects(mob.id, mob.effect_skill.clone());
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            _ => {}
        }
        false
    }

    async fn use_skill_6(&mut self) -> bool {
        let Some(skill) = self.get_skill_by_index(5).await else {
            return false;
        };

        match skill.template_id {
            Skill::HUYT_SAO => {
                if self.pet.player.is_skill_ready_by_index(5)
                    && self.pet.player.has_enough_mana_by_index(5)
                {
                    self.pet.player.player_skill.skill_select = Some(skill);
                    skill_service::execute_skill(&mut self.pet.player, None, None).await;
                    return true;
                }
            }
            _ => {}
        }
        false
    }

    async fn use_skill_7(&mut self) -> bool {
        let Some(skill) = self.get_skill_by_index(6).await else {
            return false;
        };

        match skill.template_id {
            Skill::LIEN_HOAN => {
                if self.pet.player.is_skill_ready_by_index(6)
                    && self.pet.player.has_enough_mana_by_index(6)
                {
                    if let Some(target_mob_id) = self.find_mob_attack().await {
                        if let Some(zone) =
                            ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id)
                        {
                            if let Ok(mobs) = zone.get_all_mobs().await {
                                if let Some(mut mob) =
                                    mobs.into_iter().find(|m| m.id == target_mob_id)
                                {
                                    let dist =
                                        (self.pet.player.location.x - mob.location.x).abs() as f32;
                                    if dist <= 150.0 {
                                        self.pet.player.player_skill.skill_select = Some(skill);
                                        skill_service::execute_skill(
                                            &mut self.pet.player,
                                            None,
                                            Some(&mut mob),
                                        )
                                        .await;
                                        zone.mob_effects(mob.id, mob.effect_skill.clone());
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            _ => {}
        }
        false
    }

    async fn ai_attack_mob(&mut self, mob_id: u64) {
        if self.pet.player.is_die() {
            return;
        }

        let zone_opt = ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);
        let Some(zone) = zone_opt else { return };

        let Ok(mobs) = zone.get_all_mobs().await else {
            return;
        };

        if let Some(mut mob) = mobs.into_iter().find(|m| m.id == mob_id) {
            let dist = (self.pet.player.location.x - mob.location.x).abs() as f32;
            let has_multiple_skills = self.pet.player.player_skill.skills.len() > 1;

            let skill_type = PetSkillType::from_dis(dist, has_multiple_skills);
            let skill_index = skill_type.to_index();

            let need_select = match &self.pet.player.player_skill.skill_select {
                Some(current) => self
                    .pet
                    .player
                    .player_skill
                    .skills
                    .get(skill_index)
                    .map_or(false, |target| current.template_id != target.template_id),
                None => true,
            };

            if need_select {
                if let Some(skill) = self.pet.player.player_skill.skills.get(skill_index) {
                    self.pet.player.player_skill.skill_select = Some(skill.clone());
                }
            }

            if !self.pet.player.is_skill_ready() || !self.pet.player.has_enough_mana() {
                if dist > APPROACH_THRESHOLD {
                    self.move_to(mob.location.x + MOVE_OFFSET_FAR, mob.location.y)
                        .await;
                }
                return;
            }
            if skill_type == PetSkillType::Melee && dist > MELEE_RANGE {
                let target_x = if self.pet.player.location.x < mob.location.x {
                    mob.location.x - MOVE_OFFSET_CLOSE
                } else {
                    mob.location.x + MOVE_OFFSET_CLOSE
                };
                self.move_to(target_x, mob.location.y).await;
            } else if dist > RANGED_MAX_DISTANCE {
                self.move_to(mob.location.x + MOVE_OFFSET_FAR, mob.location.y)
                    .await;
            }
            if skill_type.can_attack(dist) {
                if let Some(master_handle) = PLAYER_MANAGER.get(self.pet.master_id) {
                    if let Some(master_snapshot) = master_handle.get_snapshot().await {
                        self.pet.player.charms.td_de_tu = master_snapshot.charms.td_de_tu;
                    }
                }
                // let mut mob_clone = mob.clone();
                skill_service::execute_skill(&mut self.pet.player, None, Some(&mut mob)).await;

                zone.mob_effects(mob.id, mob.effect_skill.clone());
                self.pet.player.n_point.current_stamina_sub(1);
                self.pet_chat(None).await;
            }
        }
    }

    async fn move_to(&mut self, x: i16, y: i16) {
        self.pet.player.location.x = x;
        self.pet.player.location.y = y;

        let zone_opt = ZONE_MANAGER.get_zone(self.pet.player.map_id, self.pet.player.zone_id);

        if let Some(zone) = zone_opt {
            let mut msg = Message::new(-7);
            let _ = msg.write_int(self.pet.player.id as i32);
            let _ = msg.write_short(self.pet.player.location.x);
            let _ = msg.write_short(self.pet.player.location.y);
            let _ = ServiceHandles::send_to_all_in_zone(&zone, msg);
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
