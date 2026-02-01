#![allow(dead_code)]
use crate::combine::model::Combine;
use crate::entities;
use crate::item::inventory::{self, Inventory};

use crate::models::EffectSkill;
use crate::models::IntrinsicPlayer;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::InteractionState;
use crate::player::NPoint;
use crate::player::PlayerSkill;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::utils::Location;
use serde_json::Value;

use std::sync::Arc;
use std::time::SystemTime;
use tokio::sync::RwLock;

#[derive(Debug, Clone)]
pub enum PlayerEvent {
    RemoveShield,
    StopCharge,
    EffectBienKhiFinished,
    EffectMonkeyFinished,
    EffectHuytSaoExpired,
}

#[derive(Clone)]
pub struct Player {
    pub id: u64,
    pub name: String,
    pub gender: i8,
    pub head: i16,
    pub session_id: Option<String>,
    pub session: Option<SessionArc>,

    pub n_point: NPoint,
    pub inventory: Inventory,
    pub player_skill: PlayerSkill,
    pub intrinsic: IntrinsicPlayer,
    pub location: Location,
    pub combine_new: Combine,
    pub effect_skill: EffectSkill,

    pub dead_flag: bool,
    pub is_new_member: bool,
    pub before_dispose: bool,

    pub is_train: bool,
    pub type_train: u8,
    pub time_off: u64,

    pub type_pk: i8,

    pub zone_id: i32,
    pub map_id: i32,
    pub last_time_use_option: u64,
    pub last_time_revived: u64,

    pub just_revived: bool,
    pub is_fight: bool,
    pub is_fight1: bool,
    pub is_try: bool,
    pub is_try1: bool,

    pub is_admin: bool,
    pub admin_key: bool,

    pub interaction_state: InteractionState,

    pub task_id: i32,
    pub is_boss: bool,
    pub notify: Option<String>,
}

impl Player {
    pub fn new(id: u64, name: String, gender: u8) -> Self {
        let current_time = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;

        Player {
            id,
            name,
            gender: 0,
            head: 0,
            session_id: None,
            session: None,
            n_point: NPoint::new(),
            inventory: Inventory::new(),
            player_skill: PlayerSkill::new(),
            intrinsic: IntrinsicPlayer::new(),
            location: Location::new(),
            combine_new: Combine::new(),
            effect_skill: EffectSkill::new(),
            dead_flag: false,
            is_new_member: true,
            before_dispose: false,
            is_train: false,
            type_train: 0,
            time_off: 0,
            type_pk: 0,
            zone_id: 0,
            map_id: 0,
            last_time_use_option: current_time,
            last_time_revived: 0,
            just_revived: false,
            is_fight: false,
            is_fight1: false,
            is_try: false,
            is_try1: false,

            is_admin: false,
            admin_key: false,
            interaction_state: InteractionState::new(),
            task_id: 0,
            is_boss: false,
            notify: None,
        }
    }

    pub fn is_die(&self) -> bool {
        self.dead_flag || self.n_point.hp_current <= 0
    }

    pub fn get_name(&self) -> &str {
        &self.name
    }
    const HEAD_MONKEY: [i16; 7] = [192, 195, 196, 199, 197, 200, 198];
    const BODY_MONKEY: [i16; 7] = [193, 193, 193, 193, 193, 193, 193];
    const LEG_MONKEY: [i16; 7] = [194, 194, 194, 194, 194, 194, 194];

