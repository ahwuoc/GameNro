#![allow(dead_code)]
use crate::entities::player;
use crate::item::inventory::Inventory;
use crate::item::item::{self, Item as RtItem};
use crate::item::item_option::ItemOption as RtItemOption;
use crate::item::item_service::ItemService;
use crate::models::Intrinsic;
use crate::player::player::Player;
use crate::player::NPoint;
use crate::templates::intrinsic_template_manager;
use crate::utils::skill_util;
use crate::{data, entities};
use anyhow::Result;
use chrono::TimeZone;
use sea_orm::Set;
use serde::{Deserialize, Serialize};
use serde_json::Value;

#[derive(Deserialize, Serialize, Debug)]
#[serde(rename_all = "camelCase")]
struct SkillData {
    id: i32,
    point: i32,
    last_time_use: u64,
    curr_level: i16,
}

#[derive(Debug, Deserialize, Serialize, Default)]
struct IntrinsicData {
    #[serde(default)]
    intrinsic_id: i32,
    #[serde(default)]
    param1: i16,
    #[serde(default)]
    param2: i16,
    #[serde(default)]
    count_open: i8,
}
#[derive(Debug, Deserialize, Serialize, Default)]
struct PointData {
    #[serde(default)]
    limit_power: i8,
    #[serde(default)]
    power: i64,
    #[serde(default)]
    tiem_nang: i64,
    #[serde(default)]
    stamina: i16,
    #[serde(default)]
    max_stamina: i16,
    #[serde(default)]
    hp_goc: i32,
    #[serde(default)]
    mp_goc: i32,
    #[serde(default)]
    damege_goc: i32,
    #[serde(default)]
    defen_goc: i32,
    #[serde(default)]
    crit_goc: i8,
    #[serde(default)]
    crit_max: i8,
    #[serde(default)]
    nang_dong: i32,
    #[serde(default)]
    pl_hp: i32,
    #[serde(default)]
    pl_mp: i32,
}
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct InventoryData {
    #[serde(default)]
    gold: i64,
    #[serde(default)]
    gem: i32,
    #[serde(default)]
    ruby: i32,
}
pub async fn from_entity(model: &entities::player::Model) -> Result<Player, String> {
    let mut p = Player::new(model.id as u64, model.name.clone(), model.gender as u8);
    match parse_inventory_array(&model.data_inventory) {
        Ok(data_inventory) => {
            p.inventory.gold = data_inventory.gold;
            p.inventory.ruby = data_inventory.ruby;
            p.inventory.gem = data_inventory.gem;
        }
        Err(e) => {
            println!("Failed to parse inventory data: {}", e);
        }
    }
    p.head = model.head;
    match parse_point_array(&model.data_point) {
        Ok(data_point) => {
            p.n_point.critg = data_point.crit_goc;
            p.n_point.dameg = data_point.damege_goc;
            p.n_point.defg = data_point.defen_goc;
            p.n_point.limit_power = data_point.limit_power;
            p.n_point.tiem_nang = data_point.tiem_nang;
            p.n_point.max_stamina = data_point.max_stamina;
            p.n_point.stamina = data_point.stamina;
            p.n_point.hpg = data_point.hp_goc;
            p.n_point.mpg = data_point.mp_goc;
            p.n_point.power = data_point.power;
            p.n_point.hp = data_point.pl_hp;
            p.n_point.mp = data_point.pl_mp;
        }
        Err(e) => {
            println!("Failed to parse point data: {}", e);
        }
    }
    match parse_location_array(&model.data_location) {
        Ok((map_id, x, y)) => {
            p.map_id = map_id;
            p.zone_id = 0;
            p.location.set_map(p.map_id, p.zone_id);
            p.location.set_position(x, y);
        }
        Err(e) => {
            println!("Failed to parse location data: {}", e);
        }
    }

    let items_body = parser_item_json(&model.items_body);
    for item_data in items_body {
        if item_data.template_id != -1 {
            if let Some(mut item) = ItemService::create_new_item_with_quantity(
                item_data.template_id,
                item_data.quantity,
            ) {
                for (opt_id, param) in item_data.options {
                    item.add_option_param(opt_id, param);
                }
                p.inventory.items_body.push(item);
            }
        } else {
            p.inventory.items_body.push(ItemService::create_item_null());
        }
    }

    let items_bags = parser_item_json(&model.items_bag);
    for items_bag in items_bags {
        if items_bag.template_id != -1 {
            if let Some(mut item) = ItemService::create_new_item_with_quantity(
                items_bag.template_id,
                items_bag.quantity,
            ) {
                for (opt_id, opt_param) in items_bag.options {
                    item.add_option_param(opt_id, opt_param);
                }
                p.inventory.items_bag.push(item);
            }
        } else {
            p.inventory.items_bag.push(ItemService::create_item_null());
        }
    }

    let items_boxs = parser_item_json(&model.items_box);
    for item_box in items_boxs {
        if item_box.template_id != -1 {
            if let Some(mut item) =
                ItemService::create_new_item_with_quantity(item_box.template_id, item_box.quantity)
            {
                for (opt_id, param) in item_box.options {
                    item.add_option_param(opt_id, param);
                }
                p.inventory.items_box.push(item);
            }
        } else {
            p.inventory.items_box.push(ItemService::create_item_null());
        }
    }
    let intrinsic_data = parse_intrinsic_array(&model.data_intrinsic);
    if let Ok(intrinsic_data) = intrinsic_data {
        if let Some(template) = intrinsic_template_manager::get(intrinsic_data.intrinsic_id as i8) {
            p.intrinsic.intrinsic = Intrinsic::from_entity(&template);
        }
        p.intrinsic.intrinsic.param1 = intrinsic_data.param1;
        p.intrinsic.intrinsic.param2 = intrinsic_data.param2;
        p.intrinsic.count_open = intrinsic_data.count_open;
    }
    println!(
        "[PLAYER_DAO] Parsed inventory - Body: {} items, Bag: {} items, Box: {} items",
        p.inventory.items_body.len(),
        p.inventory.items_bag.len(),
        p.inventory.items_box.len()
    );

    match parse_task_data(&model.data_task) {
        Ok((task_main_id, task_index)) => {
            let calculated_task_id = (task_main_id << 10) + (task_index << 1);
            p.task_id = calculated_task_id;
            println!(
                "[PLAYER_DAO] Parsed task data - task_main_id={}, task_index={}, calculated_task_id={}",
                task_main_id, task_index, calculated_task_id
            );
        }
        Err(e) => {
            println!(
                "[PLAYER_DAO] Failed to parse task data: {}, using task_id=0",
                e
            );
            p.task_id = 0;
        }
    }

    let skill_data_arr = parse_raw_skills(&model.skills).unwrap_or_default();
    println!(
        "[PLAYER_DAO] Số lượng skill thô đã parse: {}",
        skill_data_arr.len()
    );
    for skill_data in skill_data_arr {
        let temp_id = skill_data.id;
        let point = skill_data.point;
        let last_time_use = skill_data.last_time_use;
        let curr_level = skill_data.curr_level;

        let mut skill_opt = if point != 0 {
            skill_util::create_skill(temp_id, point).await
        } else {
            skill_util::create_skill_level0(temp_id).await
        };
        if let Some(mut skill) = skill_opt {
            skill.last_time_use = last_time_use;
            skill.curr_level = curr_level;
            p.player_skill.skills.push(skill);
        } else {
            println!("[PLAYER_DAO] Thất bại khi tạo skill với id={}", temp_id);
        }
    }
    println!(
        "[PLAYER_DAO] Tổng số skill đã load: {}",
        p.player_skill.skills.len()
    );

    if let Some(first_skill) = p.player_skill.skills.first() {
        p.player_skill.skill_select = Some(first_skill.clone());
    }

    if !model.skills_shortcut.is_empty() {
        if let Ok(shortcuts) =
            serde_json::from_str::<Vec<serde_json::Value>>(&model.skills_shortcut)
        {
            for (i, v) in shortcuts.iter().enumerate() {
                if i < p.player_skill.skill_shortcut.len() {
                    let shortcut_id = v
                        .as_i64()
                        .or_else(|| v.as_str().and_then(|s| s.parse().ok()))
                        .unwrap_or(-1);
                    p.player_skill.skill_shortcut[i] = shortcut_id as u8;
                }
            }
        }
    }

    Ok(p)
}

