use crate::entities::player;
use crate::map::services::mob_service;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::{map_service, zone_manager};
use crate::models::skill_model::Skill;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use crate::player::player_actor::PlayerMessage;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::services::{player_info_service, ServiceHandles};
use crate::utils::{skill_util, time, MapUtils};
use crate::{mob::mob::RtMob, templates::skill_template_manager};
use tracing::{debug, info};

pub async fn handle_use_skill_packet(
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
    execute_skill(player, pl_target, mob_target).await;
}

pub async fn execute_skill(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) -> Option<Message> {
    if !player.is_skill_ready() || !player.has_enough_mana() {
        if player.is_boss {
            tracing::info!("Boss {} cannot use skill: cooldown or mana", player.id);
        } else {
            debug!("Player {} cannot use skill (cooldown or mana)", player.name);
        }
        return None;
    }

    let skill_id = match &player.player_skill.skill_select {
        Some(s) => s.template_id,
        None => return None,
    };

    let Some(temp) = crate::templates::skill_template_manager::get(skill_id) else {
        return None;
    };

    info!(
        "use_skill called. Player: {}, Skill ID: {}, Type: {}",
        player.name, skill_id, temp.r#type
    );

    match (temp.r#type, skill_id) {
        (_, Skill::KAIOKEN) => {
            let hp_use = player.n_point.hp_max / 10;
            if player.n_point.hp_current > hp_use {
                player.n_point.current_hp_sub(hp_use);
                execute_attack_skill(player, pl_target, mob_target).await
            } else {
                None
            }
        }
        (_, Skill::QUA_CAU_KENH_KHI) => {
            execute_genkidama(player, pl_target, mob_target).await;
            None
        }
        (_, Skill::DICH_CHUYEN_TUC_THOI) => {
            execute_dichchuyentucthoi(player, pl_target, mob_target).await;
            None
        }
        (_, Skill::THOI_MIEN) => {
            execute_thoimien(player, pl_target, mob_target);
            None
        }
        (_, Skill::HUYT_SAO) => {
            execute_huyt_sao(player).await;
            None
        }
        (_, Skill::TU_SAT) => {
            execute_tu_sat(player).await;
            None
        }
        (_, Skill::TROI) => {
            execute_troi(player, pl_target, mob_target);
            None
        }
        (3, _) => {
            execute_skill_type3(player).await;
            None
        }
        (1, _) | (4, _) => execute_attack_skill(player, pl_target, mob_target).await,
        (t, id) => {
            debug!("Skill Type {} / ID {} chua dc trien khai", t, id);
            None
        }
    }
}

pub async fn execute_dichchuyentucthoi(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    debug!(
        "execute_instant_transmission called for player {} with mob_target: {}",
        player.name,
        mob_target.is_some()
    );
    let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let skill_point = skill_select.point;
    let time_stun = skill_util::get_time_dctt(skill_point);

    let mut messages: Vec<Message> = Vec::new();

    if let Some(target) = pl_target {
        player.location.x = target.location.x;
        player.location.y = target.location.y;
        messages.push(map_service::build_player_teleport_message(player));

        deal_damage_to_player(player, target, false).await;
        EffectSkillService::apply_blind_dctt(&mut target.effect_skill, time_stun);
        messages.push(EffectSkillService::build_effect_message(
            player.id,
            target.id,
            false,
            EffectAction::START,
            EffectSkillService::BLIND_EFFECT,
        ));
        let _ = ServiceHandles::send_item_time(target, 3779, (time_stun / 1000) as i16);
    }

    if let Some(mob) = mob_target {
        player.location.x = mob.location.x;
        player.location.y = mob.location.y;
        messages.push(map_service::build_player_teleport_message(player));

        EffectSkillService::apply_blind_dctt(&mut mob.effect_skill, time_stun);
        messages.push(EffectSkillService::build_effect_message(
            player.id,
            mob.id,
            true,
            EffectAction::START,
            EffectSkillService::BLIND_EFFECT,
        ));
    }

    apply_skill_cost(player);

    for msg in messages {
        let _ = ServiceHandles::send_mess_all_player_in_map(player, msg);
    }
}

