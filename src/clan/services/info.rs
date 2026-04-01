use crate::clan::clan_manager::CLAN_MANAGER;
use crate::network::message::Message;
use crate::player::Player;

pub struct InfoService;

impl InfoService {
    pub async fn send_my_clan(player: &Player) -> anyhow::Result<()> {
        if player.clan_id == -1 {
            let mut msg = Message::new(-53);
            msg.write_int(-1)?;
            player.send_to_client(msg);
            return Ok(());
        }

        if let Some(clan_handle) = CLAN_MANAGER.get_clan(player.clan_id) {
            let Some(clan) = clan_handle.get_snapshot().await else { return Ok(()); };
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

            for member in &clan.members {
                msg.write_int(member.id)?;
                msg.write_short(member.head)?;
                msg.write_short(-1)?;
                msg.write_short(member.leg)?;
                msg.write_short(member.body)?;
                msg.write_utf(&member.name)?;
                msg.write_byte(member.role)?;
                msg.write_utf(&crate::utils::number_util::number_to_money(member.power_point))?;
                msg.write_int(member.donate)?;
                msg.write_int(member.receive_donate)?;
                msg.write_int(member.clan_point)?;
                msg.write_int(member.member_point)?;
                msg.write_int(member.join_time)?;
            }

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

    /// Send clan search results (-47)
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

    /// Send member list for a specific clan (-48)
    pub async fn send_member_list(player: &Player, clan_id: i32) -> anyhow::Result<()> {
        if let Some(clan_handle) = CLAN_MANAGER.get_clan(clan_id) {
            if let Some(clan) = clan_handle.get_snapshot().await {
                let mut msg = Message::new(-48);
                msg.write_byte(clan.members.len() as i8)?;
                for member in &clan.members {
                    msg.write_int(member.id)?;
                    msg.write_short(member.head)?;
                    msg.write_short(-1)?;
                    msg.write_short(member.leg)?;
                    msg.write_short(member.body)?;
                    msg.write_utf(&member.name)?;
                    msg.write_byte(member.role)?;
                    msg.write_utf(&crate::utils::number_util::number_to_money(member.power_point))?;
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
}
