use crate::item::inventory::Inventory;
use crate::item::item::Item;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::player::Player;

pub struct InventoryService;

impl InventoryService {
    pub fn find_item_index_in_bag(inventory: &Inventory, target_item: &Item) -> Option<usize> {
        for (index, item) in inventory.items_bag.iter().enumerate() {
            if item.is_not_null_item() && target_item.is_not_null_item() {
                if let (Some(item_id), Some(target_id)) =
                    (item.get_template_id(), target_item.get_template_id())
                {
                    if item_id == target_id && item.quantity == target_item.quantity {
                        return Some(index);
                    }
                }
            }
        }
        None
    }

    pub fn create_item_box_message(pl: &Player) -> anyhow::Result<Message> {
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
    pub fn create_item_bag_message(pl: &Player) -> anyhow::Result<Message> {
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

    pub async fn send_item_bag_to_client(session: &mut AsyncSession) -> anyhow::Result<()> {
        let msg = Self::create_item_bag_message(session.get_player().unwrap())?;
        session.send_message(&msg).await?;
        Ok(())
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
    pub async fn send_item_body_to_client(pl: &mut Player) -> anyhow::Result<()> {
        let mut response = Message::new(-37);
        response.write_byte(0);
        response.write_short(pl.get_head());
        response.write_byte(pl.inventory.items_body.len() as i8);
        for item in &pl.inventory.items_body {
            if item.is_not_null_item() {
                continue;
            };
            response.write_short(item.get_template_id().unwrap_or(1));
            response.write_int(item.quantity);
            response.write_utf(&item.get_description());
            response.write_utf(&item.get_content());
            for option in &item.item_options {
                response.write_byte(option.option_id);
                response.write_short(option.param);
            }
        }
        pl.send_message(response).await?;
        Ok(())
    }
}
