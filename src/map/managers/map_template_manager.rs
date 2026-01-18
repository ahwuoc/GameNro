use dashmap::DashMap;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};

use crate::entities::map_template::{self, Model as MapTemplate};

pub static MAP_TEMPLATES: Lazy<DashMap<i32, MapTemplate>> = Lazy::new(|| DashMap::new());

pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let map_templates = map_template::Entity::find().all(pool).await?;
    for map_template in map_templates {
        MAP_TEMPLATES.insert(map_template.id, map_template);
    }
    Ok(())
}

pub fn get(id: i32) -> Option<MapTemplate> {
    MAP_TEMPLATES.get(&id).map(|entry| entry.value().clone())
}

pub fn get_all() -> Vec<MapTemplate> {
    MAP_TEMPLATES
        .iter()
        .map(|entry| entry.value().clone())
        .collect()
}
