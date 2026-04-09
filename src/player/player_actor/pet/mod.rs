use crate::player::Player;

pub mod handle;
pub mod message;
pub mod pet_actor;
pub mod service;

pub use handle::PetHandle;
pub use pet_actor::PetActor;
pub use service::PetService;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PetStatus {
    Follow = 0,
    Protect = 1,
    Attack = 2,
    GoHome = 3,
    Fusion = 4,
    HTVV = 5,
}

impl TryFrom<i8> for PetStatus {
    type Error = anyhow::Error;

    fn try_from(value: i8) -> Result<Self, Self::Error> {
        match value {
            0 => Ok(PetStatus::Follow),
            1 => Ok(PetStatus::Protect),
            2 => Ok(PetStatus::Attack),
            3 => Ok(PetStatus::GoHome),
            4 => Ok(PetStatus::Fusion),
            5 => Ok(PetStatus::HTVV),
            _ => Err(anyhow::anyhow!("Invalid PetStatus value: {}", value)),
        }
    }
}

#[derive(Debug, Clone)]
pub struct Pet {
    pub player: Player,
    pub master_id: u64,
    pub status: PetStatus,
    pub type_pet: i8,
    pub is_tranform: bool,
    pub last_time_die: u64,
    pub last_time_unfusion: u64,
    pub is_gohome: bool,
    pub master_location: Option<(i16, i16)>,
    pub target_mob_id: Option<u64>,
    pub target_player_id: Option<u64>,
    pub last_time_chat: u64,
    pub chat_index: usize,
    pub last_time_idle_move: u64,
    pub last_time_ask_pea: u64,
    pub last_time_stamina_update: u64,
    pub last_time_gohome: u64,
}
