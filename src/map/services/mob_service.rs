use crate::constant::const_item::{ITEM_DUI_GA_BINH_THUONG, ITEM_DUI_GA_NUONG};
use crate::constant::task_type::TaskType;
use crate::constant::{self, const_mob};
use crate::item::item::Item;
use crate::item::{ItemOption, ItemService};
use crate::map::item_map::ItemMap;
use crate::map::map_service::{self, is_map_black_ball_war, is_map_tanthu};
use crate::map::models::zone::ZoneHandle;
use crate::map::models::zone_actor::ZoneActor;
use crate::map::services::item_map_service::ItemMapService;
use crate::map::zone::ZoneMessage;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use crate::services::effect_skill_service::EffectSkillService;
use crate::services::player_tnsm_services::TypeTNSM;
use crate::services::task_utils::TaskUtils;
use crate::services::ServiceHandles;
use crate::templates::item_template_manager;
use crate::utils::{time, MapUtils};
use rand::{rng, Rng};
use tracing::{debug, info};

pub async fn attack_mob(
    player: &Player,
    mob_id: i32,
    damage: i32,
    is_crit: bool,
    die_when_hp_full: bool,
) {
    let zone_manager = &ZONE_MANAGER;
    if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
        let _ = zone
            .tx
            .send(ZoneMessage::AttackMob {
                player_id: player.id,
                mob_id: mob_id as u64,
                damage,
                is_crit,
                die_when_hp_full,
                player_power: player.n_point.power,
            })
            .await;
    }
}

pub async fn attack_mob_actor(
    zone: &mut ZoneActor,
    player_id: u64,
    mob_id: u64,
    damage: i32,
    is_crit: bool,
    die_when_hp_full: bool,
    player_power: i64,
) {
    let (msg_opt, drop_info) = {
        if let Some(mob) = zone.active_mobs.iter_mut().find(|m| m.id == mob_id) {
            if !mob.is_alive {
                (None, None)
            } else {
                let real_damage = mob.take_damage(damage, die_when_hp_full);
                mob.add_temporary_enemy(player_id);
                let new_hp = mob.hp;
                info!(
                    "Mob {} (temp {}) takes {} damage. HP: {}/{}",
                    mob.id, mob.template_id, real_damage, new_hp, mob.max_hp
                );

                if let Some(handle) = zone.active_players.get(&player_id) {
                    let tnsm_amount = mob.get_tiemnang_for_player(player_power, real_damage as i64);
                    handle.send_forget(PlayerMessage::AddTNSM {
                        type_tnsm: TypeTNSM::All,
                        param: tnsm_amount,
                        is_ori: true,
                    });
                }
                if !mob.is_dead() {
                    (
                        Some(build_mob_take_dame_client(
                            mob.id as i8,
                            new_hp,
                            real_damage,
                            is_crit,
                        )),
                        None,
                    )
                } else {
                    info!("Mob {} is dead!", mob.id);
                    let drop_x = mob.location.x;
                    let drop_y = mob.location.y;
                    let mob_temp_id = mob.template_id;

                    if let Some(handle) = zone.active_players.get(&player_id) {
                        handle.send_forget(PlayerMessage::TaskAction(
                            TaskType::KillMob,
                            mob_temp_id.to_string(),
                        ));
                    }

                    handle_mob_death(mob);
                    (
                        Some(build_mob_die_message(mob.id as i8, real_damage, is_crit)),
                        Some((drop_x, drop_y, mob_temp_id)),
                    )
                }
            }
        } else {
            info!("Mob {} NOT FOUND in active_mobs", mob_id);
            (None, None)
        }
    };

    if let Some(msg) = msg_opt {
        for handle in zone.active_players.values() {
            handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                msg.clone(),
            ));
        }
    }

    if let Some((x, y, mob_temp_id)) = drop_info {
        drop_item_on_mob_death_actor(
            zone,
            x as i32,
            y as i32,
            player_id,
            zone.map_id,
            zone.zone_id,
            mob_temp_id as i16,
        )
        .await;
    }
}

