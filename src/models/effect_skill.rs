#[derive(Debug, Clone, Default)]
pub struct EffectSkill {
    // choang
    pub is_stun: bool,
    pub time_stun: u64,
    pub last_time_stun: u64,

    // khieng nang luong
    pub is_shield: bool,
    pub shield_duration_ms: u64,
    pub shield_start_time: u64,

    // dich chuyen tuc thoi (blind)
    pub is_blind_dctt: bool,
    pub time_blind_dctt: u64,
    pub start_time_dctt: u64,

    // thoi mien
    pub is_thoi_mien: bool,
    pub time_thoi_mien: u64,
    pub start_time_thoi_mien: u64,

    // tai tao nang luong (charging)
    pub is_charging: bool,
    pub count_charging: i32,

    // bien khi (monkey transformation)
    pub is_skill_bienkhi: bool,
    pub time_duration_bienkhi: u64,
    pub time_start_bienkhi: u64,
    pub is_monkey: bool,
    pub time_monkey: u64,
    pub last_time_up_monkey: u64,
    pub level_monkey: i8,
}

impl EffectSkill {
    pub fn new() -> Self {
        Self::default()
    }
}
