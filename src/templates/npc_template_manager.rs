use crate::entities::npc_template::{self, Model as NpcTemplate};
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static NPC_TEMPLATE: Lazy<RwLock<Vec<NpcTemplate>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut templates = npc_template::Entity::find().all(db).await?;
    templates.sort_by_key(|t| t.id);

    match NPC_TEMPLATE.write() {
        Ok(mut lock) => *lock = templates,
        Err(poisoned) => *poisoned.into_inner() = templates,
    }
    Ok(())
}

pub fn get(id: i16) -> Option<NpcTemplate> {
    let lock = match NPC_TEMPLATE.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&(id as i32), |t| t.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<NpcTemplate> {
    match NPC_TEMPLATE.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