    pub fn get_head(&self) -> i16 {
        if self.effect_skill.is_monkey && self.effect_skill.level_monkey > 0 {
            let idx = (self.effect_skill.level_monkey - 1).clamp(0, 6) as usize;
            return Self::HEAD_MONKEY[idx];
        }
        // Check outfit item
        if let Some(item) = self.inventory.items_body.get(5) {
            if item.is_not_null_item() {
                if let Some(tpl) = &item.template {
                    let head = tpl.head;
                    if head != -1 {
                        return head as i16;
                    }
                }
            }
        }
        self.head
    }
    pub fn get_body(&self) -> i16 {
        if self.effect_skill.is_monkey && self.effect_skill.level_monkey > 0 {
            let idx = (self.effect_skill.level_monkey - 1).clamp(0, 6) as usize;
            return Self::BODY_MONKEY[idx];
        }
        if let Some(item) = self.inventory.items_body.get(5) {
            if item.is_not_null_item() {
                if let Some(tpl) = &item.template {
                    let body = tpl.body;
                    if body != -1 {
                        return body as i16;
                    }
                }
            }
        }
        if self.gender == 1 {
            59
        } else {
            57
        }
    }
    pub fn get_leg(&self) -> i16 {
        if self.effect_skill.is_monkey && self.effect_skill.level_monkey > 0 {
            let idx = (self.effect_skill.level_monkey - 1).clamp(0, 6) as usize;
            return Self::LEG_MONKEY[idx];
        }
        if let Some(item) = self.inventory.items_body.get(5) {
            if item.is_not_null_item() {
                if let Some(tpl) = &item.template {
                    let leg = tpl.leg;
                    if leg != -1 {
                        return leg as i16;
                    }
                }
            }
        }
        if self.gender == 1 {
            60
        } else {
            58
        }
    }
    pub fn send_to_client(&self, msg: Message) -> anyhow::Result<()> {
        if let Some(ref session) = self.session {
            session.transmit(msg);
        }
        Ok(())
    }

    pub fn is_pl(&self) -> bool {
        !self.is_die() && self.session.is_some()
    }

    pub fn injured(&mut self, damage: u64, piercing: bool) -> u64 {
        let mut dame = damage as i32;

        if !piercing && self.effect_skill.is_shield {
            if dame > self.n_point.hp_max {
                crate::services::effect_skill_service::EffectSkillService::break_shield(self);
            }
            dame = 1;
        }

        if !piercing {
            dame -= self.n_point.def;
        }
        if dame < 0 {
            dame = 1;
        }
        self.n_point.set_hp(self.n_point.hp_current - dame);
        if self.n_point.hp_current <= 0 {
            self.set_die();
        }
        dame as u64
    }

    pub fn set_die(&mut self) {
        self.dead_flag = true;
        self.n_point.hp_current = 0;
        let _ = crate::services::services::ServiceHandles::send_player_die(self);
    }

    pub fn revive(&mut self) {
        self.dead_flag = false;
        if self.n_point.hp_current <= 0 {
            self.n_point.hp_current = 1;
        }
        self.just_revived = true;
        self.last_time_revived = SystemTime::now()
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
    }
    pub fn chat(&self, text: &str) {
        println!("[{}]: {}", self.name, text);
    }

    pub fn is_admin(&self) -> bool {
        self.is_admin
    }

    pub fn admin_key(&self) -> bool {
        self.admin_key
    }

    pub fn prepared_to_dispose(&mut self) {
        self.before_dispose = true;
    }

    pub fn dispose(&mut self) {
        self.before_dispose = true;
        self.session_id = None;
        self.session = None; // Clear session reference
        println!("Player {} disposed", self.name);
    }

    pub fn set_fight(&mut self, _type_fight: u8, _type_target: u8) {
        self.is_fight = true;
    }

    pub fn reset_fight(&mut self) {
        self.is_fight = false;
        self.is_fight1 = false;
        self.is_try = false;
        self.is_try1 = false;
    }

    pub fn start_training(&mut self, type_train: u8) {
        self.is_train = true;
        self.type_train = type_train;
        self.time_off = 0;
    }
    pub fn stop_training(&mut self) {
        self.is_train = false;
        self.type_train = 0;
        self.time_off = 0;
    }
    pub fn set_notify(&mut self, notify: String) {
        self.notify = Some(notify);
    }

    pub fn clear_notify(&mut self) {
        self.notify = None;
    }

    pub fn has_tennis_spaceship(&self) -> bool {
        false
    }

    pub fn get_task_id(&self) -> i32 {
        self.task_id
    }

    pub fn set_task_id(&mut self, task_id: i32) {
        self.task_id = task_id;
    }

    pub fn is_boss(&self) -> bool {
        self.is_boss
    }

