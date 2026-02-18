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
pub const TOURNAMENT_THOI_VANGS: [i64; 5] = [0, 0, 0, 0, 5];
pub const THOI_VANG_ITEM_ID: i16 = 457;

pub const TOURNAMENT_NAMES: [&str; 5] = [
    "Nhi đồng",
    "Siêu cấp 1",
    "Siêu cấp 2",
    "Siêu cấp 3",
    "Ngoại hạng",
];

// Map IDs
pub const MAP_VO_DAI: i32 = 51;
pub const MAP_PHONG_CHO: i32 = 52;
pub const MAP_SIEU_HANG: i32 = 113;
pub const MAP_DHVT_23: i32 = 129;

// Võ đài boundaries (fallOut check)
pub const ARENA_X_MIN: i32 = 158;
pub const ARENA_X_MAX: i32 = 610;
pub const ARENA_Y_MAX: i32 = 320;

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

/// Xác định hạng đấu theo giờ hiện tại
pub fn get_tournament_by_hour(hour: u32) -> i32 {
    match hour {
        // Mở thường trực để test — mỗi giờ 1 giải
        0 | 1 | 8 | 14 | 18 => NHI_DONG,
        2 | 3 | 9 | 13 | 19 => SIEU_CAP_1,
        4 | 5 | 10 | 15 | 20 => SIEU_CAP_2,
        6 | 7 | 11 | 16 | 21 => SIEU_CAP_3,
        12 | 17 | 22 | 23 => NGOAI_HANG,
        _ => NHI_DONG, // fallback
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
pub fn say_text(can_reg: bool, tournament: i32, reg_count: usize, hour: u32) -> String {
    if can_reg && tournament >= 0 {
        format!(
            "Chào mừng bạn đến với đại hội võ thuật\nGiải {} đang có {} người đăng ký thi đấu",
            TOURNAMENT_NAMES[tournament as usize], reg_count
        )
    } else {
        format!(
            "Đã hết hạn đăng ký thi đấu, xin vui lòng chờ đến giải sau vào lúc {}h",
            get_next_tournament_time(hour)
        )
    }
}
