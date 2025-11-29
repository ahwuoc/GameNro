use crate::item::inventory::Inventory;
use crate::item::item::Item as RtItem;
use crate::item::item_option::ItemOption as RtItemOption;
use crate::item::item_service::ItemService;
use crate::player::n_point::NPoint;
use crate::player::player::Player;
use crate::{data, entities, item};
use anyhow::Result;
use chrono::TimeZone;
use chrono::format::Item;
use serde::{Deserialize, Serialize};
use serde_json::Value;
use tracing_subscriber::layer;

#[derive(Debug, Deserialize, Default)]
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
#[derive(Debug, Deserialize, Default)]
pub struct InventoryData {
    #[serde(default)]
    gold: i64,
    #[serde(default)]
    gem: i32,
    #[serde(default)]
    ruby: i32,
}
pub fn from_entity(model: &entities::player::Model) -> Result<Player, String> {
    println!(
        "[PLAYER_DAO] Starting from_entity for player: {} (ID: {})",
        model.name, model.id
    );
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
            p.n_point.base_crit = data_point.crit_goc;
            p.n_point.base_dame = data_point.damege_goc;
            p.n_point.base_def = data_point.defen_goc;
            p.n_point.limit_power = data_point.limit_power;
            p.n_point.tiem_nang = data_point.tiem_nang;
            p.n_point.max_satamina = data_point.max_stamina;
            p.n_point.base_satamina = data_point.stamina;
            p.n_point.base_hp = data_point.hp_goc;
            p.n_point.base_mp = data_point.mp_goc;
            p.n_point.power = data_point.power;
            p.n_point.final_hp = data_point.pl_hp;
            p.n_point.final_mp = data_point.pl_mp;
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

    let items_body = parser_item_raw(&model.items_body);
    for item_data in items_body {
        if item_data.template_id != -1 {
            if let Some(mut item) = ItemService::create_new_item_with_quantity(item_data.template_id, item_data.quantity) {
                for (opt_id, param) in item_data.options {
                    item.add_option_param(opt_id, param);
                }
                p.inventory.items_body.push(item);
            }
        } else {
            println!("item null");
            p.inventory.items_body.push(ItemService::create_item_null());
        }
    }
    if p.inventory.items_body.len() == 11 {
        let null_item = ItemService::create_item_null();
        p.inventory.items_body.push(null_item);
        println!(
            "[PLAYER_DAO] Added null item to body inventory, total: {} items",
            p.inventory.items_body.len()
        );
    }

    println!(
        "[PLAYER_DAO] Parsed inventory - Body: {} items, Bag: {} items, Box: {} items",
        p.inventory.items_body.len(),
        p.inventory.items_bag.len(),
        p.inventory.items_box.len()
    );
    Ok(p)
}

fn parse_location_array(s: &str) -> anyhow::Result<(i32, i16, i16), String> {
    if s.is_empty() {
        return Err("empty location".into());
    }
    serde_json::from_str::<(i32, i16, i16)>(s).map_err(|e| e.to_string())
}

fn parse_point_array(s: &str) -> anyhow::Result<PointData> {
    let data: PointData = serde_json::from_str(s)
        .map_err(|e| anyhow::anyhow!("Failed to parse point data: {}", e))?;
    Ok(data)
}

struct ItemDataParsed {
    template_id: i16,
    quantity: i32,
    options: Vec<(i8, i16)>,
    created: i64,
}

fn parser_item_raw(raw: &str) -> Vec<ItemDataParsed> {
    let Ok(level_1) = serde_json::from_str::<Vec<String>>(raw) else { return vec![]; };

    level_1.into_iter().filter_map(|s| {
        // Layer 1
        let arr1: Vec<serde_json::Value> = serde_json::from_str(&s).ok()?;
        if arr1.len() < 4 { return None; }

        let tid = arr1[0].as_i64()? as i16;
        let qty = arr1[1].as_i64()? as i32;

        // Layer 2
        let layer2_str = arr1[2].as_str().unwrap_or("[]");
        let Ok(layer_2) = serde_json::from_str::<Vec<String>>(layer2_str) else { return None; };

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
    }).collect()
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
