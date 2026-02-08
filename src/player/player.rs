#![allow(dead_code)]
use crate::combine::model::Combine;
use crate::entities;
use crate::item::inventory::{self, Inventory};

use crate::features::task_player::TaskPlayer;
use crate::models::radar;
use crate::models::EffectSkill;
use crate::models::IntrinsicPlayer;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player_actor::TypePk;
use crate::player::player_data::PetData;
use crate::player::InteractionState;
use crate::player::MagicTree;
use crate::player::NPoint;
use crate::player::PlayerSkill;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::templates::pet_template_manager;
use crate::templates::power_manager;
use crate::utils::{skill_util, time, Location};
use serde_json::Value;

use std::sync::Arc;
use std::time::SystemTime;
use tokio::sync::RwLock;

#[derive(Clone, Debug)]
pub struct Player {
    pub id: u64,
    pub clan_id: i32,
    pub name: String,
    pub gender: i8,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub session_id: Option<String>,
    pub session: Option<SessionArc>,

    pub n_point: NPoint,
    pub inventory: Inventory,
    pub player_skill: PlayerSkill,
    pub intrinsic: IntrinsicPlayer,
    pub location: Location,
    pub combine_new: Combine,
    pub effect_skill: EffectSkill,
    pub pet_id: Option<u64>,
    pub dead_flag: bool,
    pub is_new_member: bool,
    pub before_dispose: bool,

    pub is_train: bool,
    pub type_train: u8,
    pub time_off: u64,

    pub type_pk: TypePk,

    pub zone_id: i32,
    pub map_id: i32,
    pub last_time_use_option: u64,
    pub last_time_revived: u64,
    pub last_time_eat_pea: u64,

    pub just_revived: bool,
    pub is_fight: bool,
    pub is_fight1: bool,
    pub is_try: bool,
    pub is_try1: bool,

    pub is_admin: bool,
    pub admin_key: bool,

    pub interaction_state: InteractionState,

    pub task_player: TaskPlayer,
    pub is_boss: bool,
    pub is_pet: bool,
    pub type_pet: i8,
    pub is_transform: bool,
    pub fusion: crate::player::components::fusion::Fusion,
    pub notify: Option<String>,
    pub stats_need_update: bool,
    pub pet_data: Option<PetData>,
    pub boss_component: Option<crate::player::components::boss::BossComponent>,
    pub magic_tree: MagicTree,
    pub radar_cards: Vec<radar::Card>,
    pub map_id_before_capsule: i32,
    pub zone_id_before_capsule: i32,
    pub spaceship_id: i8,
}

impl Player {
    pub fn new(id: u64, name: String, gender: u8) -> Self {
        let current_time = time::current_time_millis();
        Player {
            id,
            clan_id: -1,
            name,
            gender: 0,
            head: 0,
            body: -1,
            leg: -1,
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
            pet_id: None,
            is_new_member: true,
            before_dispose: false,
            is_train: false,
            type_train: 0,
            time_off: 0,
            type_pk: TypePk::PkNon,
            zone_id: 0,
            map_id: 0,
            last_time_use_option: current_time,
            last_time_revived: 0,
            last_time_eat_pea: 0,
            just_revived: false,
            is_fight: false,
            is_fight1: false,
            is_try: false,
            is_try1: false,

            is_admin: false,
            admin_key: false,
            interaction_state: InteractionState::new(),
            task_player: TaskPlayer::new(),
            is_boss: false,
            is_pet: false,
            type_pet: 0,
            is_transform: false,
            fusion: crate::player::components::fusion::Fusion::new(),
            notify: None,
            stats_need_update: true,
            pet_data: None,
            boss_component: None,
            magic_tree: MagicTree::new(),
            radar_cards: Vec::new(),
            map_id_before_capsule: -1,
            zone_id_before_capsule: -1,
            spaceship_id: 1, // Default spaceship
        }
    }

    pub fn is_die(&self) -> bool {
        self.dead_flag || self.n_point.hp_current <= 0
    }

