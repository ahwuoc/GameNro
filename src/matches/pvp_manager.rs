use crate::matches::{pvp::PvpMatch, TypeLosePvp};
use crate::player::player_actor::TypePk;
use std::sync::OnceLock;
use std::time::Duration;
use tokio::sync::{mpsc, oneshot};

static PVP_HANDLE: OnceLock<PvpHandle> = OnceLock::new();

pub fn init_pvp() {
    let (handle, actor) = PvpActor::new();
    handle.spawn_update_loop();
    tokio::spawn(actor.run());
    PVP_HANDLE
        .set(handle)
        .expect("PVP_HANDLE already initialized");
    tracing::info!("PvpActor started");
}

pub fn get_pvp_handle() -> &'static PvpHandle {
    PVP_HANDLE
        .get()
        .expect("PVP_HANDLE not initialized, call init_pvp() first")
}

pub enum PvpMessage {
    CreatePvp {
        pvp: Box<dyn PvpMatch>,
    },
    PlayerLose {
        player_id: i64,
        type_lose: TypeLosePvp,
    },
    CheckInPvp {
        player_id_1: i64,
        player_id_2: i64,
        reply: oneshot::Sender<bool>,
    },
    HasPvp {
        player_id: i64,
        reply: oneshot::Sender<bool>,
    },
    Update,
}

#[derive(Clone, Debug)]
pub struct PvpHandle {
    sender: mpsc::Sender<PvpMessage>,
}

pub struct PvpActor {
    receiver: mpsc::Receiver<PvpMessage>,
    pvps: Vec<Box<dyn PvpMatch>>,
}

impl PvpActor {
    pub fn new() -> (PvpHandle, Self) {
        let (tx, rx) = mpsc::channel(256);
        let handle = PvpHandle { sender: tx };
        let actor = Self {
            receiver: rx,
            pvps: Vec::new(),
        };
        (handle, actor)
    }

    pub async fn run(mut self) {
        while let Some(msg) = self.receiver.recv().await {
            match msg {
                PvpMessage::CreatePvp { mut pvp } => {
                    pvp.start();
                    self.pvps.push(pvp);
                }
                PvpMessage::PlayerLose {
                    player_id,
                    type_lose,
                } => {
                    if let Some(idx) = self.pvps.iter().position(|p| p.is_in_pvp(player_id)) {
                        let p1_id = self.pvps[idx].player1_id();
                        let p2_id = self.pvps[idx].player2_id();
                        self.pvps[idx].lose(player_id, type_lose);
                        crate::matches::pvp::change_type_pk(p1_id, TypePk::PkNon);
                        crate::matches::pvp::change_type_pk(p2_id, TypePk::PkNon);
                        self.pvps.swap_remove(idx);
                    }
                }
                PvpMessage::CheckInPvp {
                    player_id_1,
                    player_id_2,
                    reply,
                } => {
                    let result = self
                        .pvps
                        .iter()
                        .any(|p| p.is_in_pvp(player_id_1) && p.is_in_pvp(player_id_2));
                    let _ = reply.send(result);
                }
                PvpMessage::HasPvp { player_id, reply } => {
                    let result = self.pvps.iter().any(|p| p.is_in_pvp(player_id));
                    let _ = reply.send(result);
                }
                PvpMessage::Update => {
                    for pvp in self.pvps.iter_mut() {
                        pvp.update();
                    }
                }
            }
        }
    }
}

impl PvpHandle {
    pub fn create_pvp(&self, pvp: Box<dyn PvpMatch>) {
        let _ = self.sender.try_send(PvpMessage::CreatePvp { pvp });
    }

    pub fn player_lose(&self, player_id: i64, type_lose: TypeLosePvp) {
        let _ = self.sender.try_send(PvpMessage::PlayerLose {
            player_id,
            type_lose,
        });
    }

    pub async fn check_in_pvp(&self, p1_id: i64, p2_id: i64) -> bool {
        let (tx, rx) = oneshot::channel();
        let _ = self
            .sender
            .send(PvpMessage::CheckInPvp {
                player_id_1: p1_id,
                player_id_2: p2_id,
                reply: tx,
            })
            .await;
        rx.await.unwrap_or(false)
    }

    pub async fn has_pvp(&self, player_id: i64) -> bool {
        let (tx, rx) = oneshot::channel();
        let _ = self
            .sender
            .send(PvpMessage::HasPvp {
                player_id,
                reply: tx,
            })
            .await;
        rx.await.unwrap_or(false)
    }

    pub fn spawn_update_loop(&self) {
        let handle = self.clone();
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(Duration::from_secs(1));
            loop {
                interval.tick().await;
                let _ = handle.sender.send(PvpMessage::Update).await;
            }
        });
    }
}
