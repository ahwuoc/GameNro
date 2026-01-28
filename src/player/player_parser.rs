//! Parser utilities for player data deserialization
#![allow(dead_code)]

use crate::item::item::Item as RtItem;
use crate::player::player_data::*;
use anyhow::Result;

// ============================================
// Inventory Parser
// ============================================

pub fn parse_inventory_array(s: &str) -> Result<InventoryData> {
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

// ============================================
// Location Parser
// ============================================

pub fn parse_location_array(s: &str) -> Result<(i32, i16, i16), String> {
    if s.is_empty() {
        return Err("empty location".into());
    }
    serde_json::from_str::<(i32, i16, i16)>(s).map_err(|e| e.to_string())
}

// ============================================
// Point Parser
// ============================================

pub fn parse_point_array(s: &str) -> Result<PointData> {
    if s.is_empty() || s == "[]" {
        return Ok(PointData::default());
    }

    if let Ok(data) = serde_json::from_str::<PointData>(s) {
        return Ok(data);
    }

    Err(anyhow::anyhow!("Failed to parse point data: {}", s))
}

// ============================================
// Intrinsic Parser
// ============================================

pub fn parse_intrinsic_array(s: &str) -> Result<IntrinsicData> {
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

// ============================================
// Skill Parser
// ============================================

pub fn parse_raw_skills(s: &str) -> Result<Vec<SkillData>> {
    if s.is_empty() {
        return Ok(Vec::new());
    }
    let array: Vec<SkillData> = serde_json::from_str(s)
        .map_err(|e| anyhow::anyhow!("Failed to parse raw skills: {}", e))?;
    Ok(array)
}

// ============================================
// Task Parser
// ============================================

pub fn parse_task_data(s: &str) -> Result<(i32, i32)> {
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

// ============================================
// Item Parser (JSON format)
// ============================================

pub fn parser_item_json(raw: &str) -> Vec<ItemDataParsed> {
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

// ============================================
// Item Parser (raw format - legacy)
// ============================================

pub fn parser_item_raw(raw: &str) -> Vec<ItemDataParsed> {
    let Ok(level_1) = serde_json::from_str::<Vec<String>>(raw) else {
        return vec![];
    };

    level_1
        .into_iter()
        .filter_map(|s| {
            let arr1: Vec<serde_json::Value> = serde_json::from_str(&s).ok()?;
            if arr1.len() < 4 {
                return None;
            }

            let tid = arr1[0].as_i64()? as i16;
            let qty = arr1[1].as_i64()? as i32;

            let layer2_str = arr1[2].as_str().unwrap_or("[]");
            let Ok(layer_2) = serde_json::from_str::<Vec<String>>(layer2_str) else {
                return None;
            };

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

// ============================================
// Item Serializer (to JSON)
// ============================================

pub fn map_items_to_json(items: &[RtItem]) -> Vec<ItemDataJson> {
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
                    options: item
                        .item_options
                        .iter()
                        .map(|opt| ItemOptionJson {
                            id: opt.option_id as i32,
                            value: opt.param as i32,
                        })
                        .collect(),
                    create_time: 0,
                }
            }
        })
        .collect()
}
