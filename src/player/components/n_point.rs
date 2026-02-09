use rand::Rng;

use crate::player::components::PointType;
use crate::templates::power_manager;

#[derive(Debug, Clone)]
pub struct NPoint {
    // ==========================================
    // 1. CHỈ SỐ GỐC (Base Stats)
    // ==========================================
    pub hp_base: i32,   // HP Gốc
    pub mp_base: i32,   // MP Gốc (KI)
    pub dame_base: i32, // Sức đánh Gốc
    pub def_base: i32,  // Giáp Gốc
    pub crit_base: i8,  // Chí mạng Gốc

    // ==========================================
    // 2. CHỈ SỐ HIỆN TẠI (Current Stats)
    // ==========================================
    pub hp_current: i32, // HP hiện tại
    pub mp_current: i32, // MP hiện tại
    pub dame: i32,       // Sức đánh thực tế
    pub def: i32,        // Giáp thực tế
    pub crit: i8,        // Chí mạng thực tế

    // ==========================================
    // 3. CHỈ SỐ TỐI ĐA (Max Stats)
    // - Là giới hạn trên của chỉ số hiện tại
    // - Tính toán từ: Gốc + Đồ + Skill + Buff
    // - hp_max = hpg + (hpg * %máu đồ) + máu đồ + ...
    // ==========================================
    pub hp_max: i32,      // HP Tối đa
    pub mp_max: i32,      // MP Tối đa
    pub max_stamina: i16, // Thể lực tối đa (thường là 10000)
    pub stamina: i16,     // Thể lực hiện tại

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

    pub huyt_sao_buff: i32, // % buff HP max từ skill Huýt Sáo

    // Các chỉ số khác
    pub speed: i8,
    pub power: i64,
    pub tiem_nang: i64,
    pub limit_power: i8,

    pub hp_fusion: i32,
    pub mp_fusion: i32,
    pub dame_fusion: i32,
    pub def_fusion: i32,
    pub crit_fusion: i8,

    pub hp_fusion_tl: i32,
    pub mp_fusion_tl: i32,
    pub dame_fusion_tl: i32,

    pub is_monkey_active: bool,
    pub hp_hoi: i32,
    pub mp_hoi: i32,
    pub last_time_hoi_phuc: u64,
    pub last_time_hoi_stamina: u64,
}

impl NPoint {
    pub fn new() -> Self {
        NPoint {
            hp_base: 100,
            mp_base: 100,
            dame_base: 10,
            def_base: 5,
            crit_base: 1,

            hp_current: 100,
            mp_current: 100,
            dame: 10,
            def: 5,
            crit: 1,

            hp_max: 100,
            mp_max: 100,
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

            huyt_sao_buff: 0,

            speed: 8,
            power: 1000,
            tiem_nang: 0,
            limit_power: 1,

            hp_fusion: 0,
            mp_fusion: 0,
            dame_fusion: 0,
            def_fusion: 0,
            crit_fusion: 0,

            hp_fusion_tl: 0,
            mp_fusion_tl: 0,
            dame_fusion_tl: 0,

            is_monkey_active: false,
            hp_hoi: 0,
            mp_hoi: 0,
            last_time_hoi_phuc: 0,
            last_time_hoi_stamina: 0,
        }
    }
    pub fn set_power(&mut self, power: i64) {
        self.power = power;
    }
    pub fn set_tiem_nang(&mut self, tiem_nang: i64) {
        self.tiem_nang = tiem_nang;
    }
    pub fn set_limit_power(&mut self, limit_power: i8) {
        self.limit_power = limit_power;
    }
    pub fn set_stamina(&mut self, stamina: i16) {
        self.stamina = stamina;
    }
    pub fn set_max_stamina(&mut self, max_stamina: i16) {
        self.max_stamina = max_stamina;
    }

    pub fn set_hp_chiso(&mut self, hp_chiso: i32) {
        self.hp_base = hp_chiso;
    }

