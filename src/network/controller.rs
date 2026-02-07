use crate::account::account_dao::AccountDao;
use crate::account::account_services::AccountServices;
use crate::clan::clan_service::ClanService;
use crate::combine::combine_service;
use crate::constant::cmd::cmd;
use crate::data::data_game::DataGame;
use crate::data::ItemData;
use crate::database::DbManager;
use crate::entities::{account, player};
use crate::item::{item_controller, type_item_inventory};
use crate::map::change_map_service::ChangeMapService;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::map::Zone;
use crate::network::SESSION_MANAGER;
use crate::npc::{self, npc_service};
use crate::player::player_actor::{
    pet::{message::PetMessage, PetStatus},
    PlayerMessage,
};
use crate::player::{player_mapper, Player};
use crate::services::auth_service;
use crate::services::{self, player_info_service, player_service, ServiceHandles};
use crate::shop::shop_services::shop_service;
use crate::templates::power_manager;
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

                if let Some(handle) = session.get_player_handle().await {
                    let mut handled = false;
                    handle.send_forget(PlayerMessage::AttackMob { mob_id });
                    handled = true;
                }
                Ok(())
            }
            cmd::GET_EFFECT_TEMPLATE => {
                let eff_id = msg.read_short()?;
                let id_t = eff_id;
                DataGame::send_effect_template(&session, eff_id, Some(id_t)).await?;

                Ok(())
            }
            cmd::GET_ITEM => {
                let type_byte = msg.read_byte()?;
                let type_inventory = type_item_inventory::TypeItemInventory::try_from(type_byte)?;
                let index = msg.read_byte()?;
                item_controller::ItemController::get_item(&session, type_inventory, index).await?;
                Ok(())
            }
            cmd::GET_CAPTIONS => {
                match msg.read_byte() {
                    Ok(gender) => {
                        let captions = power_manager::get_all_captions();
                        let mut response = Message::new(-41);
                        response.write_byte(captions.len() as i8)?;

                        let planet_name = match gender {
                            0 => "Trái Đất",
                            1 => "Namếc",
                            2 => "Xayda",
                            _ => "",
                        };

                        for caption in captions {
                            let name = caption.name.replace("{planet}", planet_name);
                            response.write_utf(&name)?;
                        }
                        session.transmit(response);
                    }
                    Err(e) => {
                        error!("Error reading byte {}", e);
                        return Ok(());
                    }
                }
                Ok(())
            }
            cmd::DO_ITEM => {
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
            cmd::NOT_LOGIN_ALT => {
                Self::handle_not_login(&session, msg).await?;
                Ok(())
            }
            cmd::GET_MOB_TEMPLATE => {
                let mob_id = msg.read_byte()?;
                DataGame::send_mob_temp(&session, mob_id).await?;
                Ok(())
            }
            cmd::NPC_SELECT => {
                let npc_id = msg.read_short()?;
                let select = msg.read_byte()?;
                npc_service::npc_service::handle_menu_confirm(&session, npc_id, select).await?;
                Ok(())
            }
            cmd::BUY_ITEM => {
                let type_shop = msg.read_byte()?;
                let temp_id = msg.read_short()?;
                if let Err(e) = shop_service::take_item_shop(&session, type_shop, temp_id).await {
                    error!("Shop Error: {:?}", e);
                }
                Ok(())
            }
            cmd::GET_IMAGE_BY_NAME => {
                let img_name = msg.read_utf()?;
                DataGame::send_image_by_name(&session, &img_name).await?;
                Ok(())
            }
            cmd::DAU_THAN_CONFIRM => {
                let _ = msg.read_byte()?;
                let select = msg.read_byte()?;
                npc_service::npc_service::handle_menu_confirm(&session, 4, select).await?;
                Ok(())
            }
            cmd::GET_ITEM_BG_TEMPLATE => {
                let bg_id = msg.read_short()?;
                DataGame::send_item_bg_template(&session, bg_id).await?;
                Ok(())
            }
            cmd::MAGIC_TREE => {
                let action = msg.read_byte()?;
                debug!("MagicTree action: {}", action);
                match action {
                    1 | 2 => {
                        if let Some(handle) = session.get_player_handle().await {
                            handle.send_forget(PlayerMessage::MagicTreeAction(action as u8));
                        }
                    }
                    _ => {}
                }
                Ok(())
            }
            cmd::RADAR => {
                let action = msg.read_byte()?;
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::RadarAction(action, msg));
                }
                Ok(())
            }
            cmd::CHECK_MOVE => {
                let _ = msg.read_int();
                Ok(())
            }
            cmd::GET_PLAYER_MENU => {
                let target_id = msg.read_int()?;
                if let Some(snapshot) = session.get_player_snapshot().await {
                    if let Some(zone) = ZONE_MANAGER.get_zone(snapshot.map_id, snapshot.zone_id) {
                        if let Some(target_handle) = zone.get_player(target_id as u64).await? {
                            if let Some(target_snapshot) = target_handle.get_snapshot().await {
                                ServiceHandles::send_player_menu(&snapshot, &target_snapshot)?;
                            }
                        }
                    }
                }
                Ok(())
            }
            cmd::GET_ICON => {
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
            cmd::NPC_MENU => {
                let npc_id = msg.read_short()?;
                npc_service::npc_service::open_menu_controller(&session, npc_id).await?;
                Ok(())
            }
            cmd::NOT_MAP => {
                Self::handle_message_not_map(&session, msg).await?;
                Ok(())
            }
            cmd::SHOW_INFO_PET => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::ShowInfoPet);
                }
                Ok(())
            }
            cmd::PET_CHANGE_STATUS => {
                let status_byte = msg.read_byte()?;
                if let Ok(status) = PetStatus::try_from(status_byte) {
                    if let Some(handle) = session.get_player_handle().await {
                        if status == PetStatus::Fusion {
                            handle.send_forget(PlayerMessage::Fusion {
                                type_fusion: 4,
                                template_id: 1,
                            });
                        } else {
                            handle
                                .send_forget(PlayerMessage::Pet(PetMessage::ChangeStatus(status)));
                        }
                    }
                }
                Ok(())
            }
            cmd::SELECT_SKILL => {
                if let Some(handle) = session.get_player_handle().await {
                    let skill_template_id = msg.read_short().unwrap_or(0);
                    handle.send_forget(PlayerMessage::SelectSkill {
                        skill_template_id: skill_template_id as i32,
                    });
                }
                Ok(())
            }
            cmd::CHAT => {
                let text = msg.read_utf()?;
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::Chat { text });
                }
                Ok(())
            }
            cmd::USE_SKILL => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::UseSkill { msg });
                }
                Ok(())
            }
            cmd::UPDATE_DATA => {
                if let Err(e) = DataGame::update_data(&session).await {
                    error!("Error updating data {}", e);
                }
                Ok(())
            }
            cmd::CHANGE_TYPE_PK => {
                let type_byte = msg.read_byte()?;
                match type_byte {
                    16 => {
                        let type_increment = msg.read_byte()?;
                        let point = msg.read_short()?;
                        if let Some(handle) = session.get_player_handle().await {
                            handle.send_forget(PlayerMessage::IncreasePoint {
                                type_increment: type_increment as u8,
                                point,
                            });
                        }
                    }
                    64 => {}
                    _ => {
                        warn!("Unknown type for -30 command: {}", type_byte);
                    }
                }
                Ok(())
            }
            cmd::FINISH_UPDATE => Ok(()),
            cmd::FINISH_LOAD_MAP => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::FinishLoadMap);
                }
                Ok(())
            }
            cmd::OPEN_ZONE_UI => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ChangeMapService::open_zone_ui(&snapshot).await?;
                }
                Ok(())
            }
            cmd::CHANGE_ZONE => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::NetworkMessage(msg));
                }
                Ok(())
            }
            cmd::PLAYER_ATTACK_PLAYER => {
                let player_id = msg.read_int()?;
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::AttackPlayer { player_id });
                }
                Ok(())
            }
            cmd::CHANGE_MAP_WAYPOINT | cmd::CHANGE_MAP_WAYPOINT_ALT => {
                if let Some(handle) = session.get_player_handle().await {
                    let _ = handle.send(PlayerMessage::NetworkMessage(msg)).await;
                }
                Ok(())
            }
            cmd::HOI_SINH => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::HoiSinh);
                }
                Ok(())
            }
            cmd::GO_HOME => {
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::NetworkMessage(msg));
                }
                Ok(())
            }
            cmd::CAPSULE_MENU => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ChangeMapService::open_capsule_menu(&snapshot)?;
                }
                Ok(())
            }
            cmd::PLAYER_MOVE => {
                Self::handle_player_move(&session, msg).await?;
                Ok(())
            }
            cmd::FLAG_BAG_ICON => Ok(()),
            cmd::INTRINSIC_MENU => {
                if let Some(handle) = session.get_player_handle().await {
                    services::IntrinsicService::show_menu(&session).await?;
                }
                Ok(())
            }
            cmd::SKILL_SHORTCUT_UPDATE => {
                let mut shortcuts = Vec::new();
                for _ in 0..10 {
                    shortcuts.push(msg.read_byte()?);
                }
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::UpdateSkillShortcuts { shortcuts });
                }
                Ok(())
            }
            cmd::COMBINE_INFO => {
                let _ = msg.read_byte()?;
                let len = msg.read_byte()?;
                let mut index_item = Vec::new();
                for _ in 0..len {
                    index_item.push(msg.read_byte()? as i16);
                }
                combine_service::show_info_combine(&session, index_item).await?;
                Ok(())
            }
            cmd::PICK_ITEM => {
                let item_map_id = msg.read_short()? as i32;
                if let Some(handle) = session.get_player_handle().await {
                    handle.send_forget(PlayerMessage::PickItem { item_map_id });
                }
                Ok(())
            }
            cmd::GET_MY_CLAN => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::send_my_clan(&snapshot).await?;
                }
                Ok(())
            }
            cmd::CLAN_MESSAGE => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::clan_message(&snapshot, msg).await?;
                }
                Ok(())
            }
            cmd::GET_CLAN_LIST => {
                let name = msg.read_utf()?;
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::send_clan_list(&snapshot, &name).await?;
                }
                Ok(())
            }
            cmd::GET_MEMBER_LIST => {
                let clan_id = msg.read_int()?;
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::send_member_list(&snapshot, clan_id).await?;
                }
                Ok(())
            }
            cmd::CLAN_REMOTE => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::clan_remote(&snapshot, msg).await?;
                }
                Ok(())
            }
            cmd::CLAN_INVITE => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::clan_invite(&snapshot, msg).await?;
                }
                Ok(())
            }
            cmd::CLAN_JOIN => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::join_clan(&snapshot, msg).await?;
                }
                Ok(())
            }
            cmd::CLAN_INFO => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::get_clan(&snapshot, msg).await?;
                }
                Ok(())
            }
            cmd::CLAN_DONATE => {
                if let Some(snapshot) = session.get_player_snapshot().await {
                    ClanService::clan_donate(&snapshot, msg).await?;
                }
                Ok(())
            }
            _ => {
                warn!("Unknown command: {}", msg.command);
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
            account_data.last_time_login = Set(Some(Utc::now()));
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
        if let Some(zone) = zone_handle {
            if let Some(handle) = session.get_player_handle().await {
                if let Err(e) = zone.add_player(handle).await {
                    error!("Error adding player to zone: {:?}", e);
                } else {
                    info!(
                        "Player {} added to zone {} map {}",
                        player_with_zone.name, zone.zone_id, zone.map_id
                    );
                    zone.map_info(session.clone(), player_id).await?;
                }
            } else {
                error!(
                    "Failed to get player handle from session for player {}",
                    player_id
                );
            }
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
        let player_opt = session.get_player_snapshot().await;
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

        if let Some(handle) = session.get_player_handle().await {
            let y = match to_y_result {
                Ok(y) => y,
                Err(_) => {
                    if let Some(snapshot) = session.get_player_snapshot().await {
                        snapshot.location.y
                    } else {
                        0
                    }
                }
            };
            handle.send(PlayerMessage::Move { x: to_x, y }).await?;
        }

        Ok(())
    }

    async fn send_player_move_to_zone(player: &Player, zone: &Zone) -> Result<()> {
        let mut msg = Message::new(-7);
        msg.write_int(player.id as i32)?;
        msg.write_short(player.location.x)?;
        msg.write_short(player.location.y)?;
        ServiceHandles::send_mess_another_not_me_in_map(player, msg)?;

        Ok(())
    }
}
