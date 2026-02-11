use crate::entities::fusion_template::Entity as FusionTemplateEntity;
use crate::models::fusion::{FusionAvatarData, FusionTemplate};
use std::sync::LazyLock;
use sea_orm::{DatabaseConnection, EntityTrait};
use std::collections::HashMap;
use std::sync::RwLock;

static FUSION_TEMPLATES: LazyLock<RwLock<HashMap<i32, FusionTemplate>>> =
    LazyLock::new(|| RwLock::new(HashMap::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let templates = FusionTemplateEntity::find().all(db).await?;
    let mut map = HashMap::new();

    for t in templates {
        let fusion_template = FusionTemplate {
            id: t.id,
            name: t.name,
            fusion_type: t.fusion_type,
            data_0: t.data_0.and_then(|s| serde_json::from_str(&s).ok()),
            data_1: t.data_1.and_then(|s| serde_json::from_str(&s).ok()),
            data_2: t.data_2.and_then(|s| serde_json::from_str(&s).ok()),
            hp_percent: t.hp_percent,
            mp_percent: t.mp_percent,
            dame_percent: t.dame_percent,
            crit_bonus: t.crit_bonus,
            is_permanent: t.is_permanent,
        };
        map.insert(t.id, fusion_template);
    }

    match FUSION_TEMPLATES.write() {
        Ok(mut lock) => *lock = map,
        Err(poisoned) => *poisoned.into_inner() = map,
    }
    Ok(())
}

pub fn get(id: i32) -> Option<FusionTemplate> {
    let lock = match FUSION_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.get(&id).cloned()
}
