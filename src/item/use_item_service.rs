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
            _ => {
                let item_id = item_id.ok_or(anyhow::anyhow!("Item template id not found"))?;
                match item_id {
                    457 => {
                        pl.inventory.add_gold(500_000_000);
                        player_info_service::send_point_info_sync(session, pl)?;
                        InventoryService::sub_quantity_item_bag(pl, index, 1);
                        InventoryService::send_item_bag_to_client(session, pl)?;
                        ServiceHandles::send_gold_gem_ruby_to_client(session, pl)?;
                    }
                    _ => {
                        ServiceHandles::send_message_alert(
                            session,
                            "Không thể sử dụng vật phẩm này",
                        )?;
                        return Ok(());
                    }
                }
            }
        }
        Ok(())
    }
}
