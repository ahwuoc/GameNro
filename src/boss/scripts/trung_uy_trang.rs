use async_trait::async_trait;
use rand::Rng;

use crate::boss::boss_actor::{BossActor, BossState};
use crate::boss::scripts::traits::BossScript;
use crate::map::ItemMap;
use crate::services::ServiceHandles;
use crate::templates::item_template_manager;

pub struct TrungUyTrangScript;

#[async_trait]
impl BossScript for TrungUyTrangScript {
    fn name(&self) -> &'static str {
        crate::boss::boss_id::BOSS_TRUNG_UY_TRANG
    }

    async fn on_update(&self, actor: &mut BossActor) {
        actor.default_update().await;
    }

    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        if actor.player.is_die() {
            return 0;
        }

        if let Ok(mobs) = actor.zone_handle.get_all_mobs().await {
            let bulon_alive = mobs
                .iter()
                .any(|m| m.template_id == 22 && m.hp > 0 && m.is_alive);
            if bulon_alive {
                return 0;
            }
        }

        if !piercing && rand::rng().random_ratio(20, 100) {
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
            actor.state = BossState::Changing;
        }
        real_damage
    }

    async fn on_death(&self, actor: &mut BossActor) {
        let killer_id = actor.last_attacker_id.unwrap_or(0);
        if rand::rng().random_ratio(50, 100) {
            if let Some(template) = item_template_manager::get(17) {
                let mut item_map = ItemMap::new(
                    Some(template),
                    1,
                    actor.player.location.x as i32,
                    actor.player.location.y as i32,
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
