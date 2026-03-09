use crate::clan::clan_manager::CLAN_MANAGER;
use crate::clan::message::ClanMessage;
use crate::database::DbManager;
use crate::models::clan::{Clan, ClanMember, ClanMessage as ClanMsg};
use crate::network::message::Message;
use crate::player::player_actor::{PlayerHandle, PlayerMessage};
use crate::player::player_manager::PLAYER_MANAGER;
use crate::player::Player;
use crate::services::player_info_service;
use crate::services::services::ServiceHandles;
use sea_orm::{EntityTrait, Set};
use std::sync::Arc;
use tracing::{info, warn};

// Constants
const CHAT: i8 = 0;
const ASK_FOR_PEA: i8 = 1;
const ASK_FOR_JOIN_CLAN: i8 = 2;

const RED: i8 = 1;
const BLACK: i8 = 0;

// Clan sub-commands (cmd -46)
const REQUEST_FLAGS_CHOOSE_CREATE_CLAN: i8 = 1;
const ACCEPT_CREATE_CLAN: i8 = 2;
const REQUEST_FLAGS_CHOOSE_CHANGE_CLAN: i8 = 3;
const ACCEPT_CHANGE_INFO_CLAN: i8 = 4;

pub struct ClanService;

impl ClanService {
    // ==================== ONLINE MEMBER MANAGEMENT ====================

