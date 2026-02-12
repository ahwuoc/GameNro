use crate::player::{player_actor::PlayerMessage, player_manager::PLAYER_MANAGER, Player};
use crate::services::ServiceHandles;

#[derive(Debug, Clone, Copy)]
#[repr(u8)]
pub enum TypeTNSM {
    SucManh = 0,
    TiemNang = 1,
    All = 2,
}

pub fn tiemnang_sucmanh_add(pl: &mut Player, type_tnsm: TypeTNSM, mut param: i64, is_ori: bool) {
    if pl.is_pet {
        pl.n_point.sucmanh_add(param);
        pl.n_point.tiemnang_add(param);

        tracing::debug!("Pet {} added {} SMTN", pl.name, param);

        if let Some(master_id) = pl.master_id {
            if let Some(master) = PLAYER_MANAGER.get(master_id) {
                let param_master = (param as f64 * 0.5) as i64;
                master.send_forget(PlayerMessage::AddTNSM {
                    type_tnsm,
                    param: param_master,
                    is_ori: true,
                });
            }
        }
    } else {
        let power = pl.n_point.power;
        let limit = pl.n_point.get_power_limit();

        let param_scaled = pl.n_point.scale_tiemnang_by_power(param);
        pl.n_point.tiemnang_add(param_scaled);

        if power < limit {
            match type_tnsm {
                TypeTNSM::TiemNang => {}
                TypeTNSM::SucManh | TypeTNSM::All => {
                    pl.n_point.sucmanh_add(param_scaled);
                }
            }
            tracing::debug!(
                "Player {} added {} (scaled: {}) SMTN. Power: {}/{}",
                pl.name,
                param,
                param_scaled,
                pl.n_point.power,
                limit
            );
        } else {
            tracing::debug!(
                "Player {} reached power limit ({}), add potential only: {} (scaled: {})",
                pl.name,
                limit,
                param,
                param_scaled
            );
        }

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
