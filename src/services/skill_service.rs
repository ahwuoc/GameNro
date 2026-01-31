use crate::entities::player;
use crate::map::services::mob_service;
use crate::map::{map_service, zone, zone_manager};
use crate::models::skill_model::Skill;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::services::{player_info_service, ServiceHandles};
use crate::utils::{skill_util, time, MapUtils};
use crate::{mob::mob::RtMob, templates::skill_template_manager};

pub fn handle_use_skill_packet(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
    mut message: Option<Message>,
) {
    let mut status = 0;
    if let Some(msg) = &mut message {
        if let Ok(s) = msg.read_byte() {
            status = s;
        }
    }

    if status == Skill::USE_SKILL_NOT_FOCUS {
        if let Some(msg) = &mut message {}
    }
    execute_skill(player, pl_target, mob_target);
}

pub fn execute_skill(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) -> Option<Message> {
    if !player.is_skill_ready() || !player.has_enough_mana() {
        println!(
            "[DEBUG SKILL] Player {} cannot use skill (cooldown or mana)",
            player.name
        );
        return None;
    }

    let skill_id = match &player.player_skill.skill_select {
        Some(s) => s.template_id,
        None => return None,
    };

    let Some(temp) = crate::templates::skill_template_manager::get(skill_id) else {
        return None;
    };

    println!(
        "[DEBUG SKILL] use_skill called. Player: {}, Skill ID: {}, Type: {}",
        player.name, skill_id, temp.r#type
    );

    match (temp.r#type, skill_id) {
        (_, Skill::KAIOKEN) => {
            let hp_use = player.n_point.hp_max / 10;
            if player.n_point.hp_current > hp_use {
                player.n_point.hp_current -= hp_use;
                execute_attack_skill(player, pl_target, mob_target)
            } else {
                None
            }
        }
        (_, Skill::QUA_CAU_KENH_KHI) => {
            execute_genkidama(player, pl_target, mob_target);
            None
        }
        (_, Skill::DICH_CHUYEN_TUC_THOI) => {
            execute_dichchuyentucthoi(player, pl_target, mob_target);
            None
        }
        (_, Skill::THOI_MIEN) => {
            execute_thoimien(player, pl_target, mob_target);
            None
        }
        (_, Skill::HUYT_SAO) => {
            execute_huyt_sao(player);
            None
        }
        (_, Skill::TU_SAT) => {
            execute_tu_sat(player);
            None
        }
        (3, _) => {
            execute_skill_type3(player);
            None
        }
        (1, _) | (4, _) => execute_attack_skill(player, pl_target, mob_target),
        (t, id) => {
            println!("Skill Type {} / ID {} chua dc trien khai", t, id);
            None
        }
    }
}

pub fn execute_dichchuyentucthoi(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    println!(
        "[DEBUG SKILL] execute_instant_transmission called for player {} with mob_target: {}",
        player.name,
        mob_target.is_some()
    );
    let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let skill_point = skill_select.point;
    let time_stun = skill_util::get_time_dctt(skill_point);

    if let Some(target) = pl_target {
        // Teleport
        player.location.x = target.location.x;
        player.location.y = target.location.y;
        map_service::send_player_teleport(player);

        // Attack & Stun
        deal_damage_to_player(player, target, false);
        EffectSkillService::apply_blind_dctt(&mut target.effect_skill, time_stun);
        EffectSkillService::send_effect_player(
            player,
            target,
            EffectAction::START,
            EffectSkillService::BLIND_EFFECT,
        );
        let _ = ServiceHandles::send_item_time(target, 3779, (time_stun / 1000) as i16);
    }

    if let Some(mob) = mob_target {
        println!(
            "[DEBUG SKILL] DCTT: Teleporting to mob {} at ({}, {})",
            mob.id, mob.location.x, mob.location.y
        );
        // Teleport
        player.location.x = mob.location.x;
        player.location.y = mob.location.y;
        map_service::send_player_teleport(player);
        EffectSkillService::apply_blind_dctt(&mut mob.effect_skill, time_stun);
        EffectSkillService::send_effect_mob(
            player,
            mob,
            EffectAction::START,
            EffectSkillService::BLIND_EFFECT,
        );
    }
    apply_skill_cost(player);
}

