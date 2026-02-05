use std::sync::Arc;

use crate::boss::scripts::{default::DefaultScript, traits::BossScript};

pub fn get_script(template_id: &str) -> Arc<dyn BossScript> {
    match template_id {
        _ => Arc::new(DefaultScript),
    }
}
