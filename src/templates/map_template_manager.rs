use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

use crate::entities::map_template::{self, Model as MapTemplate};

pub static MAP_TEMPLATES: Lazy<RwLock<Vec<MapTemplate>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let mut map_templates = map_template::Entity::find().all(pool).await?;
    map_templates.sort_by_key(|t| t.id);

    let mut lock = MAP_TEMPLATES.write().unwrap();
    *lock = map_templates;
    Ok(())
}

pub fn get(id: i32) -> Option<MapTemplate> {
    let lock = MAP_TEMPLATES.read().unwrap();
    lock.binary_search_by_key(&id, |t| t.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<MapTemplate> {
    let lock = MAP_TEMPLATES.read().unwrap();
    lock.clone()
}
