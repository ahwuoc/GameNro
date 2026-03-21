use std::sync::Arc;

use crate::boss::boss_id::{
    BOSS_NINJA_AO_TIM, BOSS_NINJA_AO_TIM_CLONE, BOSS_TAU_PAY_PAY, BOSS_THAN_MEO_KARIN,
    BOSS_TRUNG_UY_THEP, BOSS_TRUNG_UY_TRANG, BOSS_TRUNG_UY_XANH_LO, BOSS_YAJIRO,
};
use crate::boss::scripts::boss_trung_uy_thep::BossTrungUyThepScript;
use crate::boss::scripts::{
    boss_ninja_ao_tim::BossNinjaAoTimScript, boss_ninja_clone::BossNinjaCloneScript,
    default::DefaultScript, training::TrainingScript, traits::BossScript,
    trung_uy_trang::TrungUyTrangScript, trung_uy_xanh_lo::TrungUyXanhLoScript,
};

pub fn get_script(template_id: &str) -> Arc<dyn BossScript> {
    match template_id {
        BOSS_THAN_MEO_KARIN | BOSS_YAJIRO | BOSS_TAU_PAY_PAY => Arc::new(TrainingScript),
        BOSS_TRUNG_UY_XANH_LO => Arc::new(TrungUyXanhLoScript),
        BOSS_TRUNG_UY_TRANG => Arc::new(TrungUyTrangScript),
        BOSS_TRUNG_UY_THEP => Arc::new(BossTrungUyThepScript),
        BOSS_NINJA_AO_TIM => Arc::new(BossNinjaAoTimScript::new()),
        BOSS_NINJA_AO_TIM_CLONE => Arc::new(BossNinjaCloneScript),
        _ => Arc::new(DefaultScript),
    }
}
