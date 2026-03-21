use async_trait::async_trait;
use rand::Rng;

use crate::{
    boss::{self, boss_actor::BossActor, scripts::traits::BossScript},
    services::ServiceHandles,
};

pub struct BossNinjaCloneScript;

#[async_trait]
impl BossScript for BossNinjaCloneScript {
    fn name(&self) -> &'static str {
        boss::boss_id::BOSS_NINJA_AO_TIM_CLONE
    }

    async fn on_update(&self, actor: &mut BossActor) {
        actor.default_update().await;
    }

    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        if actor.player.is_die() {
            return 0;
        }

        if !piercing && rand::rng().random_bool(0.2) {
            actor.chat(&["Xí hụt"]);
            return 0;
        }

        let mut final_damage = damage / 2;

        if !piercing && actor.player.effect_skill.is_shield {
            final_damage /= 2;
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
