use std::sync::LazyLock;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

use crate::entities::map_template::{self, Model as MapTemplate};

pub static MAP_TEMPLATES: LazyLock<RwLock<Vec<MapTemplate>>> = LazyLock::new(|| RwLock::new(Vec::new()));

pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let mut map_templates = map_template::Entity::find().all(pool).await?;
    map_templates.sort_by_key(|t| t.id);

    match MAP_TEMPLATES.write() {
        Ok(mut lock) => *lock = map_templates,
        Err(poisoned) => *poisoned.into_inner() = map_templates,
    }
    Ok(())
}

pub fn get(id: i32) -> Option<MapTemplate> {
    let lock = match MAP_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&id, |t| t.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<MapTemplate> {
    match MAP_TEMPLATES.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
