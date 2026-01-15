use dashmap::DashMap;
use once_cell::sync::Lazy;

use crate::entities::intrinsic::{self, Model as IntrinsicTemplate};
use sea_orm::{DatabaseConnection, EntityTrait};

static INSTRINSIC_TEMPLATES: Lazy<DashMap<i8, IntrinsicTemplate>> = Lazy::new(|| DashMap::new());

pub async fn load(pool: &DatabaseConnection) -> anyhow::Result<()> {
    let intrinsics = intrinsic::Entity::find().all(pool).await?;
    for intrinsic in intrinsics {
        INSTRINSIC_TEMPLATES.insert(intrinsic.id as i8, intrinsic);
    }
    Ok(())
}

pub fn get(id: i8) -> Option<IntrinsicTemplate> {
    INSTRINSIC_TEMPLATES.get(&id).map(|v| v.value().clone())
}

pub fn get_all() -> Vec<IntrinsicTemplate> {
    INSTRINSIC_TEMPLATES
        .iter()
        .map(|kv| kv.value().clone())
        .collect()
}
