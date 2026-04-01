//! Clan chat, ask-for-pea, and join request messages
use crate::clan::clan_manager::CLAN_MANAGER;
use crate::clan::message::ClanMessage;
use crate::models::clan::{ClanMessage as ClanMsg};
use crate::player::Player;
use crate::services::services::ServiceHandles;

const CHAT: i8 = 0;
const ASK_FOR_PEA: i8 = 1;
const ASK_FOR_JOIN_CLAN: i8 = 2;
const RED: i8 = 1;
const BLACK: i8 = 0;

pub struct ChatService;

impl ChatService {
    /// Dispatcher for cmd -51
    pub async fn clan_message(player: &Player, mut msg: crate::network::message::Message) -> anyhow::Result<()> {
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

    pub async fn chat(player: &Player, text: &str) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
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
            super::broadcast::send_message_clan(clan.id, cmg).await?;
        }
        Ok(())
    }

    pub async fn ask_for_pea(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            return Ok(());
        }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            if let Some(member) = clan.members.iter().find(|m| m.id == player.id as i32) {
                let now = crate::utils::time::current_time_millis() as i64;
                if member.time_ask_pea + 5 * 60 * 1000 > now {
                    let wait = (member.time_ask_pea + 5 * 60 * 1000 - now) / 1000;
                    ServiceHandles::send_message_alert(player, &format!("Vui lòng chờ {} giây nữa", wait))?;
                    return Ok(());
                }
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
            super::broadcast::send_message_clan(clan.id, cmg).await?;
        }
        Ok(())
    }

    pub async fn ask_for_join_clan(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        if player.clan_id != -1 {
            ServiceHandles::send_message_alert(player, "Bạn đang ở trong bang")?;
            return Ok(());
        }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
            let already_asked = clan.clan_messages.iter()
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
            super::broadcast::send_message_clan(clan_id, cmg).await?;
            ServiceHandles::send_message_alert(player, "Đã gửi yêu cầu gia nhập")?;
        } else {
            ServiceHandles::send_message_alert(player, "Không tìm thấy bang")?;
        }
        Ok(())
    }
}
