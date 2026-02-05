use crate::entities::pet_template::Model as PetTemplate;
use crate::entities::prelude::PetTemplate as PetTemplateEntity;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static PET_TEMPLATES: Lazy<RwLock<Vec<PetTemplate>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut templates = PetTemplateEntity::find().all(db).await?;
    templates.sort_by_key(|t| t.r#type);

    let mut lock = PET_TEMPLATES.write().unwrap();
    *lock = templates;
    Ok(())
}

pub fn get(pet_type: i32) -> Option<PetTemplate> {
    let lock = PET_TEMPLATES.read().unwrap();
    lock.binary_search_by_key(&pet_type, |v| v.r#type)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<PetTemplate> {
    PET_TEMPLATES.read().unwrap().clone()
}
