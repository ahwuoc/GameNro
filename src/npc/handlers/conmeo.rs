use sqlx::any;

use crate::constant::menu_enum::MenuId;
use crate::entities::player;
use crate::network::session::{self, AsyncSession, SessionArc};
use crate::npc::handlers::NpcHandler;
use crate::services::{IntrinsicService, ServiceHandles};
use sysinfo::System;

pub struct ConMeoHandler;

#[async_trait::async_trait]
impl NpcHandler for ConMeoHandler {
    async fn open_menu(&self, session: &SessionArc, npc_id: i16) -> anyhow::Result<()> {
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
            MenuId::Admin => match select {
                5 => {}
                _ => {}
            },
            MenuId::Intrinsic => match select {
                0 => {
                    let gender = session
                        .get_player_ref(|pl| pl.map(|p| p.gender).unwrap_or(0))
                        .await;

                    if gender == 0 && session.get_player_ref(|p| p.is_none()).await {
                        return Ok(());
                    }
                    IntrinsicService::show_all_intrinsic(session, gender).await?;
                }
                _ => {}
            },
            _ => {}
        }
        Ok(())
    }
}