pub fn execute_thoimien(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    println!(
        "[DEBUG SKILL] execute_hypnosis called for player {} with mob_target: {}",
        player.name,
        mob_target.is_some()
    );
    EffectSkillService::send_effect_use_skill(player, Skill::THOI_MIEN as i16);
    let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let skill_point = skill_select.point;
    let time_sleep = skill_util::get_time_thoi_mien(skill_point);

    if let Some(target) = pl_target {
        EffectSkillService::set_thoi_mien(target, time_sleep);
        EffectSkillService::send_effect_player(
            player,
            target,
            EffectAction::START,
            EffectSkillService::SLEEP_EFFECT,
        );
        let _ = crate::services::ServiceHandles::send_item_time(
            target,
            3782,
            (time_sleep / 1000) as i16,
        );
    }

    if let Some(mob) = mob_target {
        println!("[DEBUG SKILL] Thoi Mien: Applying sleep to mob {}", mob.id);
        EffectSkillService::set_thoi_mien_mob(mob, time_sleep);
        EffectSkillService::send_effect_mob(
            player,
            mob,
            EffectAction::START,
            EffectSkillService::SLEEP_EFFECT,
        );
    }
    apply_skill_cost(player);
}

pub fn execute_genkidama(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    if !player.player_skill.prepare_qckk {
        // Phase 1: Start Charging
        player.player_skill.prepare_qckk = true;
        player.player_skill.last_time_prepare_qckk = crate::utils::time::current_time_millis();
        broadcast_skill_charging(player, 4000);
    } else {
        // Phase 2: Release
        player.player_skill.prepare_qckk = false;

        if let Some(target) = pl_target {
            deal_damage_to_player(player, target, false);
        }

        let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
            return;
        };
        let skill_point = skill_select.point;
        let range = skill_util::get_range_qckk(skill_point);

        let mut center_loc = None;
        let mut mob_target_id = None;

        if let Some(mob) = mob_target.as_ref() {
            center_loc = Some(mob.location.clone());
            mob_target_id = Some(mob.id);
        }

        if let Some(mob) = mob_target {
            if let Some(msg) = deal_damage_to_mob(player, mob, false) {
                let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
                }
            }
        }

        if let Some(center) = center_loc {
            let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
            if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
                let mobs = zone.get_all_mobs();
                let mut hit_count = 0;
                for mut mob in mobs {
                    if !mob.is_dead()
                        && mob_target_id.map_or(true, |id| id != mob.id)
                        && MapUtils::is_position_in_range(&center, &mob.location, range)
                    {
                        if let Some(msg) = deal_damage_to_mob(player, &mut mob, false) {
                            let _ =
                                crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
                        }
                        hit_count += 1;
                    }
                }
            }
        }

        player_info_service::send_info_hp_mp_money(player);
        apply_skill_cost(player);
    }
}

/// Broadcast that player is charging a skill to all players in map
pub fn broadcast_skill_charging(player: &Player, time_prepare: i32) {
    let mut msg = Message::new(-45);
    if let Ok(_) = msg.write_byte(4) {
        let _ = msg.write_int(player.id as i32);
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let _ = msg.write_short(skill_id);
        let _ = msg.write_short(time_prepare as i16);
        let _ = crate::services::ServiceHandles::send_mess_all_player_in_map(player, msg);
    }
}

