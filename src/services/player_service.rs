use std::time::SystemTime;

use crate::constant::const_map;
use crate::database::DbManager;
use crate::map::map_service;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::player::player_mapper;
use crate::services::ServiceHandles;
use crate::utils::time::current_time_millis;
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

    let can_respawn = if map_service::is_map_black_ball_war(pl.map_id) {
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
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};

pub async fn update_player_tick(player: &mut Player) -> Result<()> {
    let now = current_time_millis();
    let effect_result = player.effect_skill.update(now);

    if effect_result.shield_removed {
        EffectSkillService::send_effect_player(
            player,
            player,
            EffectAction::REMOVE,
            EffectSkillService::SHIELD_EFFECT,
        );
    }

    if let Some(charge_result) = player.update_charging() {
        if charge_result.should_stop {
            player.effect_skill.is_charging = false;
            player.effect_skill.count_charging = 0;
            EffectSkillService::send_effect_stop_charge(player);
        }
    }

    if effect_result.bienkhi_finished {
        let update = EffectSkillService::set_is_monkey_state(player);
        player.n_point.cal_point();
        EffectSkillService::send_monkey_messages(&update);
        let _ = crate::services::player_info_service::send_point_info_sync(player);
    }

    if effect_result.monkey_down {
        let update = EffectSkillService::monkey_down_state(player);
        player.n_point.cal_point();
        EffectSkillService::send_monkey_messages(&update);
        let _ = crate::services::player_info_service::send_point_info_sync(player);
    }

    if effect_result.huyt_sao_expired {
        player.n_point.huyt_sao_buff = 0;
        player.stats_need_update = true;
        EffectSkillService::send_effect_player(
            player,
            player,
            EffectAction::REMOVE,
            EffectSkillService::HUYT_SAO_EFFECT,
        );
        let _ = crate::services::player_info_service::send_point_info_sync(player);
    }

    if effect_result.hold_expired {
        if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
            if let Some(mob_id) = player.effect_skill.mob_an_troi_id {
                zone.remove_mob_hold(mob_id, player.id);
            }
            if let Some(target_id) = player.effect_skill.pl_an_troi_id {
                zone.remove_player_hold(target_id, player.id);
            }
            EffectSkillService::remove_use_troi(player);
        }
    }

    if effect_result.an_troi_expired {
        EffectSkillService::send_effect_player(
            player,
            player,
            EffectAction::REMOVE,
            EffectSkillService::HOLD_EFFECT,
        );
    }
    if player.n_point.hp_current <= 0 && !player.dead_flag {
        player.dead_flag = true;
        if player.effect_skill.is_monkey || player.effect_skill.is_skill_bienkhi {
            player.is_transform = false;
            let update = EffectSkillService::monkey_down_state(player);
            player.n_point.cal_point();
            EffectSkillService::send_monkey_messages(&update);
            let _ = crate::services::player_info_service::send_point_info_sync(player);
            tracing::info!("Player {} transformation reset on death", player.id);
        }
    }

    Ok(())
}
