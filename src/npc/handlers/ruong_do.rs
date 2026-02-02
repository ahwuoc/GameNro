use crate::constant::menu_enum::MenuId;
use crate::item::InventoryService;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::npc::handlers::NpcHandler;
use async_trait::async_trait;

pub struct RuongDoHandler;

#[async_trait]
impl NpcHandler for RuongDoHandler {
    async fn open_menu(&self, session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
        if let Some(player) = session.get_player_snapshot().await {
            InventoryService::send_open_box(&player)?;
        }
        Ok(())
    }
    async fn handle_menu(
        &self,
        session: &SessionArc,
        npc_id: i16,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        Ok(())
    }
}
