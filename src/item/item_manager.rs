use std::clone;

use crate::{entities::item_template::Model as ItemTemplate, item::ItemDao};
use once_cell::sync::Lazy;
use sea_orm::{Database, DatabaseConnection};
use dashmap::{self, DashMap};


static ITEM_TEMPLATES:Lazy<DashMap<i16,ItemTemplate>> = Lazy::new(||DashMap::new());


pub async fn load_item_templates(db:&DatabaseConnection)->anyhow::Result<()>{
    let itemplates = ItemDao::get_all_item_templates(db).await?;
    for itemplate in itemplates{
         ITEM_TEMPLATES.insert(itemplate.id, itemplate);
    }
    Ok(())
}

pub fn get_item_template(id:i16)->Option<ItemTemplate>{
    ITEM_TEMPLATES.get(&id).map(|v|v.clone())
}

pub fn get_all_templates() -> Vec<ItemTemplate> {
    ITEM_TEMPLATES.iter().map(|kv| kv.value().clone()).collect()
}