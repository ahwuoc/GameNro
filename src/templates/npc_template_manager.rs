use crate::entities::npc_template::{self, Model as NpcTemplate};
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static NPC_TEMPLATE: Lazy<RwLock<Vec<NpcTemplate>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut templates = npc_template::Entity::find().all(db).await?;
    templates.sort_by_key(|t| t.id);

    let mut lock = NPC_TEMPLATE.write().unwrap();
    *lock = templates;
    Ok(())
}

pub fn get(id: i16) -> Option<NpcTemplate> {
    let lock = NPC_TEMPLATE.read().unwrap();
    lock.binary_search_by_key(&(id as i32), |t| t.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<NpcTemplate> {
    let lock = NPC_TEMPLATE.read().unwrap();
    lock.clone()
}
