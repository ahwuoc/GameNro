// ── Sub-directories (grouped by domain) ──────────────────────
pub mod combat; // skill_service, effect_skill_service
pub mod player_svc; // player_service, player_info_service, player_tnsm_services
pub mod world; // task_service, task_utils, magic_tree, radar, intrinsic, black_ball_war

// ── Remaining top-level services ─────────────────────────────
pub mod services;

// ── Backward-compatible re-exports ───────────────────────────
// These allow existing `crate::services::X` imports to keep working
pub use combat::effect_skill_service;
pub use combat::skill_service;
pub use player_svc::player_info_service;
pub use player_svc::player_service;
pub use player_svc::player_tnsm_services;
pub use world::black_ball_war_service;
pub use world::intrinsic_service;
pub use world::magic_tree_service;
pub use world::radar_service;
pub use world::task_service;
pub use world::task_utils;

// ── Type re-exports ──────────────────────────────────────────
pub use services::ServiceHandles;
pub use world::intrinsic_service::IntrinsicService;
