use std::sync::Arc;

use crate::boss::boss_id::{BOSS_TAU_PAY_PAY, BOSS_THAN_MEO_KARIN, BOSS_YAJIRO};
use crate::boss::scripts::{default::DefaultScript, training::TrainingScript, traits::BossScript};

pub fn get_script(template_id: &str) -> Arc<dyn BossScript> {
    match template_id {
        BOSS_THAN_MEO_KARIN | BOSS_YAJIRO | BOSS_TAU_PAY_PAY => Arc::new(TrainingScript),
        _ => Arc::new(DefaultScript),
    }
}
