#[derive(Debug, Clone)]
pub struct Fusion {
    pub type_fusion: i8,
    pub last_time_fusion: u64,
}

impl Fusion {
    pub const NON_FUSION: i8 = 0;
    pub const LUONG_LONG_NHAT_THE: i8 = 4;
    pub const HOP_THE_PORATA: i8 = 6;
    pub const HOP_THE_PORATA2: i8 = 7;

    pub fn new() -> Self {
        Self {
            type_fusion: Self::NON_FUSION,
            last_time_fusion: 0,
        }
    }
}
