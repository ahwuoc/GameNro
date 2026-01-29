use crate::player::player::Player;
use dashmap::DashMap;
use once_cell::sync::Lazy;

pub static PLAYER_MANAGER: Lazy<PlayerManager> = Lazy::new(|| PlayerManager::new());

pub struct PlayerManager {
    players: DashMap<u64, Player>,
}

impl PlayerManager {
    pub fn new() -> Self {
        Self {
            players: DashMap::new(),
        }
    }

    pub fn add(&self, player: Player) {
        self.players.insert(player.id, player);
    }

    pub fn remove(&self, id: u64) {
        self.players.remove(&id);
    }

    pub fn get(&self, id: u64) -> Option<Player> {
        self.players.get(&id).map(|p| p.clone())
    }

    pub fn get_mut(&self, id: u64) -> Option<dashmap::mapref::one::RefMut<'_, u64, Player>> {
        self.players.get_mut(&id)
    }

    pub fn update(&self, player: Player) {
        self.players.insert(player.id, player);
    }

    pub fn contains(&self, id: u64) -> bool {
        self.players.contains_key(&id)
    }

    pub fn size(&self) -> usize {
        self.players.len()
    }

    pub fn iter_mut(&self) -> dashmap::iter::IterMut<'_, u64, Player> {
        self.players.iter_mut()
    }
}
