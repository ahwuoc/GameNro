//! Player entity mapper - converts between database entities and runtime Player objects
#![allow(dead_code)]

use crate::entities::player;
use crate::item::item_service::ItemService;
use crate::models::Intrinsic;
use crate::player::player::Player;
use crate::player::player_data::*;
use crate::player::player_parser::*;
use crate::templates::intrinsic_template_manager;
use crate::utils::skill_util;
use sea_orm::Set;

pub async fn from_entity(model: &crate::entities::player::Model) -> Result<Player, String> {
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
    p.gender = model.gender as i8;

    // Parse point data
    match parse_point_array(&model.data_point) {
        Ok(data_point) => {
            p.n_point.crit_base = data_point.crit_goc;
            p.n_point.dame_base = data_point.damege_goc;
            p.n_point.def_base = data_point.defen_goc;
            p.n_point.limit_power = data_point.limit_power;
            p.n_point.tiem_nang = data_point.tiem_nang;
            p.n_point.max_stamina = data_point.max_stamina;
            p.n_point.stamina = data_point.stamina;
            p.n_point.hp_base = data_point.hp_goc;
            p.n_point.mp_base = data_point.mp_goc;
            p.n_point.power = data_point.power;
            p.n_point.hp_current = data_point.pl_hp;
            p.n_point.mp_current = data_point.pl_mp;
        }
        Err(e) => {
            println!("Failed to parse point data: {}", e);
        }
    }

    match parse_location_array(&model.data_location) {
        Ok((mut map_id, mut x, mut y)) => {
            if p.n_point.hp_current <= 0 {
                p.n_point.hp_current = 1;
                p.n_point.mp_current = 1;
                p.dead_flag = false;

                map_id = (p.gender as i32) + 21;
                x = 300;
                y = 336;
                println!(
                    "[PLAYER_DAO] Player {} was dead, resetting to home map {} at ({}, {})",
                    p.name, map_id, x, y
                );
            }

            p.map_id = map_id;
            p.zone_id = 0;
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

    // Parse intrinsic
    let intrinsic_data = parse_intrinsic_array(&model.data_intrinsic);
    if let Ok(intrinsic_data) = intrinsic_data {
        if let Some(template) = intrinsic_template_manager::get(intrinsic_data.intrinsic_id as i8) {
            p.intrinsic.intrinsic = Intrinsic::from_entity(&template);
        }
        p.intrinsic.intrinsic.param1 = intrinsic_data.param1;
        p.intrinsic.intrinsic.param2 = intrinsic_data.param2;
        p.intrinsic.count_open = intrinsic_data.count_open;
    }
    // Parse task
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

    // Parse skills
    let skill_data_arr = parse_raw_skills(&model.skills).unwrap_or_default();
    for skill_data in skill_data_arr {
        let temp_id = skill_data.template_id;
        let point = skill_data.point;
        let last_time_use = skill_data.last_time_use;
        let curr_level = skill_data.curr_level;

        let skill_opt = if point != 0 {
            skill_util::create_skill(temp_id, point).await
        } else {
            skill_util::create_skill_level0(temp_id).await
        };
        if let Some(mut skill) = skill_opt {
            skill.start_time_use = last_time_use;
            skill.curr_level = curr_level;
            p.player_skill.skills.push(skill);
        } else {
            println!("[PLAYER_DAO] Thất bại khi tạo skill với id={}", temp_id);
        }
    }

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

    p.n_point.cal_point();

    // Parse pet data
    match parse_pet_data(&model.pet) {
        Ok(pet_data) => {
            if let Some(ref data) = pet_data {
                println!(
                    "[PLAYER_DAO] Pet {} items_body: {:?}",
                    data.name, data.items_body
                );
            }
            p.pet_data = pet_data;
            p.is_pet = true;
        }
        Err(e) => {
            println!("[PLAYER_DAO] Failed to parse pet data: {}", e);
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

    let point_data = PointData {
        limit_power: p.n_point.limit_power,
        power: p.n_point.power,
        tiem_nang: p.n_point.tiem_nang,
        stamina: p.n_point.stamina,
        max_stamina: p.n_point.max_stamina,
        hp_goc: p.n_point.hp_base,
        mp_goc: p.n_point.mp_base,
        damege_goc: p.n_point.dame_base,
        defen_goc: p.n_point.def_base,
        crit_goc: p.n_point.crit_base,
        crit_max: 0,
        nang_dong: 0,
        pl_hp: p.n_point.hp_current,
        pl_mp: p.n_point.mp_current,
    };
    let data_point = serde_json::to_string(&point_data).unwrap_or_else(|_| "{}".to_string());

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
            template_id: s.template_id,
            skill_id: s.skill_id,
            point: s.point as i32,
            last_time_use: s.start_time_use,
            curr_level: s.curr_level,
        })
        .collect();
    let skills_str = serde_json::to_string(&skills).unwrap_or_else(|_| "[]".to_string());

    let skills_shortcut_str = serde_json::to_string(&p.player_skill.skill_shortcut.to_vec())
        .unwrap_or_else(|_| "[]".to_string());

    let pet_str = p
        .pet_data
        .as_ref()
        .map(|d| serde_json::to_string(d).unwrap_or_else(|_| "{}".to_string()))
        .unwrap_or_else(|| "{}".to_string());

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
        pet: Set(pet_str),
        ..Default::default()
    }
}

pub fn player_to_pet_data(p: &Player) -> PetData {
    PetData {
        name: p.name.clone(),
        gender: p.gender,
        head: p.head,
        status: 0,
        type_pet: 0,
        n_point: PointData {
            limit_power: p.n_point.limit_power,
            power: p.n_point.power,
            tiem_nang: p.n_point.tiem_nang,
            stamina: p.n_point.stamina,
            max_stamina: p.n_point.max_stamina,
            hp_goc: p.n_point.hp_base,
            mp_goc: p.n_point.mp_base,
            damege_goc: p.n_point.dame_base,
            defen_goc: p.n_point.def_base,
            crit_goc: p.n_point.crit_base,
            crit_max: 0,
            nang_dong: 0,
            pl_hp: p.n_point.hp_current,
            pl_mp: p.n_point.mp_current,
        },
        items_body: map_items_to_json(&p.inventory.items_body),
        skills: p
            .player_skill
            .skills
            .iter()
            .map(|s| SkillData {
                template_id: s.template_id,
                skill_id: s.skill_id,
                point: s.point as i32,
                last_time_use: s.start_time_use,
                curr_level: s.curr_level,
            })
            .collect(),
    }
}
