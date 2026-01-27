use crate::entities::player;
use crate::item::type_item_inventory::TypeItemInventory;
use crate::item::{InventoryService, Item};
use crate::network::session::SessionArc;
use crate::player::Player;
use crate::services::ServiceHandles;

pub struct InventoryTransferService;

impl InventoryTransferService {
    pub fn transfer_item(
        session: &SessionArc,
        pl: &mut Player,
        type_item_inventory: TypeItemInventory,
        index: usize,
    ) -> anyhow::Result<()> {
        match type_item_inventory {
            TypeItemInventory::BodyToBag => {
                let bag_idx_opt = pl
                    .inventory
                    .items_bag
                    .iter()
                    .position(|it: &Item| it.is_null_item());
                if bag_idx_opt.is_none() {
                    ServiceHandles::send_message_alert(session, "Hành trang đã đầy")?;
                    return Ok(());
                }
                if let Some(bag_idx) = bag_idx_opt {
                    let item: Item = std::mem::take(&mut pl.inventory.items_body[index]);
                    if !item.is_null_item() {
                        pl.inventory.items_bag[bag_idx] = item;
                        InventoryService::send_item_bag_to_client(session, pl)?;
                        InventoryService::send_item_body_to_client(session, pl)?;
                    }
                }
            }
            TypeItemInventory::BagToBody => {
                let idx = index as usize;
                let bag_item = &mut pl.inventory.items_bag[idx];
                if bag_item.is_null_item() {
                    return Ok(());
                }
                let body_idx = bag_item.get_type() as usize;
                std::mem::swap(
                    &mut pl.inventory.items_bag[idx],
                    &mut pl.inventory.items_body[body_idx],
                );

                InventoryService::send_item_bag_to_client(session, pl)?;
                InventoryService::send_item_body_to_client(session, pl)?;
            }
            TypeItemInventory::BagToBox => {
                if let Some(box_idx) = pl
                    .inventory
                    .items_box
                    .iter()
                    .position(|it: &Item| it.is_null_item())
                {
                    let it_bag: Item = std::mem::take(&mut pl.inventory.items_bag[index]);
                    if !it_bag.is_null_item() {
                        pl.inventory.items_box[box_idx] = it_bag;
                        InventoryService::send_item_box_to_client(session, pl)?;
                        InventoryService::send_item_bag_to_client(session, pl)?;
                        InventoryService::send_open_box(session)?;
                    }
                } else {
                    ServiceHandles::send_message_alert(session, "Hòm đồ đã đầy")?;
                }
            }
            TypeItemInventory::BodyToBox => {
                if let Some(box_idx) = pl
                    .inventory
                    .items_body
                    .iter()
                    .position(|it| it.is_null_item())
                {
                    let it_body: Item = std::mem::take(&mut pl.inventory.items_body[index]);
                    if it_body.is_not_null_item() {
                        pl.inventory.items_box[box_idx] = it_body;
                        InventoryService::send_item_box_to_client(session, pl)?;
                        InventoryService::send_item_body_to_client(session, pl)?;
                        InventoryService::send_open_box(session)?;
                    }
                } else {
                    ServiceHandles::send_message_alert(session, "Hòm đồ đã đầy")?;
                }
            }
            TypeItemInventory::BoxToBodyOrBag => {
                let it_box: Item = std::mem::take(&mut pl.inventory.items_box[index]);
                let bag_idx_opt = pl
                    .inventory
                    .items_bag
                    .iter()
                    .position(|it: &Item| it.is_null_item());

                if let Some(bag_idx) = bag_idx_opt {
                    if !it_box.is_null_item() {
                        let it_box_type = it_box.get_type() as usize;
                        if it_box.is_item_body()
                            && pl.inventory.items_body[it_box_type].is_null_item()
                        {
                            InventoryService::set_item_body(session, pl, it_box)?;
                            InventoryService::send_item_box_to_client(session, pl)?;
                        } else {
                            pl.inventory.items_bag[bag_idx] = it_box;
                            InventoryService::send_item_bag_to_client(session, pl)?;
                            InventoryService::send_item_box_to_client(session, pl)?;
                        }
                        InventoryService::send_open_box(session)?;
                    }
                }
            }
            _ => {
                println!("Non-transfer action in InventoryTransferService");
            }
        }
        Ok(())
    }
}
