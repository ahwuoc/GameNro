use crate::map::Zone;
use crate::models::{effect_skill, EffectSkill};
use crate::network::message::Message;
use crate::player::player::Player;
use crate::services::ServiceHandles;
use crate::utils::skill_util;
use crate::{mob::mob::RtMob, utils::time};
use std::time::{SystemTime, UNIX_EPOCH};

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
}

pub struct EffectSkillService;

impl EffectSkillService {
    pub const SHIELD_EFFECT: u8 = 33;
    pub const BLIND_EFFECT: u8 = 40;
    pub const SLEEP_EFFECT: u8 = 41;

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

    pub fn apply_blind_dctt(effect: &mut EffectSkill, duration_ms: u64) {
        let now = time::current_time_millis();
        effect.is_blind_dctt = true;
        effect.time_blind_dctt = duration_ms;
        effect.start_time_dctt = now;
    }

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
            let _ = msg.write_short(player.player_skill.skill_select.as_ref().unwrap().skill_id);
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

    pub fn send_effect_player(
        player_use: &Player,
        player_target: &Player,
        action: EffectAction,
        effect: u8,
    ) {
        let mut msg = Message::new(-124);
        let _ = msg.write_byte(action as i8); // 0: huy, 1: bat dau
        let _ = msg.write_byte(0); // 0: player, 1: mob

        match action {
            EffectAction::UPDATE => {
                let _ = msg.write_int(player_target.id as i32);
            }
            _ => {
                let _ = msg.write_byte(effect as i8);
                let _ = msg.write_int(player_target.id as i32);
                let _ = msg.write_int(player_use.id as i32);
            }
        }
        ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }

    pub fn send_effect_mob(
        player_use: &Player,
        mob_target: &RtMob,
        action: EffectAction,
        effect: u8,
    ) {
        let mut msg = Message::new(-124);
        msg.write_byte(action as i8).ok();
        msg.write_byte(1).ok(); // 1 = mob

        match action {
            EffectAction::UPDATE => {
                msg.write_byte(mob_target.id as i8).ok();
            }
            _ => {
                msg.write_byte(effect as i8).ok();
                msg.write_byte(mob_target.id as i8).ok();
                msg.write_int(player_use.id as i32).ok();
            }
        }

        let _ = ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }

    pub fn send_remove_effect_mob_in_zone(zone: &Zone, mob: &RtMob, effect: u8) {
        let mut msg = Message::new(-124);
        msg.write_byte(EffectAction::REMOVE as i8).ok();
        msg.write_byte(1).ok(); // 1 = mob
        msg.write_byte(effect as i8).ok();
        msg.write_byte(mob.id as i8).ok();
        msg.write_int(-1).ok(); // no player reference
        let _ = zone.send_message_to_all_players(msg);
    }

