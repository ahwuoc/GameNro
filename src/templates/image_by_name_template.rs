use std::sync::RwLock;

use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};

use crate::entities::img_by_name::{self, Model as ImageByName};

pub static IMAGE_BY_NAME: Lazy<RwLock<Vec<ImageByName>>> = Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let mut rows = img_by_name::Entity::find().all(db).await?;
    rows.sort_by(|a, b| a.name.cmp(&b.name));
    match IMAGE_BY_NAME.write() {
        Ok(mut lock) => *lock = rows,
        Err(poisoned) => *poisoned.into_inner() = rows,
    }
    Ok(())
}

pub fn get(name: &str) -> Option<ImageByName> {
    let lock = match IMAGE_BY_NAME.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&name.to_string(), |v| v.name.clone())
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_n_frame(name: &str) -> i8 {
    get(name).map(|img| img.n_frame as i8).unwrap_or(0) as i8
}
