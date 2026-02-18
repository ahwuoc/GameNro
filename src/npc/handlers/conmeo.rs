use crate::constant::menu_enum::MenuId;
use crate::matches::pvp_service;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::services::IntrinsicService;

pub struct ConMeoHandler;

#[async_trait::async_trait]
impl NpcHandler for ConMeoHandler {
    async fn open_menu(&self, _ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::Admin => match select {
                5 => {}
                _ => {}
            },
            MenuId::Intrinsic => match select {
                0 => {
                    if let Some(player) = ctx.get_player_snapshot().await {
                        IntrinsicService::show_all_intrinsic(&player).await?;
                    }
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
