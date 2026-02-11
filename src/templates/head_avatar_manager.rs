use crate::entities::head_avatar::{self, Model as HeadAvatar};
use std::sync::LazyLock;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static HEAD_AVATARS: LazyLock<RwLock<Vec<HeadAvatar>>> = LazyLock::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut rows = head_avatar::Entity::find().all(db).await?;
    rows.sort_by_key(|r| r.avatar_id);
    match HEAD_AVATARS.write() {
        Ok(mut lock) => *lock = rows,
        Err(poisoned) => *poisoned.into_inner() = rows,
    }
    Ok(())
}

pub fn get(id: i32) -> Option<HeadAvatar> {
    let lock = match HEAD_AVATARS.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&id, |v| v.avatar_id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<HeadAvatar> {
    match HEAD_AVATARS.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
