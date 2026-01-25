use crate::account::account_dao::AccountDao;
use crate::account::account_services::AccountServices;
use crate::constant::cmd::cmd;
use crate::data::data_game::DataGame;
use crate::data::ItemData;
use crate::database::DbManager;
use crate::entities::{account, player};
use crate::item::use_item::UseItem;
use crate::item::{type_item_inventory, use_item};
use crate::map::change_map_service::ChangeMapService;
use crate::network::SESSION_MANAGER;
use crate::npc::{self, npc_service};
use crate::services::mob_service;
use crate::services::{self, player_info_service, PlayerInfoService, ServiceHandles};
use crate::shop::shop_services::shop_service;
use anyhow::{anyhow, Result};
use chrono::{self, Utc};
use sea_orm::*;
use std::sync::Arc;

use super::message::Message;
use super::session::{AsyncSession, SessionArc};

pub struct AsyncController;

impl AsyncController {
    pub async fn process(session: SessionArc, mut msg: Message) -> Result<()> {
        match msg.command {
            cmd::KEY => {
                if let Err(e) = session.send_key_async().await {
                    println!("Error sending key {}", e);
                }
                session.set_sent_key(true).await;
                if let Err(e) = DataGame::send_version_res(&session).await {
                    println!("Error sending version res {}", e);
                }
                Ok(())
            }
            cmd::NOT_LOGIN => {
                Self::handle_message_not_login(&session, msg).await?;
                Ok(())
            }
            cmd::GET_IMAGES_SOURCE => {
                Self::handle_get_image_source(&session, msg).await?;
                Ok(())
            }
            54 => {
                let mob_id = msg.read_byte()? as i32;
                let is_mob_me = mob_id == -1;
                let master_id = if is_mob_me { msg.read_int()? } else { -1 };

                if let Some(player) = session.take_player().await {
                    if !is_mob_me {
                        let dame = player.n_point.get_dame_attack(false);
                        mob_service::attack_mob(&player, mob_id, dame).await;
                    } else {
                        println!("[ATTACK_MOB] Attacking master_id={}'s mob", master_id);
                    }
                    session.set_player(player).await;
                }
                Ok(())
            }
            -40 => {
                let type_byte = msg.read_byte()?;
                let type_inventory = type_item_inventory::TypeItemInventory::try_from(type_byte)?;
                let index = msg.read_byte()?;
                use_item::UseItem::get_item(&session, type_inventory, index).await?;
                Ok(())
            }
            -41 => {
                match msg.read_byte() {
                    Ok(gender) => {
                        println!("Gender: {}", gender);
                        let mut msg = Message::new(-41);
                        msg.write_byte(1);
                        msg.write_utf("Dau vuong cuong gia")?;
                        session.transmit(msg);
                    }
                    Err(e) => {
                        println!("Error reading byte {}", e);
                        return Ok(());
                    }
                }
                Ok(())
            }
            -43 => Ok(()),
            -93 => {
                Self::handle_not_login(&session, msg).await?;
                Ok(())
            }
            11 => {
                let mob_id = msg.read_byte()?;
                DataGame::send_mob_temp(&session, mob_id).await?;
                Ok(())
            }
            32 => {
                let npc_id = msg.read_short()?;
                let select = msg.read_byte()?;
                npc_service::npc_service::handle_menu_confirm(&session, npc_id, select).await?;
                Ok(())
            }
            6 => {
                let type_shop = msg.read_byte()?;
                let temp_id = msg.read_short()?;
                if let Err(e) = shop_service::take_item_shop(&session, type_shop, temp_id).await {
                    println!("Shop Error: {:?}", e);
                }
                Ok(())
            }
            -67 => {
                match msg.read_int() {
                    Ok(id) => {
                        DataGame::send_icon(&session, id).await?;
                    }
                    Err(e) => {
                        print!("Error -67 {:?}", e);
                    }
                }
                Ok(())
            }
            33 => {
                let npc_id = msg.read_short()?;
                npc_service::npc_service::open_menu_controller(&session, npc_id).await?;
                Ok(())
            }
            -28 => {
                Self::handle_message_not_map(&session, msg).await?;
                Ok(())
            }
            44 => {
                let text = msg.read_utf()?;
                if !services::command::CommandService::check(&session, &text).await? {
                    ServiceHandles::chat(&session, &text).await?;
                }
                Ok(())
            }
            -45 => {
                if let Some(mut player) = session.take_player().await {
                    services::skill_service::use_skill(&mut player, None, None, Some(msg)).await;
                    session.set_player(player).await;
                }
                Ok(())
            }
            -87 => {
                if let Err(e) = DataGame::update_data(&session).await {
                    println!("Error updating data {}", e);
                }
                Ok(())
            }
            -38 => Ok(()),
            -39 => {
                if let Some(player) = session.get_player().await {
                    ChangeMapService::finish_load_map(&player).await?;
                }
                Ok(())
            }
            29 => {
                if let Some(player) = session.take_player().await {
                    let change_map_service = ChangeMapService::new();
                    let res = change_map_service.open_zone_ui(&player, &session).await;
                    session.set_player(player).await;
                    res?;
                }
                Ok(())
            }
            21 => {
                if let Some(mut player) = session.take_player().await {
                    let zone_id = msg.read_byte()? as i32;
                    let change_map_service = ChangeMapService::new();
                    let res = change_map_service
                        .change_zone(&mut player, zone_id, &session)
                        .await;
                    session.set_player(player).await;
                    res?;
                }
                Ok(())
            }
            -33 | -23 => {
                if let Some(mut player) = session.take_player().await {
                    let change_map_service = ChangeMapService::new();
                    let res = change_map_service
                        .change_map_waypoint_handler(&mut player, &session)
                        .await;
                    session.set_player(player).await;
                    res?;
                }
                Ok(())
            }
            -15 => {
                if let Some(mut player) = session.take_player().await {
                    let change_map_service = ChangeMapService::new();
                    let res = change_map_service
                        .go_home_handler(&mut player, &session)
                        .await;
                    session.set_player(player).await;
                    res?;
                }
                Ok(())
            }
            -91 => {
                if let Some(player) = session.take_player().await {
                    let change_map_service = ChangeMapService::new();
                    let res = change_map_service
                        .open_capsule_menu(&player, &session)
                        .await;
                    session.set_player(player).await;
                    res?;
                }
                Ok(())
            }
            -7 => {
                Self::handle_player_move(&session, msg).await?;
                Ok(())
            }
            -63 => Ok(()),
            112 => {
                if session.get_player_ref(|p| p.is_some()).await {
                    services::IntrinsicService::show_menu(&session).await?;
                }
                Ok(())
            }
            -113 => {
                if let Some(mut player) = session.take_player().await {
                    for i in 0..10 {
                        let skill_id = msg.read_byte()? as u8;
                        if i < player.player_skill.skill_shortcut.len() {
                            player.player_skill.skill_shortcut[i] = skill_id;
                        }
                    }
                    session.set_player(player).await;
                    services::skill_service::send_skill_shortcut(&session).await?;
                }
                Ok(())
            }
            -81 => {
                let _ = msg.read_byte()?;
                let len = msg.read_byte()?;
                let mut index_item = Vec::new();
                for _ in 0..len {
                    index_item.push(msg.read_byte()? as i16);
                }
                crate::combine::combine_service::show_info_combine(&session, index_item).await?;
                Ok(())
            }
            _ => {
                println!("Unknown command: {}", msg.command);
                Ok(())
            }
        }
    }

