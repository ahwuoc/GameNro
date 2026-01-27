use crate::item::inventory_transfer_service::InventoryTransferService;
use crate::item::type_item_inventory::{TypeItemAction, TypeItemInventory};
use crate::item::use_item_service::UseItemService;
use crate::network::session::{self, SessionArc};

use crate::player::Player;
use crate::{
    constant::cmd::cmd,
    item::{InventoryService, Item},
    network::{message::Message, session::AsyncSession},
    services::{player_info_service, ServiceHandles},
};

pub struct ItemController;

impl ItemController {
    pub async fn handle_item_action(
        session: &SessionArc,
        type_action: TypeItemAction,
        where_item: i8,
        index: i8,
    ) -> anyhow::Result<()> {
        if index < 0 {
            return Ok(());
        }
        match type_action {
            TypeItemAction::DoThrowItem => {
                session
                    .modify_player(|pl| {
                        let item = if where_item == 0 {
                            &mut pl.inventory.items_body[index as usize]
                        } else {
                            &mut pl.inventory.items_bag[index as usize]
                        };
                        if item.is_null_item() {
                            return Ok(());
                        }

                        let text = format!(
                            "Bạn có chắc chắn muốn vứt '{}' này không ?",
                            item.get_name()
                        );
                        Self::confirm_popup_to_client(
                            session,
                            type_action as i8,
                            where_item,
                            index,
                            &text,
                        );
                        Ok(())
                    })
                    .await?
            }
            TypeItemAction::AcceptThrowItem => {
                session
                    .modify_player(|pl| {
                        let item = if where_item == 0 {
                            &mut pl.inventory.items_body[index as usize]
                        } else {
                            &mut pl.inventory.items_bag[index as usize]
                        };
                        if item.is_null_item() {
                            return Ok(());
                        }
                        if item.get_template_id() == Some(457) {
                            ServiceHandles::send_message_alert(pl, "Bạn không thể bỏ vật phẩm này");
                            return Ok(());
                        }
                        std::mem::take(item);
                        InventoryService::send_item_bag_to_client(pl)?;
                        InventoryService::send_item_body_to_client(pl)?;
                        Ok(())
                    })
                    .await?;
            }
            TypeItemAction::DoUseItem => {
                session
                    .modify_player(|pl| {
                        let item = &mut pl.inventory.items_bag[index as usize];
                        if item.is_null_item() {
                            return Ok(());
                        }
                        UseItemService::handle_use_item(session, pl, index as usize)?;
                        Ok(())
                    })
                    .await?;
            }

            _ => {
                println!("type_action: ");
                return Ok(());
            }
        }
        Ok(())
    }

    fn confirm_popup_to_client(
        session: &SessionArc,
        type_action: i8,
        where_item: i8,
        index: i8,
        text: &str,
    ) {
        let mut msg = Message::new(-43);
        msg.write_byte(type_action);
        msg.write_byte(where_item);
        msg.write_byte(index);
        msg.write_utf(text);
        session.transmit(msg);
    }

    pub async fn get_item(
        session: &SessionArc,
        type_item_inventory: TypeItemInventory,
        index: i8,
    ) -> anyhow::Result<()> {
        if index < 0 {
            return Ok(());
        }
        session
            .modify_player(|pl| {
                InventoryTransferService::transfer_item(
                    session,
                    pl,
                    type_item_inventory,
                    index as usize,
                )?;
                Ok(())
            })
            .await?;

        Ok(())
    }
}
