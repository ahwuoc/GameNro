use crate::{entities::item_template::Model as ItemTemplate, item::ItemDao};
use std::sync::LazyLock;
use sea_orm::DatabaseConnection;
use std::sync::RwLock;

static ITEM_TEMPLATES: LazyLock<RwLock<Vec<ItemTemplate>>> = LazyLock::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut itemplates = ItemDao::get_all_item_templates(db).await?;
    itemplates.sort_by_key(|t| t.id);

    match ITEM_TEMPLATES.write() {
        Ok(mut lock) => *lock = itemplates,
        Err(poisoned) => *poisoned.into_inner() = itemplates,
    }
    Ok(())
}

pub fn get(id: i16) -> Option<ItemTemplate> {
    let lock = match ITEM_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&id, |v| v.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<ItemTemplate> {
    match ITEM_TEMPLATES.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
