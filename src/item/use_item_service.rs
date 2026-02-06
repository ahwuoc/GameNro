use crate::item::InventoryService;
use crate::network::session::SessionArc;
use crate::player::Player;
use crate::services::{player_info_service, ServiceHandles};

pub struct UseItemService;

#[derive(Debug)]
pub enum UseItemResult {
    None,
    RecoveredHpMp { index: usize },
    AddedGold { index: usize },
    Error(String),
}

impl UseItemService {
    pub fn handle_use_item(pl: &mut Player, index: usize) -> anyhow::Result<UseItemResult> {
        let (type_item, item_id) = {
            let item = pl
                .inventory
                .items_bag
                .get(index)
                .ok_or(anyhow::anyhow!("Item not found"))?;
            (item.get_type(), item.get_template_id())
        };

        match type_item {
            6 => {
                if Self::eat_pea(pl, index) {
                    return Ok(UseItemResult::RecoveredHpMp { index });
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
                    _ => {
                        return Ok(UseItemResult::Error(
                            "Không thể sử dụng vật phẩm này".to_string(),
                        ));
                    }
                }
            }
        }
    }

    pub fn eat_pea(pl: &mut Player, index: usize) -> bool {
        let hp_ki_hoiphuc = {
            let item = pl.inventory.items_bag.get(index);
            if let Some(it) = item {
                if it.is_not_null_item() && it.get_type() == 6 {
                    it.item_options
                        .iter()
                        .find(|op| matches!(op.get_option_id(), 2 | 48))
                        .map(|op| match op.get_option_id() {
                            2 => op.get_param() as i32 * 1000,
                            48 => op.get_param() as i32,
                            _ => unreachable!(),
                        })
                } else {
                    None
                }
            } else {
                None
            }
        };

        if let Some(recovered) = hp_ki_hoiphuc {
            pl.n_point.set_hp(pl.n_point.hp_current + recovered);
            pl.n_point.set_mp(pl.n_point.mp_current + recovered);
            pl.n_point.stamina = pl.n_point.max_stamina; // Full stamina

            InventoryService::sub_quantity_item_bag(pl, index, 1);
            return true;
        }
        false
    }
}
