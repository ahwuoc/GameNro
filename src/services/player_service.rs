use std::time::SystemTime;

use crate::constant::const_map;
use crate::database::DbManager;
use crate::map::map_service;
use crate::map::zone::Zone;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::player::player::Player;
use crate::player::player_mapper;
use crate::services::effect_skill_service::{EffectAction, EffectSkillService};
use crate::services::{player_info_service, ServiceHandles};
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
    pl.n_point.set_full_hp_mp();

    ServiceHandles::send_message_eat_dauthan(pl)?;
    ServiceHandles::send_gold_gem_ruby_to_client(pl)?;
    send_message_hs_char(pl)?;

    Ok(())
}

pub fn send_message_hs_char(player: &Player) -> anyhow::Result<()> {
    if let Some(ref session) = player.session {
        let msg_16 = Message::new(-16);
        session.transmit(msg_16);
    }

    let mut msg_30 = Message::new(-30);
    msg_30.write_byte(15)?;
    msg_30.write_int(player.id as i32)?;
    msg_30.write_int(player.n_point.hp_current as i32)?;
    msg_30.write_int(player.n_point.mp_current as i32)?;
    msg_30.write_short(player.location.x)?;
    msg_30.write_short(player.location.y)?;

    ServiceHandles::send_mess_all_player_in_map(player, msg_30)?;

    player_info_service::send_point_info_sync(player);
    player_info_service::send_message_info_hpmp(player);

    Ok(())
}

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
        player_info_service::send_point_info_sync(player);
        println!("Player {} transformation bien khi", player.id);
    }

    if effect_result.monkey_down {
        let update = EffectSkillService::monkey_down_state(player);
        player.n_point.cal_point();
        EffectSkillService::send_monkey_messages(&update);
        player_info_service::send_point_info_sync(player);
        println!("Player {} transformation reset on death", player.id);
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
        player_info_service::send_point_info_sync(player);
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
    if now - player.n_point.last_time_hoi_phuc >= 30000 {
        player.n_point.last_time_hoi_phuc = now;
        if !player.is_die() {
            let hp_hoi = player.n_point.hp_hoi;
            let mp_hoi = player.n_point.mp_hoi;
            if hp_hoi > 0 || mp_hoi > 0 {
                player.n_point.current_hp_add(hp_hoi);
                player.n_point.current_mp_add(mp_hoi);
                let _ = crate::services::player_info_service::send_info_hp_mp_money(player);
            }
        }
    }

    if now - player.n_point.last_time_hoi_stamina >= 60000 {
        player.n_point.last_time_hoi_stamina = now;
        if player.n_point.stamina < player.n_point.max_stamina {
            player.n_point.current_stamina_add(1);
            if !player.is_boss && !player.is_pet {
                let _ = crate::services::player_info_service::send_current_stamina(player);
            }
        }
    }

    Ok(())
}
