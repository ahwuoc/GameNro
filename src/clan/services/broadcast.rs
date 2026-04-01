//! Shared broadcast helpers - build and fan-out packets to online clan members
use crate::clan::clan_manager::CLAN_MANAGER;
use crate::models::clan::ClanMessage as ClanMsg;
use crate::network::message::Message;
use crate::player::player_actor::{PlayerHandle, PlayerMessage};

/// Broadcast a ClanMessage packet (-51) to all online members of `clan_id`.
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

/// Send an updated -53 (MyClan) packet to every online member.
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
            super::info::InfoService::send_my_clan(&snapshot).await?;
        }
    }
    Ok(())
}
