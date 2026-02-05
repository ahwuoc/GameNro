use crate::entities::boss_template;
use crate::entities::prelude::BossTemplate;
use once_cell::sync::Lazy;
use sea_orm::*;
use std::collections::HashMap;
use std::sync::RwLock;

static BOSS_TEMPLATES: Lazy<RwLock<HashMap<String, boss_template::Model>>> =
    Lazy::new(|| RwLock::new(HashMap::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let items = BossTemplate::find().all(db).await?;
    let mut lock = BOSS_TEMPLATES.write().unwrap();
    lock.clear();
    for item in items {
        lock.insert(item.id.clone(), item);
    }
    tracing::info!("Loaded {} boss templates", lock.len());
    Ok(())
}

pub fn get(id: &str) -> Option<boss_template::Model> {
    let lock = BOSS_TEMPLATES.read().unwrap();
    lock.get(id).cloned()
}

pub fn get_all() -> Vec<boss_template::Model> {
    let lock = BOSS_TEMPLATES.read().unwrap();
    lock.values().cloned().collect()
}
