use crate::entities::npc_template::{self, Model as NpcTemplate};
use dashmap::DashMap;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use sqlx::any;

static NPC_TEMPLATE: Lazy<DashMap<i16, NpcTemplate>> = Lazy::new(DashMap::new);

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let templates = npc_template::Entity::find().all(db).await?;
    for template in templates {
        NPC_TEMPLATE.insert(template.id as i16, template);
    }
    Ok(())
}
pub fn get(id: i16) -> Option<NpcTemplate> {
    NPC_TEMPLATE.get(&id).map(|v| v.value().clone())
}
pub fn get_all() -> Vec<NpcTemplate> {
    let mut npc: Vec<NpcTemplate> = NPC_TEMPLATE.iter().map(|kv| kv.value().clone()).collect();
    npc.sort_by_key(|it| it.id);
    npc
}
