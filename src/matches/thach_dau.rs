use tracing_subscriber::util;

use crate::matches::pvp::{change_type_pk, send_money, send_thong_bao, PvpMatch};
use crate::matches::{TypeLosePvp, TypePvp};
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::TypePk;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::ServiceHandles;
use crate::utils;
use crate::utils::number_util::number_to_money;

/// Mức cược vàng cho phép
pub const GOLD_CHALLENGE: [i64; 3] = [1_000_000, 10_000_000, 100_000_000];

pub struct ThachDau {
    p1_id: i64,
    p2_id: i64,
    gold_thach_dau: i64,
    gold_reward: i64,
    started: bool,
}

impl ThachDau {
    pub fn new(p1_id: i64, p2_id: i64, gold_thach_dau: i64) -> Self {
        let gold_reward = gold_thach_dau / 100 * 80;
        Self {
            p1_id,
            p2_id,
            gold_thach_dau,
            gold_reward,
            started: false,
        }
    }
}

impl PvpMatch for ThachDau {
    fn pvp_type(&self) -> TypePvp {
        TypePvp::ThachDau
    }

    fn player1_id(&self) -> i64 {
        self.p1_id
    }

    fn player2_id(&self) -> i64 {
        self.p2_id
    }

    fn is_started(&self) -> bool {
        self.started
    }

    fn start(&mut self) {
        // Trừ vàng cả 2 player khi bắt đầu
        Self::modify_player_gold(self.p1_id, -self.gold_thach_dau);
        send_money(self.p1_id);
        Self::modify_player_gold(self.p2_id, -self.gold_thach_dau);
        send_money(self.p2_id);

        // Đổi type PK
        change_type_pk(self.p1_id, TypePk::PkPvp);
        change_type_pk(self.p2_id, TypePk::PkPvp);
        self.started = true;
    }

    fn finish(&mut self) {
        // Không có logic đặc biệt
    }

    fn update(&mut self) {
        // Không có update logic
    }

    fn reward(&mut self, winner_id: i64) {
        Self::modify_player_gold(winner_id, self.gold_reward);
        send_money(winner_id);
    }

    fn send_result(&self, loser_id: i64, type_lose: TypeLosePvp) {
        let winner_id = self.get_winner_id(loser_id);
        let gold_display = number_to_money(self.gold_reward);

        match type_lose {
            TypeLosePvp::RunsAway => {
                if PLAYER_MANAGER.contains(loser_id as u64) {
                    send_thong_bao(
                        winner_id,
                        &format!(
                            "Đối thủ sợ quá bỏ chạy, bạn thắng được {} vàng",
                            gold_display
                        ),
                    );
                } else {
                    send_thong_bao(
                        winner_id,
                        &format!("Đối thủ rời game, bạn thắng được {} vàng", gold_display),
                    );
                }
                send_thong_bao(loser_id, "Bạn bị xử thua vì đã bỏ chạy");
            }
            TypeLosePvp::Dead => {
                send_thong_bao(
                    winner_id,
                    &format!("Đối thủ đã kiệt sức, bạn thắng được {} vàng", gold_display),
                );
                send_thong_bao(loser_id, "Bạn đã thua vì đã kiệt sức");
            }
        }
    }
}

impl ThachDau {
    fn modify_player_gold(player_id: i64, gold_delta: i64) {
        if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
            handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
                let current_gold = player.inventory.get_gold();
                let new_gold = (current_gold + gold_delta).max(0);
                player.inventory.set_gold(new_gold);
                let _ = ServiceHandles::send_gold_gem_ruby_to_client(player);
            })));
        }
    }
}
