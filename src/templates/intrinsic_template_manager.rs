use crate::entities::intrinsic::{self, Model as IntrinsicTemplate};
use std::sync::LazyLock;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static INSTRINSIC_TEMPLATES: LazyLock<RwLock<Vec<IntrinsicTemplate>>> =
    LazyLock::new(|| RwLock::new(Vec::new()));

pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let mut intrinsics = intrinsic::Entity::find().all(pool).await?;
    intrinsics.sort_by_key(|i| i.id);

    match INSTRINSIC_TEMPLATES.write() {
        Ok(mut lock) => *lock = intrinsics,
        Err(poisoned) => *poisoned.into_inner() = intrinsics,
    }
    Ok(())
}

pub fn get(id: i8) -> Option<IntrinsicTemplate> {
    let lock = match INSTRINSIC_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&(id as i32), |i| i.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<IntrinsicTemplate> {
    match INSTRINSIC_TEMPLATES.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
