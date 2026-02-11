use crate::entities::boss_template;
use crate::entities::prelude::BossTemplate;
use std::sync::LazyLock;
use sea_orm::*;
use std::collections::HashMap;
use std::sync::RwLock;

static BOSS_TEMPLATES: LazyLock<RwLock<HashMap<String, boss_template::Model>>> =
    LazyLock::new(|| RwLock::new(HashMap::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let items = BossTemplate::find().all(db).await?;
    match BOSS_TEMPLATES.write() {
        Ok(mut lock) => {
            lock.clear();
            for item in items {
                lock.insert(item.id.clone(), item);
            }
            tracing::info!("Loaded {} boss templates", lock.len());
        }
        Err(poisoned) => {
            let mut lock = poisoned.into_inner();
            lock.clear();
            for item in items {
                lock.insert(item.id.clone(), item);
            }
            tracing::info!("Loaded {} boss templates (poisoned)", lock.len());
        }
    }
    Ok(())
}

pub fn get(id: &str) -> Option<boss_template::Model> {
    let lock = match BOSS_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.get(id).cloned()
}

pub fn get_all() -> Vec<boss_template::Model> {
    let lock = match BOSS_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.values().cloned().collect()
}