    async fn handle_get_image_source(session: &SessionArc, mut msg: Message) -> Result<()> {
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

    async fn handle_message_not_login(session: &SessionArc, mut msg: Message) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        println!("Handling -29 sub-command: {}", sub_cmd);
        match sub_cmd {
            0 => {
                let username = msg.read_utf()?;
                let password = msg.read_utf()?;
                session.set_version(240).await;
                Self::handle_login_authentication(session, &username, &password).await?;
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
                        session.set_version(version).await;
                        println!("Client platform={} version={}", platform, version);
                    } else {
                        println!("Invalid client version string: {}", version_str);
                    }
                }

                session.set_zoom_level(zoom_level as u8).await;

                DataGame::send_link_ip(session).await?;
            }

            _ => {
                println!("Unknown sub-command for -29: {}", sub_cmd);
            }
        }

        Ok(())
    }

    async fn handle_not_login(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _username_len = msg.read_byte()? as usize;
        let _password_len = msg.read_byte()? as usize;
        let version = msg.read_int()?;
        let username = msg.read_utf()?;
        let password = msg.read_utf()?;
        session.set_version(version).await;
        Self::handle_login_authentication(session, &username, &password).await?;

        Ok(())
    }

    async fn initialize_logged_in_session(
        session: &SessionArc,
        account: account::Model,
        mut player_with_zone: crate::player::Player,
    ) -> Result<()> {
        let player_id = player_with_zone.id;
        player_with_zone.is_admin = account.is_admin;

        let has_old_session = (&*SESSION_MANAGER).is_online(player_id as i64).await;
        if has_old_session {
            println!(
                "[LOGIN] Found old session for player {}, kicking old session",
                player_id
            );
            (&*SESSION_MANAGER)
                .kick_player(player_id as i64, "Tai khoan dang nhap o noi khac")
                .await;
            println!(
                "[LOGIN] Old session kicked, allowing new login for player {}",
                player_id
            );
        }

        {
            let pool = DbManager::get_pool();
            let mut account_data = account.into_active_model();
            account_data.last_time_login = Set(Some(Utc::now()));
            AccountDao::update_account(&pool, account_data).await?;
        }

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

        player_with_zone.session = Some(session.clone());
        if let Some(zone) = &player_with_zone.zone {
            if let Err(e) = zone.add_player(player_with_zone.clone()).await {
                println!("Error adding player to zone: {:?}", e);
            } else {
                println!(
                    "Player {} added to zone {} map {}",
                    player_with_zone.name, zone.zone_id, zone.map_id
                );
            }
        }

        session.set_player(player_with_zone.clone()).await;
        {
            (&*SESSION_MANAGER)
                .add_session(player_id as i64, session.clone())
                .await;
        }
        Self::send_login_success_data(session).await?;
        Ok(())
    }

    async fn handle_login_authentication(
        session: &SessionArc,
        username: &str,
        password: &str,
    ) -> Result<()> {
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

                const SECOND_WAIT_LOGIN: i64 = 5;
                let now = Utc::now();
                let last_login = account.last_time_login;
                let last_logout = account.last_time_logout;

                let seconds_pass_1 = if let Some(t) = last_login {
                    (now - t).num_seconds()
                } else {
                    999999
                };

                let seconds_pass = if let Some(t) = last_logout {
                    (now - t).num_seconds()
                } else {
                    999999
                };

                if seconds_pass_1 < SECOND_WAIT_LOGIN {
                    let wait_time = if seconds_pass < seconds_pass_1 {
                        SECOND_WAIT_LOGIN - seconds_pass
                    } else {
                        SECOND_WAIT_LOGIN - seconds_pass_1
                    };

                    let mut msg = Message::new(122);
                    msg.write_short(wait_time as i16)?;
                    session.transmit(msg);
                    return Ok(());
                }

                let player_result = AccountDao::get_player_by_account_id(&pool, account_id).await;

                match player_result {
                    Ok(Some(db_player)) => {
                        session.set_user_id(account_id).await;
                        let rt_player = crate::player::player_mapper::from_entity(&db_player)
                            .await
                            .map_err(|e| anyhow!("Failed to build runtime player: {}", e))?;

                        Self::initialize_logged_in_session(session, account, rt_player).await?;
                    }
                    Ok(None) => {
                        session.set_user_id(account_id).await;
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

    async fn send_login_success_data(session: &SessionArc) -> Result<()> {
        DataGame::send_small_version(session).await?;
        Self::send_message_93(session).await?;
        DataGame::send_version_game(session).await?;
        DataGame::send_data_item_bg(session).await?;
        PlayerInfoService::send_all_player_info(session).await?;
        Ok(())
    }

    async fn send_message_93(session: &SessionArc) -> Result<()> {
        let mut msg = Message::new(-93);
        msg.write_utf("1630679752231_-93_r")?;
        session.transmit(msg);
        Ok(())
    }

    async fn switch_to_create_char(session: &SessionArc) -> Result<()> {
        DataGame::send_data_item_bg(session).await?;
        DataGame::send_version_game(session).await?;
        DataGame::send_tile_set_info(session).await?;
        DataGame::update_data(session).await?;
        Ok(())
    }

    async fn handle_create_char(session: &SessionArc, mut msg: Message) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        if sub_cmd != 2 {
            return Err(anyhow!("Invalid sub command"));
        }

        let name = msg.read_utf()?;
        let gender = msg.read_byte()? as i32;
        let hair = msg.read_byte()? as i32;
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

        let account_id = session.get_user_id().await.unwrap_or(0);
        let db = DbManager::get_pool();

        let player_result = {
            let player_data = player::ActiveModel {
                account_id: Set(Some(account_id)),
                name: Set(name.to_string()),
                head: Set(hair as i16),
                gender: Set(gender),
                have_tennis_space_ship: Set(Some(true)),
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
            AccountDao::create_player(db, player_data).await
        };

        match player_result {
            Ok(db_player) => {
                println!("Character created successfully: {}", name);
                let rt_player = crate::player::player_mapper::from_entity(&db_player)
                    .await
                    .map_err(|e| anyhow!("Failed to build runtime player: {}", e))?;

                // Fetch account to pass to initialize logic
                if let Ok(Some(account)) = AccountDao::get_account_by_id(db, account_id).await {
                    Self::initialize_logged_in_session(session, account, rt_player).await?;
                } else {
                    return Err(anyhow!(
                        "Failed to retrieve account after character creation"
                    ));
                }
            }
            Err(e) => {
                println!("Error creating character: {:?}", e);
                return Err(anyhow!("Failed to create character: {:?}", e));
            }
        }

        Ok(())
    }

    async fn handle_message_not_map(session: &SessionArc, mut msg: Message) -> Result<()> {
        let sub_cmd = msg.read_byte()?;

        match sub_cmd {
            2 => Self::handle_create_char(session, msg).await,
            6 => {
                DataGame::update_map(session).await?;
                Ok(())
            }
            7 => {
                println!("Updating skill data for client");
                DataGame::update_skill(session).await?;
                Ok(())
            }
            8 => {
                ItemData::update_item(session).await?;
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

    async fn handle_client_ok_enhanced(session: &SessionArc) -> anyhow::Result<()> {
        let player = session
            .get_player()
            .await
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

    async fn handle_player_move(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _can_fly = msg.read_byte()?; // Unused but must be read to advance cursor
        let to_x = msg.read_short()?;
        let to_y_result = msg.read_short();

        if let Some(mut player) = session.take_player().await {
            if player.is_die() {
                session.set_player(player).await;
                return Ok(());
            }

            let final_y = match to_y_result {
                Ok(y) => y,
                Err(_) => player.location.y,
            };

            player.location.x = to_x;
            player.location.y = final_y;

            let zone_opt = player.zone.clone();
            if let Some(zone) = &zone_opt {
                Self::send_player_move_to_zone(&player, zone).await?;
            }

            session.set_player(player).await;
        }

        Ok(())
    }

    async fn send_player_move_to_zone(
        player: &crate::player::Player,
        zone: &crate::map::Zone,
    ) -> Result<()> {
        let mut msg = Message::new(-7);
        msg.write_int(player.id as i32)?;
        msg.write_short(player.location.x)?;
        msg.write_short(player.location.y)?;
        zone.send_message_to_other_players(player.id, msg).await?;

        Ok(())
    }
}
