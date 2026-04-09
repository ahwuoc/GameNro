use std::time::Instant;

use async_trait::async_trait;
use rand::Rng;

use crate::boss::boss_actor::{BossActor, BossState};
use crate::boss::boss_id::{BOSS_TAU_PAY_PAY, BOSS_THAN_MEO_KARIN, BOSS_YAJIRO};
use crate::boss::scripts::traits::BossScript;
use crate::constant::task_type::TaskType;
use crate::map::services::training_services;
use crate::network::message::Message;
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::TypePk;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::ServiceHandles;

pub struct TrainingScript;

#[async_trait]
impl BossScript for TrainingScript {
    fn name(&self) -> &'static str {
        "training"
    }

    async fn on_spawn(&self, actor: &mut BossActor) {
        actor.player.type_pk = TypePk::PkAll;
        let _ = ServiceHandles::send_type_pk(&actor.player);
        if let Some(attacker_id) = actor.attacker_player_id {
            if let Ok(Some(handle)) = actor.zone_handle.get_player(attacker_id).await {
                let mut msg = Message::new(-30);
                let _ = msg.write_byte(35);
                let _ = msg.write_int(actor.player.id as i64 as i32);
                let _ = msg.write_byte(3);
                handle.send_forget(PlayerMessage::SendPacket(msg.clone()));

                let mut msg2 = Message::new(-30);
                let _ = msg2.write_byte(35);
                let _ = msg2.write_int(attacker_id as i64 as i32);
                let _ = msg2.write_byte(3);
                handle.send_forget(PlayerMessage::SendPacket(msg2));
            }
        }
    }

    async fn on_update(&self, actor: &mut BossActor) {
        if actor.last_update.elapsed().as_millis() < 1000 {
            return;
        }
        actor.last_update = Instant::now();

        match actor.state {
            BossState::Appearing => {
                actor.handle_appear().await;
            }
            BossState::Chatting => {
                actor.handle_chatting().await;
            }
            BossState::Fighting => {
                self.training_fighting(actor).await;
            }
            BossState::Changing => {
                self.training_on_defeat(actor).await;
            }
            _ => {}
        }
    }

    async fn on_injured(&self, actor: &mut BossActor, damage: u64, piercing: bool) -> u64 {
        if actor.player.is_die() {
            return 0;
        }

        if !piercing && rand::rng().random_ratio(400, 1000) {
            actor.chat(&["Xí hụt"]);
            return 0;
        }

        let real_damage = actor.player.injured(damage, piercing);

        ServiceHandles::send_player_injured(&actor.player, real_damage as i32, false, 0);
        ServiceHandles::send_hp_sync(&actor.player);

        if actor.player.n_point.hp_current > 0
            && actor.player.n_point.hp_current < actor.player.n_point.hp_max / 5
        {
            let texts = ["AAAAAAAAA", "ai da"];
            actor.chat(&texts);
        }

        if actor.player.is_die() {
            actor.chat_end();
            actor.state = BossState::Changing;
        }
        real_damage
    }

    async fn on_death(&self, actor: &mut BossActor) {
        actor.chat_end();
        actor.state = BossState::Changing;
    }

    async fn find_target(&self, actor: &BossActor) -> Option<u64> {
        actor.attacker_player_id
    }
}

