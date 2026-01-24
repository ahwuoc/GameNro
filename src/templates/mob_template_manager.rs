#![allow(dead_code)]
use dashmap::DashMap;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};

use crate::entities::mob_template::{self, Model as MobTemplate};

static MOB_TEMPLATES: Lazy<DashMap<i8, MobTemplate>> = Lazy::new(|| DashMap::new());
pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let mobs = mob_template::Entity::find().all(pool).await?;
    for mob in mobs {
        MOB_TEMPLATES.insert(mob.id as i8, mob);
    }
    Ok(())
}

pub fn get(id: i8) -> Option<MobTemplate> {
    MOB_TEMPLATES.get(&id).map(|kv| kv.value().clone())
}

pub fn get_all() -> Vec<MobTemplate> {
    MOB_TEMPLATES.iter().map(|kv| kv.value().clone()).collect()
}
