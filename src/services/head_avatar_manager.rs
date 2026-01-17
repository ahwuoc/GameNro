use crate::entities::head_avatar::{self, Model as HeadAvatar};
use dashmap::DashMap;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
static HEAD_AVATARS: Lazy<DashMap<i32, HeadAvatar>> = Lazy::new(DashMap::new);

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let rows = head_avatar::Entity::find().all(db).await?;
    for row in rows {
        HEAD_AVATARS.insert(row.avatar_id, row);
    }
    Ok(())
}
pub fn get(id: i32) -> Option<HeadAvatar> {
    HEAD_AVATARS.get(&id).map(|v| v.value().clone())
}
pub fn get_all() -> Vec<HeadAvatar> {
    HEAD_AVATARS.iter().map(|v| v.value().clone()).collect()
}
