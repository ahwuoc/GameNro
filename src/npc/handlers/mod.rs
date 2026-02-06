use crate::constant::menu_enum::MenuId;
use crate::network::message::Message;
use crate::network::session::{AsyncSession, SessionArc};
use crate::player::player_actor::{PlayerHandle, PlayerMessage};
use crate::player::Player;

pub mod bahatmit;
pub mod conmeo;
pub mod dau_than;
pub mod dynamic_shop_handler;
pub mod ong_gohan;
pub mod ruong_do;
pub mod santa;

use async_trait::async_trait;

/// Context chứa tất cả thông tin cần thiết cho NPC interaction
pub struct NpcContext<'a> {
    /// Network session để gửi message cho client
    pub session: &'a SessionArc,
    /// Player handle để thao tác với player state (actor pattern)
    pub player_handle: Option<PlayerHandle>,
    /// NPC ID đang tương tác
    pub npc_id: i16,
}

impl<'a> NpcContext<'a> {
    /// Tạo NpcContext mới
    pub async fn new(session: &'a SessionArc, npc_id: i16) -> Self {
        let player_handle = session.get_player_handle().await;
        Self {
            session,
            player_handle,
            npc_id,
        }
    }

    /// Gửi message trực tiếp cho client
    pub fn transmit(&self, msg: Message) {
        self.session.transmit(msg);
    }

    /// NPC nói với player
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
