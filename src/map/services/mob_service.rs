use crate::item::item::Item;
use crate::item::{ItemOption, ItemService};
use crate::map::item_map::ItemMap;
use crate::map::map_service::is_ma_black_ball_war;
use crate::map::services::item_map_service::ItemMapService;
use crate::map::zone::Zone;
use crate::mob::mob::RtMob;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::effect_skill_service::EffectSkillService;
use crate::services::ServiceHandles;
use crate::templates::item_template_manager;
use crate::utils::random::{is_true, next_int};
use crate::utils::{time, MapUtils};

pub fn attack_mob(player: &Player, mob_id: i32, damage: i32) {
    // println!("[DEBUG MOB] attack_mob called. Player: {}, MobID: {}, Dmg: {}", player.name, mob_id, damage);
    let _ = ServiceHandles::send_player_attack_mob(player, mob_id as u8);
    let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
    if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
        let (msg_opt, drop_info) = {
            let mut mobs = zone.active_mobs.write().unwrap();
            if let Some(mob) = mobs.iter_mut().find(|m| m.id == mob_id as u64) {
                // println!("[DEBUG MOB] Found mob {}. HP: {}", mob.id, mob.hp);
                let real_damage = mob.take_damage(damage);
                mob.add_temporary_enemy(player.id);
                let new_hp = mob.hp;

                // println!("[DEBUG MOB] After dmg. New HP: {}, Real Dmg: {}", new_hp, real_damage);

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
                    // println!("[DEBUG MOB] Mob dead!");
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
                println!("[DEBUG MOB] Mob {} NOT FOUND in active_mobs", mob_id);
                (None, None)
            }
        };
        if let Some(msg) = msg_opt {
            let res = crate::services::ServiceHandles::send_to_all_in_zone(&zone, msg);
            if let Err(e) = res {
                println!("[DEBUG MOB] Failed to send msg: {:?}", e);
            } else {
                // println!("[DEBUG MOB] Send damage msg OK");
            }
        } else {
            // println!("[DEBUG MOB] No message generated");
        }
        if let Some((x, y, mob_temp_id)) = drop_info {
            drop_item_on_mob_death(&zone, x as i32, y as i32, player, mob_temp_id as i16);
        }
    } else {
        println!("[DEBUG MOB] Zone NOT FOUND");
    }
}

fn drop_item_on_mob_death(zone: &Zone, x: i32, y: i32, player: &Player, mob_template_id: i16) {
    if mob_template_id == 0 {
        return;
    }

    let items = get_mob_rewards(player, mob_template_id);

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
            player.id as i64,
        );
        item_map.set_location(player.map_id, player.zone_id, x, y);
        item_map.options = item.item_options.clone();

        if zone.add_item(item_map.clone()).is_ok() {
            let msg = ItemMapService::build_item_appear_message(&item_map);
            let _ = crate::services::ServiceHandles::send_to_all_in_zone(zone, msg);
        }
    }
}