    pub fn set_mp_chiso(&mut self, mp_chiso: i32) {
        self.mp_base = mp_chiso;
    }

    pub fn set_dame_chiso(&mut self, dame_chiso: i32) {
        self.dame_base = dame_chiso;
    }

    pub fn set_def_chiso(&mut self, def_chiso: i32) {
        self.def_base = def_chiso;
    }

    pub fn set_crit_chiso(&mut self, crit_chiso: i8) {
        self.crit_base = crit_chiso;
    }
    pub fn set_hp_current(&mut self, hp_current: i32) {
        self.hp_current = hp_current;
    }
    pub fn set_mp_current(&mut self, mp_current: i32) {
        self.mp_current = mp_current;
    }

    pub fn cal_point(&mut self) {
        self.reset_bonus();
        self.set_base_point();
    }
    pub fn current_hp_add(&mut self, hp_add: i32) -> i32 {
        self.hp_current = (self.hp_current + hp_add).min(self.hp_max);
        self.hp_current
    }

    pub fn current_mp_add(&mut self, mp_add: i32) -> i32 {
        self.mp_current = (self.mp_current + mp_add).min(self.mp_max);
        self.mp_current
    }

    pub fn current_hp_sub(&mut self, hp_sub: i32) -> i32 {
        self.hp_current = (self.hp_current - hp_sub).max(0);
        self.hp_current
    }

    pub fn current_mp_sub(&mut self, mp_sub: i32) -> i32 {
        self.mp_current = (self.mp_current - mp_sub).max(0);
        self.mp_current
    }

    pub fn current_stamina_add(&mut self, stamina_add: i16) -> i16 {
        self.stamina = (self.stamina + stamina_add).min(self.max_stamina);
        self.stamina
    }

    pub fn current_stamina_sub(&mut self, stamina_sub: i16) -> i16 {
        self.stamina = (self.stamina - stamina_sub).max(0);
        self.stamina
    }

    pub fn power_add(&mut self, power_add: i64) -> i64 {
        self.power = self.power.saturating_add(power_add);
        self.power
    }

    pub fn tiem_nang_add(&mut self, tiem_nang_add: i64) -> i64 {
        self.tiem_nang = self.tiem_nang.saturating_add(tiem_nang_add);
        self.tiem_nang
    }

    pub fn tiem_nang_sub(&mut self, tiem_nang_sub: i64) -> bool {
        if self.tiem_nang >= tiem_nang_sub {
            self.tiem_nang -= tiem_nang_sub;
            true
        } else {
            false
        }
    }

    pub fn is_full_hp(&self) -> bool {
        self.hp_current >= self.hp_max
    }

    pub fn is_full_mp(&self) -> bool {
        self.mp_current >= self.mp_max
    }

    pub fn is_full_hp_mp(&self) -> bool {
        self.is_full_hp() && self.is_full_mp()
    }

    pub fn has_mp(&self, amount: i32) -> bool {
        self.mp_current >= amount
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
        self.hp_hoi = 0;
        self.mp_hoi = 0;
    }

    pub fn set_base_point(&mut self) {
        self.set_hp_max();
        self.set_mp_max();
        self.set_dame();
        self.set_def();
        self.set_crit();
        self.clamp_current_values();
    }

    fn set_hp_max(&mut self) {
        let mut hp_max: i64 = (self.hp_base + self.hp_add + self.hp_fusion) as i64;
        for tl in &self.tl_hp {
            hp_max += hp_max * (*tl as i64) / 100;
        }
        if self.hp_fusion_tl > 0 {
            hp_max += hp_max * (self.hp_fusion_tl as i64) / 100;
        }
        if self.huyt_sao_buff > 0 {
            hp_max += hp_max * (self.huyt_sao_buff as i64) / 100;
        }
        self.hp_max = hp_max.min(i32::MAX as i64) as i32;
    }

