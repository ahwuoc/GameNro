#[derive(Debug, Clone, Default)]
pub struct EffectSkill {
    // choang
    pub is_stun: bool,
    pub time_stun: u64,
    pub last_time_stun: u64,

    // khieng nang luong
    pub is_shielding: bool,
    pub time_shield: u64,
    pub last_time_shield_up: u64,

    // dich chuyen tuc thoi (blind)
    pub is_blind_dctt: bool,
    pub time_blind_dctt: u64,
    pub last_time_blind_dctt: u64,

    // thoi mien
    pub is_thoi_mien: bool,
    pub time_thoi_mien: u64,
    pub last_time_thoi_mien: u64,
}

impl EffectSkill {
    pub fn new() -> Self {
        Self::default()
    }
}
