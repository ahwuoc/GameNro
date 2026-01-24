use crate::network::session::SessionArc;
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
        session: &SessionArc,
        type_item_inventory: TypeItemInventory,
        index: i8,
    ) -> anyhow::Result<()> {
        if index < 0 {
            return Ok(());
        }

        let mut messages = Vec::new();

        session
            .modify_player(|pl| {
                let msgs = match type_item_inventory {
                    TypeItemInventory::BodyToBag => {
                        let bag_idx_opt = pl
                            .inventory
                            .items_bag
                            .iter()
                            .position(|it: &Item| it.is_null_item());
                        if let Some(bag_idx) = bag_idx_opt {
                            let item: Item =
                                std::mem::take(&mut pl.inventory.items_body[index as usize]);
                            if !item.is_null_item() {
                                pl.inventory.items_bag[bag_idx] = item;
                                let bag_msg = InventoryService::create_item_bag_message(pl)?;
                                let body_msg = InventoryService::create_item_body_to_client(pl)?;
                                vec![bag_msg, body_msg]
                            } else {
                                Vec::new()
                            }
                        } else {
                            Vec::new()
                        }
                    }
                    TypeItemInventory::BagToBody => {
                        let bag_item = &pl.inventory.items_bag[index as usize];
                        if !bag_item.is_null_item() {
                            let body_slot = bag_item.get_type() as usize;
                            std::mem::swap(
                                &mut pl.inventory.items_bag[index as usize],
                                &mut pl.inventory.items_body[body_slot],
                            );
                            let bag_msg = InventoryService::create_item_bag_message(pl)?;
                            let body_msg = InventoryService::create_item_body_to_client(pl)?;
                            vec![bag_msg, body_msg]
                        } else {
                            Vec::new()
                        }
                    }
                    TypeItemInventory::BagToBox => {
                        let box_idx_opt = pl
                            .inventory
                            .items_box
                            .iter()
                            .position(|it: &Item| it.is_null_item());
                        if let Some(box_idx) = box_idx_opt {
                            let it_bag: Item =
                                std::mem::take(&mut pl.inventory.items_bag[index as usize]);
                            if !it_bag.is_null_item() {
                                pl.inventory.items_box[box_idx] = it_bag;
                                let bag_msg = InventoryService::create_item_bag_message(pl)?;
                                let box_msg = InventoryService::create_item_box_message(pl)?;
                                let mut open_box = Message::new(-35);
                                open_box.write_byte(1)?;
                                vec![bag_msg, box_msg, open_box]
                            } else {
                                Vec::new()
                            }
                        } else {
                            Vec::new()
                        }
                    }
                    TypeItemInventory::BodyToBox => {
                        let box_idx_opt = pl
                            .inventory
                            .items_box
                            .iter()
                            .position(|it: &Item| it.is_null_item());
                        if let Some(box_idx) = box_idx_opt {
                            let it_body: Item =
                                std::mem::take(&mut pl.inventory.items_body[index as usize]);
                            if !it_body.is_null_item() {
                                pl.inventory.items_box[box_idx] = it_body;
                                let box_msg = InventoryService::create_item_box_message(pl)?;
                                let body_msg = InventoryService::create_item_body_to_client(pl)?;
                                vec![box_msg, body_msg]
                            } else {
                                Vec::new()
                            }
                        } else {
                            Vec::new()
                        }
                    }
                    TypeItemInventory::BoxToBodyOrBag => {
                        let it_box: Item =
                            std::mem::take(&mut pl.inventory.items_box[index as usize]);
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
                                    pl.inventory.items_body[it_box_type] = it_box;
                                } else {
                                    pl.inventory.items_bag[bag_idx] = it_box;
                                }
                                let body_msg = InventoryService::create_item_body_to_client(pl)?;
                                let bag_msg = InventoryService::create_item_bag_message(pl)?;
                                let box_msg = InventoryService::create_item_box_message(pl)?;
                                vec![bag_msg, box_msg, body_msg]
                            } else {
                                Vec::new()
                            }
                        } else {
                            Vec::new()
                        }
                    }
                    _ => Vec::new(),
                };
                messages = msgs;
                Ok(())
            })
            .await?;

        for msg in messages {
            session.send_message(&msg).await?;
        }
        Ok(())
    }
}
