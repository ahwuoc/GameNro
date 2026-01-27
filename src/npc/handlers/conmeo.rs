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
                    let player_opt = session.get_player().await;
                    if let Some(player) = player_opt {
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
