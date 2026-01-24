use std::{collections::HashMap, sync::Arc};

use dashmap::DashMap;

use once_cell::sync::Lazy;

use sea_orm::{ColumnTrait, EntityTrait, QueryFilter};

use crate::network::session::SessionArc;
use crate::templates::item_template_manager;
use crate::{
    database::DbManager,
    entities::{item_shop, item_shop_option, shop, tab_shop},
    item::{InventoryService, ItemService},
    network::{message::Message, session::AsyncSession},
};

static SHOP_DATA: Lazy<DashMap<String, Arc<ShopData>>> = Lazy::new(DashMap::new);

#[derive(Debug, Clone)]

pub struct ShopData {
    pub shop: shop::Model,

    pub tabs: Vec<ShopTab>,
}

#[derive(Debug, Clone)]

pub struct ShopTab {
    pub tab: tab_shop::Model,

    pub items: Vec<ShopItem>,
}

#[derive(Debug, Clone)]

pub struct ShopItem {
    pub item: item_shop::Model,

    pub options: Vec<item_shop_option::Model>,
}

impl ShopData {
    pub async fn get(tag_name: &str) -> anyhow::Result<Arc<ShopData>> {
        if let Some(entry) = SHOP_DATA.get(tag_name) {
            return Ok(entry.value().clone());
        }

        let data = Self::load_from_db(tag_name).await?;

        let arc_data = Arc::new(data);

        SHOP_DATA.insert(tag_name.to_string(), arc_data.clone());

        Ok(arc_data)
    }

    async fn load_from_db(tag_name: &str) -> anyhow::Result<ShopData> {
        let db = DbManager::get_pool();

        let shop_model = shop::Entity::find()
            .filter(shop::Column::TagName.eq(tag_name))
            .one(db)
            .await?
            .ok_or_else(|| anyhow::anyhow!("Shop not found"))?;

        let tab_shops = tab_shop::Entity::find()
            .filter(tab_shop::Column::ShopId.eq(shop_model.id))
            .all(db)
            .await?;

        if tab_shops.is_empty() {
            return Err(anyhow::anyhow!("Tab shop not found"));
        }

        let tab_ids: Vec<i32> = tab_shops.iter().map(|t| t.id).collect();

        let item_shops = item_shop::Entity::find()
            .filter(item_shop::Column::TabId.is_in(tab_ids))
            .all(db)
            .await?;

        let item_ids: Vec<i32> = item_shops.iter().map(|i| i.id).collect();

        let item_shop_options = item_shop_option::Entity::find()
            .filter(item_shop_option::Column::ItemShopId.is_in(item_ids))
            .all(db)
            .await?;

        let mut option_map: HashMap<i32, Vec<item_shop_option::Model>> = HashMap::new();

        for opt in item_shop_options {
            option_map.entry(opt.item_shop_id).or_default().push(opt);
        }

        let mut item_map: HashMap<i32, Vec<ShopItem>> = HashMap::new();

        for item in item_shops {
            let options = option_map.remove(&item.id).unwrap_or_default();

            item_map
                .entry(item.tab_id)
                .or_default()
                .push(ShopItem { item, options });
        }

        let tabs = tab_shops
            .into_iter()
            .map(|tab| {
                let items = item_map.remove(&tab.id).unwrap_or_default();

                ShopTab { tab, items }
            })
            .collect();

        Ok(ShopData {
            shop: shop_model,

            tabs,
        })
    }
}

pub mod shop_service {
    use super::*;

    pub async fn open_shop(tag_name: &str, session: &SessionArc) -> anyhow::Result<()> {
        let shop_data = ShopData::get(tag_name).await?;

        let shop_type = shop_data.shop.type_shop.unwrap_or(0);

        if let Some(mut pl) = session.take_player().await {
            pl.interaction_state.set_tag_shop(tag_name.to_string());
            session.set_player(pl).await;
        }

        let mut msg = Message::new(-44);

        msg.write_byte(shop_type as i8)?;

        msg.write_byte(shop_data.tabs.len() as i8)?;

        for tab in &shop_data.tabs {
            msg.write_utf(&tab.tab.name.replace("<>", "\n"))?;

            msg.write_byte(tab.items.len() as i8)?;

            for item in &tab.items {
                write_shop_item(&mut msg, item, shop_type)?;
            }
        }

        session.transmit(msg);

        println!("Sent shop data for: {}", tag_name);

        Ok(())
    }

    fn write_shop_item(
        msg: &mut Message,

        shop_item: &ShopItem,

        shop_type: i32,
    ) -> anyhow::Result<()> {
        let item = &shop_item.item;

        msg.write_short(item.temp_id as i16)?;

        match shop_type {
            0 => {
                let cost = item.cost.unwrap_or(0);

                let type_sell = item.type_sell.unwrap_or(0);

                if type_sell == 0 {
                    msg.write_int(cost)?;

                    msg.write_int(0)?;
                } else {
                    msg.write_int(0)?;

                    msg.write_int(cost)?;
                }
            }

            3 => {
                msg.write_short(item.icon_spec.unwrap_or(1) as i16)?;

                msg.write_int(item.cost.unwrap_or(0))?;
            }

            _ => {}
        }

        msg.write_byte(shop_item.options.len() as i8)?;

        for option in &shop_item.options {
            msg.write_byte(option.option_id as i8)?;

            msg.write_short(option.param as i16)?;
        }

        msg.write_byte(item.is_new)?;

        if let Some(template) = item_template_manager::get(item.temp_id as i16) {
            if template.r#type == 5 {
                msg.write_byte(1)?;

                msg.write_short(template.head as i16)?;

                msg.write_short(template.body as i16)?;

                msg.write_short(template.leg as i16)?;

                msg.write_short(-1)?;
            } else {
                msg.write_byte(0)?;
            }
        } else {
            msg.write_byte(0)?;
        }

        Ok(())
    }

    pub async fn take_item_shop(
        session: &SessionArc,

        _type_shop: i8,

        temp_id: i16,
    ) -> anyhow::Result<()> {
        let tag_shop = session
            .get_player()
            .await
            .map(|p| p.interaction_state.get_tag_shop().to_string())
            .ok_or_else(|| anyhow::anyhow!("Player not found"))?;

        let shop_data = ShopData::get(&tag_shop).await?;

        let shop_item = shop_data
            .tabs
            .iter()
            .flat_map(|tab| tab.items.iter())
            .find(|it| it.item.temp_id == temp_id as i32)
            .ok_or_else(|| anyhow::anyhow!("Shop item not found"))?;

        if let Some(mut player) = session.take_player().await {
            if let Some(idx_bag) = player
                .inventory
                .items_bag
                .iter()
                .position(|it: &crate::item::item::Item| it.is_null_item())
            {
                if let Some(mut new_item) =
                    ItemService::create_new_item(shop_item.item.temp_id as i16)
                {
                    for opt in &shop_item.options {
                        new_item.add_option_param(opt.option_id as i8, opt.param as i16);
                    }

                    player.inventory.items_bag[idx_bag] = new_item;

                    // Note: inventory bag message requires &Player, but we modified player in place
                    // So we can send it directly using the modified player object if we refactor InventoryService to take &Player
                    // or use the static method with the modified player
                    if let Ok(msg) = InventoryService::create_item_bag_to_client(&player) {
                        session.transmit(msg);
                    }

                    println!("mua thanh cong {}", temp_id);
                }
            }
            session.set_player(player).await;
        } else {
            return Err(anyhow::anyhow!("Player not found"));
        }

        Ok(())
    }
}
