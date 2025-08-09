use crate::network::async_net::session::AsyncSession;
use crate::data::data_game::DataGame;
use crate::services::god_gk::GodGK;
use crate::entities::{account, player};
use crate::player::Player as RtPlayer;
use sea_orm::*;
use chrono;
use crate::models::zone::ZONE_MANAGER;

pub struct AsyncController;

impl AsyncController {
    pub async fn handle_message(session: &mut AsyncSession, cmd: i8, _data: Vec<u8>) -> Result<(), Box<dyn std::error::Error>> {
        println!("=== CLIENT MESSAGE ===");
        println!("Command: {}", cmd);
        println!("Data length: {} bytes", _data.len());
        println!("=====================");
        
        match cmd {
            -27 => {
                session.send_key_async().await?;
                session.set_sent_key(true); 
                DataGame::send_version_res(session).await?;
                Ok(())
            },
            -29 => {
                Self::handle_message_not_login(session, _data).await
            },
            -74 => {
                Self::handle_get_image_source(session, _data).await
            },
            -93 => {
                Self::handle_not_login(session, _data).await
            },
            -28 => {
                Self::handle_message_not_map(session, _data).await
            },
            -87 => {
                DataGame::update_data(session).await?;
                Ok(())
            },
            -39=>{

                Ok(())
            },
            -67 => {
                if _data.len() >= 4 {
                    let id = i32::from_be_bytes([_data[0], _data[1], _data[2], _data[3]]);
                    crate::data::data_game::DataGame::send_icon(session, id).await?;
                } else {
                    println!("-67 missing id, len={}", _data.len());
                }
                Ok(())
            }
            _ => {
                println!("Unknown command: {}", cmd);
                Ok(())
            }
        }
    }

    async fn handle_get_image_source(session: &mut AsyncSession, data: Vec<u8>) -> Result<(), Box<dyn std::error::Error>> {
        if data.len() < 1 {
            return Err("Invalid data length for -74 command".into());
        }
        
        let type_byte = data[0];
        println!("Handling -74 command with type: {}", type_byte);
        
        match type_byte {
            1 => {
                println!("Sending size response");
                DataGame::send_size_res(session).await?;
            },
            2 => {
                println!("Sending resource files");
                DataGame::send_res(session).await?;
            },
            _ => {
                println!("Unknown type for -74 command: {}", type_byte);
            }
        }
        
        Ok(())
    }

    async fn handle_message_not_login(session: &mut AsyncSession, data: Vec<u8>) -> Result<(), Box<dyn std::error::Error>> {
        if data.len() < 1 {
            return Err("Invalid data length for -29 command".into());
        }
        
        let sub_cmd = data[0];
        println!("Handling -29 sub-command: {}", sub_cmd);
        
        match sub_cmd {
            0 => {
                if data.len() < 5 { 
                    return Err("Invalid data length for login".into());
                }
                let mut pos = 1;
                if pos + 2 > data.len() {
                    return Err("Data too short for username length".into());
                }
                let username_len = u16::from_be_bytes([data[pos], data[pos + 1]]) as usize;
                pos += 2;
                
                if pos + username_len > data.len() {
                    return Err("Data too short for username".into());
                }
                let username = String::from_utf8_lossy(&data[pos..pos + username_len]).to_string();
                pos += username_len;
                
                if pos + 2 > data.len() {
                    return Err("Data too short for password length".into());
                }
                let password_len = u16::from_be_bytes([data[pos], data[pos + 1]]) as usize;
                pos += 2;
                
                if pos + password_len > data.len() {
                    return Err("Data too short for password".into());
                }
                let password = String::from_utf8_lossy(&data[pos..pos + password_len]).to_string();        
                session.set_credentials(username.clone(), password.clone());
                Self::handle_login_authentication(session, &username, &password).await?;
            },
            2 => {
                if data.len() < 15 {
                    return Err("Invalid data length for client type".into());
                }
                let mut pos = 1; 
                
                let _client_type = data[pos];
                pos += 1;
                
                let zoom_level = data[pos];
                pos += 1;
                let _is_gprs = data[pos] != 0;
                pos += 1;
                if pos + 4 > data.len() {
                    return Err("Data too short for width".into());
                }
                let _width = i32::from_be_bytes([data[pos], data[pos+1], data[pos+2], data[pos+3]]);
                pos += 4;
                if pos + 4 > data.len() {
                    return Err("Data too short for height".into());
                }
                let _height = i32::from_be_bytes([data[pos], data[pos+1], data[pos+2], data[pos+3]]);
                pos += 4;
                if pos >= data.len() {
                    return Err("Data too short for is_qwerty".into());
                }
                let _is_qwerty = data[pos] != 0;
                pos += 1;
                if pos >= data.len() {
                    return Err("Data too short for is_touch".into());
                }
                let _is_touch = data[pos] != 0;
                pos += 1;
                if pos + 2 > data.len() {
                    return Err("Data too short for platform length".into());
                }
                let platform_len = u16::from_be_bytes([data[pos], data[pos+1]]) as usize;
                pos += 2;
                
                if pos + platform_len > data.len() {
                    return Err("Data too short for platform string".into());
                }
                let platform = String::from_utf8_lossy(&data[pos..pos + platform_len]).to_string();
                pos += platform_len;
                if let Some(version_part) = platform.split('|').nth(1) {
                    let version_str = version_part.replace(".", "");
                    if let Ok(version) = version_str.parse::<i32>() {
                        session.set_version(version);
                        println!("Parsed version: {}", version);
                    }
                }
                session.zoom_level = zoom_level;
                DataGame::send_link_ip(session).await?;
            },
            _ => {
                println!("Unknown sub-command for -29: {}", sub_cmd);
            }
        }
        
        Ok(())
    }

