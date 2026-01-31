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
    let now = current_time_millis();

    let mut shield_removed: Vec<u64> = Vec::new();
    let mut charge_stopped: Vec<u64> = Vec::new();
    let mut bienkhi_finished: Vec<u64> = Vec::new();
    let mut monkey_down: Vec<u64> = Vec::new();
    let mut huyt_sao_expired: Vec<u64> = Vec::new();

    for player_id in zone.player_ids.iter() {
        if let Some(mut player) = PLAYER_MANAGER.get_mut(*player_id) {
            let result = player.effect_skill.update(now);

            if result.shield_removed {
                shield_removed.push(player.id);
            }
            if result.charge_stopped {
                charge_stopped.push(player.id);
            }
            if result.bienkhi_finished {
                bienkhi_finished.push(player.id);
            }
            if result.monkey_down {
                monkey_down.push(player.id);
            }
            if result.huyt_sao_expired {
                player.n_point.huyt_sao_buff = 0;
                player.n_point.set_base_point();
                huyt_sao_expired.push(player.id);
            }

            player.n_point.set_base_point();
            if player.n_point.hp_current <= 0 && !player.dead_flag {
                player.dead_flag = true;
            }
        }
    }
    for pid in shield_removed {
        if let Some(player) = PLAYER_MANAGER.get(pid) {
            EffectSkillService::send_effect_player(
                &player,
                &player,
                EffectAction::REMOVE,
                EffectSkillService::SHIELD_EFFECT,
            );
        }
    }

    for pid in charge_stopped {
        if let Some(player) = PLAYER_MANAGER.get(pid) {
            EffectSkillService::send_effect_stop_charge(&player);
        }
    }

    for pid in bienkhi_finished {
        let update_opt = {
            if let Some(mut player) = PLAYER_MANAGER.get_mut(pid) {
                Some(EffectSkillService::set_is_monkey_state(&mut player))
            } else {
                None
            }
        };
        if let Some(update) = update_opt {
            EffectSkillService::send_monkey_messages(&update);
        }
    }

    for pid in monkey_down {
        let update_opt = {
            if let Some(mut player) = PLAYER_MANAGER.get_mut(pid) {
                Some(EffectSkillService::monkey_down_state(&mut player))
            } else {
                None
            }
        };
        if let Some(update) = update_opt {
            EffectSkillService::send_monkey_messages(&update);
        }
    }

    for pid in huyt_sao_expired {
        if let Some(player) = PLAYER_MANAGER.get(pid) {
            EffectSkillService::send_effect_player(
                &player,
                &player,
                EffectAction::REMOVE,
                EffectSkillService::HUYT_SAO_EFFECT,
            );
            let _ = crate::services::player_info_service::send_point_info_sync(&player);
        }
    }
}
