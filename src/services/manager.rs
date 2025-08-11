use crate::item::ItemManager;
use crate::map::MapDao;
use crate::utils::Database;
use crate::map::map_manager::MAP_MANAGER;
use crate::item::item_dao::ItemDao;
use crate::mob::mob_dao::MobDao;
use tokio::time::{sleep, Duration};
use sea_orm::{EntityTrait, DatabaseBackend, Statement, TryGetable, QueryResult, ConnectionTrait};
use serde_json::Value as JsonValue;
use std::fs;
use std::io::Write;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;

use crate::entities::{item_template, item_option_template, map_template};
use crate::entities::npc_template;
use crate::entities::mob_template;
use crate::entities::skill_template;
use crate::entities::intrinsic;
use crate::mob::MobService;
use crate::item::item_time_service::ItemTimeService;
use crate::npc::NpcService;
use crate::npc::NpcManager;

static MANAGER: Lazy<Arc<Mutex<Manager>>> = Lazy::new(|| {
    Arc::new(Mutex::new(Manager::new()))
});

pub struct Manager {
    pub map_templates: Vec<map_template::Model>,
    pub npc_templates: Vec<npc_template::Model>,
    pub mob_templates: Vec<mob_template::Model>,
    pub skill_templates: Vec<skill_template::Model>,
    pub intrinsic_templates: Vec<intrinsic::Model>,
    pub item_templates:Vec<item_template::Model>,
    pub item_option_templates: Vec<item_option_template::Model>,
    pub item_templates_by_id: HashMap<i32, item_template::Model>,
    pub map_templates_by_id: HashMap<i32, map_template::Model>,
    pub npc_templates_by_id: HashMap<i32, npc_template::Model>,
    pub mob_templates_by_id: HashMap<i32, mob_template::Model>,
    pub skill_templates_by_id: HashMap<i32, skill_template::Model>,
    pub intrinsic_templates_by_id: HashMap<i32, intrinsic::Model>,
    pub item_option_templates_by_id: HashMap<i32, item_option_template::Model>,
    pub map_waypoints: HashMap<i32, Vec<crate::map::map::WayPoint>>, 
    pub map_mobs: HashMap<i32, Vec<(i32, i32, i32, i32, i32)>>, 
    pub map_npcs: HashMap<i32, Vec<(i32, i16, i16)>>, 
    pub mob_service: MobService,
    pub item_time_service: ItemTimeService,
    pub npc_service: NpcService,
    pub npc_manager: NpcManager,
    database: Option<Database>,
}

impl Manager {
    pub fn new() -> Self {
        Manager {
            map_templates: Vec::new(),
            npc_templates: Vec::new(),
            mob_templates: Vec::new(),
            item_templates:Vec::new(),
            item_option_templates: Vec::new(),
            item_templates_by_id: HashMap::new(),
            skill_templates: Vec::new(),
            intrinsic_templates: Vec::new(),
            map_templates_by_id: HashMap::new(),
            item_option_templates_by_id: HashMap::new(),
            npc_templates_by_id: HashMap::new(),
            mob_templates_by_id: HashMap::new(),
            skill_templates_by_id: HashMap::new(),
            intrinsic_templates_by_id: HashMap::new(),
            map_waypoints: HashMap::new(),
            map_mobs: HashMap::new(),
            map_npcs: HashMap::new(),
            mob_service: MobService::new(),
            item_time_service: ItemTimeService::new(),
            npc_service: NpcService::new(),
            npc_manager: NpcManager::new(),
            database: None,
        }
    }

    pub fn get_instance() -> Arc<Mutex<Manager>> {
        Arc::clone(&MANAGER)
    }

    pub async fn init(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        let database = Database::new().await?;
        self.database = Some(database.clone());
        self.mob_service.set_database(database.connection);
        
        self.load_item_templates().await?;
        self.load_map_templates().await?;
        self.load_npc_templates().await?;
        self.load_mob_templates().await?;
        self.load_skill_templates().await?;
        self.load_intrinsic_templates().await?;
        self.npc_service.init(self.npc_templates.clone());
        if let Err(e) = self.load_part_update_data().await {
            eprintln!("Failed to load part update data: {:?}", e);
        }
        println!("Manager initialized successfully!");
        Ok(())
    }

