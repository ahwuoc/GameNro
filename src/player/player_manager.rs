use crate::player::player::Player;
use crate::player::player_actor::PlayerHandle;
use dashmap::DashMap;
use once_cell::sync::Lazy;

pub static PLAYER_MANAGER: Lazy<PlayerManager> = Lazy::new(|| PlayerManager::new());

pub struct PlayerManager {
    players: DashMap<u64, PlayerHandle>,
}

impl PlayerManager {
    pub fn new() -> Self {
        Self {
            players: DashMap::new(),
        }
    }

    pub fn add(&self, id: u64, handle: PlayerHandle) {
        self.players.insert(id, handle);
    }

    pub fn remove(&self, id: u64) {
        self.players.remove(&id);
    }

    pub fn get(&self, id: u64) -> Option<PlayerHandle> {
        self.players.get(&id).map(|p| p.value().clone())
    }

    pub fn get_ref(&self, id: u64) -> Option<dashmap::mapref::one::Ref<'_, u64, PlayerHandle>> {
        self.players.get(&id)
    }

    pub fn update(&self, id: u64, handle: PlayerHandle) {
        self.players.insert(id, handle);
    }

    pub fn contains(&self, id: u64) -> bool {
        self.players.contains_key(&id)
    }

    pub fn size(&self) -> usize {
        self.players.len()
    }

    pub fn iter(&self) -> dashmap::iter::Iter<'_, u64, PlayerHandle> {
        self.players.iter()
    }
}
