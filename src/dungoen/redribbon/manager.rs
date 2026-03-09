use crate::dungoen::redribbon::actor::RedRibbonActor;
use crate::dungoen::redribbon::handle::RedRibbonHandle;
use dashmap::DashMap;
use std::sync::{Arc, LazyLock};
use tokio::sync::mpsc;

pub struct RedRibbonManager {
    dungeons: DashMap<i32, RedRibbonHandle>,
}

static INSTANCE: LazyLock<Arc<RedRibbonManager>> =
    LazyLock::new(|| Arc::new(RedRibbonManager::new()));

pub fn get() -> Arc<RedRibbonManager> {
    INSTANCE.clone()
}

impl RedRibbonManager {
    fn new() -> Self {
        Self {
            dungeons: DashMap::new(),
        }
    }

    pub fn get_dungeon(&self, clan_id: i32) -> Option<RedRibbonHandle> {
        self.dungeons.get(&clan_id).map(|v| v.value().clone())
    }

    pub fn create_dungeon(&self, clan_id: i32, map_id: i32) -> RedRibbonHandle {
        let (tx, rx) = mpsc::channel(100);
        let handle = RedRibbonHandle::new(clan_id, tx);

        let mut actor = RedRibbonActor::new(clan_id, map_id, rx);
        tokio::spawn(async move {
            actor.run().await;
        });

        self.dungeons.insert(clan_id, handle.clone());
        handle
    }

    pub fn remove_dungeon(&self, clan_id: i32) {
        self.dungeons.remove(&clan_id);
    }
}
