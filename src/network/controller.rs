use std::process::Command;

use crate::account::account_dao::AccountDao;
use crate::account::account_services::AccountServices;
use crate::constant::cmd::cmd;
use crate::data::data_game::DataGame;
use crate::database::DbManager;
use crate::entities::{account, comment, player, resources};
use crate::player::player_service::PLAYER_SERVICE;
use crate::services::{player_info_service, ServiceHandles};
use crate::services::{GodGK, PlayerInfoService};
use anyhow::{anyhow, Result};
use chrono::{self, Utc};
use sea_orm::*;

use super::message::Message;
use super::session::AsyncSession;
pub struct AsyncController;

impl AsyncController {
    pub async fn process(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
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
                Self::handle_message_not_login(session, msg).await?;
                Ok(())
            }
            cmd::GET_IMAGES_SOURCE => {
                Self::handle_get_image_source(session, msg).await?;
                Ok(())
            }
            -93 => {
                Self::handle_not_login(session, msg).await?;
                Ok(())
            }
            -28 => {
                Self::handle_message_not_map(session, msg).await?;
                Ok(())
            }
            44 => {
                Self::handle_chat_map(session, msg).await?;
                Ok(())
            }
            -87 => {
                if let Err(e) = DataGame::update_data(session).await {
                    println!("Error updating data {}s", e);
                }
                Ok(())
            }
            -39 => Ok(()),
            -7 => {
                Self::handle_player_move(session, msg).await?;
                Ok(())
            }
            -63 => Ok(()),
            -67 => {
                if msg.payload.len() >= 4 {
                    let id = msg.read_int()?;
                    if let Err(e) = crate::data::data_game::DataGame::send_icon(session, id).await {
                        println!("Error sending icon {}s", e);
                    }
                } else {
                    println!("-67 missing id, len={}", msg.payload.len());
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
        if msg.payload.len() < 1 {
            return Err(anyhow!("Invalid data length for -74 command"));
        }

        let type_byte = msg.read_byte()?;
        println!("Handling -74 command with type: {}", type_byte);

        match type_byte {
            1 => {
                println!("Sending size response");
                DataGame::send_size_res(session).await?;
            }
            2 => {
                println!("Sending resource files");
                DataGame::send_res(session).await?;
            }
            _ => {
                println!("Unknown type for -74 command: {}", type_byte);
            }
        }

        Ok(())
    }

    async fn handle_message_not_login(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        if msg.payload.is_empty() {
            return Err(anyhow!("data empty"));
        }
        let sub_cmd = msg.read_byte()?;
        println!("Handling -29 sub-command: {}", sub_cmd);
        match sub_cmd {
            0 => {
                let username = msg.read_utf()?;
                let password = msg.read_utf()?;

                println!("Login request: username={}", username);

                session.set_credentials(username.clone(), password.clone());

                Self::handle_login_authentication(session, &username, &password).await?;
            }
            2 => {
                if msg.payload.len() < 15 {
                    return Err(anyhow!("invalid data length for client type"));
                }

                let _client_type = msg.read_byte()?;
                let zoom_level = msg.read_byte()?;
                let _is_gprs = msg.read_byte()? != 0;
                let _width = msg.read_int()?;
                let _height = msg.read_int()?;
                let _is_qwerty = msg.read_byte()? != 0;
                let _is_touch = msg.read_byte()? != 0;
                let platform = msg.read_utf()?;

                // version parse
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
    async fn handle_not_login(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        let username_len = msg.read_byte()? as usize;
        let password_len = msg.read_byte()? as usize;
        let version = msg.read_int()?;

        if msg.payload.len() < username_len + password_len {
            return Err(anyhow!("Data too short"));
        }
        let username = msg.read_utf()?;
        let password = msg.read_utf()?;
        println!(
            "Login attempt - Username: {}, Version: {}",
            username, version
        );
        session.set_credentials(username.clone(), password.clone());
        session.set_version(version);
        Self::handle_login_authentication(session, &username, &password).await?;

        Ok(())
    }

    async fn handle_login_authentication(
        session: &mut AsyncSession,
        username: &str,
        password: &str,
    ) -> Result<()> {
        let god_gk = GodGK::get_instance();
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

                        session.set_player(player_with_zone.clone());
                        {
                            let player_service = PLAYER_SERVICE.write().await;
                            player_service.add_player(player_with_zone).await;
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

    async fn handle_create_char(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        if msg.payload.len() < 5 {
            // Need at least: [sub_cmd][name_len_2bytes][gender][hair]
            return Err(anyhow!("Invalid data length"));
        }

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
                    head: Set(hair),
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
                Self::handle_login_authentication(session, &username, &password).await?;
            }
            Err(e) => {
                println!("Error creating character: {:?}", e);
                return Err(anyhow!("Failed to create character: {:?}", e));
            }
        }

        Ok(())
    }

    async fn handle_message_not_map(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        if msg.payload.len() < 1 {
            return Err(anyhow!("Invalid data length for -28 command"));
        }

        let sub_cmd = msg.read_byte()?;
        println!("Handling -28 sub-command: {}", sub_cmd);

        match sub_cmd {
            2 => Self::handle_create_char(session, msg).await,
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

    async fn handle_client_ok_enhanced(session: &mut AsyncSession) -> Result<()> {
        let player = session
            .get_player()
            .cloned()
            .ok_or_else(|| anyhow!("Player not set"))?;
        player_info_service::PlayerInfoService::send_player_blob(session, &player).await?;
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

    async fn handle_chat_map(session: &mut AsyncSession, msg: Message) -> Result<()> {
        if let Some(player) = session.get_player() {
            if msg.payload.is_empty() {
                return Err(anyhow!("Chat message data is empty"));
            }
            let message = String::from_utf8_lossy(&msg.payload).to_string();
            let mut msg = Message::new(44);
            msg.write_utf(&format!("{}: {}", player.name, message))?;
            if let Some(zone) = &player.zone {
                zone.send_message_to_all_players(msg).await?;
            }
        }
        Ok(())
    }

    async fn handle_player_move(session: &mut AsyncSession, mut msg: Message) -> Result<()> {
        let _flag = msg.read_byte()?;

        if msg.payload.len() < 2 {
            return Ok(());
        }

        let to_x = msg.read_short()?;
        let to_y = if msg.payload.len() >= 2 {
            msg.read_short()?
        } else {
            session
                .get_player()
                .map(|p| p.get_position().1)
                .unwrap_or(0)
        };

        if let Some(player) = session.get_player() {
            let player_id = player.id;
            if let Some(mut player) = crate::player::player_service::PLAYER_SERVICE
                .read()
                .await
                .get_player(player_id)
                .await
            {
                if let Err(_) = crate::player::player_service::PLAYER_SERVICE
                    .read()
                    .await
                    .player_move(&mut player, to_x, to_y)
                    .await
                {
                    return Ok(());
                }

                crate::player::player_service::PLAYER_SERVICE
                    .write()
                    .await
                    .update_player(player_id, |p| {
                        *p = player;
                    })
                    .await;
            }
        }

        Ok(())
    }
}
