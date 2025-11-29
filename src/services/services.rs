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
    pub async fn send_message_alert(session: &mut AsyncSession, text: &str) -> Result<()> {
        let mut response = Message::new(cmd::SEND_ALTER_MESSAGE);
        response.write_utf(&text);
        session.send_message(&response).await?;
        Ok(())
    }
    pub async fn chat(session: &mut AsyncSession, text: &str) -> Result<()> {
        if let Some(player) = session.get_player() {
            let mut response = Message::new(cmd::CHAT);
            response.write_int(player.id as i32);
            response.write_utf(&text);
            if let Some(zone) = player.zone.as_ref() {
                zone.send_message_all_player_in_map(player, response)
                    .await?;
            }
        }
        Ok(())
    }
}
