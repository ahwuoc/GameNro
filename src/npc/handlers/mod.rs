use sqlx::any;

use crate::constant::menu_enum::MenuId;
use crate::network::session::AsyncSession;
use std::future::Future;
use std::pin::Pin;

pub mod bahatmit;
pub mod conmeo;
pub mod ruong_do;
pub mod santa;
use async_trait::async_trait;
#[async_trait]
pub trait NpcHandler {
    async fn open_menu(&self, session: &mut AsyncSession, npc_id: i16) -> anyhow::Result<()>;
    async fn handle_menu(
        &self,
        session: &mut AsyncSession,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()>;
}