    // ========== TAI TAO NANG LUONG (Charging) ==========

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
        let _ = msg.write_byte(3); // effect type: stop charge
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(-1);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }

    // ========== BIEN KHI (Monkey) ==========

    pub fn start_use_skill_monkey(player: &mut Player) {
        println!(
            "[DEBUG BIENKHI] start_use_skill_monkey called for player {}",
            player.name
        );
        Self::send_effect_monkey(player);
        if player.is_boss {
            let _ = Self::set_is_monkey_state(player);
            return;
        }
        ServiceHandles::send_speed_to_client(player, 0);
        let now = time::current_time_millis();
        player.effect_skill.is_skill_bienkhi = true;
        player.effect_skill.time_duration_bienkhi = 1500;
        player.effect_skill.time_start_bienkhi = now;
        println!(
            "[DEBUG BIENKHI] Animation started at {}, will finish after 1500ms",
            now
        );
    }

    pub fn finish_use_monkey_state(player: &mut Player) -> Option<MonkeyStateUpdate> {
        println!(
            "[DEBUG BIENKHI] finish_use_monkey_state for player {}",
            player.name
        );
        player.effect_skill.is_skill_bienkhi = false;

        if !player.is_die() {
            let result = Self::set_is_monkey_state(player);
            Some(result)
        } else {
            println!("[DEBUG BIENKHI] Player is dead, skipping transformation");
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

        println!(
            "[DEBUG BIENKHI] set_is_monkey: skill_point={}, time_monkey={}ms",
            skill_point, time_monkey
        );

        player.effect_skill.is_monkey = true;
        player.effect_skill.time_monkey = time_monkey;
        player.effect_skill.last_time_up_monkey = now;
        player.effect_skill.level_monkey = skill_point;

        // HP x2
        let old_hp = player.n_point.hp_current;
        player.n_point.hp_current *= 2;
        if player.n_point.hp_current > player.n_point.hp_max * 2 {
            player.n_point.hp_current = player.n_point.hp_max * 2;
        }
        println!(
            "[DEBUG BIENKHI] HP doubled: {} -> {}",
            old_hp, player.n_point.hp_current
        );

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
        }
    }

    /// Phase 1: Update monkey_down state only
    pub fn monkey_down_state(player: &mut Player) -> MonkeyStateUpdate {
        println!(
            "[DEBUG BIENKHI] monkey_down_state for player {}",
            player.name
        );
        player.effect_skill.is_monkey = false;
        player.effect_skill.level_monkey = 0;
        if player.n_point.hp_current > player.n_point.hp_max {
            player.n_point.hp_current = player.n_point.hp_max;
        }

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
        }
    }

    /// Phase 2: Send messages (call after releasing player lock)
    pub fn send_monkey_messages(update: &MonkeyStateUpdate) {
        println!(
            "[DEBUG BIENKHI] send_monkey_messages is_monkey={}",
            update.is_monkey
        );

        // Send effect monkey (-45 cmd 6)
        let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(update.map_id, update.zone_id) {
            let mut msg = Message::new(-45);
            let _ = msg.write_byte(6); // effect type: monkey
            let _ = msg.write_int(update.player_id as i32);
            let _ = msg.write_short(update.skill_id);
            let _ = zone.send_message_to_all_players(msg);

            // Send cai trang (-90)
            let mut msg2 = Message::new(-90);
            let _ = msg2.write_byte(1);
            let _ = msg2.write_int(update.player_id as i32);
            let _ = msg2.write_short(update.head);
            let _ = msg2.write_short(update.body);
            let _ = msg2.write_short(update.leg);
            let _ = msg2.write_byte(if update.is_monkey { 1 } else { 0 });
            let _ = zone.send_message_to_all_players(msg2);

            // Send speed
            let mut msg3 = crate::services::player_info_service::sub_command_i30(8).unwrap();
            let _ = msg3.write_int(update.player_id as i32);
            let _ = msg3.write_byte(update.speed);
            let _ = zone.send_message_to_all_players(msg3);

            // Send info player eat pea (-30 subcommand 14) - to update HP display for others
            if let Ok(mut msg4) = crate::services::player_info_service::sub_command_i30(14) {
                let _ = msg4.write_int(update.player_id as i32);
                let _ = msg4.write_int(update.hp_current);
                let _ = msg4.write_byte(1);
                let _ = msg4.write_int(update.hp_max);
                let _ = zone.send_message_to_all_players(msg4);
            }
        }
        println!("[DEBUG BIENKHI] send_monkey_messages COMPLETE!");
    }

    pub fn send_effect_monkey(player: &Player) {
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let mut msg = Message::new(-45);
        let _ = msg.write_byte(6); // effect type: monkey
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(skill_id);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }

    pub fn send_effect_end_charge(player: &Player) {
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let mut msg = Message::new(-45);
        let _ = msg.write_byte(5); // effect type: end charge
        let _ = msg.write_int(player.id as i32);
        let _ = msg.write_short(skill_id);
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
