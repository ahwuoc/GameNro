use crate::{entities::item_template::Model as ItemTemplate, item::ItemDao};
use once_cell::sync::Lazy;
use sea_orm::DatabaseConnection;
use std::sync::RwLock;

static ITEM_TEMPLATES: Lazy<RwLock<Vec<ItemTemplate>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut itemplates = ItemDao::get_all_item_templates(db).await?;
    itemplates.sort_by_key(|t| t.id);

    let mut lock = ITEM_TEMPLATES.write().unwrap();
    *lock = itemplates;
    Ok(())
}

pub fn get(id: i16) -> Option<ItemTemplate> {
    let lock = ITEM_TEMPLATES.read().unwrap();
    lock.binary_search_by_key(&id, |v| v.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<ItemTemplate> {
    ITEM_TEMPLATES.read().unwrap().clone()
}
