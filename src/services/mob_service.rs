use crate::map::zone::Zone;
use crate::mob::mob::RtMob;
use crate::network::message::Message;
use crate::player::player::Player;
use std::time::{SystemTime, UNIX_EPOCH};

/// Xử lý khi người chơi tấn công quái
pub async fn attack_mob(player: &Player, mob_id: i32, damage: i32) {
    if let Some(zone) = &player.zone {
        let msg_opt = {
            let mut mobs = zone.active_mobs.write().await;
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
                    handle_mob_death(mob);
                    Some(build_mob_die_message(mob.id as i8, real_damage, false))
                }
            } else {
                None
            }
        };

        if let Some(msg) = msg_opt {
            let _ = zone.send_message_to_all_players(msg).await;
        }
    }
}

/// Cập nhật logic của quái trong Zone (hồi sinh, tấn công, hồi máu)
pub async fn update(zone: &Zone) {
    let current_time = get_current_time();
    let mut global_msgs = Vec::new();
    let mut player_specific_msgs = Vec::new();

    {
        let mut mobs = zone.active_mobs.write().await;
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
                )
                .await;
                handle_self_recovery(mob, current_time, &mut global_msgs);
            }
        }
    }

    broadcast_messages(zone, global_msgs, player_specific_msgs).await;
}

fn handle_mob_death(mob: &mut RtMob) {
    mob.status = 0;
    mob.is_alive = false;
    mob.temporary_enemies.clear();
    mob.last_time_die = get_current_time();
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
async fn hanlde_mob_attack_player(
    mob: &mut RtMob,
    zone: &Zone,
    current_time: u64,
    global_msgs: &mut Vec<Message>,
    player_msgs: &mut Vec<(u64, Message)>,
) {
    if mob.template_id == 0 {
        return;
    }
    if current_time > mob.last_time_attack_player + 2000 {
        let target_id = find_target_in_range(mob, zone);

        if let Some(pid) = target_id {
            if let Some(mut p_entry) = zone.players.get_mut(&pid) {
                let player = p_entry.value_mut();
                mob.last_time_attack_player = current_time;

                let damage = mob.get_dame_attack();
                let damage_taken = player.injured(damage as u64, false);

                println!(
                    "[MOB_SERVICE] Mob {} (Type: {}) attacked Player {} for {} damage (HP: {} -> {})",
                    mob.id,
                    mob.template_id,
                    player.name,
                    damage_taken,
                    player.n_point.hp as i64 + damage_taken as i64,
                    player.n_point.hp
                );

                player_msgs.push((
                    player.id,
                    build_mob_attack_me_message(mob.id as i8, damage_taken as i32),
                ));
                global_msgs.push(build_mob_attack_player_message(
                    mob.id as i8,
                    player.id as i32,
                    player.n_point.hp as i32,
                ));
            }
        }
    }
}

fn find_target_in_range(mob: &RtMob, zone: &Zone) -> Option<u64> {
    for entry in zone.players.iter() {
        let player = entry.value();
        if !player.is_die() {
            let dx = (mob.location.x - player.location.x) as i32;
            let dy = (mob.location.y - player.location.y) as i32;
            let dist = ((dx * dx + dy * dy) as f32).sqrt();

            let limit = if mob.temporary_enemies.contains(&player.id) {
                300.0
            } else {
                100.0
            };

            if dist <= limit {
                return Some(player.id);
            }
        }
    }
    None
}

async fn broadcast_messages(
    zone: &Zone,
    global_msgs: Vec<Message>,
    player_msgs: Vec<(u64, Message)>,
) {
    for msg in global_msgs {
        let _ = zone.send_message_to_all_players(msg).await;
    }
    for (player_id, msg) in player_msgs {
        if let Some(entry) = zone.players.get(&player_id) {
            let _ = entry.value().send_to_client(msg).await;
        }
    }
}

fn get_current_time() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_millis() as u64
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
    let _ = msg.write_int(-1); // unknown
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