pub fn execute_thoimien(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
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

pub fn execute_troi(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    info!(
        "execute_troi called by {}. Target Player: {}, Target Mob: {}",
        player.name,
        pl_target.is_some(),
        mob_target.is_some()
    );
    let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    EffectSkillService::send_effect_use_skill(player, Skill::TROI as i16);

    let skill_point = skill_select.point;
    let time_hold = skill_util::get_time_troi(skill_point);

    EffectSkillService::set_use_troi(player, time_hold);

    if let Some(target) = pl_target {
        let is_preparing = target.player_skill.prepare_qckk
            || target.player_skill.prepare_laze
            || target.player_skill.prepare_tu_sat;

        if !is_preparing {
            player.effect_skill.pl_an_troi_id = Some(target.id);
            EffectSkillService::set_an_troi(target, player.id, time_hold);
            EffectSkillService::send_effect_player(
                player,
                target,
                EffectAction::START,
                EffectSkillService::HOLD_EFFECT,
            );
        }
    }

    if let Some(mob) = mob_target {
        info!("Applying TROI effect to Mob ID: {}", mob.id);
        player.effect_skill.mob_an_troi_id = Some(mob.id);
        EffectSkillService::set_troi_mob(mob, player.id, time_hold);
        EffectSkillService::send_effect_mob(
            player,
            mob,
            EffectAction::START,
            EffectSkillService::HOLD_EFFECT,
        );
    }

    apply_skill_cost(player);
}

pub async fn execute_genkidama(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    if !player.player_skill.prepare_qckk {
        player.player_skill.prepare_qckk = true;
        player.player_skill.last_time_prepare_qckk = crate::utils::time::current_time_millis();
        broadcast_skill_charging(player, 4000);
    } else {
        player.player_skill.prepare_qckk = false;

        if let Some(target) = pl_target {
            deal_damage_to_player(player, target, false).await;
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
            if let Some(msg) = deal_damage_to_mob(player, mob, false, true).await {
                let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
                }
            }
        }

        if let Some(center) = center_loc {
            let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
            if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
                let _ = zone
                    .tx
                    .send(crate::map::models::zone::ZoneMessage::AreaDamage {
                        attacker_id: player.id,
                        x: center.x,
                        y: center.y,
                        range: range as i16,
                        damage: player.n_point.get_dame_attack(false) as i64,
                        is_player: true,
                        die_when_hp_full: true,
                    })
                    .await;
            }
        }

        player_info_service::send_info_hp_mp_money(player);
        apply_skill_cost(player);
    }
}

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

pub async fn execute_skill_type3(player: &mut Player) {
    let Some(skill_select) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let skill_id = skill_select.template_id;
    match skill_id {
        Skill::THAI_DUONG_HA_SAN => {
            execute_thai_duong_ha_san(player).await;
        }
        Skill::TAI_TAO_NANG_LUONG => {
            EffectSkillService::start_charge(player);
            apply_skill_cost(player);
        }
        Skill::BIEN_KHI => {
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
        _ => {}
    }
}

pub async fn execute_thai_duong_ha_san(player: &mut Player) {
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
    if let Ok(mobs) = zone.get_all_mobs().await {
        for mob in mobs {
            if MapUtils::is_position_in_range(&player.location, &mob.location, range_skill) {
                let _ = zone.start_stun_mob(mob.id, time_stun).await;
                affected_mobs.push(mob.id as u8);
            }
        }
    }
    EffectSkillService::send_effect_blind_thai_duong_ha_san(
        player,
        Vec::new(),
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
            pl.player_skill.skills.push(skill);
        }
    }
    player_info_service::send_player_blob_internal(pl).await?;
    Ok(())
}

pub async fn execute_attack_skill(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) -> Option<Message> {
    if let Some(target) = pl_target {
        deal_damage_to_player(player, target, false).await;
    }

    if let Some(mob) = mob_target {
        if let Some(msg) = deal_damage_to_mob(player, mob, false, false).await {
            let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
            if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
                let _ = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
            }
        }
    }

    apply_skill_cost(player);
    None
}

