pub mod actor;
pub mod handle;
pub mod message;
pub mod pet;

pub use actor::PlayerActor;
pub use handle::PlayerHandle;
pub use message::PlayerMessage;
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Type_PK {
    PK_NON = 0,
    PK_PVP = 3,
    PK_PVP2 = 4,
    PK_ALL = 5,
}
