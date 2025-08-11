use std::collections::HashMap;
use std::io;
use tokio::time::Duration;
use dotenv::dotenv;
use std::env;
use crate::entities::{array_head_2_frames, item_option_template, item_template};
use crate::entities::{nclass, skill_template};
use crate::network::async_net::message::Message;
use crate::network::async_net::session::AsyncSession;
use crate::utils::Database as DbUtil;
use sea_orm::EntityTrait;
use serde_json::Value;
use crate::msg_write;

#[derive(Debug, Clone)]
pub struct Skill {
    pub skill_id: i16,
    pub point: i8,
    pub pow_require: i64,
    pub mana_use: i16,
    pub cool_down: i32,
    pub dx: i16,
    pub dy: i16,
    pub max_fight: i8,
    pub damage: i16,
    pub price: i16,
    pub more_info: String,
}

#[derive(Debug, Clone)]
pub struct SkillTemplate {
    pub id: i8,
    pub class_id: i32,
    pub name: String,
    pub max_point: i8,
    pub mana_use_type: i8,
    pub r#type: i8,
    pub icon_id: i16,
    pub dam_info: String,
    pub skills: Vec<Skill>,
}

#[derive(Debug, Clone)]
pub struct NClass {
    pub class_id: i32,
    pub name: String,
    pub skill_templates: Vec<SkillTemplate>,
}

pub struct DataGame;

impl DataGame {
    pub async fn send_size_res(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending size response");
        let zoom_level = session.zoom_level;
        let res_path = format!("data/girlkun/res/x{}", zoom_level);

        let mut file_count: i32 = 0;
        if let Ok(entries) = std::fs::read_dir(&res_path) {
            for entry in entries {
                if let Ok(entry) = entry {
                    if entry.file_type().map(|ft| ft.is_file()).unwrap_or(false) {
                        file_count += 1;
                    }
                }
            }
        }

        println!("Found {} files in {}", file_count, res_path);

        let mut data = vec![1]; // type = 1 for size response
        data.extend_from_slice(&(file_count as u16).to_be_bytes()); // file count as short

        session.send_message_old(-74, data).await?;

        Ok(())
    }

    pub async fn send_res(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let zoom_level = session.zoom_level;
        let res_path = format!("data/girlkun/res/x{}", zoom_level);

        if let Ok(entries) = std::fs::read_dir(&res_path) {
            for entry in entries {
                if let Ok(entry) = entry {
                    if entry.file_type().map(|ft| ft.is_file()).unwrap_or(false) {
                        let file_path = entry.path();
                        if let Some(file_name) = file_path.file_name() {
                            if let Some(name_str) = file_name.to_str() {
                                if let Ok(content) = std::fs::read(&file_path) {
                                    let mut data = vec![2];
                                    let name_bytes = name_str.as_bytes();
                                    data.extend_from_slice(
                                        &(name_bytes.len() as u16).to_be_bytes(),
                                    );
                                    data.extend_from_slice(name_bytes);

                                    data.extend_from_slice(&(content.len() as u32).to_be_bytes());
                                    data.extend_from_slice(&content);
                                    session.send_message_old(-74, data).await?;
                                    tokio::time::sleep(tokio::time::Duration::from_millis(5)).await;
                                }
                            }
                        }
                    }
                }
            }
        }
        let mut final_data = vec![3]; 
        final_data.extend_from_slice(&752012i32.to_be_bytes()); 
        session.send_message_old(-74, final_data).await?;

        Ok(())
    }

    pub async fn send_version_res(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut version_data = vec![0];
        version_data.extend_from_slice(&752012i32.to_be_bytes());
        session.send_message_old(-74, version_data).await?;

        Ok(())
    }

