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
    let home_map = match gender {
        0 => 21,
        1 => 22,
        _ => 23,
    };
    let player_data = player::ActiveModel {
        account_id: Set(Some(account_id)),
        name: Set(name.to_string()),
        head: Set(hair as i16),
        gender: Set(gender),
        have_tennis_space_ship: Set(Some(true)),
        clan_id: Set(-1),
        data_inventory: Set(r#"[0, 0, 0, 0]"#.to_string()),
        data_location: Set(format!(r#"[{0}, 300, 336]"#, home_map)),
        data_point: Set(r#"[0, 0, 0, 100, 100, 100, 100, 10, 0, 0, 0, 0, 100, 100]"#.to_string()),
        data_magic_tree: Set(r#"[0, 0, 0, 0, 0]"#.to_string()),
        items_body: Set(r#"[]"#.to_string()),
        items_bag: Set(r#"[]"#.to_string()),
        items_box: Set(r#"[]"#.to_string()),
        items_box_lucky_round: Set(r#"[]"#.to_string()),
        items_daban: Set(r#"[]"#.to_string()),
        friends: Set(r#"[]"#.to_string()),
        enemies: Set(r#"[]"#.to_string()),
        data_intrinsic: Set(r#"[]"#.to_string()),
        data_item_time: Set(r#"[]"#.to_string()),
        data_task: Set(r#"[]"#.to_string()),
        data_mabu_egg: Set(r#"[]"#.to_string()),
        data_charm: Set(r#"[]"#.to_string()),
        skills: Set(r#"[]"#.to_string()),
        skills_shortcut: Set(r#"[]"#.to_string()),
        pet: Set(r#"[]"#.to_string()),
        data_black_ball: Set(r#"[]"#.to_string()),
        data_side_task: Set(r#"[]"#.to_string()),
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
        bought_skill: Set(Some(r#"[]"#.to_string())),
        learn_skill: Set(Some(r#"[]"#.to_string())),
        daily_gift: Set(Some(r#"[]"#.to_string())),
        ..Default::default()
    };

    AccountDao::create_player(db, player_data).await
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
