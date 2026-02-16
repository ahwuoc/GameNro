use crate::boss::boss_id::{BOSS_THAN_MEO_KARIN, BOSS_YAJIRO};
use crate::boss::manager::BossManager;
use crate::constant::const_npc::NpcId;
use crate::player::Player;
use crate::services::ServiceHandles;

pub fn get_npc_by_boss_id(boss_id: &str) -> Option<NpcId> {
    match boss_id {
        BOSS_THAN_MEO_KARIN => Some(NpcId::ThanMeoKarin),
        BOSS_YAJIRO => Some(NpcId::ThanMeoKarin),
        _ => None,
    }
}
pub fn call_boss_by_id(pl: &mut Player, boss_id: &str, is_thachdau: bool) -> anyhow::Result<()> {
    if pl.interaction_state.get_has_training_boss() {
        tracing::info!(
            "[TRAINING] skip call_boss_by_id: player={} already has training boss",
            pl.id
        );
        return Ok(());
    }
    tracing::info!(
        "[TRAINING] call_boss_by_id: player={}, boss={}, map={}, zone={}",
        pl.id,
        boss_id,
        pl.map_id,
        pl.zone_id
    );
    let npc_id = get_npc_by_boss_id(boss_id);
    if let Some(npc_id) = npc_id {
        ServiceHandles::send_hidden_npc(pl, npc_id, true)?;
    }
    pl.interaction_state.set_is_thachdau(is_thachdau);
    pl.interaction_state.set_has_training_boss(true);
    BossManager::spawn_boss_async(
        boss_id.to_string(),
        pl.map_id,
        pl.zone_id,
        pl.location.x,
        pl.location.y,
        None,
        -1,
        Vec::new(),
        Some(pl.id),
    );

    Ok(())
}
pub fn get_tnsm_by_level(pl: &Player) -> i64 {
    match pl.level_luyentap {
        0 => 20,
        1 => 40,
        2 => 80,
        3 => 160,
        4 => 320,
        5 => 640,
        _ => 1280,
    }
}
pub fn luyen_tap_end(pl: &mut Player, boss_id: &str) {
    let npc_id = get_npc_by_boss_id(boss_id);
    if let Some(npc_id) = npc_id {
        ServiceHandles::send_hidden_npc(pl, npc_id, false);
    }
}
