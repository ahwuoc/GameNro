use sqlx::any;

use crate::constant::const_npc;
use crate::constant::menu_enum::MenuId;
use crate::entities::player;
use crate::network::session::{self, AsyncSession, SessionArc};
use crate::npc::handlers::NpcHandler;
use crate::npc::npc_service;
use crate::shop::shop_services::shop_service;

pub struct SantaHandler;

#[async_trait::async_trait]
impl NpcHandler for SantaHandler {
    async fn open_menu(&self, session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
        npc_service::npc_service::create_menu(
            session,
            npc_id,
            "hello world",
            vec![
                "shop 1", "shop 2", "shop 3", "shop 4", "shop 5", "shop 6", "shop 7",
            ],
            MenuId::SantaMenu,
        )
        .await?;
        Ok(())
    }

    async fn handle_menu(
        &self,
        session: &SessionArc,
        npc_id: i16,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::SantaMenu => match select {
                0 => {
                    shop_service::open_shop("VIHOP", session).await?;
                }
                1 => {
                    shop_service::open_shop("SU_KIEN", session).await?;
                }
                2 => {
                    shop_service::open_shop("VIHOP", session).await?;
                }
                3 => {
                    shop_service::open_shop("SHOP_NGU_SAC", session).await?;
                }
                4 => {
                    shop_service::open_shop("SANTA_HEAD", session).await?;
                }
                5 => {
                    shop_service::open_shop("XXXX", session).await?;
                }
                6 => {
                    shop_service::open_shop("SANTA_RUBY", session).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