impl TrainingScript {
    async fn training_fighting(&self, actor: &mut BossActor) {
        if actor.player.is_die() {
            actor.state = BossState::Changing;
            return;
        }
        let target_id = match actor.attacker_player_id {
            Some(id) => id,
            None => return,
        };
        let pl_handle = match actor.zone_handle.get_player(target_id).await {
            Ok(Some(h)) => h,
            _ => {
                self.training_leave(actor).await;
                return;
            }
        };

        let target_snapshot = match pl_handle.get_snapshot().await {
            Some(s) => s,
            None => return,
        };

        if target_snapshot.is_die() {
            actor.chat(&["Luyện tập tiếp đi"]);
            actor.player.type_pk = TypePk::PkNon;
            ServiceHandles::send_type_pk(&actor.player);
            let mut msg = Message::new(-30);
            msg.write_byte(35);
            msg.write_int(target_id as i32);
            msg.write_byte(0);
            pl_handle.send_forget(PlayerMessage::SendPacket(msg));
            let afks_time = 5000;
            tokio::time::sleep(tokio::time::Duration::from_millis(afks_time)).await;
            self.training_leave(actor).await;
            return;
        }

        if actor.template_id == BOSS_THAN_MEO_KARIN {
            let offset_x = rand::rng().random_range(-80..80i16);
            let target_x = target_snapshot.location.x.saturating_add(offset_x);
            let target_y = target_snapshot.location.y.saturating_sub(100);

            if rand::rng().random_ratio(1, 5) {
                actor.move_to(target_x, target_y);
            }
        }
        let dist = actor.calculate_distance(target_snapshot.location);
        if let Some(skill) = actor.choose_skill() {
            let range = match skill.template_id {
                0 | 2 | 4 => 50,
                1 | 3 | 5 => 300,
                _ => 150,
            };

            if dist > range as f32 {
                actor
                    .move_to(target_snapshot.location.x, target_snapshot.location.y);
            } else {
                if rand::rng().random_ratio(15, 100) {
                    let dodge_x = target_snapshot
                        .location
                        .x
                        .saturating_add(rand::rng().random_range(-80..80i16));
                    actor.move_to(dodge_x, target_snapshot.location.y);
                }
                actor.use_skill(skill, target_id).await;
            }
        } else {
            if dist > 200.0 {
                actor
                    .move_to(target_snapshot.location.x, target_snapshot.location.y);
            }
        }

        if rand::rng().random_ratio(5, 100) {
            actor.chat_random_middle();
        }
    }

    async fn training_on_defeat(&self, actor: &mut BossActor) {
        let attacker_id = actor.attacker_player_id;
        let is_thachdau = if let Some(id) = attacker_id {
            if let Ok(Some(handle)) = actor.zone_handle.get_player(id).await {
                if let Some(snapshot) = handle.get_snapshot().await {
                    snapshot.interaction_state.is_thachdau
                } else {
                    false
                }
            } else {
                false
            }
        } else {
            false
        };
        // ==========Player state thach dau thi thuc hien task action
        if is_thachdau || actor.template_id == BOSS_TAU_PAY_PAY {
            if let Some(id) = attacker_id {
                if let Ok(Some(handle)) = actor.zone_handle.get_player(id).await {
                    handle.send_forget(PlayerMessage::Modify(Box::new(|p| {
                        p.level_luyentap += 1;
                    })));
                    println!(
                        "Send task action to player {} template_id boss {}",
                        id, actor.template_id
                    );
                    handle.send_forget(PlayerMessage::TaskAction(
                        TaskType::KillBoss,
                        actor.template_id.clone(),
                    ));
                }
            }
        }
        self.training_leave(actor).await;
    }

    async fn training_leave(&self, actor: &mut BossActor) {
        if let Some(attacker_id) = actor.attacker_player_id {
            let handle = match actor.zone_handle.get_player(attacker_id).await {
                Ok(Some(h)) => Some(h),
                _ => PLAYER_MANAGER
                    .get_ref(attacker_id)
                    .map(|entry| entry.value().clone()),
            };

            if let Some(handle) = handle {
                if let Some(npc_id) = training_services::get_npc_by_boss_id(&actor.template_id) {
                    let mut msg = Message::new(-73);
                    let _ = msg.write_byte(npc_id as i8);
                    let _ = msg.write_byte(1);
                    handle.send_forget(PlayerMessage::SendPacket(msg));
                }
                let mut msg_pk = Message::new(-30);
                let _ = msg_pk.write_byte(35);
                let _ = msg_pk.write_int(actor.player.id as i64 as i32);
                let _ = msg_pk.write_byte(0);
                handle.send_forget(PlayerMessage::SendPacket(msg_pk));
                handle.send_forget(PlayerMessage::Modify(Box::new(|p| {
                    p.interaction_state.set_is_thachdau(false);
                    p.interaction_state.set_has_training_boss(false);
                })));
            }
        }

        let mut remove_msg = Message::new(-6);
        let _ = remove_msg.write_int(actor.player.id as i64 as i32);
        actor.zone_handle.broadcast(remove_msg);

        actor.receiver.close();
        actor.state = BossState::Escaping;
    }
}
