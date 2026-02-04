use crate::constant::cmd;

use super::message::Message;
use super::session::{AsyncSession, SessionArc};
use dashmap::DashMap;
use std::sync::Arc;
use std::time::Duration;

use tracing::{error, info, warn};

pub struct SessionManager {
    sessions: DashMap<i64, SessionArc>,
}

impl SessionManager {
    pub fn new() -> Self {
        Self {
            sessions: DashMap::new(),
        }
    }

    pub fn add_session(&self, player_id: i64, session: SessionArc) -> bool {
        let inserted = self.sessions.insert(player_id, session);
        if inserted.is_some() {
            info!("Replaced session for player {}", player_id);
        }
        true
    }

    pub fn remove_session(&self, player_id: i64) -> bool {
        let removed = self.sessions.remove(&player_id).is_some();
        if removed {
            info!("Removed session for player {}", player_id);
        }
        removed
    }

    pub async fn kick_player(&self, player_id: i64, reason: &str) -> bool {
        let session_arc = self.sessions.remove(&player_id).map(|(_, v)| v);

        if let Some(session) = session_arc {
            info!("Kicking player {} and waiting...", player_id);

            if let Some(player) = session.get_player_snapshot().await {
                let zone_manager = &crate::map::zone_manager::ZONE_MANAGER;
                if let Some(zone) = zone_manager.get_zone(player.map_id, player.zone_id) {
                    let _ = zone.remove_player(player.id);
                    info!(
                        "Removed player {} from zone {} map {}",
                        player.name, zone.zone_id, zone.map_id
                    );
                }
            }

            let mut response = Message::new(cmd::cmd::SEND_ALTER_MESSAGE);
            if let Err(e) = response.write_utf(reason) {
                error!("Failed to write kick message: {:?}", e);
                return true;
            }

            let kick_result = tokio::time::timeout(Duration::from_secs(3), async {
                let _ = session.transmit(response);
                session.shutdown().await
            })
            .await;

            match kick_result {
                Ok(Ok(_)) => info!("Kicked player {} (connection closed)", player_id),
                Ok(Err(e)) => {
                    error!("Failed to kick {}: {:?}", player_id, e)
                }
                Err(_) => warn!("Kick timeout for player {} after 3s", player_id),
            }

            true
        } else {
            false
        }
    }

    pub fn is_online(&self, player_id: i64) -> bool {
        self.sessions.contains_key(&player_id)
    }

    pub fn get_online_count(&self) -> usize {
        self.sessions.len()
    }

    pub fn get_session(&self, player_id: i64) -> Option<SessionArc> {
        self.sessions.get(&player_id).map(|s| s.clone())
    }
}
