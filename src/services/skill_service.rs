use crate::entities::player;
use crate::map::{map_service, zone, zone_manager};
use crate::models::skill_model::Skill;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::services::{player_info_service, ServiceHandles};
use crate::utils::{skill_util, time, MapUtils};
use crate::{mob::mob::RtMob, templates::skill_template_manager};

/// Handle incoming USE_SKILL packet from client (-45)
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
) {
    if !player.is_skill_ready() || !player.has_enough_mana() {
        println!(
            "[DEBUG SKILL] Player {} cannot use skill (cooldown or mana)",
            player.name
        );
        return;
    }

    let skill_id = match &player.player_skill.skill_select {
        Some(s) => s.template_id,
        None => return,
    };

    let Some(temp) = crate::templates::skill_template_manager::get(skill_id) else {
        return;
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
                execute_attack_skill(player, pl_target, mob_target);
            }
        }
        (_, Skill::QUA_CAU_KENH_KHI) => execute_genkidama(player, pl_target, mob_target),
        (_, Skill::DICH_CHUYEN_TUC_THOI) => {
            execute_dichchuyentucthoi(player, pl_target, mob_target)
        }
        (_, Skill::THOI_MIEN) => execute_thoimien(player, pl_target, mob_target),
        (3, _) => execute_skill_type3(player),
        (1, _) | (4, _) => execute_attack_skill(player, pl_target, mob_target),
        (t, id) => println!("Skill Type {} / ID {} chua dc trien khai", t, id),
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
    let skill_point = player.player_skill.skill_select.as_ref().unwrap().point;
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
    let skill_point = player.player_skill.skill_select.as_ref().unwrap().point;
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
            // TODO: Check mobs around player target if needed
        }

        // Handle mob target (attack main target + AoE around it)
        let skill_point = player.player_skill.skill_select.as_ref().unwrap().point;
        let range = skill_util::get_range_qckk(skill_point);

        // Find main mob target location to center AoE
        let mut center_loc = None;
        let mut mob_target_id = None;

        if let Some(mob) = mob_target.as_ref() {
            // Borrow immutably first
            center_loc = Some(mob.location.clone());
            mob_target_id = Some(mob.id);
        }

        // If mob_target provided, attack it first (Mutable borrow)
        if let Some(mob) = mob_target {
            deal_damage_to_mob(player, mob, false);
        }

        // Calculate AoE Damage for surrounding mobs
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
                        // Apply damage
                        let dame_attack = player.n_point.get_dame_attack(true);
                        mob.take_damage(dame_attack);

                        deal_damage_to_mob(player, &mut mob, false);
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
        let _ = msg.write_short(player.player_skill.skill_select.as_ref().unwrap().skill_id);
        let _ = msg.write_short(time_prepare as i16);
        let _ = crate::services::ServiceHandles::send_mess_all_player_in_map(player, msg);
    }
}

pub fn execute_skill_type3(player: &mut Player) {
    let skill_id = player
        .player_skill
        .skill_select
        .as_ref()
        .unwrap()
        .template_id;
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
            EffectSkillService::send_effect_use_skill(
                player,
                player.player_skill.skill_select.as_ref().unwrap().skill_id,
            );
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

/// Execute a basic attack skill (type 1 or 4)
pub fn execute_attack_skill(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    let miss = false;

    if let Some(target) = pl_target {
        deal_damage_to_player(player, target, miss);
    }

    if let Some(mob) = mob_target {
        deal_damage_to_mob(player, mob, miss);
    }

    apply_skill_cost(player);
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
}

/// Calculate and apply damage from player to mob
pub fn deal_damage_to_mob(player: &mut Player, mob: &mut RtMob, miss: bool) {
    if miss {
        return;
    }
    let dame_attack = player.n_point.get_dame_attack(false);
    let dame_hit = dame_attack;
    mob.take_damage(dame_hit);
}

/// Apply mana cost and update cooldown after using a skill
pub fn apply_skill_cost(player: &mut Player) {
    if let Some(ref mut skill) = player.player_skill.skill_select {
        skill.start_time_use = time::current_time_millis();
        if player.n_point.mp_current >= skill.mana_use as i32 {
            player.n_point.mp_current -= skill.mana_use as i32;
        }
    }
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
