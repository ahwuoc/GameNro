pub mod actor;
pub mod handle;
pub mod message;
pub mod pet;

pub use actor::PlayerActor;
pub use handle::PlayerHandle;
pub use message::PlayerMessage;
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TypePk {
    PkNon = 0,
    PkPvp = 3,
    PkPvp2 = 4,
    PkAll = 5,
}
