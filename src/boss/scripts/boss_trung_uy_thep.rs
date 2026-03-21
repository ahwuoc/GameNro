use async_trait::async_trait;
use rand::Rng;

use crate::{
    boss::{self, boss_actor::BossActor, scripts::traits::BossScript},
    services::ServiceHandles,
    utils::Location,
};

pub struct BossTrungUyThepScript;

impl BossTrungUyThepScript {
    fn get_y(&self, x: i16) -> i16 {
        if x < 638 || x > 966 {
            240
        } else if x < 707 {
            264
        } else if x > 949 {
            288
        } else {
            312
        }
    }
}

#[async_trait]
impl BossScript for BossTrungUyThepScript {
    fn name(&self) -> &'static str {
        boss::boss_id::BOSS_TRUNG_UY_THEP
    }

    async fn on_update(&self, actor: &mut BossActor) {
        if let Some(target_id) = actor.target_id {
            if let Ok(Some(handle)) = actor.zone_handle.get_player(target_id).await {
                if let Some(snapshot) = handle.get_snapshot().await {
                    if snapshot.location.x < 640 || snapshot.location.x > 980 {
                        actor.move_to(884, 312).await;
                        actor.target_id = None;
                    }
                }
            }
        }

        actor.default_update().await;
    }

    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        if actor.player.is_die() {
            return 0;
        }

        // 20% né tránh
        if !piercing && rand::rng().random_bool(0.2) {
            actor.chat(&["Xí hụt"]);
            return 0;
        }

        // Giảm 50% sát thương nhận vào (innate)
        let mut final_damage = damage / 2;

        // Nếu có giáp thì giảm thêm 50%
        if !piercing && actor.player.effect_skill.is_shield {
            if final_damage > actor.player.n_point.hp_max as u64 {
                //    todo! pha giap
            }
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

    async fn on_move(&self, actor: &mut BossActor, target_x: i16, _target_y: i16) {
        // Áp dụng terrain elevation của Trung Úy Thép
        let y = self.get_y(target_x);
        actor.default_move(target_x, y).await;
    }
}