async fn drop_item_on_mob_death_actor(
    zone: &mut ZoneActor,
    x: i32,
    y: i32,
    player_id: u64,
    map_id: i32,
    zone_id: i32,
    mob_template_id: i16,
) {
    if mob_template_id == 0 {
        return;
    }

    let mut task_id = -1;
    let mut task_index = -1;
    if let Some(handle) = zone.active_players.get(&player_id) {
        let (tx, rx) = tokio::sync::oneshot::channel();
        let _ = handle.send(PlayerMessage::GetSnapshot(tx)).await;
        if let Ok(player) = rx.await {
            task_id = TaskUtils::get_id_task(&player);
            task_index = TaskUtils::get_task_index(&player);
        }
    }

    let items = get_mob_rewards(map_id, mob_template_id, task_id, task_index);

    if items.is_empty()
        && task_id == constant::task_id::TASK_6
        && task_index == 1
        && (mob_template_id == const_mob::THAN_LAN_ME as i16
            || mob_template_id == const_mob::PHI_LONG_ME as i16
            || mob_template_id == const_mob::QUY_BAY_ME as i16)
    {
        if let Some(handle) = zone.active_players.get(&player_id) {
            let _ = handle.send_forget(PlayerMessage::SendPacket(ServiceHandles::build_thong_bao(
                "Con thằn lằn mẹ này không giữ ngọc, hãy tìm con thằn lằn mẹ khác",
            )));
        }
    } else if !items.is_empty()
        && task_id == constant::task_id::TASK_6
        && task_index == 1
        && (mob_template_id == constant::const_mob::THAN_LAN_ME as i16
            || mob_template_id == constant::const_mob::PHI_LONG_ME as i16
            || mob_template_id == constant::const_mob::QUY_BAY_ME as i16)
    {
        if let Some(handle) = zone.active_players.get(&player_id) {
            handle.send_forget(PlayerMessage::TaskAction(
                TaskType::TaskScripts,
                mob_template_id.to_string(),
            ));
        }
    }

    for item in items {
        if item.template.is_none() {
            continue;
        }

        let mut item_map =
            ItemMap::new(item.template.clone(), item.quantity, x, y, player_id as i64);
        item_map.set_location(map_id, zone_id, x, y);

        item_map.options = item.item_options.clone();

        zone.active_items.push(item_map.clone());
        let msg = ItemMapService::build_item_appear_message(&item_map);
        for handle in zone.active_players.values() {
            handle.send_forget(PlayerMessage::SendPacket(msg.clone()));
        }
    }
}

fn get_mob_rewards(map_id: i32, mob_template_id: i16, task_id: i32, task_index: i32) -> Vec<Item> {
    let mut drops: Vec<Item> = Vec::new();

    if is_map_tanthu(map_id) && task_id == constant::task_id::TASK_2 {
        if let Some(dui_ga) = ItemService::create_new_item(ITEM_DUI_GA_BINH_THUONG) {
            drops.push(dui_ga);
        }
    }

    if task_id == constant::task_id::TASK_6
        && task_index == 1
        && (mob_template_id == constant::const_mob::THAN_LAN_ME as i16
            || mob_template_id == constant::const_mob::PHI_LONG_ME as i16
            || mob_template_id == constant::const_mob::QUY_BAY_ME as i16)
    {
        if rng().random_ratio(10, 100) {
            if let Some(item) = ItemService::create_new_item(20) {
                drops.push(item);
            }
        }
    }
    if map_service::is_map_black_ball_war(map_id) {
        let vang_quantity = rng().random_range(500..=3000);
        let gold_id = if vang_quantity < 1000 {
            76
        } else if vang_quantity < 2000 {
            188
        } else {
            189
        };
        if let Some(item) = ItemService::create_new_item(gold_id) {
            drops.push(item);
        }
    }
    drops
}