    async fn handle_not_login(session: &mut AsyncSession, data: Vec<u8>) -> Result<(), Box<dyn std::error::Error>> {
        println!("Handling -93 command (not login)");
        
        if data.len() < 6 {
            return Err("Invalid data length".into());
        }

        let username_len = data[0] as usize;
        let password_len = data[1] as usize;
        let version = i32::from_le_bytes([data[2], data[3], data[4], data[5]]);

        if data.len() < 6 + username_len + password_len {
            return Err("Data too short".into());
        }
        let username = String::from_utf8_lossy(&data[6..6 + username_len]).to_string();
        let password = String::from_utf8_lossy(&data[6 + username_len..6 + username_len + password_len]).to_string();
        println!("Login attempt - Username: {}, Version: {}", username, version);
        session.set_credentials(username.clone(), password.clone());
        session.set_version(version);
        Self::handle_login_authentication(session, &username, &password).await?;

        Ok(())
    }

    async fn handle_login_authentication(session: &mut AsyncSession, username: &str, password: &str) -> Result<(), Box<dyn std::error::Error>> {
        let god_gk = GodGK::get_instance();
        let account_result = {
            let db = {
                let god_gk_guard = god_gk.lock().unwrap();
                god_gk_guard.db.clone()
            };
            
            if let Some(db) = db {
                if let Some(account) = db.get_account(username).await? {
                    if account.password == password {
                        if account.ban == 1 {
                            return Err(DbErr::Custom("Tài khoản đã bị khóa".to_string()).into());
                        }
                        Ok::<Option<account::Model>, Box<dyn std::error::Error + Send + Sync>>(Some(account))
                    } else {
                        Err(DbErr::Custom("Sai mật khẩu".to_string()).into())
                    }
                } else {
                    Err(DbErr::Custom("Tài khoản không tồn tại".to_string()).into())
                }
            } else {
                Err(DbErr::Custom("Database not initialized".to_string()).into())
            }
        };
        match account_result {
            Ok(Some(account)) => {
                {
                    let db = {
                        let god_gk_guard: std::sync::MutexGuard<'_, GodGK> = god_gk.lock().unwrap();
                        god_gk_guard.db.clone()
                    };
                    
                    if let Some(db) = db {
                        let mut account_data = account.clone().into_active_model();
                        account_data.last_time_login = Set(chrono::Utc::now());
                        db.update_account(account_data).await?;
                    }
                }

                let player_result = {
                    let db = {
                        let god_gk_guard = god_gk.lock().unwrap();
                        god_gk_guard.db.clone()
                    };
                    
                    if let Some(db) = db {
                        db.get_player_by_account_id(account.id).await
                    } else {
                        Err(DbErr::Custom("Database not initialized".to_string()))
                    }
                };

                match player_result {
                    Ok(Some(db_player)) => {
                        println!("Player found, sending login success data");
                        session.set_user_id(account.id);
                        let rt_player = RtPlayer::from_entity(&db_player)
                            .map_err(|e| format!("Failed to build runtime player: {}", e))?;
                        session.set_player(rt_player);
                        Self::send_login_success_data(session).await?;
                    }
                    Ok(None) => {
                        println!("No player found, switching to character creation");
                        session.set_user_id(account.id);
                        Self::switch_to_create_char(session).await?;
                    }
                    Err(e) => {
                        println!("Error getting player: {:?}", e);
                        return Err(format!("Database error: {:?}", e).into());
                    }
                }
            }
            Ok(None) => {
                println!("Authentication failed for user: {}", username);
                return Err("Invalid credentials".into());
            }
            Err(e) => {
                println!("Authentication error: {:?}", e);
                return Err(format!("Authentication error: {:?}", e).into());
            }
        }

        Ok(())
    }

    async fn send_login_success_data(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Sending login success data");
        
        // -77 send small version (Java: DataGame.sendSmallVersion)
        DataGame::send_small_version(session).await?;
        
        // -93 bgitem version (Java: Service.gI().sendMessage(this, -93, "1630679752231_-93_r"))
        Self::send_message_93(session).await?;
        
        // -28 send version game (Java: DataGame.sendVersionGame)
        DataGame::send_version_game(session).await?;
        
        // -31 send data item background (Java: DataGame.sendDataItemBG)
        DataGame::send_data_item_bg(session).await?;
        
        // Send player info (Java: Controller.gI().sendInfo)
        Self::send_player_info(session).await?;
        
        Ok(())
    }
    
