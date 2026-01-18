#[derive(Debug, Clone)]
pub struct NPoint {
    // Chỉ số gốc (base stats từ database hoặc level up)
    pub hpg: i32,
    pub mpg: i32,
    pub dameg: i32,
    pub defg: i32,
    pub critg: i8,

    // Chỉ số hiện tại (sau khi tính toán)
    pub hp: i32,
    pub mp: i32,
    pub dame: i32,
    pub def: i32,
    pub crit: i8,

    // Chỉ số tối đa
    pub hp_max: i32,
    pub mp_max: i32,
    pub max_stamina: i16,
    pub stamina: i16,

    // Chỉ số bonus cộng thêm (từ item options, buff, ...)
    pub hp_add: i32,
    pub mp_add: i32,
    pub dame_add: i32,
    pub def_add: i32,
    pub crit_add: i8,

    // Tỉ lệ % cộng thêm (từ item options)
    pub tl_hp: Vec<i32>,   // +#% HP
    pub tl_mp: Vec<i32>,   // +#% KI
    pub tl_dame: Vec<i32>, // +#% Sức đánh
    pub tl_def: i16,       // +#% Giáp

    // Các chỉ số khác
    pub speed: i8,
    pub power: i64,
    pub tiem_nang: i64,
    pub limit_power: i8,
}

impl NPoint {
    pub fn new() -> Self {
        NPoint {
            hpg: 100,
            mpg: 100,
            dameg: 10,
            defg: 5,
            critg: 1,

            hp: 1000000,
            mp: 1000000,
            dame: 100000,
            def: 5,
            crit: 1,

            hp_max: 1000000,
            mp_max: 1000000,
            max_stamina: 100,
            stamina: 100,

            hp_add: 0,
            mp_add: 0,
            dame_add: 0,
            def_add: 0,
            crit_add: 0,

            tl_hp: Vec::new(),
            tl_mp: Vec::new(),
            tl_dame: Vec::new(),
            tl_def: 0,

            speed: 8,
            power: 1000,
            tiem_nang: 0,
            limit_power: 1,
        }
    }

    pub fn cal_point(&mut self) {
        self.reset_bonus();
        self.set_base_point();
    }

    fn reset_bonus(&mut self) {
        self.hp_add = 0;
        self.mp_add = 0;
        self.dame_add = 0;
        self.def_add = 0;
        self.crit_add = 0;
        self.tl_hp.clear();
        self.tl_mp.clear();
        self.tl_dame.clear();
        self.tl_def = 0;
    }

    pub fn set_base_point(&mut self) {
        self.set_hp_max();
        self.set_mp_max();
        self.set_dame();
        self.set_def();
        self.set_crit();
        self.clamp_current_values();
    }

    /// Tính HP Max = hpg + hp_add + (hpg * sum(tl_hp) / 100)
    fn set_hp_max(&mut self) {
        let mut hp_max: i64 = (self.hpg + self.hp_add) as i64;

        // Cộng thêm % từ trang bị
        for tl in &self.tl_hp {
            hp_max += hp_max * (*tl as i64) / 100;
        }

        // TODO: Thêm các bonus khác (set đồ, buff, ...)

        self.hp_max = hp_max.min(i32::MAX as i64) as i32;
    }

    /// Tính MP Max = mpg + mp_add + (mpg * sum(tl_mp) / 100)
    fn set_mp_max(&mut self) {
        let mut mp_max: i64 = (self.mpg + self.mp_add) as i64;

        for tl in &self.tl_mp {
            mp_max += mp_max * (*tl as i64) / 100;
        }

        self.mp_max = mp_max.min(i32::MAX as i64) as i32;
    }

    /// Tính Dame = dameg + dame_add + (dameg * sum(tl_dame) / 100)
    fn set_dame(&mut self) {
        let mut dame: i64 = (self.dameg + self.dame_add) as i64;

        for tl in &self.tl_dame {
            dame += dame * (*tl as i64) / 100;
        }

        self.dame = dame.min(i32::MAX as i64) as i32;
    }

    /// Tính Def = defg + def_add + (defg * tl_def / 100)
    fn set_def(&mut self) {
        let mut def: i64 = (self.defg + self.def_add) as i64;
        def += def * (self.tl_def as i64) / 100;
        self.def = def.min(i32::MAX as i64) as i32;
    }

    /// Tính Crit = critg + crit_add
    fn set_crit(&mut self) {
        self.crit = self.critg.saturating_add(self.crit_add);
    }

    /// Đảm bảo hp/mp không vượt quá max
    fn clamp_current_values(&mut self) {
        if self.hp > self.hp_max {
            self.hp = self.hp_max;
        }
        if self.mp > self.mp_max {
            self.mp = self.mp_max;
        }
        if self.stamina > self.max_stamina {
            self.stamina = self.max_stamina;
        }
    }

    pub fn set_hp(&mut self, value: i32) {
        self.hp = value.min(self.hp_max).max(0);
    }

    pub fn set_mp(&mut self, value: i32) {
        self.mp = value.min(self.mp_max).max(0);
    }

    pub fn add_option(&mut self, option_id: i8, param: i16) {
        match option_id {
            0 => self.dame_add += param as i32, // Tấn công +#
            2 => {
                // HP, KI +#000
                self.hp_add += (param as i32) * 1000;
                self.mp_add += (param as i32) * 1000;
            }
            6 => self.hp_add += param as i32, // HP +#
            7 => self.mp_add += param as i32, // KI +#
            14 => self.crit_add = self.crit_add.saturating_add(param as i8), // Chí mạng +#%
            22 => self.hp_add += (param as i32) * 1000, // HP +#K
            23 => self.mp_add += (param as i32) * 1000, // MP +#K
            47 => self.def_add += param as i32, // Giáp +#
            48 => {
                // HP/KI +#
                self.hp_add += param as i32;
                self.mp_add += param as i32;
            }
            49 | 50 => self.tl_dame.push(param as i32), // Tấn công/Sức đánh +#%
            77 => self.tl_hp.push(param as i32),        // HP +#%
            94 => self.tl_def += param,                 // Giáp +#%
            103 => self.tl_mp.push(param as i32),       // KI +#%
            _ => {}                                     // Các option khác chưa xử lý
        }
    }

    pub fn get_dame_attack(&self, is_crit: bool) -> i32 {
        let mut dame = self.dame;
        if is_crit {
            dame *= 2;
        }
        dame
    }
}

impl Default for NPoint {
    fn default() -> Self {
        Self::new()
    }
}
