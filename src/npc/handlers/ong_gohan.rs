use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::shop::shop_services::shop_service;

pub struct NpcHomeHandler;

#[async_trait::async_trait]
impl NpcHandler for NpcHomeHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        ctx.create_menu(
            "Chào con, ông Gohan đây!",
            vec!["Giftcode", "Đổi Mật Khẩu", "Đổi Skill", "Shop Skill"],
            MenuId::OngGohanMenu,
        )?;
        Ok(())
    }

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::OngGohanMenu => match select {
                0 => {
                    ctx.npc_chat("Chức năng Giftcode đang được phát triển")?;
                }
                1 => {
                    ctx.npc_chat("Chức năng Đổi Mật Khẩu đang được phát triển")?;
                }
                2 => {
                    ctx.npc_chat("Chức năng Đổi Skill đang được phát triển")?;
                }
                3 => {
                    shop_service::open_shop("DOI_SKILL_DE", ctx.session).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
