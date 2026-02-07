use async_trait::async_trait;

use crate::{boss::actor::BossActor, models::skill_model};

#[async_trait]
pub trait BossScript: Send + Sync {
    fn name(&self) -> &'static str;

    // =============== Lifecycle hooks =============

    /// Gọi khi boss spawn xong
    async fn on_spawn(&self, actor: &mut BossActor) {
        // Default: không làm gì
    }

    /// Gọi mỗi tick - main update loop
    async fn on_update(&self, actor: &mut BossActor) {
        actor.default_update().await;
    }

    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        actor.default_injured(damage, piercing).await
    }
    async fn on_death(&self, actor: &mut BossActor) {
        actor.default_death().await;
    }
    async fn on_stage_change(&self, actor: &mut BossActor, new_stage: usize) {
        actor.default_stage_change(new_stage).await;
    }

    // =============== Combat hooks =============

    /// Tìm target để đánh
    async fn find_target(&self, actor: &BossActor) -> Option<u64> {
        actor.default_find_target().await
    }

    /// Chọn skill để dùng
    fn choose_skill(&self, actor: &BossActor) -> Option<skill_model::Skill> {
        actor.default_choose_skill()
    }

    /// Thực hiện tấn công
    async fn do_attack(&self, actor: &mut BossActor, target_id: u64) {
        actor.default_attack(target_id).await;
    }

    /// Xử lý di chuyển
    async fn on_move(&self, actor: &mut BossActor, target_x: i16, target_y: i16) {
        actor.default_move(target_x, target_y).await;
    }

    // =============== Chat hooks =============

    /// Chat khi xuất hiện
    fn chat_on_appear(&self, actor: &BossActor) -> Vec<String> {
        actor.default_chat_appear()
    }

    /// Chat random khi đánh nhau
    fn chat_on_fighting(&self, actor: &BossActor) -> Option<String> {
        actor.default_chat_fighting()
    }

    /// Chat khi chết
    fn chat_on_death(&self, actor: &BossActor) -> Vec<String> {
        actor.default_chat_death()
    }
}