    pub async fn init_maps_world(&self) -> Result<(), Box<dyn std::error::Error>> {
        for template in &self.map_templates {
            {
                let mgr = MAP_MANAGER.write().await;
                mgr.create_map(template).await?;
                mgr.load_tiles_for_map(template.id, template.tile_id as i32).await?;
            }

            if let Some(wps) = self.map_waypoints.get(&template.id) {
                if let Some(map) = MAP_MANAGER.read().await.get_map(template.id).await {
                    for wp in wps {
                        map.add_waypoint(wp.clone()).await?;
                    }
                }
            }

            if let Some(map) = MAP_MANAGER.read().await.get_map(template.id).await {
                let specs = self.map_mobs.get(&template.id).cloned().unwrap_or_default();
                map.init_mobs(&self.mob_templates_by_id, &specs).await?;
            }

            if let Some(map) = MAP_MANAGER.read().await.get_map(template.id).await {
                if let Some(nv_list) = self.map_npcs.get(&template.id) {
                    let mut npc_ids: Vec<i32> = Vec::with_capacity(nv_list.len());
                    let mut npc_x: Vec<i16> = Vec::with_capacity(nv_list.len());
                    let mut npc_y: Vec<i16> = Vec::with_capacity(nv_list.len());
                    for (id, x, y) in nv_list {
                        npc_ids.push(*id);
                        npc_x.push(*x);
                        npc_y.push(*y);
                    }
                    map.init_npcs(&npc_ids, &npc_x, &npc_y).await?;
                }
            }
        }

        println!("Initialized {} maps into world", self.map_templates.len());
        Ok(())
    }

