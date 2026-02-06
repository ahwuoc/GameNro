use crate::player::Player;

pub mod actor;
pub mod handle;
pub mod message;
pub mod service;

pub use actor::PetActor;
pub use handle::PetHandle;
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
}
