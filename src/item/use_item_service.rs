use crate::item::InventoryService;
use crate::network::session::SessionArc;
use crate::player::Player;
use crate::services::{player_info_service, ServiceHandles};

pub struct UseItemService;

impl UseItemService {
    pub fn handle_use_item(
        session: &SessionArc,
        pl: &mut Player,
        index: usize,
    ) -> anyhow::Result<()> {
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
                let Some(item) = pl
                    .inventory
                    .items_bag
                    .iter()
                    .find(|it| it.is_not_null_item() && it.get_type() == 6)
                else {
                    return Ok(());
                };
                let Some(hp_ki_hoiphuc) = item
                    .item_options
                    .iter()
                    .find(|op| matches!(op.get_option_id(), 2 | 48))
                    .map(|op| match op.get_option_id() {
                        2 => op.get_param() * 1000,
                        48 => op.get_param(),
                        _ => unreachable!(),
                    })
                else {
                    return Ok(());
                };
                pl.n_point.set_hp(pl.n_point.hp + hp_ki_hoiphuc as i32);
                pl.n_point.set_mp(pl.n_point.mp + hp_ki_hoiphuc as i32);
                player_info_service::send_message_info_hpmp(pl)?;
                ServiceHandles::send_message_eat_dauthan(pl);
                InventoryService::sub_quantity_item_bag(pl, index, 1);
                InventoryService::send_item_bag_to_client(pl)?;
            }
            _ => {
                let item_id = item_id.ok_or(anyhow::anyhow!("Item template id not found"))?;
                match item_id {
                    457 => {
                        pl.inventory.add_gold(500_000_000);
                        InventoryService::sub_quantity_item_bag(pl, index, 1);
                        InventoryService::send_item_bag_to_client(pl)?;
                        ServiceHandles::send_gold_gem_ruby_to_client(pl)?;
                    }
                    _ => {
                        ServiceHandles::send_message_alert(pl, "Không thể sử dụng vật phẩm này")?;
                        return Ok(());
                    }
                }
            }
        }
        Ok(())
    }
}
