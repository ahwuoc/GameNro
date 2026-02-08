use crate::account::account_dao::AccountDao;
use crate::database::DbManager;
use crate::entities::{account, player};
use chrono::Utc;
use sea_orm::*;

pub async fn get_player_by_account(account_id: i32) -> Result<Option<player::Model>, DbErr> {
    let db = DbManager::get_pool();
    AccountDao::get_player_by_account_id(db, account_id).await
}

pub async fn create_new_player(
    account_id: i32,
    name: &str,
    gender: i32,
    hair: i32,
) -> Result<player::Model, DbErr> {
    let db = DbManager::get_pool();
    let home_map = 39 + gender;
    let now = Utc::now().timestamp_millis();
    let inventory = format!("[2000, 1000, 0, 0, 0]");
    let location = format!("[{}, 100, 384]", home_map);
    let hpg = if gender == 0 { 200 } else { 100 };
    let mpg = if gender == 1 { 200 } else { 100 };
    let dameg = if gender == 2 { 15 } else { 10 };
    let point = format!(
        r#"{{"limitPower":0,"power":2000,"tiemNang":2000,"stamina":1000,"maxStamina":1000,"hpg":{},"mpg":{},"dameg":{},"defg":0,"critg":0,"crit_max":0,"nangDong":0,"plHp":{},"plMp":{}}}"#,
        hpg, mpg, dameg, hpg, mpg
    );
    let magic_tree = format!("[1, 5, 0, {}, {}]", now, now);
    let id_ao = gender;
    let id_quan = 6 + gender;
    let def = if gender == 2 { 3 } else { 2 };
    let hp_opt = if gender == 0 { 30 } else { 20 };
    let items_body = generate_items_body(id_ao, id_quan, def, hp_opt, now);
    let items_bag = generate_items_bag(now);
    let items_box = generate_items_box(now);
    let items_box_lucky_round = generate_null_items(110, now);
    let items_daban = generate_null_items(110, now);
    let intrinsic = "[0, 0, 0, 0, 0, 0, 0, 0]".to_string();
    let item_time = "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]".to_string();
    let task = "[0, 0, 0]".to_string();
    let charm = format!("[{0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}, {0}]", now);
    let skills = generate_skills(gender, now);
    let first_skill = gender * 2;
    let skills_shortcut = format!("[{}, -1, -1, -1, -1, -1, -1, -1, -1, -1]", first_skill);
    let black_ball = r#"["[0, 0, 0]", "[0, 0, 0]", "[0, 0, 0]", "[0, 0, 0]", "[0, 0, 0]", "[0, 0, 0]", "[0, 0, 0]"]"#.to_string();
    let side_task = "[-1, 0, 0, 0, 20, 0]".to_string();
    let bought_skill = format!("[{}]", first_skill);
    let player_data = player::ActiveModel {
        account_id: Set(Some(account_id)),
        name: Set(name.to_string()),
        head: Set(hair as i16),
        gender: Set(gender),
        have_tennis_space_ship: Set(Some(true)),
        clan_id: Set(-1),
        data_inventory: Set(inventory),
        data_location: Set(location),
        data_point: Set(point),
        data_magic_tree: Set(magic_tree),
        items_body: Set(items_body),
        items_bag: Set(items_bag),
        items_box: Set(items_box),
        items_box_lucky_round: Set(items_box_lucky_round),
        items_daban: Set(items_daban),
        friends: Set(r#"[]"#.to_string()),
        enemies: Set(r#"[]"#.to_string()),
        data_intrinsic: Set(intrinsic),
        data_item_time: Set(item_time),
        data_task: Set(task),
        data_mabu_egg: Set(r#"[]"#.to_string()),
        data_charm: Set(charm),
        skills: Set(skills),
        skills_shortcut: Set(skills_shortcut),
        pet: Set(r#"{}"#.to_string()),
        data_black_ball: Set(black_ball),
        data_side_task: Set(side_task),
        create_time: Set(Utc::now()),
        notify: Set(None),
        baovetaikhoan: Set(r#"[]"#.to_string()),
        captcha: Set(r#"[]"#.to_string()),
        data_card: Set(r#"[]"#.to_string()),
        lasttimepkcommeson: Set(0),
        bandokhobau: Set(r#"[]"#.to_string()),
        doanhtrai: Set(0),
        conduongrandoc: Set(r#"[]"#.to_string()),
        master_does_not_attack: Set("0".to_string()),
        nhanthoivang: Set(r#"[]"#.to_string()),
        ruonggo: Set(r#"[]"#.to_string()),
        sieuthanthuy: Set(r#"[]"#.to_string()),
        vodaisinhtu: Set(r#"[]"#.to_string()),
        rongxuong: Set(0),
        data_item_event: Set(r#"[]"#.to_string()),
        data_luyentap: Set(r#"[]"#.to_string()),
        data_clan_task: Set(r#"[]"#.to_string()),
        data_vip: Set(Some(r#"[]"#.to_string())),
        rank: Set(0),
        data_achievement: Set(r#"[]"#.to_string()),
        giftcode: Set("".to_string()),
        event_point: Set(0),
        data_event: Set(Some(r#"[]"#.to_string())),
        data_badges: Set(Some(r#"[]"#.to_string())),
        data_task_badges: Set(Some(r#"[]"#.to_string())),
        first_time_login: Set(Utc::now()),
        bought_skill: Set(Some(bought_skill)),
        learn_skill: Set(Some(r#"[]"#.to_string())),
        daily_gift: Set(Some(r#"[]"#.to_string())),
        ..Default::default()
    };
    AccountDao::create_player(db, player_data).await
}

fn generate_items_body(id_ao: i32, id_quan: i32, def: i32, hp_opt: i32, now: i64) -> String {
    use crate::player::player_data::{ItemDataJson, ItemOptionJson};
    let mut items = Vec::new();
    for i in 0..11 {
        match i {
            0 => {
                items.push(ItemDataJson {
                    id: id_ao,
                    quantity: 1,
                    options: vec![ItemOptionJson { id: 47, value: def }],
                    create_time: now,
                });
            }
            1 => {
                items.push(ItemDataJson {
                    id: id_quan,
                    quantity: 1,
                    options: vec![ItemOptionJson {
                        id: 6,
                        value: hp_opt,
                    }],
                    create_time: now,
                });
            }
            _ => {
                items.push(ItemDataJson {
                    id: -1,
                    quantity: 0,
                    options: vec![],
                    create_time: now,
                });
            }
        }
    }
    serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string())
}

fn generate_items_bag(now: i64) -> String {
    use crate::player::player_data::{ItemDataJson, ItemOptionJson};
    let mut items = Vec::new();
    for i in 0..30 {
        if i == 0 {
            items.push(ItemDataJson {
                id: 63,
                quantity: 10,
                options: vec![ItemOptionJson { id: 2, value: 8 }],
                create_time: now,
            });
        } else {
            items.push(ItemDataJson {
                id: -1,
                quantity: 0,
                options: vec![],
                create_time: now,
            });
        }
    }
    serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string())
}

/// Generate items_box with rada at slot 0 (30 slots)
fn generate_items_box(now: i64) -> String {
    use crate::player::player_data::{ItemDataJson, ItemOptionJson};
    let mut items = Vec::new();
    for i in 0..30 {
        if i == 0 {
            // Rada với option
            items.push(ItemDataJson {
                id: 12,
                quantity: 1,
                options: vec![ItemOptionJson { id: 14, value: 1 }],
                create_time: now,
            });
        } else {
            items.push(ItemDataJson {
                id: -1,
                quantity: 0,
                options: vec![],
                create_time: now,
            });
        }
    }
    serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string())
}

/// Generate null items for box_lucky_round and daban
fn generate_null_items(count: usize, now: i64) -> String {
    use crate::player::player_data::ItemDataJson;
    let items: Vec<ItemDataJson> = (0..count)
        .map(|_| ItemDataJson {
            id: -1,
            quantity: 0,
            options: vec![],
            create_time: now,
        })
        .collect();
    serde_json::to_string(&items).unwrap_or_else(|_| "[]".to_string())
}

/// Generate skills based on gender
fn generate_skills(gender: i32, _now: i64) -> String {
    use crate::player::player_data::SkillData;
    let skill_ids = match gender {
        0 => vec![0, 1, 6, 9, 10, 20, 22, 19],  // Trái Đất
        1 => vec![2, 3, 7, 11, 12, 17, 18, 19], // Namec
        _ => vec![4, 5, 8, 13, 14, 21, 23, 19], // Xayda
    };
    let skills: Vec<SkillData> = skill_ids
        .iter()
        .enumerate()
        .map(|(i, &id)| SkillData {
            template_id: id,
            skill_id: 0,
            point: if i == 0 { 1 } else { 0 },
            last_time_use: 0,
            curr_level: if i == 0 { 1 } else { 0 },
        })
        .collect();
    serde_json::to_string(&skills).unwrap_or_else(|_| "[]".to_string())
}

pub async fn name_is_taken(name: &str) -> anyhow::Result<()> {
    let db = DbManager::get_pool();
    let player_opt = player::Entity::find()
        .filter(player::Column::Name.eq(name))
        .one(db)
        .await
        .map_err(|e| anyhow::anyhow!(e))?;

    if player_opt.is_some() {
        return Err(anyhow::anyhow!("Tên nhân vật đã tồn tại"));
    }

    Ok(())
}
pub async fn update_account_last_login(account_id: i32) -> Result<account::Model, DbErr> {
    let db = DbManager::get_pool();
    if let Some(account_model) = account::Entity::find_by_id(account_id).one(db).await? {
        let mut account_data = account_model.into_active_model();
        account_data.last_time_login = Set(Some(Utc::now()));
        AccountDao::update_account(db, account_data).await
    } else {
        Err(DbErr::Custom("Account not found".to_string()))
    }
}