pub fn execute_skill_type3(player: &mut Player) {
    let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let skill_id = skill_select.template_id;
    match skill_id {
        Skill::THAI_DUONG_HA_SAN => {
            execute_thai_duong_ha_san(player);
        }
        Skill::TAI_TAO_NANG_LUONG => {
            println!("[DEBUG SKILL] TAI TAO NANG LUONG");
            EffectSkillService::start_charge(player);
            apply_skill_cost(player);
        }
        Skill::BIEN_KHI => {
            println!("[DEBUG SKILL] BIEN KHI");
            EffectSkillService::start_use_skill_monkey(player);
            apply_skill_cost(player);
        }
        Skill::KHIEN_NANG_LUONG => {
            let skill_id = player
                .player_skill
                .skill_select
                .as_ref()
                .map(|s| s.skill_id)
                .unwrap_or(0);
            EffectSkillService::send_effect_use_skill(player, skill_id);
            EffectSkillService::start_shield(player);
            EffectSkillService::send_effect_player(
                player,
                player,
                EffectAction::START,
                EffectSkillService::SHIELD_EFFECT,
            );
            let _ = ServiceHandles::send_item_time(
                player,
                3784,
                (player.effect_skill.shield_duration_ms / 1000) as i16,
            );
            apply_skill_cost(player);
        }
        _ => {
            println!("Skill Alone {} not implemented", skill_id);
        }
    }
}

pub fn execute_thai_duong_ha_san(player: &mut Player) {
    let Some(skill) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let skill_level = skill.point;
    let time_stun = skill_util::get_time_stun(skill_level);
    let range_skill = skill_util::get_range_stun(skill_level);

    let Some(zone) = &zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id) else {
        return;
    };
    let mut affected_mobs = Vec::new();
    let mobs = zone.get_all_mobs();
    for mob in mobs {
        if MapUtils::is_position_in_range(&player.location, &mob.location, range_skill) {
            let _ = zone.start_stun_mob(mob.id, time_stun);
            affected_mobs.push(mob.id as u8);
        }
    }
    let affected_players = Vec::new(); //player chua trien khai
    EffectSkillService::send_effect_blind_thai_duong_ha_san(
        player,
        affected_players,
        affected_mobs,
        time_stun as i32,
    );
    apply_skill_cost(player);
}

pub async fn learn_full_skill(pl: &mut Player) -> anyhow::Result<()> {
    let template_skill = skill_template_manager::get_by_nclass(pl.gender as i32);
    pl.player_skill.skills.clear();
    for temp in template_skill {
        let max_level = temp.skills.len() as i32;
        if let Some(skill) = skill_util::create_skill(temp.id as i32, max_level).await {
            println!("Skill {} created with level {}", temp.id, max_level);
            pl.player_skill.skills.push(skill);
        }
    }
    player_info_service::send_player_blob_internal(pl).await?;
    Ok(())
}

pub fn execute_attack_skill(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) -> Option<Message> {
    let miss = false;

    if let Some(target) = pl_target {
        deal_damage_to_player(player, target, miss);
    }

    let damage_msg = if let Some(mob) = mob_target {
        deal_damage_to_mob(player, mob, miss)
    } else {
        None
    };

    apply_skill_cost(player);
    damage_msg
}

/// Calculate and apply damage from player to another player
pub fn deal_damage_to_player(player: &mut Player, target: &mut Player, miss: bool) {
    if miss {
        return;
    }
    let dame_attack = player.n_point.get_dame_attack(false);
    let dame_hit = if target.n_point.def < dame_attack {
        dame_attack - target.n_point.def
    } else {
        1
    };

    target.injured(dame_hit as u64, false);
    let is_die = target.is_die();
    let is_crit = player.n_point.crit == 100;
    let _ = ServiceHandles::send_player_attack_player(player, target.id, dame_hit, is_die, is_crit);
}

pub fn deal_damage_to_mob(player: &mut Player, mob: &mut RtMob, miss: bool) -> Option<Message> {
    if miss {
        return None;
    }
    let dame_attack = player.n_point.get_dame_attack(false);
    let dame_hit = dame_attack;

    let _ = ServiceHandles::send_player_attack_mob(player, mob.id as u8);

    let real_damage = mob.take_damage(dame_hit);
    let is_crit = player.n_point.crit >= 100;

    // Build damage message
    if mob.is_dead() {
        Some(crate::map::services::mob_service::build_mob_die_message(
            mob.id as i8,
            real_damage,
            is_crit,
        ))
    } else {
        Some(crate::map::services::mob_service::build_mob_alive_message(
            mob.id as i8,
            mob.hp,
            real_damage,
            is_crit,
        ))
    }
}

