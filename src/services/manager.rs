#![allow(dead_code)]
use crate::database::DbManager;
use crate::map::map_manager;
use crate::templates::{
    head_avatar_manager, intrinsic_template_manager, item_template_manager, map_template_manager,
    mob_template_manager, npc_template_manager, option_template_manager, skill_template_manager,
    task_template_manager,
};
use sea_orm::{ConnectionTrait, DatabaseBackend, QueryResult, Statement};
use serde_json::Value as JsonValue;
use std::fs;
use std::io::Write;
use tokio::time::Duration;

use anyhow::Result;

/// Initialize all template managers by loading data from database
pub async fn init() -> Result<()> {
    let pool = DbManager::get_pool();

    item_template_manager::load(&pool).await?;
    map_template_manager::load(&pool).await?;
    option_template_manager::load(&pool).await?;
    head_avatar_manager::load(&pool).await?;
    mob_template_manager::load(&pool).await?;
    skill_template_manager::load(&pool).await?;
    npc_template_manager::load(&pool).await?;
    intrinsic_template_manager::load(&pool).await?;
    task_template_manager::TASK_TEMPLATE_MANAGER
        .init(&pool)
        .await?;

    if let Err(e) = load_part_update_data().await {
        eprintln!("Failed to load part update data: {:?}", e);
    }

    println!("Manager initialized successfully!");
    Ok(())
}

/// Initialize all maps in the world
pub async fn init_maps_world() -> Result<()> {
    let map_templates = map_template_manager::get_all();
    for template in &map_templates {
        let _ = map_manager::MAP_MANAGER
            .init_and_register_map(template)
            .await;
        let _ = map_manager::MapManager::load_tiles(template.id, template.tile_id);
    }
    println!("Initialized {} maps into world", map_templates.len());
    Ok(())
}

/// Start the background task that updates all maps every second
pub fn start_map_update_task() {
    tokio::spawn(async move {
        let mut interval = tokio::time::interval(Duration::from_millis(1000));
        interval.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Skip);
        loop {
            interval.tick().await;
            if let Err(e) = map_manager::MAP_MANAGER.update_game_loop().await {
                println!("Failed to update map: {:?}", e);
            }
        }
    });
}

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
    let dir = "data/girlkun/update_data";
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
    println!("Load part thành công ({} parts)", parts.len());
    Ok(())
}
