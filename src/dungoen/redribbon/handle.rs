use crate::dungoen::redribbon::message::{RedRibbonMessage, RedRibbonSnapshot};
use tokio::sync::mpsc;

#[derive(Debug, Clone)]
pub struct RedRibbonHandle {
    pub clan_id: i32,
    pub tx: mpsc::Sender<RedRibbonMessage>,
}

impl RedRibbonHandle {
    pub fn new(clan_id: i32, tx: mpsc::Sender<RedRibbonMessage>) -> Self {
        Self { clan_id, tx }
    }

    pub fn send_forget(&self, msg: RedRibbonMessage) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            if let Err(e) = tx.send(msg).await {
                tracing::error!("RedRibbonHandle send error: {:?}", e);
            }
        });
    }

    pub async fn send(&self, msg: RedRibbonMessage) -> anyhow::Result<()> {
        self.tx
            .send(msg)
            .await
            .map_err(|e| anyhow::anyhow!("RedRibbonHandle send error: {:?}", e))
    }

    pub async fn get_snapshot(&self) -> Option<RedRibbonSnapshot> {
        let (tx, rx) = tokio::sync::oneshot::channel();
        self.send(RedRibbonMessage::GetSnapshot(tx)).await.ok()?;
        rx.await.ok()
    }
}