pub async fn update_actor(zone: &mut ZoneActor) {
    let current_time = time::current_time_millis();
    let mut global_msgs = Vec::new();
    let mut attack_candidates = Vec::new();

    for mob in zone.active_mobs.iter_mut() {
        if !mob.is_alive {
            handle_respawn(mob, current_time, &mut global_msgs);
        } else {
            handle_mob_effects(mob, current_time, &mut global_msgs);
            handle_self_recovery(mob, current_time, &mut global_msgs);

            let is_passive = mob
                .template
                .as_ref()
                .map(|t| t.r#type == 0)
                .unwrap_or(false);
            if !is_passive && current_time > mob.start_time_attack_player + 1000 {
                attack_candidates.push((
                    mob.id,
                    mob.location.clone(),
                    mob.temporary_enemies.clone(),
                    mob.template_id,
                    mob.start_time_attack_player,
                ));
            }
        }
    }

    let mut attacks = Vec::new();
    for (mob_id, location, enemies, template_id, last_attack_time) in attack_candidates {
        if let Some((target_id, is_retaliation, player_loc, dist, player_hp)) =
            find_target_accurate_actor(zone, &location, &enemies, template_id).await
        {
            let cooldown = if is_retaliation { 1000 } else { 2000 };
            if current_time > last_attack_time + cooldown {
                attacks.push((
                    mob_id,
                    target_id,
                    is_retaliation,
                    location,
                    player_loc,
                    dist,
                    player_hp,
                ));
            }
        }
    }

    if !attacks.is_empty() {
        for (mob_id, target_id, is_retaliation, mob_loc, player_loc, dist, player_hp) in attacks {
            if let Some(mob) = zone.active_mobs.iter_mut().find(|m| m.id == mob_id) {
                if mob.is_alive && can_mob_attack(mob, current_time) {
                    mob.start_time_attack_player = current_time;
                    if let Some(handle) = zone.active_players.get(&target_id) {
                        let damage = mob.get_dame_attack();
                        tracing::info!(
                            "[MOB_ATK] Mob {} (temp {}) ATK Pl {}. Dmg: {}. Retaliatory: {}. Last Atk: {}, Wait: {}",
                            mob.id, mob.template_id, target_id, damage, is_retaliation, mob.start_time_attack_player, current_time - mob.start_time_attack_player
                        );

                        handle.send_forget(crate::player::player_actor::PlayerMessage::Injured {
                            damage: damage as u64,
                            piercing: false,
                            from_mob: true,
                            attacker_id: None,
                        });

                        let msg_me = build_mob_attack_me_message(mob.id as i8, damage);
                        tracing::info!("[MOB_ATK] SENDING Command -11 to Victim {}", target_id);
                        handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                            msg_me,
                        ));

                        let msg_others = build_mob_attack_player_message(
                            mob.id as i8,
                            target_id as i32,
                            player_hp.saturating_sub(damage as i32),
                        );
                        for (other_id, other_handle) in zone.active_players.iter() {
                            if *other_id != target_id {
                                tracing::info!(
                                    "[MOB_ATK] BROADCAST Command -10 to Observer {}",
                                    other_id
                                );
                                other_handle.send_forget(
                                    crate::player::player_actor::PlayerMessage::SendPacket(
                                        msg_others.clone(),
                                    ),
                                );
                            }
                        }
                        tracing::info!("[MOB_ATK] DONE Mob {} atk on Pl {}", mob.id, target_id);
                        tracing::debug!(
                            "[DEBUG_MOB_ATK] FINISH Mob {} attack on Pl {}",
                            mob.id,
                            target_id
                        );
                    }
                }
            }
        }
    }

    for msg in global_msgs {
        for handle in zone.active_players.values() {
            handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                msg.clone(),
            ));
        }
    }
}

fn handle_mob_death(mob: &mut RtMob) {
    mob.die();
}

fn handle_respawn(mob: &mut RtMob, current_time: u64, msgs: &mut Vec<Message>) {
    if current_time > mob.last_time_die + 3000 {
        mob.is_alive = true;
        mob.hp = mob.max_hp;
        mob.status = mob.spawn_status;
        msgs.push(build_mob_respawn_message(
            mob.id as i8,
            mob.template_id,
            mob.hp,
        ));
        debug!("Mob {} respawned at HP {}", mob.id, mob.hp);
    }
}

fn handle_self_recovery(mob: &mut RtMob, current_time: u64, msgs: &mut Vec<Message>) {
    if current_time > mob.last_time_recovery + 30000 {
        mob.last_time_recovery = current_time;
        if mob.hp < mob.max_hp {
            let recover_amount = mob.max_hp / 10;
            mob.hp = (mob.hp + recover_amount).min(mob.max_hp);

            msgs.push(build_mob_take_dame_client(
                mob.id as i8,
                mob.hp,
                recover_amount,
                false,
            ));
        } else {
            msgs.push(build_mob_respawn_message(
                mob.id as i8,
                mob.template_id,
                mob.hp,
            ));
        }
    }
}

fn can_mob_attack(mob: &RtMob, current_time: u64) -> bool {
    if let Some(template) = &mob.template {
        if template.r#type == 0 {
            return false;
        }
    }
    if mob.effect_skill.is_stun
        || mob.effect_skill.is_blind_dctt
        || mob.effect_skill.is_thoi_mien
        || mob.effect_skill.an_troi
    {
        return false;
    }
    current_time > mob.start_time_attack_player + 2000
}

