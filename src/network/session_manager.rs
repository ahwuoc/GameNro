use crate::constant::cmd;

use super::message::Message;
use super::session::AsyncSession;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

pub type SessionArc = Arc<RwLock<AsyncSession>>;

pub struct SessionManager {
    sessions: Arc<RwLock<HashMap<i64, SessionArc>>>,
}

impl SessionManager {
    pub fn new() -> Self {
        Self {
            sessions: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn add_session(&self, player_id: i64, session: SessionArc) -> bool {
        let mut sessions = self.sessions.write().await;
        let inserted = sessions.insert(player_id, session);
        if inserted.is_some() {
            println!(
                "[SESSION_MANAGER] Replaced session for player {}",
                player_id
            );
        }
        true
    }

    pub async fn remove_session(&self, player_id: i64) -> bool {
        let mut sessions = self.sessions.write().await;
        let removed = sessions.remove(&player_id).is_some();
        if removed {
            println!("[SESSION_MANAGER] Removed session for player {}", player_id);
        }
        removed
    }

    pub async fn kick_player(&self, player_id: i64, reason: &str) -> bool {
        let session_arc = {
            let mut sessions = self.sessions.write().await;
            sessions.remove(&player_id)
        };

        if let Some(session_arc) = session_arc {
            let reason_owned = reason.to_string();

            tokio::spawn(async move {
                println!("[SESSION_MANAGER] Kicking player {}...", player_id);
                let mut response = Message::new(cmd::cmd::SEND_ALTER_MESSAGE);
                if let Err(e) = response.write_utf(&reason_owned) {
                    println!("[SESSION_MANAGER] Failed to write kick message: {:?}", e);
                    return;
                }

                let kick_result = tokio::time::timeout(std::time::Duration::from_secs(5), async {
                    let mut session = session_arc.write().await;

                    let _ = session.send_message(&response).await;

                    session.shutdown().await
                })
                .await;

                match kick_result {
                    Ok(Ok(_)) => println!(
                        "[SESSION_MANAGER] Kicked player {} (connection closed)",
                        player_id
                    ),
                    Ok(Err(e)) => {
                        println!("[SESSION_MANAGER] Failed to kick {}: {:?}", player_id, e)
                    }
                    Err(_) => println!(
                        "[SESSION_MANAGER] Kick timeout for player {} after 5s",
                        player_id
                    ),
                }
            });

            true
        } else {
            false
        }
    }
    pub async fn is_online(&self, player_id: i64) -> bool {
        let sessions = self.sessions.read().await;
        sessions.contains_key(&player_id)
    }

    pub async fn get_online_count(&self) -> usize {
        let sessions = self.sessions.read().await;
        sessions.len()
    }
}
