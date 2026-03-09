use crate::player::player_actor::PlayerHandle;

pub enum DoanhTraiMessage {
    /// Mở doanh trại mới
    Open {
        clan_id: i32,
        opener_handle: PlayerHandle,
        teammate_handles: Vec<PlayerHandle>,
    },
    /// Player join vào doanh trại đang active
    Join { player_handle: PlayerHandle },
    /// Đóng doanh trại
    Shutdown,
    /// Query: đang active?
    IsActive(tokio::sync::oneshot::Sender<bool>),
    /// Query: clan_id hiện tại
    GetClanId(tokio::sync::oneshot::Sender<Option<i32>>),
    /// Query: thời gian còn lại (seconds)
    GetTimeLeft(tokio::sync::oneshot::Sender<i64>),
}
