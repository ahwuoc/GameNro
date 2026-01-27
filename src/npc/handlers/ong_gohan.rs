use crate::constant::menu_enum::MenuId;
use crate::network::session::{AsyncSession, SessionArc};
use crate::npc::handlers::NpcHandler;
use crate::npc::npc_service;
use crate::services::ServiceHandles;
use crate::shop::shop_services::shop_service;

pub struct NpcHomeHandler;

#[async_trait::async_trait]
impl NpcHandler for NpcHomeHandler {
    async fn open_menu(&self, session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
        npc_service::npc_service::create_menu(
            session,
            npc_id,
            "Chao con ong gohan",
            vec!["Giftcode", "Đổi Mật Khẩu", "Đổi Skill", "Đổi Skill"],
            MenuId::OngGohanMenu,
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
            MenuId::OngGohanMenu => match select {
                0 => {
                    // ServiceHandles::send_message_alert(
                    //     session,
                    //     "Chức năng Giftcode đang được phát triển",
                    // )?;
                }
                1 => {
                    // ServiceHandles::send_message_alert(
                    //     session,
                    //     "Chức năng Đổi Mật Khẩu đang được phát triển",
                    // )?;
                }
                2 => {}
                3 => {
                    shop_service::open_shop("DOI_SKILL_DE", session).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
