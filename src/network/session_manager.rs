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

    pub async fn get_session(&self, player_id: i64) -> Option<SessionArc> {
        let sessions = self.sessions.read().await;
        sessions.get(&player_id).cloned()
    }
    pub async fn send_to_player(&self, player_id: i64, msg: &Message) -> anyhow::Result<()> {
        let session_arc = {
            let sessions = self.sessions.read().await;
            sessions.get(&player_id).cloned()
        };

        if let Some(session_arc) = session_arc {
            let mut session = session_arc.write().await;
            session.send_message(msg).await?;
            Ok(())
        } else {
            Err(anyhow::anyhow!(
                "Session not found for player {}",
                player_id
            ))
        }
    }
    pub async fn send_to_players(&self, player_ids: &[i64], msg: &Message) {
        for &player_id in player_ids {
            if let Err(e) = self.send_to_player(player_id, msg).await {
                println!(
                    "[SESSION_MANAGER] Failed to send to player {}: {:?}",
                    player_id, e
                );
            }
        }
    }
    pub async fn broadcast(&self, msg: &Message) {
        let sessions = {
            let sessions_guard = self.sessions.read().await;
            sessions_guard.values().cloned().collect::<Vec<_>>()
        };
        for session in sessions {
            let mut session = session.write().await;
            session.send_message(msg).await;
        }
    }
    pub async fn kick_player(&self, player_id: i64, reason: &str) -> bool {
        let session_arc = {
            let mut sessions = self.sessions.write().await;
            sessions.remove(&player_id)
        };

        if let Some(session_arc) = session_arc {
            // Clone reason để move vào async block
            let reason_owned = reason.to_string();

            // Spawn task riêng để kick KHÔNG ĐỒNG BỘ
            tokio::spawn(async move {
                println!("[SESSION_MANAGER] Kicking player {}...", player_id);

                // Chuẩn bị kick message
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
                        "[SESSION_MANAGER] ✅ Kicked player {} (connection closed)",
                        player_id
                    ),
                    Ok(Err(e)) => {
                        println!("[SESSION_MANAGER] ❌ Failed to kick {}: {:?}", player_id, e)
                    }
                    Err(_) => println!(
                        "[SESSION_MANAGER] ⏱ Kick timeout for player {} after 5s",
                        player_id
                    ),
                }
            });

            true
        } else {
            false
        }
    }

    // Utility methods
    pub async fn count_online(&self) -> usize {
        let sessions = self.sessions.read().await;
        sessions.len()
    }

    pub async fn is_online(&self, player_id: i64) -> bool {
        let sessions = self.sessions.read().await;
        sessions.contains_key(&player_id)
    }

    pub async fn get_online_player_ids(&self) -> Vec<i64> {
        let sessions = self.sessions.read().await;
        sessions.keys().copied().collect()
    }
}
