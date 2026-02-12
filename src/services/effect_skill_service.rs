use crate::map::Zone;
use crate::models::EffectSkill;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::services::ServiceHandles;
use crate::utils::skill_util;
use crate::{mob::mob::RtMob, utils::time};
use tracing::debug;

pub struct MonkeyStateUpdate {
    pub player_id: u64,
    pub map_id: i32,
    pub zone_id: i32,
    pub skill_id: i16,
    pub is_monkey: bool,
    pub head: i16,
    pub body: i16,
    pub leg: i16,
    pub speed: i8,
    pub hp_current: i32,
    pub hp_max: i32,
    pub time_monkey: u64,
}

pub struct EffectSkillService;

impl EffectSkillService {
    pub const SHIELD_EFFECT: u8 = 33;
    pub const BLIND_EFFECT: u8 = 40;
    pub const SLEEP_EFFECT: u8 = 41;
    pub const HUYT_SAO_EFFECT: u8 = 39;
    pub const HOLD_EFFECT: u8 = 32;

    // ========== SHIELD (Khiêng năng lượng) ==========
    pub fn start_shield(player: &mut Player) {
        let now = time::current_time_millis();
        let Some(skill) = player.player_skill.skill_select.as_ref() else {
            return;
        };
        player.effect_skill.is_shield = true;
        player.effect_skill.shield_start_time = now;
        player.effect_skill.shield_duration_ms = skill_util::get_time_shield(skill.point);
    }

    pub fn remove_shield(player: &mut Player) {
        player.effect_skill.is_shield = false;
        Self::send_effect_player(player, player, EffectAction::REMOVE, Self::SHIELD_EFFECT);
    }

    pub fn break_shield(player: &mut Player) {
        Self::remove_shield(player);
        ServiceHandles::send_item_time(player, 3784, 0);
    }

    // ========== BLIND (Thái dương hạ san / DCTT) ==========
    pub fn apply_blind_dctt(effect: &mut EffectSkill, duration_ms: u64) {
        let now = time::current_time_millis();
        effect.is_blind_dctt = true;
        effect.time_blind_dctt = duration_ms;
        effect.start_time_dctt = now;
    }

    // ========== SLEEP (Thôi miên) ==========
    pub fn set_thoi_mien(player: &mut Player, time_sleep: u64) {
        let current_time = time::current_time_millis();
        player.effect_skill.is_thoi_mien = true;
        player.effect_skill.time_thoi_mien = time_sleep;
        player.effect_skill.start_time_thoi_mien = current_time;
    }

    pub fn set_thoi_mien_mob(mob: &mut RtMob, time_sleep: u64) {
        let current_time = time::current_time_millis();
        mob.effect_skill.is_thoi_mien = true;
        mob.effect_skill.time_thoi_mien = time_sleep;
        mob.effect_skill.start_time_thoi_mien = current_time;
    }

    // ========== TROI (Hold/Bind) ==========
    pub fn set_use_troi(player: &mut Player, time_hold: u64) {
        let current_time = time::current_time_millis();
        player.effect_skill.use_troi = true;
        player.effect_skill.time_troi = time_hold;
        player.effect_skill.start_time_troi = current_time;
    }

    pub fn set_an_troi(target: &mut Player, caster_id: u64, time_hold: u64) {
        let current_time = time::current_time_millis();
        target.effect_skill.an_troi = true;
        target.effect_skill.time_an_troi = time_hold;
        target.effect_skill.start_time_an_troi = current_time;
        target.effect_skill.pl_troi_id = Some(caster_id);
    }

    pub fn remove_use_troi(player: &mut Player) {
        player.effect_skill.use_troi = false;
        player.effect_skill.time_troi = 0;
        player.effect_skill.start_time_troi = 0;
        player.effect_skill.mob_an_troi_id = None;
        player.effect_skill.pl_an_troi_id = None;
    }

    pub fn remove_an_troi(target: &mut Player) {
        target.effect_skill.an_troi = false;
        target.effect_skill.time_an_troi = 0;
        target.effect_skill.start_time_an_troi = 0;
        target.effect_skill.pl_troi_id = None;
    }

    pub fn set_troi_mob(mob: &mut RtMob, player_id: u64, time_hold: u64) {
        let current_time = time::current_time_millis();
        mob.effect_skill.an_troi = true;
        mob.effect_skill.time_an_troi = time_hold;
        mob.effect_skill.start_time_an_troi = current_time;
        mob.effect_skill.pl_troi_id = Some(player_id);
    }

    pub fn remove_troi_mob(mob: &mut RtMob) {
        mob.effect_skill.an_troi = false;
        mob.effect_skill.time_an_troi = 0;
        mob.effect_skill.start_time_an_troi = 0;
        mob.effect_skill.pl_troi_id = None;
    }

    pub fn send_remove_troi_mob(player_use: &Player, mob_target: &RtMob) {
        let msg = Self::build_effect_message(
            player_use.id,
            mob_target.id,
            true,
            EffectAction::REMOVE,
            Self::HOLD_EFFECT,
        );
        let _ = ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }

