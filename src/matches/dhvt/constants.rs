/// Constants for DHVT (Đại Hội Võ Thuật) tournament system

// 5 hạng đấu
pub const NHI_DONG: i32 = 0;
pub const SIEU_CAP_1: i32 = 1;
pub const SIEU_CAP_2: i32 = 2;
pub const SIEU_CAP_3: i32 = 3;
pub const NGOAI_HANG: i32 = 4;

// Thời gian (phút trong giờ)
pub const MINS_MAX_CAN_REG: u32 = 50; // mở cả giờ để test (bản gốc: 25)
pub const MINS_START: u32 = 55;
pub const MINS_END: u32 = 57;

// Phí đăng ký
pub const TOURNAMENT_GEMS: [i64; 5] = [200, 400, 600, 800, 0];
// Map IDs
pub const MAP_VO_DAI: i32 = 51;
pub const MAP_PHONG_CHO: i32 = 52;
pub const MAP_SIEU_HANG: i32 = 113;
pub const MAP_DHVT_23: i32 = 129;

// Võ đài boundaries (fallOut check)
pub const ARENA_X_MIN: i16 = 158;
pub const ARENA_X_MAX: i16 = 610;
pub const ARENA_Y_MAX: i16 = 320;

// Vị trí spawn trong võ đài
pub const P1_SPAWN_X: i32 = 328;
pub const P1_SPAWN_Y: i32 = 312;
pub const P2_SPAWN_X: i32 = 443;
pub const P2_SPAWN_Y: i32 = 312;
pub const NPC_Y_VISIBLE: i32 = 312;
pub const NPC_Y_HIDDEN: i32 = 10000;

// Match timing
pub const MATCH_TICK_MS: u64 = 150;
pub const MATCH_COUNTDOWN_TICKS: i32 = 23;
pub const MATCH_FIGHT_TICKS: i32 = 180;

// Reward items
pub const REWARD_ITEM_ID: i16 = 77;
pub const REWARD_ITEM_QUANTITY: i32 = 50;
pub const REWARD_STONE_IDS: [i16; 5] = [220, 221, 222, 223, 224];

// Text thông báo
pub const TEXT_TRUAT_QUYEN: &str = "Bạn đã bị truất quyền thi đấu vì không đến đúng giờ";
pub const TEXT_DOI_THU_BO_CUOC: &str =
    "Bạn đã thắng vì đối thủ đã bỏ cuộc, chờ tại đây để thi đấu vòng tiếp theo";
pub const TEXT_NPC_CHAT_ROI_DAI: &str = "Đối thủ đã rơi khỏi võ đài, %1 đã thắng";
pub const TEXT_DANG_KY_THANH_CONG: &str =
    "Bạn đã đăng ký thành công, nhớ có mặt tại đây trước %1h30\nBây giờ là %2, đến trễ coi như bỏ cuộc";
pub const TEXT_CHIA_BUON: &str = "Bạn đã thua, hẹn gặp lại ở giải sau";
pub const TEXT_DOI_THU_BO_CUOC_ROI_MAP: &str = "Đối thủ bỏ cuộc, bạn đã chiến thắng";
pub const TEXT_XU_THUA_BO_CHAY: &str = "Bạn bị xử thua vì đã bỏ chạy";
pub const TEXT_NPC_CHAT_DOI_THU_BO_CUOC_ROI_MAP: &str = "Đối thủ bỏ cuộc %1 đã thắng";
pub const TEXT_DOI_THU_KIET_SUC: &str = "Đối thủ đã kiệt sức, bạn đã thắng";
pub const TEXT_NPC_CHAT_DOI_THU_KIET_SUC: &str = "Đối thủ đã kiệt sức, %1 đã thắng";
pub const TEXT_NPC_CHAT_HET_GIO: &str = "Hết giờ thi đấu %1 đã chiến thắng vì bị thương ít hơn";
pub const TEXT_HUY_DANG_KY: &str = "Bạn đã hủy đăng ký thành công";
pub const TEXT_THANG_VONG_NAY: &str =
    "Bạn đã thắng vòng này, xin chờ tại đây ít phút để thi đấu vòng sau";
pub const TEXT_DA_VO_DICH: &str = "Bạn đã vô địch giải gần đây, vui lòng đợi giải sau";
pub const TEXT_VO_DICH: &str =
    "Bạn đã vô địch giải đấu, xin chúc mừng bạn, bạn được thưởng 5 viên đá nâng cấp";
pub const TEXT_KHOE_VO_DICH: &str = "Chúc mừng %1 vừa vô địch giải %2";

// ─── Tournament Class ───

#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TournamentClass {
    NhiDong,
    SieuCap1,
    Sieucap2,
    Sieucap3,
    Ngoaihang,
}
impl Default for TournamentClass {
    fn default() -> Self {
        Self::NhiDong
    }
}
impl TournamentClass {
    pub fn get_name(&self) -> &str {
        match self {
            Self::Ngoaihang => "Ngoai hang",
            Self::NhiDong => "Nhi Dong",
            Self::Sieucap2 => "Sieu cap 2",
            Self::Sieucap3 => "Sieu cap 3",
            Self::SieuCap1 => "Sieu cap 1",
        }
    }
    pub fn register_cost(&self) -> CostType {
        match self {
            Self::Ngoaihang => CostType::Gem(20),
            Self::NhiDong => CostType::Gold(20),
            Self::Sieucap2 => CostType::Gem(150),
            Self::Sieucap3 => CostType::Gem(175),
            Self::SieuCap1 => CostType::Gem(125),
        }
    }
    pub fn from_hour(hour: u32) -> Option<Self> {
        match hour {
            0 | 1 | 8 | 14 | 18 => Some(Self::Sieucap3),
            2 | 3 | 9 | 13 | 19 => Some(Self::SieuCap1),
            4 | 5 | 10 | 15 | 20 => Some(Self::Sieucap2),
            6 | 7 | 11 | 16 | 21 => Some(Self::Ngoaihang),
            12 | 17 | 22 | 23 => Some(Self::NhiDong),
            _ => None,
        }
    }
}
pub enum CostType {
    Gold(i32),
    Gem(i32),
}
impl CostType {
    pub fn get_text(&self) -> String {
        match self {
            Self::Gold(amt) => format!("{} thỏi vàng", amt),
            Self::Gem(amt) => format!("{} ngọc", amt),
        }
    }
}

/// Giờ giải đấu tiếp theo
pub fn get_next_tournament_time(hour: u32) -> u32 {
    let next = hour + 1;
    if next > 23 || next < 8 {
        8
    } else {
        next
    }
}

/// Text NPC nói khi mở menu
pub fn say_text(can_reg: bool, tournament: TournamentClass, reg_count: usize, hour: u32) -> String {
    if can_reg {
        format!(
            "Chào mừng bạn đến với đại hội võ thuật\nGiải {} đang có {} người đăng ký thi đấu",
            tournament.get_name(),
            reg_count
        )
    } else {
        format!(
            "Đã hết hạn đăng ký thi đấu, xin vui lòng chờ đến giải sau vào lúc {}h",
            get_next_tournament_time(hour)
        )
    }
}
