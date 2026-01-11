use crate::mob::RtMob;
use std::sync::Arc;
use tokio::sync::RwLock;

// This service is likely redundant now that mobs are managed directly by Zones/Maps.
// Keeping a minimal structure if needed for global operations, but purely zone-based logic is preferred.
pub struct MobService {
    // Global tracking might be removed in favor of distributed Zone management
}

impl MobService {
    pub fn new() -> Self {
        Self {}
    }
}
