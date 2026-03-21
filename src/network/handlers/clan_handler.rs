use crate::clan::clan_service::ClanService;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player_actor::PlayerMessage;
use anyhow::Result;

pub struct ClanHandler;

impl ClanHandler {
    pub async fn get_my_clan(session: &SessionArc) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::send_my_clan(&snapshot).await?;
        }
        Ok(())
    }

    pub async fn clan_message(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::clan_message(&snapshot, msg).await?;
        }
        Ok(())
    }

    pub async fn get_clan_list(session: &SessionArc, mut msg: Message) -> Result<()> {
        let name = msg.read_utf()?;
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::send_clan_list(&snapshot, &name).await?;
        }
        Ok(())
    }

    pub async fn get_member_list(session: &SessionArc, mut msg: Message) -> Result<()> {
        let clan_id = msg.read_int()?;
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::send_member_list(&snapshot, clan_id).await?;
        }
        Ok(())
    }

    pub async fn clan_remote(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::clan_remote(&snapshot, msg).await?;
        }
        Ok(())
    }

    pub async fn leave_clan(session: &SessionArc) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::leave_clan(&snapshot).await?;
        }
        Ok(())
    }

    pub async fn clan_invite(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::clan_invite(&snapshot, msg).await?;
        }
        Ok(())
    }

    pub async fn clan_join(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            ClanService::join_clan_controller(handle, msg).await?;
        }
        Ok(())
    }

    pub async fn clan_info(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::get_clan(&snapshot, msg).await?;
        }
        Ok(())
    }

    pub async fn clan_donate(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ClanService::clan_donate(&snapshot, msg).await?;
        }
        Ok(())
    }
}
