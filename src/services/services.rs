use crate::{
    constant::cmd::cmd,
    network::{
        message::{self, Message},
        session::{self, AsyncSession},
    },
    player::Player,
};
use crate::network::session::SessionArc;
use anyhow::Result;

pub struct ServiceHandles {}
impl ServiceHandles {
    pub async fn send_message_alert(session: &SessionArc, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::SEND_ALTER_MESSAGE);
        response.write_utf(text);
        session.send_message(&response).await?;
        Ok(())
    }
    pub async fn chat(session: &SessionArc, text: &str) -> Result<()> {
        let (player_id, zone) = if let Some(player) = session.get_player().await {
            (player.id, player.zone.clone())
        } else {
            return Ok(());
        };

        let mut response = Message::new(cmd::CHAT);
        response.write_int(player_id as i32)?;
        response.write_utf(text)?;
        session.send_message(&response).await?;
        if let Some(zone) = zone {
            zone.send_message_to_other_players(player_id, response)
                .await?;
        }
        Ok(())
    }
}
