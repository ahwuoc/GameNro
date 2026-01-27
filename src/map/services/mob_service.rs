use crate::map::zone::Zone;
use crate::network::message::Message;
use crate::player::player::Player;
use std::time::{SystemTime, UNIX_EPOCH};

pub fn attack_mob(player: &Player, mob_id: i32, damage: i32) {
    let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
    if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
        let msg_opt = {
            let mut mobs = zone.active_mobs.write().unwrap();
            if let Some(mob) = mobs.iter_mut().find(|m| m.id == mob_id as u64) {
                let old_hp = mob.hp;
                let real_damage = mob.take_damage(damage);
                mob.add_temporary_enemy(player.id);
                let new_hp = mob.hp;

                println!(
                    "[MOB_SERVICE] Player {} attacked mob {} for {} damage (HP: {} -> {})",
                    player.name, mob_id, real_damage, old_hp, new_hp
                );
                if !mob.is_dead() {
                    Some(build_mob_alive_message(
                        mob.id as i8,
                        new_hp,
                        real_damage,
                        false,
                    ))
                } else {
                    mob.status = 0;
                    mob.is_alive = false;
                    mob.temporary_enemies.clear();
                    mob.last_time_die = SystemTime::now()
                        .duration_since(UNIX_EPOCH)
                        .unwrap()
                        .as_millis() as u64;

                    Some(build_mob_die_message(mob.id as i8, real_damage, false))
                }
            } else {
                None
            }
        };

        if let Some(msg) = msg_opt {
            let _ = zone.send_message_to_all_players(msg);
        }
    }
}

pub async fn update(zone: &Zone) {
    if zone.players.is_empty() {
        return;
    }

    let current_time = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_millis() as u64;
    let mut msgs = Vec::new();
    let mut player_specific_msgs = Vec::new();

    {
        let mut mobs = zone.active_mobs.write().unwrap();
        for mob in mobs.iter_mut() {
            if !mob.is_alive {
                if current_time > mob.last_time_die + 3000 {
                    mob.is_alive = true;
                    mob.hp = mob.max_hp;
                    mob.status = 5;
                    msgs.push(build_mob_respawn_message(
                        mob.id as i8,
                        mob.template_id,
                        mob.hp,
                    ));
                    println!("[MOB_SERVICE] Mob {} respawned at HP {}", mob.id, mob.hp);
                }
            } else if current_time > mob.last_time_attack_player + 2000 {
                let mut target_id = None;
                for entry in zone.players.iter() {
                    let player = entry.value();
                    if !player.is_die() {
                        let dist = ((mob.location.x - player.location.x).pow(2)
                            + (mob.location.y - player.location.y).pow(2))
                            as f32;
                        let limit = if mob.temporary_enemies.contains(&player.id) {
                            300.0
                        } else {
                            100.0
                        };
                        if dist.sqrt() <= limit {
                            target_id = Some(player.id);
                            break;
                        }
                    }
                }

                if let Some(pid) = target_id {
                    if let Some(mut p_entry) = zone.players.get_mut(&pid) {
                        let player = p_entry.value_mut();
                        mob.last_time_attack_player = current_time;
                        let dame_mob = mob.get_dame_attack();
                        let damage_taken = player.injured(dame_mob as u64, false);

                        println!(
                            "[MOB_SERVICE] Mob {} attacked Player {} for {} damage (HP: {} -> {})",
                            mob.id,
                            player.name,
                            damage_taken,
                            player.n_point.hp as i64 + damage_taken as i64,
                            player.n_point.hp
                        );

                        let msg_me = build_mob_attack_me_message(mob.id as i8, damage_taken as i32);
                        player_specific_msgs.push((player.id, msg_me));

                        let msg_other = build_mob_attack_player_message(
                            mob.id as i8,
                            player.id as i32,
                            player.n_point.hp as i32,
                        );
                        msgs.push(msg_other);
                    }
                }
            }
        }
    }

    for msg in msgs {
        let _ = zone.send_message_to_all_players(msg);
    }
    for (player_id, msg) in player_specific_msgs {
        if let Some(mut p_entry) = zone.players.get_mut(&player_id) {
            let player = p_entry.value_mut();
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
    let _ = msg.write_byte(0); // item count
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