    pub async fn add_player_to_clan_online(player: &Player, handle: PlayerHandle) {
        if player.clan_id != -1 {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
                clan_handle.add_member_online(handle);
                clan_handle.update_power(player.id as i32, player.n_point.power);
            }
        }
    }

    pub async fn remove_player_from_clan_online(player_id: u64, clan_id: i32) {
        if clan_id != -1 {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                clan_handle.remove_member_online(player_id);
            }
        }
    }

    pub fn get_clan_by_id(clan_id: i32) -> Option<super::handle::ClanHandle> {
        CLAN_MANAGER.get_clan(clan_id)
    }

    // ==================== SEND MY CLAN (-53) ====================
    pub async fn send_my_clan(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            let mut msg = Message::new(-53);
            msg.write_int(-1)?;
            player.send_to_client(msg);
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };
            let mut msg = Message::new(-53);
            msg.write_int(clan.id)?;
            msg.write_utf(&clan.name)?;
            msg.write_utf(&clan.slogan)?;
            msg.write_byte(clan.img_id as i8)?;
            msg.write_utf(&clan.power_point.to_string())?;
            msg.write_utf(&clan.get_leader_name())?;
            msg.write_byte(clan.get_curr_members())?;
            msg.write_byte(clan.max_member)?;
            msg.write_byte(clan.get_role(player.id as i32))?;
            msg.write_int(clan.capsule_clan)?;
            msg.write_byte(clan.level as i8)?;

            // Members
            for member in &clan.members {
                msg.write_int(member.id)?;
                msg.write_short(member.head)?;
                msg.write_short(-1)?;
                msg.write_short(member.leg)?;
                msg.write_short(member.body)?;
                msg.write_utf(&member.name)?;
                msg.write_byte(member.role)?;
                msg.write_utf(&crate::utils::number_util::number_to_money(
                    member.power_point,
                ))?;
                msg.write_int(member.donate)?;
                msg.write_int(member.receive_donate)?;
                msg.write_int(member.clan_point)?;
                msg.write_int(member.member_point)?;
                msg.write_int(member.join_time)?;
            }

            // Messages
            let messages = &clan.clan_messages;
            msg.write_byte(messages.len() as i8)?;
            for cmg in messages {
                msg.write_byte(cmg.message_type)?;
                msg.write_int(cmg.id)?;
                msg.write_int(cmg.player_id)?;
                if cmg.message_type == 2 {
                    msg.write_utf(&format!(
                        "{} ({})",
                        cmg.player_name,
                        crate::utils::number_util::number_to_money(cmg.player_power)
                    ))?;
                } else {
                    msg.write_utf(&cmg.player_name)?;
                }
                msg.write_byte(cmg.role)?;
                msg.write_int(cmg.time)?;
                if cmg.message_type == 0 {
                    msg.write_utf(&cmg.text)?;
                    msg.write_byte(cmg.color)?;
                } else if cmg.message_type == 1 {
                    msg.write_byte(cmg.receive_donate)?;
                    msg.write_byte(cmg.max_donate)?;
                    msg.write_byte(cmg.is_new_message)?;
                }
            }
            player.send_to_client(msg);
        }
        Ok(())
    }

    // ==================== BROADCAST MESSAGE TO CLAN ====================
    pub async fn send_message_clan(clan_id: i32, cmg: ClanMsg) -> anyhow::Result<()> {
        let handles: Vec<PlayerHandle> = {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                if let Some(clan) = clan_handle.get_snapshot().await {
                    clan.members_online.clone()
                } else {
                    return Ok(());
                }
            } else {
                return Ok(());
            }
        };

        // Build message
        let mut msg = Message::new(-51);
        msg.write_byte(cmg.message_type)?;
        msg.write_int(cmg.id)?;
        msg.write_int(cmg.player_id)?;
        if cmg.message_type == 2 {
            msg.write_utf(&format!("{} ({})", cmg.player_name, cmg.player_power))?;
        } else {
            msg.write_utf(&cmg.player_name)?;
        }
        msg.write_byte(cmg.role)?;
        msg.write_int(cmg.time)?;
        if cmg.message_type == 0 {
            msg.write_utf(&cmg.text)?;
            msg.write_byte(cmg.color)?;
        } else if cmg.message_type == 1 {
            msg.write_byte(cmg.receive_donate)?;
            msg.write_byte(cmg.max_donate)?;
            msg.write_byte(cmg.is_new_message)?;
        }

        for handle in handles {
            handle.send_forget(PlayerMessage::SendPacket(msg.clone()));
        }
        Ok(())
    }

    // ==================== SEND MY CLAN FOR ALL MEMBERS ====================
    pub async fn send_my_clan_for_all_members(clan_id: i32) -> anyhow::Result<()> {
        let handles: Vec<PlayerHandle> = {
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                if let Some(clan) = clan_handle.get_snapshot().await {
                    clan.members_online.clone()
                } else {
                    return Ok(());
                }
            } else {
                return Ok(());
            }
        };

        for handle in handles {
            if let Some(snapshot) = handle.get_snapshot().await {
                Self::send_my_clan(&snapshot).await?;
            }
        }
        Ok(())
    }

    // ==================== CLAN MESSAGE HANDLER (-51) ====================
    pub async fn clan_message(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        let message_type = msg.read_byte()?;
        match message_type {
            CHAT => {
                let text = msg.read_utf()?;
                Self::chat(player, &text).await?;
            }
            ASK_FOR_PEA => {
                Self::ask_for_pea(player).await?;
            }
            ASK_FOR_JOIN_CLAN => {
                let clan_id = msg.read_int()?;
                Self::ask_for_join_clan(player, clan_id).await?;
            }
            _ => {}
        }
        Ok(())
    }

    // ==================== CHAT IN CLAN ====================
    pub async fn chat(player: &Player, text: &str) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };

            let cmg = ClanMsg {
                id: clan.clan_message_id + 1,
                message_type: 0,
                player_id: player.id as i32,
                player_name: player.name.clone(),
                player_power: player.n_point.power,
                role: clan.get_role(player.id as i32),
                time: (crate::utils::time::current_time_millis() / 1000) as i32,
                text: text.to_string(),
                receive_donate: 0,
                max_donate: 0,
                is_new_message: 0,
                color: BLACK,
            };
            clan_handle.add_message(cmg.clone());
            Self::send_message_clan(clan.id, cmg).await?;
        }
        Ok(())
    }

    // ==================== ASK FOR PEA ====================
    pub async fn ask_for_pea(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };

            // Check cooldown
            if let Some(member) = clan.members.iter().find(|m| m.id == player.id as i32) {
                let now = crate::utils::time::current_time_millis() as i64;
                if member.time_ask_pea + 5 * 60 * 1000 > now {
                    let wait = (member.time_ask_pea + 5 * 60 * 1000 - now) / 1000;
                    ServiceHandles::send_message_alert(
                        player,
                        &format!("Vui lòng chờ {} giây nữa", wait),
                    )?;
                    return Ok(());
                }
                // Update cooldown (via UpdateMemberPower or separate message if needed)
                // For simplicity now, let's assume we add a UpdateAskPeaTime message
                // clan_handle.send_forget(ClanMessage::UpdateAskPeaTime(player.id as i32, now));
            }

            let cmg = ClanMsg {
                id: clan.clan_message_id + 1,
                message_type: 1,
                player_id: player.id as i32,
                player_name: player.name.clone(),
                player_power: player.n_point.power,
                role: clan.get_role(player.id as i32),
                time: (crate::utils::time::current_time_millis() / 1000) as i32,
                text: String::new(),
                receive_donate: 0,
                max_donate: 5,
                is_new_message: 1,
                color: BLACK,
            };
            clan_handle.add_message(cmg.clone());
            Self::send_message_clan(clan.id, cmg).await?;
        }
        Ok(())
    }

    // ==================== ASK FOR JOIN CLAN ====================
    pub async fn ask_for_join_clan(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đang ở trong bang")?;
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };

            // Check if already asked
            let already_asked = clan
                .clan_messages
                .iter()
                .any(|c| c.message_type == 2 && c.player_id == player.id as i32 && c.role == -1);
            if already_asked {
                ServiceHandles::send_message_alert(player, "Bạn đã gửi yêu cầu rồi")?;
                return Ok(());
            }

            let cmg = ClanMsg {
                id: clan.clan_message_id + 1,
                message_type: 2,
                player_id: player.id as i32,
                player_name: player.name.clone(),
                player_power: player.n_point.power,
                role: -1,
                time: (crate::utils::time::current_time_millis() / 1000) as i32,
                text: String::new(),
                receive_donate: 0,
                max_donate: 0,
                is_new_message: 1,
                color: BLACK,
            };
            clan_handle.add_message(cmg.clone());
            Self::send_message_clan(clan_id, cmg).await?;
            ServiceHandles::send_message_alert(player, "Đã gửi yêu cầu gia nhập")?;
        } else {
            ServiceHandles::send_message_alert(player, "Không tìm thấy bang")?;
        }
        Ok(())
    }

    // ==================== CLAN REMOTE (KICK/PROMOTE/DEMOTE) (-55) ====================
    pub async fn clan_remote(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        let member_id = msg.read_int()?;
        let role = msg.read_byte()?;

        match role {
            -1 => Self::kick_out(player, member_id).await?,
            0 => Self::transfer_leader(player, member_id).await?,
            1 => Self::promote_deputy(player, member_id).await?,
            2 => Self::demote_member(player, member_id).await?,
            _ => {}
        }
        Ok(())
    }

    // ==================== KICK OUT ====================
    pub async fn kick_out(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };
            let my_role = clan.get_role(player.id as i32);
            let target_role = clan.get_role(member_id);

            // Only leader can kick deputy, leader/deputy can kick members
            if my_role != Clan::LEADER && !(my_role == Clan::DEPUTY && target_role == Clan::MEMBER)
            {
                ServiceHandles::send_message_alert(player, "Bạn không có quyền")?;
                return Ok(());
            }

            let member_name = clan
                .members
                .iter()
                .find(|m| m.id == member_id)
                .map(|m| m.name.clone())
                .unwrap_or_default();

            clan_handle.send_forget(ClanMessage::KickMember(member_id));

            let kicked_handle = clan
                .members_online
                .iter()
                .find(|h| h.id == member_id as u64)
                .cloned();

            let cmg = ClanMsg {
                id: clan.clan_message_id + 1,
                message_type: 0,
                player_id: player.id as i32,
                player_name: player.name.clone(),
                player_power: player.n_point.power,
                role: my_role,
                time: (crate::utils::time::current_time_millis() / 1000) as i32,
                text: format!("Đuổi {} ra khỏi bang.", member_name),
                receive_donate: 0,
                max_donate: 0,
                is_new_message: 0,
                color: RED,
            };
            clan_handle.add_message(cmg.clone());

            // Update kicked player's clan_id in DB
            Self::update_player_clan_id(member_id, -1).await?;

            // Notify kicked player if online
            if let Some(handle) = kicked_handle {
                handle.send_forget(crate::player::player_actor::message::PlayerMessage::Modify(
                    Box::new(|p| p.clan_id = -1),
                ));
            }

            Self::send_message_clan(clan.id, cmg).await?;
            Self::send_my_clan_for_all_members(clan.id).await?;
        }
        Ok(())
    }

    // ==================== PROMOTE TO DEPUTY ====================
    pub async fn promote_deputy(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };
            let my_role = clan.get_role(player.id as i32);

            if my_role != Clan::LEADER {
                ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                return Ok(());
            }

            let target = clan.members.iter().find(|m| m.id == member_id);
            if let Some(member) = target {
                if member.role != Clan::MEMBER {
                    ServiceHandles::send_message_alert(player, "Không thể thực hiện")?;
                    return Ok(());
                }
                let member_name = member.name.clone();
                clan_handle.send_forget(ClanMessage::PromoteMember(member_id, Clan::DEPUTY));

                let cmg = ClanMsg {
                    id: clan.clan_message_id + 1,
                    message_type: 0,
                    player_id: player.id as i32,
                    player_name: player.name.clone(),
                    player_power: player.n_point.power,
                    role: Clan::LEADER,
                    time: (crate::utils::time::current_time_millis() / 1000) as i32,
                    text: format!("Phong phó bang cho {}", member_name),
                    receive_donate: 0,
                    max_donate: 0,
                    is_new_message: 0,
                    color: RED,
                };
                clan_handle.add_message(cmg.clone());
                Self::send_message_clan(clan.id, cmg).await?;
                Self::send_my_clan_for_all_members(clan.id).await?;
            }
        }
        Ok(())
    }

    // ==================== DEMOTE TO MEMBER ====================
    pub async fn demote_member(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };
            let my_role = clan.get_role(player.id as i32);

            if my_role != Clan::LEADER {
                ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                return Ok(());
            }

            if let Some(member) = clan.members.iter().find(|m| m.id == member_id) {
                if member.role != Clan::DEPUTY {
                    ServiceHandles::send_message_alert(player, "Không thể thực hiện")?;
                    return Ok(());
                }
                let member_name = member.name.clone();
                clan_handle.send_forget(ClanMessage::PromoteMember(member_id, Clan::MEMBER));

                let cmg = ClanMsg {
                    id: clan.clan_message_id + 1,
                    message_type: 0,
                    player_id: player.id as i32,
                    player_name: player.name.clone(),
                    player_power: player.n_point.power,
                    role: Clan::LEADER,
                    time: (crate::utils::time::current_time_millis() / 1000) as i32,
                    text: format!("Cắt chức phó bang của {}", member_name),
                    receive_donate: 0,
                    max_donate: 0,
                    is_new_message: 0,
                    color: RED,
                };
                clan_handle.add_message(cmg.clone());
                Self::send_message_clan(clan.id, cmg).await?;
                Self::send_my_clan_for_all_members(clan.id).await?;
            }
        }
        Ok(())
    }

    // ==================== TRANSFER LEADER ====================
    pub async fn transfer_leader(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };

            if !clan.is_leader(player.id as i32) {
                ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                return Ok(());
            }

            let target_role = clan.get_role(member_id);
            if target_role != Clan::DEPUTY {
                ServiceHandles::send_message_alert(player, "Chỉ có thể nhường cho phó bang")?;
                return Ok(());
            }

            let new_leader_name = clan
                .members
                .iter()
                .find(|m| m.id == member_id)
                .map(|m| m.name.clone())
                .unwrap_or_default();

            clan_handle.send_forget(ClanMessage::PromoteMember(player.id as i32, Clan::MEMBER));
            clan_handle.send_forget(ClanMessage::PromoteMember(member_id, Clan::LEADER));

            let cmg = ClanMsg {
                id: clan.clan_message_id + 1,
                message_type: 0,
                player_id: player.id as i32,
                player_name: player.name.clone(),
                player_power: player.n_point.power,
                role: Clan::LEADER,
                time: (crate::utils::time::current_time_millis() / 1000) as i32,
                text: format!("Nhường chức bang chủ cho {}", new_leader_name),
                receive_donate: 0,
                max_donate: 0,
                is_new_message: 0,
                color: RED,
            };
            clan_handle.add_message(cmg.clone());
            Self::send_message_clan(clan.id, cmg).await?;
            Self::send_my_clan_for_all_members(clan.id).await?;
        }
        Ok(())
    }

    // ==================== LEAVE CLAN ====================
    pub async fn leave_clan(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else {
                return Ok(());
            };

            if clan.is_leader(player.id as i32) {
                ServiceHandles::send_message_alert(player, "Phải nhường chức bang chủ trước")?;
                return Ok(());
            }

            let role = clan.get_role(player.id as i32);
            clan_handle.send_forget(ClanMessage::LeaveClan(player.id));

            let cmg = ClanMsg {
                id: clan.clan_message_id + 1,
                message_type: 0,
                player_id: player.id as i32,
                player_name: player.name.clone(),
                player_power: player.n_point.power,
                role,
                time: (crate::utils::time::current_time_millis() / 1000) as i32,
                text: format!("{} đã rời bang.", player.name),
                receive_donate: 0,
                max_donate: 0,
                is_new_message: 0,
                color: RED,
            };
            clan_handle.add_message(cmg.clone());
            Self::update_player_clan_id(player.id as i32, -1).await?;
            Self::send_message_clan(clan.id, cmg).await?;
            Self::send_my_clan_for_all_members(clan.id).await?;
        }
        Ok(())
    }

    pub async fn update_player_clan_id(player_id: i32, clan_id: i32) -> anyhow::Result<()> {
        use crate::entities::player;
        let db = DbManager::get_pool();
        player::Entity::update(player::ActiveModel {
            id: Set(player_id),
            clan_id: Set(clan_id),
            ..Default::default()
        })
        .exec(db)
        .await?;
        Ok(())
    }

    // ==================== JOIN CLAN ====================
    pub async fn join_clan(
        player_handle: PlayerHandle,
        clan_id: i32,
        role: i8,
    ) -> anyhow::Result<()> {
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
            if let Some(snapshot) = player_handle.get_snapshot().await {
                let member = ClanMember {
                    id: snapshot.id as i32,
                    name: snapshot.name.clone(),
                    head: snapshot.get_head(),
                    body: snapshot.get_body(),
                    leg: snapshot.get_leg(),
                    role,
                    power_point: snapshot.n_point.power,
                    donate: 0,
                    receive_donate: 0,
                    member_point: 0,
                    clan_point: 0,
                    join_time: (crate::utils::time::current_time_millis() / 1000) as i32,
                    time_ask_pea: 0,
                };

                clan_handle.send_forget(ClanMessage::AddMember(member));

                // Update player actor
                player_handle.send_forget(
                    crate::player::player_actor::message::PlayerMessage::Modify(Box::new(
                        move |p| p.clan_id = clan_id,
                    )),
                );

                Self::update_player_clan_id(snapshot.id as i32, clan_id).await?;

                let cmg = ClanMsg {
                    id: 0, // Actor will increment
                    message_type: 0,
                    player_id: snapshot.id as i32,
                    player_name: snapshot.name.clone(),
                    player_power: snapshot.n_point.power,
                    role,
                    time: (crate::utils::time::current_time_millis() / 1000) as i32,
                    text: format!("Chào mừng {} đã gia nhập bang!", snapshot.name),
                    receive_donate: 0,
                    max_donate: 0,
                    is_new_message: 0,
                    color: RED,
                };
                clan_handle.add_message(cmg.clone());
                Self::send_my_clan_for_all_members(clan_id).await?;
            }
        }
        Ok(())
    }

    pub async fn join_clan_controller(
        player_handle: PlayerHandle,
        mut msg: Message,
    ) -> anyhow::Result<()> {
        let clan_id = msg.read_int()?;
        Self::join_clan(player_handle, clan_id, Clan::MEMBER).await
    }

    // ==================== SEND CLAN LIST (-47) ====================
    pub async fn send_clan_list(player: &Player, name: &str) -> anyhow::Result<()> {
        let clan_handles = CLAN_MANAGER.search_clans(name).await;
        let mut msg = Message::new(-47);
        msg.write_byte(clan_handles.len() as i8)?;
        for handle in clan_handles {
            if let Some(clan) = handle.get_snapshot().await {
                msg.write_int(clan.id)?;
                msg.write_utf(&clan.name)?;
                msg.write_utf(&clan.slogan)?;
                msg.write_byte(clan.img_id as i8)?;
                msg.write_utf(&clan.power_point.to_string())?;
                msg.write_utf(&clan.get_leader_name())?;
                msg.write_byte(clan.get_curr_members())?;
                msg.write_byte(clan.max_member)?;
                msg.write_int(clan.create_time)?;
            }
        }
        player.send_to_client(msg);
        Ok(())
    }

    // ==================== SEND MEMBER LIST (-48) ====================
    pub async fn send_member_list(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
            if let Some(clan) = clan_handle.get_snapshot().await {
                let mut msg = Message::new(-48);
                msg.write_byte(clan.members.len() as i8)?;
                for member in &clan.members {
                    msg.write_int(member.id)?;
                    msg.write_short(member.head)?;
                    msg.write_short(-1)?; // head icon
                    msg.write_short(member.leg)?;
                    msg.write_short(member.body)?;
                    msg.write_utf(&member.name)?;
                    msg.write_byte(member.role)?;
                    msg.write_utf(&crate::utils::number_util::number_to_money(
                        member.power_point,
                    ))?;
                    msg.write_int(member.donate)?;
                    msg.write_int(member.receive_donate)?;
                    msg.write_int(member.clan_point)?;
                    msg.write_int(member.member_point)?;
                    msg.write_int(member.join_time)?;
                }
                player.send_to_client(msg);
            }
        }
        Ok(())
    }

    // ==================== CLAN INVITE (-49) ====================
    pub async fn clan_invite(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }
        let target_id = msg.read_int()?;
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            if let Some(clan) = clan_handle.get_snapshot().await {
                let role = clan.get_role(player.id as i32);
                if role != Clan::LEADER && role != Clan::DEPUTY {
                    ServiceHandles::send_message_alert(player, "Bạn không có quyền mời!")?;
                    return Ok(());
                }

                if clan.get_curr_members() >= clan.max_member {
                    ServiceHandles::send_message_alert(player, "Bang hội đã đầy!")?;
                    return Ok(());
                }

                if let Some(target_handle) = PLAYER_MANAGER.get(target_id as u64) {
                    if let Some(target_snap) = target_handle.get_snapshot().await {
                        if target_snap.clan_id != -1 {
                            ServiceHandles::send_message_alert(
                                player,
                                "Người này đã có bang hội!",
                            )?;
                            return Ok(());
                        }

                        // Send invite packet
                        let mut msg_invite = Message::new(-49);
                        msg_invite.write_int(player.id as i32)?;
                        msg_invite.write_utf(&player.name)?;
                        msg_invite.write_int(clan.id)?;
                        msg_invite.write_utf(&clan.name)?;
                        msg_invite.write_int(target_id)?;
                        target_handle.send_forget(
                            crate::player::player_actor::message::PlayerMessage::SendPacket(
                                msg_invite,
                            ),
                        );

                        clan_handle.send_forget(ClanMessage::AddInvite(target_id));
                        ServiceHandles::send_message_alert(
                            player,
                            &format!("Đã gửi lời mời vào bang cho {}", target_snap.name),
                        )?;
                    }
                } else {
                    ServiceHandles::send_message_alert(player, "Người này không online!")?;
                }
            }
        }
        Ok(())
    }

    // ==================== CLAN DONATE (-50) ====================
    pub async fn clan_donate(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }
        let pea_count = msg.read_byte()?;
        if pea_count <= 0 {
            return Ok(());
        }

        // Logic for donation (pea count and updating clan messages)
        // This usually involves checking IF there is a request for pea from someone else
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            if let Some(clan) = clan_handle.get_snapshot().await {
                // Find first pea request
                let mut found_request = None;
                for cmg in &clan.clan_messages {
                    if cmg.message_type == 1 && cmg.receive_donate < cmg.max_donate {
                        found_request = Some(cmg.clone());
                        break;
                    }
                }

                if let Some(mut req) = found_request {
                    // TODO: Deduct pea from player and give to receiver
                    // For now, let's just update the clan state
                    req.receive_donate += 1;
                    clan_handle.update_message(req.clone());
                    clan_handle.send_forget(ClanMessage::UpdateDonate(player.id as i32, 1, 10)); // +10 clan point

                    ServiceHandles::send_message_alert(player, "Đã cho đậu thành công!")?;
                    Self::send_my_clan_for_all_members(clan.id).await?;
                } else {
                    ServiceHandles::send_message_alert(player, "Không có yêu cầu xin đậu nào!")?;
                }
            }
        }
        Ok(())
    }

    // ==================== GET CLAN INFO (-46) ====================
    pub async fn get_clan(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        let action = msg.read_byte()?;
        match action {
            REQUEST_FLAGS_CHOOSE_CREATE_CLAN => {
                // For now, let's send a mock list of flags if FlagBagService is missing
                // Or just send a hardcoded response matching the client expectation
                let mut msg_res = Message::new(-46);
                msg_res.write_byte(1)?; // type
                msg_res.write_byte(5)?; // size
                for i in 1..=5 {
                    msg_res.write_byte(i as i8)?; // id
                    msg_res.write_utf(&format!("Cờ bang {}", i))?;
                    msg_res.write_int(2_000_000)?; // gold
                    msg_res.write_int(0)?; // gem
                }
                player.send_to_client(msg_res);
            }
            ACCEPT_CREATE_CLAN => {
                let img_id = msg.read_byte()?;
                let name = msg.read_utf()?;
                Self::create_clan(player, img_id, &name).await?;
            }
            _ => {
                warn!("Unknown clan action: {}", action);
            }
        }
        Ok(())
    }

    pub async fn create_clan(player: &Player, img_id: i8, name: &str) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đang ở trong bang hội")?;
            return Ok(());
        }

        if name.len() < 5 || name.len() > 30 {
            ServiceHandles::send_message_alert(player, "Tên bang hội từ 5 đến 30 ký tự")?;
            return Ok(());
        }

        // Check fee (Fixed 2,000,000 gold)
        let fee = 2_000_000;
        if player.inventory.gold < fee {
            ServiceHandles::send_message_alert(
                player,
                &format!(
                    "Bạn không đủ vàng, còn thiếu {} vàng",
                    crate::utils::number_util::number_to_money(fee - player.inventory.gold)
                ),
            )?;
            return Ok(());
        }

        // Deduct gold
        let fee = 2_000_000;
        if let Some(h) = session_to_player_handle(player).await {
            h.send_forget(crate::player::player_actor::message::PlayerMessage::Modify(
                Box::new(move |p| {
                    p.inventory.sub_gold(fee);
                }),
            ));
            player_info_service::send_info_hp_mp_money(player)?;
        }

        // Create Clan Model
        let db = DbManager::get_pool();
        let mut clan = Clan::new();
        clan.name = name.to_string();
        clan.img_id = img_id as i32;
        clan.create_time = (crate::utils::time::current_time_millis() / 1000) as i32;

        // Add leader (the player)
        let leader = ClanMember {
            id: player.id as i32,
            name: player.name.clone(),
            head: player.get_head(),
            body: player.get_body(),
            leg: player.get_leg(),
            role: Clan::LEADER,
            power_point: player.n_point.power,
            donate: 0,
            receive_donate: 0,
            member_point: 0,
            clan_point: 0,
            join_time: clan.create_time,
            time_ask_pea: 0,
        };
        clan.add_member(leader);

        // Save to DB and get ID
        use crate::entities::clan as clan_entity;
        use sea_orm::ActiveValue::Set;

        let members_json = serde_json::to_string(&clan.members).unwrap_or_default();
        let active_model = clan_entity::ActiveModel {
            name: Set(clan.name.clone()),
            slogan: Set(clan.slogan.clone()),
            img_id: Set(clan.img_id),
            power_point: Set(clan.power_point),
            max_member: Set(clan.max_member as i16),
            level: Set(clan.level),
            members: Set(members_json),
            name_2: Set(clan.name_2.clone()),
            clan_point: Set(clan.capsule_clan),
            create_time: Set(chrono::Local::now()),
            tops: Set("[]".to_string()),
            ..Default::default()
        };

        let result = clan_entity::Entity::insert(active_model).exec(db).await?;
        clan.id = result.last_insert_id;

        // Add to manager
        CLAN_MANAGER.add_clan(clan.clone());

        // Update player's clan_id
        if let Some(h) = session_to_player_handle(player).await {
            let clan_id = clan.id;
            h.send_forget(crate::player::player_actor::message::PlayerMessage::Modify(
                Box::new(move |p| {
                    p.clan_id = clan_id;
                }),
            ));
            // Add as online member
            if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
                clan_handle.add_member_online(h);
            }
        }

        ServiceHandles::send_message_alert(player, "Chúc mừng bạn đã tạo bang thành công!")?;
        Self::send_my_clan(player).await?;

        Ok(())
    }
}

async fn session_to_player_handle(player: &Player) -> Option<PlayerHandle> {
    if let Some(ref session) = player.session {
        session.get_player_handle().await
    } else {
        if let Some(session) = crate::network::SESSION_MANAGER.get_session(player.id as i64) {
            return session.get_player_handle().await;
        }
        None
    }
}
