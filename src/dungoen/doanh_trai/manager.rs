use super::actor::DoanhTraiActor;
use super::handle::DoanhTraiHandle;
use super::message::DoanhTraiMessage;
use crate::player::player_actor::PlayerHandle;
use tokio::sync::mpsc;
use tracing::info;

use std::sync::OnceLock;

static INSTANCE: OnceLock<DoanhTraiManager> = OnceLock::new();

const MAX_SLOTS: usize = 50;

pub struct DoanhTraiManager {
    pub handles: Vec<DoanhTraiHandle>,
}

impl DoanhTraiManager {
    pub fn init() -> Self {
        let mut handles = Vec::with_capacity(MAX_SLOTS);

        for i in 0..MAX_SLOTS {
            let (tx, rx) = mpsc::channel(32);
            let actor = DoanhTraiActor::new(i as i32, tx.clone(), rx);
            tokio::spawn(actor.run());
            handles.push(DoanhTraiHandle::new(i as i32, tx));
        }

        info!("DoanhTraiManager initialized with {} slots", MAX_SLOTS);
        Self { handles }
    }
    pub async fn find_available(&self) -> Option<&DoanhTraiHandle> {
        for h in &self.handles {
            if !h.is_active().await {
                return Some(h);
            }
        }
        None
    }
    pub async fn find_by_clan(&self, clan_id: i32) -> Option<&DoanhTraiHandle> {
        for h in &self.handles {
            if let Some(cid) = h.get_clan_id().await {
                if cid == clan_id {
                    return Some(h);
                }
            }
        }
        None
    }
    pub async fn join_doanh_trai(
        &self,
        clan_id: i32,
        player: PlayerHandle,
        teammates: Vec<PlayerHandle>,
    ) -> Result<(), String> {
        if let Some(h) = self.find_by_clan(clan_id).await {
            let _ = h
                .send(DoanhTraiMessage::Join {
                    player_handle: player,
                })
                .await;
            return Ok(());
        }
        if let Some(h) = self.find_available().await {
            let _ = h
                .send(DoanhTraiMessage::Open {
                    clan_id,
                    opener_handle: player,
                    teammate_handles: teammates,
                })
                .await;
            return Ok(());
        }

        Err("Doanh trại đã đầy, hãy quay lại vào lúc khác!".to_string())
    }
}
pub fn global_init() {
    INSTANCE.get_or_init(|| DoanhTraiManager::init());
}
pub fn get() -> &'static DoanhTraiManager {
    INSTANCE
        .get()
        .expect("DoanhTraiManager chưa được init! Gọi global_init() trước.")
}
