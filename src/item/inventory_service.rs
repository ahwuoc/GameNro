use std::ops::Add;

use sqlx::any;

use crate::item::inventory::{self, Inventory};
use crate::item::item::Item;
use crate::network::message::Message;
use crate::network::session::{self, AsyncSession, SessionArc};
use crate::player::Player;
use crate::services::ServiceHandles;
use crate::{constant, item};

pub struct InventoryService;

impl InventoryService {
    pub fn find_item_bag_with_id(items: &[Item], targert_id: i16) -> Option<&Item> {
        items
            .iter()
            .find(|it| it.is_not_null_item() && it.get_template_id() == Some(targert_id))
    }

    pub fn send_open_box(session: &SessionArc) -> anyhow::Result<()> {
        let mut msg = Message::new(-35);
        msg.write_byte(1)?;
        session.transmit(msg);
        Ok(())
    }
    pub fn create_item_box_to_client(pl: &Player) -> anyhow::Result<Message> {
        let mut msg = Message::new(-35);
        msg.write_byte(0)?;
        msg.write_byte(pl.inventory.items_box.len() as i8);
        for it in pl.inventory.items_box.iter() {
            if it.is_null_item() {
                msg.write_short(-1);
            } else {
                msg.write_short(it.get_template_id().unwrap_or(-1))?;
                msg.write_int(it.quantity)?;
                msg.write_utf(&it.get_description())?;
                msg.write_utf(&it.get_content())?;
                msg.write_byte(it.item_options.len() as i8)?;
                for option in &it.item_options {
                    msg.write_byte(option.option_id)?;
                    msg.write_short(option.param)?;
                }
            }
        }
        return Ok(msg);
    }
    pub async fn add_item_bag(session: &SessionArc, item: Item) {
        // TODO: ngoc rong sao den
        // TODO: ngoc rong namek
        let item_type = item.get_type() as usize;
        let _ = session
            .modify_player(|pl| {
                if item_type == 9 {
                    pl.inventory.add_gold(item.quantity as i64);
                } else if item_type == 10 {
                    pl.inventory.add_gem(item.quantity);
                } else if item_type == 34 {
                    pl.inventory.add_ruby(item.quantity);
                }
                ServiceHandles::send_gold_gem_ruby_to_client(session, pl)
            })
            .await;
        let item_id = item.get_template_id().unwrap_or(-1);
        match item_id {
            517 => {
                session
                    .modify_player(|pl| {
                        if pl.inventory.items_bag.len() < constant::limit::MAX_ITEMS_BAG {
                            ServiceHandles::send_message_alert(
                                session,
                                "Bạn đã mờ thành công thêm 1 ô hành trang",
                            );
                            pl.inventory.items_bag.push(Item::default());
                            return Ok(());
                        } else {
                            ServiceHandles::send_message_alert(
                                session,
                                "Hành trang của bạn đã đầy",
                            );
                            return Ok(());
                        }
                    })
                    .await;
            }
            518 => {
                session
                    .modify_player(|pl| {
                        if pl.inventory.items_box.len() < constant::limit::MAX_ITEMS_BOX {
                            ServiceHandles::send_message_alert(
                                session,
                                "Bạn đã mờ thành công thêm 1 ô hộp",
                            );
                            pl.inventory.items_box.push(Item::default());
                            return Ok(());
                        } else {
                            ServiceHandles::send_message_alert(session, "Hộp của bạn đã đầy");
                            return Ok(());
                        }
                    })
                    .await;
            }
            _ => {
                session
                    .modify_player(|pl| {
                        let success =
                            Self::add_item_to_inventory(&mut pl.inventory.items_bag, item.clone());
                        if success {
                            let _ = ServiceHandles::send_message_alert(
                                session,
                                "Nhận được vật phẩm thành công",
                            );
                            let _ = Self::send_item_bag_to_client(session, pl);
                        } else {
                            let _ = ServiceHandles::send_message_alert(session, "Hành trang đầy");
                        }
                        Ok(())
                    })
                    .await
                    .unwrap_or(());
            }
        }
    }
    pub fn add_item_to_inventory(items: &mut Vec<Item>, mut item_new: Item) -> bool {
        if item_new.get_is_up_to() {
            for it in items.iter_mut() {
                if it.is_null_item() {
                    continue;
                }
                if it.get_template_id() != item_new.get_template_id() {
                    continue;
                }
                if it.quantity >= constant::limit::MAX_ITEM_STACK_SIZE {
                    continue;
                }
                let can_add = constant::limit::MAX_ITEM_STACK_SIZE - it.quantity;
                let add_quantity = item_new.quantity.min(can_add);
                it.quantity += add_quantity;
                item_new.quantity -= add_quantity;
                if item_new.quantity == 0 {
                    return true;
                }
            }
        }

        if item_new.quantity > 0 {
            for item in items.iter_mut() {
                if item.is_null_item() {
                    *item = item_new;
                    return true;
                }
            }
        }
        false
    }

