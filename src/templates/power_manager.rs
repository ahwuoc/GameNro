use crate::entities::power_caption::{self, Model as PowerCaption};
use crate::entities::power_limit::{self, Model as PowerLimit};
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::sync::RwLock;

static POWER_LIMITS: Lazy<RwLock<Vec<PowerLimit>>> = Lazy::new(|| RwLock::new(Vec::new()));
static POWER_CAPTIONS: Lazy<RwLock<Vec<PowerCaption>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    // Load Power Limits
    let mut limits = power_limit::Entity::find().all(db).await?;
    limits.sort_by_key(|t| t.id);

    match POWER_LIMITS.write() {
        Ok(mut lock) => *lock = limits,
        Err(poisoned) => *poisoned.into_inner() = limits,
    }

    // Load Power Captions
    let mut captions = power_caption::Entity::find().all(db).await?;
    captions.sort_by_key(|t| t.power_required);

    match POWER_CAPTIONS.write() {
        Ok(mut lock) => *lock = captions,
        Err(poisoned) => *poisoned.into_inner() = captions,
    }

    Ok(())
}

pub fn get_limit(id: i32) -> Option<PowerLimit> {
    let lock = match POWER_LIMITS.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.iter().find(|l| l.id == id).cloned()
}

pub fn get_caption(power: i64) -> String {
    let lock = match POWER_CAPTIONS.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.iter()
        .filter(|c| power >= c.power_required)
        .last()
        .map(|c| c.name.clone())
        .unwrap_or_else(|| "Tân thủ".to_string())
}

pub fn get_all_captions() -> Vec<PowerCaption> {
    let lock = match POWER_CAPTIONS.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.clone()
}
