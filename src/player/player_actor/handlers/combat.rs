use crate::map::zone_manager::ZONE_MANAGER;
use crate::matches::{pvp_manager, TypeLosePvp};
use crate::network::message::Message;
use crate::player::player::Player;
use crate::player::player_actor::pet::message::PetMessage;
use crate::player::player_actor::pet::PetHandle;
use crate::services::effect_skill_service::EffectSkillService;
use crate::services::{player_info_service, ServiceHandles};

pub struct CombatHandler;

impl CombatHandler {
    pub async fn handle_injured(
        player: &mut Player,
        mut damage: u64,
        piercing: bool,
        from_mob: bool,
    ) {
        let was_alive = !player.is_die();
        let curr_time = crate::utils::time::current_time_millis();
        
        if from_mob {
            if player.charms.td_da_trau > curr_time {
                damage /= 2;
            }
            if player.charms.td_bat_tu > curr_time {
                let hp = player.n_point.hp_current as u64;
                if damage >= hp {
                    damage = hp.saturating_sub(1);
                }
            }
        }
        
        let real_damage = player.injured(damage, piercing);
        
        if !from_mob {
            player_info_service::send_info_hp_mp_money(player);
            ServiceHandles::send_player_injured(player, real_damage as i32, false, 255);
            ServiceHandles::send_hp_sync(player);
        }
        
        if was_alive && player.is_die() {
            let pvp_handle = pvp_manager::get_pvp_handle();
            pvp_handle.player_lose(player.id as i64, TypeLosePvp::Dead);
        }
    }

    pub async fn handle_attack_mob(player: &mut Player, mob_id: i32) {
        if player.effect_skill.use_troi {
            Self::release_hold(player);
        }
        
        let zone_opt = ZONE_MANAGER.get_zone(player.map_id, player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(mobs) = zone.get_all_mobs().await {
                if let Some(mob) = mobs.iter().find(|m| m.id == mob_id as u64) {
                    let mut mob_clone = mob.clone();
                    crate::services::skill_service::execute_skill(player, None, Some(&mut mob_clone))
                        .await;
                    zone.mob_effects(mob_clone.id, mob_clone.effect_skill.clone());
                }
            }
        }
    }

    pub async fn handle_attack_player(player: &mut Player, player_id: i32) {
        if player.effect_skill.use_troi {
            Self::release_hold(player);
        }

        let zone_opt = ZONE_MANAGER.get_zone(player.map_id, player.zone_id);

        if let Some(zone) = zone_opt {
            if let Ok(Some(target_handle)) = zone.get_player(player_id as u64).await {
                if let Some(mut target_snapshot) = target_handle.get_snapshot().await {
                    let _ = crate::services::skill_service::execute_skill(
                        player,
                        Some(&mut target_snapshot),
                        None,
                    )
                    .await;
                }
            }
        }
    }

    pub async fn handle_use_skill(player: &mut Player, mut msg: Message) {
        if player.effect_skill.use_troi {
            Self::release_hold(player);
        }

        let status = msg.read_byte().unwrap_or(0);
        let mut pl_target_snapshot = None;
        let mut mob_target = None;

        let zone_opt = ZONE_MANAGER.get_zone(player.map_id, player.zone_id);

        if let Some(zone) = zone_opt.clone() {
            if status == 1 {
                if let Ok(mob_id) = msg.read_byte() {
                    if let Ok(mobs) = zone.get_all_mobs().await {
                        if let Some(m) = mobs.iter().find(|m| m.id == mob_id as u64) {
                            mob_target = Some(m.clone());
                        }
                    }
                }
            } else if status == 2 {
                if let Ok(player_id) = msg.read_int() {
                    if let Ok(Some(handle)) = zone.get_player(player_id as u64).await {
                        pl_target_snapshot = handle.get_snapshot().await;
                    }
                }
            }
        }

        if let Some(mut mob) = mob_target {
            let _ = crate::services::skill_service::execute_skill(player, None, Some(&mut mob)).await;
            if let Some(zone) = zone_opt.clone() {
                zone.mob_effects(mob.id, mob.effect_skill.clone());
            }
        } else if let Some(mut pl_target) = pl_target_snapshot {
            let _ = crate::services::skill_service::execute_skill(player, Some(&mut pl_target), None)
                .await;
        } else {
            let _ = crate::services::skill_service::execute_skill(player, None, None).await;
        }
    }

    pub async fn handle_huyt_sao_buff(player: &mut Player, percent_hp: i32) {
        player.effect_skill.ti_le_hp_huyt_sao = percent_hp;
        player.effect_skill.last_time_huyt_sao = crate::utils::time::current_time_millis();
        player.n_point.huyt_sao_buff = percent_hp;
        player.stats_need_update = true;
        
        let heal_amount = (player.n_point.hp_current as i64 * percent_hp as i64 / 100) as i32;
        player.n_point.current_hp_add(heal_amount);
        
        let _ = player_info_service::send_point_info_sync(player);
        let _ = player_info_service::send_info_hp_mp_money(player);
    }

    pub fn release_hold(player: &mut Player) {
        if let Some(zone) = ZONE_MANAGER.get_zone(player.map_id, player.zone_id) {
            if let Some(mob_id) = player.effect_skill.mob_an_troi_id {
                zone.remove_mob_hold(mob_id, player.id);
            }
            if let Some(target_id) = player.effect_skill.pl_an_troi_id {
                zone.remove_player_hold(target_id, player.id);
            }
        }
        EffectSkillService::remove_use_troi(player);
    }

    pub fn handle_an_troi(
        player: &mut Player,
        is_an_troi: bool,
        time_an_troi: u64,
        caster_id: Option<u64>,
    ) {
        if is_an_troi {
            player.effect_skill.an_troi = true;
            player.effect_skill.time_an_troi = time_an_troi;
            player.effect_skill.start_time_an_troi = crate::utils::time::current_time_millis();
            player.effect_skill.pl_troi_id = caster_id;
        } else {
            EffectSkillService::remove_an_troi(player);
        }
    }
}
