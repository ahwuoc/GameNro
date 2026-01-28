use crate::network::message::Message;
use crate::player::player::Player;
use crate::services::ServiceHandles;
use crate::utils::skill_util;
use crate::{mob::mob::RtMob, utils::time};
use std::time::{SystemTime, UNIX_EPOCH};

pub struct EffectSkillService;

impl EffectSkillService {
    pub const TURN_ON_EFFECT: u8 = 1;
    pub const TURN_OFF_EFFECT: u8 = 0;
    pub const SHIELD_EFFECT: u8 = 33;
    pub const BLIND_EFFECT: u8 = 40;
    pub const SLEEP_EFFECT: u8 = 41;

    pub fn set_start_shield(player: &mut Player) {
        let current_time = time::current_time_millis();
        let Some(skill) = player.player_skill.skill_select.as_ref() else {
            return;
        };
        player.effect_skill.is_shielding = true;
        player.effect_skill.last_time_shield_up = current_time;
        player.effect_skill.time_shield = skill_util::get_time_shield(skill.point);
    }

    pub fn remove_shield(player: &mut Player) {
        player.effect_skill.is_shielding = false;
        Self::send_effect_player(player, player, Self::TURN_OFF_EFFECT, Self::SHIELD_EFFECT);
    }

    pub fn break_shield(player: &mut Player) {
        Self::remove_shield(player);
        let _ = ServiceHandles::send_item_time(player, 3784, 0);
    }

    pub fn set_blind_dctt(player: &mut Player, time_stun: u64) {
        let current_time = time::current_time_millis();
        player.effect_skill.is_blind_dctt = true;
        player.effect_skill.time_blind_dctt = time_stun;
        player.effect_skill.last_time_blind_dctt = current_time;
    }

    pub fn set_blind_dctt_mob(mob: &mut RtMob, time_stun: u64) {
        let current_time = time::current_time_millis();
        mob.effect_skill.is_blind_dctt = true;
        mob.effect_skill.time_blind_dctt = time_stun;
        mob.effect_skill.last_time_blind_dctt = current_time;
    }

    pub fn set_thoi_mien(player: &mut Player, time_sleep: u64) {
        let current_time = time::current_time_millis();
        player.effect_skill.is_thoi_mien = true;
        player.effect_skill.time_thoi_mien = time_sleep;
        player.effect_skill.last_time_thoi_mien = current_time;
    }

    pub fn set_thoi_mien_mob(mob: &mut RtMob, time_sleep: u64) {
        let current_time = time::current_time_millis();
        mob.effect_skill.is_thoi_mien = true;
        mob.effect_skill.time_thoi_mien = time_sleep;
        mob.effect_skill.last_time_thoi_mien = current_time;
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

        Self::send_effect_player(player, player, Self::TURN_ON_EFFECT, Self::BLIND_EFFECT);
    }

    pub fn start_stun_mob(mob: &mut RtMob, time_stun: u64) {
        let current_time = time::current_time_millis();
        mob.effect_skill.is_stun = true;
        mob.effect_skill.time_stun = time_stun;
        mob.effect_skill.last_time_stun = current_time;
    }

    pub fn send_effect_player(player_use: &Player, player_target: &Player, toggle: u8, effect: u8) {
        let mut msg = Message::new(-124);
        let _ = msg.write_byte(toggle as i8); // 0: huy, 1: bat dau
        let _ = msg.write_byte(0); // 0: player, 1: mob

        if toggle == 2 {
            let _ = msg.write_int(player_target.id as i32);
        } else {
            let _ = msg.write_byte(effect as i8);
            let _ = msg.write_int(player_target.id as i32);
            let _ = msg.write_int(player_use.id as i32);
        }
        let _ = ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }

    pub fn send_effect_mob(player_use: &Player, mob_target: &RtMob, toggle: u8, effect: u8) {
        let mut msg = Message::new(-124);
        let _ = msg.write_byte(toggle as i8); // 0: huy, 1: bat dau
        let _ = msg.write_byte(1); // 0: player, 1: mob

        if toggle == 2 {
            let _ = msg.write_byte(mob_target.id as i8);
        } else {
            let _ = msg.write_byte(effect as i8);
            let _ = msg.write_byte(mob_target.id as i8);
            let _ = msg.write_int(player_use.id as i32);
        }
        let _ = ServiceHandles::send_mess_all_player_in_map(player_use, msg);
    }
}