    pub fn send_item_bag_to_client(session: &SessionArc, pl: &Player) -> anyhow::Result<()> {
        let msg = Self::create_item_bag_to_client(pl)?;
        session.transmit(msg);
        Ok(())
    }
    pub fn send_item_body_to_client(session: &SessionArc, pl: &Player) -> anyhow::Result<()> {
        let msg = Self::create_item_body_to_client(pl)?;
        session.transmit(msg);
        Ok(())
    }
    pub fn send_item_box_to_client(session: &SessionArc, pl: &Player) -> anyhow::Result<()> {
        let msg = Self::create_item_box_to_client(pl)?;
        session.transmit(msg);
        Ok(())
    }
    pub fn create_item_bag_to_client(pl: &Player) -> anyhow::Result<Message> {
        let mut msg = Message::new(-36);
        msg.write_byte(0)?;
        msg.write_byte(pl.inventory.items_bag.len() as i8)?;
        for item in &pl.inventory.items_bag {
            if item.is_null_item() {
                msg.write_short(-1);
            } else {
                msg.write_short(item.get_template_id().unwrap_or(-1))?;
                msg.write_int(item.quantity)?;
                msg.write_utf(&item.get_description())?;
                msg.write_utf(&item.get_content())?;
                msg.write_byte(item.item_options.len() as i8)?;
                for option in &item.item_options {
                    msg.write_byte(option.option_id);
                    msg.write_short(option.param);
                }
            }
        }
        Ok(msg)
    }

    pub fn create_item_body_to_client(pl: &Player) -> anyhow::Result<Message> {
        let mut msg = Message::new(-37);
        msg.write_byte(0)?;
        msg.write_short(pl.get_head())?;
        msg.write_byte(pl.inventory.items_body.len() as i8)?;
        for item in &pl.inventory.items_body {
            if item.is_null_item() {
                msg.write_short(-1);
            } else {
                msg.write_short(item.get_template_id().unwrap_or(-1));
                msg.write_int(item.quantity);
                msg.write_utf(&item.get_description())?;
                msg.write_utf(&item.get_content())?;
                msg.write_byte(item.item_options.len() as i8)?;
                for option in &item.item_options {
                    msg.write_byte(option.option_id)?;
                    msg.write_short(option.param)?;
                }
            }
        }
        Ok(msg)
    }
    pub fn send_item_bag(session: &SessionArc, pl: &Player) -> anyhow::Result<()> {
        let msg = Self::create_item_bag_to_client(pl)?;
        session.transmit(msg);
        Ok(())
    }

    pub fn sub_quantity_item_bag(pl: &mut Player, index: usize, sub_quantity: i32) {
        if index >= pl.inventory.items_bag.len() {
            return;
        }

        let item = &mut pl.inventory.items_bag[index];
        if item.is_null_item() {
            return;
        }

        if item.quantity > sub_quantity {
            item.quantity -= sub_quantity;
        } else {
            std::mem::take(item);
        }
    }

    pub fn set_item_body(session: &SessionArc, pl: &mut Player, item: Item) -> anyhow::Result<()> {
        let index_body = match item.get_type() {
            0..=5 => item.get_type() as usize,
            32 => 6,
            23 | 24 => 7,
            _ => {
                ServiceHandles::send_message_alert(session, "Vật phẩm không thể trang bị");
                return Ok(());
            }
        };
        if index_body >= pl.inventory.items_body.len() {
            ServiceHandles::send_message_alert(
                session,
                "Vật phẩm vượt qua giới hạn có thể trang bị",
            );
            return Ok(());
        }

        pl.inventory.items_body[index_body] = item;
        Self::send_item_body_to_client(session, pl)?;
        Ok(())
    }
}
