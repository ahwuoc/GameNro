use crate::data;
use crate::entities::map_template;
use crate::map::Map;
use crate::map::WayPoint;
use sea_orm::*;
use serde::Deserialize;
use serde_json;

pub struct MapDao;

#[derive(Debug, Deserialize)]
pub struct MobSpawn {
    pub template: i32,
    pub level: i32,
    pub hp: i32,
    pub x: i16,
    pub y: i16,
}

impl MapDao {
    pub async fn load_map_waypoints(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> anyhow::Result<Vec<WayPoint>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;

        if let Some(template) = template {
            if !template.waypoints.is_empty() {
                let cleaned = template
                    .waypoints
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
                                    waypoints.push(WayPoint::new(
                                        min_x, min_y, max_x, max_y, is_enter, is_offline, name,
                                        go_map, go_x, go_y,
                                    ));
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
        db: &DatabaseConnection,
        map_id: i32,
    ) -> anyhow::Result<Vec<MobSpawn>> {
        let Some(template) = map_template::Entity::find_by_id(map_id).one(db).await? else {
            return Ok(Vec::new());
        };
        if template.mobs.is_empty() {
            return Ok(Vec::new());
        }

        let outer_json: serde_json::Value = match serde_json::from_str(&template.mobs) {
            Ok(v) => v,
            Err(e) => {
                println!("Failed to parse mobs json for map {}: {}", map_id, e);
                return Ok(Vec::new());
            }
        };

        let mut mobs = Vec::new();

        if let Some(arr) = outer_json.as_array() {
            for element in arr {
                let inner_value = match element {
                    serde_json::Value::String(s) => {
                        serde_json::from_str::<serde_json::Value>(s).ok()
                    }
                    _ => Some(element.clone()),
                };

                if let Some(val) = inner_value {
                    if let Ok(mob) = serde_json::from_value::<MobSpawn>(val.clone()) {
                        println!("Parser mob {:?}", mob);
                        mobs.push(mob);
                    } else if let Some(mob_arr) = val.as_array() {
                        if mob_arr.len() >= 5 {
                            let template = mob_arr[0].as_i64().unwrap_or(0) as i32;
                            let level = mob_arr[1].as_i64().unwrap_or(0) as i32;
                            let hp = mob_arr[2].as_i64().unwrap_or(0) as i32;
                            let x = mob_arr[3].as_i64().unwrap_or(0) as i16;
                            let y = mob_arr[4].as_i64().unwrap_or(0) as i16;
                            let mob = MobSpawn {
                                template,
                                level,
                                hp,
                                x,
                                y,
                            };
                            println!("Parser mob array {:?}", mob);
                            mobs.push(mob);
                        }
                    } else {
                        println!("Failed to parse individual mob: {:?}", val);
                    }
                }
            }
        }
        Ok(mobs)
    }
    pub async fn load_map_npcs(
        database: &DatabaseConnection,
        map_id: i32,
    ) -> anyhow::Result<Vec<(i32, i16, i16)>> {
        let template = map_template::Entity::find_by_id(map_id)
            .one(database)
            .await?;

        if let Some(template) = template {
            if !template.npcs.is_empty() {
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
                                }
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
                                }
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
