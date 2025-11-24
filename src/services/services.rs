use crate::{
    constant::cmd::cmd,
    network::{
        message::{self, Message},
        session::AsyncSession,
    },
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
}