    pub fn start_map_update_task(&self) {
        tokio::spawn(async move {
            loop {
                let start = std::time::Instant::now();
                {
                    let mgr = MAP_MANAGER.read().await;
                    let _ = mgr.update_all_maps().await;
                }
                let elapsed_ms = start.elapsed().as_millis() as u64;
                let sleep_ms = if elapsed_ms >= 1000 { 0 } else { 1000 - elapsed_ms };
                sleep(Duration::from_millis(sleep_ms)).await;
            }
        });
    }
    pub async fn load_part_update_data(&self) -> Result<(), Box<dyn std::error::Error>> {
        let Some(ref database) = self.database else { return Ok(()); };

        let stmt = Statement::from_string(DatabaseBackend::MySql, "SELECT id, type, data FROM part".to_string());
        let rows: Vec<QueryResult> = database.connection.query_all(stmt).await?;

        struct PartDetail { icon_id: i16, dx: i8, dy: i8 }
        struct Part { _id: i16, part_type: i8, details: Vec<PartDetail> }

        let mut parts: Vec<Part> = Vec::new();

        for row in rows {
            let id: i16 = row.try_get("", "id").unwrap_or(0);
            let part_type: i8 = row.try_get("", "type").unwrap_or(0);
            let data_str: String = row.try_get("", "data").unwrap_or_default();

            // Clean escapes similar to Java replaceAll("\\\"", "")
            let cleaned = data_str.replace("\\\"", "");
            let parsed: JsonValue = serde_json::from_str(&cleaned).unwrap_or(JsonValue::Array(vec![]));

            let mut details: Vec<PartDetail> = Vec::new();
            if let Some(arr) = parsed.as_array() {
                for elem in arr {
                    // Each elem can be an array or stringified array
                    let arr_val_opt: Option<JsonValue> = if let Some(a) = elem.as_array() {
                        Some(JsonValue::Array(a.clone()))
                    } else if let Some(s) = elem.as_str() {
                        serde_json::from_str::<JsonValue>(s).ok()
                    } else { None };

                    if let Some(JsonValue::Array(pd)) = arr_val_opt {
                        if pd.len() >= 3 {
                            let icon_id = pd[0].as_i64()
                                .or_else(|| pd[0].as_str().and_then(|s| s.parse::<i64>().ok()))
                                .unwrap_or(0) as i16;
                            let dx = pd[1].as_i64()
                                .or_else(|| pd[1].as_str().and_then(|s| s.parse::<i64>().ok()))
                                .unwrap_or(0) as i8;
                            let dy = pd[2].as_i64()
                                .or_else(|| pd[2].as_str().and_then(|s| s.parse::<i64>().ok()))
                                .unwrap_or(0) as i8;
                            details.push(PartDetail { icon_id, dx, dy });
                        }
                    }
                }
            }

            parts.push(Part { _id: id, part_type, details });
        }

        // Serialize to file
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

    async fn load_map_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let map_templates = map_template::Entity::find().all(&database.connection).await?;
            self.map_templates = map_templates.clone();
            for template in map_templates {
                self.map_templates_by_id.insert(template.id, template.clone());
                let waypoints_data = MapDao::load_map_waypoints(&database.connection, template.id).await?;
                let mut waypoints = Vec::new();
                for wp in waypoints_data {
                    let map_wp = crate::map::map::WayPoint::new(
                        wp.min_x, wp.min_y, wp.max_x, wp.max_y,
                        wp.is_enter, wp.is_offline, wp.name,
                        wp.go_map, wp.go_x, wp.go_y
                    );
                    waypoints.push(map_wp);
                }
                self.map_waypoints.insert(template.id, waypoints);

                // Load mobs using MapDao
                let mobs = MapDao::load_map_mobs(&database.connection, template.id).await?;
                self.map_mobs.insert(template.id, mobs);

                // Load NPCs using MapDao
                let npcs = MapDao::load_map_npcs(&database.connection, template.id).await?;
                self.map_npcs.insert(template.id, npcs);
            }
            
            println!("Loaded {} map templates", self.map_templates.len());
        }
        Ok(())
    }

    //load npc templates
    async fn load_npc_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let npc_templates = npc_template::Entity::find().all(&database.connection).await?;
            
            self.npc_templates = npc_templates.clone();
            for template in npc_templates {
                self.npc_templates_by_id.insert(template.id, template);
            }
            
            println!("Loaded {} NPC templates", self.npc_templates.len());
        }
        Ok(())
    }
    async fn load_item_templates(&mut self)->Result<(),Box<dyn std::error::Error>>{
        if let Some(ref database) = self.database{
            let item_manager = crate::item::item_manager::ITEM_MANAGER.read().await;
            item_manager.load_from_db(&database.connection).await?;
        }
        Ok(())
    }


    async fn load_mob_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let mob_templates = MobDao::load_all_mob_templates(&database.connection).await?;
            self.mob_templates = mob_templates.clone();
            for template in mob_templates {
                self.mob_templates_by_id.insert(template.id, template);
            }
            println!("Loaded {} mob templates", self.mob_templates.len());
        }
        Ok(())
    }

    async fn load_skill_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let skill_templates = skill_template::Entity::find().all(&database.connection).await?;
            
            self.skill_templates = skill_templates.clone();
            for template in skill_templates {
                self.skill_templates_by_id.insert(template.id, template);
            }
            
            println!("Loaded {} skill templates", self.skill_templates.len());
        }
        Ok(())
    }

    async fn load_intrinsic_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let intrinsic_templates = intrinsic::Entity::find().all(&database.connection).await?;
            
            self.intrinsic_templates = intrinsic_templates.clone();
            for template in intrinsic_templates {
                self.intrinsic_templates_by_id.insert(template.id, template);
            }
            
            println!("Loaded {} intrinsic templates", self.intrinsic_templates.len());
        }
        Ok(())
    }

    pub fn get_map_templates(&self) -> &Vec<map_template::Model> {
        &self.map_templates
    }

    pub fn get_npc_templates(&self) -> &Vec<npc_template::Model> {
        &self.npc_templates
    }

    pub fn get_mob_templates(&self) -> &Vec<mob_template::Model> {
        &self.mob_templates
    }

    pub fn get_skill_templates(&self) -> &Vec<skill_template::Model> {
        &self.skill_templates
    }
    pub fn get_item_templates(&self) -> &Vec<item_template::Model> {
        &self.item_templates
    }
    pub fn get_intrinsic_templates(&self) -> &Vec<intrinsic::Model> {
        &self.intrinsic_templates
    }
    pub fn get_intrinsic_template_by_id(&self, id: i32) -> Option<&intrinsic::Model> {
        self.intrinsic_templates_by_id.get(&id)
    }

    pub fn get_mob_service(&self) -> &MobService {
        &self.mob_service
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