pub fn to_active_model(p: &Player) -> player::ActiveModel {
    use sea_orm::ActiveValue::Set;

    let data_inventory = serde_json::to_string(&vec![
        p.inventory.gold,
        p.inventory.gem as i64,
        p.inventory.ruby as i64,
        0, // coupon
    ])
    .unwrap_or_else(|_| "[0,0,0,0]".to_string());

    let data_location = serde_json::to_string(&vec![
        p.map_id as i64,
        p.location.x as i64,
        p.location.y as i64,
    ])
    .unwrap_or_else(|_| "[0,0,0]".to_string());

    let data_point = serde_json::to_string(&vec![
        p.n_point.limit_power as i64,
        p.n_point.power as i64,
        p.n_point.tiem_nang as i64,
        p.n_point.stamina as i64,
        p.n_point.max_stamina as i64,
        p.n_point.hpg as i64,
        p.n_point.mpg as i64,
        p.n_point.dameg as i64,
        p.n_point.defg as i64,
        p.n_point.critg as i64,
        0, // crit_max/dragon
        0, // nang_dong
        p.n_point.hp as i64,
        p.n_point.mp as i64,
    ])
    .unwrap_or_else(|_| "[]".to_string());

    let items_body = serde_json::to_string(&map_items_to_json(&p.inventory.items_body))
        .unwrap_or_else(|_| "[]".to_string());
    let items_bag = serde_json::to_string(&map_items_to_json(&p.inventory.items_bag))
        .unwrap_or_else(|_| "[]".to_string());
    let items_box = serde_json::to_string(&map_items_to_json(&p.inventory.items_box))
        .unwrap_or_else(|_| "[]".to_string());

    let skills: Vec<SkillData> = p
        .player_skill
        .skills
        .iter()
        .map(|s| SkillData {
            id: s.template_id,
            point: s.point as i32,
            last_time_use: s.last_time_use,
            curr_level: s.curr_level,
        })
        .collect();
    let skills_str = serde_json::to_string(&skills).unwrap_or_else(|_| "[]".to_string());

    let skills_shortcut_str = serde_json::to_string(&p.player_skill.skill_shortcut.to_vec())
        .unwrap_or_else(|_| "[]".to_string());

    player::ActiveModel {
        id: Set(p.id as i32),
        name: Set(p.name.clone()),
        head: Set(p.head),
        gender: Set(p.gender as i32),
        data_inventory: Set(data_inventory),
        data_location: Set(data_location),
        data_point: Set(data_point),
        items_body: Set(items_body),
        items_bag: Set(items_bag),
        items_box: Set(items_box),
        skills: Set(skills_str),
        skills_shortcut: Set(skills_shortcut_str),
        ..Default::default()
    }
}

