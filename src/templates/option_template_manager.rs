use std::sync::LazyLock;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

use crate::entities::item_option_template::{self, Model as ItemOptionModel};

static ITEM_OPTION_TEMPLATES: LazyLock<RwLock<Vec<ItemOptionModel>>> =
    LazyLock::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut rows = item_option_template::Entity::find().all(db).await?;
    rows.sort_by_key(|r| r.id);
    match ITEM_OPTION_TEMPLATES.write() {
        Ok(mut lock) => *lock = rows,
        Err(poisoned) => *poisoned.into_inner() = rows,
    }
    Ok(())
}

pub fn get(id: i8) -> Option<ItemOptionModel> {
    let lock = match ITEM_OPTION_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&(id as i32), |item| item.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<ItemOptionModel> {
    match ITEM_OPTION_TEMPLATES.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
