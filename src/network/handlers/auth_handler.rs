use crate::account::account_dao::AccountDao;
use crate::account::account_services::AccountServices;
use crate::clan::clan_service::ClanService;
use crate::data::data_game::DataGame;
use crate::database::DbManager;
use crate::entities::{account, player};
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::network::SESSION_MANAGER;
use crate::player::player_actor::{PlayerActor, PlayerHandle, PlayerMessage};
use crate::player::{player_mapper, Player};
use crate::account::auth_service;
use crate::services::{player_info_service, ServiceHandles};
use anyhow::{anyhow, Result};
use chrono::Utc;
use sea_orm::*;
use tracing::{debug, error, info, warn};

pub struct AuthHandler;

impl AuthHandler {
    pub async fn handle_not_login(session: &SessionArc, mut msg: Message) -> Result<()> {
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

    pub async fn handle_not_login_alt(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _username_len = msg.read_byte()? as usize;
        let _password_len = msg.read_byte()? as usize;
        let version = msg.read_int()?;
        let username = msg.read_utf()?;
        let password = msg.read_utf()?;
        session.set_version(version).await;
        Self::handle_login_authentication(session, &username, &password).await?;
        Ok(())
    }

    pub async fn handle_login_authentication(
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

                let seconds_pass_1 = now.signed_duration_since(last_login).num_seconds();
                let seconds_pass = now.signed_duration_since(last_logout).num_seconds();

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
                        let rt_player = player_mapper::from_entity(&db_player)
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

    pub async fn initialize_logged_in_session(
        session: &SessionArc,
        account: account::Model,
        mut player_with_zone: Player,
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
            account_data.last_time_login = Set(Utc::now());
            AccountDao::update_account(&pool, account_data).await?;
        }

        let zone_handle = {
            let zone_manager = &ZONE_MANAGER;
            if let Some(zone) = zone_manager.get_best_zone(player_with_zone.map_id as i32) {
                player_with_zone.zone_id = zone.zone_id;
                Some(zone)
            } else {
                warn!(
                    "[LOGIN] No zone found for map {}, using default zone_id 0",
                    player_with_zone.map_id
                );
                player_with_zone.zone_id = 0;
                None
            }
        };

        session
            .set_player(player_with_zone.clone(), session.clone())
            .await;

        if let Some(handle) = session.get_player_handle().await {
            ClanService::add_player_to_clan_online(&player_with_zone, handle).await;
        }

        {
            (&*SESSION_MANAGER).add_session(player_id as i64, session.clone());
        }
        Self::send_login_success_data(session).await?;

        if let Some(zone) = zone_handle {
            if let Some(handle) = session.get_player_handle().await {
                if let Err(e) = zone.add_player(handle).await {
                    error!("Error adding player to zone: {:?}", e);
                } else {
                    info!(
                        "Player {} added to zone {} map {}",
                        player_with_zone.name, zone.zone_id, zone.map_id
                    );
                    zone.map_info(
                        session.clone(),
                        player_id,
                        player_with_zone.location.x,
                        player_with_zone.location.y,
                        Some((
                            crate::services::task_utils::TaskUtils::get_id_task(&player_with_zone),
                            crate::services::task_utils::TaskUtils::get_task_index(
                                &player_with_zone,
                            ),
                        )),
                        player_with_zone.spaceship_id,
                    )
                    .await?;
                }
            } else {
                error!(
                    "Failed to get player handle from session for player {}",
                    player_id
                );
            }
        } else {
            error!(
                "[LOGIN] zone_handle is NONE for player {}, MAP_INFO skipped!",
                player_id
            );
        }
        Ok(())
    }

    pub async fn send_login_success_data(session: &SessionArc) -> Result<()> {
        DataGame::send_small_version(session).await?;
        Self::send_message_93(session)?;
        DataGame::send_version_game(session).await?;
        DataGame::send_data_item_bg(session).await?;
        player_info_service::send_all_player_info(session).await?;
        info!("[LEGACY LOGIN] All login data sent!");
        Ok(())
    }

    fn send_message_93(session: &SessionArc) -> Result<()> {
        let mut msg = Message::new(-93);
        msg.write_utf("1630679752231_-93_r")?;
        session.transmit(msg);
        Ok(())
    }

    pub async fn switch_to_create_char(session: &SessionArc) -> Result<()> {
        DataGame::send_data_item_bg(session).await?;
        DataGame::send_version_game(session).await?;
        DataGame::send_tile_set_info(session).await?;
        DataGame::update_data(session).await?;
        session.transmit(Message::new(2));
        Ok(())
    }

    pub async fn handle_create_char(session: &SessionArc, mut msg: Message) -> anyhow::Result<()> {
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
                let rt_player = player_mapper::from_entity(&db_player)
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
}
