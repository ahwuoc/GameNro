use crate::constant::const_map;
use crate::item::InventoryService;
use crate::map::services::change_map_service::ChangeMapService;
use crate::network::session::SessionArc;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Player;
use crate::services::{player_info_service, ServiceHandles};

pub struct UseItemService;

#[derive(Debug)]
pub enum UseItemResult {
    None,
    RecoveredHpMp {
        index: usize,
        hp_ki: i32,
        stamina: i16,
    },
    AddedGold {
        index: usize,
    },
    Error(String),
}

#[derive(Debug, Clone, Copy)]
pub struct PeaRecoveryData {
    pub hp_ki: i32,
    pub stamina: i16,
}

impl UseItemService {
    pub async fn handle_use_item(pl: &mut Player, index: usize) -> anyhow::Result<UseItemResult> {
        let (type_item, item_id) = {
            let item = pl
                .inventory
                .items_bag
                .get(index)
                .ok_or(anyhow::anyhow!("Item not found"))?;
            (item.get_type(), item.get_template_id())
        };

        match type_item {
            76 => {
                let item_id = item_id.ok_or(anyhow::anyhow!("Item template id not found"))?;
                if pl.fusion.type_fusion != 0 {
                    if let Some(ref session) = pl.session {
                        if let Some(handle) = session.get_player_handle().await {
                            handle.send_forget(PlayerMessage::Unfusion);
                        }
                    }
                    return Ok(UseItemResult::None);
                }
                if let Some(template) =
                    crate::templates::fusion_template_manager::get(item_id as i32)
                {
                    if let Some(ref session) = pl.session {
                        if let Some(handle) = session.get_player_handle().await {
                            handle.send_forget(PlayerMessage::Fusion {
                                type_fusion: template.fusion_type,
                                template_id: template.id,
                            });
                        }
                    }
                    return Ok(UseItemResult::None);
                } else {
                    return Ok(UseItemResult::Error(
                        "Không tìm thấy dữ liệu hợp thể cho vật phẩm này".to_string(),
                    ));
                }
            }
            6 => {
                if let Some(recovery) = Self::eat_pea(pl, index) {
                    return Ok(UseItemResult::RecoveredHpMp {
                        index,
                        hp_ki: recovery.hp_ki,
                        stamina: recovery.stamina,
                    });
                }
                return Ok(UseItemResult::None);
            }
            _ => {
                let item_id = item_id.ok_or(anyhow::anyhow!("Item template id not found"))?;
                match item_id {
                    457 => {
                        pl.inventory.add_gold(500_000_000);
                        InventoryService::sub_quantity_item_bag(pl, index, 1);
                        return Ok(UseItemResult::AddedGold { index });
                    }
                    193 => {
                        pl.interaction_state.type_change_map = const_map::CHANGE_CAPSULE;
                        ChangeMapService::open_capsule_menu(pl)?;
                        InventoryService::sub_quantity_item_bag(pl, index, 1);
                        InventoryService::send_item_bag_to_client(pl)?;
                        return Ok(UseItemResult::None);
                    }
                    194 => {
                        pl.interaction_state.type_change_map = const_map::CHANGE_CAPSULE;
                        ChangeMapService::open_capsule_menu(pl)?;
                        return Ok(UseItemResult::None);
                    }
                    _ => {
                        return Ok(UseItemResult::Error(
                            "Không thể sử dụng vật phẩm này".to_string(),
                        ));
                    }
                }
            }
        }
    }

    pub fn eat_pea(pl: &mut Player, index: usize) -> Option<PeaRecoveryData> {
        let now = crate::utils::time::current_time_millis();
        if now - pl.last_time_eat_pea < 1000 {
            return None;
        }

        let (hp_ki_hoiphuc, stamina_pet) = {
            let item = pl.inventory.items_bag.get(index);
            if let Some(it) = item {
                if it.is_not_null_item() && it.get_type() == 6 {
                    let level = it
                        .get_name()
                        .chars()
                        .filter(|c| c.is_ascii_digit())
                        .collect::<String>()
                        .parse::<i16>()
                        .unwrap_or(1);

                    let recover = it
                        .item_options
                        .iter()
                        .find(|op| matches!(op.get_option_id(), 2 | 48))
                        .map(|op| match op.get_option_id() {
                            2 => op.get_param() as i32 * 1000,
                            48 => op.get_param() as i32,
                            _ => unreachable!(),
                        })
                        .unwrap_or(0);
                    (recover, level * 100)
                } else {
                    (0, 0)
                }
            } else {
                (0, 0)
            }
        };

        if hp_ki_hoiphuc > 0 {
            pl.n_point.set_hp(pl.n_point.hp_current + hp_ki_hoiphuc);
            pl.n_point.set_mp(pl.n_point.mp_current + hp_ki_hoiphuc);
            pl.last_time_eat_pea = now;

            InventoryService::sub_quantity_item_bag(pl, index, 1);
            return Some(PeaRecoveryData {
                hp_ki: hp_ki_hoiphuc,
                stamina: stamina_pet,
            });
        }
        None
    }
}
