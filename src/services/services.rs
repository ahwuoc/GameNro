use crate::network::session::SessionArc;
use crate::{
    constant::cmd::cmd,
    network::{
        message::{self, Message},
        session::{self, AsyncSession},
    },
    player::Player,
};
use anyhow::Result;

pub struct ServiceHandles {}
impl ServiceHandles {
    pub fn send_message_alert(session: &SessionArc, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::SEND_ALTER_MESSAGE);
        response.write_utf(text);
        session.transmit(response);
        Ok(())
    }
    pub async fn chat(session: &SessionArc, text: &str) -> Result<()> {
        let (player_id, zone) = session
            .get_player_ref(|player| {
                if let Some(player) = player {
                    Some((player.id, player.zone.clone()))
                } else {
                    None
                }
            })
            .await
            .unwrap_or_else(|| (0, None));

        if player_id == 0 {
            return Ok(());
        }

        let mut response = Message::new(cmd::CHAT);
        response.write_int(player_id as i32)?;
        response.write_utf(text)?;
        session.transmit(response.clone());
        if let Some(zone) = zone {
            zone.send_message_to_other_players(player_id, response)
                .await?;
        }
        Ok(())
    }
}
