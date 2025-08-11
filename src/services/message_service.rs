use crate::network::async_net::message::Message;
use crate::player::player::Player;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;

pub struct MessageService;

impl MessageService {
    pub async fn send_to_all_players(
        players: &HashMap<u64, Player>,
        mut msg: Message,
    ) -> Result<(), Box<dyn std::error::Error>> {
        msg.finalize_write();
        for player in players.values() {
            player.send_message(msg.clone()).await?;
        }
        Ok(())
    }

    pub async fn send_to_other_players(
        players: &HashMap<u64, Player>,
        except_player_id: u64,
        mut msg: Message,
    ) -> Result<(), Box<dyn std::error::Error>> {
        msg.finalize_write();
        for (pid, player) in players.iter() {
            if *pid != except_player_id {
                let _ = player.send_message(msg.clone()).await;
            }
        }
        Ok(())
    }

    /// Send message to a specific player
    pub async fn send_to_player(
        player: &Player,
        mut msg: Message,
    ) -> Result<(), Box<dyn std::error::Error>> {
        msg.finalize_write();
        player.send_message(msg).await?;
        Ok(())
    }
}