    pub fn get_name(&self) -> &str {
        &self.name
    }

    pub fn get_caption(&self) -> String {
        let caption = power_manager::get_caption(self.n_point.power);
        let planet_name = match self.gender {
            0 => "Trái Đất",
            1 => "Namếc",
            2 => "Xayda",
            _ => "",
        };
        caption.replace("{planet}", planet_name)
    }
    const HEAD_MONKEY: [i16; 7] = [192, 195, 196, 199, 197, 200, 198];
    const BODY_MONKEY: [i16; 7] = [193, 193, 193, 193, 193, 193, 193];
    const LEG_MONKEY: [i16; 7] = [194, 194, 194, 194, 194, 194, 194];

    pub fn get_head(&self) -> i16 {
        if self.effect_skill.is_monkey && self.effect_skill.level_monkey > 0 {
            let idx = (self.effect_skill.level_monkey - 1).clamp(0, 6) as usize;
            return Self::HEAD_MONKEY[idx];
        }

        // ========Handle Fusion=========
        if self.fusion.type_fusion != 0 {
            if let Some(template) =
                crate::templates::fusion_template_manager::get(self.fusion.template_id)
            {
                let data = match self.gender {
                    1 => template.data_1.as_ref(), // Namek
                    2 => template.data_2.as_ref(), // Xayda
                    _ => template.data_0.as_ref(), // Trai Dat
                };
                if let Some(d) = data {
                    if d.head != -1 {
                        return d.head;
                    }
                }
            }
        }

        // ========Handle Pet=========
        if self.is_pet {
            if let Some(template) = pet_template_manager::get(self.type_pet as i32) {
                if self.is_transform {
                    if template.head_transform != -1 {
                        return template.head_transform;
                    }
                } else {
                    if template.head != -1 {
                        return template.head;
                    }
                }
            }
        }

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

        if self.is_pet {
            if self.n_point.power < 1500000 {
                return match self.gender {
                    0 => 285,
                    1 => 288,
                    _ => 282,
                };
            } else {
                return match self.gender {
                    0 => 304,
                    1 => 305,
                    _ => 303,
                };
            }
        }

        self.head
    }
    pub fn get_body(&self) -> i16 {
        if self.effect_skill.is_monkey && self.effect_skill.level_monkey > 0 {
            let idx = (self.effect_skill.level_monkey - 1).clamp(0, 6) as usize;
            return Self::BODY_MONKEY[idx];
        }

        // ========Handle Fusion=========
        if self.fusion.type_fusion != 0 {
            if let Some(template) =
                crate::templates::fusion_template_manager::get(self.fusion.template_id)
            {
                let data = match self.gender {
                    1 => template.data_1.as_ref(), // Namek
                    2 => template.data_2.as_ref(), // Xayda
                    _ => template.data_0.as_ref(), // Trai Dat
                };
                if let Some(d) = data {
                    if d.body != -1 {
                        return d.body;
                    }
                }
            }
        }

        // ========Handle Pet=========
        if self.is_pet {
            if let Some(template) = pet_template_manager::get(self.type_pet as i32) {
                if self.is_transform {
                    if template.body_transform != -1 {
                        return template.body_transform;
                    }
                } else {
                    if template.body != -1 {
                        return template.body;
                    }
                }
            }
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

        if self.is_pet {
            if let Some(item) = self.inventory.items_body.get(0) {
                if item.is_not_null_item() {
                    if let Some(tpl) = &item.template {
                        return tpl.part as i16;
                    }
                }
            }

            if self.n_point.power < 1500000 {
                return match self.gender {
                    0 => 286,
                    1 => 289,
                    _ => 283,
                };
            } else {
                return if self.gender == 1 { 59 } else { 57 };
            }
        }

        if self.body != -1 {
            return self.body;
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

        // ========Handle Fusion=========
        if self.fusion.type_fusion != 0 {
            if let Some(template) =
                crate::templates::fusion_template_manager::get(self.fusion.template_id)
            {
                let data = match self.gender {
                    1 => template.data_1.as_ref(), // Namek
                    2 => template.data_2.as_ref(), // Xayda
                    _ => template.data_0.as_ref(), // Trai Dat
                };
                if let Some(d) = data {
                    if d.leg != -1 {
                        return d.leg;
                    }
                }
            }
        }

        // ========Handle Pet=========
        if self.is_pet {
            if let Some(template) = pet_template_manager::get(self.type_pet as i32) {
                if self.is_transform {
                    if template.leg_transform != -1 {
                        return template.leg_transform;
                    }
                } else {
                    if template.leg != -1 {
                        return template.leg;
                    }
                }
            }
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

        if self.is_pet {
            // Check đồ đang mặc (quần)
            if let Some(item) = self.inventory.items_body.get(1) {
                if item.is_not_null_item() {
                    if let Some(tpl) = &item.template {
                        return tpl.part as i16;
                    }
                }
            }
            if self.n_point.power < 1500000 {
                return match self.gender {
                    0 => 287,
                    1 => 290,
                    _ => 284,
                };
            } else {
                return if self.gender == 1 { 60 } else { 58 };
            }
        }

        if self.leg != -1 {
            return self.leg;
        }

        if self.gender == 1 {
            60
        } else {
            58
        }
    }

    pub fn get_aura(&self) -> i16 {
        0
    }

    pub fn get_eff_front(&self) -> u8 {
        0
    }

    pub fn get_hat(&self) -> i16 {
        -1
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
        self.n_point.current_hp_sub(dame);
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
        self.last_time_revived = time::current_time_millis();
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
        (self.task_player.task_main.id << 10) + (self.task_player.task_main.index << 1)
    }

    pub fn set_task_id(&mut self, task_id: i32) {
        self.task_player.task_main.id = task_id >> 10;
        self.task_player.task_main.index = (task_id & 1023) >> 1;
    }

    pub fn is_boss(&self) -> bool {
        self.is_boss
    }

    pub fn has_previous_capsule_location(&self) -> bool {
        self.map_id_before_capsule != -1
    }

    pub fn save_capsule_location(&mut self, map_id: i32, zone_id: i32) {
        self.map_id_before_capsule = map_id;
        self.zone_id_before_capsule = zone_id;
    }

    pub fn get_previous_capsule_location(&self) -> Option<(i32, i32)> {
        if self.has_previous_capsule_location() {
            Some((self.map_id_before_capsule, self.zone_id_before_capsule))
        } else {
            None
        }
    }

    pub fn update_zone_change_time(&mut self) {
        println!("Updated zone change time for player {}", self.name);
    }

    pub fn has_enough_mana(&self) -> bool {
        if self.is_boss {
            return true;
        }
        if let Some(skill) = &self.player_skill.skill_select {
            return self.n_point.has_mp(skill.mana_use as i32);
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
        if self.effect_skill.count_charging >= 20 {
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
        let percent_charge = skill_util::get_percent_charge(skill_point);
        let is_dead = self.is_die();
        let is_full = self.n_point.is_full_hp_mp();

        if is_dead || is_full {
            return Some(ChargeUpdateResult {
                should_stop: true,
                hp_recovered: 0,
                mp_recovered: 0,
                should_chat: false,
            });
        }
        let hp_recovered = (self.n_point.hp_max / 100) * percent_charge;
        let mp_recovered = (self.n_point.mp_max / 100) * percent_charge;

        self.n_point.current_hp_add(hp_recovered);
        self.n_point.current_mp_add(mp_recovered);

        let should_chat = self.effect_skill.count_charging % 3 == 0;
        self.effect_skill.count_charging += 1;
        let should_stop = self.effect_skill.count_charging >= 20;

        Some(ChargeUpdateResult {
            should_stop,
            hp_recovered,
            mp_recovered,
            should_chat,
        })
    }
}

#[derive(Debug, Clone)]
pub struct ChargeUpdateResult {
    pub should_stop: bool,
    pub hp_recovered: i32,
    pub mp_recovered: i32,
    pub should_chat: bool,
}
