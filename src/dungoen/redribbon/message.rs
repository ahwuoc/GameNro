use crate::models::clan::Clan;
use crate::player::player_actor::PlayerHandle;
use tokio::sync::oneshot;

#[derive(Debug)]
pub enum RedRibbonMessage {
    GetSnapshot(oneshot::Sender<RedRibbonSnapshot>),
    AddPlayer(PlayerHandle),
    RemovePlayer(u64),
    Finish,
    Close,
}

#[derive(Debug, Clone)]
pub struct RedRibbonSnapshot {
    pub clan_id: i32,
    pub map_id: i32,
    pub start_time: i64,
}
