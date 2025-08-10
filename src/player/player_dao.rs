use crate::entities;
use crate::item::item::{Item as RtItem};
use crate::item::item_option::{ItemOption as RtItemOption};
use crate::player::player::Player;
use crate::item::inventory::Inventory;
use crate::player::n_point::NPoint;
use crate::item::item_service::ItemService;
// use crate::utils::Location; // not needed here
use chrono::TimeZone;
use serde_json::Value;

pub fn from_entity(model: &entities::player::Model) -> Result<Player, String> {
    let mut p = Player::new(model.id as u64, model.name.clone(), model.gender as u8);
    p.head = model.head as i16;
    p.inventory = parse_inventory_json(&model.data_inventory)?;
    if let Ok(n_point) = parse_point_array(&model.data_point) {
        p.n_point = n_point;
    }
    if let Ok((map_id, x, y)) = parse_location_array(&model.data_location) {
        p.map_id = map_id as u32;
        p.zone_id = 0;
        p.location.set_map(p.map_id, p.zone_id);
        p.location.set_position(x as i16, y as i16);
    }
    p.inventory.items_body = parse_items_json(&model.items_body);
    p.inventory.items_bag = parse_items_json(&model.items_bag);
    p.inventory.items_box = parse_items_json(&model.items_box);
    
    println!("[PLAYER_DAO] Parsed inventory - Body: {} items, Bag: {} items, Box: {} items", 
             p.inventory.items_body.len(), p.inventory.items_bag.len(), p.inventory.items_box.len());
    
    if p.inventory.items_body.len() == 11 {
        let null_item = ItemService::get_instance().create_item_null();
        p.inventory.items_body.push(null_item);
    }
    Ok(p)
}

fn parse_inventory_json(s: &str) -> Result<Inventory, String> {
    if s.is_empty() { return Ok(Inventory::new()); }
    let v: Value = serde_json::from_str(s).map_err(|e| e.to_string())?;
    let mut inv = Inventory::new();
    if let Some(obj) = v.as_object() {
        if let Some(gold) = obj.get("gold").and_then(|x| x.as_i64()) { inv.gold = gold; }
        if let Some(gem) = obj.get("gem").and_then(|x| x.as_i64()) { inv.gem = gem as i32; }
        if let Some(ruby) = obj.get("ruby").and_then(|x| x.as_i64()) { inv.ruby = ruby as i32; }
    }
    Ok(inv)
}

fn parse_location_array(s: &str) -> Result<(i64, i64, i64), String> {
    if s.is_empty() { return Err("empty location".into()); }
    let v: Value = serde_json::from_str(s).map_err(|e| e.to_string())?;
    let arr = v.as_array().ok_or("location not array")?;
    let map_id = arr.get(0).and_then(|x| x.as_i64()).ok_or("no map id")?;
    let x = arr.get(1).and_then(|x| x.as_i64()).ok_or("no x")?;
    let y = arr.get(2).and_then(|x| x.as_i64()).ok_or("no y")?;
    Ok((map_id, x, y))
}

fn parse_point_array(s: &str) -> Result<NPoint, String> {
    if s.is_empty() { return Err("empty data_point".into()); }
    let v: Value = serde_json::from_str(s).map_err(|e| e.to_string())?;
    let arr = v.as_array().ok_or("data_point not array")?;

    let read_i64 = |idx: usize| -> i64 {
        arr.get(idx).and_then(|x| x.as_i64()).unwrap_or(0)
    };

    let mut np = NPoint::new();
    let hp_max = read_i64(3).max(1) as u64;
    let mp_max = read_i64(4).max(1) as u64;
    let hp = read_i64(0) as u64;
    let mp = read_i64(1) as u64;
    let damage = read_i64(5) as u64;
    let defense = read_i64(6) as u64;
    let crit = read_i64(7) as u32;
    let power = read_i64(8) as u64;

    np.hp_max = hp_max;
    np.mp_max = mp_max;
    np.hp = if hp == 0 { hp_max } else { hp.min(hp_max) };
    np.mp = if mp == 0 { mp_max } else { mp.min(mp_max) };
    if damage != 0 { np.damage = damage; }
    if defense != 0 { np.defense = defense; }
    np.crit = crit;
    np.power = power;
    Ok(np)
}

