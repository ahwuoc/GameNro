use super::message::DoanhTraiMessage;
use tokio::sync::mpsc;

#[derive(Clone, Debug)]
pub struct DoanhTraiHandle {
    pub id: i32,
    pub tx: mpsc::Sender<DoanhTraiMessage>,
}

impl DoanhTraiHandle {
    pub fn new(id: i32, tx: mpsc::Sender<DoanhTraiMessage>) -> Self {
        Self { id, tx }
    }

    pub async fn send(&self, msg: DoanhTraiMessage) -> anyhow::Result<()> {
        self.tx
            .send(msg)
            .await
            .map_err(|_| anyhow::anyhow!("DoanhTrai actor terminated"))
    }

    pub fn send_forget(&self, msg: DoanhTraiMessage) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx.send(msg).await;
        });
    }

    pub async fn is_active(&self) -> bool {
        let (tx, rx) = tokio::sync::oneshot::channel();
        if self.tx.send(DoanhTraiMessage::IsActive(tx)).await.is_err() {
            return false;
        }
        rx.await.unwrap_or(false)
    }

    pub async fn get_clan_id(&self) -> Option<i32> {
        let (tx, rx) = tokio::sync::oneshot::channel();
        if self.tx.send(DoanhTraiMessage::GetClanId(tx)).await.is_err() {
            return None;
        }
        rx.await.unwrap_or(None)
    }

    pub async fn get_time_left(&self) -> i64 {
        let (tx, rx) = tokio::sync::oneshot::channel();
        if self
            .tx
            .send(DoanhTraiMessage::GetTimeLeft(tx))
            .await
            .is_err()
        {
            return 0;
        }
        rx.await.unwrap_or(0)
    }
}
