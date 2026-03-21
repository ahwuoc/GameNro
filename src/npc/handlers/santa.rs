use crate::constant::menu_enum::MenuId;
use crate::npc::handlers::{NpcContext, NpcHandler};
use crate::shop::shop_services::shop_service;

pub struct SantaHandler;

#[async_trait::async_trait]
impl NpcHandler for SantaHandler {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()> {
        ctx.create_menu(
            "Xin chào! Tôi là Santa. Bạn cần gì?",
            vec![
                "Shop Vi Hợp",
                "Shop Sự Kiện",
                "Shop Vi Hợp 2",
                "Shop Ngũ Sắc",
                "Tiệm hớt tóc",
                "Shop X",
                "Shop Ruby",
            ],
            MenuId::SantaMenu,
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
            MenuId::SantaMenu => match select {
                0 => shop_service::open_shop("VIHOP", ctx.session).await?,
                1 => shop_service::open_shop("SU_KIEN", ctx.session).await?,
                2 => shop_service::open_shop("VIHOP", ctx.session).await?,
                3 => shop_service::open_shop("SHOP_NGU_SAC", ctx.session).await?,
                4 => shop_service::open_shop("SANTA_HEAD", ctx.session).await?,
                5 => shop_service::open_shop("XXXX", ctx.session).await?,
                6 => shop_service::open_shop("SANTA_RUBY", ctx.session).await?,
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
