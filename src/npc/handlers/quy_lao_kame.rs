use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::player::player_actor::PlayerMessage;
use async_trait::async_trait;

pub struct QuyLaoKameHandler;

#[async_trait]
impl NpcHandler for QuyLaoKameHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        let player = ctx.get_snapshot().await;
        let Some(player) = player else {
            return Ok(());
        };

        ctx.create_menu(
            "Chào con, ta có thể giúp gì cho con?",
            vec![
                "Bản đồ\nkho báu",
                "Nhiệm vụ",
                "Kho báu\ndưới biển",
                "Từ chối",
            ],
            MenuId::BaseMenu,
        )?;

        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        let player = ctx.get_snapshot().await;
        let Some(player) = player else {
            return Ok(());
        };

        match menu_id {
            MenuId::BaseMenu => {
                match select {
                    0 => {
                        // Bản đồ kho báu
                        ctx.create_menu(
                            "Con có muốn tham gia Bản đồ kho báu không?",
                            vec!["Mở bằng\nBản đồ\nkho báu", "Huống\ndẫn", "Từ chối"],
                            MenuId::MenuJoinRedRibbon,
                        )?;
                    }
                    _ => {
                        ctx.npc_chat("Tính năng đang phát triển")?;
                    }
                }
            }
            MenuId::MenuJoinRedRibbon => {
                if select == 0 {
                    // Start Red Ribbon Logic
                    ctx.npc_chat("Ngươi cần có Bản đồ kho báu để vào đây.")?;
                }
            }
            _ => {}
        }
        Ok(())
    }
}