fn handle_mob_effects(mob: &mut RtMob, current_time: u64, global_msgs: &mut Vec<Message>) {
    if mob.effect_skill.is_stun {
        if current_time >= mob.effect_skill.last_time_stun + mob.effect_skill.time_stun {
            mob.effect_skill.is_stun = false;
            mob.effect_skill.time_stun = 0;
            debug!("Mob {} het choang", mob.id);
        }
    }

    if mob.effect_skill.is_blind_dctt {
        if current_time >= mob.effect_skill.start_time_dctt + mob.effect_skill.time_blind_dctt {
            mob.effect_skill.is_blind_dctt = false;
            mob.effect_skill.time_blind_dctt = 0;
            debug!("Mob {} het choang DCTT", mob.id);
            global_msgs.push(build_remove_effect_mob_message(
                mob.id as i8,
                EffectSkillService::BLIND_EFFECT,
            ));
        }
    }

    if mob.effect_skill.is_thoi_mien {
        if current_time >= mob.effect_skill.start_time_thoi_mien + mob.effect_skill.time_thoi_mien {
            mob.effect_skill.is_thoi_mien = false;
            mob.effect_skill.time_thoi_mien = 0;
            debug!("Mob {} het thoi mien", mob.id);
            global_msgs.push(build_remove_effect_mob_message(
                mob.id as i8,
                EffectSkillService::SLEEP_EFFECT,
            ));
        }
    }

    if mob.effect_skill.an_troi {
        if current_time >= mob.effect_skill.start_time_an_troi + mob.effect_skill.time_an_troi {
            mob.effect_skill.an_troi = false;
            mob.effect_skill.time_an_troi = 0;
            mob.effect_skill.start_time_an_troi = 0;
            debug!("Mob {} het bi troi", mob.id);
            global_msgs.push(build_remove_effect_mob_message(
                mob.id as i8,
                EffectSkillService::HOLD_EFFECT,
            ));
        }
    }
}

async fn find_target_accurate_actor(
    zone: &ZoneActor,
    mob_location: &crate::utils::location::Location,
    temporary_enemies: &[u64],
    template_id: i8,
) -> Option<(u64, bool, crate::utils::location::Location, f64, i32)> {
    for &player_id in temporary_enemies {
        if let Some(handle) = zone.active_players.get(&player_id) {
            if handle.boss_info.is_some() {
                continue;
            }
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = handle
                .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
                .await;
            if let Ok(snapshot) = rx.await {
                if !snapshot.is_die() && !snapshot.is_boss {
                    let dist = MapUtils::calculate_distance(mob_location, &snapshot.location);
                    if dist <= 250.0 {
                        return Some((
                            player_id,
                            true,
                            snapshot.location.clone(),
                            dist,
                            snapshot.n_point.hp_current,
                        ));
                    }
                }
            }
        }
    }
    let is_boss = is_big_boss(template_id);
    if template_id > 18 || is_boss {
        let mut closest_info = None;
        let mut min_dist = if is_boss { f64::MAX } else { 100.0 };
        for handle in zone.active_players.values() {
            if handle.boss_info.is_some() {
                continue;
            }
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = handle
                .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
                .await;
            if let Ok(snapshot) = rx.await {
                if !snapshot.is_die() && !snapshot.is_boss {
                    let dist = MapUtils::calculate_distance(mob_location, &snapshot.location);
                    if dist <= min_dist {
                        min_dist = dist;
                        closest_info = Some((
                            handle.id,
                            false,
                            snapshot.location.clone(),
                            dist,
                            snapshot.n_point.hp_current,
                        ));
                    }
                }
            }
        }
        if let Some(res) = closest_info {
            return Some(res);
        }
    }

    None
}

fn is_big_boss(template_id: i8) -> bool {
    matches!(template_id, 70 | 71 | 72 | 77 | 82 | 83 | 84 | 85)
}

pub fn build_mob_attack_me_message(mob_id: i8, damage: i32) -> Message {
    let mut msg = Message::new(-11);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_int(damage);
    msg
}

pub fn build_mob_attack_player_message(mob_id: i8, player_id: i32, hp: i32) -> Message {
    let mut msg = Message::new(-10);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_int(player_id);
    let _ = msg.write_int(hp);
    msg
}

pub fn build_mob_take_dame_client(mob_id: i8, hp: i32, damage: i32, is_crit: bool) -> Message {
    let mut msg = Message::new(-9);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_int(hp);
    let _ = msg.write_int(damage);
    let _ = msg.write_bool(is_crit);
    let _ = msg.write_int(-1);
    msg
}

pub fn build_mob_die_message(mob_id: i8, damage: i32, is_crit: bool) -> Message {
    let mut msg = Message::new(-12);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_int(damage);
    let _ = msg.write_bool(is_crit);
    let _ = msg.write_byte(0);
    msg
}

pub fn build_remove_effect_mob_message(mob_id: i8, effect: u8) -> Message {
    let mut msg = Message::new(-124);
    let _ = msg.write_byte(0); // action = REMOVE
    let _ = msg.write_byte(1); // 1 = mob
    let _ = msg.write_byte(effect as i8);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_int(-1); // no player reference
    msg
}

pub fn build_mob_respawn_message(mob_id: i8, template_id: i8, hp: i32) -> Message {
    let mut msg = Message::new(-13);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_byte(template_id);
    let _ = msg.write_byte(0);
    let _ = msg.write_int(hp);
    msg
}
