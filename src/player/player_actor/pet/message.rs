use crate::player::player_actor::pet::PetStatus;

pub enum PetMessage {
    ChangeStatus(PetStatus),
    UpdateTick,
    MasterLocation(i16, i16),
    MasterAttackTarget(Option<u64>, Option<u32>),
    Fusion(bool),
    GetSnapshot(tokio::sync::oneshot::Sender<crate::player::player_actor::pet::Pet>),
}
