#![allow(dead_code)]
use crate::database::DbManager;
use crate::map::map_manager;
use crate::templates::{
    boss_template_manager, fusion_template_manager, head_avatar_manager, image_by_name_template,
    intrinsic_template_manager, item_template_manager, map_template_manager, mob_template_manager,
    npc_template_manager, option_template_manager, pet_template_manager, power_manager,
    radar_template_manager, skill_template_manager, task_template_manager,
};
use sea_orm::{ConnectionTrait, DatabaseBackend, QueryResult, Statement};
use serde_json::Value as JsonValue;
use std::fs;
use std::io::Write;
use tokio::time::Duration;
use tracing::{error, info, instrument};

use anyhow::Result;

#[instrument]
pub async fn init() -> Result<()> {
    let pool = DbManager::get_pool();

    item_template_manager::load(&pool).await?;
    info!(
        "Loaded {} item templates",
        item_template_manager::get_all().len()
    );

    map_template_manager::load(&pool).await?;
    info!(
        "Loaded {} map templates",
        map_template_manager::get_all().len()
    );

    option_template_manager::load(&pool).await?;
    info!(
        "Loaded {} item option templates",
        option_template_manager::get_all().len()
    );

    head_avatar_manager::load(&pool).await?;
    info!(
        "Loaded {} head avatar templates",
        head_avatar_manager::get_all().len()
    );

    mob_template_manager::load(&pool).await?;
    info!(
        "Loaded {} mob templates",
        mob_template_manager::get_all().len()
    );

    skill_template_manager::load(&pool).await?;
    info!(
        "Loaded {} skill templates",
        skill_template_manager::get_all().len()
    );

    npc_template_manager::load(&pool).await?;
    info!(
        "Loaded {} npc templates",
        npc_template_manager::get_all().len()
    );

    intrinsic_template_manager::load(&pool).await?;
    info!(
        "Loaded {} intrinsic templates",
        intrinsic_template_manager::get_all().len()
    );

    task_template_manager::TASK_TEMPLATE_MANAGER
        .init(&pool)
        .await?;

    pet_template_manager::load(&pool).await?;
    info!(
        "Loaded {} pet templates",
        pet_template_manager::get_all().len()
    );

    boss_template_manager::load(&pool).await?;
    fusion_template_manager::load(&pool).await?;
    radar_template_manager::load(&pool).await?;
    image_by_name_template::load(&pool).await?;
    power_manager::load(&pool).await?;

    crate::clan::clan_manager::CLAN_MANAGER.load_all().await?;

    if let Err(e) = load_part_update_data().await {
        error!("Failed to load part update data: {:?}", e);
    }

    info!("Manager initialized successfully!");
    Ok(())
}

#[instrument]
pub async fn init_maps_world() -> Result<()> {
    let map_templates = map_template_manager::get_all();
    for template in &map_templates {
        map_manager::MAP_MANAGER
            .init_and_register_map(template)
            .await?;
        map_manager::MapManager::load_tiles(template.id, template.tile_id)?;
    }
    info!("Initialized {} maps into world", map_templates.len());
    Ok(())
}

#[instrument]
async fn load_part_update_data() -> Result<()> {
    let database = DbManager::get_pool();

    let stmt = Statement::from_string(
        DatabaseBackend::MySql,
        "SELECT id, type, data FROM part".to_string(),
    );
    let rows: Vec<QueryResult> = database.query_all(stmt).await?;

    struct PartDetail {
        icon_id: i16,
        dx: i8,
        dy: i8,
    }
    struct Part {
        _id: i16,
        part_type: i8,
        details: Vec<PartDetail>,
    }

    let mut parts: Vec<Part> = Vec::new();

    for row in rows {
        let id: i16 = row.try_get("", "id").unwrap_or(0);
        let part_type: i8 = row.try_get("", "type").unwrap_or(0);
        let data_str: String = row.try_get("", "data").unwrap_or_default();
        let cleaned = data_str.replace("\\\"", "");
        let parsed: JsonValue = serde_json::from_str(&cleaned).unwrap_or(JsonValue::Array(vec![]));

        let mut details: Vec<PartDetail> = Vec::new();
        if let Some(arr) = parsed.as_array() {
            for elem in arr {
                let arr_val_opt: Option<JsonValue> = if let Some(a) = elem.as_array() {
                    Some(JsonValue::Array(a.clone()))
                } else if let Some(s) = elem.as_str() {
                    serde_json::from_str::<JsonValue>(s).ok()
                } else {
                    None
                };

                if let Some(JsonValue::Array(pd)) = arr_val_opt {
                    if pd.len() >= 3 {
                        let icon_id = pd[0]
                            .as_i64()
                            .or_else(|| pd[0].as_str().and_then(|s| s.parse::<i64>().ok()))
                            .unwrap_or(0) as i16;
                        let dx = pd[1]
                            .as_i64()
                            .or_else(|| pd[1].as_str().and_then(|s| s.parse::<i64>().ok()))
                            .unwrap_or(0) as i8;
                        let dy = pd[2]
                            .as_i64()
                            .or_else(|| pd[2].as_str().and_then(|s| s.parse::<i64>().ok()))
                            .unwrap_or(0) as i8;
                        details.push(PartDetail { icon_id, dx, dy });
                    }
                }
            }
        }

        parts.push(Part {
            _id: id,
            part_type,
            details,
        });
    }
    let dir = "data/arc/update_data";
    fs::create_dir_all(dir)?;
    let path = format!("{}/part", dir);
    let mut buf: Vec<u8> = Vec::new();
    buf.extend_from_slice(&(parts.len() as u16).to_be_bytes());
    for part in &parts {
        buf.push(part.part_type as u8);
        for d in &part.details {
            buf.extend_from_slice(&(d.icon_id as u16).to_be_bytes());
            buf.push(d.dx as u8);
            buf.push(d.dy as u8);
        }
    }
    let mut file = fs::File::create(&path)?;
    file.write_all(&buf)?;
    file.flush()?;
    info!("Load part thành công ({} parts)", parts.len());
    Ok(())
}
