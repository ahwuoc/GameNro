use crate::constant::cmd;
use crate::network::message::Message;
use crate::player::Player;
use once_cell::sync::Lazy;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

pub struct PlayerService {
    players: Arc<RwLock<HashMap<u64, Player>>>,
}

impl PlayerService {
    pub fn new() -> Self {
        Self {
            players: Arc::new(RwLock::new(HashMap::new())),
        }
    }

    pub async fn add_player(&self, player: Player) {
        let mut players = self.players.write().await;
        players.insert(player.id, player);
    }

    pub async fn kick_old_session_if_exists(&self, player_id: u64) -> bool {
        let old_session = {
            let mut players = self.players.write().await;
            if let Some(old_player) = players.remove(&player_id) {
                println!("[KICK] Removed old player {} from service", player_id);
                old_player.session.clone() // Clone Arc, không phải session
            } else {
                println!("[KICK] No old session found for player {}", player_id);
                return false;
            }
        };
        if let Some(session_arc) = old_session {
            tokio::spawn(async move {
                println!("[KICK] Attempting to send kick message...");

                // Tạo message trước
                let mut response = Message::new(cmd::cmd::SEND_ALTER_MESSAGE);
                if let Err(e) = response.write_utf("Tai khoan da dang nhap o 1 noi khac") {
                    println!("[KICK] Failed to write message: {:?}", e);
                    return;
                }

                // Gửi message
                let mut session = session_arc.write().await;
                match session.send_message(&response).await {
                    Ok(_) => println!("[KICK] ✅ Kick message sent successfully"),
                    Err(e) => println!("[KICK] ❌ Failed to send kick message: {:?}", e),
                }
            });
        }

        true
    }

    pub async fn remove_player(&self, player_id: u64) -> bool {
        let mut players = self.players.write().await;
        players.remove(&player_id).is_some()
    }

    pub async fn get_player(&self, player_id: u64) -> Option<Player> {
        let players = self.players.read().await;
        players.get(&player_id).cloned()
    }

    pub async fn get_all_players(&self) -> Vec<Player> {
        let players = self.players.read().await;
        players.values().cloned().collect()
    }

    pub async fn update_player(&self, player_id: u64, update_fn: impl FnOnce(&mut Player)) -> bool {
        let mut players = self.players.write().await;
        if let Some(player) = players.get_mut(&player_id) {
            update_fn(player);
            true
        } else {
            false
        }
    }

    // Movement methods
    pub async fn move_player(&self, player_id: u64, x: i16, y: i16) -> bool {
        self.update_player(player_id, |player| {
            player.set_position(x, y);
        })
        .await
    }
}

pub static PLAYER_SERVICE: Lazy<Arc<RwLock<PlayerService>>> =
    Lazy::new(|| Arc::new(RwLock::new(PlayerService::new())));
