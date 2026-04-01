pub mod combat;
pub mod fusion;
pub mod inventory;
pub mod magic_tree;
pub mod map;
pub mod misc;
pub mod network;
pub mod pet;
pub mod skill;
pub mod task;

// Re-export handler structs for convenience
pub use combat::CombatHandler;
pub use fusion::FusionHandler;
pub use inventory::InventoryHandler;
pub use magic_tree::MagicTreeHandler;
pub use map::MapHandler;
pub use misc::MiscHandler;
pub use network::NetworkHandler;
pub use pet::PetHandler;
pub use skill::SkillHandler;
pub use task::TaskHandler;
