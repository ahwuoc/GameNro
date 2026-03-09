use super::message::ClanMessage;
use crate::dungoen::doanh_trai::handle::DoanhTraiHandle;
use crate::models::clan::Clan;
use crate::models::clan::ClanMessage as ClanMsg;
use crate::player::player_actor::PlayerHandle;
use tokio::sync::mpsc;

#[derive(Clone, Debug)]
pub struct ClanHandle {
    pub id: i32,
    pub tx: mpsc::Sender<ClanMessage>,
}

impl ClanHandle {
    pub fn new(id: i32, tx: mpsc::Sender<ClanMessage>) -> Self {
        Self { id, tx }
    }

    pub async fn send(&self, msg: ClanMessage) -> anyhow::Result<()> {
        self.tx
            .send(msg)
            .await
            .map_err(|_| anyhow::anyhow!("Clan actor {} terminated", self.id))
    }

    pub fn send_forget(&self, msg: ClanMessage) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx.send(msg).await;
        });
    }

    pub async fn get_snapshot(&self) -> Option<Clan> {
        let (tx, rx) = tokio::sync::oneshot::channel();
        if self.send(ClanMessage::GetSnapshot(tx)).await.is_err() {
            return None;
        }
        rx.await.ok()
    }

    pub fn add_member_online(&self, handle: PlayerHandle) {
        self.send_forget(ClanMessage::AddMemberOnline(handle));
    }

    pub fn remove_member_online(&self, player_id: u64) {
        self.send_forget(ClanMessage::RemoveMemberOnline(player_id));
    }

    pub fn add_message(&self, cmg: ClanMsg) {
        self.send_forget(ClanMessage::AddMessage(cmg));
    }

    pub fn set_dungeon(&self, handle: DoanhTraiHandle) {
        self.send_forget(ClanMessage::JoinDungeon(handle));
    }

    pub fn update_power(&self, player_id: i32, power: i64) {
        self.send_forget(ClanMessage::UpdateMemberPower(player_id, power));
    }

    pub fn update_message(&self, msg: crate::models::clan::ClanMessage) {
        self.send_forget(ClanMessage::UpdateMessage(msg));
    }
}