    pub fn has_previous_capsule_location(&self) -> bool {
        false
    }

    pub fn save_capsule_location(&mut self, map_id: i32, zone_id: i32) {
        println!("Saving capsule location: map {} zone {}", map_id, zone_id);
    }

    pub fn get_previous_capsule_location(&self) -> Option<(i32, i32)> {
        None
    }

    pub fn update_zone_change_time(&mut self) {
        println!("Updated zone change time for player {}", self.name);
    }

    pub fn has_enough_mana(&self) -> bool {
        if let Some(skill) = &self.player_skill.skill_select {
            return self.n_point.mp_current >= skill.mana_use as i32;
        }
        false
    }

    pub fn is_skill_ready(&self) -> bool {
        if let Some(skill) = &self.player_skill.skill_select {
            let now = crate::utils::time::current_time_millis();
            return now > skill.start_time_use + skill.cool_down as u64;
        }
        false
    }

    pub fn update_charging(&mut self) -> Option<ChargeUpdateResult> {
        if !self.effect_skill.is_charging {
            return None;
        }

        if self.effect_skill.count_charging >= 10 {
            return Some(ChargeUpdateResult {
                should_stop: true,
                hp_recovered: 0,
                mp_recovered: 0,
                should_chat: false,
            });
        }

        let skill_point = self
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.point)
            .unwrap_or(1);
        let percent_charge = crate::utils::skill_util::get_percent_charge(skill_point);
        let is_dead = self.is_die();
        let is_full = self.n_point.hp_current >= self.n_point.hp_max
            && self.n_point.mp_current >= self.n_point.mp_max;

        if is_dead || is_full {
            return Some(ChargeUpdateResult {
                should_stop: true,
                hp_recovered: 0,
                mp_recovered: 0,
                should_chat: false,
            });
        }

        let hp_recovered = self.n_point.hp_max * percent_charge / 100;
        let mp_recovered = self.n_point.mp_max * percent_charge / 100;

        self.n_point.hp_current += hp_recovered;
        if self.n_point.hp_current > self.n_point.hp_max {
            self.n_point.hp_current = self.n_point.hp_max;
        }

        // Hồi MP
        self.n_point.mp_current += mp_recovered;
        if self.n_point.mp_current > self.n_point.mp_max {
            self.n_point.mp_current = self.n_point.mp_max;
        }

        let should_chat = self.effect_skill.count_charging % 3 == 0;

        self.effect_skill.count_charging += 1;

        let should_stop = self.effect_skill.count_charging >= 10;

        Some(ChargeUpdateResult {
            should_stop,
            hp_recovered,
            mp_recovered,
            should_chat,
        })
    }

    pub fn update(&mut self) -> Vec<PlayerEvent> {
        let now = crate::utils::time::current_time_millis();
        let mut events = Vec::new();

        if self.effect_skill.is_shield
            && now > self.effect_skill.shield_start_time + self.effect_skill.shield_duration_ms
        {
            self.effect_skill.is_shield = false;
            events.push(PlayerEvent::RemoveShield);
        }

        if let Some(charge_result) = self.update_charging() {
            if charge_result.should_stop {
                self.effect_skill.is_charging = false;
                self.effect_skill.count_charging = 0;
                events.push(PlayerEvent::StopCharge);
            }
        }
        self.n_point.set_base_point();
        if self.n_point.hp_current <= 0 && !self.dead_flag {
            self.dead_flag = true;
        }

        let effect_result = self.effect_skill.update(now);

        if effect_result.bienkhi_finished {
            events.push(PlayerEvent::EffectBienKhiFinished);
        }
        if effect_result.monkey_down {
            events.push(PlayerEvent::EffectMonkeyFinished);
        }
        if effect_result.huyt_sao_expired {
            events.push(PlayerEvent::EffectHuytSaoExpired);
        }

        events
    }
}

#[derive(Debug, Clone)]
pub struct ChargeUpdateResult {
    pub should_stop: bool,
    pub hp_recovered: i32,
    pub mp_recovered: i32,
    pub should_chat: bool,
}
