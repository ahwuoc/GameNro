use crate::account::account_dao::AccountDao;
use crate::account::account_services::AccountServices;
use crate::constant::cmd::cmd;
use crate::data::data_game::DataGame;
use crate::database::DbManager;
use crate::entities::{account, player};
use crate::map::change_map_service::ChangeMapService;
use crate::network::SESSION_MANAGER;
use crate::services::{player_info_service, ServiceHandles};
use crate::services::{GodGK, PlayerInfoService};
use anyhow::{anyhow, Result};
use bytes::Buf;
use chrono::{self, Utc};
use sea_orm::*;
use std::sync::Arc;
use tokio::sync::RwLock;

use super::message::Message;
use super::session::AsyncSession;
pub struct AsyncController;

impl AsyncController {
    pub async fn process(
        session: &mut AsyncSession,
        mut msg: Message,
        session_arc: Arc<RwLock<AsyncSession>>,
    ) -> Result<()> {
        println!("=== CLIENT MESSAGE ===");
        println!("Command: {}", msg.command);
        println!("Data length: {} bytes", msg.payload.len());
        println!("=====================");
        match msg.command {
            cmd::KEY => {
                if let Err(e) = session.send_key_async().await {
                    println!("Error sending key {}", e);
                }
                session.set_sent_key(true);
                if let Err(e) = DataGame::send_version_res(session).await {
                    println!("Error sending version res {}", e);
                }
                Ok(())
            }
            cmd::NOT_LOGIN => {
                Self::handle_message_not_login(session, msg, session_arc).await?;
                Ok(())
            }
            cmd::GET_IMAGES_SOURCE => {
                Self::handle_get_image_source(session, msg).await?;
                Ok(())
            }
            -93 => {
                Self::handle_not_login(session, msg, session_arc).await?;
                Ok(())
            }
            -28 => {
                Self::handle_message_not_map(session, msg, session_arc).await?;
                Ok(())
            }
            44 => {
                let text = msg.read_utf()?;
                ServiceHandles::chat(session, &text).await?;
                Ok(())
            }
            -87 => {
                if let Err(e) = DataGame::update_data(session).await {
                    println!("Error updating data {}", e);
                }
                Ok(())
            }
            -38 => {
                Ok(())
            }
            -39 => {
                if let Some(player) = session.get_player() {
                    ChangeMapService::finish_load_map(&player).await?;
                }
                Ok(())
            }
            -7 => {
                Self::handle_player_move(session, msg).await?;
                Ok(())
            }
            -63 => Ok(()),
            -67 => {
                let id = msg.read_int()?;
                if let Err(e) = DataGame::send_icon(session, id).await {
                    println!("Error sending icon {}", e);
                }
                Ok(())
            }
            _ => {
                println!("Unknown command: {}", msg.command);
                Ok(())
            }
        }
    }

    async fn handle_get_image_source(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        let type_byte = msg.read_byte()?;

        match type_byte {
            1 => {
                DataGame::send_size_res(session).await?;
            }
            2 => {
                DataGame::send_res(session).await?;
            }
            _ => {
                println!("Unknown type for -74 command: {}", type_byte);
            }
        }

        Ok(())
    }

    async fn handle_message_not_login(
        session: &mut AsyncSession,
        mut msg: Message,
        session_arc: Arc<RwLock<AsyncSession>>,
    ) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        println!("Handling -29 sub-command: {}", sub_cmd);
        match sub_cmd {
            0 => {
                let username = msg.read_utf()?;
                let password = msg.read_utf()?;
                    session.set_version(240);
                session.set_credentials(username.clone(), password.clone());
                Self::handle_login_authentication(
                    session,
                    &username,
                    &password,
                    session_arc.clone(),
                )
                .await?;
            }
            
            2 => {
                let _client_type = msg.read_byte()?;
                let zoom_level = msg.read_byte()?;
                let _is_gprs = msg.read_byte()? != 0;
                let _width = msg.read_int()?;
                let _height = msg.read_int()?;
                let _is_qwerty = msg.read_byte()? != 0;
                let _is_touch = msg.read_byte()? != 0;
                let platform = msg.read_utf()?;
                if let Some(version_part) = platform.split('|').nth(1) {
                    let version_str = version_part.replace(".", "");
                    if let Ok(version) = version_str.parse::<i32>() {
                        session.set_version(version);
                        println!("Client platform={} version={}", platform, version);
                    } else {
                        println!("Invalid client version string: {}", version_str);
                    }
                }

                session.zoom_level = zoom_level as u8;

                DataGame::send_link_ip(session).await?;
            }

            _ => {
                println!("Unknown sub-command for -29: {}", sub_cmd);
            }
        }