    pub fn send_effect_use_skill(player: &Player, skill_id: i16) {
        let mut msg = Message::new(-45);
        if let Ok(_) = msg.write_byte(8) {
            let _ = msg.write_int(player.id as i32);
            let _ = msg.write_short(skill_id);
            let _ = ServiceHandles::send_mess_another_not_me_in_map(player, msg);
        }
    }

    pub fn send_effect_blind_thai_duong_ha_san(
        player: &Player,
        list_players: Vec<u32>,
        list_mobs: Vec<u8>,
        time_stun: i32,
    ) {
        let mut msg = Message::new(-45);
        if let Ok(_) = msg.write_byte(0) {
            let _ = msg.write_int(player.id as i32);
            let skill_id = player
                .player_skill
                .skill_select
                .as_ref()
                .map(|s| s.skill_id)
                .unwrap_or(0);
            let _ = msg.write_short(skill_id);
            let _ = msg.write_byte(list_mobs.len() as i8);
            for mob_id in list_mobs {
                let _ = msg.write_byte(mob_id as i8);
                let _ = msg.write_byte((time_stun / 1000) as i8);
            }
            let _ = msg.write_byte(list_players.len() as i8);
            for pl_id in list_players {
                let _ = msg.write_int(pl_id as i32);
                let _ = msg.write_byte((time_stun / 1000) as i8);
            }
            let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
        }
    }

    pub fn start_stun(player: &mut Player, time_stun: u64) {
        let current_time = time::current_time_millis();
        player.effect_skill.is_stun = true;
        player.effect_skill.time_stun = time_stun;
        player.effect_skill.last_time_stun = current_time;

        Self::send_effect_player(player, player, EffectAction::START, Self::BLIND_EFFECT);
    }

    pub fn start_stun_mob(mob: &mut RtMob, time_stun: u64) {
        let current_time = time::current_time_millis();
        mob.effect_skill.is_stun = true;
        mob.effect_skill.time_stun = time_stun;
        mob.effect_skill.last_time_stun = current_time;
    }

    // ========== MESSAGE BUILDING & SENDING (Gửi nhận tin nhắn) ==========
    pub fn build_effect_message(
        player_use_id: u64,
        target_id: u64,
        is_mob: bool,
        action: EffectAction,
        effect: u8,
    ) -> Message {
        let mut msg = Message::new(-124);
        let _ = msg.write_byte(action as i8);
        let _ = msg.write_byte(if is_mob { 1 } else { 0 });

        match action {
            EffectAction::UPDATE => {
                if is_mob {
                    let _ = msg.write_byte(target_id as i8);
                } else {
                    let _ = msg.write_int(target_id as i32);
                }
            }
            _ => {
                let _ = msg.write_byte(effect as i8);
                if is_mob {
                    let _ = msg.write_byte(target_id as i8);
                } else {
                    let _ = msg.write_int(target_id as i32);
                }
                let _ = msg.write_int(player_use_id as i32);
            }
        }
        msg
    }

    pub fn send_effect_player(
        player_use: &Player,
        player_target: &Player,
        action: EffectAction,
        effect: u8,
    ) {
        let msg =
            Self::build_effect_message(player_use.id, player_target.id, false, action, effect);
        ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }

    pub fn send_effect_mob(
        player_use: &Player,
        mob_target: &RtMob,
        action: EffectAction,
        effect: u8,
    ) {
        let msg = Self::build_effect_message(player_use.id, mob_target.id, true, action, effect);
        ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }

    // ========== CHARGING (Tái tạo năng lượng) ==========
    pub fn start_charge(player: &mut Player) {
        if !player.effect_skill.is_charging {
            player.effect_skill.is_charging = true;
            Self::send_effect_charge(player);
        }
    }

    pub fn stop_charge(player: &mut Player) {
        player.effect_skill.count_charging = 0;
        player.effect_skill.is_charging = false;
        Self::send_effect_stop_charge(player);
    }

    pub fn send_effect_charge(player: &Player) {
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let mut msg = Message::new(-45);
        let _ = msg.write_byte(1); // effect type: charging
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(skill_id);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }

    pub fn send_effect_stop_charge(player: &Player) {
        let mut msg = Message::new(-45);
        let _ = msg.write_byte(3);
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(-1);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }

    // ========== BIEN KHI (Monkey) ==========

    pub fn start_use_skill_monkey(player: &mut Player) {
        debug!("start_use_skill_monkey called for player {}", player.name);

        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        Self::send_effect_monkey_by_id(player, skill_id);

        if player.is_boss {
            let _ = Self::set_is_monkey_state(player);
            return;
        }
        ServiceHandles::send_speed_to_client(player, 0);
        let now = time::current_time_millis();
        player.effect_skill.is_skill_bienkhi = true;
        player.effect_skill.time_duration_bienkhi = 1500;
        player.effect_skill.time_start_bienkhi = now;
        debug!("Animation started at {}, will finish after 1500ms", now);
    }