    fn set_mp_max(&mut self) {
        let mut mp_max: i64 = (self.mp_base + self.mp_add + self.mp_fusion) as i64;

        for tl in &self.tl_mp {
            mp_max += mp_max * (*tl as i64) / 100;
        }

        if self.mp_fusion_tl > 0 {
            mp_max += mp_max * (self.mp_fusion_tl as i64) / 100;
        }

        self.mp_max = mp_max.min(i32::MAX as i64) as i32;
    }

    fn set_dame(&mut self) {
        let mut dame: i64 = (self.dame_base + self.dame_add + self.dame_fusion) as i64;

        for tl in &self.tl_dame {
            dame += dame * (*tl as i64) / 100;
        }

        if self.dame_fusion_tl > 0 {
            dame += dame * (self.dame_fusion_tl as i64) / 100;
        }

        self.dame = dame.min(i32::MAX as i64) as i32;
    }

    fn set_def(&mut self) {
        let mut def: i64 = (self.def_base + self.def_add + self.def_fusion) as i64;
        def += def * (self.tl_def as i64) / 100;
        self.def = def.min(i32::MAX as i64) as i32;
    }

    fn set_crit(&mut self) {
        self.crit = self
            .crit_base
            .saturating_add(self.crit_add)
            .saturating_add(self.crit_fusion);
        if self.is_monkey_active {
            self.crit = 110;
        }
    }

    fn clamp_current_values(&mut self) {
        if self.hp_current > self.hp_max {
            self.hp_current = self.hp_max;
        }
        if self.mp_current > self.mp_max {
            self.mp_current = self.mp_max;
        }
        if self.stamina > self.max_stamina {
            self.stamina = self.max_stamina;
        }
    }

    pub fn set_hp(&mut self, value: i32) {
        self.hp_current = value.min(self.hp_max).max(0);
    }

    pub fn set_mp(&mut self, value: i32) {
        self.mp_current = value.min(self.mp_max).max(0);
    }

    pub fn set_full_hp(&mut self) {
        self.hp_current = self.hp_max;
    }

    pub fn set_full_mp(&mut self) {
        self.mp_current = self.mp_max;
    }

    pub fn set_full_hp_mp(&mut self) {
        self.set_full_hp();
        self.set_full_mp();
    }

