use crate::entities::prelude::Radar as RadarEntity;
use crate::models::radar::{OptionCard, RadarCardTemplate};
use once_cell::sync::Lazy;
use sea_orm::{DatabaseConnection, EntityTrait};
use serde_json::Value;
use std::sync::RwLock;

static RADAR_TEMPLATES: Lazy<RwLock<Vec<RadarCardTemplate>>> =
    Lazy::new(|| RwLock::new(Vec::new()));

pub async fn load(db: &DatabaseConnection) -> anyhow::Result<()> {
    let models = RadarEntity::find().all(db).await?;
    let mut templates = Vec::new();

    for m in models {
        let mut rd = RadarCardTemplate {
            id: m.id as i16,
            icon_id: m.icon_id.unwrap_or(0) as i16,
            rank: m.rank.unwrap_or(0),
            max: m.max.unwrap_or(0) as i8,
            type_radar: m.r#type.unwrap_or(0) as i8,
            template: m.mob_id.unwrap_or(0) as i16,
            name: m.name.clone().unwrap_or_default(),
            info: m.info.clone().unwrap_or_default(),
            head: -1,
            body: -1,
            leg: -1,
            bag: -1,
            options: Vec::new(),
            require: m.require.unwrap_or(0) as i16,
            require_level: m.require_level.unwrap_or(0) as i16,
            aura_id: m.aura_id.unwrap_or(-1),
        };

        // Parse body parts
        if let Some(body_str) = m.body {
            if let Ok(v) = serde_json::from_str::<Value>(&body_str) {
                if let Some(arr) = v.as_array() {
                    for item in arr {
                        if let Some(obj) = item.as_object() {
                            if let Some(h) = obj.get("head").and_then(|v| v.as_i64()) {
                                rd.head = h as i16;
                            }
                            if let Some(b) = obj.get("body").and_then(|v| v.as_i64()) {
                                rd.body = b as i16;
                            }
                            if let Some(l) = obj.get("leg").and_then(|v| v.as_i64()) {
                                rd.leg = l as i16;
                            }
                            if let Some(bg) = obj.get("bag").and_then(|v| v.as_i64()) {
                                rd.bag = bg as i16;
                            }
                        }
                    }
                }
            }
        }

        // Parse options
        if let Some(options_str) = m.options {
            if let Ok(v) = serde_json::from_str::<Value>(&options_str) {
                if let Some(arr) = v.as_array() {
                    for item in arr {
                        if let Some(obj) = item.as_object() {
                            let id = obj.get("id").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                            let param =
                                obj.get("param").and_then(|v| v.as_i64()).unwrap_or(0) as i32;
                            let active =
                                obj.get("activeCard").and_then(|v| v.as_i64()).unwrap_or(0) as i8;
                            rd.options.push(OptionCard {
                                id,
                                param,
                                active_card: active,
                            });
                        }
                    }
                }
            }
        }

        templates.push(rd);
    }

    templates.sort_by_key(|t| t.id);

    match RADAR_TEMPLATES.write() {
        Ok(mut lock) => *lock = templates,
        Err(poisoned) => *poisoned.into_inner() = templates,
    }
    Ok(())
}

pub fn get(id: i16) -> Option<RadarCardTemplate> {
    let lock = match RADAR_TEMPLATES.read() {
        Ok(guard) => guard,
        Err(poisoned) => poisoned.into_inner(),
    };
    lock.binary_search_by_key(&id, |v| v.id)
        .ok()
        .map(|idx| lock[idx].clone())
}

pub fn get_all() -> Vec<RadarCardTemplate> {
    match RADAR_TEMPLATES.read() {
        Ok(guard) => guard.clone(),
        Err(poisoned) => poisoned.into_inner().clone(),
    }
}
