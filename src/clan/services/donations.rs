//! Clan donation (pea giving)
use crate::clan::clan_manager::CLAN_MANAGER;
use crate::clan::message::ClanMessage;
use crate::player::Player;
use crate::services::services::ServiceHandles;

pub struct DonationService;

impl DonationService {
    pub async fn clan_donate(player: &Player, mut msg: crate::network::message::Message) -> anyhow::Result<()> {
        if player.clan_id == -1 { return Ok(()); }
        let pea_count = msg.read_byte()?;
        if pea_count <= 0 { return Ok(()); }
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            if let Some(clan) = clan_handle.get_snapshot().await {
                let found_request = clan.clan_messages.iter()
                    .find(|cmg| cmg.message_type == 1 && cmg.receive_donate < cmg.max_donate)
                    .cloned();
                if let Some(mut req) = found_request {
                    req.receive_donate += 1;
                    clan_handle.update_message(req.clone());
                    clan_handle.send_forget(ClanMessage::UpdateDonate(player.id as i32, 1, 10));
                    ServiceHandles::send_message_alert(player, "Đã cho đậu thành công!")?;
                    super::broadcast::send_my_clan_for_all_members(clan.id).await?;
                } else {
                    ServiceHandles::send_message_alert(player, "Không có yêu cầu xin đậu nào!")?;
                }
            }
        }
        Ok(())
    }
}
