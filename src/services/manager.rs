use crate::data;
use crate::models::item;
use crate::utils::Database;
use sea_orm::EntityTrait;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;

// Import entities from the generated module
use crate::entities::{item_template, map_template};
use crate::entities::npc_template;
use crate::entities::mob_template;
use crate::entities::skill_template;
use crate::entities::intrinsic;

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
    pub map_templates_by_id: HashMap<i32, map_template::Model>,
    pub npc_templates_by_id: HashMap<i32, npc_template::Model>,
    pub mob_templates_by_id: HashMap<i32, mob_template::Model>,
    pub skill_templates_by_id: HashMap<i32, skill_template::Model>,
    pub intrinsic_templates_by_id: HashMap<i32, intrinsic::Model>,
    // Cached per-map parsed data
    pub map_waypoints: HashMap<i32, Vec<crate::models::map::WayPoint>>, // map_id -> waypoints
    pub map_mobs: HashMap<i32, Vec<(i32, i32, i32, i32, i32)>>, // map_id -> Vec<(temp, level, hp, x, y)>
    pub map_npcs: HashMap<i32, Vec<(i32, i16, i16)>>, // map_id -> Vec<(npcId, x, y)>
    database: Option<Database>,
}

impl Manager {
    pub fn new() -> Self {
        Manager {
            map_templates: Vec::new(),
            npc_templates: Vec::new(),
            mob_templates: Vec::new(),
            item_templates:Vec::new(),
            skill_templates: Vec::new(),
            intrinsic_templates: Vec::new(),
            map_templates_by_id: HashMap::new(),
            npc_templates_by_id: HashMap::new(),
            mob_templates_by_id: HashMap::new(),
            skill_templates_by_id: HashMap::new(),
            intrinsic_templates_by_id: HashMap::new(),
            map_waypoints: HashMap::new(),
            map_mobs: HashMap::new(),
            map_npcs: HashMap::new(),
            database: None,
        }
    }

    pub fn get_instance() -> Arc<Mutex<Manager>> {
        Arc::clone(&MANAGER)
    }

    pub async fn init(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        let database = Database::new().await?;
        self.database = Some(database);
        
        self.load_item_templates().await?;

        // Load map templates
        self.load_map_templates().await?;
        
        // Load NPC templates
        self.load_npc_templates().await?;
        
        // Load mob templates
        self.load_mob_templates().await?;
        
        // Load skill templates
        self.load_skill_templates().await?;
        
        // Load intrinsic templates
        self.load_intrinsic_templates().await?;
        
        println!("Manager initialized successfully!");
        Ok(())
    }

    async fn load_map_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let map_templates = map_template::Entity::find().all(&database.connection).await?;
            self.map_templates = map_templates.clone();
            for template in map_templates {
                // Cache by id
                self.map_templates_by_id.insert(template.id, template.clone());

                // Parse waypoints JSON (string may contain escaped arrays)
                if !template.waypoints.is_empty() {
                    let cleaned = template.waypoints
                        .replace("[\"[", "[[")
                        .replace("]\"]", "]]")
                        .replace("\",\"", ",");
                    if let Ok(json) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                        let mut list = Vec::new();
                        if let Some(arr) = json.as_array() {
                            for wpv in arr {
                                if let Some(wp_arr) = wpv.as_array() {
                                    if wp_arr.len() >= 10 {
                                        let name = wp_arr[0].as_str().unwrap_or("").to_string();
                                        let min_x = wp_arr[1].as_i64().unwrap_or(0) as i16;
                                        let min_y = wp_arr[2].as_i64().unwrap_or(0) as i16;
                                        let max_x = wp_arr[3].as_i64().unwrap_or(0) as i16;
                                        let max_y = wp_arr[4].as_i64().unwrap_or(0) as i16;
                                        let is_enter = (wp_arr[5].as_i64().unwrap_or(0) as i8) == 1;
                                        let is_offline = (wp_arr[6].as_i64().unwrap_or(0) as i8) == 1;
                                        let go_map = wp_arr[7].as_i64().unwrap_or(0) as i32;
                                        let go_x = wp_arr[8].as_i64().unwrap_or(0) as i16;
                                        let go_y = wp_arr[9].as_i64().unwrap_or(0) as i16;
                                        list.push(crate::models::map::WayPoint::new(min_x, min_y, max_x, max_y, is_enter, is_offline, name, go_map, go_x, go_y));
                                    }
                                }
                            }
                        }
                        self.map_waypoints.insert(template.id, list);
                    }
                }

                if !template.mobs.is_empty() {
                    let cleaned = template.mobs.replace('\"', "");
                    if let Ok(json) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                        let mut list = Vec::new();
                        if let Some(arr) = json.as_array() {
                            for mv in arr {
                                if let Some(ma) = mv.as_array() {
                                    if ma.len() >= 5 {
                                        let temp = ma[0].as_i64().unwrap_or(0) as i32;
                                        let level = ma[1].as_i64().unwrap_or(1) as i32;
                                        let hp = ma[2].as_i64().unwrap_or(0) as i32;
                                        let x = ma[3].as_i64().unwrap_or(0) as i32;
                                        let y = ma[4].as_i64().unwrap_or(0) as i32;
                                        list.push((temp, level, hp, x, y));
                                    }
                                }
                            }
                        }
                        self.map_mobs.insert(template.id, list);
                    }
                }
                if !template.npcs.is_empty() {
                    let cleaned = template.npcs.replace('\"', "");
                    if let Ok(json) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                        let mut list = Vec::new();
                        if let Some(arr) = json.as_array() {
                            for nv in arr {
                                match nv {
                                    serde_json::Value::Array(a) => {
                                        if a.len() >= 3 {
                                            let id = a[0].as_i64().unwrap_or(0) as i32;
                                            let x = a[1].as_i64().unwrap_or(0) as i16;
                                            let y = a[2].as_i64().unwrap_or(0) as i16;
                                            list.push((id, x, y));
                                        }
                                    },
                                    serde_json::Value::String(s) => {
                                        if let Ok(val) = serde_json::from_str::<serde_json::Value>(s) {
                                            if let Some(a) = val.as_array() {
                                                if a.len() >= 3 {
                                                    let id = a[0].as_i64().unwrap_or(0) as i32;
                                                    let x = a[1].as_i64().unwrap_or(0) as i16;
                                                    let y = a[2].as_i64().unwrap_or(0) as i16;
                                                    list.push((id, x, y));
                                                }
                                            }
                                        }
                                    },
                                    _ => {}
                                }
                            }
                        }
                        self.map_npcs.insert(template.id, list);
                    }
                }
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
    //load item templates
    async fn load_item_templates(&mut self)->Result<(),Box<dyn std::error::Error>>{
        if let Some(ref database) = self.database{
            let item_templates = item_template::Entity::find().all(&database.connection).await?;
            self.item_templates = item_templates.clone();
             println!("Loaded {} item templates",self.item_templates.len());
        }
        Ok(())
    }


    async fn load_mob_templates(&mut self) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(ref database) = self.database {
            let mob_templates = mob_template::Entity::find().all(&database.connection).await?;
            
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
}