    pub async fn send_small_version(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = crate::network::async_net::message::Message::new_for_writing(-77);
        let zoom_level = session.zoom_level;
        let file_path = format!("data/girlkun/data_img_version/x{}/img_version", zoom_level);

        match std::fs::read(&file_path) {
            Ok(data) => {
                msg_write!(msg, write(&data));
            }
            Err(_) => {
                println!("Warning: Small version file not found: {}", file_path);
            }
        }

        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_version_game(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = crate::network::async_net::message::Message::new_for_writing(-28);
        msg_write!(msg, write_byte(4)); // vsData
        msg_write!(msg, write_byte(1)); // vsMap
        msg_write!(msg, write_byte(1)); // vsSkill
        msg_write!(msg, write_byte(1)); // vsItem
        msg_write!(msg, write_byte(0)); // padding

        let standard_levels = [
            1000i64,
            3000,
            15000,
            40000,
            90000,
            170000,
            340000,
            700000,
            1500000,
            15000000,
            150000000,
            1500000000,
            5000000000,
            10000000000,
            40000000000,
            50010000000,
            60010000000,
            70010000000,
            80010000000,
            100010000000,
        ];

        msg_write!(msg, write_byte(standard_levels.len() as i8));
        for &level in &standard_levels {
            msg_write!(msg, write_long(level));
        }

        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn send_data_item_bg(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = crate::network::async_net::message::Message::new_for_writing(-31);
        match std::fs::read("data/girlkun/item_bg_temp/item_bg_data") {
            Ok(data) => {
                msg_write!(msg, write(&data));
            }
            Err(_) => {
                println!("Warning: Item background data file not found");
            }
        }

        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }

    pub async fn update_data(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Updating data for client");
        let dart_data = match std::fs::read("data/girlkun/update_data/dart") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let arrow_data = match std::fs::read("data/girlkun/update_data/arrow") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let effect_data = match std::fs::read("data/girlkun/update_data/effect") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let image_data = match std::fs::read("data/girlkun/update_data/image") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let part_data = match std::fs::read("data/girlkun/update_data/part") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let skill_data = match std::fs::read("data/girlkun/update_data/skill") {
            Ok(data) => data,
            Err(_) => vec![],
        };

        let mut msg = crate::network::async_net::message::Message::new_for_writing(-87);

        msg_write!(msg, write_byte(80));

        // Write dart data
        msg_write!(msg, write_int(dart_data.len() as i32));
        msg_write!(msg, write(&dart_data));

        // Write arrow data
        msg_write!(msg, write_int(arrow_data.len() as i32));
        msg_write!(msg, write(&arrow_data));

        // Write effect data
        msg_write!(msg, write_int(effect_data.len() as i32));
        msg_write!(msg, write(&effect_data));

        // Write image data
        msg_write!(msg, write_int(image_data.len() as i32));
        msg_write!(msg, write(&image_data));

        // Write part data
        msg_write!(msg, write_int(part_data.len() as i32));
        msg_write!(msg, write(&part_data));

        // Write skill data
        msg_write!(msg, write_int(skill_data.len() as i32));
        msg_write!(msg, write(&skill_data));

        msg.finalize_write();
        session.send_message_old(-87, msg.get_data()).await?;
        println!("Update data sent successfully");
        Ok(())
    }

    pub async fn update_map(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let manager = crate::services::Manager::get_instance();
        let (map_templates, npc_templates, mob_templates) = {
            let manager_guard = manager.lock().unwrap();
            (
                manager_guard.get_map_templates().clone(),
                manager_guard.get_npc_templates().clone(),
                manager_guard.get_mob_templates().clone(),
            )
        };
        let mut msg = crate::network::async_net::message::Message::new_for_writing(-28);
        msg_write!(msg, write_byte(6));
        msg_write!(msg, write_byte(80));
        msg_write!(msg, write_byte((map_templates.len() as u8) as i8));
        for template in &map_templates {
            msg_write!(msg, write_utf(&template.name));
        }
        msg_write!(msg, write_byte((npc_templates.len() as u8) as i8));
        for template in &npc_templates {
            msg_write!(msg, write_utf(&template.name));
            msg_write!(msg, write_short(template.head as i16));
            msg_write!(msg, write_short(template.body as i16));
            msg_write!(msg, write_short(template.leg as i16));
            msg_write!(msg, write_byte(0));
            // padding
        }
        msg_write!(msg, write_byte((mob_templates.len() as u8) as i8));
        for template in &mob_templates {
            msg_write!(msg, write_byte((template.r#type as u8) as i8));
            msg_write!(msg, write_utf(&template.name));
            msg_write!(msg, write_int(template.hp));
            msg_write!(msg, write_byte((template.range_move as u8) as i8));
            msg_write!(msg, write_byte((template.speed as u8) as i8));
            msg_write!(msg, write_byte((template.dart_type as u8) as i8));
        }
        msg.finalize_write();
        session.send_message_old(-28, msg.get_data()).await?;

        println!(
            "Map data updated successfully with {} maps, {} NPCs, {} mobs",
            map_templates.len(),
            npc_templates.len(),
            mob_templates.len()
        );
        Ok(())
    }

    pub async fn update_skill(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = crate::network::async_net::message::Message::new_for_writing(-28);
        msg_write!(msg, write_byte(7));
        msg_write!(msg, write_byte(60));
        msg_write!(msg, write_byte(0));
        let nclasses = load_skill_data().await?;
        msg_write!(msg, write_byte(nclasses.len() as i8));

        for nclass in &nclasses {
            msg_write!(msg, write_utf(&nclass.name));
            msg_write!(msg, write_byte(nclass.skill_templates.len() as i8));

            for skill_temp in &nclass.skill_templates {
                msg_write!(msg, write_byte(skill_temp.id));
                msg_write!(msg, write_utf(&skill_temp.name));
                msg_write!(msg, write_byte(skill_temp.max_point as i8));
                msg_write!(msg, write_byte(skill_temp.mana_use_type as i8));
                msg_write!(msg, write_byte(skill_temp.r#type as i8));
                msg_write!(msg, write_short(skill_temp.icon_id as i16));
                msg_write!(msg, write_utf(&skill_temp.dam_info));
                msg_write!(msg, write_utf("Nro Wars"));

                if skill_temp.id != 0 {
                    msg_write!(msg, write_byte(skill_temp.skills.len() as i8));
                    for skill in &skill_temp.skills {
                        msg_write!(msg, write_short(skill.skill_id));
                        msg_write!(msg, write_byte(skill.point as i8));
                        msg_write!(msg, write_long(skill.pow_require));
                        msg_write!(msg, write_short(skill.mana_use));
                        msg_write!(msg, write_int(skill.cool_down));
                        msg_write!(msg, write_short(skill.dx));
                        msg_write!(msg, write_short(skill.dy));
                        msg_write!(msg, write_byte(skill.max_fight as i8));
                        msg_write!(msg, write_short(skill.damage));
                        msg_write!(msg, write_short(skill.price));
                        msg_write!(msg, write_utf(&skill.more_info));
                    }
                } else {
                    msg_write!(msg, write_byte((skill_temp.skills.len() + 2) as i8));
                    for skill in &skill_temp.skills {
                        msg_write!(msg, write_short(skill.skill_id));
                        msg_write!(msg, write_byte(skill.point as i8));
                        msg_write!(msg, write_long(skill.pow_require));
                        msg_write!(msg, write_short(skill.mana_use));
                        msg_write!(msg, write_int(skill.cool_down));
                        msg_write!(msg, write_short(skill.dx));
                        msg_write!(msg, write_short(skill.dy));
                        msg_write!(msg, write_byte(skill.max_fight as i8));
                        msg_write!(msg, write_short(skill.damage));
                        msg_write!(msg, write_short(skill.price));
                        msg_write!(msg, write_utf(&skill.more_info));
                    }
                    for i in 105..=106 {
                        msg_write!(msg, write_short(i));
                        msg_write!(msg, write_byte(0));
                        msg_write!(msg, write_long(0));
                        msg_write!(msg, write_short(0));
                        msg_write!(msg, write_int(0));
                        msg_write!(msg, write_short(0));
                        msg_write!(msg, write_short(0));
                        msg_write!(msg, write_byte(0));
                        msg_write!(msg, write_short(0));
                        msg_write!(msg, write_short(0));
                        msg_write!(msg, write_utf(""));
                    }
                }
            }
        }

        msg.finalize_write();
        let data = msg.get_data();
        session.send_message_old(-28, data).await?;
        Ok(())
    }

    pub async fn send_map_temp(
        session: &mut AsyncSession,
        map_id: u8,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let file_path = format!("data/girlkun/map/tile_map_data/{}", map_id);
        match std::fs::read(&file_path) {
            Ok(data) => {
                if data.len() < 2 {
                    let mut msg = Message::new_for_writing(-28);
                    msg.write_byte(0)?;
                    msg.write_byte(0)?;
                    msg.finalize_write();
                    session.send_message(&msg).await?;
                    return Ok(());
                }
                let tmw = data[0] as usize;
                let tmh = data[1] as usize;
                let expected = 2 + tmw * tmh;
                let to_send: &[u8] = if data.len() >= expected {
                    &data[..expected]
                } else {
                    &data[..]
                };
                let payload_len = 1 + to_send.len();
                let hi = ((payload_len as u16) >> 8) as u8;
                let lo = (payload_len as u16 & 0xFF) as u8;
                let preview_n = to_send.len().min(8);
                let mut preview = String::from("0A");
                for b in &to_send[..preview_n] {
                    preview.push_str(&format!(" {:02X}", b));
                }
                let mut msg = Message::new_for_writing(-28);
                msg.write_byte(10)?;
                msg.write(to_send)?;
                msg.finalize_write();
                session.send_message(&msg).await?;
            }
            Err(_) => {
                let mut msg = Message::new_for_writing(-28);
                msg.write_byte(0)?;
                msg.write_byte(0)?;
                msg.finalize_write();
                session.send_message(&msg).await?;
            }
        }
        Ok(())
    }

    pub async fn send_skill_data(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let skill_data = b"skill_data".to_vec();
        session.send_message_old(-72, skill_data).await?;

        Ok(())
    }

    pub async fn send_item_data(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending item data");
        let item_data = b"item_data".to_vec();
        session.send_message_old(-73, item_data).await?;

        Ok(())
    }

    /// Send icon (-67) like Java DataGame.sendIcon
    pub async fn send_icon(
        session: &mut AsyncSession,
        id: i32,
    ) -> Result<(), Box<dyn std::error::Error>> {
        let zoom_level = session.zoom_level;
        let file_path = format!("data/girlkun/icon/x{}/{}.png", zoom_level, id);
        let mut msg = Message::new_for_writing(-67);
        match std::fs::read(&file_path) {
            Ok(icon) => {
                msg.write_int(id)?;
                msg.write_int(icon.len() as i32)?;
                msg.write(&icon)?;
            }
            Err(_) => {
                // Send empty payload for missing icon
                msg.write_int(id)?;
                msg.write_int(0)?;
            }
        }
        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }
   
    pub async fn update_item(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        // Delegate to ItemData module
        crate::data::ItemData::update_item(session).await
    }

    pub async fn send_tile_set_info(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        match std::fs::read("data/girlkun/map/tile_set_info") {
            Ok(data) => {
                let mut msg = Message::new_for_writing(-82);
                msg_write!(msg, write(&data));
                msg.finalize_write();
                session.send_message(&msg).await?;
            }
            Err(_) => {
                println!("Warning: Tile set info file not found");
                let mut msg = Message::new_for_writing(-82);
                msg.finalize_write();
                session.send_message(&msg).await?;
            }
        }

        Ok(())
    }

    pub async fn send_client_ok(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending client OK");
        let ok_data = b"ok".to_vec();
        session.send_message_old(-75, ok_data).await?;
        Ok(())
    }
    pub async fn send_link_ip(
        session: &mut AsyncSession,
    ) -> Result<(), Box<dyn std::error::Error>> {
        dotenv().ok();
        let link_data = env::var("GAME_LINK")
            .unwrap_or_else(|_| "Ngọc rồng Wars:127.0.0.1:14445:0,0,0".to_string());
        let link_bytes = link_data.as_bytes();
        let mut data = vec![2];
        data.extend_from_slice(&(link_bytes.len() as u16).to_be_bytes());
        data.extend_from_slice(link_bytes);
        data.push(1);
        session.send_message_old(-29, data).await?;
        Ok(())
    }
}

async fn load_skill_data() -> Result<Vec<NClass>, Box<dyn std::error::Error>> {
    let manager = crate::services::Manager::get_instance();
    let manager_guard = manager.lock().unwrap();
    let skill_templates = manager_guard.get_skill_templates();
    let mut nclasses_map: HashMap<i32, NClass> = HashMap::new();
    for template in skill_templates {
        let nclass_id = template.nclass_id;

        // Get or create NClass
        let nclass = nclasses_map.entry(nclass_id).or_insert_with(|| {
            let name = match nclass_id {
                0 => "Trái Đất".to_string(),
                1 => "Namếc".to_string(),
                2 => "Xayda".to_string(),
                _ => format!("Class {}", nclass_id),
            };

            NClass {
                class_id: nclass_id,
                name,
                skill_templates: Vec::new(),
            }
        });

        let skills = parse_skills_json(&template.skills)?;
        let skill_template = SkillTemplate {
            id: template.id as i8,
            class_id: template.nclass_id,
            name: template.name.clone(),
            max_point: template.max_point as i8,
            mana_use_type: template.mana_use_type as i8,
            r#type: template.r#type as i8,
            icon_id: template.icon_id as i16,
            dam_info: template.dam_info.clone(),
            skills,
        };
        nclass.skill_templates.push(skill_template);
    }
    let mut nclasses: Vec<NClass> = nclasses_map.into_values().collect();
    nclasses.sort_by_key(|nclass| nclass.class_id);

    println!("Loaded {} NClasses with skill data", nclasses.len());
    Ok(nclasses)
}

fn parse_skills_json(skills_json: &str) -> Result<Vec<Skill>, Box<dyn std::error::Error>> {
    // Parse JSON like Java: JSONArray from template.skills
    if skills_json.is_empty() {
        return Ok(Vec::new());
    }

    let cleaned_json = skills_json
        .replace("[\"", "[")
        .replace("\"[", "[")
        .replace("\"]", "]")
        .replace("]\"", "]")
        .replace("}\",\"{", "},{");

    let json_value: Value = serde_json::from_str(&cleaned_json)?;

    let mut skills = Vec::new();

    if let Value::Array(skills_array) = json_value {
        for skill_obj in skills_array {
            if let Value::Object(skill_map) = skill_obj {
                let skill = Skill {
                    skill_id: skill_map
                        .get("id")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    point: skill_map
                        .get("point")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    pow_require: skill_map
                        .get("power_require")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    mana_use: skill_map
                        .get("mana_use")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    cool_down: skill_map
                        .get("cool_down")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    dx: skill_map
                        .get("dx")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    dy: skill_map
                        .get("dy")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    max_fight: skill_map
                        .get("max_fight")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    damage: skill_map
                        .get("damage")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    price: skill_map
                        .get("price")
                        .and_then(|v| v.as_str())
                        .and_then(|s| s.parse().ok())
                        .unwrap_or(0),
                    more_info: skill_map
                        .get("info")
                        .and_then(|v| v.as_str())
                        .unwrap_or("")
                        .to_string(),
                };
                skills.push(skill);
            }
        }
    }

    Ok(skills)
}


