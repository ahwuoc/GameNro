use crate::player::Player;
use crate::network::async_net::session::AsyncSession;
use crate::network::async_net::message::Message;
use crate::map::Zone;
use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use once_cell::sync::Lazy;

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
        println!("[PLAYER_SERVICE] Added player to service");
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

    // Combat methods
    pub async fn damage_player(&self, player_id: u64, damage: u64, piercing: bool) -> u64 {
        self.update_player(player_id, |player| {
            player.injured(damage, piercing);
        }).await;
        damage
    }

    pub async fn heal_player(&self, player_id: u64, amount: u64) -> bool {
        self.update_player(player_id, |player| {
            player.n_point.hp = (player.n_point.hp + amount).min(player.n_point.hp_max);
        }).await
    }

    pub async fn revive_player(&self, player_id: u64) -> bool {
        self.update_player(player_id, |player| {
            player.revive();
        }).await
    }

    // Movement methods
    pub async fn move_player(&self, player_id: u64, x: i16, y: i16) -> bool {
        self.update_player(player_id, |player| {
            player.set_position(x, y);
        }).await
    }

    pub async fn player_move(&self, player: &mut Player, to_x: i16, to_y: i16) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
        if player.is_die || player.zone.is_none() {
            return Ok(());
        }

        if to_x < 0 || to_y < 0 || to_x > 1000 || to_y > 1000 {
            return Ok(());
        }

        player.set_position(to_x, to_y);
        
        use crate::map::map_service::MapService;
        let _ = MapService::get_instance().send_player_move(player).await;
        Ok(())
    }

    pub async fn change_player_map(&self, player_id: u64, map_id: u32, zone_id: u32, x: i16, y: i16) -> bool {
        self.update_player(player_id, |player| {
            player.map_id = map_id;
            player.zone_id = zone_id;
            player.set_position(x, y);
        }).await
    }
    pub async fn send_message_to_player(&self, player_id: u64, msg: Message) -> Result<(), Box<dyn std::error::Error>> {
        if let Some(player) = self.get_player(player_id).await {
            // TODO: Implement actual message sending
            println!("[PLAYER_SERVICE] Sending message to player {}", player_id);
        }
        Ok(())
    }

    pub async fn broadcast_message(&self, msg: Message) -> Result<(), Box<dyn std::error::Error>> {
        let players = self.get_all_players().await;
        for player in players {
            self.send_message_to_player(player.id, msg.clone()).await?;
        }
        Ok(())
    }

    // Player info methods
    pub async fn get_player_count(&self) -> usize {
        let players = self.players.read().await;
        players.len()
    }

    pub async fn get_online_players(&self) -> Vec<Player> {
        let players = self.players.read().await;
        players.values()
            .filter(|player| player.is_pl())
            .cloned()
            .collect()
    }

    pub async fn get_players_in_map(&self, map_id: u32) -> Vec<Player> {
        let players = self.players.read().await;
        players.values()
            .filter(|player| player.map_id == map_id)
            .cloned()
            .collect()
    }

    pub async fn get_players_in_zone(&self, map_id: u32, zone_id: u32) -> Vec<Player> {
        let players = self.players.read().await;
        players.values()
            .filter(|player| player.map_id == map_id && player.zone_id == zone_id)
            .cloned()
            .collect()
    }

    // Admin methods
    pub async fn set_player_admin(&self, player_id: u64, is_admin: bool) -> bool {
        self.update_player(player_id, |player| {
            player.is_admin = is_admin;
        }).await
    }

    pub async fn kick_player(&self, player_id: u64) -> bool {
        if let Some(player) = self.get_player(player_id).await {
            println!("[PLAYER_SERVICE] Kicking player {} ({})", player.name, player_id);
            self.remove_player(player_id).await
        } else {
            false
        }
    }

    // Cleanup methods
    pub async fn cleanup_disconnected_players(&self) -> usize {
        let mut players = self.players.write().await;
        let initial_count = players.len();
        
        players.retain(|_, player| player.session_id.is_some());
        
        let removed_count = initial_count - players.len();
        if removed_count > 0 {
            println!("[PLAYER_SERVICE] Cleaned up {} disconnected players", removed_count);
        }
        
        removed_count
    }
}

pub static PLAYER_SERVICE: Lazy<Arc<RwLock<PlayerService>>> = Lazy::new(|| {
    Arc::new(RwLock::new(PlayerService::new()))
});
