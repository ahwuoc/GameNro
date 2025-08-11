use sea_orm::*;
use serde_json;
use crate::entities::map_template;
use crate::map::Map;
use crate::map::WayPoint;

pub struct MapDao;

impl MapDao {
    pub async fn load_map_template(
        database: &DatabaseConnection,
        template_id: i32,
    ) -> Result<Option<map_template::Model>, DbErr> {
        let template = map_template::Entity::find_by_id(template_id)
            .one(database)
            .await?;

        Ok(template)
    }

    pub async fn load_all_map_templates(
        database: &DatabaseConnection,
    ) -> Result<Vec<map_template::Model>, DbErr> {
        let templates = map_template::Entity::find().all(database).await?;
        Ok(templates)
    }

    pub async fn create_map_from_template(
        database: &DatabaseConnection,
        template: &map_template::Model,
    ) -> Result<Map, Box<dyn std::error::Error>> {
        let map = Map::from_template(template);
        Ok(map)
    }
    pub async fn load_map_waypoints(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> Result<Vec<WayPoint>, Box<dyn std::error::Error>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;
        
        if let Some(template) = template {
            if !template.waypoints.is_empty() {
                // Clean escaped JSON like Manager does
                let cleaned = template.waypoints
                    .replace("[\"[", "[[")
                    .replace("]\"]", "]]")
                    .replace("\",\"", ",");
                
                if let Ok(json) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                    let mut waypoints = Vec::new();
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
                                    waypoints.push(WayPoint::new(min_x, min_y, max_x, max_y, is_enter, is_offline, name, go_map, go_x, go_y));
                                }
                            }
                        }
                    }
                    return Ok(waypoints);
                }
            }
        }
        
        Ok(Vec::new())
    }

    pub async fn load_map_mobs(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> Result<Vec<(i32, i32, i32, i32, i32)>, Box<dyn std::error::Error>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;
        
        if let Some(template) = template {
            if !template.mobs.is_empty() {
                // Clean escaped JSON like Manager does
                let cleaned = template.mobs.replace('\"', "");
                if let Ok(json) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                    let mut mobs = Vec::new();
                    if let Some(arr) = json.as_array() {
                        for mv in arr {
                            if let Some(ma) = mv.as_array() {
                                if ma.len() >= 5 {
                                    let temp = ma[0].as_i64().unwrap_or(0) as i32;
                                    let level = ma[1].as_i64().unwrap_or(1) as i32;
                                    let hp = ma[2].as_i64().unwrap_or(0) as i32;
                                    let x = ma[3].as_i64().unwrap_or(0) as i32;
                                    let y = ma[4].as_i64().unwrap_or(0) as i32;
                                    mobs.push((temp, level, hp, x, y));
                                }
                            }
                        }
                    }
                    return Ok(mobs);
                }
            }
        }
        
        Ok(Vec::new())
    }
    pub async fn load_map_npcs(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> Result<Vec<(i32, i16, i16)>, Box<dyn std::error::Error>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;
        
        if let Some(template) = template {
            if !template.npcs.is_empty() {
                // Clean escaped JSON like Manager does
                let cleaned = template.npcs.replace('\"', "");
                if let Ok(json) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                    let mut npcs = Vec::new();
                    if let Some(arr) = json.as_array() {
                        for nv in arr {
                            match nv {
                                serde_json::Value::Array(a) => {
                                    if a.len() >= 3 {
                                        let id = a[0].as_i64().unwrap_or(0) as i32;
                                        let x = a[1].as_i64().unwrap_or(0) as i16;
                                        let y = a[2].as_i64().unwrap_or(0) as i16;
                                        npcs.push((id, x, y));
                                    }
                                },
                                serde_json::Value::String(s) => {
                                    if let Ok(val) = serde_json::from_str::<serde_json::Value>(s) {
                                        if let Some(a) = val.as_array() {
                                            if a.len() >= 3 {
                                                let id = a[0].as_i64().unwrap_or(0) as i32;
                                                let x = a[1].as_i64().unwrap_or(0) as i16;
                                                let y = a[2].as_i64().unwrap_or(0) as i16;
                                                npcs.push((id, x, y));
                                            }
                                        }
                                    }
                                },
                                _ => {}
                            }
                        }
                    }
                    return Ok(npcs);
                }
            }
        }
        
        Ok(Vec::new())
    }
}