    async fn send_message_93(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = crate::network::async_net::message::Message::new_for_writing(-93);
        msg.write_utf("1630679752231_-93_r").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.finalize_write();
        session.send_message(&msg).await?;
        Ok(())
    }

    //hanlder send player info
    async fn send_player_info(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        crate::services::player_info_service::PlayerInfoService::send_all_player_info(session).await?;
        Ok(())
    }

    async fn send_welcome_message(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        let welcome_msg = "Chào mừng bạn đến với GameNro!";
        let msg_bytes = welcome_msg.as_bytes().to_vec();
        session.send_message_old(10, msg_bytes).await?;
        Ok(())
    }

    async fn switch_to_create_char(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Switching to character creation");

        // Send data item background
        DataGame::send_data_item_bg(session).await?;

        // Send version game
        DataGame::send_version_game(session).await?;

        DataGame::send_tile_set_info(session).await?;

        // Send -93 message (character creation mode)
        session.send_message_old(-93, vec![2]).await?;

        // Send update data
        DataGame::update_data(session).await?;

        Ok(())
    }

    async fn handle_create_char(session: &mut AsyncSession, data: Vec<u8>) -> Result<(), Box<dyn std::error::Error>> {
        println!("Handling character creation");
        println!("DEBUG: Create char data length: {}", data.len());
        println!("DEBUG: Create char data (hex): {:?}", data.iter().map(|b| format!("{:02x}", b)).collect::<Vec<_>>());

        if data.len() < 5 { // Need at least: [sub_cmd][name_len_2bytes][gender][hair]
            return Err("Invalid data length".into());
        }

        let sub_cmd = data[0];
        if sub_cmd != 2 {
            return Err("Invalid sub command".into());
        }

        // Parse name length (2 bytes, big-endian) - Java readUTF format
        let name_len = u16::from_be_bytes([data[1], data[2]]) as usize;
        let gender = data[3] as i32;
        let hair = data[4] as i32;

        println!("DEBUG: name_len: {}, gender: {}, hair: {}", name_len, gender, hair);

        if data.len() < 5 + name_len {
            return Err("Data too short for name".into());
        }

        let name = String::from_utf8_lossy(&data[5..5 + name_len]).to_string();
        println!("DEBUG: Character name: '{}'", name);

        println!("Creating character - Name: {}, Gender: {}, Hair: {}", name, gender, hair);

        // Validate name
        if !Self::is_valid_name(&name) {
            return Err("Invalid character name".into());
        }

        // Check if name is taken
        if Self::is_name_taken(&name).await? {
            return Err("Character name already taken".into());
        }

        // Check if name is ignored
        if Self::is_ignored_name(&name) {
            return Err("Character name not allowed".into());
        }

        // Create new player
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
                    head: Set(hair), // Use hair as head
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
                    ..Default::default()
                };
                db.create_player(player_data).await
            } else {
                Err(DbErr::Custom("Database not initialized".to_string()))
            }
        };

        match player_result {
            Ok(db_player) => {
                println!("Character created successfully: {}", name);
                let rt_player = RtPlayer::from_entity(&db_player)
                    .map_err(|e| format!("Failed to build runtime player: {}", e))?;
                session.set_player(rt_player);
                let username = session.get_username().unwrap_or(&String::new()).clone();
                let password = session.get_password().unwrap_or(&String::new()).clone();
                Self::handle_login_authentication(session, &username, &password).await?;
            }
            Err(e) => {
                println!("Error creating character: {:?}", e);
                return Err(format!("Failed to create character: {:?}", e).into());
            }
        }

        Ok(())
    }

    async fn handle_message_not_map(session: &mut AsyncSession, data: Vec<u8>) -> Result<(), Box<dyn std::error::Error>> {
        if data.len() < 1 {
            return Err("Invalid data length for -28 command".into());
        }
        
        let sub_cmd = data[0];
        println!("Handling -28 sub-command: {}", sub_cmd);
        
        match sub_cmd {
            2 => {
                Self::handle_create_char(session, data).await
            },
            6 => {
                DataGame::update_map(session).await?;
                Ok(())
            },
            7 => {
                DataGame::update_skill(session).await?;
                Ok(())
            },
            8 => {
                DataGame::update_item(session).await?;
                Ok(())
            },
            10 => {
                DataGame::send_map_temp(session, data[1]).await?;
                Ok(())
            },
            13 => {
                Self::handle_client_ok(session).await?;
                Ok(())
            },
            _ => {
                println!("Unknown -28 sub-command: {}", sub_cmd);
                Ok(())
            }
        }
    }

    async fn handle_client_ok(session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Handling client ok - player initialization");
        //TODO: 
        println!("Client ok initialization completed");
        Ok(())
    }
    fn is_valid_name(name: &str) -> bool {
        // TODO: Implement name validation
        name.len() >= 3 && name.len() <= 20
    }
    async fn is_name_taken(_name: &str) -> Result<bool, Box<dyn std::error::Error>> {
        // TODO: Implement name taken check
        Ok(false)
    }

    fn is_ignored_name(_name: &str) -> bool {
        // TODO: Implement ignored name check
        false
    }
}


