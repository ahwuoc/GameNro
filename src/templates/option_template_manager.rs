use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

use crate::entities::item_option_template::{self, Model as ItemOptionModel};

static ITEM_OPTION_TEMPLATES: Lazy<RwLock<Vec<ItemOptionModel>>> =
    Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut rows = item_option_template::Entity::find().all(db).await?;
    rows.sort_by_key(|r| r.id);
    let mut lock = ITEM_OPTION_TEMPLATES.write().unwrap();
    *lock = rows;
    Ok(())
}

pub fn get(id: i8) -> Option<ItemOptionModel> {
    let lock = ITEM_OPTION_TEMPLATES.read().unwrap();
    lock.binary_search_by_key(&(id as i32), |item| item.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<ItemOptionModel> {
    let lock = ITEM_OPTION_TEMPLATES.read().unwrap();
    lock.clone()
}
