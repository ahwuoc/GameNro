use crate::mob::mob::RtMob;
use crate::models::skill_model::Skill;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player::Player;
use std::time::{SystemTime, UNIX_EPOCH};

pub fn can_use_skill_with_mana(player: &Player) -> bool {
    if let Some(skill) = &player.player_skill.skill_select {
        if player.n_point.mp >= skill.mana_use as i32 {
            return true;
        }
    }
    false
}

pub fn can_use_skill_with_cooldown(player: &Player) -> bool {
    if let Some(skill) = &player.player_skill.skill_select {
        let current_time = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
        return current_time > skill.last_time_use + skill.cool_down as u64;
    }
    false
}

pub async fn use_skill(
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

    if status == 20 {
        if let Some(msg) = &mut message {
            let _skill_id = msg.read_byte().unwrap_or(0);
            let _dx = msg.read_short().unwrap_or(0);
            let _dy = msg.read_short().unwrap_or(0);
            let _dir = msg.read_byte().unwrap_or(0);
            let _x = msg.read_short().unwrap_or(0);
            let _y = msg.read_short().unwrap_or(0);
        }
    }

    if !can_use_skill_with_cooldown(player) || !can_use_skill_with_mana(player) {
        return;
    }

    let skill_id = if let Some(skill) = &player.player_skill.skill_select {
        skill.template_id
    } else {
        return;
    };

    match skill_id {
        Skill::DRAGON | Skill::DEMON | Skill::GALICK => {
            use_skill_attack(player, pl_target, mob_target).await;
        }
        Skill::KAMEJOKO | Skill::MASENKO | Skill::ANTOMIC => {
            use_skill_attack(player, pl_target, mob_target).await;
        }
        _ => {
            println!("Skill {} not implemented yet", skill_id);
        }
    }
}

pub async fn use_skill_attack(
    player: &mut Player,
    pl_target: Option<&mut Player>,
    mob_target: Option<&mut RtMob>,
) {
    let miss = false;

    if let Some(target) = pl_target {
        player_attack_player(player, target, miss).await;
    }

    if let Some(mob) = mob_target {
        player_attack_mob(player, mob, miss).await;
    }

    after_use_skill(player);
}

pub async fn player_attack_player(player: &mut Player, target: &mut Player, miss: bool) {
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

pub async fn player_attack_mob(player: &mut Player, mob: &mut RtMob, miss: bool) {
    if miss {
        return;
    }
    let dame_attack = player.n_point.get_dame_attack(false);
    let dame_hit = dame_attack;
    mob.take_damage(dame_hit);
}

pub fn after_use_skill(player: &mut Player) {
    if let Some(ref mut skill) = player.player_skill.skill_select {
        skill.last_time_use = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
        if player.n_point.mp >= skill.mana_use as i32 {
            player.n_point.mp -= skill.mana_use as i32;
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
