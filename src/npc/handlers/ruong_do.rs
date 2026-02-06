use crate::constant::menu_enum::MenuId;
use crate::item::InventoryService;
use crate::npc::handlers::{NpcContext, NpcHandler};
use async_trait::async_trait;

pub struct RuongDoHandler;

#[async_trait]
impl NpcHandler for RuongDoHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        if let Some(player) = ctx.get_player_snapshot().await {
            InventoryService::send_open_box(&player)?;
        }
        Ok(())
    }

    async fn handle_menu(
        &self,
        _ctx: &NpcContext<'_>,
        _menu_id: MenuId,
        _select: i8,
    ) -> anyhow::Result<()> {
        Ok(())
    }
}
