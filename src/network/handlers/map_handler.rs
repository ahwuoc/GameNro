use crate::constant::const_map;
use crate::map::change_map_service::ChangeMapService;
use crate::map::zone_manager::ZONE_MANAGER;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player_actor::PlayerMessage;
use crate::services::ServiceHandles;
use anyhow::Result;
use tracing::warn;

pub struct MapHandler;

impl MapHandler {
    pub async fn open_zone_ui(session: &SessionArc) -> Result<()> {
        if let Some(snapshot) = session.get_player_snapshot().await {
            ChangeMapService::open_zone_ui(&snapshot).await?;
        }
        Ok(())
    }

    pub async fn change_zone(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::NetworkMessage(msg));
        }
        Ok(())
    }

    pub async fn change_map_waypoint(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            let _ = handle.send(PlayerMessage::NetworkMessage(msg)).await;
        }
        Ok(())
    }

    pub async fn go_home(session: &SessionArc, msg: Message) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::NetworkMessage(msg));
        }
        Ok(())
    }

    pub async fn finish_load_map(session: &SessionArc) -> Result<()> {
        if let Some(handle) = session.get_player_handle().await {
            handle.send_forget(PlayerMessage::FinishLoadMap);
        }
        Ok(())
    }

    pub async fn capsule_menu(session: &SessionArc, mut msg: Message) -> Result<()> {
        let index = msg.read_byte()?;
        if let Some(snapshot) = session.get_player_snapshot().await {
            if let Some(handle) = session.get_player_handle().await {
                match snapshot.interaction_state.type_change_map {
                    const_map::CHANGE_CAPSULE => {
                        handle.send_forget(PlayerMessage::ChangeMapCapsule(index as i32));
                    }
                    const_map::CHANGE_BLACK_BALL => {
                        handle.send_forget(PlayerMessage::ChangeMapBlackBall(index));
                    }
                    _ => {
                        warn!(
                            "Unknown type_change_map: {} for player {}",
                            snapshot.interaction_state.type_change_map, snapshot.id
                        );
                    }
                }
            }
        }
        Ok(())
    }

    pub async fn player_move(session: &SessionArc, mut msg: Message) -> Result<()> {
        let _can_fly = msg.read_byte()?;
        let to_x = msg.read_short()?;
        let to_y_result = msg.read_short();
        if let Some(handle) = session.get_player_handle().await {
            let y = match to_y_result {
                Ok(y) => y,
                Err(_) => {
                    if let Some(snapshot) = session.get_player_snapshot().await {
                        snapshot.location.y
                    } else {
                        0
                    }
                }
            };
            handle.send(PlayerMessage::Move { x: to_x, y }).await?;
        }
        Ok(())
    }
}