        Ok(())
    }

    async fn handle_not_login(
        session: &mut AsyncSession,
        mut msg: Message,
        session_arc: Arc<RwLock<AsyncSession>>,
    ) -> Result<()> {
        let _username_len = msg.read_byte()? as usize;
        let _password_len = msg.read_byte()? as usize;
        let version = msg.read_int()?;
        let username = msg.read_utf()?;
        let password = msg.read_utf()?;
        session.set_credentials(username.clone(), password.clone());
        session.set_version(version);
        Self::handle_login_authentication(session, &username, &password, session_arc).await?;

        Ok(())
    }

    async fn handle_login_authentication(
        session: &mut AsyncSession,
        username: &str,
        password: &str,
        session_arc: Arc<RwLock<AsyncSession>>,
    ) -> Result<()> {
        let _god_gk = GodGK::get_instance();
        let pool = DbManager::get_pool();
        let account_result: Result<Option<account::Model>> = {
            match AccountServices::login(&pool, username, password).await {
                Ok(account) => Ok(Some(account)),
                Err(e) => {
                    let text = e.to_string();
                    ServiceHandles::send_message_alert(session, &text).await?;
                    return Ok(());
                }
            }
        };
        match account_result {
            Ok(Some(account)) => {
                let account_id = account.id;
                {
                    let mut account_data = account.into_active_model();
                    account_data.last_time_login = Set(Some(Utc::now()));
                    AccountDao::update_account(&pool, account_data).await?;
                }
                let player_result = AccountDao::get_player_by_account_id(&pool, account_id).await;

                match player_result {
                    Ok(Some(db_player)) => {
                        session.set_user_id(account_id);
                        let rt_player = crate::player::player_dao::from_entity(&db_player)
                            .map_err(|e| anyhow!("Failed to build runtime player: {}", e))?;
                        let mut player_with_zone = rt_player.clone();
                        {
                            let zone_manager = crate::map::zone_manager::ZONE_MANAGER.read().await;
                            if let Some(zone) = zone_manager
                                .get_best_zone(player_with_zone.map_id as i32)
                                .await
                            {
                                player_with_zone.set_zone(zone);
                            } else {
                                println!(
                                    "[LOGIN] No zone found for map {}, creating default zone",
                                    player_with_zone.map_id
                                );
                                let default_zone = crate::map::Zone::new(
                                    player_with_zone.map_id as i32,
                                    player_with_zone.zone_id as i32,
                                    100,
                                );
                                player_with_zone.set_zone(default_zone);
                            }
                        }

                        let player_id = player_with_zone.id;

                        let has_old_session = (&*SESSION_MANAGER).is_online(player_id as i64).await;

                        if has_old_session {
                            println!(
                                "[LOGIN] Found old session for player {}, kicking old session",
                                player_id
                            );

                            (&*SESSION_MANAGER)
                                .kick_player(player_id as i64, "Tai khoan dang nhap o noi khac")
                                .await;

                            println!("[LOGIN] Old session kicked, allowing new login for player {}", player_id);
                        }

                        player_with_zone.session = Some(session_arc.clone());
                        session.set_player(player_with_zone.clone());
                        {
                            (&*SESSION_MANAGER)
                                .add_session(player_id as i64, session_arc.clone())
                                .await;
                        }
                        Self::send_login_success_data(session).await?;
                    }
                    Ok(None) => {
                        session.set_user_id(account_id);
                        Self::switch_to_create_char(session).await?;
                    }
                    Err(e) => {
                        println!("Error getting player: {:?}", e);
                        return Err(anyhow!("Database error: {:?}", e));
                    }
                }
            }
            Ok(None) => {
                return Err(anyhow!("Invalid credentials"));
            }
            Err(e) => {
                return Err(anyhow!("Authentication error: {:?}", e));
            }
        }

        Ok(())
    }

    async fn send_login_success_data(session: &mut AsyncSession) -> Result<()> {
        DataGame::send_small_version(session).await?;
        Self::send_message_93(session).await?;
        DataGame::send_version_game(session).await?;
        DataGame::send_data_item_bg(session).await?;
        PlayerInfoService::send_all_player_info(session).await?;
        Ok(())
    }

    async fn send_message_93(session: &mut AsyncSession) -> Result<()> {
        let mut msg = Message::new(-93);
        msg.write_utf("1630679752231_-93_r")?;
        session.send_message(&msg).await?;
        Ok(())
    }

    async fn switch_to_create_char(session: &mut AsyncSession) -> Result<()> {
        DataGame::send_data_item_bg(session).await?;
        DataGame::send_version_game(session).await?;
        DataGame::send_tile_set_info(session).await?;
        DataGame::update_data(session).await?;
        Ok(())
    }

    async fn handle_create_char(
        session: &mut AsyncSession,
        mut msg: Message,
        session_arc: Arc<RwLock<AsyncSession>>,
    ) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        if sub_cmd != 2 {
            return Err(anyhow!("Invalid sub command"));
        }

        let name = msg.read_utf()?;
        let gender = msg.read_byte()? as i32;
        let hair = msg.read_byte()? as i32;

        println!("DEBUG: name: {}, gender: {}, hair: {}", name, gender, hair);

        if !Self::is_valid_name(&name) {
            return Err(anyhow!("Invalid character name"));
        }

        if Self::is_name_taken(&name)
            .await
            .map_err(|_| anyhow!("Name check failed"))?
        {
            return Err(anyhow!("Character name already taken"));
        }

        if Self::is_ignored_name(&name) {
            return Err(anyhow!("Character name not allowed"));
        }

        let account_id = session.get_user_id().unwrap_or(0);
        let god_gk = GodGK::get_instance();

        let player_result = {
            let db = {
                let god_gk_guard = god_gk.lock().unwrap();
                god_gk_guard.db.clone()
            };

            if let Some(db) = db {
                let player_data = player::ActiveModel {
                    account_id: Set(Some(account_id)),
                    name: Set(name.to_string()),
                    head: Set(hair as i16),
                    gender: Set(gender),
                    have_tennis_space_ship: Set(Some(true)),
                    data_inventory: Set(r#"{"gold": 0, "gem": 0, "ruby": 0}"#.to_string()),
                    data_location: Set(r#"[0, 300, 336]"#.to_string()),
                    data_point: Set(
                        r#"[0, 0, 0, 100, 100, 0, 0, 0, 0, 0, 0, 100, 100]"#.to_string()
                    ),
                    data_magic_tree: Set(r#"[0, 0, 0, 0, 0]"#.to_string()),
                    items_body: Set(r#"[]"#.to_string()),
                    items_bag: Set(r#"[]"#.to_string()),
                    items_box: Set(r#"[]"#.to_string()),
                    items_box_lucky_round: Set(r#"[]"#.to_string()),
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
                    ..Default::default()
                };
                AccountDao::create_player(&db, player_data).await
            } else {
                Err(DbErr::Custom("Database not initialized".to_string()))
            }
        };

        match player_result {
            Ok(db_player) => {
                println!("Character created successfully: {}", name);
                let rt_player = crate::player::player_dao::from_entity(&db_player)
                    .map_err(|e| anyhow!("Failed to build runtime player: {}", e))?;
                session.set_player(rt_player);
                let username = session.get_username().unwrap_or(&String::new()).clone();
                let password = session.get_password().unwrap_or(&String::new()).clone();
                Self::handle_login_authentication(
                    session,
                    &username,
                    &password,
                    session_arc.clone(),
                )
                .await?;
            }
            Err(e) => {
                println!("Error creating character: {:?}", e);
                return Err(anyhow!("Failed to create character: {:?}", e));
            }
        }

        Ok(())
    }

    async fn handle_message_not_map(
        session: &mut AsyncSession,
        mut msg: Message,
        session_arc: Arc<RwLock<AsyncSession>>,
    ) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        println!("Handling -28 sub-command: {}", sub_cmd);

        match sub_cmd {
            2 => Self::handle_create_char(session, msg, session_arc).await,
            6 => {
                DataGame::update_map(session).await?;
                Ok(())
            }
            7 => {
                DataGame::update_skill(session).await?;
                Ok(())
            }
            8 => {
                crate::data::ItemData::update_item(session).await?;
                Ok(())
            }
            10 => {
                let map_id = msg.read_byte()?;
                DataGame::send_map_temp(session, map_id as u8).await?;
                Ok(())
            }
            13 => {
                Self::handle_client_ok_enhanced(session).await?;
                Ok(())
            }
            _ => {
                println!("Unknown -28 sub-command: {}", sub_cmd);
                Ok(())
            }
        }
    }

    async fn handle_client_ok_enhanced(session: &mut AsyncSession) -> anyhow::Result<()> {
        let player = session
            .get_player()
            .cloned()
            .ok_or(anyhow!("Player not set"))?;
        player_info_service::PlayerInfoService::send_player_blob_internal(session, &player).await?;
        player_info_service::PlayerInfoService::send_cai_trang(session, &player).await?;

        println!("Client ok enhanced initialization completed");
        Ok(())
    }

    fn is_valid_name(name: &str) -> bool {
        name.len() >= 3 && name.len() <= 20
    }

    async fn is_name_taken(_name: &str) -> Result<bool> {
        Ok(false)
    }

    fn is_ignored_name(_name: &str) -> bool {
        false
    }

    async fn handle_player_move(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        let _can_fly = msg.read_byte()?;
        let _to_x = msg.read_short()?;
        let _to_y = msg.read_short()?;

        if let Some(player) = session.get_player() {
            let player_id = player.id;
        }

        Ok(())
    }
}