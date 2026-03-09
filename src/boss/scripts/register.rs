use std::sync::Arc;

use crate::boss::boss_id::{
    BOSS_TAU_PAY_PAY, BOSS_THAN_MEO_KARIN, BOSS_TRUNG_UY_TRANG, BOSS_TRUNG_UY_XANH_LO, BOSS_YAJIRO,
};
use crate::boss::scripts::{
    default::DefaultScript, training::TrainingScript, traits::BossScript,
    trung_uy_trang::TrungUyTrangScript, trung_uy_xanh_lo::TrungUyXanhLoScript,
};

pub fn get_script(template_id: &str) -> Arc<dyn BossScript> {
    match template_id {
        BOSS_THAN_MEO_KARIN | BOSS_YAJIRO | BOSS_TAU_PAY_PAY => Arc::new(TrainingScript),
        BOSS_TRUNG_UY_XANH_LO => Arc::new(TrungUyXanhLoScript),
        BOSS_TRUNG_UY_TRANG => Arc::new(TrungUyTrangScript),
        _ => Arc::new(DefaultScript),
    }
}