fn map_items_to_json(items: &[RtItem]) -> Vec<ItemDataJson> {
    items
        .iter()
        .map(|item| {
            if let Some(tpl) = &item.template {
                ItemDataJson {
                    id: tpl.id as i32,
                    quantity: item.quantity,
                    options: item
                        .item_options
                        .iter()
                        .map(|opt| ItemOptionJson {
                            id: opt.option_id as i32,
                            value: opt.param as i32,
                        })
                        .collect(),
                    create_time: item.create_time.timestamp_millis(),
                }
            } else {
                ItemDataJson {
                    id: -1,
                    quantity: 0,
                    options: vec![],
                    create_time: 0,
                }
            }
        })
        .collect()
}

fn parse_raw_skills(s: &str) -> anyhow::Result<Vec<SkillData>> {
    if s.is_empty() {
        return Ok(Vec::new());
    }
    let array: Vec<SkillData> = serde_json::from_str(s)
        .map_err(|e| anyhow::anyhow!("Failed to parse raw skills: {}", e))?;
    Ok(array)
}

fn parse_location_array(s: &str) -> anyhow::Result<(i32, i16, i16), String> {
    if s.is_empty() {
        return Err("empty location".into());
    }
    serde_json::from_str::<(i32, i16, i16)>(s).map_err(|e| e.to_string())
}

