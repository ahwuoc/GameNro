use crate::clan::clan_manager::CLAN_MANAGER;
use crate::database::DbManager;
use crate::models::clan::{Clan, ClanMember, ClanMessage};
use crate::network::message::Message;
use crate::player::player_actor::PlayerHandle;
use crate::player::Player;
use crate::services::ServiceHandles;
use sea_orm::{EntityTrait, Set};
use std::sync::Arc;
use tokio::sync::RwLock;
use tracing::{info, warn};

// Constants
const CHAT: i8 = 0;
const ASK_FOR_PEA: i8 = 1;
const ASK_FOR_JOIN_CLAN: i8 = 2;

const RED: i8 = 1;
const BLACK: i8 = 0;

pub struct ClanService;

impl ClanService {
    // ==================== ONLINE MEMBER MANAGEMENT ====================
    // Pattern: Get lock, modify, release - NO broadcast inside

    pub async fn add_player_to_clan_online(player: &Player, handle: PlayerHandle) {
        if player.clan_id != -1 {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;
                clan.add_member_online(handle);
                // Update member power point
                if let Some(member) = clan.members.iter_mut().find(|m| m.id == player.id as i32) {
                    member.power_point = player.n_point.power;
                }
            }
        }
    }

    pub async fn remove_player_from_clan_online(player_id: u64, clan_id: i32) {
        if clan_id != -1 {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(clan_id) {
                let mut clan = clan_arc.write().await;
                clan.remove_member_online(player_id);
            }
        }
    }

    pub fn get_clan_by_id(clan_id: i32) -> Option<Arc<RwLock<Clan>>> {
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

        if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
            let clan = clan_arc.read().await;
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
    // Pattern: Read lock, collect handles, release, then send
    pub async fn send_message_clan(clan_id: i32, cmg: ClanMessage) -> anyhow::Result<()> {
        let handles: Vec<PlayerHandle> = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(clan_id) {
                let clan = clan_arc.read().await;
                clan.members_online.clone()
            } else {
                return Ok(());
            }
        };

        // Build message OUTSIDE of lock
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

        // Broadcast OUTSIDE of lock - no deadlock risk
        for handle in handles {
            handle.send_forget(
                crate::player::player_actor::message::PlayerMessage::SendPacket(msg.clone()),
            );
        }
        Ok(())
    }

    // ==================== SEND MY CLAN FOR ALL MEMBERS ====================
    pub async fn send_my_clan_for_all_members(clan_id: i32) -> anyhow::Result<()> {
        let handles: Vec<PlayerHandle> = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(clan_id) {
                let clan = clan_arc.read().await;
                clan.members_online.clone()
            } else {
                return Ok(());
            }
        };

        // Tell each player to refresh their clan info
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

        let (cmg, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;
                clan.clan_message_id += 1;
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
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
                clan.clan_messages.push(cmg.clone());
                if clan.clan_messages.len() > 20 {
                    clan.clan_messages.remove(0);
                }
                (cmg, clan.id)
            } else {
                return Ok(());
            }
        };
        // Broadcast OUTSIDE of lock
        Self::send_message_clan(clan_id, cmg).await?;
        Ok(())
    }

    // ==================== ASK FOR PEA ====================
    pub async fn ask_for_pea(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (cmg, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

                // Check cooldown
                if let Some(member) = clan.members.iter_mut().find(|m| m.id == player.id as i32) {
                    let now = crate::utils::time::current_time_millis() as i64;
                    if member.time_ask_pea + 5 * 60 * 1000 > now {
                        let wait = (member.time_ask_pea + 5 * 60 * 1000 - now) / 1000;
                        ServiceHandles::send_message_alert(
                            player,
                            &format!("Vui lòng chờ {} giây nữa", wait),
                        )?;
                        return Ok(());
                    }
                    member.time_ask_pea = now;
                }

                clan.clan_message_id += 1;
                let role = clan.get_role(player.id as i32);
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
                    message_type: 1,
                    player_id: player.id as i32,
                    player_name: player.name.clone(),
                    player_power: player.n_point.power,
                    role,
                    time: (crate::utils::time::current_time_millis() / 1000) as i32,
                    text: String::new(),
                    receive_donate: 0,
                    max_donate: 5,
                    is_new_message: 1,
                    color: BLACK,
                };
                clan.clan_messages.push(cmg.clone());
                if clan.clan_messages.len() > 20 {
                    clan.clan_messages.remove(0);
                }
                (cmg, clan.id)
            } else {
                return Ok(());
            }
        };
        Self::send_message_clan(clan_id, cmg).await?;
        Ok(())
    }

    // ==================== ASK FOR JOIN CLAN ====================
    pub async fn ask_for_join_clan(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đang ở trong bang")?;
            return Ok(());
        }

        let cmg = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(clan_id) {
                let mut clan = clan_arc.write().await;

                // Check if already asked
                let already_asked = clan.clan_messages.iter().any(|c| {
                    c.message_type == 2 && c.player_id == player.id as i32 && c.role == -1
                });
                if already_asked {
                    ServiceHandles::send_message_alert(player, "Bạn đã gửi yêu cầu rồi")?;
                    return Ok(());
                }

                clan.clan_message_id += 1;
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
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
                clan.clan_messages.push(cmg.clone());
                if clan.clan_messages.len() > 20 {
                    clan.clan_messages.remove(0);
                }
                cmg
            } else {
                ServiceHandles::send_message_alert(player, "Không tìm thấy bang")?;
                return Ok(());
            }
        };
        Self::send_message_clan(clan_id, cmg).await?;
        ServiceHandles::send_message_alert(player, "Đã gửi yêu cầu gia nhập")?;
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

        let (cmg, clan_id, kicked_handle) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;
                let my_role = clan.get_role(player.id as i32);
                let target_role = clan.get_role(member_id);

                // Only leader can kick deputy, leader/deputy can kick members
                if my_role != Clan::LEADER
                    && !(my_role == Clan::DEPUTY && target_role == Clan::MEMBER)
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

                clan.remove_member(member_id);

                let kicked_handle = clan
                    .members_online
                    .iter()
                    .find(|h| h.id == member_id as u64)
                    .cloned();

                clan.remove_member_online(member_id as u64);

                clan.clan_message_id += 1;
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
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
                clan.clan_messages.push(cmg.clone());
                (cmg, clan.id, kicked_handle)
            } else {
                return Ok(());
            }
        };

        // Update kicked player's clan_id in DB
        Self::update_player_clan_id(member_id, -1).await?;

        // Notify kicked player if online
        if let Some(handle) = kicked_handle {
            handle.send_forget(crate::player::player_actor::message::PlayerMessage::Modify(
                Box::new(|p| p.clan_id = -1),
            ));
        }

        Self::send_message_clan(clan_id, cmg).await?;
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== PROMOTE TO DEPUTY ====================
    pub async fn promote_deputy(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (cmg, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;
                let my_role = clan.get_role(player.id as i32);

                if my_role != Clan::LEADER {
                    ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                    return Ok(());
                }

                let target = clan.members.iter_mut().find(|m| m.id == member_id);
                if let Some(member) = target {
                    if member.role != Clan::MEMBER {
                        ServiceHandles::send_message_alert(player, "Không thể thực hiện")?;
                        return Ok(());
                    }
                    let member_name = member.name.clone();
                    member.role = Clan::DEPUTY;

                    clan.clan_message_id += 1;
                    let cmg = ClanMessage {
                        id: clan.clan_message_id,
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
                    clan.clan_messages.push(cmg.clone());
                    (cmg, clan.id)
                } else {
                    return Ok(());
                }
            } else {
                return Ok(());
            }
        };

        Self::send_message_clan(clan_id, cmg).await?;
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== DEMOTE TO MEMBER ====================
    pub async fn demote_member(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (cmg, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;
                let my_role = clan.get_role(player.id as i32);

                if my_role != Clan::LEADER {
                    ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                    return Ok(());
                }

                let member_name = {
                    let target = clan.members.iter_mut().find(|m| m.id == member_id);
                    if let Some(member) = target {
                        if member.role != Clan::DEPUTY {
                            ServiceHandles::send_message_alert(player, "Không thể thực hiện")?;
                            return Ok(());
                        }
                        member.role = Clan::MEMBER;
                        member.name.clone()
                    } else {
                        return Ok(());
                    }
                };

                clan.clan_message_id += 1;
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
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
                clan.clan_messages.push(cmg.clone());
                (cmg, clan.id)
            } else {
                return Ok(());
            }
        };

        Self::send_message_clan(clan_id, cmg).await?;
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== TRANSFER LEADER ====================
    pub async fn transfer_leader(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (cmg, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

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

                // Swap roles
                for member in &mut clan.members {
                    if member.id == player.id as i32 {
                        member.role = Clan::MEMBER;
                    } else if member.id == member_id {
                        member.role = Clan::LEADER;
                    }
                }

                clan.clan_message_id += 1;
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
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
                clan.clan_messages.push(cmg.clone());
                (cmg, clan.id)
            } else {
                return Ok(());
            }
        };

        Self::send_message_clan(clan_id, cmg).await?;
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== LEAVE CLAN ====================
    pub async fn leave_clan(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (cmg, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

                if clan.is_leader(player.id as i32) {
                    ServiceHandles::send_message_alert(player, "Phải nhường chức bang chủ trước")?;
                    return Ok(());
                }

                let role = clan.get_role(player.id as i32);
                clan.remove_member(player.id as i32);
                clan.remove_member_online(player.id);

                clan.clan_message_id += 1;
                let cmg = ClanMessage {
                    id: clan.clan_message_id,
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
                clan.clan_messages.push(cmg.clone());
                (cmg, clan.id)
            } else {
                return Ok(());
            }
        };

        Self::update_player_clan_id(player.id as i32, -1).await?;
        Self::send_message_clan(clan_id, cmg).await?;
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== JOIN CLAN HANDLERS (-48) ====================
    pub async fn join_clan(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        let cmg_id = msg.read_int()?;
        let action = msg.read_byte()?;

        match action {
            0 => Self::accept_join_request(player, cmg_id).await?,
            1 => Self::reject_join_request(player, cmg_id).await?,
            _ => {}
        }
        Ok(())
    }

    pub async fn accept_join_request(player: &Player, cmg_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (new_member_id, new_member_name, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

                if !clan.is_leader(player.id as i32) {
                    ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                    return Ok(());
                }

                if clan.get_curr_members() >= clan.max_member {
                    ServiceHandles::send_message_alert(player, "Bang đã đủ thành viên")?;
                    return Ok(());
                }

                let cmg = clan
                    .clan_messages
                    .iter_mut()
                    .find(|c| c.id == cmg_id && c.message_type == 2);
                if let Some(req) = cmg {
                    let new_member_id = req.player_id;
                    let new_member_name = req.player_name.clone();

                    // Mark as processed
                    req.message_type = 0;
                    req.role = Clan::LEADER;
                    req.player_id = player.id as i32;
                    req.player_name = player.name.clone();
                    req.text = format!("Chấp nhận {} vào bang.", new_member_name);
                    req.color = RED;

                    // Add new member
                    let new_cm = ClanMember {
                        id: new_member_id,
                        name: new_member_name.clone(),
                        head: 0,
                        body: -1,
                        leg: -1,
                        role: Clan::MEMBER,
                        power_point: 0,
                        donate: 0,
                        receive_donate: 0,
                        member_point: 0,
                        clan_point: 0,
                        join_time: (crate::utils::time::current_time_millis() / 1000) as i32,
                        time_ask_pea: 0,
                    };
                    clan.add_member(new_cm);

                    (new_member_id, new_member_name, clan.id)
                } else {
                    ServiceHandles::send_message_alert(player, "Không tìm thấy yêu cầu")?;
                    return Ok(());
                }
            } else {
                return Ok(());
            }
        };

        // Update new member's clan_id in DB
        Self::update_player_clan_id(new_member_id, clan_id).await?;

        info!("Player {} accepted to clan {}", new_member_name, clan_id);
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    pub async fn reject_join_request(player: &Player, cmg_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let clan_id = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

                if !clan.is_leader(player.id as i32) {
                    ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                    return Ok(());
                }

                let cmg = clan
                    .clan_messages
                    .iter_mut()
                    .find(|c| c.id == cmg_id && c.message_type == 2);
                if let Some(req) = cmg {
                    let rejected_name = req.player_name.clone();

                    req.message_type = 0;
                    req.role = Clan::LEADER;
                    req.player_id = player.id as i32;
                    req.player_name = player.name.clone();
                    req.text = format!("Từ chối {} vào bang.", rejected_name);
                    req.color = RED;
                }
                clan.id
            } else {
                return Ok(());
            }
        };

        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== INVITE CLAN (-57) ====================
    pub async fn clan_invite(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        let action = msg.read_byte()?;
        match action {
            0 => {
                let target_id = msg.read_int()?;
                Self::send_invite(player, target_id).await?;
            }
            1 => {
                let clan_id = msg.read_int()?;
                Self::accept_invite(player, clan_id).await?;
            }
            _ => {}
        }
        Ok(())
    }

    pub async fn send_invite(player: &Player, target_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let (clan_name, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let clan = clan_arc.read().await;
                if !clan.is_leader(player.id as i32) && !clan.is_deputy(player.id as i32) {
                    ServiceHandles::send_message_alert(player, "Bạn không có quyền mời")?;
                    return Ok(());
                }
                if clan.get_curr_members() >= clan.max_member {
                    ServiceHandles::send_message_alert(player, "Bang đã đủ thành viên")?;
                    return Ok(());
                }
                (clan.name.clone(), clan.id)
            } else {
                return Ok(());
            }
        };

        // Send invite message to target player via zone
        let mut msg = Message::new(-57);
        msg.write_utf(&format!("{} mời bạn vào bang {}", player.name, clan_name))?;
        msg.write_int(clan_id)?;
        msg.write_int(758435)?; // code

        // Find target player in zone and send
        if let Some(zone) =
            crate::map::zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id)
        {
            if let Ok(Some(handle)) = zone.get_player(target_id as u64).await {
                handle.send_forget(
                    crate::player::player_actor::message::PlayerMessage::SendPacket(msg),
                );
            }
        }

        ServiceHandles::send_message_alert(player, "Đã gửi lời mời")?;
        Ok(())
    }

    pub async fn accept_invite(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đang trong bang khác")?;
            return Ok(());
        }

        {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(clan_id) {
                let mut clan = clan_arc.write().await;

                if clan.get_curr_members() >= clan.max_member {
                    ServiceHandles::send_message_alert(player, "Bang đã đủ thành viên")?;
                    return Ok(());
                }

                let new_cm = ClanMember {
                    id: player.id as i32,
                    name: player.name.clone(),
                    head: player.head,
                    body: player.body,
                    leg: player.leg,
                    role: Clan::MEMBER,
                    power_point: player.n_point.power,
                    donate: 0,
                    receive_donate: 0,
                    member_point: 0,
                    clan_point: 0,
                    join_time: (crate::utils::time::current_time_millis() / 1000) as i32,
                    time_ask_pea: 0,
                };
                clan.add_member(new_cm);
            } else {
                ServiceHandles::send_message_alert(player, "Không tìm thấy bang")?;
                return Ok(());
            }
        }

        Self::update_player_clan_id(player.id as i32, clan_id).await?;
        ServiceHandles::send_message_alert(player, "Bạn đã gia nhập bang")?;
        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== CLAN LIST (-47) ====================
    pub async fn send_clan_list(snapshot: &Player, name: &str) -> anyhow::Result<()> {
        let clans = CLAN_MANAGER.search_clans(name).await;
        let mut msg = Message::new(-47);
        msg.write_byte(clans.len() as i8)?;
        for clan in clans {
            let clan = clan.read().await;
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
        snapshot.send_to_client(msg);
        Ok(())
    }

    // ==================== MEMBER LIST (-50) ====================
    pub async fn send_member_list(snapshot: &Player, clan_id: i32) -> anyhow::Result<()> {
        if let Some(clan_arc) = CLAN_MANAGER.get_clan(clan_id) {
            let clan = clan_arc.read().await;
            let mut msg = Message::new(-50);
            msg.write_byte(clan.members.len() as i8)?;
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
            snapshot.send_to_client(msg);
        }
        Ok(())
    }

    // ==================== DB HELPER ====================
    async fn update_player_clan_id(player_id: i32, clan_id: i32) -> anyhow::Result<()> {
        use crate::entities::player;
        let db = DbManager::get_pool();

        let model = player::ActiveModel {
            id: Set(player_id),
            clan_id: Set(clan_id),
            ..Default::default()
        };

        player::Entity::update(model).exec(db).await?;
        Ok(())
    }

    // ==================== GET CLAN (-46) ====================
    // action 1: Request flag list for create clan
    // action 2: Create clan
    // action 3: Request flag list for change clan
    // action 4: Change clan info
    pub async fn get_clan(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        let action = msg.read_byte()?;
        match action {
            1 | 3 => {
                // Send flag list (simplified - just send OK)
                // FlagBagService.sendListFlagClan equivalent
                info!("Player {} requested flag list", player.name);
            }
            2 => {
                let img_id = msg.read_byte()?;
                let name = msg.read_utf()?;
                Self::create_clan(player, img_id as i32, &name).await?;
            }
            4 => {
                let img_id = msg.read_byte()?;
                let slogan = msg.read_utf()?;
                Self::change_info_clan(player, img_id as i32, &slogan).await?;
            }
            _ => {}
        }
        Ok(())
    }

    // ==================== CREATE CLAN ====================
    async fn create_clan(player: &Player, img_id: i32, name: &str) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đã có bang rồi")?;
            return Ok(());
        }

        if name.len() > 30 {
            ServiceHandles::send_message_alert(player, "Tên bang không quá 30 ký tự")?;
            return Ok(());
        }

        // TODO: Check and deduct gold/gem for flag
        // For now, skip cost check

        // Create new clan in database
        use crate::entities::clan as clan_entity;
        let db = DbManager::get_pool();

        // Get next clan ID
        let clans = clan_entity::Entity::find().all(db).await?;
        let next_id = clans.iter().map(|c| c.id).max().unwrap_or(0) + 1;

        let leader = ClanMember {
            id: player.id as i32,
            name: player.name.clone(),
            head: player.head,
            body: player.body,
            leg: player.leg,
            role: Clan::LEADER,
            power_point: player.n_point.power,
            donate: 0,
            receive_donate: 0,
            member_point: 0,
            clan_point: 0,
            join_time: (crate::utils::time::current_time_millis() / 1000) as i32,
            time_ask_pea: 0,
        };

        let members_json = serde_json::to_string(&vec![leader.clone()])?;

        let new_clan = clan_entity::ActiveModel {
            id: Set(next_id),
            name: Set(name.to_string()),
            name_2: Set(String::new()),
            slogan: Set(String::new()),
            img_id: Set(img_id),
            power_point: Set(player.n_point.power),
            max_member: Set(10),
            clan_point: Set(0),
            level: Set(1),
            members: Set(members_json),
            tops: Set(String::new()),
            create_time: Set(chrono::Local::now()),
        };

        clan_entity::Entity::insert(new_clan).exec(db).await?;

        // Create clan in memory
        let mut clan = Clan::new();
        clan.id = next_id;
        clan.name = name.to_string();
        clan.img_id = img_id;
        clan.power_point = player.n_point.power;
        clan.add_member(leader);

        CLAN_MANAGER.add_clan(clan);

        // Update player's clan_id
        Self::update_player_clan_id(player.id as i32, next_id).await?;

        ServiceHandles::send_message_alert(player, "Chúc mừng bạn đã tạo bang thành công!")?;
        info!(
            "Player {} created clan {} (ID: {})",
            player.name, name, next_id
        );
        Ok(())
    }

    // ==================== CHANGE CLAN INFO ====================
    async fn change_info_clan(player: &Player, img_id: i32, slogan: &str) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let clan_id = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

                if !clan.is_leader(player.id as i32) {
                    ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                    return Ok(());
                }

                if !slogan.is_empty() {
                    clan.slogan = slogan.chars().take(250).collect();
                } else {
                    clan.img_id = img_id;
                }
                clan.id
            } else {
                return Ok(());
            }
        };

        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== CLAN DONATE (-54) ====================
    pub async fn clan_donate(player: &Player, mut msg: Message) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }

        let cmg_id = msg.read_int()?;

        let (target_id, clan_id) = {
            if let Some(clan_arc) = CLAN_MANAGER.get_clan(player.clan_id) {
                let mut clan = clan_arc.write().await;

                let cmg = clan
                    .clan_messages
                    .iter_mut()
                    .find(|c| c.id == cmg_id && c.message_type == 1);
                if let Some(req) = cmg {
                    if req.receive_donate >= req.max_donate {
                        ServiceHandles::send_message_alert(player, "Đã đủ số lượng")?;
                        return Ok(());
                    }
                    req.receive_donate += 1;
                    (req.player_id, clan.id)
                } else {
                    ServiceHandles::send_message_alert(player, "Không tìm thấy yêu cầu")?;
                    return Ok(());
                }
            } else {
                return Ok(());
            }
        };

        // TODO: Transfer pea from player to target_id
        // This requires item inventory logic
        info!("Player {} donated pea to player {}", player.name, target_id);
        ServiceHandles::send_message_alert(player, "Đã cho đậu thành công")?;

        Self::send_my_clan_for_all_members(clan_id).await?;
        Ok(())
    }

    // ==================== SEND CLAN ID (-61) ====================
    pub async fn send_clan_id(player: &Player) -> anyhow::Result<()> {
        let mut msg = Message::new(-61);
        msg.write_int(player.id as i32)?;
        msg.write_int(player.clan_id)?;

        // Send to all players in zone
        if let Some(zone) =
            crate::map::zone_manager::ZONE_MANAGER.get_zone(player.map_id, player.zone_id)
        {
            zone.broadcast(msg);
        }
        Ok(())
    }
}
