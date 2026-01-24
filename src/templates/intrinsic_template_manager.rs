use crate::entities::intrinsic::{self, Model as IntrinsicTemplate};
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static INSTRINSIC_TEMPLATES: Lazy<RwLock<Vec<IntrinsicTemplate>>> =
    Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let mut intrinsics = intrinsic::Entity::find().all(pool).await?;
    intrinsics.sort_by_key(|i| i.id);

    let mut lock = INSTRINSIC_TEMPLATES.write().unwrap();
    *lock = intrinsics;
    Ok(())
}

pub fn get(id: i8) -> Option<IntrinsicTemplate> {
    let lock = INSTRINSIC_TEMPLATES.read().unwrap();
    lock.binary_search_by_key(&(id as i32), |i| i.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<IntrinsicTemplate> {
    let lock = INSTRINSIC_TEMPLATES.read().unwrap();
    lock.clone()
}