fn get_mob_rewards(pl: &Player, mob_template_id: i16) -> Vec<Item> {
    let mut drops: Vec<Item> = Vec::new();

    if is_ma_black_ball_war(pl) {
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

pub fn update(zone: &Zone) {
    let current_time = time::current_time_millis();
    let mut global_msgs = Vec::new();
    let mut player_specific_msgs = Vec::new();

    {
        let mut mobs = zone.active_mobs.write().unwrap();
        for mob in mobs.iter_mut() {
            if !mob.is_alive {
                handle_respawn(mob, current_time, &mut global_msgs);
            } else {
                hanlde_mob_attack_player(
                    mob,
                    zone,
                    current_time,
                    &mut global_msgs,
                    &mut player_specific_msgs,
                );
                handle_self_recovery(mob, current_time, &mut global_msgs);
            }
        }
    }

    broadcast_messages(zone, global_msgs, player_specific_msgs);
}

fn handle_mob_death(mob: &mut RtMob) {
    mob.status = 0;
    mob.is_alive = false;
    mob.temporary_enemies.clear();
    mob.last_time_die = time::current_time_millis();
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
        println!("[MOB_SERVICE] Mob {} respawned at HP {}", mob.id, mob.hp);
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

fn hanlde_mob_attack_player(
    mob: &mut RtMob,
    zone: &Zone,
    current_time: u64,
    global_msgs: &mut Vec<Message>,
    player_msgs: &mut Vec<(u64, Message)>,
) {
    if mob.template_id == 0 {
        return;
    }

    if mob.effect_skill.is_stun {
        if current_time >= mob.effect_skill.last_time_stun + mob.effect_skill.time_stun {
            mob.effect_skill.is_stun = false;
            mob.effect_skill.time_stun = 0;
            println!("Mob {} het choang", mob.id);
        } else {
            println!("Mob {} dang bi choang", mob.id);
            return;
        }
    }

    if mob.effect_skill.is_blind_dctt {
        if current_time >= mob.effect_skill.start_time_dctt + mob.effect_skill.time_blind_dctt {
            mob.effect_skill.is_blind_dctt = false;
            mob.effect_skill.time_blind_dctt = 0;
            println!("Mob {} het choang DCTT", mob.id);
            EffectSkillService::send_remove_effect_mob_in_zone(
                zone,
                mob,
                EffectSkillService::BLIND_EFFECT,
            );
        } else {
            println!("Mob {} dang bi choang DCTT -> Skip attack", mob.id);
            return;
        }
    }

    if mob.effect_skill.is_thoi_mien {
        if current_time >= mob.effect_skill.start_time_thoi_mien + mob.effect_skill.time_thoi_mien {
            mob.effect_skill.is_thoi_mien = false;
            mob.effect_skill.time_thoi_mien = 0;
            println!("Mob {} het thoi mien", mob.id);
            EffectSkillService::send_remove_effect_mob_in_zone(
                zone,
                mob,
                EffectSkillService::SLEEP_EFFECT,
            );
        } else {
            println!("Mob {} dang bi thoi mien -> Skip attack", mob.id);
            return;
        }
    }

    if current_time > mob.start_time_attack_player + 2000 {
        let target_id = find_target_in_range(mob, zone);

        if let Some(pid) = target_id {
            if let Some(mut p_entry) = PLAYER_MANAGER.get_mut(pid) {
                let player = p_entry.value_mut();
                mob.start_time_attack_player = current_time;

                let damage = mob.get_dame_attack();
                let damage_taken = player.injured(damage as u64, false);

                player_msgs.push((
                    player.id,
                    build_mob_attack_me_message(mob.id as i8, damage_taken as i32),
                ));
                global_msgs.push(build_mob_attack_player_message(
                    mob.id as i8,
                    player.id as i32,
                    player.n_point.hp_current as i32,
                ));
            }
        }
    }
}

fn find_target_in_range(mob: &RtMob, zone: &Zone) -> Option<u64> {
    for player_id in zone.player_ids.iter() {
        if let Some(player) = PLAYER_MANAGER.get(*player_id) {
            if !player.is_die() {
                let limit = if mob.temporary_enemies.contains(&player.id) {
                    300.0
                } else {
                    100.0
                };
                if MapUtils::is_position_in_range(&mob.location, &player.location, limit as i16) {
                    return Some(player.id);
                }
            }
        }
    }
    None
}

fn broadcast_messages(zone: &Zone, global_msgs: Vec<Message>, player_msgs: Vec<(u64, Message)>) {
    for msg in global_msgs {
        let _ = crate::services::ServiceHandles::send_to_all_in_zone(zone, msg);
    }
    for (player_id, msg) in player_msgs {
        if let Some(player) = PLAYER_MANAGER.get(player_id) {
            let _ = player.send_to_client(msg);
        }
    }
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

pub fn build_mob_respawn_message(mob_id: i8, template_id: i8, hp: i32) -> Message {
    let mut msg = Message::new(-13);
    let _ = msg.write_byte(mob_id);
    let _ = msg.write_byte(template_id);
    let _ = msg.write_byte(0);
    let _ = msg.write_int(hp);
    msg
}