pub async fn deal_damage_to_player(player: &mut Player, target: &mut Player, miss: bool) {
    if miss {
        return;
    }
    let dame_attack = player.n_point.get_dame_attack(false);
    let dame_hit = if target.n_point.def < dame_attack {
        dame_attack - target.n_point.def
    } else {
        1
    };

    let is_crit = player.n_point.roll_crit();
    let dame_hit = if is_crit { dame_hit * 2 } else { dame_hit };

    let zone_manager = &ZONE_MANAGER;
    if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
        if let Ok(Some(target_handle)) = zone.get_player(target.id).await {
            let _ =
                target_handle.send_forget(crate::player::player_actor::PlayerMessage::Injured {
                    damage: dame_hit as u64,
                    piercing: false,
                    from_mob: false,
                    attacker_id: Some(player.id),
                });
        }
    }

    let is_die = if target.is_boss {
        false
    } else {
        target.n_point.hp_current - dame_hit <= 0
    };
    let _ = ServiceHandles::send_player_attack_player(player, target.id, dame_hit, is_die, is_crit);
}

pub async fn deal_damage_to_mob(
    player: &mut Player,
    mob: &mut RtMob,
    miss: bool,
    die_when_hp_full: bool,
) -> Option<Message> {
    if miss {
        return None;
    }
    let is_crit = player.n_point.roll_crit();
    let dame_attack = player.n_point.get_dame_attack(is_crit);
    let _ = ServiceHandles::send_player_attack_mob(player, mob.id as u8);
    let _ = mob_service::attack_mob(
        player,
        mob.id as i32,
        dame_attack,
        is_crit,
        die_when_hp_full,
    )
    .await;
    None
}

pub fn apply_skill_cost(player: &mut Player) {
    if let Some(ref mut skill) = player.player_skill.skill_select {
        skill.start_time_use = time::current_time_millis();
        if !player.is_boss && player.n_point.mp_current >= skill.mana_use as i32 {
            player.n_point.current_mp_sub(skill.mana_use as i32);
        }
    }
}

pub async fn execute_huyt_sao(player: &mut Player) {
    let Some(skill) = player.player_skill.skill_select.as_ref() else {
        return;
    };
    let percent_hp = skill_util::get_percent_hp_huyt_sao(skill.point);
    let player_id = player.id;

    if let Some(zone) = zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
        if let Ok(handles) = zone.get_all_players().await {
            for handle in handles {
                let pid = handle.id;
                if pid == player_id {
                    continue;
                }

                if let Some(target) = handle.get_snapshot().await {
                    if target.gender != 1 {
                        handle.send_forget(PlayerMessage::ApplyHuytSaoBuff { percent_hp });
                        EffectSkillService::send_effect_player(
                            player,
                            &target,
                            EffectAction::START,
                            EffectSkillService::HUYT_SAO_EFFECT,
                        );
                    } else {
                        let damage = target.n_point.hp_max as u64 * 10 / 100;
                        handle.send_forget(PlayerMessage::Injured {
                            damage,
                            piercing: false,
                            from_mob: false,
                            attacker_id: Some(player.id),
                        });
                    }
                }
            }
        }
    }

    if player.gender != 1 {
        player.effect_skill.ti_le_hp_huyt_sao = percent_hp;
        player.effect_skill.last_time_huyt_sao = time::current_time_millis();
        player.n_point.huyt_sao_buff = percent_hp;
        player.n_point.set_base_point();

        let heal_amount = (player.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
        player.n_point.current_hp_add(heal_amount);

        let _ = ServiceHandles::send_item_time(player, 3781, 30);
        let _ = player_info_service::send_point_info_sync(player);
        player_info_service::send_info_hp_mp_money(player);
    }

    apply_skill_cost(player);
}

