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
    pub async fn handle_item_action_actor(
        session: &SessionArc,
        pl: &mut Player,
        type_action: TypeItemAction,
        where_item: i8,
        index: i8,
    ) -> anyhow::Result<()> {
        if index < 0 {
            return Ok(());
        }
        match type_action {
            TypeItemAction::DoThrowItem => {
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
                Self::confirm_popup_to_client(session, type_action as i8, where_item, index, &text);
            }
            TypeItemAction::AcceptThrowItem => {
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
            }
            TypeItemAction::DoUseItem => {
                let item_id = {
                    let item = &pl.inventory.items_bag[index as usize];
                    if item.is_null_item() {
                        return Ok(());
                    }
                    item.get_template_id()
                };

                let use_result = UseItemService::handle_use_item(pl, index as usize)?;

                match use_result {
                    crate::item::use_item_service::UseItemResult::RecoveredHpMp { index: _ } => {
                        player_info_service::send_message_info_hpmp(pl)?;
                        ServiceHandles::send_message_eat_dauthan(pl)?;
                        InventoryService::send_item_bag_to_client(pl)?;
                    }
                    crate::item::use_item_service::UseItemResult::AddedGold { index: _ } => {
                        InventoryService::send_item_bag_to_client(pl)?;
                        ServiceHandles::send_gold_gem_ruby_to_client(pl)?;
                    }
                    crate::item::use_item_service::UseItemResult::Error(msg) => {
                        ServiceHandles::send_message_alert(pl, &msg)?;
                    }
                    crate::item::use_item_service::UseItemResult::None => {}
                }

                // Kích hoạch nhiệm vụ sử dụng vật phẩm
                if let Some(id) = item_id {
                    let _ = crate::services::task_service::TaskService::check_done_task_use_item(
                        pl,
                        &id.to_string(),
                    );
                }
            }
            _ => {}
        }
        Ok(())
    }

    pub async fn handle_get_item_actor(
        session: &SessionArc,
        pl: &mut Player,
        type_item_inventory: TypeItemInventory,
        index: i8,
    ) -> anyhow::Result<()> {
        if index < 0 {
            return Ok(());
        }
        InventoryTransferService::transfer_item(session, pl, type_item_inventory, index as usize)?;
        Ok(())
    }

    pub async fn handle_item_action(
        session: &SessionArc,
        type_action: TypeItemAction,
        where_item: i8,
        index: i8,
    ) -> anyhow::Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(crate::player::player_actor::PlayerMessage::ItemAction {
                type_action,
                where_item,
                index,
            });
        }
        Ok(())
    }

    pub async fn get_item(
        session: &SessionArc,
        type_item_inventory: TypeItemInventory,
        index: i8,
    ) -> anyhow::Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(crate::player::player_actor::PlayerMessage::GetItem {
                type_item_inventory,
                index,
            });
        }
        Ok(())
    }

    pub fn confirm_popup_to_client(
        session: &SessionArc,
        command: i8,
        where_item: i8,
        index: i8,
        text: &str,
    ) {
        let mut msg = Message::new(-91);
        let _ = msg.write_byte(command);
        let _ = msg.write_byte(where_item);
        let _ = msg.write_byte(index);
        let _ = msg.write_utf(text);
        session.transmit(msg);
    }
}
