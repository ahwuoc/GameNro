use crate::account::account_dao::AccountDao;
use crate::database::DbManager;
use crate::entities::{account, player};
use once_cell::sync::Lazy;
use sea_orm::*;
use std::sync::{Arc, Mutex};

pub struct GodGK {
    pub db: Option<DatabaseConnection>,
    pub maintenance: bool,
    pub server_open_time: i64,
    pub maintenance_message: String,
}

impl GodGK {
    pub fn new() -> Self {
        GodGK {
            db: None,
            maintenance: false,
            server_open_time: 0,
            maintenance_message: "Server đang bảo trì".to_string(),
        }
    }

    pub async fn init_database(
        &mut self,
        config: &crate::config::DatabaseConfig,
    ) -> Result<(), anyhow::Error> {
        let db_manager = DbManager::new(config).await?;
        let pool = db_manager.get_pool().await?;
        self.db = Some(pool);
        Ok(())
    }

    pub async fn login_god_gk(
        &self,
        username: &str,
        password: &str,
    ) -> Result<Option<account::Model>, DbErr> {
        if let Some(db) = &self.db {
            if let Some(account) = AccountDao::get_account(db, username).await? {
                if account.password == password {
                    if account.ban {
                        return Err(DbErr::Custom("Tài khoản đã bị khóa".to_string()));
                    }
                    if self.maintenance {
                        return Err(DbErr::Custom(self.maintenance_message.clone()));
                    }
                    Ok(Some(account))
                } else {
                    Err(DbErr::Custom("Sai mật khẩu".to_string()))
                }
            } else {
                Err(DbErr::Custom("Tài khoản không tồn tại".to_string()))
            }
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }

    pub async fn get_player_by_account(
        &self,
        account_id: i32,
    ) -> Result<Option<player::Model>, DbErr> {
        if let Some(db) = &self.db {
            AccountDao::get_player_by_account_id(db, account_id).await
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }

    pub async fn create_new_player(
        &self,
        account_id: i32,
        name: &str,
        gender: i32,
    ) -> Result<player::Model, DbErr> {
        if let Some(db) = &self.db {
            let player_data = player::ActiveModel {
                account_id: Set(Some(account_id)),
                name: Set(name.to_string()),
                head: Set(102),
                gender: Set(gender),
                have_tennis_space_ship: Set(Some(false)),
                clan_id: Set(-1),
                data_inventory: Set(r#"{"gold": 0, "gem": 0, "ruby": 0}"#.to_string()),
                data_location: Set(r#"[0, 300, 336]"#.to_string()),
                data_point: Set(r#"[0, 0, 0, 100, 100, 0, 0, 0, 0, 0, 0, 100, 100]"#.to_string()),
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
                create_time: Set(chrono::Local::now().naive_local()),
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
                data_vip: Set(None),
                rank: Set(0),
                data_achievement: Set(r#"[]"#.to_string()),
                giftcode: Set("".to_string()),
                event_point: Set(0),
                data_event: Set(None),
                data_badges: Set(None),
                data_task_badges: Set(None),
                first_time_login: Set(chrono::Local::now().naive_local()),
                bought_skill: Set(None),
                learn_skill: Set(None),
                daily_gift: Set(None),
                ..Default::default()
            };

            AccountDao::create_player(db, player_data).await
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }

    pub async fn update_account_last_login(
        &self,
        account_id: i32,
    ) -> Result<account::Model, DbErr> {
        if let Some(db) = &self.db {
            if let Some(account_model) = account::Entity::find_by_id(account_id).one(db).await? {
                let mut account_data = account_model.into_active_model();
                account_data.last_time_login = Set(chrono::Local::now().naive_local());
                AccountDao::update_account(db, account_data).await
            } else {
                Err(DbErr::Custom("Account not found".to_string()))
            }
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }
}

static GOD_GK: Lazy<Arc<Mutex<GodGK>>> = Lazy::new(|| Arc::new(Mutex::new(GodGK::new())));

impl GodGK {
    pub fn get_instance() -> Arc<Mutex<GodGK>> {
        GOD_GK.clone()
    }
}