pub async fn execute_tu_sat(player: &mut Player) {
    if !player.player_skill.prepare_tu_sat {
        player.player_skill.prepare_tu_sat = true;
        player.player_skill.last_time_prepare_tu_sat = time::current_time_millis();
        broadcast_skill_bomb(player, 2000);
    } else {
        let elapsed = time::current_time_millis() - player.player_skill.last_time_prepare_tu_sat;
        if elapsed < 1500 {
            player.player_skill.prepare_tu_sat = false;
            if let Some(ref mut skill) = player.player_skill.skill_select {
                skill.start_time_use = time::current_time_millis();
            }
            return;
        }

        player.player_skill.prepare_tu_sat = false;
        let Some(skill) = player.player_skill.skill_select.as_ref() else {
            return;
        };
        let range_bom = skill_util::get_range_bom(skill.point);
        let dame = player.n_point.hp_max as i64;
        let p_loc = player.location.clone();

        if let Some(zone) = zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
            let _ = zone
                .tx
                .send(crate::map::models::zone::ZoneMessage::AreaDamage {
                    attacker_id: player.id,
                    x: p_loc.x,
                    y: p_loc.y,
                    range: range_bom as i16,
                    damage: dame,
                    is_player: true,
                    die_when_hp_full: true,
                })
                .await;

            if let Ok(handles) = zone.get_all_players().await {
                let is_monkey = player.effect_skill.is_monkey;
                let base_dame = dame;
                for handle in handles {
                    if handle.id == player.id {
                        continue;
                    }
                    let p_loc_clone = p_loc.clone();
                    let attacker_id = player.id;
                    tokio::spawn(async move {
                        if let Some(target) = handle.get_snapshot().await {
                            if MapUtils::is_position_in_range(
                                &p_loc_clone,
                                &target.location,
                                range_bom,
                            ) {
                                let mut actual_dame = base_dame;
                                if target.is_boss {
                                    actual_dame = if is_monkey {
                                        base_dame / 3
                                    } else {
                                        base_dame / 2
                                    };
                                }

                                handle.send_forget(PlayerMessage::Injured {
                                    damage: actual_dame as u64,
                                    piercing: false,
                                    from_mob: false,
                                    attacker_id: Some(attacker_id),
                                });
                            }
                        }
                    });
                }
            }
        }

        apply_skill_cost(player);
        player.n_point.hp_current = 0;
        let _ = player_info_service::send_info_hp_mp_money(player);
        send_char_die(player);
    }
}

pub fn broadcast_skill_bomb(player: &Player, time_prepare: i32) {
    let mut msg = Message::new(-45);
    if let Ok(_) = msg.write_byte(7) {
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
    let mut msg_self = Message::new(-17);
    let _ = msg_self.write_byte(player.id as i8);
    let _ = msg_self.write_short(player.location.x);
    let _ = msg_self.write_short(player.location.y);
    let _ = player.send_to_client(msg_self);

    let mut msg_others = Message::new(-8);
    let _ = msg_others.write_short(player.id as i16);
    let _ = msg_others.write_byte(0);
    let _ = msg_others.write_short(player.location.x);
    let _ = msg_others.write_short(player.location.y);
    let _ = ServiceHandles::send_mess_all_player_in_map(player, msg_others);
}

pub fn send_skill_shortcut(player: &Player) -> anyhow::Result<()> {
    let skill_data = &player.player_skill.skill_shortcut;
    let mut msg_k = Message::new(-30);
    msg_k.write_byte(61)?;
    msg_k.write_utf("KSkill")?;
    msg_k.write_int(skill_data.len() as i32)?;
    for &b in skill_data.iter() {
        msg_k.write_byte(b)?;
    }
    player.send_to_client(msg_k)?;

    let mut msg_o = Message::new(-30);
    msg_o.write_byte(61)?;
    msg_o.write_utf("OSkill")?;
    msg_o.write_int(skill_data.len() as i32)?;
    for &b in skill_data.iter() {
        msg_o.write_byte(b)?;
    }
    player.send_to_client(msg_o)?;
    Ok(())
}

pub fn select_skill(player: &mut Player, skill_template_id: i32) -> anyhow::Result<()> {
    if let Some(skill) = player
        .player_skill
        .skills
        .iter()
        .find(|s| s.template_id == skill_template_id)
    {
        player.player_skill.skill_select = Some(skill.clone());
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
