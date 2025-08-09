use std::sync::{Arc, Mutex};
use once_cell::sync::Lazy;
use crate::utils::Database;
use crate::entities::{account, player};
use sea_orm::*;

pub struct GodGK {
    pub db: Option<Database>,
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

    pub async fn init_database(&mut self) -> Result<(), DbErr> {
        let db = Database::new().await?;
        db.test_connection().await?;
        db.init_database().await?;
        self.db = Some(db);
        Ok(())
    }

    pub async fn login_god_gk(&self, username: &str, password: &str) -> Result<Option<account::Model>, DbErr> {
        if let Some(db) = &self.db {
            // Check if account exists and password matches
            if let Some(account) = db.get_account(username).await? {
                if account.password == password {
                    // Check if account is banned
                    if account.ban == 1 {
                        return Err(DbErr::Custom("Tài khoản đã bị khóa".to_string()));
                    }

                    // Check if server is in maintenance
                    if self.maintenance {
                        return Err(DbErr::Custom(self.maintenance_message.clone()));
                    }

                    // Check server open time (stubbed for now)
                    if self.server_open_time > 0 {
                        // TODO: Implement server open time check
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

    pub async fn get_player_by_account(&self, account_id: i32) -> Result<Option<player::Model>, DbErr> {
        if let Some(db) = &self.db {
            db.get_player_by_account_id(account_id).await
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }

    pub async fn create_new_player(&self, account_id: i32, name: &str, gender: i32) -> Result<player::Model, DbErr> {
        if let Some(db) = &self.db {
            let player_data = player::ActiveModel {
                account_id: Set(Some(account_id)),
                name: Set(name.to_string()),
                head: Set(0),
                gender: Set(gender),
                have_tennis_space_ship: Set(Some(0)),
                clan_id_sv1: Set(-1),
                clan_id_sv2: Set(-1),
                data_inventory: Set(r#"{"gold": 0, "gem": 0, "ruby": 0}"#.to_string()),
                data_location: Set(r#"[0, 300, 336]"#.to_string()),
                data_point: Set(r#"[0, 0, 0, 100, 100, 0, 0, 0, 0, 0, 0, 100, 100]"#.to_string()),
                data_magic_tree: Set(r#"[0, 0, 0, 0, 0]"#.to_string()),
                items_body: Set(r#"[]"#.to_string()),
                items_bag: Set(r#"[]"#.to_string()),
                items_box: Set(r#"[]"#.to_string()),
                items_box_lucky_round: Set(r#"[]"#.to_string()),
                friends: Set(r#"[]"#.to_string()),
                enemies: Set(r#"[]"#.to_string()),
                data_offtrain: Set(r#"[0, 0]"#.to_string()),
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
                create_time: Set(chrono::Utc::now()),
                diem_danh: Set(0),
                diem_danh2: Set(0),
                soi_boss: Set(0),
                point_pvp: Set(Some(0)),
                gold_vo_dai: Set(0),
                gold_dai_hoi: Set(0),
                rank_sieu_hang: Set(999999),
                ngu_hanh_son_point: Set(Some(0)),
                cap_yari: Set(0),
                data_card: Set(r#"[]"#.to_string()),
                bill_data: Set(r#"[]"#.to_string()),
                data_item_time_sieu_cap: Set(r#"[]"#.to_string()),
                vodaisinhtu: Set(r#"[]"#.to_string()),
                bandokhobau: Set(r#"[]"#.to_string()),
                doanhtrai: Set(0),
                conduongrandoc: Set(r#"[]"#.to_string()),
                data_achievement: Set(r#"[]"#.to_string()),
                data_luyentap: Set(r#"[]"#.to_string()),
                ruonggo: Set(r#"[]"#.to_string()),
                point_noel: Set(0),
                chottop: Set(r#"[]"#.to_string()),
                dhtime: Set(r#"[]"#.to_string()),
                moc_1: Set(0),
                moc_2: Set(0),
                moc_3: Set(0),
                moc_4: Set(0),
                ..Default::default()
            };

            db.create_player(player_data).await
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }

    pub async fn update_account_last_login(&self, account_id: i32) -> Result<account::Model, DbErr> {
        if let Some(db) = &self.db {
            if let Some(account_model) = db.get_account_by_id(account_id).await? {
                let mut account_data = account_model.into_active_model();
                account_data.last_time_login = Set(chrono::Utc::now());
                db.update_account(account_data).await
            } else {
                Err(DbErr::Custom("Account not found".to_string()))
            }
        } else {
            Err(DbErr::Custom("Database not initialized".to_string()))
        }
    }
}

// Global instance
static GOD_GK: Lazy<Arc<Mutex<GodGK>>> = Lazy::new(|| {
    Arc::new(Mutex::new(GodGK::new()))
});

impl GodGK {
    pub fn get_instance() -> Arc<Mutex<GodGK>> {
        GOD_GK.clone()
    }
}
