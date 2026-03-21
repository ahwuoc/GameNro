use crate::player::{player_actor::PlayerMessage, player_manager::PLAYER_MANAGER, Player};
use crate::services::task_service::TaskService;
use crate::services::ServiceHandles;
use crate::utils::time;

#[derive(Debug, Clone, Copy)]
#[repr(u8)]
pub enum TypeTNSM {
    SucManh = 0,
    TiemNang = 1,
    All = 2,
}

pub fn tiemnang_sucmanh_add(pl: &mut Player, type_tnsm: TypeTNSM, mut param: i64, is_ori: bool) {
    if pl.is_pet {
        let mut pet_param = param;
        if pl.charms.td_de_tu > time::current_time_millis() {
            pet_param *= 2;
        }
        pl.n_point.sucmanh_add(pet_param);
        pl.n_point.tiemnang_add(pet_param);
        tracing::debug!(
            "Pet {} received TNSM: param={}, pet_param={}",
            pl.id,
            param,
            pet_param
        );
        if let Some(master_id) = pl.master_id {
            if let Some(master) = PLAYER_MANAGER.get(master_id) {
                let param_master = (param as f64 * 0.5) as i64;
                tracing::debug!(
                    "Forwarding TNSM to master {}: param_master={}",
                    master_id,
                    param_master
                );
                master.send_forget(PlayerMessage::AddTNSM {
                    type_tnsm,
                    param: param_master,
                    is_ori: true,
                });
            } else {
                tracing::warn!("Master {} not found in PLAYER_MANAGER!", master_id);
            }
        } else {
            tracing::warn!("Pet {} has no master_id!", pl.id);
        }
    } else {
        let power = pl.n_point.power;
        let limit = pl.n_point.get_power_limit();

        let curr_time = time::current_time_millis();
        let tn_param = param;
        if pl.charms.td_tri_tue > curr_time {
            param += tn_param;
        }
        if pl.charms.td_tri_tue3 > curr_time {
            param += tn_param * 2;
        }
        if pl.charms.td_tri_tue4 > curr_time {
            param += tn_param * 3;
        }
        let param_scaled = pl.n_point.scale_tiemnang_by_power(param);

        pl.n_point.tiemnang_add(param_scaled);

        if power < limit {
            match type_tnsm {
                TypeTNSM::TiemNang => {}
                TypeTNSM::SucManh | TypeTNSM::All => {
                    pl.n_point.sucmanh_add(param_scaled);
                }
            }
        }
        TaskService::check_done_task_power(pl);
        if let Err(e) = ServiceHandles::send_tnsm(pl, type_tnsm, param_scaled) {
            tracing::error!("Failed to send TNSM to client: {:?}", e);
        }
        if is_ori {
            if pl.clan_id != -1 {
                // TODO: Triển khai cộng TNSM cho bang hội
            }
        }
    }
}
