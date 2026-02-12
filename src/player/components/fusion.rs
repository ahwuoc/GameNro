#[derive(Debug, Clone)]
pub struct Fusion {
    pub type_fusion: i8,
    pub last_time_fusion: u64,
    pub template_id: i32,
}

impl Fusion {
    pub const NON_FUSION: i8 = 0;
    pub const LUONG_LONG_NHAT_THE: i8 = 4;
    pub const HOP_THE_VINH_VIEN: i8 = 1;
    pub const TIME_FUSION: u64 = 600_000;
    pub fn new() -> Self {
        Self {
            type_fusion: Self::NON_FUSION,
            last_time_fusion: 0,
            template_id: -1,
        }
    }

    pub fn is_fusion_expired(&self, now: u64) -> bool {
        if self.type_fusion == Self::LUONG_LONG_NHAT_THE {
            return now >= self.last_time_fusion + Self::TIME_FUSION;
        }
        false
    }

    pub fn is_timed_fusion(&self) -> bool {
        self.type_fusion == Self::LUONG_LONG_NHAT_THE
    }
}
