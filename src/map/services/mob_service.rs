use crate::item::item::Item;
use crate::item::{ItemOption, ItemService};
use crate::map::item_map::ItemMap;
use crate::map::map_service::is_ma_black_ball_war;
use crate::map::models::zone::{Zone, ZoneHandle};
use crate::map::services::item_map_service::ItemMapService;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::services::effect_skill_service::EffectSkillService;
use crate::services::ServiceHandles;
use crate::templates::item_template_manager;
use crate::utils::random::{is_true, next_int};
use crate::utils::{time, MapUtils};
use tracing::{debug, info};

pub async fn attack_mob(player: &Player, mob_id: i32, damage: i32) {
    let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
    if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
        // Now we send a message to the actor instead of locking
        // We'll add this message to ZoneActor soon
        let _ = zone
            .tx
            .send(crate::map::models::zone::ZoneMessage::AttackMob {
                player_id: player.id,
                mob_id: mob_id as u64,
                damage,
            })
            .await;
    }
}

pub async fn attack_mob_actor(zone: &mut Zone, player_id: u64, mob_id: u64, damage: i32) {
    let (msg_opt, drop_info) = {
        if let Some(mob) = zone.active_mobs.iter_mut().find(|m| m.id == mob_id) {
            let real_damage = mob.take_damage(damage);
            mob.add_temporary_enemy(player_id);
            let new_hp = mob.hp;
            if !mob.is_dead() {
                (
                    Some(build_mob_alive_message(
                        mob.id as i8,
                        new_hp,
                        real_damage,
                        false,
                    )),
                    None,
                )
            } else {
                let drop_x = mob.location.x;
                let drop_y = mob.location.y;
                let mob_temp_id = mob.template_id;
                handle_mob_death(mob);
                (
                    Some(build_mob_die_message(mob.id as i8, real_damage, false)),
                    Some((drop_x, drop_y, mob_temp_id)),
                )
            }
        } else {
            debug!("Mob {} NOT FOUND in active_mobs", mob_id);
            (None, None)
        }
    };

    if let Some(msg) = msg_opt {
        for handle in zone.players.values() {
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
    zone: &mut Zone,
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

    let items = get_mob_rewards(map_id, mob_template_id);

    for item in items {
        if item.template.is_none() {
            continue;
        }

        static NEXT_ID: std::sync::atomic::AtomicI32 = std::sync::atomic::AtomicI32::new(1);
        let item_map_id = NEXT_ID.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        let mut item_map = ItemMap::new(
            item_map_id,
            item.template.clone(),
            item.quantity,
            x,
            y,
            player_id as i64,
        );
        item_map.set_location(map_id, zone_id, x, y);

        item_map.options = item.item_options.clone();

        zone.active_items.push(item_map.clone());
        let msg = ItemMapService::build_item_appear_message(&item_map);
        for handle in zone.players.values() {
            handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                msg.clone(),
            ));
        }
    }
}