    pub fn add_option(&mut self, option_id: i8, param: i16) {
        match option_id {
            0 => self.dame_add += param as i32, // Tấn công +#
            2 => {
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
            80 => self.hp_hoi += param as i32,          // HP hồi +#
            81 => self.mp_hoi += param as i32,          // KI hồi +#
            82 => self.hp_fusion_tl += param as i32,    // HP +#% khi hợp thể
            83 => self.mp_fusion_tl += param as i32,    // KI +#% khi hợp thể
            84 => self.dame_fusion_tl += param as i32,  // Sức đánh +#% khi hợp thể
            94 => self.tl_def += param,                 // Giáp +#%
            103 => self.tl_mp.push(param as i32),       // KI +#%
            _ => {}                                     // Các option khác chưa xử lý
        }
    }

    pub fn roll_crit(&self) -> bool {
        if self.crit >= 100 {
            return true;
        }
        if self.crit <= 0 {
            return false;
        }
        let mut rng = rand::rng();
        let roll: i8 = rng.random_range(0..100);
        roll < self.crit
    }

    pub fn get_dame_attack(&self, is_crit: bool) -> i32 {
        let mut dame = self.dame;
        if is_crit {
            dame *= 2;
        }
        dame
    }
    pub fn scale_tiemnang_by_power(&self, amount: i64) -> i64 {
        if self.power >= 80_000_000_000 {
            return amount / 100;
        }
        if self.power >= 50_000_000_000 {
            return amount / 50;
        }
        if self.power >= 30_000_000_000 {
            return amount / 20;
        }
        if self.power >= 10_000_000_000 {
            return amount / 10;
        }
        if self.power >= 5_000_000_000 {
            return amount / 5;
        }
        if self.power >= 1_000_000_000 {
            return amount / 2;
        }
        amount
    }

    pub fn tiemnang_add(&mut self, amount: i64) {
        self.tiem_nang += amount;
    }
    pub fn sucmanh_add(&mut self, amount: i64) {
        self.power += amount;
    }

    pub fn increase_point(&mut self, type_incr: u8, point: i16) -> Result<(), &'static str> {
        if !(1..1000).contains(&point) {
            return Err("Số lượng điểm không hợp lệ");
        }
        let point_type = PointType::try_from(type_incr)?;

        let p = i32::from(point);
        let p64 = i64::from(p);
        match point_type {
            PointType::Hp => {
                let inc = p * 20;
                let cost = p64 * (2 * (self.hp_base as i64 + 1000) + inc as i64 - 20) / 2;

                if self.hp_base + inc > self.get_hp_mp_limit() {
                    return Err("Vui lòng mở giới hạn sức mạnh");
                }
                if !self.do_use_tiem_nang(cost) {
                    return Err("Bạn không đủ tiềm năng");
                }
                self.hp_base += inc;
            }

            PointType::Mp => {
                let inc = p * 20;
                let cost = p64 * (2 * (self.mp_base as i64 + 1000) + inc as i64 - 20) / 2;

                if self.mp_base + inc > self.get_hp_mp_limit() {
                    return Err("Vui lòng mở giới hạn sức mạnh");
                }
                if !self.do_use_tiem_nang(cost) {
                    return Err("Bạn không đủ tiềm năng");
                }
                self.mp_base += inc;
            }

            PointType::Dame => {
                let cost = p64 * (2 * self.dame_base as i64 + p64 - 1) / 2 * 100;

                if self.dame_base + p > self.get_dame_limit() {
                    return Err("Vui lòng mở giới hạn sức mạnh");
                }
                if !self.do_use_tiem_nang(cost) {
                    return Err("Bạn không đủ tiềm năng");
                }
                self.dame_base += p;
            }

            PointType::Def => {
                let cost = (self.def_base as i64 + 5) * 100_000;

                if self.def_base + p > self.get_def_limit() {
                    return Err("Vui lòng mở giới hạn sức mạnh");
                }
                if !self.do_use_tiem_nang(cost) {
                    return Err("Bạn không đủ tiềm năng");
                }
                self.def_base += p;
            }

            PointType::Crit => {
                let mut cost = 50_000_000i64;
                for _ in 0..self.crit_base {
                    cost *= 5;
                }

                if i16::from(self.crit_base) + point > self.get_crit_limit() as i16 {
                    return Err("Vui lòng mở giới hạn sức mạnh");
                }
                if !self.do_use_tiem_nang(cost) {
                    return Err("Bạn không đủ tiềm năng");
                }
                self.crit_base += point as i8;
            }
        }
        Ok(())
    }

    fn do_use_tiem_nang(&mut self, tiem_nang: i64) -> bool {
        self.tiem_nang_sub(tiem_nang)
    }

    pub fn get_power_limit(&self) -> i64 {
        if let Some(limit) = power_manager::get_limit(self.limit_power as i32) {
            return limit.power as i64;
        }
        0
    }
    pub fn get_hp_mp_limit(&self) -> i32 {
        if let Some(limit) = power_manager::get_limit(self.limit_power as i32) {
            return limit.hp as i32;
        }
        0
    }

    pub fn get_dame_limit(&self) -> i32 {
        if let Some(limit) = power_manager::get_limit(self.limit_power as i32) {
            return limit.damage as i32;
        }
        0
    }

    pub fn get_def_limit(&self) -> i32 {
        if let Some(limit) = power_manager::get_limit(self.limit_power as i32) {
            return limit.defense;
        }
        0
    }

    pub fn get_crit_limit(&self) -> i8 {
        if let Some(limit) = power_manager::get_limit(self.limit_power as i32) {
            return limit.critical as i8;
        }
        0
    }
}

impl Default for NPoint {
    fn default() -> Self {
        Self::new()
    }
}
