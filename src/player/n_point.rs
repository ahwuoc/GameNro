#[derive(Debug, Clone)]
pub struct NPoint {
    pub base_hp: i32,
    pub base_mp: i32,
    pub base_dame: i32,
    pub base_def: i32,
    pub base_crit: i8,
    pub base_satamina: i16,

    pub final_hp: i32,
    pub final_mp: i32,
    pub final_dame: i32,
    pub final_def: i32,
    pub final_crit: i8,

    pub max_hp: i32,
    pub max_mp: i32,
    pub max_satamina: i16,

    pub speed: i8,
    pub power: i64,
    pub tiem_nang: i64,
    pub limit_power: i8,
}

impl NPoint {
    pub fn new() -> Self {
        NPoint {
            base_hp: 100,
            base_mp: 100,
            base_dame: 10,
            base_def: 5,
            base_crit: 0,
            final_crit: 0,
            final_dame: 0,
            final_def: 0,
            final_hp: 0,
            final_mp: 0,
            max_hp: 0,
            max_mp: 0,
            speed: 8,
            power: 0,
            tiem_nang: 0,
            limit_power: 0,
            base_satamina: 0,
            max_satamina: 0,
        }
    }
    pub fn update(&mut self) {
        if self.final_hp > self.max_hp {
            self.final_hp = self.max_hp;
        }
        if self.final_mp > self.max_mp {
            self.final_mp = self.max_mp;
        }
    }
}
