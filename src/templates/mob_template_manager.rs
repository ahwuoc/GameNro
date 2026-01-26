#![allow(dead_code)]
use std::{collections::HashMap, hash::Hash, sync::RwLock};

use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};

use crate::{
    entities::mob_template::{self, Model as MobTemplate},
    mob,
};

static MOB_TEMPLATES: Lazy<RwLock<HashMap<i8, MobTemplate>>> =
    Lazy::new(|| RwLock::new(HashMap::new()));
pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let mobs = mob_template::Entity::find().all(pool).await?;
    let mut map = MOB_TEMPLATES.write().unwrap();
    for mob in mobs {
        map.insert(mob.id as i8, mob);
    }
    Ok(())
}
pub fn get(id: i8) -> Option<MobTemplate> {
    let map = MOB_TEMPLATES.read().unwrap();
    map.get(&id).cloned()
}
pub fn get_all() -> Vec<MobTemplate> {
    let map = MOB_TEMPLATES.read().unwrap();
    let mut mobs: Vec<MobTemplate> = map.values().cloned().collect();
    mobs.sort_by(|a, b| a.id.cmp(&b.id));
    mobs
}