fn parse_point_array(s: &str) -> anyhow::Result<PointData> {
    if s.is_empty() || s == "[]" {
        return Ok(PointData::default());
    }

    let array: Vec<serde_json::Value> = serde_json::from_str(s)
        .map_err(|e| anyhow::anyhow!("Failed to parse point data array: {}", e))?;

    Ok(PointData {
        limit_power: array.get(0).and_then(|v| v.as_i64()).unwrap_or(0) as i8,
        power: array.get(1).and_then(|v| v.as_i64()).unwrap_or(0) as i64,
        tiem_nang: array.get(2).and_then(|v| v.as_i64()).unwrap_or(0) as i64,
        stamina: array.get(3).and_then(|v| v.as_i64()).unwrap_or(0) as i16,
        max_stamina: array.get(4).and_then(|v| v.as_i64()).unwrap_or(0) as i16,
        hp_goc: array.get(5).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        mp_goc: array.get(6).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        damege_goc: array.get(7).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        defen_goc: array.get(8).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        crit_goc: array.get(9).and_then(|v| v.as_i64()).unwrap_or(0) as i8,
        crit_max: array.get(10).and_then(|v| v.as_i64()).unwrap_or(0) as i8,
        nang_dong: array.get(11).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        pl_hp: array.get(12).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        pl_mp: array.get(13).and_then(|v| v.as_i64()).unwrap_or(0) as i32,
    })
}
fn parse_intrinsic_array(s: &str) -> anyhow::Result<IntrinsicData> {
    if s.is_empty() || s == "[]" {
        return Ok(IntrinsicData::default());
    }
    let array: Vec<serde_json::Value> = serde_json::from_str(s)
        .map_err(|e| anyhow::anyhow!("Failed to parse intrinsic array: {}", e))?;

    Ok(IntrinsicData {
        intrinsic_id: array.first().and_then(|v| v.as_i64()).unwrap_or(0) as i32,
        param1: array.get(1).and_then(|v| v.as_i64()).unwrap_or(0) as i16,
        param2: array.get(2).and_then(|v| v.as_i64()).unwrap_or(0) as i16,
        count_open: array.get(3).and_then(|v| v.as_i64()).unwrap_or(0) as i8,
    })
}

struct ItemDataParsed {
    template_id: i16,
    quantity: i32,
    options: Vec<(i8, i16)>,
    created: i64,
}

#[derive(Deserialize, Serialize)]
struct ItemOptionJson {
    id: i32,
    value: i32,
}

#[derive(Deserialize, Serialize)]
struct ItemDataJson {
    id: i32,
    #[serde(default)]
    quantity: i32,
    #[serde(default)]
    options: Vec<ItemOptionJson>,
    #[serde(default, rename = "createTime")]
    create_time: i64,
}

fn parser_item_json(raw: &str) -> Vec<ItemDataParsed> {
    let items: Vec<ItemDataJson> = serde_json::from_str(raw).unwrap_or_default();
    items
        .into_iter()
        .map(|item| ItemDataParsed {
            template_id: item.id as i16,
            quantity: item.quantity,
            options: item
                .options
                .into_iter()
                .map(|opt| (opt.id as i8, opt.value as i16))
                .collect(),
            created: item.create_time,
        })
        .collect()
}

fn parser_item_raw(raw: &str) -> Vec<ItemDataParsed> {
    let Ok(level_1) = serde_json::from_str::<Vec<String>>(raw) else {
        return vec![];
    };

    level_1
        .into_iter()
        .filter_map(|s| {
            // Layer 1
            let arr1: Vec<serde_json::Value> = serde_json::from_str(&s).ok()?;
            if arr1.len() < 4 {
                return None;
            }

            let tid = arr1[0].as_i64()? as i16;
            let qty = arr1[1].as_i64()? as i32;

            // Layer 2
            let layer2_str = arr1[2].as_str().unwrap_or("[]");
            let Ok(layer_2) = serde_json::from_str::<Vec<String>>(layer2_str) else {
                return None;
            };

            // Layer 3
            let mut options = Vec::new();
            for opt in layer_2 {
                if let Ok(v) = serde_json::from_str::<Vec<i64>>(&opt) {
                    if v.len() >= 2 {
                        options.push((v[0] as i8, v[1] as i16));
                    }
                }
            }

            let created = arr1[3].as_i64()?;

            Some(ItemDataParsed {
                template_id: tid,
                quantity: qty,
                options,
                created,
            })
        })
        .collect()
}

fn parse_inventory_array(s: &str) -> anyhow::Result<InventoryData> {
    if s.is_empty() {
        return Ok(InventoryData::default());
    }
    let array: Vec<i64> = serde_json::from_str(s)
        .map_err(|e| anyhow::anyhow!("Failed to parse inventory array: {}", e))?;
    Ok(InventoryData {
        gold: array[0],
        gem: array[1] as i32,
        ruby: array[2] as i32,
    })
}
fn parse_task_data(s: &str) -> anyhow::Result<(i32, i32)> {
    if s.is_empty() || s == "[]" {
        return Ok((0, 0));
    }

    let array: Vec<serde_json::Value> =
        serde_json::from_str(s).map_err(|e| anyhow::anyhow!("Failed to parse task data: {}", e))?;

    if array.len() < 2 {
        return Ok((0, 0));
    }

    let task_main_id = array[0].as_i64().unwrap_or(0) as i32;
    let task_index = array[1].as_i64().unwrap_or(0) as i32;

    Ok((task_main_id, task_index))
}
