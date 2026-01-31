use crate::account::account_dao::AccountDao;
use crate::account::account_services::AccountServices;
use crate::constant::cmd::cmd;
use crate::data::data_game::DataGame;
use crate::data::ItemData;
use crate::database::DbManager;
use crate::entities::{account, player};
use crate::item::{item_controller, type_item_inventory};
use crate::map::change_map_service::ChangeMapService;
use crate::map::services::mob_service;
use crate::network::SESSION_MANAGER;
use crate::npc::{self, npc_service};
use crate::services::auth_service;
use crate::services::{self, player_info_service, player_service, ServiceHandles};
use crate::shop::shop_services::shop_service;
use anyhow::{anyhow, Result};
use chrono::{self, Utc};
use sea_orm::*;
use std::sync::Arc;
use tracing::{debug, error, info, instrument, warn};

use super::message::Message;
use super::session::{AsyncSession, SessionArc};

pub struct AsyncController;

impl AsyncController {
    #[instrument(skip(session, msg), fields(command = msg.command, sub_cmd))]
    pub async fn process(session: SessionArc, mut msg: Message) -> Result<()> {
        let data_len = msg.payload.len();
        let sub_cmd = msg.payload.get(0).copied().map(|b| b as i8);
        debug!(
            "CMD: {} (0x{:02X}), len={}, sub_cmd={:?}",
            msg.command, msg.command as u8, data_len, sub_cmd
        );
        match msg.command {
            cmd::KEY => {
                if let Err(e) = session.send_key_async().await {
                    error!("Error sending key {}", e);
                }
                session.set_sent_key(true).await;
                if let Err(e) = DataGame::send_version_res(&session).await {
                    error!("Error sending version res {}", e);
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
            cmd::ATTACK_MOB => {
                let mob_id = msg.read_byte()? as i32;
                let is_mob_me = mob_id == -1;
                let master_id = if is_mob_me { msg.read_int()? } else { -1 };

                if let Some(mut player) = session.take_player().await {
                    let mut handled = false;

                    let skill_info = player.player_skill.skill_select.as_ref().map(|s| {
                        let temp = crate::templates::skill_template_manager::get(s.template_id);
                        (s.template_id, temp.map(|t| t.r#type).unwrap_or(0))
                    });
                    if let Some((skill_id, skill_type)) = skill_info {
                        if skill_type == 1 || skill_type == 4 || skill_id == 20 || skill_id == 22 {
                            let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                            if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id)
                            {
                                // Two-Phase: Collect damage message inside lock, send after
                                let damage_msg = {
                                    let mut mobs = zone.active_mobs.write().unwrap();
                                    if let Some(mob) =
                                        mobs.iter_mut().find(|m| m.id == mob_id as u64)
                                    {
                                        handled = true;
                                        services::skill_service::execute_skill(
                                            &mut player,
                                            None,
                                            Some(mob),
                                        )
                                    } else {
                                        None
                                    }
                                }; // Lock released here

                                // Phase 2: Send message (no mob lock held)
                                if let Some(msg) = damage_msg {
                                    let _ = crate::services::ServiceHandles::send_to_all_in_zone(
                                        &zone, msg,
                                    );
                                }
                            }
                        }
                    }

                    if !handled {
                        if !is_mob_me {
                            let dame = player.n_point.get_dame_attack(false);
                            mob_service::attack_mob(&player, mob_id, dame);
                        } else {
                            debug!("[ATTACK_MOB] Attacking master_id={}'s mob", master_id);
                        }
                    }
                    session.set_player(player, session.clone()).await;
                }
                Ok(())
            }
            cmd::GET_EFFECT_TEMPLATE => {
                let eff_id = msg.read_short()?;
                let id_t = eff_id;
                DataGame::send_effect_template(&session, eff_id, Some(id_t)).await?;

                Ok(())
            }
            -40 => {
                let type_byte = msg.read_byte()?;
                let type_inventory = type_item_inventory::TypeItemInventory::try_from(type_byte)?;
                let index = msg.read_byte()?;
                item_controller::ItemController::get_item(&session, type_inventory, index).await?;
                Ok(())
            }
            -41 => {
                match msg.read_byte() {
                    Ok(gender) => {
                        info!("Gender: {}", gender);
                        let mut msg = Message::new(-41);
                        msg.write_byte(1);
                        msg.write_utf("Dau vuong cuong gia")?;
                        session.transmit(msg);
                    }
                    Err(e) => {
                        error!("Error reading byte {}", e);
                        return Ok(());
                    }
                }
                Ok(())
            }
            -43 => {
                let type_byte = msg.read_byte()?;
                let type_action = type_item_inventory::TypeItemAction::try_from(type_byte)?;
                let where_item = msg.read_byte()?;
                let index = msg.read_byte()?;
                item_controller::ItemController::handle_item_action(
                    &session,
                    type_action,
                    where_item,
                    index,
                )
                .await?;
                Ok(())
            }
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
                    error!("Shop Error: {:?}", e);
                }
                Ok(())
            }
            66 => {
                let img_name = msg.read_utf()?;
                DataGame::send_image_by_name(&session, &img_name).await?;
                Ok(())
            }
            -32 => {
                let bg_id = msg.read_short()?;
                DataGame::send_item_bg_template(&session, bg_id).await?;
                Ok(())
            }

            -78 => {
                let _ = msg.read_int();
                Ok(())
            }
            -67 => {
                match msg.read_int() {
                    Ok(id) => {
                        DataGame::send_icon(&session, id).await?;
                    }
                    Err(e) => {
                        error!("Error -67 {:?}", e);
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
            cmd::SELECT_SKILL => {
                if let Some(mut player) = session.take_player().await {
                    let skill_template_id = msg.read_short().unwrap_or(0);
                    services::skill_service::select_skill(&mut player, skill_template_id as i32)?;
                    session.set_player(player, session.clone()).await;
                }
                Ok(())
            }
            cmd::CHAT => {
                let text = msg.read_utf()?;
                let mut is_command = false;
                if let Some(mut player) = session.take_player().await {
                    is_command =
                        services::command::CommandService::check(&mut player, &session, &text)
                            .await?;
                    session.set_player(player, session.clone()).await;
                }

                if !is_command {
                    ServiceHandles::chat(&session, &text).await?;
                }
                Ok(())
            }
            cmd::USE_SKILL => {
                if let Some(mut player) = session.take_player().await {
                    services::skill_service::handle_use_skill_packet(
                        &mut player,
                        None,
                        None,
                        Some(msg),
                    );
                    session.set_player(player, session.clone()).await;
                }
                Ok(())
            }
            -87 => {
                if let Err(e) = DataGame::update_data(&session).await {
                    error!("Error updating data {}", e);
                }
                Ok(())
            }
            -30 => {
                Self::sub_command_30(&session, msg).await;
                Ok(())
            }
            -38 => Ok(()),
            -39 => {
                if let Some(player) = session.get_player().await {
                    ChangeMapService::finish_load_map(&player)?;
                }
                Ok(())
            }
            29 => {
                if let Some(player) = session.take_player().await {
                    let res = ChangeMapService::open_zone_ui(&player);
                    session.set_player(player, session.clone()).await;
                    res?;
                }
                Ok(())
            }
            21 => {
                if let Some(mut player) = session.take_player().await {
                    let zone_id = msg.read_byte()? as i32;

                    let res = ChangeMapService::change_zone(&mut player, zone_id, &session);
                    session.set_player(player, session.clone()).await;
                    res?;
                }
                Ok(())
            }
            -33 | -23 => {
                if let Some(mut player) = session.take_player().await {
                    let res = ChangeMapService::change_map_waypoint_handler(&mut player, &session);
                    session.set_player(player, session.clone()).await;
                    res?;
                }
                Ok(())
            }
            -16 => {
                if let Some(mut player) = session.take_player().await {
                    let res = player_service::hoi_sinh(&mut player);
                    session.set_player(player, session.clone()).await;
                    res?;
                }
                Ok(())
            }
            -15 => {
                if let Some(mut player) = session.take_player().await {
                    let res = ChangeMapService::go_home_handler(&mut player, &session);
                    session.set_player(player, session.clone()).await;
                    res?;
                }
                Ok(())
            }
            -91 => {
                if let Some(player) = session.take_player().await {
                    let res = ChangeMapService::open_capsule_menu(&player);
                    session.set_player(player, session.clone()).await;
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
                    services::skill_service::send_skill_shortcut(&player)?;
                    session.set_player(player, session.clone()).await;
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
            cmd::PICK_ITEM => {
                let item_map_id = msg.read_short()? as i32;
                if let Some(mut player) = session.take_player().await {
                    if player.is_die() {
                        session.set_player(player, session.clone()).await;
                        return Ok(());
                    }

                    let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                    let zone_opt = zone_manager.get_zone(player.map_id, player.zone_id);

                    if let Some(zone) = zone_opt {
                        let item_opt = zone.get_item(item_map_id);

                        if let Some(item_map) = item_opt {
                            if let Some(template) = item_map.item_template {
                                let mut item = crate::item::item::Item::with_template(
                                    template,
                                    item_map.quantity,
                                );
                                item.item_options = item_map.options.clone();

                                use crate::item::inventory_service::InventoryService;

                                let result = InventoryService::add_item_bag(&mut player, item);

                                if let Ok(_) = result {
                                    if let Some(_removed_item) = zone.remove_item(item_map_id) {
                                        use crate::map::services::item_map_service::ItemMapService;

                                        let disappear_msg =
                                            ItemMapService::build_item_disappear_message(
                                                item_map_id,
                                            );
                                        let _ =
                                            crate::services::ServiceHandles::send_to_all_in_zone(
                                                &zone,
                                                disappear_msg,
                                            );

                                        let pickup_msg =
                                            ItemMapService::build_pickup_notification_message(
                                                item_map_id,
                                                player.id,
                                            );
                                        let _ = ServiceHandles::send_mess_another_not_me_in_map(
                                            &player, pickup_msg,
                                        );

                                        println!(
                                            "[PICKUP] Success: Player {} picked up item {}",
                                            player.id, item_map_id
                                        );
                                    }
                                }
                            }
                        } else {
                            println!("[PICKUP] Item {} not found in zone", item_map_id);
                        }
                    }
                    session.set_player(player, session.clone()).await;
                }
                Ok(())
            }
            _ => {
                warn!("Unknown command: {}", msg.command);
                Ok(())
            }
        }
    }

    async fn sub_command_30(session: &SessionArc, mut msg: Message) -> anyhow::Result<()> {
        let type_byte = msg.read_byte()?;
        match type_byte {
            16 => {
                let type_increment = msg.read_byte()?;
                let point = msg.read_short()?;
                if let Some(mut player) = session.take_player().await {
                    match player.n_point.increase_point(type_increment as u8, point) {
                        Ok(_) => {
                            use crate::services::player_info_service;
                            player_info_service::send_point_info(&player).await?;
                        }
                        Err(e) => {
                            use crate::services::ServiceHandles;
                            ServiceHandles::send_message_alert(&player, e)?;
                        }
                    }
                    session.set_player(player, session.clone()).await;
                }
            }
            64 => {}
            _ => {
                warn!("Unknown type for -30 command: {}", type_byte);
            }
        }
        Ok(())
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
                warn!("Unknown type for -74 command: {}", type_byte);
            }
        }

        Ok(())
    }

    #[instrument(skip(session, msg))]
    async fn handle_message_not_login(session: &SessionArc, mut msg: Message) -> Result<()> {
        let sub_cmd = msg.read_byte()?;
        debug!("Handling -29 sub-command: {}", sub_cmd);
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
                        info!("Client platform={} version={}", platform, version);
                    } else {
                        warn!("Invalid client version string: {}", version_str);
                    }
                }

                session.set_zoom_level(zoom_level as u8).await;

                DataGame::send_link_ip(session).await?;
            }

            _ => {
                warn!("Unknown sub-command for -29: {}", sub_cmd);
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

        let has_old_session = (&*SESSION_MANAGER).is_online(player_id as i64);
        if has_old_session {
            info!(
                "[LOGIN] Found old session for player {}, kicking old session",
                player_id
            );
            (&*SESSION_MANAGER)
                .kick_player(player_id as i64, "Tai khoan dang nhap o noi khac")
                .await;
            info!(
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
            let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
            if let Some(zone) = zone_manager.get_best_zone(player_with_zone.map_id as i32) {
                player_with_zone.zone_id = zone.zone_id;
                // let mob_count = zone.active_mobs.read().unwrap().len();
                if let Err(e) = zone.add_player(player_with_zone.clone()) {
                    error!("Error adding player to zone: {:?}", e);
                } else {
                    info!(
                        "Player {} added to zone {} map {}",
                        player_with_zone.name, zone.zone_id, zone.map_id
                    );
                    zone.load_another_to_me(player_with_zone.id);
                    zone.load_me_to_another(player_with_zone.id);
                    zone.map_info(session, player_with_zone.id);
                }
            } else {
                warn!(
                    "[LOGIN] No zone found for map {}, using default zone_id 0",
                    player_with_zone.map_id
                );
                player_with_zone.zone_id = 0;
            }
        }

        session
            .set_player(player_with_zone.clone(), session.clone())
            .await;
        {
            (&*SESSION_MANAGER).add_session(player_id as i64, session.clone());
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
                    ServiceHandles::send_message_alert_session(session, &text)?;
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
                        error!("Error getting player: {:?}", e);
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
        debug!("[LEGACY LOGIN] Step 1: send_small_version (-77)");
        DataGame::send_small_version(session).await?;
        debug!("[LEGACY LOGIN] Step 2: send_message_93 (-93)");
        Self::send_message_93(session).await?;
        debug!("[LEGACY LOGIN] Step 3: send_version_game (-28)");
        DataGame::send_version_game(session).await?;
        debug!("[LEGACY LOGIN] Step 4: send_data_item_bg (-31)");
        DataGame::send_data_item_bg(session).await?;
        debug!("[LEGACY LOGIN] Step 5: send_all_player_info");
        player_info_service::send_all_player_info(session).await?;
        info!("[LEGACY LOGIN] All login data sent!");
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
        session.transmit(Message::new(2));
        Ok(())
    }

    async fn handle_create_char(session: &SessionArc, mut msg: Message) -> anyhow::Result<()> {
        let name = msg.read_utf()?;
        let gender = msg.read_byte()?;
        let hair = msg.read_byte()?;

        if name.len() < 5 || name.len() > 12 {
            return Err(anyhow!("Tên nhân vật phải từ 5 đến 12 ký tự"));
        }
        auth_service::name_is_taken(&name).await?;

        let account_id = session.get_user_id().await.unwrap_or(0);
        let db = DbManager::get_pool();

        let player_result =
            auth_service::create_new_player(account_id, &name, gender as i32, hair as i32).await;

        match player_result {
            Ok(db_player) => {
                info!("Character created successfully: {}", name);
                let rt_player = crate::player::player_mapper::from_entity(&db_player)
                    .await
                    .map_err(|e| anyhow!("Failed to build runtime player: {}", e))?;

                if let Ok(Some(account)) = AccountDao::get_account_by_id(db, account_id).await {
                    Self::initialize_logged_in_session(session, account, rt_player).await?;
                } else {
                    return Err(anyhow!(
                        "Failed to retrieve account after character creation"
                    ));
                }
            }
            Err(e) => {
                error!("Error creating character: {:?}", e);
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
                warn!("Unknown -28 sub-command: {}", sub_cmd);
                Ok(())
            }
        }
    }

    async fn handle_client_ok_enhanced(session: &SessionArc) -> anyhow::Result<()> {
        let player_opt = session.get_player().await;
        if let Some(player) = player_opt {
            player_info_service::send_player_blob_internal(&player).await?;
            info!(
                "Client ok enhanced initialization completed for player: {}",
                player.name
            );
        } else {
            debug!("Client ok enhanced: Player not set yet, ignoring");
        }
        Ok(())
    }

    async fn handle_player_move(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _can_fly = msg.read_byte()?;
        let to_x = msg.read_short()?;
        let to_y_result = msg.read_short();

        if let Some(mut player) = session.take_player().await {
            if player.is_die() {
                session.set_player(player, session.clone()).await;
                return Ok(());
            }

            let final_y = match to_y_result {
                Ok(y) => y,
                Err(_) => player.location.y,
            };

            player.location.x = to_x;
            player.location.y = final_y;

            let zone_opt =
                crate::map::zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id);
            if let Some(zone) = zone_opt {
                Self::send_player_move_to_zone(&player, &zone).await?;
            }

            session.set_player(player, session.clone()).await;
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
        ServiceHandles::send_mess_another_not_me_in_map(player, msg)?;

        Ok(())
    }
}