    pub fn send_effect_monkey_by_id(player: &Player, skill_id: i16) {
        let mut msg = Message::new(-45);
        let _ = msg.write_byte(6);
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(skill_id);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }

    pub fn finish_use_monkey_state(player: &mut Player) -> Option<MonkeyStateUpdate> {
        debug!("finish_use_monkey_state for player {}", player.name);
        player.effect_skill.is_skill_bienkhi = false;

        if !player.is_die() {
            let result = Self::set_is_monkey_state(player);
            Some(result)
        } else {
            debug!("Player is dead, skipping transformation");
            None
        }
    }
    pub fn set_is_monkey_state(player: &mut Player) -> MonkeyStateUpdate {
        let skill_point = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.point)
            .unwrap_or(1);
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let time_monkey = skill_util::get_time_monkey(skill_point);
        let now = time::current_time_millis();

        debug!(
            "set_is_monkey: skill_point={}, time_monkey={}ms",
            skill_point, time_monkey
        );

        player.effect_skill.is_monkey = true;
        player.effect_skill.time_monkey = time_monkey;
        player.effect_skill.last_time_up_monkey = now;
        player.effect_skill.level_monkey = skill_point;
        player.n_point.is_monkey_active = true;

        // HP x2
        let old_hp = player.n_point.hp_current;
        player.n_point.hp_current *= 2;
        if player.n_point.hp_current > player.n_point.hp_max * 2 {
            player.n_point.hp_current = player.n_point.hp_max * 2;
        }
        debug!("HP doubled: {} -> {}", old_hp, player.n_point.hp_current);

        MonkeyStateUpdate {
            player_id: player.id,
            map_id: player.map_id,
            zone_id: player.zone_id,
            skill_id,
            is_monkey: true,
            head: player.get_head(),
            body: player.get_body(),
            leg: player.get_leg(),
            speed: player.n_point.speed,
            hp_current: player.n_point.hp_current,
            hp_max: player.n_point.hp_max * 2,
            time_monkey,
        }
    }

    pub fn monkey_down_state(player: &mut Player) -> MonkeyStateUpdate {
        debug!("monkey_down_state for player {}", player.name);
        player.effect_skill.is_monkey = false;
        player.effect_skill.level_monkey = 0;
        player.effect_skill.is_skill_bienkhi = false;
        player.n_point.is_monkey_active = false;
        player.n_point.current_hp_add(0);

        MonkeyStateUpdate {
            player_id: player.id,
            map_id: player.map_id,
            zone_id: player.zone_id,
            skill_id: player
                .player_skill
                .skill_select
                .as_ref()
                .map(|s| s.skill_id)
                .unwrap_or(0),
            is_monkey: false,
            head: player.get_head(),
            body: player.get_body(),
            leg: player.get_leg(),
            speed: player.n_point.speed,
            hp_current: player.n_point.hp_current,
            hp_max: player.n_point.hp_max,
            time_monkey: 0,
        }
    }

    pub fn send_monkey_messages(update: &MonkeyStateUpdate) {
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(update.map_id, update.zone_id) {
            // Hiệu ứng biến khỉ (-45, 6)
            let mut msg = Message::new(-45);
            let _ = msg.write_byte(6);
            let _ = msg.write_int(update.player_id as i32);
            let _ = msg.write_short(update.skill_id);
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);

            // Thay đổi ngoại hình (-90)
            let mut msg2 = Message::new(-90);
            let _ = msg2.write_byte(1);
            let _ = msg2.write_int(update.player_id as i32);
            let _ = msg2.write_short(update.head);
            let _ = msg2.write_short(update.body);
            let _ = msg2.write_short(update.leg);
            let _ = msg2.write_byte(if update.is_monkey { 1 } else { 0 });
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg2);

            let mut msg3 = crate::services::player_info_service::sub_command_i30(8).unwrap();
            let _ = msg3.write_int(update.player_id as i32);
            let _ = msg3.write_byte(update.speed);
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg3);

            // Cập nhật HP (-30, 14)
            if let Ok(mut msg4) = crate::services::player_info_service::sub_command_i30(14) {
                let _ = msg4.write_int(update.player_id as i32);
                let _ = msg4.write_int(update.hp_current);
                let _ = msg4.write_byte(1);
                let _ = msg4.write_int(update.hp_max);
                let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg4);
            }
        }
    }

    pub fn send_effect_end_charge(player: &Player) {
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let mut msg = Message::new(-45);
        let _ = msg.write_byte(5);
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(skill_id);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }

    pub fn send_not_monkey(player: &Player) {
        let mut msg = Message::new(-90);
        let _ = msg.write_byte(-1);
        let _ = msg.write_int(player.id as i32);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }
}

#[repr(i8)]
#[derive(Debug, Clone, Copy)]
pub enum EffectAction {
    START = 1,
    REMOVE = 0,
    UPDATE = 2,
}
