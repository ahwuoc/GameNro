use crate::dungoen::redribbon::message::{RedRibbonMessage, RedRibbonSnapshot};
use tokio::sync::mpsc;
use tracing::info;

pub struct RedRibbonActor {
    pub clan_id: i32,
    pub map_id: i32,
    pub start_time: i64,
    pub rx: mpsc::Receiver<RedRibbonMessage>,
    pub player_handles: Vec<crate::player::player_actor::PlayerHandle>,
}

impl RedRibbonActor {
    pub fn new(clan_id: i32, map_id: i32, rx: mpsc::Receiver<RedRibbonMessage>) -> Self {
        Self {
            clan_id,
            map_id,
            start_time: crate::utils::time::current_time_millis() as i64,
            rx,
            player_handles: Vec::new(),
        }
    }

    pub async fn run(&mut self) {
        info!("RedRibbonActor started for clan {}", self.clan_id);
        while let Some(msg) = self.rx.recv().await {
            match msg {
                RedRibbonMessage::GetSnapshot(tx) => {
                    let snapshot = RedRibbonSnapshot {
                        clan_id: self.clan_id,
                        map_id: self.map_id,
                        start_time: self.start_time,
                    };
                    let _ = tx.send(snapshot);
                }
                RedRibbonMessage::AddPlayer(handle) => {
                    self.player_handles.push(handle);
                }
                RedRibbonMessage::RemovePlayer(id) => {
                    self.player_handles.retain(|h| h.id != id);
                    if self.player_handles.is_empty() {
                        // Optional: close if empty after some time
                    }
                }
                RedRibbonMessage::Finish | RedRibbonMessage::Close => {
                    break;
                }
            }
        }
        info!("RedRibbonActor finished for clan {}", self.clan_id);
        crate::dungoen::redribbon::manager::get().remove_dungeon(self.clan_id);
    }
}
