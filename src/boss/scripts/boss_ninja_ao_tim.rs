use std::sync::atomic::{AtomicBool, Ordering};

use async_trait::async_trait;
use rand::Rng;

use crate::{
    boss::{self, boss_actor::BossActor, manager::BossManager, scripts::traits::BossScript},
    services::ServiceHandles,
};

pub struct BossNinjaAoTimScript {
    called_ninja: AtomicBool,
}
impl BossNinjaAoTimScript {
    pub fn new() -> Self {
        Self {
            called_ninja: AtomicBool::new(false),
        }
    }
}

#[async_trait]
impl BossScript for BossNinjaAoTimScript {
    fn name(&self) -> &'static str {
        boss::boss_id::BOSS_NINJA_AO_TIM
    }

    async fn on_update(&self, actor: &mut BossActor) {
        actor.default_update().await;
    }
    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        if actor.player.is_die() {
            return 0;
        }

        if !piercing && rand::rng().random_bool(0.3) {
            actor.chat(&["Xí hụt"]);
            return 0;
        }
        let mut final_damage = damage / 2;
        if !piercing && actor.player.effect_skill.is_shield {
            final_damage /= 2;
        }
        if actor.player.n_point.hp_current <= actor.player.n_point.hp_max / 2
            && !self.called_ninja.load(Ordering::SeqCst)
        {
            if rand::rng().random_bool(0.8) {
                let count_boss = rand::rng().random_range(4..=6);
                for _ in 0..count_boss {
                    BossManager::spawn_boss_async(
                        boss::boss_id::BOSS_NINJA_AO_TIM_CLONE.to_string(),
                        actor.player.map_id,
                        actor.player.zone_id,
                        actor.player.location.x + rand::rng().random_range(-100..100),
                        actor.player.location.y,
                        None,
                        -1,
                        Vec::new(),
                        None,
                        None,
                    );
                }
            }
            self.called_ninja.store(true, Ordering::SeqCst);
            return 0;
        }
        let real_damage = actor.player.injured(final_damage, piercing);
        let _ = ServiceHandles::send_player_injured(&actor.player, real_damage as i32, false, 0);
        let _ = ServiceHandles::send_hp_sync(&actor.player);

        if actor.player.is_die() {
            actor.chat_end();
            actor.state = boss::boss_actor::BossState::Changing;
        }
        real_damage
    }
}
