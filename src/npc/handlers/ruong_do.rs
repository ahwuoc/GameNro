use crate::constant::menu_enum::MenuId;
use crate::network::message::Message;
use crate::network::session::AsyncSession;
use crate::npc::handlers::NpcHandler;
use async_trait::async_trait;

pub struct RuongDoHandler;

#[async_trait]
impl NpcHandler for RuongDoHandler {
    async fn open_menu(&self, session: &mut AsyncSession) -> anyhow::Result<()> {
        let mut msg = Message::new(-35);
        msg.write_byte(1)?;
        session.send_message(&msg).await?;
        Ok(())
    }
    async fn handle_menu(
        &self,
        session: &mut AsyncSession,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        Ok(())
    }
}
