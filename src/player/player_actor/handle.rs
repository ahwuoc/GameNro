use crate::player::player::Player;
use crate::player::player_actor::message::PlayerMessage;
use anyhow::Result;
use tokio::sync::mpsc;

#[derive(Clone, Debug)]
pub struct BossInfo {
    pub group_id: Option<u64>,
    pub group_index: i32,
    pub template_id: String,
}

use std::sync::Arc;
use tokio::sync::RwLock;

#[derive(Clone, Debug)]
pub struct PlayerHandle {
    pub id: u64,
    pub is_pet: bool,
    pub boss_info: Option<BossInfo>,
    pub tx: mpsc::Sender<PlayerMessage>,
    pub public_state: Arc<RwLock<crate::player::player::PlayerPublicState>>,
}

impl PlayerHandle {
    pub fn new(
        id: u64,
        is_pet: bool,
        tx: mpsc::Sender<PlayerMessage>,
        public_state: Arc<RwLock<crate::player::player::PlayerPublicState>>,
    ) -> Self {
        Self {
            id,
            is_pet,
            boss_info: None,
            tx,
            public_state,
        }
    }

    pub async fn send(&self, msg: PlayerMessage) -> Result<()> {
        self.tx
            .send(msg)
            .await
            .map_err(|_| anyhow::anyhow!("Actor terminated"))
    }

    pub async fn get_snapshot(&self) -> Option<Player> {
        let (tx, rx) = tokio::sync::oneshot::channel();
        if self.tx.send(PlayerMessage::GetSnapshot(tx)).await.is_err() {
            return None;
        }
        rx.await.ok()
    }

    pub fn send_forget(&self, msg: PlayerMessage) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx.send(msg).await;
        });
    }
}
