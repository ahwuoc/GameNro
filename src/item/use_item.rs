use crate::{
    constant::cmd::cmd,
    entities::player,
    item::{type_item_inventory::TypeItemInventory, InventoryService, Item},
    network::{message::Message, session::AsyncSession},
    services::ServiceHandles,
};

pub struct UseItem;

impl UseItem {
    pub async fn get_item(
        session: &mut AsyncSession,
        type_item_inventory: TypeItemInventory,
        index: i8,
    ) -> anyhow::Result<()> {
        if index < 0 {
            return Ok(());
        }

        let messages = {
            let Some(pl) = session.get_player_mut() else {
                return Ok(());
            };

            match type_item_inventory {
                TypeItemInventory::BodyToBag => {
                    let Some(bag_idx) = pl
                        .inventory
                        .items_bag
                        .iter()
                        .position(|it| it.is_null_item())
                    else {
                        return Ok(());
                    };
                    let item = std::mem::take(&mut pl.inventory.items_body[index as usize]);
                    if item.is_null_item() {
                        return Ok(());
                    }
                    pl.inventory.items_bag[bag_idx] = item;
                    let bag_msg = InventoryService::create_item_bag_message(pl)?;
                    let body_msg = InventoryService::create_item_body_to_client(pl)?;
                    vec![bag_msg, body_msg]
                }

                TypeItemInventory::BagToBody => {
                    let bag_item = &pl.inventory.items_bag[index as usize];
                    if bag_item.is_null_item() {
                        return Ok(());
                    }
                    let body_slot = bag_item.get_type();

                    std::mem::swap(
                        &mut pl.inventory.items_bag[index as usize],
                        &mut pl.inventory.items_body[body_slot as usize],
                    );

                    let bag_msg = InventoryService::create_item_bag_message(pl)?;
                    let body_msg = InventoryService::create_item_body_to_client(pl)?;
                    vec![bag_msg, body_msg]
                }
                TypeItemInventory::BagToBox => {
                    let Some(box_idx) = pl
                        .inventory
                        .items_box
                        .iter()
                        .position(|it| it.is_null_item())
                    else {
                        return Ok(());
                    };
                    let it_bag = std::mem::take(&mut pl.inventory.items_bag[index as usize]);
                    if it_bag.is_null_item() {
                        return Ok(());
                    }
                    pl.inventory.items_box[box_idx] = it_bag;

                    let bag_msg = InventoryService::create_item_bag_message(pl)?;
                    let box_msg = InventoryService::create_item_box_message(pl)?;
                    let mut open_box = Message::new(-35);
                    open_box.write_byte(1)?;
                    vec![bag_msg, box_msg, open_box]
                }
                TypeItemInventory::BodyToBox => {
                    let Some(box_idx) = pl
                        .inventory
                        .items_box
                        .iter()
                        .position(|it| it.is_null_item())
                    else {
                        return Ok(());
                    };
                    let it_body = std::mem::take(&mut pl.inventory.items_body[index as usize]);
                    if it_body.is_null_item() {
                        return Ok(());
                    };
                    pl.inventory.items_box[box_idx] = it_body;
                    let box_msg = InventoryService::create_item_box_message(pl)?;
                    let body_msg = InventoryService::create_item_body_to_client(pl)?;
                    vec![box_msg, body_msg]
                }
                TypeItemInventory::BoxToBodyOrBag => {
                    let it_box = std::mem::take(&mut pl.inventory.items_box[index as usize]);
                    let Some(bag_idx) = pl
                        .inventory
                        .items_bag
                        .iter()
                        .position(|it| it.is_null_item())
                    else {
                        return Ok(());
                    };
                    if it_box.is_null_item() {
                        return Ok(());
                    };

                    let it_box_type = it_box.get_type();
                    if it_box.is_item_body()
                        && pl.inventory.items_body[it_box_type as usize].is_null_item()
                    {
                        pl.inventory.items_body[it_box_type as usize] = it_box;
                    } else {
                        pl.inventory.items_bag[bag_idx] = it_box;
                    }
                    let body_msg = InventoryService::create_item_body_to_client(pl)?;
                    let bag_msg = InventoryService::create_item_bag_message(pl)?;
                    let box_msg = InventoryService::create_item_box_message(pl)?;
                    vec![bag_msg, box_msg, body_msg]
                }
                _ => Vec::new(),
            }
        };

        for msg in messages {
            session.send_message(&msg).await?;
        }
        Ok(())
    }
}