fn parse_items_json(s: &str) -> Vec<RtItem> {
    if s.is_empty() { 
        return Vec::new(); 
    }
    let parsed: serde_json::Result<serde_json::Value> = serde_json::from_str(s);
    if parsed.is_err() { 
        return Vec::new(); 
    }
    let v = parsed.unwrap();
    let Some(arr) = v.as_array() else { 
        return Vec::new(); 
    };
    let item_service = ItemService::get_instance();
    let mut items: Vec<RtItem> = Vec::new();
    for (index, el) in arr.iter().enumerate() {
        let mut template_id_opt: Option<i32> = None;
        let mut quantity: i32 = 1;
        let mut options_acc: Vec<(i32, i32)> = Vec::new();
        let mut create_time_ms: Option<i64> = None;
        if let Some(item_str) = el.as_str() {
            if let Ok(item_array) = serde_json::from_str::<serde_json::Value>(item_str) {
                if let Some(item_arr) = item_array.as_array() {
                    if item_arr.len() >= 4 {
                        if let Some(tid) = item_arr[0].as_i64() {
                            template_id_opt = Some(tid as i32);
                        }
                        if let Some(q) = item_arr[1].as_i64() {
                            quantity = q as i32;
                        }
                        if let Some(opts_str) = item_arr[2].as_str() {
                            if let Ok(opts_array) = serde_json::from_str::<serde_json::Value>(opts_str) {
                                if let Some(opts_arr) = opts_array.as_array() {
                                    for opt in opts_arr {
                                        if let Some(opt_str) = opt.as_str() {
                                            if let Ok(opt_array) = serde_json::from_str::<serde_json::Value>(opt_str) {
                                                if let Some(opt_arr) = opt_array.as_array() {
                                                    if opt_arr.len() >= 2 {
                                                        let opt_id = opt_arr[0].as_i64().unwrap_or(0) as i32;
                                                        let opt_param = opt_arr[1].as_i64().unwrap_or(0) as i32;
                                                        options_acc.push((opt_id, opt_param));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        if let Some(ct) = item_arr[3].as_i64() {
                            create_time_ms = Some(ct);
                        }
                    }
                }
            }
        } else if let Some(obj) = el.as_object() {
            if let Some(tid) = obj.get("template_id").and_then(|x| x.as_i64()) {
                template_id_opt = Some(tid as i32);
            } else if let Some(tid) = obj.get("id").and_then(|x| x.as_i64()) {
                template_id_opt = Some(tid as i32);
            }
            if let Some(q) = obj.get("quantity").and_then(|x| x.as_i64()) { quantity = q as i32; }
            else if let Some(q) = obj.get("q").and_then(|x| x.as_i64()) { quantity = q as i32; }

            if let Some(opts) = obj.get("options").and_then(|x| x.as_array()) {
                for opt in opts {
                    if let Some(oobj) = opt.as_object() {
                        let id_opt = oobj.get("id").or_else(|| oobj.get("option_id"));
                        let param_opt = oobj.get("param").or_else(|| oobj.get("p"));
                        if let (Some(idv), Some(pv)) = (id_opt.and_then(|x| x.as_i64()), param_opt.and_then(|x| x.as_i64())) {
                            options_acc.push((idv as i32, pv as i32));
                        }
                    } else if let Some(t) = opt.as_array() {
                        if t.len() >= 2 {
                            let idv = t.get(0).and_then(|x| x.as_i64()).unwrap_or(0) as i32;
                            let pv = t.get(1).and_then(|x| x.as_i64()).unwrap_or(0) as i32;
                            options_acc.push((idv, pv));
                        }
                    }
                }
            }
            if let Some(ct) = obj.get("create_time").and_then(|x| x.as_i64()) { create_time_ms = Some(ct); }
        } else if let Some(t) = el.as_array() {
            if !t.is_empty() {
                let tid_v = t.get(0).map(|x| if let Some(n) = x.as_i64() { n } else { x.as_str().and_then(|s| s.parse::<i64>().ok()).unwrap_or(0) });
                if let Some(tid) = tid_v { template_id_opt = Some(tid as i32); }
                if let Some(q) = t.get(1).and_then(|x| if let Some(n) = x.as_i64() { Some(n) } else { x.as_str().and_then(|s| s.parse::<i64>().ok()) }) { quantity = q as i32; }

                if let Some(opt_field) = t.get(2) {
                    if let Some(sopts) = opt_field.as_str() {
                        let cleaned = sopts.replace('"', "");
                        if let Ok(val) = serde_json::from_str::<serde_json::Value>(&cleaned) {
                            if let Some(oarr) = val.as_array() {
                                for o in oarr {
                                    if let Some(a) = o.as_array() {
                                        if a.len() >= 2 {
                                            let oid = a.get(0).and_then(|x| if let Some(n) = x.as_i64() { Some(n) } else { x.as_str().and_then(|s| s.parse::<i64>().ok()) }).unwrap_or(0) as i32;
                                            let prm = a.get(1).and_then(|x| if let Some(n) = x.as_i64() { Some(n) } else { x.as_str().and_then(|s| s.parse::<i64>().ok()) }).unwrap_or(0) as i32;
                                            options_acc.push((oid, prm));
                                        }
                                    }
                                }
                            }
                        }
                    } else if let Some(oarr) = opt_field.as_array() {
                        for o in oarr {
                            if let Some(a) = o.as_array() {
                                if a.len() >= 2 {
                                    let oid = a.get(0).and_then(|x| if let Some(n) = x.as_i64() { Some(n) } else { x.as_str().and_then(|s| s.parse::<i64>().ok()) }).unwrap_or(0) as i32;
                                    let prm = a.get(1).and_then(|x| if let Some(n) = x.as_i64() { Some(n) } else { x.as_str().and_then(|s| s.parse::<i64>().ok()) }).unwrap_or(0) as i32;
                                    options_acc.push((oid, prm));
                                }
                            }
                        }
                    }
                }

                if let Some(ctv) = t.get(3) {
                    create_time_ms = if let Some(n) = ctv.as_i64() { Some(n) } else { ctv.as_str().and_then(|s| s.parse::<i64>().ok()) };
                }
            }
        } else if let Some(tid) = el.as_i64() {
            template_id_opt = Some(tid as i32);
        }

        match template_id_opt {
            Some(tid) if tid == -1 => {
                items.push(item_service.create_item_null());
            }
            Some(tid) => {
                if let Some(template) = item_service.get_template(tid).cloned() {
                    let mut item = RtItem::with_template(template, quantity);
                    for (opt_id, param) in options_acc {
                        item.add_option(RtItemOption::new(opt_id, param));
                    }
                    if let Some(ms) = create_time_ms {
                        if let Some(dt) = chrono::Utc.timestamp_millis_opt(ms).single() {
                            item.create_time = dt;
                        }
                    }
                    items.push(item);
                } else {
                    items.push(item_service.create_item_null());
                }
            }
            None => { 
                println!("[PARSE_ITEMS] Element {}: No template ID found", index);
            }
        }
    }
    items
}