fn get_mob_rewards(map_id: i32, mob_template_id: i16) -> Vec<Item> {
    let mut drops: Vec<Item> = Vec::new();

    if crate::map::services::map_service::is_ma_black_ball_war(map_id) {
        let vang_quantity = next_int(500, 3000);
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
    if is_true(1, 1) {
        if let Some(mut item) = ItemService::create_new_item(14) {
            item.add_option_param(30, 100);
            drops.push(item);
        }
    }

    drops
}

pub async fn update_actor(zone: &mut Zone) {
    let current_time = time::current_time_millis();
    let mut global_msgs = Vec::new();
    let mut attack_candidates = Vec::new();

    for mob in zone.active_mobs.iter_mut() {
        if !mob.is_alive {
            handle_respawn(mob, current_time, &mut global_msgs);
        } else {
            handle_mob_effects(mob, current_time, &mut global_msgs);
            handle_self_recovery(mob, current_time, &mut global_msgs);

            if current_time > mob.start_time_attack_player + 1000 {
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
        if let Some((target_id, is_retaliation, player_loc, dist)) =
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
                ));
            }
        }
    }

    if !attacks.is_empty() {
        for (mob_id, target_id, is_retaliation, mob_loc, player_loc, dist) in attacks {
            if let Some(mob) = zone.active_mobs.iter_mut().find(|m| m.id == mob_id) {
                if mob.is_alive && can_mob_attack(mob, current_time) {
                    let reason = if is_retaliation {
                        "retaliation"
                    } else {
                        "aggressive"
                    };
                    info!(
                        "Mob {} (temp {}) Name {} ATK Player {} [Reason: {}] Mob:({},{}) -> Pl:({},{}) Dist:{:.1}",
                        mob.id, mob.template_id, mob.name, target_id, reason, mob_loc.x, mob_loc.y, player_loc.x, player_loc.y, dist
                    );
                    mob.start_time_attack_player = current_time;
                    if let Some(handle) = zone.players.get(&target_id) {
                        let damage = mob.get_dame_attack();
                        handle.send_forget(crate::player::player_actor::PlayerMessage::Injured {
                            damage: damage as u64,
                            piercing: false,
                        });

                        let msg_me = build_mob_attack_me_message(mob.id as i8, damage);
                        handle.send_forget(crate::player::player_actor::PlayerMessage::SendPacket(
                            msg_me,
                        ));

                        let msg_others =
                            build_mob_attack_player_message(mob.id as i8, target_id as i32, 0);
                        for other_handle in zone.players.values() {
                            if other_handle.id != target_id {
                                other_handle.send_forget(
                                    crate::player::player_actor::PlayerMessage::SendPacket(
                                        msg_others.clone(),
                                    ),
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    for msg in global_msgs {
        for handle in zone.players.values() {
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

            msgs.push(build_mob_alive_message(
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
    zone: &Zone,
    mob_location: &crate::utils::location::Location,
    temporary_enemies: &[u64],
    template_id: i8,
) -> Option<(u64, bool, crate::utils::location::Location, f64)> {
    for &player_id in temporary_enemies {
        if let Some(handle) = zone.players.get(&player_id) {
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = handle
                .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
                .await;
            if let Ok(snapshot) = rx.await {
                if !snapshot.is_die() {
                    let dist = MapUtils::calculate_distance(mob_location, &snapshot.location);
                    if dist <= 250.0 {
                        return Some((player_id, true, snapshot.location.clone(), dist));
                    }
                }
            }
        }
    }
    let is_boss = is_big_boss(template_id);
    if template_id > 18 || is_boss {
        let mut closest_info = None;
        let mut min_dist = if is_boss { f64::MAX } else { 100.0 };

        for handle in zone.players.values() {
            let (tx, rx) = tokio::sync::oneshot::channel();
            let _ = handle
                .send(crate::player::player_actor::PlayerMessage::GetSnapshot(tx))
                .await;
            if let Ok(snapshot) = rx.await {
                if !snapshot.is_die() {
                    let dist = MapUtils::calculate_distance(mob_location, &snapshot.location);
                    if dist <= min_dist {
                        min_dist = dist;
                        closest_info = Some((handle.id, false, snapshot.location.clone(), dist));
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
    info!(
        "build_mob_attack_me_message: mob_id: {}, damage: {}",
        mob_id, damage
    );
    msg
}

pub fn build_mob_attack_player_message(mob_id: i8, player_id: i32, hp: i32) -> Message {
    let mut msg = Message::new(-10);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_int(player_id);
    let _ = msg.write_int(hp);
    info!(
        "build_mob_attack_player_message: mob_id: {}, player_id: {}, hp: {}",
        mob_id, player_id, hp
    );
    msg
}

pub fn build_mob_alive_message(mob_id: i8, hp: i32, damage: i32, is_crit: bool) -> Message {
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
