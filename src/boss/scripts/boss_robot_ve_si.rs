use async_trait::async_trait;
use rand::Rng;

use crate::{
    boss::{
        self,
        boss_actor::{BossActor, BossState},
        scripts::traits::BossScript,
    },
    map::ItemMap,
    services::{effect_skill_service::EffectSkillService, ServiceHandles},
    templates::item_template_manager,
};

pub struct BossRobotVeSiScript;

#[async_trait]
impl BossScript for BossRobotVeSiScript {
    fn name(&self) -> &'static str {
        boss::boss_id::BOSS_ROBOT_VE_SI
    }

    async fn on_update(&self, actor: &mut BossActor) {
        actor.default_update().await;
    }

    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        if actor.player.is_die() {
            return 0;
        }

        // Né đòn 20% (Java: Util.isTrue(this.nPoint.tlNeDon, 1000))
        if !piercing && rand::rng().random_ratio(20, 100) {
            actor.chat(&["Xí hụt"]);
            return 0;
        }

        let mut final_damage = damage / 2;

        if !piercing && actor.player.effect_skill.is_shield {
            if final_damage > actor.player.n_point.hp_max as u64 {
                EffectSkillService::break_shield(&mut actor.player);
            }
            final_damage /= 2;
        }

        let real_damage = actor.player.injured(final_damage, piercing);
        let _ = ServiceHandles::send_player_injured(&actor.player, real_damage as i32, false, 0);
        let _ = ServiceHandles::send_hp_sync(&actor.player);

        if actor.player.is_die() {
            actor.chat_end();
            actor.state = BossState::Changing;
        }

        real_damage
    }

    async fn on_death(&self, actor: &mut BossActor) {
        // Reward: 30% drop đậu thần (item id 17)
        let killer_id = actor.last_attacker_id.unwrap_or(0);
        if rand::rng().random_ratio(30, 100) {
            if let Some(template) = item_template_manager::get(17) {
                let mut item_map = ItemMap::new(
                    Some(template),
                    1,
                    actor.player.location.x as i32,
                    actor.player.location.y as i32 - 24,
                    killer_id as i64,
                );
                item_map.map_id = actor.player.map_id;
                item_map.zone_id = actor.player.zone_id;
                let _ = actor.zone_handle.add_item(item_map).await;
            }
        }
        actor.default_death().await;
    }
}
