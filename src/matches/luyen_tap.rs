use crate::matches::pvp::{change_type_pk, send_thong_bao, PvpMatch};
use crate::matches::{TypeLosePvp, TypePvp};
use crate::player::player_actor::TypePk;

/// Luyện Tập - PVP không cược, không thưởng/phạt
pub struct LuyenTap {
    p1_id: i64,
    p2_id: i64,
    started: bool,
}

impl LuyenTap {
    pub fn new(p1_id: i64, p2_id: i64) -> Self {
        Self {
            p1_id,
            p2_id,
            started: false,
        }
    }
}

impl PvpMatch for LuyenTap {
    fn pvp_type(&self) -> TypePvp {
        TypePvp::LuyenTap
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
        // LuyenTap dùng PK_PVP_2 (khác ThachDau)
        change_type_pk(self.p1_id, TypePk::PkPvp2);
        change_type_pk(self.p2_id, TypePk::PkPvp2);
    }

    fn finish(&mut self) {
        // Không có gì đặc biệt
    }

    fn update(&mut self) {
        // Không có update logic
    }

    fn reward(&mut self, _winner_id: i64) {
        // Luyện tập không có phần thưởng
    }

    fn send_result(&self, loser_id: i64, type_lose: TypeLosePvp) {
        // Gửi thông báo "Kết thúc luyện tập" cho người còn lại
        let other_id = self.get_winner_id(loser_id);
        match type_lose {
            TypeLosePvp::RunsAway | TypeLosePvp::Dead => {
                send_thong_bao(other_id, "Kết thúc luyện tập");
            }
        }
    }

    fn pk_type(&self) -> TypePk {
        TypePk::PkPvp2
    }
}
