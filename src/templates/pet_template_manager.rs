use crate::entities::pet_template::Model as PetTemplate;
use crate::entities::prelude::PetTemplate as PetTemplateEntity;
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static PET_TEMPLATES: Lazy<RwLock<Vec<PetTemplate>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut templates = PetTemplateEntity::find().all(db).await?;
    templates.sort_by_key(|t| t.r#type);

    match PET_TEMPLATES.write() {
        Ok(mut lock) => *lock = templates,
        Err(poisoned) => *poisoned.into_inner() = templates,
    }
    Ok(())
}

pub fn get(pet_type: i32) -> Option<PetTemplate> {
    let lock = match PET_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&pet_type, |v| v.r#type)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<PetTemplate> {
    match PET_TEMPLATES.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