pub fn apply_skill_cost(player: &mut Player) {
    if let Some(ref mut skill) = player.player_skill.skill_select {
        skill.start_time_use = time::current_time_millis();
        if player.n_point.mp_current >= skill.mana_use as i32 {
            player.n_point.mp_current -= skill.mana_use as i32;
        }
    }
}

/// Skill Huýt Sáo - Hồi HP cho đồng đội trong zone
pub fn execute_huyt_sao(player: &mut Player) {
    println!(
        "[DEBUG SKILL] execute_huyt_sao called for player {}",
        player.name
    );

    let Some(skill) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let percent_hp = skill_util::get_percent_hp_huyt_sao(skill.point);
    let player_id = player.id;
    let player_map_id = player.map_id;
    let player_zone_id = player.zone_id;

    let zone_manager = &zone_manager::ZONE_MANAGER;

    // Two-Phase pattern để tránh deadlock
    // Phase 1: Collect targets và update HP (không gửi message)
    struct BuffedTarget {
        id: u64,
        heal_amount: i32,
    }
    let mut buffed_targets: Vec<BuffedTarget> = Vec::new();
    let mut damaged_targets: Vec<u64> = Vec::new();

    if let Some(zone) = zone_manager.get_zone(player_map_id, player_zone_id) {
        let player_ids: Vec<u64> = zone.player_ids.iter().map(|r| *r.key()).collect();

        for pid in player_ids {
            if pid == player_id {
                continue;
            }

            if let Some(mut target_entry) =
                crate::player::player_manager::PLAYER_MANAGER.get_mut(pid)
            {
                let target = target_entry.value_mut();
                let is_namec = target.gender == 1; // NAMEC = 1

                if !is_namec {
                    target.effect_skill.ti_le_hp_huyt_sao = percent_hp;
                    target.effect_skill.last_time_huyt_sao = time::current_time_millis();
                    target.n_point.huyt_sao_buff = percent_hp;
                    target.n_point.set_base_point();
                    let heal_amount =
                        (target.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
                    target.n_point.hp_current =
                        target.n_point.hp_current.saturating_add(heal_amount);
                    if target.n_point.hp_current > target.n_point.hp_max {
                        target.n_point.hp_current = target.n_point.hp_max;
                    }
                    buffed_targets.push(BuffedTarget {
                        id: pid,
                        heal_amount,
                    });
                } else {
                    // Namec bị mất HP
                    let damage = target.n_point.hp_max * 10 / 100;
                    if target.n_point.hp_current > damage {
                        target.n_point.hp_current -= damage;
                    }
                    damaged_targets.push(pid);
                }
            }
        }
    } // Lock released here

    // Phase 2: Send messages (không giữ lock)
    for buffed in &buffed_targets {
        if let Some(target) = crate::player::player_manager::PLAYER_MANAGER.get(buffed.id) {
            EffectSkillService::send_effect_player(
                player,
                &target,
                EffectAction::START,
                EffectSkillService::HUYT_SAO_EFFECT,
            );
            let _ = ServiceHandles::send_item_time(&target, 3781, 30);
            let _ = player_info_service::send_point_info_sync(&target);
            let _ = player_info_service::send_info_hp_mp_money(&target);
            println!(
                "[DEBUG SKILL] HUYT_SAO buffed player {} HP max +{}%, healed {} HP",
                target.name, 100, buffed.heal_amount
            );
        }
    }

    for pid in &damaged_targets {
        if let Some(target) = crate::player::player_manager::PLAYER_MANAGER.get(*pid) {
            let _ = player_info_service::send_info_hp_mp_money(&target);
            println!("[DEBUG SKILL] HUYT_SAO damaged Namec {}", target.name);
        }
    }

    let is_namec = player.gender == 1;
    if !is_namec {
        player.effect_skill.ti_le_hp_huyt_sao = percent_hp;
        player.effect_skill.last_time_huyt_sao = time::current_time_millis();

        player.n_point.huyt_sao_buff = percent_hp;
        player.n_point.set_base_point();

        let heal_amount = (player.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
        player.n_point.hp_current = player.n_point.hp_current.saturating_add(heal_amount);
        if player.n_point.hp_current > player.n_point.hp_max {
            player.n_point.hp_current = player.n_point.hp_max;
        }

        let _ = ServiceHandles::send_item_time(player, 3781, 30);
        let _ = player_info_service::send_point_info_sync(player);
        player_info_service::send_info_hp_mp_money(player);

        println!(
            "[DEBUG SKILL] HUYT_SAO self-buff: healed {} HP ({}%), HP max now: {}",
            heal_amount, percent_hp, player.n_point.hp_max
        );
    }

    apply_skill_cost(player);
}

/// Skill Tự Sát - Gồng lên rồi nổ, gây damage xung quanh
pub fn execute_tu_sat(player: &mut Player) {
    println!(
        "[DEBUG SKILL] execute_tu_sat called for player {}",
        player.name
    );

    if !player.player_skill.prepare_tu_sat {
        // Phase 1: Gồng tự sát
        player.player_skill.prepare_tu_sat = true;
        player.player_skill.last_time_prepare_tu_sat = time::current_time_millis();
        broadcast_skill_bomb(player, 2000);
        println!("[DEBUG SKILL] TU_SAT Phase 1: Charging...");
    } else {
        // Check thời gian gồng (1.5s)
        let elapsed = time::current_time_millis() - player.player_skill.last_time_prepare_tu_sat;
        if elapsed < 1500 {
            player.player_skill.prepare_tu_sat = false;
            if let Some(ref mut skill) = player.player_skill.skill_select {
                skill.start_time_use = time::current_time_millis();
            }
            println!("[DEBUG SKILL] TU_SAT cancelled - too early");
            return;
        }

        // Phase 2: Nổ
        player.player_skill.prepare_tu_sat = false;

        let Some(skill) = player.player_skill.skill_select.as_ref() else {
            return;
        };
        let range_bom = skill_util::get_range_bom(skill.point);
        let dame = player.n_point.hp_max as i64;

        let player_id = player.id;
        let player_location = player.location.clone();
        let player_map_id = player.map_id;
        let player_zone_id = player.zone_id;

        println!(
            "[DEBUG SKILL] TU_SAT Phase 2: Exploding! Range={}, Damage={}",
            range_bom, dame
        );

        let zone_manager = &zone_manager::ZONE_MANAGER;
        if let Some(zone) = zone_manager.get_zone(player_map_id, player_zone_id) {
            let mob_damage_msgs: Vec<Message> = {
                let mut mobs = zone.active_mobs.write().unwrap();
                let mut msgs = Vec::new();
                for mob in mobs.iter_mut() {
                    if MapUtils::is_position_in_range(&player_location, &mob.location, range_bom) {
                        let real_damage = mob.take_damage(dame as i32);
                        let msg = if mob.is_dead() {
                            mob_service::build_mob_die_message(mob.id as i8, real_damage, false)
                        } else {
                            mob_service::build_mob_alive_message(
                                mob.id as i8,
                                mob.hp,
                                real_damage,
                                false,
                            )
                        };
                        msgs.push(msg);
                        println!(
                            "[DEBUG SKILL] TU_SAT hit mob {} for {} damage",
                            mob.id, dame
                        );
                    }
                }
                msgs
            }; // Lock released here

            // Phase 2: Send messages (no lock held)
            for msg in mob_damage_msgs {
                let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
            }

            // Gây damage cho player trong tầm
            let player_ids: Vec<u64> = zone.player_ids.iter().map(|r| *r.key()).collect();
            for pid in player_ids {
                if pid == player_id {
                    continue;
                }
                if let Some(mut target_entry) =
                    crate::player::player_manager::PLAYER_MANAGER.get_mut(pid)
                {
                    let target = target_entry.value_mut();
                    if MapUtils::is_position_in_range(&player_location, &target.location, range_bom)
                    {
                        target.injured(dame as u64, false);
                        player_info_service::send_info_hp_mp_money(target);
                        println!(
                            "[DEBUG SKILL] TU_SAT hit player {} for {} damage",
                            target.name, dame
                        );
                    }
                }
            }
        }

        apply_skill_cost(player);

        // Player chết sau khi nổ
        player.n_point.hp_current = 0;
        let _ = player_info_service::send_info_hp_mp_money(player);
        send_char_die(player);
        println!(
            "[DEBUG SKILL] TU_SAT: Player {} died from self-destruct",
            player.name
        );
    }
}

/// Broadcast that player is charging bomb skill
pub fn broadcast_skill_bomb(player: &Player, time_prepare: i32) {
    let mut msg = Message::new(-45);
    if let Ok(_) = msg.write_byte(7) {
        // 7 = bomb prepare
        let _ = msg.write_int(player.id as i32);
        let skill_id = player
            .player_skill
            .skill_select
            .as_ref()
            .map(|s| s.skill_id)
            .unwrap_or(0);
        let _ = msg.write_short(skill_id);
        let _ = msg.write_short(time_prepare as i16);
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }
}

pub fn send_char_die(player: &Player) {
    // Message -17: Tell the player they died (for self)
    let mut msg_self = Message::new(-17);
    let _ = msg_self.write_byte(player.id as i8);
    let _ = msg_self.write_short(player.location.x);
    let _ = msg_self.write_short(player.location.y);
    let _ = player.send_to_client(msg_self);

    // Message -8: Tell others this player died
    let mut msg_others = Message::new(-8);
    let _ = msg_others.write_short(player.id as i16);
    let _ = msg_others.write_byte(0); // cPk = 0
    let _ = msg_others.write_short(player.location.x);
    let _ = msg_others.write_short(player.location.y);
    let _ = ServiceHandles::send_mess_all_player_in_map(player, msg_others);

    println!(
        "[DEBUG] send_char_die: Player {} died at ({}, {})",
        player.name, player.location.x, player.location.y
    );
}

pub fn send_skill_shortcut(player: &Player) -> anyhow::Result<()> {
    let skill_data = player.player_skill.skill_shortcut.clone();

    // Send KSkill
    let mut msg_k = Message::new(-30);
    msg_k.write_byte(61)?;
    msg_k.write_utf("KSkill")?;
    msg_k.write_int(skill_data.len() as i32)?;
    msg_k.write(&skill_data)?;
    player.send_to_client(msg_k)?;

    // Send OSkill
    let mut msg_o = Message::new(-30);
    msg_o.write_byte(61)?;
    msg_o.write_utf("OSkill")?;
    msg_o.write_int(skill_data.len() as i32)?;
    msg_o.write(&skill_data)?;
    player.send_to_client(msg_o)?;

    Ok(())
}

pub fn select_skill(player: &mut Player, skill_template_id: i32) -> anyhow::Result<()> {
    if let Some(skill) = player
        .player_skill
        .skills
        .iter()
        .find(|s| s.template_id == skill_template_id as i32)
    {
        player.player_skill.skill_select = Some(skill.clone());
        println!(
            "[DEBUG SKILL] Selected skill template_id: {}",
            skill_template_id
        );
    } else {
        println!(
            "Skill not found with template_id: {}. Player has {} skills: {:?}",
            skill_template_id,
            player.player_skill.skills.len(),
            player
                .player_skill
                .skills
                .iter()
                .map(|s| s.template_id)
                .collect::<Vec<_>>()
        );
    }
    Ok(())
}

pub fn send_release_cooldown(player: &Player) -> anyhow::Result<()> {
    let mut msg = Message::new(-94);
    for skill in &player.player_skill.skills {
        msg.write_short(skill.skill_id)?;
        msg.write_int(0)?;
    }
    player.send_to_client(msg)?;
    Ok(())
}
