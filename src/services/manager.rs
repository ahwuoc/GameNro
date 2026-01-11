#![allow(dead_code)]
use crate::database::DbManager;
use crate::item::{item_manager, option_template_manager};
use crate::map::{map_manager, map_template_manager};
use crate::services::head_avatar_manager;
use once_cell::sync::Lazy;
use sea_orm::{
    ConnectionTrait, DatabaseBackend, DatabaseConnection, EntityTrait, QueryResult, Statement,
};
use serde_json::Value as JsonValue;
use std::collections::HashMap;
use std::fs;
use std::io::Write;
use std::sync::{Arc, Mutex};
use tokio::time::Duration;

use crate::entities::intrinsic;
use crate::entities::npc_template;
use crate::entities::skill_template;
use crate::item::item_time_service::ItemTimeService;
use crate::mob::mob_template_manager;
use crate::npc::NpcManager;
use crate::npc::NpcService;
use anyhow::{Ok, Result};

static MANAGER: Lazy<Arc<Mutex<Manager>>> = Lazy::new(|| Arc::new(Mutex::new(Manager::new())));

pub struct Manager {
    pub npc_templates: Vec<npc_template::Model>,
    pub skill_templates: Vec<skill_template::Model>,
    pub intrinsic_templates: Vec<intrinsic::Model>,
    pub npc_templates_by_id: HashMap<i32, npc_template::Model>,
    pub skill_templates_by_id: HashMap<i32, skill_template::Model>,
    pub intrinsic_templates_by_id: HashMap<i32, intrinsic::Model>,
    pub item_time_service: ItemTimeService,
    pub npc_service: NpcService,
    pub npc_manager: NpcManager,
    database: Option<DatabaseConnection>,
}

impl Manager {
    pub fn new() -> Self {
        Manager {
            npc_templates: Vec::new(),
            skill_templates: Vec::new(),
            intrinsic_templates: Vec::new(),

            npc_templates_by_id: HashMap::new(),
            skill_templates_by_id: HashMap::new(),
            intrinsic_templates_by_id: HashMap::new(),
            item_time_service: ItemTimeService::new(),
            npc_service: NpcService::new(),
            npc_manager: NpcManager::new(),
            database: None,
        }
    }

    pub fn get_instance() -> Arc<Mutex<Manager>> {
        Arc::clone(&MANAGER)
    }

    pub async fn init(&mut self) -> anyhow::Result<()> {
        let database = DbManager::get_pool();
        self.database = Some(database.clone());

        item_manager::load(&database).await?;
        map_template_manager::load(&database).await?;
        option_template_manager::load(&database).await?;
        head_avatar_manager::load(&database).await?;
        self.load_npc_templates().await?;
        mob_template_manager::load(&database).await?;
        self.load_skill_templates().await?;
        self.load_intrinsic_templates().await?;
        self.npc_service.init(self.npc_templates.clone());
        if let Err(e) = self.load_part_update_data().await {
            eprintln!("Failed to load part update data: {:?}", e);
        }
        println!("Manager initialized successfully!");
        Ok(())
    }

    pub async fn init_maps_world(&self) -> Result<()> {
        let map_templates = map_template_manager::get_all();
        for template in &map_templates {
            let _ = map_manager::MAP_MANAGER
                .init_and_register_map(template)
                .await;
            let _ = map_manager::MapManager::load_tiles(template.id, template.tile_id as i32);
        }
        println!("Initialized {} maps into world", map_templates.len());
        Ok(())
    }

    pub fn start_map_update_task(&self) {
        std::thread::spawn(move || {
            let runtime = tokio::runtime::Runtime::new().unwrap();
            loop {
                let start = std::time::Instant::now();
                runtime.block_on(async {
                    let _ = map_manager::MAP_MANAGER.update_game_loop().await;
                });
                let elapsed_ms = start.elapsed().as_millis() as u64;
                let sleep_ms = if elapsed_ms >= 1000 {
                    0
                } else {
                    1000 - elapsed_ms
                };
                std::thread::sleep(Duration::from_millis(sleep_ms));
            }
        });
    }
    pub async fn load_part_update_data(&self) -> Result<()> {
        let Some(ref database) = self.database else {
            return Ok(());
        };

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
            let parsed: JsonValue =
                serde_json::from_str(&cleaned).unwrap_or(JsonValue::Array(vec![]));

            let mut details: Vec<PartDetail> = Vec::new();
            if let Some(arr) = parsed.as_array() {
                for elem in arr {
                    // Each elem can be an array or stringified array
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

    async fn load_npc_templates(&mut self) -> anyhow::Result<()> {
        if let Some(ref database) = self.database {
            let npc_templates = npc_template::Entity::find().all(database).await?;

            self.npc_templates = npc_templates.clone();
            for template in npc_templates {
                self.npc_templates_by_id.insert(template.id, template);
            }

            println!("Loaded {} NPC templates", self.npc_templates.len());
        }
        Ok(())
    }
    async fn load_skill_templates(&mut self) -> anyhow::Result<()> {
        if let Some(ref database) = self.database {
            let skill_templates = skill_template::Entity::find().all(database).await?;

            self.skill_templates = skill_templates.clone();
            for template in skill_templates {
                self.skill_templates_by_id.insert(template.id, template);
            }

            println!("Loaded {} skill templates", self.skill_templates.len());
        }
        Ok(())
    }

    async fn load_intrinsic_templates(&mut self) -> anyhow::Result<()> {
        if let Some(ref database) = self.database {
            let intrinsic_templates = intrinsic::Entity::find().all(database).await?;

            self.intrinsic_templates = intrinsic_templates.clone();
            for template in intrinsic_templates {
                self.intrinsic_templates_by_id.insert(template.id, template);
            }

            println!(
                "Loaded {} intrinsic templates",
                self.intrinsic_templates.len()
            );
        }
        Ok(())
    }

    pub fn get_npc_templates(&self) -> &Vec<npc_template::Model> {
        &self.npc_templates
    }
    pub fn get_skill_templates(&self) -> &Vec<skill_template::Model> {
        &self.skill_templates
    }
    pub fn get_intrinsic_templates(&self) -> &Vec<intrinsic::Model> {
        &self.intrinsic_templates
    }
    pub fn get_intrinsic_template_by_id(&self, id: i32) -> Option<&intrinsic::Model> {
        self.intrinsic_templates_by_id.get(&id)
    }

    pub fn get_item_time_service(&self) -> &ItemTimeService {
        &self.item_time_service
    }

    pub fn get_npc_service(&self) -> &NpcService {
        &self.npc_service
    }

    pub fn get_npc_manager(&self) -> &NpcManager {
        &self.npc_manager
    }
}
