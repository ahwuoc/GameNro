use crate::matches::pvp::{change_type_pk, send_thong_bao, PvpMatch};
use crate::matches::{TypeLosePvp, TypePvp};
use crate::player::player_actor::TypePk;

/// Trả Thù - PVP khi player muốn trả thù kẻ đã giết mình
/// Đặc biệt: p1 được teleport đến zone của p2
pub struct TraThu {
    p1_id: i64,
    p2_id: i64,
    started: bool,
}

impl TraThu {
    pub fn new(p1_id: i64, p2_id: i64) -> Self {
        Self {
            p1_id,
            p2_id,
            started: false,
        }
    }
}

impl PvpMatch for TraThu {
    fn pvp_type(&self) -> TypePvp {
        TypePvp::TraThu
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
        self.started = true;
        // Đổi type PK cho cả 2
        change_type_pk(self.p1_id, TypePk::PkPvp);
        change_type_pk(self.p2_id, TypePk::PkPvp);
    }

    fn finish(&mut self) {
        // Không có logic đặc biệt
    }

    fn update(&mut self) {
        // Không có update logic
    }

    fn reward(&mut self, _winner_id: i64) {
        // Trả thù không có phần thưởng vàng
    }

    fn send_result(&self, loser_id: i64, type_lose: TypeLosePvp) {
        match type_lose {
            TypeLosePvp::RunsAway => {
                send_thong_bao(loser_id, "Bạn bị xử thua vì đã bỏ chạy");
            }
            TypeLosePvp::Dead => {
                // Nếu kẻ thù (p2) thua → xóa khỏi danh sách kẻ thù
                // TODO: implement enemy list removal khi có hệ thống enemy
                if loser_id == self.p2_id {
                    // p2 chết → p1 đã trả thù thành công
                    send_thong_bao(self.p1_id, "Đã trả thù thành công!");
                }
            }
        }
    }
}
