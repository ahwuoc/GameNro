use std::time::SystemTime;

use crate::constant::const_map;
use crate::database::DbManager;
use crate::map::map_service;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message; // [NEW]
use crate::player::player::Player;
use crate::player::player_mapper;
use crate::services::ServiceHandles;
use crate::utils::time::current_time_millis; // [NEW]
use anyhow::Result;
use chrono::Duration;
use sea_orm::ActiveModelTrait;
use sqlx::any;

const TIME_REVIVE: u64 = 1500;

pub async fn save_player(player: &Player) -> Result<()> {
    let db = DbManager::get_pool();
    let active_model = player_mapper::to_active_model(player);
    active_model.update(db).await?;
    Ok(())
}

fn try_pay_gold(pl: &mut Player, cost: i64) -> Result<bool> {
    if pl.inventory.sub_gold(cost) {
        Ok(true)
    } else {
        ServiceHandles::send_message_alert(
            pl,
            &format!(
                "Không đủ vàng để thực hiện, còn thiếu {} vàng",
                cost as i64 - pl.inventory.gold
            ),
        )?;
        Ok(false)
    }
}
fn try_pay_gem(pl: &mut Player, cost: i32) -> Result<bool> {
    if pl.inventory.sub_gem(cost) {
        Ok(true)
    } else {
        ServiceHandles::send_message_alert(
            pl,
            &format!(
                "Không đủ ngọc để thực hiện, còn thiếu {} ngọc",
                cost as i64 - pl.inventory.gem as i64
            ),
        )?;
        Ok(false)
    }
}
pub fn hoi_sinh(pl: &mut Player) -> Result<()> {
    if !pl.is_die() || pl.map_id == 51 {
        return Ok(());
    }

    let now = current_time_millis();
    if now - pl.last_time_revived < TIME_REVIVE {
        return Ok(());
    }

    let can_respawn = if map_service::is_ma_black_ball_war(pl) {
        try_pay_gold(pl, 50_000)?
    } else {
        try_pay_gem(pl, 1)?
    };

    if !can_respawn {
        return Ok(());
    }

    pl.last_time_revived = now;
    pl.revive();
    pl.n_point.set_hp(pl.n_point.hp_max);
    pl.n_point.set_mp(pl.n_point.mp_max);

    ServiceHandles::send_message_eat_dauthan(pl)?;
    ServiceHandles::send_gold_gem_ruby_to_client(pl)?;
    send_message_hs_char(pl)?;

    Ok(())
}

fn send_message_hs_char(player: &Player) -> anyhow::Result<()> {
    let mut msg = Message::new(-16);
    msg.write_int(player.id as i32)?;
    msg.write_short(player.location.x)?;
    msg.write_short(player.location.y)?;
    msg.write_int(player.n_point.hp_max)?;
    msg.write_int(player.n_point.mp_max)?;
    ServiceHandles::send_mess_all_player_in_map(player, msg)?;
    Ok(())
}

use crate::map::zone::Zone;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};

pub fn update(zone: &Zone) {
    let mut shield_removed_players: Vec<u64> = Vec::new();
    let mut charge_stopped_players: Vec<u64> = Vec::new();
    let mut bienkhi_finish_players: Vec<u64> = Vec::new();
    let mut monkey_down_players: Vec<u64> = Vec::new();

    for player_id in zone.player_ids.iter() {
        if let Some(mut player) = PLAYER_MANAGER.get_mut(*player_id) {
            let now = current_time_millis();
            if player.effect_skill.is_shield
                && now
                    > player.effect_skill.shield_start_time + player.effect_skill.shield_duration_ms
            {
                player.effect_skill.is_shield = false;
                shield_removed_players.push(player.id);
            }

            // Charging update
            if let Some(result) = player.update_charging() {
                if result.should_stop {
                    player.effect_skill.is_charging = false;
                    player.effect_skill.count_charging = 0;
                    charge_stopped_players.push(player.id);
                }
            }

            // Bien Khi animation finish check (after 1500ms, transform into monkey)
            if player.effect_skill.is_skill_bienkhi
                && now
                    > player.effect_skill.time_start_bienkhi
                        + player.effect_skill.time_duration_bienkhi
            {
                bienkhi_finish_players.push(player.id);
            }

            // Monkey duration expire check
            if player.effect_skill.is_monkey
                && now > player.effect_skill.last_time_up_monkey + player.effect_skill.time_monkey
            {
                monkey_down_players.push(player.id);
            }

            // Base stats & death check
            player.n_point.set_base_point();
            if player.n_point.hp_current <= 0 && !player.dead_flag {
                player.dead_flag = true;
            }
        }
    }

    for player_id in shield_removed_players {
        if let Some(player) = PLAYER_MANAGER.get(player_id) {
            EffectSkillService::send_effect_player(
                &player,
                &player,
                EffectAction::REMOVE,
                EffectSkillService::SHIELD_EFFECT,
            );
        }
    }

    for player_id in charge_stopped_players {
        if let Some(player) = PLAYER_MANAGER.get(player_id) {
            EffectSkillService::send_effect_stop_charge(&player);
        }
    }
    // Phase 3: Finish Bien Khi animation -> transform into monkey
    // Collect state updates first, then send messages after releasing lock
    let mut monkey_updates: Vec<crate::services::effect_skill_service::MonkeyStateUpdate> =
        Vec::new();
    for player_id in bienkhi_finish_players {
        if let Some(mut player) = PLAYER_MANAGER.get_mut(player_id) {
            if let Some(update) = EffectSkillService::finish_use_monkey_state(&mut player) {
                monkey_updates.push(update);
            }
        }
    }
    // Now send messages (no locks held)
    for update in &monkey_updates {
        EffectSkillService::send_monkey_messages(update);
    }

    // Phase 4: Monkey duration expired -> revert to normal
    let mut monkey_down_updates: Vec<crate::services::effect_skill_service::MonkeyStateUpdate> =
        Vec::new();
    for player_id in monkey_down_players {
        if let Some(mut player) = PLAYER_MANAGER.get_mut(player_id) {
            let update = EffectSkillService::monkey_down_state(&mut player);
            monkey_down_updates.push(update);
        }
    }
    // Now send messages (no locks held)
    for update in &monkey_down_updates {
        EffectSkillService::send_monkey_messages(update);
    }
}
