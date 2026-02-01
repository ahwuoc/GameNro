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

    // huyt sao (HP max buff)
    pub ti_le_hp_huyt_sao: i32,
    pub last_time_huyt_sao: u64,

    // troi (hold/bind skill)
    pub use_troi: bool,          // caster is holding target
    pub time_troi: u64,          // duration
    pub start_time_troi: u64,    // start time
    pub an_troi: bool,           // target is being held
    pub time_an_troi: u64,       // duration being held
    pub start_time_an_troi: u64, // start time being held
    pub pl_troi_id: Option<u64>, // ID of player who troi this target
}

#[derive(Debug, Default)]
pub struct EffectUpdateResult {
    pub shield_removed: bool,
    pub charge_stopped: bool,
    pub bienkhi_finished: bool,
    pub monkey_down: bool,
    pub huyt_sao_expired: bool,
}

impl EffectSkill {
    pub fn new() -> Self {
        Self::default()
    }

    pub fn update(&mut self, now: u64) -> EffectUpdateResult {
        let mut result = EffectUpdateResult::default();

        // Shield expire
        if self.is_shield && now > self.shield_start_time + self.shield_duration_ms {
            self.is_shield = false;
            result.shield_removed = true;
        }

        // Charging max reached
        if self.is_charging && self.count_charging >= 10 {
            self.is_charging = false;
            self.count_charging = 0;
            result.charge_stopped = true;
        }

        // Bien Khi animation finish (transform into monkey)
        if self.is_skill_bienkhi && now > self.time_start_bienkhi + self.time_duration_bienkhi {
            self.is_skill_bienkhi = false;
            result.bienkhi_finished = true;
        }

        // Monkey duration expire
        if self.is_monkey && now > self.last_time_up_monkey + self.time_monkey {
            self.is_monkey = false;
            self.level_monkey = 0;
            result.monkey_down = true;
        }

        // Huyt Sao buff expire (30s)
        if self.ti_le_hp_huyt_sao > 0 && now > self.last_time_huyt_sao + 30000 {
            self.ti_le_hp_huyt_sao = 0;
            result.huyt_sao_expired = true;
        }

        result
    }
}
