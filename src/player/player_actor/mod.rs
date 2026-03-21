pub mod handle;
pub mod message;
pub mod pet;
pub mod player_actor;

pub use handle::PlayerHandle;
pub use message::{MagicTreeMsg, PlayerMessage};
pub use player_actor::PlayerActor;
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TypePk {
    PkNon = 0,
    PkPvp = 3,
    PkPvp2 = 4,
    PkAll = 5,
}
