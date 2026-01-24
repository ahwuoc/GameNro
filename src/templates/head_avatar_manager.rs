use crate::entities::head_avatar::{self, Model as HeadAvatar};
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static HEAD_AVATARS: Lazy<RwLock<Vec<HeadAvatar>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut rows = head_avatar::Entity::find().all(db).await?;
    rows.sort_by_key(|r| r.avatar_id);
    let mut lock = HEAD_AVATARS.write().unwrap();
    *lock = rows;
    Ok(())
}
pub fn get(id: i32) -> Option<HeadAvatar> {
    let lock = HEAD_AVATARS.read().unwrap();
    lock.binary_search_by_key(&id, |v| v.avatar_id)
        .ok()
        .map(|idx| lock[idx].clone())
}
pub fn get_all() -> Vec<HeadAvatar> {
    HEAD_AVATARS.read().unwrap().clone()
}
