//! Membership management: kick, promote, demote, transfer leader, leave, join, invite
use crate::clan::clan_manager::CLAN_MANAGER;
use crate::clan::message::ClanMessage;
use crate::database::DbManager;
use crate::models::clan::{Clan, ClanMember, ClanMessage as ClanMsg};
use crate::player::player_actor::{PlayerHandle, PlayerMessage};
use crate::player::Player;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::services::ServiceHandles;
use sea_orm::{EntityTrait, Set};

const RED: i8 = 1;

pub struct MembershipService;

impl MembershipService {
    // ─── Clan Remote dispatcher (-55) ────────────────────────────────────────
    pub async fn clan_remote(player: &Player, mut msg: crate::network::message::Message) -> anyhow::Result<()> {
        let member_id = msg.read_int()?;
        let role = msg.read_byte()?;
        match role {
            -1 => Self::kick_out(player, member_id).await?,
            0  => Self::transfer_leader(player, member_id).await?,
            1  => Self::promote_deputy(player, member_id).await?,
            2  => Self::demote_member(player, member_id).await?,
            _  => {}
        }
        Ok(())
    }

    // ─── Kick ─────────────────────────────────────────────────────────────────
    pub async fn kick_out(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            let my_role     = clan.get_role(player.id as i32);
            let target_role = clan.get_role(member_id);
            if my_role != Clan::LEADER && !(my_role == Clan::DEPUTY && target_role == Clan::MEMBER) {
                ServiceHandles::send_message_alert(player, "Bạn không có quyền")?;
                return Ok(());
            }
            let member_name = clan.members.iter().find(|m| m.id == member_id)
                .map(|m| m.name.clone()).unwrap_or_default();

            clan_handle.send_forget(ClanMessage::KickMember(member_id));

            let kicked_handle = clan.members_online.iter().find(|h| h.id == member_id as u64).cloned();
            Self::update_player_clan_id(member_id, -1).await?;
            if let Some(handle) = kicked_handle {
                handle.send_forget(PlayerMessage::Modify(Box::new(|p| p.clan_id = -1)));
            }

            let cmg = Self::system_msg(&clan, player, format!("Đuổi {} ra khỏi bang.", member_name), RED);
            clan_handle.add_message(cmg.clone());
            super::broadcast::send_message_clan(clan.id, cmg).await?;
            super::broadcast::send_my_clan_for_all_members(clan.id).await?;
        }
        Ok(())
    }

    // ─── Promote deputy ───────────────────────────────────────────────────────
    pub async fn promote_deputy(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            if clan.get_role(player.id as i32) != Clan::LEADER {
                ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                return Ok(());
            }
            if let Some(member) = clan.members.iter().find(|m| m.id == member_id) {
                if member.role != Clan::MEMBER {
                    ServiceHandles::send_message_alert(player, "Không thể thực hiện")?;
                    return Ok(());
                }
                let member_name = member.name.clone();
                clan_handle.send_forget(ClanMessage::PromoteMember(member_id, Clan::DEPUTY));
                let cmg = Self::system_msg(&clan, player, format!("Phong phó bang cho {}", member_name), RED);
                clan_handle.add_message(cmg.clone());
                super::broadcast::send_message_clan(clan.id, cmg).await?;
                super::broadcast::send_my_clan_for_all_members(clan.id).await?;
            }
        }
        Ok(())
    }

    // ─── Demote ───────────────────────────────────────────────────────────────
    pub async fn demote_member(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            if clan.get_role(player.id as i32) != Clan::LEADER {
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
                let cmg = Self::system_msg(&clan, player, format!("Cắt chức phó bang của {}", member_name), RED);
                clan_handle.add_message(cmg.clone());
                super::broadcast::send_message_clan(clan.id, cmg).await?;
                super::broadcast::send_my_clan_for_all_members(clan.id).await?;
            }
        }
        Ok(())
    }

    // ─── Transfer leader ──────────────────────────────────────────────────────
    pub async fn transfer_leader(player: &Player, member_id: i32) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            if !clan.is_leader(player.id as i32) {
                ServiceHandles::send_message_alert(player, "Chỉ bang chủ mới có quyền")?;
                return Ok(());
            }
            if clan.get_role(member_id) != Clan::DEPUTY {
                ServiceHandles::send_message_alert(player, "Chỉ có thể nhường cho phó bang")?;
                return Ok(());
            }
            let new_leader_name = clan.members.iter().find(|m| m.id == member_id)
                .map(|m| m.name.clone()).unwrap_or_default();

            clan_handle.send_forget(ClanMessage::PromoteMember(player.id as i32, Clan::MEMBER));
            clan_handle.send_forget(ClanMessage::PromoteMember(member_id, Clan::LEADER));

            let cmg = Self::system_msg(&clan, player, format!("Nhường chức bang chủ cho {}", new_leader_name), RED);
            clan_handle.add_message(cmg.clone());
            super::broadcast::send_message_clan(clan.id, cmg).await?;
            super::broadcast::send_my_clan_for_all_members(clan.id).await?;
        }
        Ok(())
    }

    // ─── Leave ────────────────────────────────────────────────────────────────
    pub async fn leave_clan(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            if clan.is_leader(player.id as i32) {
                ServiceHandles::send_message_alert(player, "Phải nhường chức bang chủ trước")?;
                return Ok(());
            }
            let role = clan.get_role(player.id as i32);
            clan_handle.send_forget(ClanMessage::LeaveClan(player.id));

            let cmg = Self::system_msg(&clan, player, format!("{} đã rời bang.", player.name), RED);
            let cmg = ClanMsg { role, ..cmg };
            clan_handle.add_message(cmg.clone());
            Self::update_player_clan_id(player.id as i32, -1).await?;
            super::broadcast::send_message_clan(clan.id, cmg).await?;
            super::broadcast::send_my_clan_for_all_members(clan.id).await?;
        }
        Ok(())
    }

    // ─── Invite (-49) ─────────────────────────────────────────────────────────
    pub async fn clan_invite(player: &Player, mut msg: crate::network::message::Message) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
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
                            ServiceHandles::send_message_alert(player, "Người này đã có bang hội!")?;
                            return Ok(());
                        }
                        let mut msg_invite = crate::network::message::Message::new(-49);
                        msg_invite.write_int(player.id as i32)?;
                        msg_invite.write_utf(&player.name)?;
                        msg_invite.write_int(clan.id)?;
                        msg_invite.write_utf(&clan.name)?;
                        msg_invite.write_int(target_id)?;
                        target_handle.send_forget(PlayerMessage::SendPacket(msg_invite));
                        clan_handle.send_forget(ClanMessage::AddInvite(target_id));
                        ServiceHandles::send_message_alert(player, &format!("Đã gửi lời mời vào bang cho {}", target_snap.name))?;
                    }
                } else {
                    ServiceHandles::send_message_alert(player, "Người này không online!")?;
                }
            }
        }
        Ok(())
    }

    // ─── Join ─────────────────────────────────────────────────────────────────
    pub async fn join_clan(player_handle: PlayerHandle, clan_id: i32, role: i8) -> anyhow::Result<()> {
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
                player_handle.send_forget(PlayerMessage::Modify(Box::new(move |p| p.clan_id = clan_id)));
                Self::update_player_clan_id(snapshot.id as i32, clan_id).await?;
                super::broadcast::send_my_clan_for_all_members(clan_id).await?;
            }
        }
        Ok(())
    }

    pub async fn join_clan_controller(player_handle: PlayerHandle, mut msg: crate::network::message::Message) -> anyhow::Result<()> {
        let clan_id = msg.read_int()?;
        Self::join_clan(player_handle, clan_id, Clan::MEMBER).await
    }

    // ─── DB helper ────────────────────────────────────────────────────────────
    pub async fn update_player_clan_id(player_id: i32, clan_id: i32) -> anyhow::Result<()> {
        use crate::entities::player;
        let db = DbManager::get_pool();
        player::Entity::update(player::ActiveModel {
            id: Set(player_id),
            clan_id: Set(clan_id),
            ..Default::default()
        }).exec(db).await?;
        Ok(())
    }

    // ─── Internal helper ──────────────────────────────────────────────────────
    fn system_msg(clan: &Clan, player: &Player, text: String, color: i8) -> ClanMsg {
        ClanMsg {
            id: clan.clan_message_id + 1,
            message_type: 0,
            player_id: player.id as i32,
            player_name: player.name.clone(),
            player_power: player.n_point.power,
            role: clan.get_role(player.id as i32),
            time: (crate::utils::time::current_time_millis() / 1000) as i32,
            text,
            receive_donate: 0,
            max_donate: 0,
            is_new_message: 0,
            color,
        }
    }
}
