use crate::constant::menu_enum::MenuId;
use crate::network::session::AsyncSession;
use crate::npc::handlers::NpcHandler;
use crate::npc::npc_service;
use crate::services::player_info_service::PlayerInfoService;
use crate::services::ServiceHandles;
use crate::shop::shop_services::shop_service;

pub struct NpcHomeHandler;

#[async_trait::async_trait]
impl NpcHandler for NpcHomeHandler {
    async fn open_menu(&self, session: &mut AsyncSession, npc_id: i16) -> anyhow::Result<()> {
        Ok(())
    }

    async fn handle_menu(
        &self,
        session: &mut AsyncSession,
        npc_id: i16,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()> {
        match menu_id {
            MenuId::OngGohanMenu => match select {
                0 => {
                    ServiceHandles::send_message_alert(
                        session,
                        "Chức năng Giftcode đang được phát triển",
                    )
                    .await?;
                }
                1 => {
                    ServiceHandles::send_message_alert(
                        session,
                        "Chức năng Đổi Mật Khẩu đang được phát triển",
                    )
                    .await?;
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
