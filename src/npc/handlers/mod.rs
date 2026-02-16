use crate::constant::menu_enum::MenuId;
use crate::map::services::change_map_models::SpaceShipType;
use crate::map::ChangeMapService;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::player::player_actor::{PlayerHandle, PlayerMessage};
use crate::player::Player;

pub mod bahatmit;
pub mod cargo;
pub mod conmeo;
pub mod cui;
pub mod dau_than;
pub mod dr_drief;
pub mod dynamic_shop_handler;
pub mod ong_gohan;
pub mod ruong_do;
pub mod santa;
pub mod than_meo;

use async_trait::async_trait;

pub struct NpcContext<'a> {
    pub session: &'a SessionArc,
    pub player_handle: Option<PlayerHandle>,
    pub npc_id: i16,
}

impl<'a> NpcContext<'a> {
    pub async fn new(session: &'a SessionArc, npc_id: i16) -> Self {
        let player_handle = session.get_player_handle().await;
        Self {
            session,
            player_handle,
            npc_id,
        }
    }

    pub fn transmit(&self, msg: Message) {
        self.session.transmit(msg);
    }

    pub fn npc_chat(&self, message: &str) -> anyhow::Result<()> {
        let mut msg = Message::new(124);
        msg.write_short(self.npc_id)?;
        msg.write_utf(message)?;
        self.session.transmit(msg);
        Ok(())
    }

    /// Ẩn dialog chờ
    pub fn hide_wait_dialog(&self) -> anyhow::Result<()> {
        let mut msg = Message::new(-99);
        msg.write_byte(-1)?;
        self.session.transmit(msg);
        Ok(())
    }

    pub async fn get_player_snapshot(&self) -> Option<Player> {
        self.session.get_player_snapshot().await
    }

    pub fn has_player_handle(&self) -> bool {
        self.player_handle.is_some()
    }

    pub fn send_player_message(&self, msg: PlayerMessage) {
        if let Some(ref handle) = self.player_handle {
            handle.send_forget(msg);
        }
    }

    pub async fn send_player_message_await(&self, msg: PlayerMessage) -> anyhow::Result<()> {
        if let Some(ref handle) = self.player_handle {
            handle.send(msg).await?;
        }
        Ok(())
    }

    pub async fn create_menu(
        &self,
        npc_say: &str,
        menu_options: Vec<&str>,
        state: MenuId,
    ) -> anyhow::Result<()> {
        if let Some(ref handle) = self.player_handle {
            let options: Vec<String> = menu_options.iter().map(|&s| s.to_string()).collect();
            handle.send_forget(PlayerMessage::CreateMenu {
                npc_id: self.npc_id,
                npc_say: npc_say.to_string(),
                menu_options: options,
                state,
            });
        }
        Ok(())
    }

    pub async fn change_map_by_spaceship(&self, map_id: i32, x: i16, y: i16) -> anyhow::Result<()> {
        if let Some(zone) = ChangeMapService::get_available_zone(map_id) {
            self.send_player_message(PlayerMessage::ChangeMap {
                map_id,
                zone_id: zone.zone_id,
                x,
                y,
                space_type: SpaceShipType::Auto,
            });
            Ok(())
        } else {
            self.npc_chat("Hiện tại khu vực này đang quá tải, vui lòng quay lại sau.")
        }
    }
}

#[async_trait]
pub trait NpcHandler: Send + Sync {
    async fn open_menu(&self, ctx: &NpcContext<'_>) -> anyhow::Result<()>;

    async fn handle_menu(
        &self,
        ctx: &NpcContext<'_>,
        menu_id: MenuId,
        select: i8,
    ) -> anyhow::Result<()>;
}
