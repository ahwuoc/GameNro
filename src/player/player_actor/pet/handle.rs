use crate::player::player_actor::pet::message::PetMessage;
use anyhow::Result;
use tokio::sync::mpsc;

#[derive(Clone, Debug)]
pub struct PetHandle {
    pub id: u64,
    pub tx: mpsc::Sender<crate::player::player_actor::message::PlayerMessage>,
}

impl PetHandle {
    pub fn new(
        id: u64,
        tx: mpsc::Sender<crate::player::player_actor::message::PlayerMessage>,
    ) -> Self {
        Self { id, tx }
    }

    pub async fn send(
        &self,
        msg: crate::player::player_actor::pet::message::PetMessage,
    ) -> anyhow::Result<()> {
        self.tx
            .send(crate::player::player_actor::message::PlayerMessage::Pet(
                msg,
            ))
            .await
            .map_err(|e| anyhow::anyhow!(e.to_string()))
    }

    pub fn send_forget(&self, msg: crate::player::player_actor::pet::message::PetMessage) {
        let tx = self.tx.clone();
        tokio::spawn(async move {
            let _ = tx
                .send(crate::player::player_actor::message::PlayerMessage::Pet(
                    msg,
                ))
                .await;
        });
    }
}
